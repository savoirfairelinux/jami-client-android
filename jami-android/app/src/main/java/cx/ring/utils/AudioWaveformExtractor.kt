/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.annotation.VisibleForTesting
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Decodes an audio file into a small set of normalized amplitudes (each in 0..1) suitable for
 * rendering a waveform. Decoding happens off the main thread; results can be cached by callers via
 * [extractCached] (persistent, keyed by a file hash) on top of any in-memory cache.
 *
 * To keep extraction fast, the decoded PCM is subsampled (one channel, roughly a thousand samples
 * per second) rather than inspecting every sample; the per-bucket RMS loudness is unaffected in
 * practice. See [TARGET_SAMPLES_PER_SECOND].
 */
object AudioWaveformExtractor {

    private val TAG = AudioWaveformExtractor::class.simpleName

    /** Number of amplitude buckets produced. The view downsamples further to fit its width. */
    const val DEFAULT_BUCKETS = 192

    /**
     * Extractions currently running, keyed by cache key. Lets concurrent requests for the same file
     * share a single decode instead of each spawning their own (see [extractCached]).
     */
    private val inFlight = ConcurrentHashMap<String, Single<FloatArray>>()

    /** Asynchronously extract a waveform from [file]. Emits an empty array if decoding fails. */
    fun extract(file: File, buckets: Int = DEFAULT_BUCKETS): Single<FloatArray> =
        Single.fromCallable { compute(file, buckets) }
            .subscribeOn(Schedulers.computation())

    /**
     * Like [extract], but backed by a persistent on-disk cache keyed by [cacheKey] (typically the
     * message file hash, which is stable for a given file content). The decode only runs on a cache
     * miss; the result is then written under [cacheDir] so later launches reuse it instantly.
     *
     * Concurrent requests for the same [cacheKey] are coalesced into a single shared decode. The
     * inline player can bind the same message several times before the first decode finishes (e.g.
     * RecyclerView recycling), and without this each bind would start its own expensive decode of
     * the same file in parallel. The shared work runs to completion even if every subscriber goes
     * away, so it still populates the disk cache.
     */
    fun extractCached(
        file: File,
        cacheDir: File,
        cacheKey: String,
        buckets: Int = DEFAULT_BUCKETS
    ): Single<FloatArray> = Single.defer {
        inFlight.computeIfAbsent(cacheKey) {
            Single.fromCallable {
                readCache(cacheDir, cacheKey, buckets)
                    ?: compute(file, buckets).also { result ->
                        if (result.isNotEmpty()) writeCache(cacheDir, cacheKey, result)
                    }
            }
                .subscribeOn(Schedulers.computation())
                .doFinally { inFlight.remove(cacheKey) }
                .cache()
        }
    }

    /** Synchronous extraction used by tests. */
    @VisibleForTesting
    internal fun computeBlocking(
        file: File,
        buckets: Int = DEFAULT_BUCKETS
    ): FloatArray = compute(file, buckets)

    private fun compute(file: File, buckets: Int): FloatArray {
        if (!file.exists() || file.length() == 0L) return FloatArray(0)

        val timeStart = System.currentTimeMillis()

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return FloatArray(0)

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
                format.getLong(MediaFormat.KEY_DURATION) else 0L
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return FloatArray(0)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            // PCM layout: drives how aggressively we subsample (see frameStride below). The decoder
            // can refine these via INFO_OUTPUT_FORMAT_CHANGED, so they're updated there too.
            var channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1) else 1
            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 0
            var frameStride = frameStrideFor(sampleRate)

            val sums = DoubleArray(buckets)
            val counts = LongArray(buckets)
            val bufferInfo = MediaCodec.BufferInfo()
            var scratch = ShortArray(0)

            // Pull the decoded PCM out of the (slow, per-element) ByteBuffer once into a reused
            // primitive array, then subsample a single channel at ~TARGET_SAMPLES_PER_SECOND. RMS is
            // a statistical measure, so a few hundred samples per second estimate loudness as well as
            // reading every one, at a fraction of the cost. Integer squares keep it float-free; the
            // constant scale cancels in the normalization below.
            fun accumulate(shorts: ShortBuffer, bucket: Int) {
                val n = shorts.remaining()
                if (n <= 0) return
                if (scratch.size < n) scratch = ShortArray(n)
                shorts.get(scratch, 0, n)
                val step = frameStride * channelCount
                var i = 0
                var localSum = 0L
                var localCount = 0
                while (i < n) {
                    val s = scratch[i].toInt()
                    localSum += (s * s).toLong()
                    localCount++
                    i += step
                }
                if (localCount > 0) {
                    sums[bucket] += localSum.toDouble()
                    counts[bucket] += localCount
                }
            }

