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
package cx.ring.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.telecom.CallAudioState
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import cx.ring.service.CallConnection
import cx.ring.services.CallServiceImpl.Companion.CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY
import cx.ring.services.CameraService.CameraListener
import cx.ring.utils.BluetoothWrapper
import cx.ring.utils.BluetoothWrapper.BluetoothChangeListener
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.daemon.IntVect
import net.jami.daemon.JamiService
import net.jami.daemon.UintVect
import net.jami.model.Conference
import net.jami.model.interaction.Call
import net.jami.model.interaction.Call.CallStatus
import net.jami.services.HardwareService
import net.jami.services.PreferencesService
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ScheduledExecutorService

class HardwareServiceImpl(
    private val context: Context,
    executor: ScheduledExecutorService,
    preferenceService: PreferencesService,
    uiScheduler: Scheduler
) : HardwareService(executor, preferenceService, uiScheduler), OnAudioFocusChangeListener, BluetoothChangeListener {
    private val videoInputs: MutableMap<String, Shm> = HashMap()
    private val cameraService = CameraService(context)
    private val mAudioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mBluetoothWrapper: BluetoothWrapper? = null
    private var currentFocus: AudioFocusRequestCompat? = null
    private var pendingScreenSharingSession: MediaProjection? = null
    private val shouldCapture = HashSet<String>()
    private var mShouldSpeakerphone = false
    private val mHasSpeakerPhone: Boolean by lazy { hasSpeakerphone() }
    private var mIsChooseExtension = false
    private var mMediaHandlerId: String? = null
    private var mExtensionCallId: String? = null
    private val sharedPreferences: SharedPreferences = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    override val pushLogFile: File = File(context.filesDir, "firebaselog.txt")

    init {
        pushLogEnabled = sharedPreferences.getBoolean(LOGGING_ENABLED_KEY, false)
    }

    override fun initVideo(): Completable = cameraService.init()

    override val maxResolutions: Observable<Pair<Int?, Int?>>
        get() = cameraService.maxResolutions
    override val isVideoAvailable: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) || cameraService.hasCamera()

    override fun hasMicrophone(): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE))
            return true
        val recorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
            else MediaRecorder()
        val testFile = File(context.cacheDir, "MediaUtil#micAvailTestFile")
        return try {
            with(recorder) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                setOutputFile(testFile.absolutePath)
                prepare()
                start()
            }
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

    override fun isSpeakerphoneOn(): Boolean = mAudioManager.isSpeakerphoneOn

    private val RINGTONE_REQUEST = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributesCompat.USAGE_NOTIFICATION_RINGTONE)
            .setLegacyStreamType(AudioManager.STREAM_RING)
            .build())
        .setOnAudioFocusChangeListener(this)
        .build()
    private val CALL_REQUEST = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
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

    @SuppressLint("NewApi")
    override fun getAudioState(conf: Conference): Observable<AudioState> =
        conf.call!!.systemConnection
            .flatMapObservable { a -> (a as CallServiceImpl.AndroidCall).connection!!.audioState }
            .map { a -> AudioState(routeToType(a.route), maskToList(a.supportedRouteMask)) }
            .onErrorResumeWith { audioState }

    private fun routeToType(a: Int): AudioOutput = when(a) {
        CallAudioState.ROUTE_EARPIECE -> OUTPUT_INTERNAL
        CallAudioState.ROUTE_WIRED_HEADSET -> OUTPUT_WIRED
        CallAudioState.ROUTE_SPEAKER -> OUTPUT_SPEAKERS
        CallAudioState.ROUTE_BLUETOOTH -> OUTPUT_BLUETOOTH
        else -> OUTPUT_INTERNAL
    }

    private fun maskToList(routeMask: Int): List<AudioOutput> = ArrayList<AudioOutput>().apply {
        if ((routeMask and CallAudioState.ROUTE_EARPIECE) != 0)
            add(OUTPUT_INTERNAL)
        if ((routeMask and CallAudioState.ROUTE_WIRED_HEADSET) != 0)
            add(OUTPUT_WIRED)
        if ((routeMask and CallAudioState.ROUTE_SPEAKER) != 0)
            add(OUTPUT_SPEAKERS)
        if ((routeMask and CallAudioState.ROUTE_BLUETOOTH) != 0)
            add(OUTPUT_BLUETOOTH)
    }

    @RequiresApi(CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY)
    fun setAudioState(call: CallConnection, wantSpeaker: Boolean) {
        Log.w(TAG, "setAudioState Telecom API $wantSpeaker ${call.callAudioState}")
        call.setWantedAudioState(if (wantSpeaker) CallConnection.ROUTE_LIST_SPEAKER_IMPLICIT else CallConnection.ROUTE_LIST_DEFAULT)
    }

    val disposables = CompositeDisposable()

    @SuppressLint("NewApi")
    @Synchronized
    override fun updateAudioState(conf: Conference?, call: Call, incomingCall: Boolean, isOngoingVideo: Boolean) {
        Log.d(TAG, "updateAudioState $conf: Call state updated to ${call.callStatus} Call is incoming: $incomingCall Call is video: $isOngoingVideo")
        disposables.add(call.systemConnection.map {
                (it as CallServiceImpl.AndroidCall).connection!!
            }
            .subscribe({ systemCall ->
                // Try using the Telecom API if available
                setAudioState(systemCall, incomingCall || isOngoingVideo)
            }) { e ->
                Log.w(TAG, "updateAudioState fallback")
                // Fallback on the AudioManager API
                try {
                    val state = call.callStatus
                    val callEnded = state == CallStatus.HUNGUP || state == CallStatus.FAILURE || state == CallStatus.OVER
                    if (mBluetoothWrapper == null && !callEnded) {
                        mBluetoothWrapper = BluetoothWrapper(context, this)
                    }
                    when (state) {
                        CallStatus.RINGING -> {
                            getFocus(RINGTONE_REQUEST)
                            if (incomingCall) {
                                // ringtone for incoming calls
                                mAudioManager.mode = AudioManager.MODE_RINGTONE
                                setAudioRouting(true)
                            } else setAudioRouting(isOngoingVideo)
                        }
                        CallStatus.CURRENT -> {
                            getFocus(CALL_REQUEST)
                            mAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                            mShouldSpeakerphone = isOngoingVideo || isSpeakerphoneOn()
                            setAudioRouting(mShouldSpeakerphone)
                        }
                        CallStatus.HOLD, CallStatus.UNHOLD, CallStatus.INACTIVE -> {}
                        else -> if (callEnded) closeAudioState()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating audio state", e)
                }
            })
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
        abandonAudioFocus()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Log.i(TAG, "onAudioFocusChange $focusChange")
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
        Log.w(TAG, "setAudioRouting requestSpeakerOn:$requestSpeakerOn isBTHeadsetConnected:${bt?.isBTHeadsetConnected} isWiredHeadsetOn:${mAudioManager.isWiredHeadsetOn}")
        if (bt != null && bt.canBluetooth() && bt.isBTHeadsetConnected) {
            routeToBTHeadset()
        } else if (!mAudioManager.isWiredHeadsetOn && mHasSpeakerPhone && requestSpeakerOn) {
            routeToSpeaker()
        } else {
            resetAudio()
        }
    }

    private fun hasSpeakerphone(): Boolean {
        // Check FEATURE_AUDIO_OUTPUT to guard against false positives.
        val packageManager = context.packageManager
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
        audioStateSubject.onNext(AudioState(OUTPUT_BLUETOOTH))
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
        audioStateSubject.onNext(AudioState(OUTPUT_SPEAKERS))
    }

    /**
     * Returns to earpiece audio
     */
    private fun resetAudio() {
        mBluetoothWrapper?.setBluetoothOn(false)
        mAudioManager.isSpeakerphoneOn = false
        audioStateSubject.onNext(STATE_INTERNAL)
    }

    @SuppressLint("NewApi")
    @Synchronized
    override fun toggleSpeakerphone(conf: Conference, checked: Boolean) {
        Log.w(TAG, "toggleSpeakerphone $conf $checked")

        conf.call?.let { call ->
            val hasVideo = conf.hasActiveVideo()
            disposables.add(
                call.systemConnection
                    .map {
                        // Map before subscribe to fallback to the error path if no Telecom API
                        (it as CallServiceImpl.AndroidCall).connection!!
                    }
                    .subscribe({
                        // Using the Telecom API
                        it.setWantedAudioState(
                            if (checked) CallConnection.ROUTE_LIST_SPEAKER_EXPLICIT
                            else if (hasVideo) CallConnection.ROUTE_LIST_SPEAKER_IMPLICIT
                            else CallConnection.ROUTE_LIST_DEFAULT
                        )
                    }) {
                        // Fallback to the AudioManager API
                        JamiService.setAudioPlugin(JamiService.getCurrentAudioOutputPlugin())
                        mShouldSpeakerphone = checked
                        if (mHasSpeakerPhone && checked) {
                            routeToSpeaker()
                        } else
                            if (mBluetoothWrapper != null && mBluetoothWrapper!!.canBluetooth()) {
                                routeToBTHeadset()
                            } else {
                                resetAudio()
                            }
                    }
            )
        } ?: Log.e(TAG, "This is a bug. Cannot toggle speaker phone as conference call is null.")
    }

    @Synchronized
    override fun onBluetoothStateChanged(status: Int) {
        Log.w(TAG, "bluetoothStateChanged to: $status")
        val event = BluetoothEvent(status == BluetoothHeadset.STATE_AUDIO_CONNECTED)
        if (status == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            Log.w(TAG, "BluetoothHeadset Connected")
            if (mBluetoothWrapper?.isBluetoothOn() == true)
                routeToBTHeadset()
        } else if (status == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
            Log.w(TAG, "BluetoothHeadset Disconnected $mShouldSpeakerphone")
            if (mAudioManager.mode == AudioManager.MODE_RINGTONE || mShouldSpeakerphone)
                routeToSpeaker()
            else
                resetAudio()
        }
        bluetoothEvents.onNext(event)
    }

    override fun decodingStarted(id: String, shmPath: String, width: Int, height: Int, isMixer: Boolean) {
        Log.i(TAG, "decodingStarted() " + id + " " + width + "x" + height)
        val shm = Shm(id, width, height)
        synchronized(videoInputs) {
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
    }

    override fun decodingStopped(id: String, shmPath: String, isMixer: Boolean) {
        Log.i(TAG, "decodingStopped() $id")
        synchronized(videoInputs) {
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
    }

    override fun connectSink(id: String, windowId: Long): Observable<Pair<Int, Int>> {
        var registered = JamiService.registerVideoCallback(id, windowId)
        val ret = videoEvents.filter { e -> e.sinkId == id }
            .doOnDispose {
                JamiService.unregisterVideoCallback(id, windowId)
            }
            .map { c ->
                if (!registered && c.start && !c.started) {
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

    override fun getSinkSize(id: String): Single<Pair<Int, Int>> {
        synchronized(videoInputs) {
            val p = videoInputs[id]?.let { input -> Pair(input.w, input.h) }
            return if (p != null) Single.just(p)
            else videoEvents.filter { it.sinkId == id }.firstOrError().map { e -> Pair(e.w, e.h) }
        }
    }

    override fun hasInput(id: String): Boolean = videoInputs[id] !== null

    override fun getCameraInfo(camId: String, formats: IntVect, sizes: UintVect, rates: UintVect) {
        // Use a larger resolution for Android 6.0+, 64 bits devices
        val useLargerSize = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() || mPreferenceService.isHardwareAccelerationEnabled
        //int MIN_WIDTH = useLargerSize ? (useHD ? VIDEO_WIDTH_HD : VIDEO_WIDTH) : VIDEO_WIDTH_MIN;
        val minVideoSize: Size = if (useLargerSize) parseResolution(mPreferenceService.resolution) else VIDEO_SIZE_LOW
        cameraService.getCameraInfo(camId, formats, sizes, rates, minVideoSize, context)
    }

    override fun setParameters(camId: String, format: Int, width: Int, height: Int, rate: Int) {
        Log.d(TAG, "setParameters: $camId, $format, $width, $height, $rate")
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        cameraService.setParameters(camId, format, width, height, rate, windowManager.defaultDisplay.rotation)
    }

    override fun startMediaHandler(mediaHandlerId: String?) {
        mIsChooseExtension = true
        mMediaHandlerId = mediaHandlerId
    }

    private fun toggleMediaHandler(callId: String) {
        if (mMediaHandlerId != null) JamiService.toggleCallMediaHandler(mMediaHandlerId, callId, true)
    }

    override fun stopMediaHandler() {
        mIsChooseExtension = false
        mMediaHandlerId = null
    }

    override fun startCapture(camId: String?) {
        val cam = camId ?: cameraService.switchInput(true) ?: return
        Log.i(TAG, "startCapture > camId: $camId, cam: $cam, mIsChooseExtension: $mIsChooseExtension")
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
            mPreferenceService.isHardwareAccelerationEnabled && (conf == null || !conf.isConference) && !mIsChooseExtension
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
            if (!cameraService.startScreenSharing(videoParams, projection, surface, context.resources.displayMetrics)) {
                projection.stop()
            }
            return
        }

        mUiScheduler.scheduleDirect {
            cameraService.openCamera(videoParams, surface,
                object : CameraListener {
                    override fun onOpened() {
                        val currentCall = conf?.id ?: return
                        if (mExtensionCallId != null && mExtensionCallId != currentCall) {
                            if (mMediaHandlerId != null) {
                                JamiService.toggleCallMediaHandler(mMediaHandlerId, currentCall,false)
                            }
                            mIsChooseExtension = false
                            mMediaHandlerId = null
                            mExtensionCallId = null
                        } else if (mIsChooseExtension && mMediaHandlerId != null) {
                            mExtensionCallId = currentCall
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

    override fun addVideoSurface(id: String, holder: Any) {
        Log.w(TAG, " addVideoSurface $id $holder")
        if (holder !is SurfaceHolder) return
        synchronized(videoInputs) {
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
    }

    override fun updateVideoSurfaceId(currentId: String, newId: String) {
        Log.w(TAG, "updateVideoSurfaceId $currentId $newId")
        synchronized(videoInputs) {
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
    }

    override fun addPreviewVideoSurface(holder: Any, conference: Conference?) {
        synchronized(videoInputs) {
            Log.w(TAG, "addPreviewVideoSurface > holder:$holder")
            if (holder !is TextureView) return
            if (mCameraPreviewSurface.get() === holder) return
            mCameraPreviewSurface = WeakReference(holder)
            mCameraPreviewCall = WeakReference(conference)
            for (c in shouldCapture) startCapture(c)
        }
    }

    override fun updatePreviewVideoSurface(conference: Conference) {
        val old = mCameraPreviewCall.get()
        mCameraPreviewCall = WeakReference(conference)
        if (old !== conference) {
            for (camId in shouldCapture) {
                cameraService.closeCamera(camId)
                startCapture(camId)
            }
        }
    }

    override fun removeVideoSurface(id: String) {
        synchronized(videoInputs) {
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
    }

    override fun removePreviewVideoSurface() {
        mCameraPreviewSurface.clear()
    }

    override fun changeCamera(setDefaultCamera: Boolean): String? {
        return cameraService.switchInput(setDefaultCamera)
    }

    override fun setPendingScreenShareProjection(screenCaptureSession: Any?) {
        pendingScreenSharingSession = screenCaptureSession as MediaProjection?
    }

    override fun setPreviewSettings() {
        setPreviewSettings(cameraService.previewSettings)
    }

    override fun cameraCount(): Int = cameraService.getCameraCount()

    override fun hasCamera(): Boolean = cameraService.hasCamera()

    override fun videoDevices() = cameraService.cameraIds()

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

    private data class Shm (val id: String, val w: Int, val h: Int) {
        var window: Long = 0
    }

    override fun unregisterCameraDetectionCallback() {
        cameraService.unregisterCameraDetectionCallback()
    }

    override fun saveLoggingState(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(LOGGING_ENABLED_KEY, enabled)
            .apply()
    }

    companion object {
        private val VIDEO_SIZE_LOW = Size(480, 320)
        private val VIDEO_SIZE_SD = Size(720, 480)
        private val VIDEO_SIZE_HD = Size(1280, 720)
        private val VIDEO_SIZE_FULL_HD = Size(1920, 1080)
        private val VIDEO_SIZE_QUAD_HD = Size(2560, 1440)
        private val VIDEO_SIZE_ULTRA_HD = Size(3840, 2160)
        val VIDEO_SIZE_DEFAULT = VIDEO_SIZE_SD
        val resolutions = listOf(VIDEO_SIZE_ULTRA_HD, VIDEO_SIZE_QUAD_HD, VIDEO_SIZE_FULL_HD, VIDEO_SIZE_HD, VIDEO_SIZE_SD, VIDEO_SIZE_LOW)

        private fun parseResolution(resolution: Int): Size {
            for (res in resolutions) {
                if (res.height <= resolution)
                    return res
            }
            return VIDEO_SIZE_DEFAULT
        }

        private val TAG = HardwareServiceImpl::class.simpleName!!
        private var mCameraPreviewSurface = WeakReference<TextureView>(null)
        private var mCameraExtensionPreviewSurface = WeakReference<SurfaceView>(null)
        private var mCameraPreviewCall = WeakReference<Conference>(null)
        private val videoSurfaces = HashMap<String, WeakReference<SurfaceHolder>>()
        private const val PREFS_NAME = "logging"
        private const val LOGGING_ENABLED_KEY = "logging_enabled"
    }
}