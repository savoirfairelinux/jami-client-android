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
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Decodes an audio file into a small set of normalized amplitudes (each in 0..1) suitable for
 * rendering a waveform. Decoding happens off the main thread; results can be cached by callers.
 */
object AudioWaveformExtractor {

    private val TAG = AudioWaveformExtractor::class.simpleName

    /** Number of amplitude buckets produced. The view downsamples further to fit its width. */
    const val DEFAULT_BUCKETS = 192

    /** Asynchronously extract a waveform from [file]. Emits an empty array if decoding fails. */
    fun extract(file: File, buckets: Int = DEFAULT_BUCKETS): Single<FloatArray> =
        Single.fromCallable { compute(file, buckets) }
            .subscribeOn(Schedulers.computation())

    private fun compute(file: File, buckets: Int): FloatArray {
        if (!file.exists() || file.length() == 0L) return FloatArray(0)
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

            val sums = DoubleArray(buckets)
            val counts = LongArray(buckets)
            val bufferInfo = MediaCodec.BufferInfo()
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
                        val shorts = outputBuffer
                            .order(ByteOrder.nativeOrder())
                            .asShortBuffer()
                        var localSum = 0.0
                        var localCount = 0
                        while (shorts.hasRemaining()) {
                            val s = shorts.get().toDouble() / Short.MAX_VALUE
                            localSum += s * s
                            localCount++
                        }
                        if (localCount > 0 && bucket in 0 until buckets) {
                            sums[bucket] += localSum
                            counts[bucket] += localCount
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }

            // RMS per bucket then normalize to the loudest bucket.
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

    private const val TIMEOUT_US = 10000L
}