            // The decoder reports its real output PCM layout via INFO_OUTPUT_FORMAT_CHANGED; refresh
            // the subsample stride from it.
            fun updateFormat(outFormat: MediaFormat) {
                if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                    channelCount = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    frameStride = frameStrideFor(sampleRate)
                }
            }

            // Decode the stream linearly, bucketing each output buffer by its presentation time.
            // Seek-per-bucket sampling was tried and measured ~3.7x slower on real hardware (the
            // codec flush + re-prime cost per seek dwarfs the saved decode), and the flush transients
            // corrupted the per-bucket RMS, so a straight decode is both faster and more accurate.
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inIndex)
                        val sampleSize = if (inputBuffer != null)
                            extractor.readSampleData(inputBuffer, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inIndex, 0, sampleSize, extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val bucket = if (durationUs > 0)
                            min(buckets - 1, ((bufferInfo.presentationTimeUs * buckets) / durationUs).toInt())
                        else 0
                        if (bucket in 0 until buckets) {
                            accumulate(
                                outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer(),
                                bucket
                            )
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    updateFormat(codec.outputFormat)
                }
            }

            // RMS per bucket then normalize to the loudest bucket. Sums are in raw PCM units; the
            // constant scale cancels here.
            val result = FloatArray(buckets)
            var maxVal = 0f
            for (i in 0 until buckets) {
                val v = if (counts[i] > 0) sqrt(sums[i] / counts[i]).toFloat() else 0f
                result[i] = v
                if (v > maxVal) maxVal = v
            }
            if (maxVal > 0f) {
                for (i in 0 until buckets) result[i] = (result[i] / maxVal).coerceIn(0f, 1f)
            }
            Log.d(TAG, "Extracted waveform from ${file.name} (${durationUs / 1000000} s) in ${System.currentTimeMillis() - timeStart} ms")
            return result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract waveform from ${file.name}", e)
            return FloatArray(0)
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    /**
     * PCM frames to skip between examined samples so we inspect roughly
     * [TARGET_SAMPLES_PER_SECOND] per second regardless of the source sample rate.
     */
    private fun frameStrideFor(sampleRate: Int): Int =
        if (sampleRate <= 0) 1 else max(1, sampleRate / TARGET_SAMPLES_PER_SECOND)

    // ---- On-disk cache -------------------------------------------------------------------------

    private fun cacheFileFor(cacheDir: File, cacheKey: String): File {
        val safeName = cacheKey.replace(UNSAFE_NAME_CHARS, "_")
        return File(File(cacheDir, CACHE_DIR_NAME), "$safeName.wf")
    }

    private fun readCache(cacheDir: File, cacheKey: String, buckets: Int): FloatArray? {
        val f = cacheFileFor(cacheDir, cacheKey)
        if (!f.exists()) return null
        return try {
            DataInputStream(f.inputStream().buffered()).use { input ->
                // Ignore stale entries written by an older format or a different bucket count.
                if (input.readInt() != CACHE_MAGIC) return null
                val n = input.readInt()
                if (n != buckets) return null
                FloatArray(n) { input.readFloat() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read waveform cache for $cacheKey", e)
            null
        }
    }

    private fun writeCache(cacheDir: File, cacheKey: String, data: FloatArray) {
        val target = cacheFileFor(cacheDir, cacheKey)
        try {
            val dir = target.parentFile ?: return
            dir.mkdirs()
            // Write to a unique temp file then rename, so a reader never sees a partial file even if
            // two extractions for the same key race.
            val tmp = File.createTempFile("wfm", ".tmp", dir)
            DataOutputStream(tmp.outputStream().buffered()).use { out ->
                out.writeInt(CACHE_MAGIC)
                out.writeInt(data.size)
                for (v in data) out.writeFloat(v)
            }
            if (!tmp.renameTo(target)) tmp.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write waveform cache for $cacheKey", e)
        }
    }

    private const val TIMEOUT_US = 10000L

    /** Subsample density used to estimate per-bucket loudness (samples per second of audio). */
    private const val TARGET_SAMPLES_PER_SECOND = 1000

    private const val CACHE_MAGIC = 0x57465631 // "WFV1"
    private const val CACHE_DIR_NAME = "waveform_cache"
    private val UNSAFE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")
}
