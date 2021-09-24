/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.graphics.Point
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
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
import net.jami.daemon.IntVect
import net.jami.daemon.JamiService
import net.jami.daemon.UintVect
import net.jami.model.Call.CallStatus
import net.jami.model.Conference
import net.jami.services.HardwareService
import net.jami.services.PreferencesService
import net.jami.utils.Tuple
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
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
    private var mCapturingId: String? = null
    private var mIsCapturing = false
    private var mIsScreenSharing = false
    private var mShouldCapture = false
    private var mShouldSpeakerphone = false
    private val mHasSpeakerPhone: Boolean = hasSpeakerphone()
    private var mIsChoosePlugin = false
    private var mMediaHandlerId: String? = null
    private var mPluginCallId: String? = null

    override fun initVideo(): Completable {
        Log.i(TAG, "initVideo()")
        return cameraService.init()
    }

    override val maxResolutions: Observable<Tuple<Int?, Int?>>
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
    override fun updateAudioState(state: CallStatus?, incomingCall: Boolean, isOngoingVideo: Boolean) {
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
                        mShouldSpeakerphone = isOngoingVideo
                    } else setAudioRouting(isOngoingVideo)
                }
                CallStatus.CURRENT -> {
                    stopRinging()
                    getFocus(CALL_REQUEST)
                    mAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    setAudioRouting(isOngoingVideo)
                }
                CallStatus.HOLD, CallStatus.UNHOLD, CallStatus.INACTIVE -> {
                }
                else -> closeAudioState()
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
        return if (mBluetoothWrapper != null && mBluetoothWrapper!!.canBluetooth() && mBluetoothWrapper!!.isBTHeadsetConnected) false else mShouldSpeakerphone
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
        mShouldSpeakerphone = requestSpeakerOn
        // prioritize bluetooth by checking for bluetooth device first
        if (mBluetoothWrapper != null && mBluetoothWrapper!!.canBluetooth() && mBluetoothWrapper!!.isBTHeadsetConnected) {
            routeToBTHeadset()
        } else if (!mAudioManager.isWiredHeadsetOn && mHasSpeakerPhone && mShouldSpeakerphone) {
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
        Log.w(TAG, "toggleSpeakerphone setSpeakerphoneOn $checked")
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

    override fun decodingStarted(id: String, shmPath: String, width: Int, height: Int, isMixer: Boolean) {
        Log.i(TAG, "DEBUG decodingStarted() " + id + " " + width + "x" + height)
        val shm = Shm(id, width, height)
        videoInputs[id] = shm
        videoEvents.onNext(VideoEvent(id, start = true))
        videoSurfaces[id]?.get()?.let { holder ->
            shm.window = startVideo(id, holder.surface, width, height)
            if (shm.window == 0L) {
                Log.w(TAG, "DJamiService.decodingStarted() no window !")
            } else {
                videoEvents.onNext(VideoEvent(shm.id, started = true, w = shm.w, h = shm.h))
            }
        }
    }

    override fun decodingStopped(id: String, shmPath: String, isMixer: Boolean) {
        Log.i(TAG, "DEBUG decodingStopped() $id")
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

    override fun hasInput(id: String): Boolean {
        return videoInputs[id] !== null
    }

    override fun getCameraInfo(camId: String, formats: IntVect, sizes: UintVect, rates: UintVect) {
        // Use a larger resolution for Android 6.0+, 64 bits devices
        val useLargerSize =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() || mPreferenceService.isHardwareAccelerationEnabled)
        //int MIN_WIDTH = useLargerSize ? (useHD ? VIDEO_WIDTH_HD : VIDEO_WIDTH) : VIDEO_WIDTH_MIN;
        val minVideoSize: Point = if (useLargerSize) parseResolution(mPreferenceService.resolution) else VIDEO_SIZE_LOW
        cameraService.getCameraInfo(camId, formats, sizes, rates, minVideoSize)
    }

    private fun parseResolution(resolution: Int): Point {
        return when (resolution) {
            480 -> VIDEO_SIZE_DEFAULT
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

    override fun startScreenShare(mediaProjection: Any?): Boolean {
        val projection = mediaProjection as MediaProjection?
        if (mIsCapturing) {
            endCapture()
        }
        return if (!mIsScreenSharing && projection != null) {
            mIsScreenSharing = true
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopScreenShare()
                }
            }, cameraService.videoHandler)
            if (!cameraService.startScreenSharing(projection, mContext.resources.displayMetrics)) {
                mIsScreenSharing = false
                projection.stop()
                return false
            }
            true
        } else {
            false
        }
    }

    override fun stopScreenShare() {
        if (mIsScreenSharing) {
            cameraService.stopScreenSharing()
            mIsScreenSharing = false
            if (mShouldCapture) startCapture(mCapturingId)
        }
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
        if (mIsScreenSharing) {
            cameraService.stopScreenSharing()
            mIsScreenSharing = false
        }
        mShouldCapture = true
        if (mIsCapturing && mCapturingId != null && mCapturingId == camId) {
            return
        }
        val cam = camId ?: if (mCapturingId != null) mCapturingId else cameraService.switchInput(true)
        val videoParams = cameraService.getParams(cam)
        if (videoParams == null) {
            Log.w(TAG, "startCapture: no video parameters ")
            return
        }
        val surface = mCameraPreviewSurface.get()
        if (surface == null) {
            Log.w(TAG, "Can't start capture: no surface registered.")
            cameraService.setPreviewParams(videoParams)
            videoEvents.onNext(VideoEvent(start = true))
            return
        }
        val conf = mCameraPreviewCall.get()
        val useHardwareCodec =
            mPreferenceService.isHardwareAccelerationEnabled && (conf == null || !conf.isConference) && !mIsChoosePlugin
        if (conf != null && useHardwareCodec) {
            val call = conf.call
            if (call != null) {
                call.setDetails(JamiService.getCallDetails(call.daemonIdString).toNative())
                videoParams.codec = call.videoCodec
            } else {
                videoParams.codec = null
            }
        }
        Log.w(TAG, "startCapture: call " + cam + " " + videoParams.codec + " useHardwareCodec:" + useHardwareCodec + " bitrate:" + mPreferenceService.bitrate)
        mIsCapturing = true
        mCapturingId = videoParams.id
        Log.d(TAG, "startCapture: startCapture " + videoParams.id + " " + videoParams.width + "x" + videoParams.height + " rot" + videoParams.rotation)
        mUiScheduler.scheduleDirect {
            cameraService.openCamera(videoParams, surface,
                object : CameraListener {
                    override fun onOpened() {
                        val currentCall = conf?.id ?: return
                        if (mPluginCallId != null && mPluginCallId != currentCall) {
                            JamiService.toggleCallMediaHandler(mMediaHandlerId, currentCall, false)
                            mIsChoosePlugin = false
                            mMediaHandlerId = null
                            mPluginCallId = null
                        } else if (mIsChoosePlugin && mMediaHandlerId != null) {
                            mPluginCallId = currentCall
                            toggleMediaHandler(currentCall)
                        }
                    }

                    override fun onError() {
                        stopCapture()
                    }
                },
                useHardwareCodec,
                mPreferenceService.resolution,
                mPreferenceService.bitrate
            )
        }
        cameraService.setPreviewParams(videoParams)
        videoEvents.onNext(VideoEvent(
            started = true,
            w = videoParams.width,
            h = videoParams.height,
            rot = videoParams.rotation
        ))
    }

    override fun stopCapture() {
        Log.d(TAG, "stopCapture: " + cameraService.isOpen)
        mShouldCapture = false
        endCapture()
        if (mIsScreenSharing) {
            cameraService.stopScreenSharing()
            mIsScreenSharing = false
        }
    }

    override fun requestKeyFrame() {
        cameraService.requestKeyFrame()
    }

    override fun setBitrate(device: String, bitrate: Int) {
        cameraService.setBitrate(bitrate)
    }

    override fun endCapture() {
        if (cameraService.isOpen) {
            //final CameraService.VideoParams params = previewParams;
            cameraService.closeCamera()
            videoEvents.onNext(VideoEvent(started = false))
        }
        mIsCapturing = false
    }

    override fun addVideoSurface(id: String, holder: Any) {
        if (holder !is SurfaceHolder) {
            return
        }
        Log.w(TAG, "addVideoSurface $id")
        val shm = videoInputs[id]
        val surfaceHolder = WeakReference(holder)
        videoSurfaces[id] = surfaceHolder
        if (shm != null && shm.window == 0L) {
            shm.window = startVideo(shm.id, holder.surface, shm.w, shm.h)
        }
        if (shm == null || shm.window == 0L) {
            Log.i(TAG, "DJamiService.addVideoSurface() no window !")
            //videoEvents.onNext(VideoEvent(id, start = true))
        } else {
            videoEvents.onNext(VideoEvent(shm.id, started = true, w = shm.w, h = shm.h))
        }
    }

    override fun updateVideoSurfaceId(currentId: String, newId: String) {
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

    override fun addPreviewVideoSurface(holder: Any, conference: Conference?) {
        if (holder !is TextureView)
            return
        Log.w(TAG, "addPreviewVideoSurface " + holder.hashCode() + " mCapturingId " + mCapturingId)
        if (mCameraPreviewSurface.get() === holder) return
        mCameraPreviewSurface = WeakReference(holder)
        mCameraPreviewCall = WeakReference(conference)
        if (mShouldCapture && !mIsCapturing) {
            startCapture(mCapturingId)
        }
    }

    override fun updatePreviewVideoSurface(conference: Conference) {
        val old = mCameraPreviewCall.get()
        mCameraPreviewCall = WeakReference(conference)
        if (old !== conference && mIsCapturing) {
            val id = mCapturingId
            stopCapture()
            startCapture(id)
        }
    }

    override fun removeVideoSurface(id: String) {
        Log.i(TAG, "removeVideoSurface $id")
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
        Log.w(TAG, "removePreviewVideoSurface")
        mCameraPreviewSurface.clear()
    }

    override fun switchInput(id: String, setDefaultCamera: Boolean) {
        Log.w(TAG, "switchInput $id")
        mCapturingId = cameraService.switchInput(setDefaultCamera)
        switchInput(id, "camera://$mCapturingId")
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
        if (mCapturingId != null) {
            val videoParams = cameraService.getParams(mCapturingId)
            videoEvents.onNext(VideoEvent(
                started = true,
                w = videoParams.width,
                h = videoParams.height,
                rot = videoParams.rotation
            ))
        }
    }

    override val videoDevices: List<String>
        get() = cameraService.cameraIds

    private class Shm (val id: String, val w: Int, val h: Int) {
        var window: Long = 0
    }

    override fun unregisterCameraDetectionCallback() {
        cameraService.unregisterCameraDetectionCallback()
    }

    companion object {
        private val VIDEO_SIZE_LOW = Point(320, 240)
        private val VIDEO_SIZE_DEFAULT = Point(720, 480)
        private val VIDEO_SIZE_HD = Point(1280, 720)
        private val VIDEO_SIZE_FULL_HD = Point(1920, 1080)
        private val VIDEO_SIZE_ULTRA_HD = Point(3840, 2160)
        private val TAG = HardwareServiceImpl::class.simpleName!!
        private var mCameraPreviewSurface = WeakReference<TextureView>(null)
        private var mCameraPreviewCall = WeakReference<Conference>(null)
        private val videoSurfaces = Collections.synchronizedMap(HashMap<String, WeakReference<SurfaceHolder>>())
    }
}