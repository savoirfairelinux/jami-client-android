/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package net.jami.services

import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Call.CallStatus
import net.jami.daemon.IntVect
import net.jami.daemon.UintVect
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.model.Conference
import net.jami.utils.Log
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.jvm.Synchronized

abstract class HardwareService(
    private val mExecutor: ScheduledExecutorService,
    val mPreferenceService: PreferencesService,
    protected val mUiScheduler: Scheduler
) {
    data class VideoEvent (
        val callId: String? = null,
        val start: Boolean = false,
        val started: Boolean = false,
        val w: Int = 0,
        val h: Int = 0,
        val rot: Int = 0
    )

    class BluetoothEvent (val connected: Boolean)

    enum class AudioOutput {
        INTERNAL, SPEAKERS, BLUETOOTH
    }

    class AudioState(val outputType: AudioOutput, val outputName: String? = null)

    protected val videoEvents: Subject<VideoEvent> = PublishSubject.create()
    protected val bluetoothEvents: Subject<BluetoothEvent> = PublishSubject.create()
    protected val audioStateSubject: Subject<AudioState> = BehaviorSubject.createDefault(STATE_INTERNAL)
    protected val connectivityEvents: Subject<Boolean> = BehaviorSubject.create()

    fun getVideoEvents(): Observable<VideoEvent> = videoEvents
    fun getBluetoothEvents(): Observable<BluetoothEvent> = bluetoothEvents

    val audioState: Observable<AudioState>
        get() = audioStateSubject
    val connectivityState: Observable<Boolean>
        get() = connectivityEvents

    abstract fun initVideo(): Completable
    abstract val isVideoAvailable: Boolean
    abstract fun updateAudioState(state: CallStatus, incomingCall: Boolean, isOngoingVideo: Boolean, isSpeakerOn: Boolean)
    abstract fun closeAudioState()
    abstract val isSpeakerphoneOn: Boolean

    abstract fun toggleSpeakerphone(checked: Boolean)
    abstract fun startRinging()
    abstract fun stopRinging()
    abstract fun abandonAudioFocus()
    abstract fun decodingStarted(id: String, shmPath: String, width: Int, height: Int, isMixer: Boolean)
    abstract fun decodingStopped(id: String, shmPath: String, isMixer: Boolean)
    abstract fun hasInput(id: String): Boolean
    abstract fun getCameraInfo(camId: String, formats: IntVect, sizes: UintVect, rates: UintVect)
    abstract fun setParameters(camId: String, format: Int, width: Int, height: Int, rate: Int)
    abstract fun startCapture(camId: String?)
    abstract fun stopCapture(camId: String)
    abstract fun hasMicrophone(): Boolean
    abstract fun requestKeyFrame(camId: String)
    abstract fun setBitrate(camId: String, bitrate: Int)
    abstract fun addVideoSurface(id: String, holder: Any)
    abstract fun updateVideoSurfaceId(currentId: String, newId: String)
    abstract fun removeVideoSurface(id: String)
    abstract fun addPreviewVideoSurface(holder: Any, conference: Conference?)
    abstract fun updatePreviewVideoSurface(conference: Conference)
    abstract fun removePreviewVideoSurface()
    abstract fun switchInput(accountId:String, callId: String, setDefaultCamera: Boolean = false, screenCaptureSession: Any? = null)
    abstract fun setPreviewSettings()
    abstract fun hasCamera(): Boolean
    abstract val cameraCount: Int
    abstract val maxResolutions: Observable<Pair<Int?, Int?>>
    abstract val isPreviewFromFrontCamera: Boolean
    abstract fun shouldPlaySpeaker(): Boolean
    abstract fun unregisterCameraDetectionCallback()
    abstract fun startMediaHandler(mediaHandlerId: String?)
    abstract fun stopMediaHandler()
    fun connectivityChanged(isConnected: Boolean) {
        Log.i(TAG, "connectivityChange() $isConnected")
        connectivityEvents.onNext(isConnected)
        mExecutor.execute { JamiService.connectivityChanged() }
    }

    protected fun switchInput(accountId:String, callId: String, uri: String) {
        Log.i(TAG, "switchInput() $uri")
        mExecutor.execute { JamiService.switchInput(accountId, callId, uri) }
    }

    fun setPreviewSettings(cameraMaps: Map<String, StringMap>) {
        mExecutor.execute {
            Log.i(TAG, "applySettings() thread running...")
            for ((key, value) in cameraMaps) {
                JamiService.applySettings(key, value)
            }
        }
    }

    fun startVideo(inputId: String, surface: Any, width: Int, height: Int): Long {
        Log.i(TAG, "startVideo $inputId ${width}x$height")
        val inputWindow = JamiService.acquireNativeWindow(surface)
        if (inputWindow == 0L) {
            return inputWindow
        }
        JamiService.setNativeWindowGeometry(inputWindow, width, height)
        JamiService.registerVideoCallback(inputId, inputWindow)
        return inputWindow
    }

    fun stopVideo(inputId: String, inputWindow: Long) {
        Log.i(TAG, "stopVideo $inputId $inputWindow")
        if (inputWindow == 0L) {
            return
        }
        JamiService.unregisterVideoCallback(inputId, inputWindow)
        JamiService.releaseNativeWindow(inputWindow)
    }

    abstract fun setDeviceOrientation(rotation: Int)
    protected abstract val videoDevices: List<String>
    private var logs: Observable<String>? = null
    private var logEmitter: Emitter<String>? = null

    @get:Synchronized
    val isLogging: Boolean
        get() = logs != null

    @Synchronized
    fun startLogs(): Observable<String> {
        return logs ?: Observable.create(ObservableOnSubscribe { emitter: ObservableEmitter<String> ->
            logEmitter = emitter
            JamiService.monitor(true)
            emitter.setCancellable {
                synchronized(this@HardwareService) {
                    JamiService.monitor(false)
                    logEmitter = null
                    logs = null
                }
            }
        } as ObservableOnSubscribe<String>)
            .observeOn(Schedulers.io())
            .scan(StringBuffer(1024)) { sb: StringBuffer, message: String -> sb.append(message).append('\n') }
            .throttleLatest(500, TimeUnit.MILLISECONDS)
            .map { obj: StringBuffer -> obj.toString() }
            .replay(1)
            .autoConnect()
            .apply { logs = this }
    }

    @Synchronized
    fun stopLogs() {
        logEmitter?.let { emitter ->
            Log.w(TAG, "stopLogs JamiService.monitor(false)")
            JamiService.monitor(false)
            emitter.onComplete()
            logEmitter = null
            logs = null
        }
    }

    fun logMessage(message: String) {
        if (message.isNotEmpty())
            logEmitter?.onNext(message)
    }

    companion object {
        private val TAG = HardwareService::class.simpleName!!
        val STATE_SPEAKERS = AudioState(AudioOutput.SPEAKERS)
        val STATE_INTERNAL = AudioState(AudioOutput.INTERNAL)
    }
}