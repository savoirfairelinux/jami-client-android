/*
 *  Copyright (C) 2018-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraManager.AvailabilityCallback
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaCodec.CodecException
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaCodecInfo.EncoderCapabilities
import android.media.projection.MediaProjection
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.annotation.RequiresApi
import cx.ring.views.AutoFitTextureView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.IntVect
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.daemon.UintVect
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class CameraService internal constructor(c: Context) {
    private val manager = c.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
    private val mParams = HashMap<String, VideoParams>()
    private val mNativeParams: MutableMap<String, DeviceParams> = HashMap()
    private val t = HandlerThread("videoHandler")
    private val videoLooper: Looper
        get() = t.apply { if (state == Thread.State.NEW) start() }.looper
    private val videoHandler: Handler by lazy { Handler(videoLooper) }

    // SPS and PPS NALs (Config Data).
    private var codecData: ByteBuffer? = null
    private val maxResolutionSubject: Subject<Pair<Int?, Int?>> = BehaviorSubject.createDefault(RESOLUTION_NONE)
    private var devices: VideoDevices? = null
    private val availabilityCallback: AvailabilityCallback = object : AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            Log.w(TAG, "onCameraAvailable $cameraId")
            filterCompatibleCamera(
                arrayOf(cameraId),
                manager!!
            ).forEach { camera: Pair<String, CameraCharacteristics> ->
                val devices = devices ?: return
                synchronized(addedDevices) {
                    if (!devices.cameras.contains(camera.first)) {
                        when (camera.second.get(CameraCharacteristics.LENS_FACING)) {
                            CameraCharacteristics.LENS_FACING_FRONT -> if (devices.cameraFront == null) {
                                devices.addCamera(camera.first)
                                devices.cameraFront = camera.first
                            }
                            CameraCharacteristics.LENS_FACING_BACK -> if (devices.cameraBack == null) {
                                devices.addCamera(camera.first)
                                devices.cameraBack = camera.first
                            }
                            else -> devices.addCamera(camera.first)
                        }
                    }
                }
            }
        }

        override fun onCameraUnavailable(cameraId: String) {
            if (devices == null || devices!!.currentId == null || devices!!.currentId != cameraId) {
                synchronized(addedDevices) {
                    val devices = devices ?: return
                    devices.cameras.remove(cameraId)
                    if (devices.cameraFront == cameraId)
                        devices.cameraFront = null
                    if (devices.cameraBack == cameraId)
                        devices.cameraBack = null
                    if (addedDevices.remove(cameraId)) {
                        Log.w(TAG, "onCameraUnavailable $cameraId")
                        JamiService.removeVideoDevice(cameraId)
                    }
                }
            }
        }
    }
    val maxResolutions: Observable<Pair<Int?, Int?>>
        get() = maxResolutionSubject

    class VideoDevices {
        val cameras: MutableList<String> = ArrayList()
        var currentId: String? = null
        var currentIndex = 0
        var cameraFront: String? = null
        var cameraBack: String? = null
        fun switchInput(setDefaultCamera: Boolean): String? {
            if (setDefaultCamera && cameras.isNotEmpty()) {
                currentId = cameras[0]
            } else if (cameras.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % cameras.size
                currentId = cameras[currentIndex]
            } else {
                currentId = null
            }
            return currentId
        }

        fun addCamera(cameraId: String) {
            cameras.add(cameraId)
            if (addedDevices.add(cameraId)) {
                JamiService.addVideoDevice(cameraId)
            }
        }

        companion object {
            const val SCREEN_SHARING = "desktop"
        }
    }

    fun switchInput(setDefaultCamera: Boolean): String? {
        return if (devices == null) null else devices!!.switchInput(setDefaultCamera)
    }

    fun getParams(camId: String?): VideoParams? {
        Log.w(TAG, "getParams() $camId")
        if (camId != null) {
            return mParams[camId]
        } else if (!devices?.cameras.isNullOrEmpty()) {
            Log.w(TAG, "getParams() fallback")
            devices!!.currentId = devices!!.cameras[0]
            return mParams[devices!!.currentId]
        }
        return null
    }

    fun setParameters(camId: String, format: Int, width: Int, height: Int, rate: Int, rotation: Int) {
        Log.w(TAG, "setParameters() $camId $format $width $height $rate $rotation")
        val deviceParams = mNativeParams[camId]
        if (deviceParams == null) {
            Log.w(TAG, "setParameters() can't find device")
            return
        }
        var params = mParams[camId]
        if (params == null) {
            params = VideoParams(camId, deviceParams.size.x, deviceParams.size.y, rate)
            mParams[camId] = params
        } else {
            params.width = deviceParams.size.x
            params.height = deviceParams.size.y
            params.rate = rate
        }
        params.rotation = getCameraDisplayRotation(deviceParams, rotation)
        val r = params.rotation
        videoHandler.post { JamiService.setDeviceOrientation(camId, r) }
    }

    fun setOrientation(rotation: Int) {
        Log.w(TAG, "setOrientation() $rotation")
        for (id in cameraIds) setDeviceOrientation(id, rotation)
    }

    private fun setDeviceOrientation(camId: String, screenRotation: Int) {
        Log.w(TAG, "setDeviceOrientation() $camId $screenRotation")
        val deviceParams = mNativeParams[camId]
        var rotation = 0
        if (deviceParams != null) {
            rotation = getCameraDisplayRotation(deviceParams, screenRotation)
        }
        mParams[camId]?.rotation = rotation
        JamiService.setDeviceOrientation(camId, rotation)
    }

    fun getCameraInfo(
        camId: String,
        formats: IntVect?,
        sizes: UintVect,
        rates: UintVect,
        minVideoSize: Point,
        context: Context
    ) {
        Log.d(TAG, "getCameraInfo: $camId min size: $minVideoSize")
        val p = DeviceParams()
        rates.clear()
        fillCameraInfo(p, camId, formats, sizes, rates, minVideoSize, context)
        sizes.add(p.size.x.toLong())
        sizes.add(p.size.y.toLong())
        mNativeParams[camId] = p
    }

    private val maxResolution: Point?
        get() {
            var max: Point? = null
            for (deviceParams in mNativeParams.values) {
                if (max == null || max.x * max.y < deviceParams.maxSize.x * deviceParams.maxSize.y)
                    max = deviceParams.maxSize
            }
            return max
        }
    val isPreviewFromFrontCamera: Boolean
        get() = mNativeParams.size == 1 || devices != null && devices!!.currentId != null && devices!!.currentId == devices!!.cameraFront
    val previewSettings: Map<String, StringMap>
        get() {
            val camSettings: MutableMap<String, StringMap> = HashMap()
            for (id in cameraIds) {
                mNativeParams[id]?.let { params -> camSettings[id] = params.toMap() }
            }
            return camSettings
        }

    fun hasCamera(): Boolean {
        return cameraCount > 0
    }

    data class VideoParams(val id: String, var width: Int, var height: Int, var rate: Int) {
        val inputUri: String = "camera://$id"

        //public int format;
        // size as captured by Android
        var rotation = 0
        var codec: String? = null
        var isCapturing: Boolean = false
        var camera: CameraDevice? = null
        var projection: MediaProjection? = null
        var display: VirtualDisplay? = null
        var mediaCodec: MediaCodec? = null
        var codecStarted: Boolean = false

        fun getAndroidCodec(): String {
            return when (val codec = codec) {
                "H264" -> MediaFormat.MIMETYPE_VIDEO_AVC
                "H265" -> MediaFormat.MIMETYPE_VIDEO_HEVC
                "VP8" -> MediaFormat.MIMETYPE_VIDEO_VP8
                "VP9" -> MediaFormat.MIMETYPE_VIDEO_VP9
                "MP4V-ES" -> MediaFormat.MIMETYPE_VIDEO_MPEG4
                null -> MediaFormat.MIMETYPE_VIDEO_AVC
                else -> codec
            }
        }
    }

    class DeviceParams {
        val size: Point = Point(0, 0)
        val maxSize: Point = Point(0, 0)
        var rate: Long = 0
        var facing = 0
        var orientation = 0
        fun toMap(): StringMap {
            val map = StringMap()
            map["size"] = size.x.toString() + "x" + size.y
            map["rate"] = rate.toString()
            return map
        }
    }

    private fun loadDevices(manager: CameraManager): Single<VideoDevices> {
        return Single.fromCallable {
            val devices = VideoDevices()
            val cameras = filterCompatibleCamera(manager.cameraIdList, manager)
            val backCamera = filterCameraIdsFacing(cameras, CameraCharacteristics.LENS_FACING_BACK).firstOrNull()
            val frontCamera = filterCameraIdsFacing(cameras, CameraCharacteristics.LENS_FACING_FRONT).firstOrNull()
            val externalCameras: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                filterCameraIdsFacing(cameras, CameraCharacteristics.LENS_FACING_EXTERNAL)
            } else {
                emptyList()
            }
            if (frontCamera != null) devices.cameras.add(frontCamera)
            if (backCamera != null) devices.cameras.add(backCamera)
            devices.cameras.addAll(externalCameras)
            if (devices.cameras.isNotEmpty()) devices.currentId = devices.cameras[0]
            devices.cameraFront = frontCamera
            devices.cameraBack = backCamera
            Log.w(TAG, "Loading video devices: found " + devices.cameras.size)
            devices
        }.subscribeOn(AndroidSchedulers.from(videoLooper))
    }

    fun init(): Completable {
        val resetCamera = false
        return if (manager == null)
            Completable.error(IllegalStateException("Video manager not available"))
        else loadDevices(manager)
            .map { devs: VideoDevices ->
                synchronized(addedDevices) {
                    val old = devices
                    devices = devs
                    // Removed devices
                    if (old != null) {
                        for (oldId in old.cameras) {
                            if (!devs.cameras.contains(oldId) || resetCamera) {
                                if (addedDevices.remove(oldId)) JamiService.removeVideoDevice(oldId)
                            }
                        }
                    }
                    // Added devices
                    for (camera in devs.cameras) {
                        Log.w(TAG, "JamiServiceJNI.addVideoDevice init $camera")
                        if (addedDevices.add(camera)) JamiService.addVideoDevice(camera)
                    }
                    // Add screen sharing device
                    if (addedDevices.add(VideoDevices.SCREEN_SHARING))
                        JamiService.addVideoDevice(VideoDevices.SCREEN_SHARING)
                    // New default
                    if (devs.currentId != null) {
                        JamiService.setDefaultDevice(devs.currentId)
                    }
                }
                devs
            }
            .ignoreElement()
            .doOnError { e: Throwable ->
                Log.e(TAG, "Error initializing video device", e)
                maxResolutionSubject.onNext(RESOLUTION_NONE)
            }
            .doOnComplete {
                val max = maxResolution
                Log.w(TAG, "Found max resolution: $max")
                maxResolutionSubject.onNext(if (max == null) RESOLUTION_NONE else Pair(max.x, max.y))
                manager.registerAvailabilityCallback(availabilityCallback, videoHandler)
            }
            .onErrorComplete()
    }

    interface CameraListener {
        fun onOpened()
        fun onError()
    }

    fun closeCamera(camId: String) {
        mParams[camId]?.let { params ->
            params.camera?.let { camera ->
                camera.close()
                params.camera = null
            }
            params.projection?.let { mediaProjection ->
                mediaProjection.stop()
                params.projection = null
            }
            params.isCapturing = false
        }
    }

    private fun openEncoder(
        videoParams: VideoParams,
        mimeType: String?,
        handler: Handler,
        resolution: Int,
        bitrate: Int,
        surface: Surface? = null
    ): Pair<MediaCodec?, Surface?> {
        Log.d(TAG, "Video with codec " + mimeType + " resolution: " + videoParams.width + "x" + videoParams.height + " Bitrate: " + bitrate)
        val bitrateValue: Int = if (bitrate == 0) if (resolution >= 720) 192 * 8 * 1024 else 100 * 8 * 1024 else bitrate * 8 * 1024
        val frameRate = videoParams.rate//30 // 30 fps
        val format = MediaFormat.createVideoFormat(mimeType!!, videoParams.width, videoParams.height).apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateValue)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, CodecCapabilities.COLOR_FormatSurface)
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.LOLLIPOP)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate)
            setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
            setInteger(MediaFormat.KEY_FRAME_RATE, 24)
        }
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, -1);
            format.setInteger(MediaFormat.KEY_INTRA_REFRESH_PERIOD, 5);
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        }*/
        //format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.CodecCapabilities.BITRATE_MODE_VBR);
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        //listSupportedCodecs(codecs);
        val codecName = codecs.findEncoderForFormat(format) ?: return Pair(null, null)
        var encoderInput: Surface? = null
        var codec: MediaCodec? = null
        try {
            codec = MediaCodec.createByCodecName(codecName)
            val params = Bundle()
            params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrateValue)
            codec.setParameters(params)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoderInput = /*surface?.also { codec!!.setInputSurface(it) } ?:*/ codec.createInputSurface()
            val callback: MediaCodec.Callback = object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}
                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    try {
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0) {
                            // Get and cache the codec data (SPS/PPS NALs)
                            val isConfigFrame = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            val buffer = codec.getOutputBuffer(index)
                            if (isConfigFrame) {
                                buffer?.let { outputBuffer ->
                                    outputBuffer.position(info.offset)
                                    outputBuffer.limit(info.offset + info.size)
                                    codecData = ByteBuffer.allocateDirect(info.size).apply {
                                        put(outputBuffer)
                                        rewind()
                                    }
                                    Log.i(TAG, "Cache new codec data (SPS/PPS, ...)")
                                }
                            } else {
                                val isKeyFrame = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                                // If it's a key-frame, send the cached SPS/PPS NALs prior to
                                // sending key-frame.
                                if (isKeyFrame) {
                                    codecData?.let { data ->
                                        JamiService.captureVideoPacket(
                                            videoParams.inputUri,
                                            data,
                                            data.capacity(),
                                            0,
                                            false,
                                            info.presentationTimeUs,
                                            videoParams.rotation
                                        )
                                    }
                                }

                                // Send the encoded frame
                                JamiService.captureVideoPacket(
                                    videoParams.inputUri,
                                    buffer,
                                    info.size,
                                    info.offset,
                                    isKeyFrame,
                                    info.presentationTimeUs,
                                    videoParams.rotation
                                )
                            }
                            codec.releaseOutputBuffer(index, false)
                        }
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "MediaCodec can't process buffer", e)
                    }
                }

                override fun onError(codec: MediaCodec, e: CodecException) {
                    Log.e(TAG, "MediaCodec onError", e)
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    Log.e(TAG, "MediaCodec onOutputFormatChanged $format")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setCodecCallback(codec, callback, handler)
            } else {
                setCodecCallback(codec, callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't open codec", e)
            if (codec != null) {
                codec.release()
                codec = null
            }
            if (encoderInput != null) {
                encoderInput.release()
                encoderInput = null
            }
        }
        return Pair(codec, encoderInput)
    }

    fun requestKeyFrame() {
        Log.w(TAG, "requestKeyFrame()")
        try {
            /*if (currentCodec != null) {
                val params = Bundle()
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                currentCodec!!.setParameters(params)
            }*/
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Can't send keyframe request", e)
        }
    }

    fun setBitrate(camId: String, bitrate: Int) {
        Log.w(TAG, "setBitrate() $camId $bitrate")
        try {
            mParams[camId]?.mediaCodec?.setParameters(Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate * 1024)
            })
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Can't set bitrate", e)
        }
    }

    private fun createVirtualDisplay(
        params: VideoParams,
        projection: MediaProjection,
        surface: TextureView,
        metrics: DisplayMetrics
    ): Pair<MediaCodec?, VirtualDisplay>? {
        val screenDensity = metrics.densityDpi
        val handler = videoHandler
        var r: Pair<MediaCodec?, Surface?>? = null
        if (params.rate == 0) {
            params.rate = 24
        }
        while (params.width >= 320) {
            Log.e(TAG, "createVirtualDisplay ${params.width}x${params.height} at ${params.rate}")
            r = openEncoder(
                params,
                MediaFormat.MIMETYPE_VIDEO_AVC,
                handler,
                720,
                0,
                surface = Surface(surface.surfaceTexture)
            )
            if (r.first == null) {
                params.width /= 2
                params.height /= 2
            } else break
        }
        if (r == null) return null
        val surface = r.second
        val codec = r.first
        codec?.start()
        return try {
            Pair(
                codec, projection.createVirtualDisplay(
                    "ScreenSharing", params.width, params.height, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, object : VirtualDisplay.Callback() {
                        override fun onPaused() {
                            Log.w(TAG, "VirtualDisplay.onPaused")
                        }

                        override fun onResumed() {
                            Log.w(TAG, "VirtualDisplay.onResumed")
                        }

                        override fun onStopped() {
                            Log.w(TAG, "VirtualDisplay.onStopped")
                            if (surface != null) {
                                surface.release()
                                codec?.release()
                            }
                        }
                    }, handler
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating virtual display", e)
            if (codec != null) {
                codec.stop()
                codec.release()
            }
            surface?.release()
            null
        }
    }

    fun startScreenSharing(
        params: VideoParams,
        mediaProjection: MediaProjection,
        surface: TextureView,
        metrics: DisplayMetrics
    ): Boolean {
        val r = createVirtualDisplay(params, mediaProjection, surface, metrics)
        if (r != null) {
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    params.mediaCodec?.let { codec ->
                        codec.signalEndOfInputStream()
                        codec.stop()
                        codec.release()
                        params.mediaCodec = null
                    }
                    params.display?.release()
                    params.display = null
                    params.codecStarted = false
                }
            }, videoHandler)
            params.codecStarted = true
            params.mediaCodec = r.first
            params.projection = mediaProjection
            params.display = r.second
            return true
        }
        mediaProjection.stop()
        return false
    }

    fun openCamera(
        videoParams: VideoParams,
        surface: TextureView,
        listener: CameraListener,
        hw_accel: Boolean,
        resolution: Int,
        bitrate: Int
    ) {
        //previewCamera?.close()
        val handler = videoHandler
        try {
            val view = surface as AutoFitTextureView
            val flip = videoParams.rotation % 180 != 0
            val cc = manager!!.getCameraCharacteristics(videoParams.id)
            val fpsRange = chooseOptimalFpsRange(cc.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES))
            val streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSize = chooseOptimalSize(
                streamConfigs?.getOutputSizes(SurfaceHolder::class.java),
                if (flip) view.height else view.width, if (flip) view.width else view.height,
                videoParams.width, videoParams.height,
                Size(videoParams.width, videoParams.height)
            )
            Log.d(TAG, "Selected preview size: " + previewSize + ", fps range: " + fpsRange + " rate: " + videoParams.rate)
            view.setAspectRatio(previewSize.height, previewSize.width)
            val texture = view.surfaceTexture
            val s = Surface(texture)
            val codec = if (hw_accel)
                openEncoder(videoParams, videoParams.getAndroidCodec(), handler, resolution, bitrate)
            else null
            val targets: MutableList<Surface> = ArrayList(2)
            targets.add(s)
            var tmpReader: ImageReader? = null
            if (codec?.second != null) {
                targets.add(codec.second!!)
            } else {
                tmpReader = ImageReader.newInstance(videoParams.width, videoParams.height, ImageFormat.YUV_420_888, 8)
                tmpReader.setOnImageAvailableListener(OnImageAvailableListener { r: ImageReader ->
                    val image = r.acquireLatestImage()
                    if (image != null) {
                        JamiService.captureVideoFrame(videoParams.inputUri, image, videoParams.rotation)
                        image.close()
                    }
                }, handler)
                targets.add(tmpReader.surface)
            }
            val reader = tmpReader
            manager.openCamera(videoParams.id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        Log.w(TAG, "onOpened " + videoParams.id)
                        //previewCamera = camera
                        videoParams.camera = camera
                        videoParams.mediaCodec = codec?.first
                        texture!!.setDefaultBufferSize(previewSize.width, previewSize.height)
                        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        builder.addTarget(s)
                        if (codec?.second != null) {
                            builder.addTarget(codec.second!!)
                        } else if (reader != null) {
                            builder.addTarget(reader.surface)
                        }
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                        val request = builder.build()
                        camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                Log.w(TAG, "onConfigured")
                                listener.onOpened()
                                try {
                                    session.setRepeatingRequest(request,
                                        if (codec?.second != null) object : CaptureCallback() {
                                            override fun onCaptureStarted(
                                                session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                timestamp: Long,
                                                frameNumber: Long
                                            ) {
                                                if (frameNumber == 1L) {
                                                    try {
                                                        videoParams.mediaCodec?.start()
                                                        videoParams.codecStarted = true
                                                    } catch (e: Exception) {
                                                        listener.onError()
                                                    }
                                                }
                                            }
                                        } else null,
                                        handler)
                                } catch (e: Exception) {
                                    Log.w(TAG, "onConfigured error:", e)
                                    camera.close()
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                listener.onError()
                                Log.w(TAG, "onConfigureFailed")
                            }

                            override fun onClosed(session: CameraCaptureSession) {
                                Log.w(TAG, "CameraCaptureSession onClosed")
                            }
                        }, handler)
                    } catch (e: Exception) {
                        Log.w(TAG, "onOpened error:", e)
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "onDisconnected")
                    camera.close()
                    listener.onError()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.w(TAG, "onError: $error")
                    camera.close()
                    listener.onError()
                }

                override fun onClosed(camera: CameraDevice) {
                    Log.w(TAG, "onClosed")
                    try {
                        videoParams.mediaCodec?.let { mediaCodec ->
                            if (videoParams.codecStarted)
                                mediaCodec.signalEndOfInputStream()
                            mediaCodec.release()
                            videoParams.mediaCodec = null
                            videoParams.codecStarted = false
                        }
                        codec?.second?.release()
                        reader?.close()
                        s.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error stopping codec", e)
                    }
                }
            }, handler)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while settings preview parameters", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while settings preview parameters", e)
        }
    }

    val cameraIds: List<String>
        get() = devices?.cameras ?: ArrayList()
    val cameraCount: Int
        get() = try {
            devices?.cameras?.size ?: manager!!.cameraIdList.size
        } catch (e: CameraAccessException) {
            0
        }

    private fun fillCameraInfo(
        p: DeviceParams,
        camId: String,
        formats: IntVect?,
        sizes: UintVect?,
        rates: UintVect,
        minVideoSize: Point,
        context: Context
    ) {
        if (manager == null) return
        try {
            if (camId == VideoDevices.SCREEN_SHARING) {
                val metrics = context.resources.displayMetrics
                var screenWidth = metrics.widthPixels
                var screenHeight = metrics.heightPixels
                while (max(screenWidth, screenHeight) > max(minVideoSize.x, minVideoSize.y)) {
                    screenWidth /= 2
                    screenHeight /= 2
                }
                p.size.x = screenWidth
                p.size.y = screenHeight
                p.rate = 24
                rates.add(p.rate)
                return
            }
            val cc = manager.getCameraCharacteristics(camId)
            val streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
            val rawSizes = streamConfigs.getOutputSizes(ImageFormat.YUV_420_888)
            var newSize = rawSizes[0]
            for (s in rawSizes) {
                if (s.width < s.height) {
                    continue
                }
                if (s.width == minVideoSize.x && s.height == minVideoSize.y // Has height closer but still higher than target
                    || (if (newSize.height < minVideoSize.y) s.height > newSize.height else s.height >= minVideoSize.y && s.height < newSize.height) // Has width closer but still higher than target
                    || if (s.height == newSize.height && newSize.width < minVideoSize.x)
                        s.width > newSize.width
                    else
                        s.width >= minVideoSize.x && s.width < newSize.width
                ) {
                    if (s.width * s.height > p.maxSize.x * p.maxSize.y) {
                        p.maxSize.x = s.width
                        p.maxSize.y = s.height
                    }
                    newSize = s
                }
            }
            p.size.x = newSize.width
            p.size.y = newSize.height
            val minDuration = streamConfigs.getOutputMinFrameDuration(ImageFormat.YUV_420_888, newSize)
            val maxfps = 1000e9 / minDuration
            val fps = maxfps.toLong()
            rates.add(fps)
            p.rate = fps
            p.orientation = cc.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            p.facing = cc.get(CameraCharacteristics.LENS_FACING)!!
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred getting camera info", e)
        }
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }
    }

    fun unregisterCameraDetectionCallback() {
        manager?.unregisterAvailabilityCallback(availabilityCallback)
    }

    companion object {
        private val TAG = CameraService::class.java.simpleName
        private const val FPS_MAX = 30
        private const val FPS_TARGET = 15
        private val addedDevices: MutableSet<String> = HashSet()
        private val RESOLUTION_NONE = Pair<Int?, Int?>(null, null)
        private fun getCameraDisplayRotation(device: DeviceParams, screenRotation: Int): Int {
            return getCameraDisplayRotation(device.orientation, rotationToDegrees(screenRotation), device.facing)
        }

        private fun getCameraDisplayRotation(sensorOrientation: Int, screenOrientation: Int, cameraFacing: Int): Int {
            val rotation = if (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                (sensorOrientation + screenOrientation + 360) % 360
            } else {
                (sensorOrientation - screenOrientation + 360) % 360
            }
            return (180 - rotation + 180) % 360
        }

        private fun filterCompatibleCamera(
            cameras: Array<String>,
            cameraManager: CameraManager
        ): List<Pair<String, CameraCharacteristics>> {
            return cameras.map { id: String -> Pair(id, cameraManager.getCameraCharacteristics(id)) }
                .filter { camera: Pair<String, CameraCharacteristics> ->
                    try {
                        val caps = camera.second.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                        for (c in caps) if (c == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME) return@filter false
                        for (c in caps) if (c == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) return@filter true
                    } catch (e: Exception) {
                        return@filter false
                    }
                    false
                }
        }

        private fun filterCameraIdsFacing(
            cameras: List<Pair<String, CameraCharacteristics>>,
            facing: Int
        ): List<String> {
            return cameras.filter { camera -> camera.second.get(CameraCharacteristics.LENS_FACING) == facing }
                .map { camera -> camera.first }
        }

        private fun listSupportedCodecs(list: MediaCodecList) {
            try {
                for (codecInfo in list.codecInfos) {
                    for (type in codecInfo.supportedTypes) {
                        try {
                            val codecCaps = codecInfo.getCapabilitiesForType(type)
                            val caps = codecCaps.encoderCapabilities ?: continue
                            val videoCaps = codecCaps.videoCapabilities ?: continue
                            Log.w(TAG, "Codec info:" + codecInfo.name + " type: " + type)
                            Log.w(TAG, "Encoder capabilities: complexityRange: " + caps.complexityRange)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                Log.w(TAG, "Encoder capabilities: qualityRange: " + caps.qualityRange)
                            }
                            Log.w(TAG, "Encoder capabilities: VBR: " + caps.isBitrateModeSupported(EncoderCapabilities.BITRATE_MODE_VBR))
                            Log.w(TAG, "Encoder capabilities: CBR: " + caps.isBitrateModeSupported(EncoderCapabilities.BITRATE_MODE_CBR))
                            Log.w(TAG, "Encoder capabilities: CQ: " + caps.isBitrateModeSupported(EncoderCapabilities.BITRATE_MODE_CQ))
                            Log.w(TAG, "Bitrate range: " + videoCaps.bitrateRange)
                            for (format in codecCaps.colorFormats) {
                                Log.w(TAG, "Supported color format: $format")
                            }
                            val widths = videoCaps.supportedWidths
                            val heights = videoCaps.supportedHeights
                            Log.w(TAG, "Supported sizes: " + widths.lower + "x" + heights.lower + " -> " + widths.upper + "x" + heights.upper)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Log.w(TAG, "AchievableFrameRates: " + videoCaps.getAchievableFrameRatesFor(1920, 1080))
                            }
                            Log.w(TAG, "SupportedFrameRates: " + videoCaps.getSupportedFrameRatesFor( /*widths.getUpper(), heights.getUpper()*/1920, 1080))
                            for (profileLevel in codecCaps.profileLevels) Log.w(TAG, "profileLevels: $profileLevel")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Log.w(TAG, "FEATURE_IntraRefresh: " + codecCaps.isFeatureSupported(CodecCapabilities.FEATURE_IntraRefresh))
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Can't query codec info", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Can't query codec info", e)
            }
        }

        private fun setCodecCallback(codec: MediaCodec, callback: MediaCodec.Callback) {
            codec.setCallback(callback)
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private fun setCodecCallback(codec: MediaCodec, callback: MediaCodec.Callback, handler: Handler) {
            codec.setCallback(callback, handler)
        }

        private fun chooseOptimalSize(
            choices: Array<Size>?,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            target: Size
        ): Size {
            if (choices == null) return target
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough: MutableList<Size> = ArrayList()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough: MutableList<Size> = ArrayList()
            val w = target.width
            val h = target.height
            for (option in choices) {
                Log.w(TAG, "supportedSize: $option")
                if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            return when {
                bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
                notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
                else -> {
                    Log.e(TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }

        private fun chooseOptimalFpsRange(ranges: Array<Range<Int>>?): Range<Int> {
            var range: Range<Int>? = null
            if (ranges != null && ranges.isNotEmpty()) {
                for (r in ranges) {
                    if (r.upper > FPS_MAX) continue
                    if (range != null) {
                        val d = abs(r.upper - FPS_TARGET) - abs(range.upper - FPS_TARGET)
                        if (d > 0) continue
                        if (d == 0 && r.lower > range.lower) continue
                    }
                    range = r
                }
                if (range == null) range = ranges[0]
            }
            return range ?: Range(FPS_TARGET, FPS_TARGET)
        }

        private fun rotationToDegrees(rotation: Int): Int {
            when (rotation) {
                Surface.ROTATION_0 -> return 0
                Surface.ROTATION_90 -> return 90
                Surface.ROTATION_180 -> return 180
                Surface.ROTATION_270 -> return 270
            }
            return 0
        }
    }
}