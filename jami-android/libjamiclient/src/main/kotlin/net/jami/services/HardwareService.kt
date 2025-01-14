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
package net.jami.services

import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.IntVect
import net.jami.daemon.UintVect
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.model.interaction.Call
import net.jami.model.Conference
import net.jami.utils.Log
import java.io.File
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.jvm.Synchronized

abstract class HardwareService(
    private val mExecutor: ScheduledExecutorService,
    val mPreferenceService: PreferencesService,
    protected val mUiScheduler: Scheduler
) {
    data class VideoEvent (
        val sinkId: String,
        val start: Boolean = false,
        val started: Boolean = false,
        val w: Int = 0,
        val h: Int = 0,
        val rot: Int = 0
    )

    data class BluetoothEvent(val connected: Boolean)

    enum class AudioOutputType {
        INTERNAL, WIRED, SPEAKERS, BLUETOOTH
    }
    data class AudioOutput(val type: AudioOutputType, val outputName: String? = null, val outputId: String? = null)

    data class AudioState(val output: AudioOutput, val availableOutputs: List<AudioOutput> = emptyList())

    protected val videoEvents: Subject<VideoEvent> = PublishSubject.create()
    protected val cameraEvents: Subject<VideoEvent> = PublishSubject.create()
    protected val bluetoothEvents: Subject<BluetoothEvent> = PublishSubject.create()
    protected val audioStateSubject: Subject<AudioState> = BehaviorSubject.createDefault(STATE_INTERNAL)
    protected val connectivityEvents: Subject<Boolean> = BehaviorSubject.create()

    fun getVideoEvents(): Observable<VideoEvent> = videoEvents
    fun getCameraEvents(): Observable<VideoEvent> = cameraEvents

    fun getBluetoothEvents(): Observable<BluetoothEvent> = bluetoothEvents

    abstract fun getAudioState(conf: Conference): Observable<AudioState>
    val audioState: Observable<AudioState>
        get() = audioStateSubject
    val connectivityState: Observable<Boolean>
        get() = connectivityEvents

    abstract fun initVideo(): Completable
    abstract val isVideoAvailable: Boolean
    abstract fun updateAudioState(conf: Conference?, call: Call, incomingCall: Boolean, isOngoingVideo: Boolean)
    abstract fun closeAudioState()
    abstract fun isSpeakerphoneOn(): Boolean

    abstract fun toggleSpeakerphone(conf: Conference, checked: Boolean)
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
    abstract fun changeCamera(setDefaultCamera: Boolean = false): String?
    abstract fun setPreviewSettings()
    abstract fun hasCamera(): Boolean
    abstract fun cameraCount(): Int
    abstract val maxResolutions: Observable<Pair<Int?, Int?>>
    abstract val isPreviewFromFrontCamera: Boolean
    abstract fun shouldPlaySpeaker(): Boolean
    abstract fun unregisterCameraDetectionCallback()
    abstract fun startMediaHandler(mediaHandlerId: String?)
    abstract fun stopMediaHandler()
    abstract fun setPendingScreenShareProjection(screenCaptureSession: Any?)
    fun connectivityChanged(isConnected: Boolean) {
        Log.i(TAG, "connectivityChange() $isConnected")
        connectivityEvents.onNext(isConnected)
        mExecutor.execute { JamiService.connectivityChanged() }
    }

    fun switchInput(accountId:String, callId: String, uri: String) {
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
        if (inputWindow != 0L) {
            JamiService.setNativeWindowGeometry(inputWindow, width, height)
            JamiService.registerVideoCallback(inputId, inputWindow)
        }
        return inputWindow
    }

    fun stopVideo(inputId: String, inputWindow: Long) {
        Log.i(TAG, "stopVideo $inputId $inputWindow")
        if (inputWindow != 0L) {
            JamiService.unregisterVideoCallback(inputId, inputWindow)
            JamiService.releaseNativeWindow(inputWindow)
        }
    }

    abstract fun connectSink(id: String, windowId: Long): Observable<Pair<Int, Int>>
    abstract fun getSinkSize(id: String): Single<Pair<Int, Int>>

    abstract fun setDeviceOrientation(rotation: Int)
    protected abstract fun videoDevices(): List<String>
    abstract fun saveLoggingState(enabled: Boolean)

    abstract val pushLogFile: File
    private var logs: Observable<List<String>>? = null
    private var logEmitter: Emitter<String>? = null
    private var pushLogs: Observable<String>? = null
    private var pushLogEmitter: Emitter<String>? = null
    var pushLogEnabled = false
    var highPriorityPushCount = 0
    var normalPriorityPushCount = 0
    var unknownPriorityPushCount = 0
    var startTime: String? = null

    @get:Synchronized
    val isLogging: Boolean
        get() = logs != null

    @Synchronized
    fun startLogs(): Observable<List<String>> {
        return logs ?: Observable.create { emitter: ObservableEmitter<String> ->
            logEmitter = emitter
            // Queue the service call on daemon executor to be sure it has been initialized.
            mExecutor.execute { JamiService.monitor(true) }
            emitter.setCancellable {
                synchronized(this@HardwareService) {
                    mExecutor.execute { JamiService.monitor(false) }
                    logEmitter = null
                    logs = null
                }
            }
        }
            .buffer(500, TimeUnit.MILLISECONDS)
            .filter { it.isNotEmpty() }
            .replay()
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

    @Synchronized
    fun startPushLogs(): Observable<String> {
        pushLogFile.createNewFile()
        pushLogEnabled = true
        saveLoggingState(true)
        return pushLogs ?: Observable.concat(
            Observable.fromIterable(pushLogFile.readLines()),
            Observable.create { emitter: ObservableEmitter<String> ->
                pushLogEmitter = emitter
                emitter.setCancellable {
                    synchronized(this@HardwareService) {
                        pushLogEmitter = null
                        pushLogs = null
                    }
                }
            }
        )
            .apply { pushLogs = this }
    }

    @Synchronized
    fun stopPushLogs() {
        pushLogEnabled = false
        saveLoggingState(false)
        pushLogEmitter?.let { emitter ->
            emitter.onComplete()
            pushLogEmitter = null
            pushLogs = null
        }
    }

    @get:Synchronized
    val loggingStatus: Boolean
        get() = pushLogEnabled


    fun pushLogMessage(message: String) {
        if (pushLogEnabled) {
            pushLogFile.appendText(message + "\n")
            pushLogEmitter?.onNext(message)
        }
    }

    companion object {
        private val TAG = HardwareService::class.simpleName!!
        val OUTPUT_SPEAKERS = AudioOutput(AudioOutputType.SPEAKERS)
        val OUTPUT_INTERNAL = AudioOutput(AudioOutputType.INTERNAL)
        val OUTPUT_WIRED = AudioOutput(AudioOutputType.WIRED)
        val OUTPUT_BLUETOOTH = AudioOutput(AudioOutputType.BLUETOOTH)
        val STATE_INTERNAL = AudioState(OUTPUT_INTERNAL)
    }
}