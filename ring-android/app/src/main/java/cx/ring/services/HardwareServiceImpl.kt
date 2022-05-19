/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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

import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.WindowManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import cx.ring.services.CameraService.CameraListener
import cx.ring.utils.BluetoothWrapper
import cx.ring.utils.BluetoothWrapper.BluetoothChangeListener
import cx.ring.utils.Ringer
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import net.jami.daemon.IntVect
import net.jami.daemon.JamiService
import net.jami.daemon.UintVect
import net.jami.model.Call.CallStatus
import net.jami.model.Conference
import net.jami.services.HardwareService
import net.jami.services.PreferencesService
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ScheduledExecutorService

class HardwareServiceImpl(
    private val mContext: Context,
    executor: ScheduledExecutorService,
    preferenceService: PreferencesService,
    uiScheduler: Scheduler
) : HardwareService(executor, preferenceService, uiScheduler), OnAudioFocusChangeListener, BluetoothChangeListener {
    private val videoInputs: MutableMap<String, Shm> = HashMap()
    private val cameraService = CameraService(mContext)
    private val mRinger = Ringer(mContext)
    private val mAudioManager: AudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mBluetoothWrapper: BluetoothWrapper? = null
    private var currentFocus: AudioFocusRequestCompat? = null
    private var pendingScreenSharingSession: MediaProjection? = null
    private val shouldCapture = HashSet<String>()
    private var mShouldSpeakerphone = false
    private val mHasSpeakerPhone: Boolean = hasSpeakerphone()
    private var mIsChoosePlugin = false
    private var mMediaHandlerId: String? = null
    private var mPluginCallId: String? = null

    override fun initVideo(): Completable {
        Log.i(TAG, "initVideo()")
        return cameraService.init()
    }

    override val maxResolutions: Observable<Pair<Int?, Int?>>
        get() = cameraService.maxResolutions
    override val isVideoAvailable: Boolean
        get() = mContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) || cameraService.hasCamera()

    override fun hasMicrophone(): Boolean {
        val pm = mContext.packageManager
        var hasMicrophone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        if (!hasMicrophone) {
            val recorder = MediaRecorder()
            val testFile = File(mContext.cacheDir, "MediaUtil#micAvailTestFile")
            hasMicrophone = try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                recorder.setOutputFile(testFile.absolutePath)
                recorder.prepare()
                recorder.start()
                true
            } catch (e: IllegalStateException) {
                // Microphone is already in use
                true
            } catch (exception: Exception) {
                false
            } finally {
                recorder.release()
                testFile.delete()
            }
        }
        return hasMicrophone
    }

    override val isSpeakerphoneOn: Boolean
        get() = mAudioManager.isSpeakerphoneOn

    private val RINGTONE_REQUEST = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_NOTIFICATION_RINGTONE)
            .setLegacyStreamType(AudioManager.STREAM_RING)
            .build())
        .setOnAudioFocusChangeListener(this)
        .build()
    private val CALL_REQUEST = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributesCompat.USAGE_VOICE_COMMUNICATION)
            .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
            .build())
        .setOnAudioFocusChangeListener(this)
        .build()

    private fun getFocus(request: AudioFocusRequestCompat?) {
        if (currentFocus === request) return
        currentFocus?.let { focus ->
            AudioManagerCompat.abandonAudioFocusRequest(mAudioManager, focus)
            currentFocus = null
        }
        if (request != null && AudioManagerCompat.requestAudioFocus(mAudioManager, request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentFocus = request
        }
    }

    @Synchronized
    override fun updateAudioState(state: CallStatus, incomingCall: Boolean, isOngoingVideo: Boolean, isSpeakerOn: Boolean) {
        Log.d(TAG, "updateAudioState: Call state updated to $state Call is incoming: $incomingCall Call is video: $isOngoingVideo")
        val callEnded = state == CallStatus.HUNGUP || state == CallStatus.FAILURE || state == CallStatus.OVER
        try {
            if (mBluetoothWrapper == null && !callEnded) {
                mBluetoothWrapper = BluetoothWrapper(mContext, this)
            }
            when (state) {
                CallStatus.RINGING -> {
                    if (incomingCall) startRinging()
                    getFocus(RINGTONE_REQUEST)
                    if (incomingCall) {
                        // ringtone for incoming calls
                        mAudioManager.mode = AudioManager.MODE_RINGTONE
                        setAudioRouting(true)
                        //mShouldSpeakerphone = isOngoingVideo
                    } else setAudioRouting(isOngoingVideo)
                }
                CallStatus.CURRENT -> {
                    stopRinging()
                    getFocus(CALL_REQUEST)
                    mAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    setAudioRouting(isSpeakerOn)
                }
                CallStatus.HOLD, CallStatus.UNHOLD, CallStatus.INACTIVE -> {
                }
                else -> if (callEnded) closeAudioState()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating audio state", e)
        }
    }

    /*
    This is required in the case where a call is incoming. If you have an incoming call, and no bluetooth device is connected, the ringer should always be played through the speaker.
    However, this results in the call starting in a state where the speaker is always on and the UI is in an incorrect state.
    If it is a bluetooth device, it takes priority and does not play on speaker regardless. Otherwise, it returns mShouldSpeakerphone which was updated in updateaudiostate.
     */
    override fun shouldPlaySpeaker(): Boolean {
        val bt = mBluetoothWrapper
        return if (bt != null && bt.canBluetooth() && bt.isBTHeadsetConnected) false else mShouldSpeakerphone
    }

    @Synchronized
    override fun closeAudioState() {
        stopRinging()
        abandonAudioFocus()
    }

    override fun startRinging() {
        mRinger.ring()
    }

    override fun stopRinging() {
        mRinger.stopRing()
    }

    override fun onAudioFocusChange(arg0: Int) {
        Log.i(TAG, "onAudioFocusChange $arg0")
    }

    @Synchronized
    override fun abandonAudioFocus() {
        if (currentFocus != null) {
            AudioManagerCompat.abandonAudioFocusRequest(mAudioManager, currentFocus!!)
            currentFocus = null
        }
        if (mAudioManager.isSpeakerphoneOn) {
            mAudioManager.isSpeakerphoneOn = false
        }
        mAudioManager.mode = AudioManager.MODE_NORMAL
        mBluetoothWrapper?.let { bluetoothWrapper ->
            bluetoothWrapper.unregister()
            bluetoothWrapper.setBluetoothOn(false)
            mBluetoothWrapper = null
        }
    }

    private fun setAudioRouting(requestSpeakerOn: Boolean) {
        // prioritize bluetooth by checking for bluetooth device first
        val bt = mBluetoothWrapper
        if (bt != null && bt.canBluetooth() && bt.isBTHeadsetConnected) {
            routeToBTHeadset()
        } else if (!mAudioManager.isWiredHeadsetOn && mHasSpeakerPhone && requestSpeakerOn) {
            routeToSpeaker()
        } else {
            resetAudio()
        }
    }

    private fun hasSpeakerphone(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check FEATURE_AUDIO_OUTPUT to guard against false positives.
            val packageManager = mContext.packageManager
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
                return false
            }
            val devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true
                }
            }
            return false
        }
        return true
    }

    /**
     * Routes audio to a bluetooth headset.
     */
    private fun routeToBTHeadset() {
        Log.d(TAG, "routeToBTHeadset: Try to enable bluetooth")
        val oldMode = mAudioManager.mode
        mAudioManager.mode = AudioManager.MODE_NORMAL
        mAudioManager.isSpeakerphoneOn = false
        mBluetoothWrapper!!.setBluetoothOn(true)
        mAudioManager.mode = oldMode
        audioStateSubject.onNext(AudioState(AudioOutput.BLUETOOTH, mBluetoothWrapper!!.deviceName))
    }

    /**
     * Routes audio to the device's speaker and takes into account whether the transition is coming from bluetooth.
     */
    private fun routeToSpeaker() {
        // if we are returning from bluetooth mode, switch to mode normal, otherwise, we switch to mode in communication
        if (mAudioManager.isBluetoothScoOn) {
            val oldMode = mAudioManager.mode
            mAudioManager.mode = AudioManager.MODE_NORMAL
            mBluetoothWrapper!!.setBluetoothOn(false)
            mAudioManager.mode = oldMode
        }
        mAudioManager.isSpeakerphoneOn = true
        audioStateSubject.onNext(STATE_SPEAKERS)
    }

    /**
     * Returns to earpiece audio
     */
    private fun resetAudio() {
        if (mBluetoothWrapper != null) mBluetoothWrapper!!.setBluetoothOn(false)
        mAudioManager.isSpeakerphoneOn = false
        audioStateSubject.onNext(STATE_INTERNAL)
    }

    @Synchronized
    override fun toggleSpeakerphone(checked: Boolean) {
        JamiService.setAudioPlugin(JamiService.getCurrentAudioOutputPlugin())
        mShouldSpeakerphone = checked

        if (mHasSpeakerPhone && checked) {
            routeToSpeaker()
        } else if (mBluetoothWrapper != null && mBluetoothWrapper!!.canBluetooth() && mBluetoothWrapper!!.isBTHeadsetConnected) {
            routeToBTHeadset()
        } else {
            resetAudio()
        }
    }

    @Synchronized
    override fun onBluetoothStateChanged(status: Int) {
        Log.d(TAG, "bluetoothStateChanged to: $status")
        val event = BluetoothEvent(status == BluetoothHeadset.STATE_AUDIO_CONNECTED)
        if (status == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            Log.d(TAG, "BluetoothHeadset Connected")
        } else if (status == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
            Log.d(TAG, "BluetoothHeadset Disconnected")
            if (mShouldSpeakerphone) routeToSpeaker()
        }
        bluetoothEvents.onNext(event)
    }

    @Synchronized override fun decodingStarted(id: String, shmPath: String, width: Int, height: Int, isMixer: Boolean) {
        Log.i(TAG, "decodingStarted() " + id + " " + width + "x" + height)
        val shm = Shm(id, width, height)
        videoInputs[id] = shm
        videoEvents.onNext(VideoEvent(id, start = true, w = shm.w, h = shm.h))
        videoSurfaces[id]?.get()?.let { holder ->
            shm.window = startVideo(id, holder.surface, width, height)
            if (shm.window == 0L) {
                Log.w(TAG, "decodingStarted() no window !")
            } else {
                videoEvents.onNext(VideoEvent(shm.id, start = true, started = true, w = shm.w, h = shm.h))
            }
        }
    }

    @Synchronized override fun decodingStopped(id: String, shmPath: String, isMixer: Boolean) {
        Log.i(TAG, "decodingStopped() $id")
        videoEvents.onNext(VideoEvent(id, started = true))
        val shm = videoInputs.remove(id) ?: return
        if (shm.window != 0L) {
            try {
                stopVideo(shm.id, shm.window)
            } catch (e: Exception) {
                Log.e(TAG, "decodingStopped error$e")
            }
            shm.window = 0
            videoEvents.onNext(VideoEvent(id, started = false))
        }
    }

    override fun connectSink(id: String, windowId: Long): Observable<Pair<Int, Int>> {
        var registered = JamiService.registerVideoCallback(id, windowId)
        Log.w(TAG, "init registerVideoCallback success:$registered: $id $windowId")
        val ret = videoEvents.filter { e -> e.sinkId == id }
            .doOnDispose {
                Log.w(TAG, "doOnDispose unregisterVideoCallback: $id $windowId")
                JamiService.unregisterVideoCallback(id, windowId)
            }
            .map { c ->
                if (!registered && c.start && !c.started) {
                    Log.w(TAG, "registerVideoCallback: $id $windowId")
                    if (JamiService.registerVideoCallback(id, windowId))
                        registered = true
                } else if (!c.start && c.started) {
                    registered = false
                }
                Pair(c.w, c.h)
            }
        val input = videoInputs[id]
        return if (input != null) ret.startWithItem(Pair(input.w, input.h)) else ret
    }

    @Synchronized override fun getSinkSize(id: String): Single<Pair<Int, Int>> {
        val p = videoInputs[id]?.let { input -> Pair(input.w, input.h) }
        return if (p != null) Single.just(p)
        else videoEvents.filter { it.sinkId == id }.firstOrError().map { e -> Pair(e.w, e.h) }
    }

    override fun hasInput(id: String): Boolean {
        return videoInputs[id] !== null
    }

    override fun getCameraInfo(camId: String, formats: IntVect, sizes: UintVect, rates: UintVect) {
        // Use a larger resolution for Android 6.0+, 64 bits devices
        val useLargerSize =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() || mPreferenceService.isHardwareAccelerationEnabled)
        //int MIN_WIDTH = useLargerSize ? (useHD ? VIDEO_WIDTH_HD : VIDEO_WIDTH) : VIDEO_WIDTH_MIN;
        val minVideoSize: Size = if (useLargerSize) parseResolution(mPreferenceService.resolution) else VIDEO_SIZE_LOW
        cameraService.getCameraInfo(camId, formats, sizes, rates, minVideoSize, mContext)
    }

    private fun parseResolution(resolution: Int): Size {
        return when (resolution) {
            320 -> VIDEO_SIZE_LOW
            480 -> VIDEO_SIZE_SD
            720 -> VIDEO_SIZE_HD
            1080 -> VIDEO_SIZE_FULL_HD
            2160 -> VIDEO_SIZE_ULTRA_HD
            else -> VIDEO_SIZE_HD
        }
    }

    override fun setParameters(camId: String, format: Int, width: Int, height: Int, rate: Int) {
        Log.d(TAG, "setParameters: $camId, $format, $width, $height, $rate")
        val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        cameraService.setParameters(camId, format, width, height, rate, windowManager.defaultDisplay.rotation)
    }

    override fun startMediaHandler(mediaHandlerId: String?) {
        mIsChoosePlugin = true
        mMediaHandlerId = mediaHandlerId
    }

    private fun toggleMediaHandler(callId: String) {
        if (mMediaHandlerId != null) JamiService.toggleCallMediaHandler(mMediaHandlerId, callId, true)
    }

    override fun stopMediaHandler() {
        mIsChoosePlugin = false
        mMediaHandlerId = null
    }

    override fun startCapture(camId: String?) {
        val cam = camId ?: cameraService.switchInput(true) ?: return
        Log.i(TAG, "startCapture > camId: $camId, cam: $cam, mIsChoosePlugin: $mIsChoosePlugin")
        shouldCapture.add(cam)
        val videoParams = cameraService.getParams(cam) ?: return
        if (videoParams.isCapturing) return

        val surface = mCameraPreviewSurface.get()
        if (surface == null) {
            Log.e(TAG, "Can't start capture: no surface registered.")
            //cameraService.setPreviewParams(videoParams)
            cameraEvents.onNext(VideoEvent(cam, start = true))
            return
        }
        val conf = mCameraPreviewCall.get()
        val useHardwareCodec =
            mPreferenceService.isHardwareAccelerationEnabled && (conf == null || !conf.isConference) && !mIsChoosePlugin
        if (conf != null && useHardwareCodec) {
            val call = conf.call
            if (call != null) {
                call.setDetails(JamiService.getCallDetails(call.account!!, call.daemonIdString!!).toNative())
                videoParams.codec = call.videoCodec
            } else {
                videoParams.codec = null
            }
        }
        Log.w(TAG, "startCapture: id:$cam codec:${videoParams.codec} size:${videoParams.size} rot${videoParams.rotation} hw:$useHardwareCodec bitrate:${mPreferenceService.bitrate}")
        videoParams.isCapturing = true

        if (videoParams.id == CameraService.VideoDevices.SCREEN_SHARING) {
            val projection = pendingScreenSharingSession ?: return
            pendingScreenSharingSession = null
            if (!cameraService.startScreenSharing(videoParams, projection, surface, mContext.resources.displayMetrics)) {
                projection.stop()
            }
            return
        }

        mUiScheduler.scheduleDirect {
            cameraService.openCamera(videoParams, surface,
                object : CameraListener {
                    override fun onOpened() {
                        val currentCall = conf?.id ?: return
                        if (mPluginCallId != null && mPluginCallId != currentCall) {
                            if (mMediaHandlerId != null) {
                                JamiService.toggleCallMediaHandler(mMediaHandlerId, currentCall,false)
                            }
                            mIsChoosePlugin = false
                            mMediaHandlerId = null
                            mPluginCallId = null
                        } else if (mIsChoosePlugin && mMediaHandlerId != null) {
                            mPluginCallId = currentCall
                            toggleMediaHandler(currentCall)
                        }
                    }
                    override fun onError() {
                        stopCapture(videoParams.id)
                    }
                },
                useHardwareCodec,
                mPreferenceService.resolution,
                mPreferenceService.bitrate
            )
        }

        cameraEvents.onNext(VideoEvent(cam,
            started = true,
            w = videoParams.size.width,
            h = videoParams.size.height,
            rot = videoParams.rotation
        ))
    }

    override fun stopCapture(camId: String) {
        shouldCapture.remove(camId)
        cameraService.closeCamera(camId)
        cameraEvents.onNext(VideoEvent(camId, started = false))
    }

    override fun requestKeyFrame(camId: String) {
        cameraService.requestKeyFrame(camId)
    }

    override fun setBitrate(camId: String, bitrate: Int) {
        cameraService.setBitrate(camId, bitrate)
    }

    @Synchronized override fun addVideoSurface(id: String, holder: Any) {
        Log.w(TAG, " addVideoSurface $id $holder")
        if (holder !is SurfaceHolder) return
        val shm = videoInputs[id]
        val surfaceHolder = WeakReference(holder)
        videoSurfaces[id] = surfaceHolder
        if (shm != null && shm.window == 0L) {
            shm.window = startVideo(shm.id, holder.surface, shm.w, shm.h)
        }
        if (shm == null || shm.window == 0L) {
            Log.i(TAG, "JamiService.addVideoSurface() no window !")
            //videoEvents.onNext(VideoEvent(id, start = true))
        } else {
            videoEvents.onNext(VideoEvent(shm.id, start = true, started = true, w = shm.w, h = shm.h))
        }
    }

    @Synchronized override fun updateVideoSurfaceId(currentId: String, newId: String) {
        Log.w(TAG, "updateVideoSurfaceId $currentId $newId")
        val surfaceHolder = videoSurfaces[currentId] ?: return
        val surface = surfaceHolder.get()
        val shm = videoInputs[currentId]
        if (shm != null && shm.window != 0L) {
            try {
                stopVideo(shm.id, shm.window)
            } catch (e: Exception) {
                Log.e(TAG, "removeVideoSurface error$e")
            }
            shm.window = 0
        }
        videoSurfaces.remove(currentId)
        surface?.let { addVideoSurface(newId, it) }
    }

    @Synchronized override fun addPreviewVideoSurface(holder: Any, conference: Conference?) {
        Log.w(TAG, "addPreviewVideoSurface > holder:$holder")
        if (holder !is TextureView) return
        if (mCameraPreviewSurface.get() === holder) return
        mCameraPreviewSurface = WeakReference(holder)
        mCameraPreviewCall = WeakReference(conference)
        for (c in shouldCapture) startCapture(c)
    }

    @Synchronized override fun updatePreviewVideoSurface(conference: Conference) {
        val old = mCameraPreviewCall.get()
        mCameraPreviewCall = WeakReference(conference)
        if (old !== conference) {
            for (camId in shouldCapture) {
                cameraService.closeCamera(camId)
                startCapture(camId)
            }
        }
    }

    @Synchronized override fun removeVideoSurface(id: String) {
        videoSurfaces.remove(id)
        val shm = videoInputs[id] ?: return
        if (shm.window != 0L) {
            try {
                stopVideo(shm.id, shm.window)
            } catch (e: Exception) {
                Log.e(TAG, "removeVideoSurface error$e")
            }
            shm.window = 0
        }
        //videoEvents.onNext(VideoEvent(shm.id, started = false))
    }

    override fun removePreviewVideoSurface() {
        mCameraPreviewSurface.clear()
    }

    override fun switchInput(accountId:String, callId: String, setDefaultCamera: Boolean, screenCaptureSession: Any?) {
        val camId = if (screenCaptureSession != null) {
            pendingScreenSharingSession = screenCaptureSession as MediaProjection
            CameraService.VideoDevices.SCREEN_SHARING
        } else {
            pendingScreenSharingSession = null
            cameraService.switchInput(setDefaultCamera)
        }
        switchInput(accountId, callId, "camera://$camId")
    }

    override fun setPreviewSettings() {
        setPreviewSettings(cameraService.previewSettings)
    }

    override val cameraCount: Int
        get() = cameraService.cameraCount

    override fun hasCamera(): Boolean {
        return cameraService.hasCamera()
    }

    override val isPreviewFromFrontCamera: Boolean
        get() = cameraService.isPreviewFromFrontCamera

    override fun setDeviceOrientation(rotation: Int) {
        cameraService.setOrientation(rotation)
        /*mCapturingId?.let { id ->
            val videoParams = cameraService.getParams(id) ?: return
            videoEvents.onNext(VideoEvent(
                started = true,
                w = videoParams.width,
                h = videoParams.height,
                rot = videoParams.rotation
            ))
        }*/
    }

    override val videoDevices: List<String>
        get() = cameraService.cameraIds

    private data class Shm (val id: String, val w: Int, val h: Int) {
        var window: Long = 0
    }

    override fun unregisterCameraDetectionCallback() {
        cameraService.unregisterCameraDetectionCallback()
    }

    companion object {
        private val VIDEO_SIZE_LOW = Size(320, 240)
        private val VIDEO_SIZE_SD = Size(720, 480)
        private val VIDEO_SIZE_HD = Size(1280, 720)
        private val VIDEO_SIZE_FULL_HD = Size(1920, 1080)
        private val VIDEO_SIZE_QUAD_HD = Size(2560, 1440)
        private val VIDEO_SIZE_ULTRA_HD = Size(3840, 2160)
        val VIDEO_SIZE_DEFAULT = VIDEO_SIZE_SD
        val resolutions = listOf(VIDEO_SIZE_ULTRA_HD, VIDEO_SIZE_QUAD_HD, VIDEO_SIZE_FULL_HD, VIDEO_SIZE_HD, VIDEO_SIZE_SD, VIDEO_SIZE_LOW)
        private val TAG = HardwareServiceImpl::class.simpleName!!
        private var mCameraPreviewSurface = WeakReference<TextureView>(null)
        private var mCameraPluginPreviewSurface = WeakReference<SurfaceView>(null)
        private var mCameraPreviewCall = WeakReference<Conference>(null)
        private val videoSurfaces = HashMap<String, WeakReference<SurfaceHolder>>()
    }
}