package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u00b0\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010$\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\t\n\u0002\b\u0011\n\u0002\u0018\u0002\n\u0002\b\f\b&\u0018\u0000 \u0088\u00012\u00020\u0001:\n\u0085\u0001\u0086\u0001\u0087\u0001\u0088\u0001\u0089\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\b\u00105\u001a\u000206H&J\u001c\u00107\u001a\u0002062\b\u00108\u001a\u0004\u0018\u00010\u00012\b\u00109\u001a\u0004\u0018\u00010:H&J\u001c\u0010;\u001a\u0002062\b\u0010<\u001a\u0004\u0018\u00010%2\b\u00108\u001a\u0004\u0018\u00010\u0001H&J\b\u0010=\u001a\u000206H&J\u000e\u0010>\u001a\u0002062\u0006\u0010?\u001a\u00020\u001aJ0\u0010@\u001a\u0002062\u0006\u0010<\u001a\u00020%2\u0006\u0010A\u001a\u00020%2\u0006\u0010B\u001a\u00020\u00162\u0006\u0010C\u001a\u00020\u00162\u0006\u0010D\u001a\u00020\u001aH&J \u0010E\u001a\u0002062\u0006\u0010<\u001a\u00020%2\u0006\u0010A\u001a\u00020%2\u0006\u0010D\u001a\u00020\u001aH&J\b\u0010F\u001a\u000206H&J\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00130\nJ(\u0010G\u001a\u0002062\u0006\u0010H\u001a\u00020%2\u0006\u0010I\u001a\u00020J2\u0006\u0010K\u001a\u00020L2\u0006\u0010M\u001a\u00020LH&J\f\u00104\u001a\b\u0012\u0004\u0012\u0002030\nJ\b\u0010N\u001a\u00020\u001aH&J\b\u0010O\u001a\u00020\u001aH&J\b\u0010P\u001a\u00020QH&J\u000e\u0010R\u001a\u0002062\u0006\u0010S\u001a\u00020%J\b\u0010T\u001a\u000206H&J\u0012\u0010U\u001a\u0002062\b\u0010<\u001a\u0004\u0018\u00010%H&J\b\u0010V\u001a\u000206H&J\u001a\u0010W\u001a\u0002062\b\u0010X\u001a\u0004\u0018\u00010%2\u0006\u0010Y\u001a\u00020\u0016H&J\u0010\u0010Z\u001a\u0002062\u0006\u0010[\u001a\u00020\u0016H&J0\u0010\\\u001a\u0002062\u0006\u0010H\u001a\u00020%2\u0006\u0010]\u001a\u00020\u00162\u0006\u0010B\u001a\u00020\u00162\u0006\u0010C\u001a\u00020\u00162\u0006\u0010^\u001a\u00020\u0016H&J\b\u0010_\u001a\u000206H&J\u001e\u0010_\u001a\u0002062\u0016\u0010`\u001a\u0012\u0012\u0006\u0012\u0004\u0018\u00010%\u0012\u0006\u0012\u0004\u0018\u00010b0aJ\b\u0010c\u001a\u00020\u001aH&J\u0012\u0010d\u001a\u0002062\b\u0010H\u001a\u0004\u0018\u00010%H&J\f\u0010e\u001a\b\u0012\u0004\u0012\u00020%0\nJ\u0012\u0010f\u001a\u0002062\b\u0010g\u001a\u0004\u0018\u00010%H&J\b\u0010h\u001a\u000206H&J\u0012\u0010i\u001a\u00020\u001a2\b\u0010j\u001a\u0004\u0018\u00010\u0001H&J*\u0010k\u001a\u00020l2\b\u0010m\u001a\u0004\u0018\u00010%2\b\u0010n\u001a\u0004\u0018\u00010\u00012\u0006\u0010B\u001a\u00020\u00162\u0006\u0010C\u001a\u00020\u0016J\b\u0010o\u001a\u000206H&J\u0006\u0010p\u001a\u000206J\b\u0010q\u001a\u000206H&J\b\u0010r\u001a\u000206H&J\b\u0010s\u001a\u000206H&J\u0018\u0010t\u001a\u0002062\b\u0010m\u001a\u0004\u0018\u00010%2\u0006\u0010u\u001a\u00020lJ\u001a\u0010v\u001a\u0002062\b\u0010<\u001a\u0004\u0018\u00010%2\u0006\u0010w\u001a\u00020\u001aH&J\u001a\u0010v\u001a\u0002062\b\u0010<\u001a\u0004\u0018\u00010%2\u0006\u0010x\u001a\u00020%H\u0004J\u0010\u0010y\u001a\u0002062\u0006\u0010z\u001a\u00020\u001aH&J\b\u0010{\u001a\u000206H&J#\u0010|\u001a\u0002062\b\u0010}\u001a\u0004\u0018\u00010~2\u0006\u0010\u007f\u001a\u00020\u001a2\u0007\u0010\u0080\u0001\u001a\u00020\u001aH&J\u0013\u0010\u0081\u0001\u001a\u0002062\b\u00109\u001a\u0004\u0018\u00010:H&J\u001f\u0010\u0082\u0001\u001a\u0002062\t\u0010\u0083\u0001\u001a\u0004\u0018\u00010%2\t\u0010\u0084\u0001\u001a\u0004\u0018\u00010%H&R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n8F\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\rR\u001a\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000fX\u0084\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u001a\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u000fX\u0084\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0011R\u0012\u0010\u0015\u001a\u00020\u0016X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0017\u0010\u0018R\u001a\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000fX\u0084\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0011R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001a0\n8F\u00a2\u0006\u0006\u001a\u0004\b\u001d\u0010\rR\u0011\u0010\u001e\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b\u001e\u0010\u001fR\u0012\u0010 \u001a\u00020\u001aX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b \u0010\u001fR\u0012\u0010!\u001a\u00020\u001aX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b!\u0010\u001fR\u0012\u0010\"\u001a\u00020\u001aX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\"\u0010\u001fR\u0016\u0010#\u001a\n\u0012\u0004\u0012\u00020%\u0018\u00010$X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010&\u001a\n\u0012\u0004\u0012\u00020%\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010(R\u0014\u0010\u0006\u001a\u00020\u0007X\u0084\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010*R(\u0010+\u001a\u0018\u0012\u0014\u0012\u0012\u0012\u0006\u0012\u0004\u0018\u00010\u0016\u0012\u0006\u0012\u0004\u0018\u00010\u00160,0\nX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b-\u0010\rR\u001c\u0010.\u001a\f\u0012\u0006\u0012\u0004\u0018\u00010%\u0018\u00010/X\u00a4\u0004\u00a2\u0006\u0006\u001a\u0004\b0\u00101R\u001a\u00102\u001a\b\u0012\u0004\u0012\u0002030\u000fX\u0084\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u0010\u0011\u00a8\u0006\u008a\u0001"}, d2 = {"Lnet/jami/services/HardwareService;", "", "mExecutor", "Ljava/util/concurrent/ScheduledExecutorService;", "mPreferenceService", "Lnet/jami/services/PreferencesService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Ljava/util/concurrent/ScheduledExecutorService;Lnet/jami/services/PreferencesService;Lio/reactivex/rxjava3/core/Scheduler;)V", "audioState", "Lio/reactivex/rxjava3/core/Observable;", "Lnet/jami/services/HardwareService$AudioState;", "getAudioState", "()Lio/reactivex/rxjava3/core/Observable;", "audioStateSubject", "Lio/reactivex/rxjava3/subjects/Subject;", "getAudioStateSubject", "()Lio/reactivex/rxjava3/subjects/Subject;", "bluetoothEvents", "Lnet/jami/services/HardwareService$BluetoothEvent;", "getBluetoothEvents", "cameraCount", "", "getCameraCount", "()I", "connectivityEvents", "", "getConnectivityEvents", "connectivityState", "getConnectivityState", "isLogging", "()Z", "isPreviewFromFrontCamera", "isSpeakerphoneOn", "isVideoAvailable", "logEmitter", "Lio/reactivex/rxjava3/core/Emitter;", "", "logs", "getMPreferenceService", "()Lnet/jami/services/PreferencesService;", "getMUiScheduler", "()Lio/reactivex/rxjava3/core/Scheduler;", "maxResolutions", "Lnet/jami/utils/Tuple;", "getMaxResolutions", "videoDevices", "", "getVideoDevices", "()Ljava/util/List;", "videoEvents", "Lnet/jami/services/HardwareService$VideoEvent;", "getVideoEvents", "abandonAudioFocus", "", "addPreviewVideoSurface", "holder", "conference", "Lnet/jami/model/Conference;", "addVideoSurface", "id", "closeAudioState", "connectivityChanged", "isConnected", "decodingStarted", "shmPath", "width", "height", "isMixer", "decodingStopped", "endCapture", "getCameraInfo", "camId", "formats", "Lnet/jami/daemon/IntVect;", "sizes", "Lnet/jami/daemon/UintVect;", "rates", "hasCamera", "hasMicrophone", "initVideo", "Lio/reactivex/rxjava3/core/Completable;", "logMessage", "message", "removePreviewVideoSurface", "removeVideoSurface", "requestKeyFrame", "setBitrate", "device", "bitrate", "setDeviceOrientation", "rotation", "setParameters", "format", "rate", "setPreviewSettings", "cameraMaps", "", "Lnet/jami/daemon/StringMap;", "shouldPlaySpeaker", "startCapture", "startLogs", "startMediaHandler", "mediaHandlerId", "startRinging", "startScreenShare", "mediaProjection", "startVideo", "", "inputId", "surface", "stopCapture", "stopLogs", "stopMediaHandler", "stopRinging", "stopScreenShare", "stopVideo", "inputWindow", "switchInput", "setDefaultCamera", "uri", "toggleSpeakerphone", "checked", "unregisterCameraDetectionCallback", "updateAudioState", "state", "Lnet/jami/model/Call$CallStatus;", "incomingCall", "isOngoingVideo", "updatePreviewVideoSurface", "updateVideoSurfaceId", "currentId", "newId", "AudioOutput", "AudioState", "BluetoothEvent", "Companion", "VideoEvent", "libringclient"})
public abstract class HardwareService {
    private final java.util.concurrent.ScheduledExecutorService mExecutor = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.services.PreferencesService mPreferenceService = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.HardwareService.VideoEvent> videoEvents = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.HardwareService.BluetoothEvent> bluetoothEvents = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.HardwareService.AudioState> audioStateSubject = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.Boolean> connectivityEvents = null;
    private io.reactivex.rxjava3.core.Observable<java.lang.String> logs;
    private io.reactivex.rxjava3.core.Emitter<java.lang.String> logEmitter;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.HardwareService.Companion Companion = null;
    private static final java.lang.String TAG = null;
    @org.jetbrains.annotations.NotNull()
    private static final net.jami.services.HardwareService.AudioState STATE_SPEAKERS = null;
    @org.jetbrains.annotations.NotNull()
    private static final net.jami.services.HardwareService.AudioState STATE_INTERNAL = null;
    
    public HardwareService(@org.jetbrains.annotations.NotNull()
    java.util.concurrent.ScheduledExecutorService mExecutor, @org.jetbrains.annotations.NotNull()
    net.jami.services.PreferencesService mPreferenceService, @org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.services.PreferencesService getMPreferenceService() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    protected final io.reactivex.rxjava3.core.Scheduler getMUiScheduler() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    protected final io.reactivex.rxjava3.subjects.Subject<net.jami.services.HardwareService.VideoEvent> getVideoEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    protected final io.reactivex.rxjava3.subjects.Subject<net.jami.services.HardwareService.BluetoothEvent> getBluetoothEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    protected final io.reactivex.rxjava3.subjects.Subject<net.jami.services.HardwareService.AudioState> getAudioStateSubject() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    protected final io.reactivex.rxjava3.subjects.Subject<java.lang.Boolean> getConnectivityEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.services.HardwareService.VideoEvent> getVideoEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.services.HardwareService.BluetoothEvent> getBluetoothEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.services.HardwareService.AudioState> getAudioState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.lang.Boolean> getConnectivityState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Completable initVideo();
    
    public abstract boolean isVideoAvailable();
    
    public abstract void updateAudioState(@org.jetbrains.annotations.Nullable()
    net.jami.model.Call.CallStatus state, boolean incomingCall, boolean isOngoingVideo);
    
    public abstract void closeAudioState();
    
    public abstract boolean isSpeakerphoneOn();
    
    public abstract void toggleSpeakerphone(boolean checked);
    
    public abstract void startRinging();
    
    public abstract void stopRinging();
    
    public abstract void abandonAudioFocus();
    
    public abstract void decodingStarted(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String shmPath, int width, int height, boolean isMixer);
    
    public abstract void decodingStopped(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String shmPath, boolean isMixer);
    
    public abstract void getCameraInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String camId, @org.jetbrains.annotations.NotNull()
    net.jami.daemon.IntVect formats, @org.jetbrains.annotations.NotNull()
    net.jami.daemon.UintVect sizes, @org.jetbrains.annotations.NotNull()
    net.jami.daemon.UintVect rates);
    
    public abstract void setParameters(@org.jetbrains.annotations.NotNull()
    java.lang.String camId, int format, int width, int height, int rate);
    
    public abstract void startCapture(@org.jetbrains.annotations.Nullable()
    java.lang.String camId);
    
    public abstract boolean startScreenShare(@org.jetbrains.annotations.Nullable()
    java.lang.Object mediaProjection);
    
    public abstract boolean hasMicrophone();
    
    public abstract void stopCapture();
    
    public abstract void endCapture();
    
    public abstract void stopScreenShare();
    
    public abstract void requestKeyFrame();
    
    public abstract void setBitrate(@org.jetbrains.annotations.Nullable()
    java.lang.String device, int bitrate);
    
    public abstract void addVideoSurface(@org.jetbrains.annotations.Nullable()
    java.lang.String id, @org.jetbrains.annotations.Nullable()
    java.lang.Object holder);
    
    public abstract void updateVideoSurfaceId(@org.jetbrains.annotations.Nullable()
    java.lang.String currentId, @org.jetbrains.annotations.Nullable()
    java.lang.String newId);
    
    public abstract void removeVideoSurface(@org.jetbrains.annotations.Nullable()
    java.lang.String id);
    
    public abstract void addPreviewVideoSurface(@org.jetbrains.annotations.Nullable()
    java.lang.Object holder, @org.jetbrains.annotations.Nullable()
    net.jami.model.Conference conference);
    
    public abstract void updatePreviewVideoSurface(@org.jetbrains.annotations.Nullable()
    net.jami.model.Conference conference);
    
    public abstract void removePreviewVideoSurface();
    
    public abstract void switchInput(@org.jetbrains.annotations.Nullable()
    java.lang.String id, boolean setDefaultCamera);
    
    public abstract void setPreviewSettings();
    
    public abstract boolean hasCamera();
    
    public abstract int getCameraCount();
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Observable<net.jami.utils.Tuple<java.lang.Integer, java.lang.Integer>> getMaxResolutions();
    
    public abstract boolean isPreviewFromFrontCamera();
    
    public abstract boolean shouldPlaySpeaker();
    
    public abstract void unregisterCameraDetectionCallback();
    
    public abstract void startMediaHandler(@org.jetbrains.annotations.Nullable()
    java.lang.String mediaHandlerId);
    
    public abstract void stopMediaHandler();
    
    public final void connectivityChanged(boolean isConnected) {
    }
    
    protected final void switchInput(@org.jetbrains.annotations.Nullable()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String uri) {
    }
    
    public final void setPreviewSettings(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends net.jami.daemon.StringMap> cameraMaps) {
    }
    
    public final long startVideo(@org.jetbrains.annotations.Nullable()
    java.lang.String inputId, @org.jetbrains.annotations.Nullable()
    java.lang.Object surface, int width, int height) {
        return 0L;
    }
    
    public final void stopVideo(@org.jetbrains.annotations.Nullable()
    java.lang.String inputId, long inputWindow) {
    }
    
    public abstract void setDeviceOrientation(int rotation);
    
    @org.jetbrains.annotations.Nullable()
    protected abstract java.util.List<java.lang.String> getVideoDevices();
    
    @kotlin.jvm.Synchronized()
    public final synchronized boolean isLogging() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    @kotlin.jvm.Synchronized()
    public final synchronized io.reactivex.rxjava3.core.Observable<java.lang.String> startLogs() {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void stopLogs() {
    }
    
    public final void logMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String message) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u000b\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002R\u001c\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001a\u0010\t\u001a\u00020\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001a\u0010\u000f\u001a\u00020\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\f\"\u0004\b\u0011\u0010\u000eR\u001a\u0010\u0012\u001a\u00020\u0013X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\u0015\"\u0004\b\u0016\u0010\u0017R\u001a\u0010\u0018\u001a\u00020\u0013X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0019\u0010\u0015\"\u0004\b\u001a\u0010\u0017R\u001a\u0010\u001b\u001a\u00020\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001c\u0010\f\"\u0004\b\u001d\u0010\u000e\u00a8\u0006\u001e"}, d2 = {"Lnet/jami/services/HardwareService$VideoEvent;", "", "()V", "callId", "", "getCallId", "()Ljava/lang/String;", "setCallId", "(Ljava/lang/String;)V", "h", "", "getH", "()I", "setH", "(I)V", "rot", "getRot", "setRot", "start", "", "getStart", "()Z", "setStart", "(Z)V", "started", "getStarted", "setStarted", "w", "getW", "setW", "libringclient"})
    public static final class VideoEvent {
        private boolean start = false;
        private boolean started = false;
        private int w = 0;
        private int h = 0;
        private int rot = 0;
        @org.jetbrains.annotations.Nullable()
        private java.lang.String callId;
        
        public VideoEvent() {
            super();
        }
        
        public final boolean getStart() {
            return false;
        }
        
        public final void setStart(boolean p0) {
        }
        
        public final boolean getStarted() {
            return false;
        }
        
        public final void setStarted(boolean p0) {
        }
        
        public final int getW() {
            return 0;
        }
        
        public final void setW(int p0) {
        }
        
        public final int getH() {
            return 0;
        }
        
        public final void setH(int p0) {
        }
        
        public final int getRot() {
            return 0;
        }
        
        public final void setRot(int p0) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getCallId() {
            return null;
        }
        
        public final void setCallId(@org.jetbrains.annotations.Nullable()
        java.lang.String p0) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\t"}, d2 = {"Lnet/jami/services/HardwareService$BluetoothEvent;", "", "()V", "connected", "", "getConnected", "()Z", "setConnected", "(Z)V", "libringclient"})
    public static final class BluetoothEvent {
        private boolean connected = false;
        
        public BluetoothEvent() {
            super();
        }
        
        public final boolean getConnected() {
            return false;
        }
        
        public final void setConnected(boolean p0) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lnet/jami/services/HardwareService$AudioOutput;", "", "(Ljava/lang/String;I)V", "INTERNAL", "SPEAKERS", "BLUETOOTH", "libringclient"})
    public static enum AudioOutput {
        /*public static final*/ INTERNAL /* = new INTERNAL() */,
        /*public static final*/ SPEAKERS /* = new SPEAKERS() */,
        /*public static final*/ BLUETOOTH /* = new BLUETOOTH() */;
        
        AudioOutput() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\u0018\u00002\u00020\u0001B\u000f\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004B\u0019\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\u0002\u0010\u0007R\u0013\u0010\b\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u000b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u000e"}, d2 = {"Lnet/jami/services/HardwareService$AudioState;", "", "ot", "Lnet/jami/services/HardwareService$AudioOutput;", "(Lnet/jami/services/HardwareService$AudioOutput;)V", "name", "", "(Lnet/jami/services/HardwareService$AudioOutput;Ljava/lang/String;)V", "outputName", "getOutputName", "()Ljava/lang/String;", "outputType", "getOutputType", "()Lnet/jami/services/HardwareService$AudioOutput;", "libringclient"})
    public static final class AudioState {
        @org.jetbrains.annotations.NotNull()
        private final net.jami.services.HardwareService.AudioOutput outputType = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String outputName = null;
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.services.HardwareService.AudioOutput getOutputType() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getOutputName() {
            return null;
        }
        
        public AudioState(@org.jetbrains.annotations.NotNull()
        net.jami.services.HardwareService.AudioOutput ot) {
            super();
        }
        
        public AudioState(@org.jetbrains.annotations.NotNull()
        net.jami.services.HardwareService.AudioOutput ot, @org.jetbrains.annotations.Nullable()
        java.lang.String name) {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0006R\u0016\u0010\t\u001a\n \u000b*\u0004\u0018\u00010\n0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lnet/jami/services/HardwareService$Companion;", "", "()V", "STATE_INTERNAL", "Lnet/jami/services/HardwareService$AudioState;", "getSTATE_INTERNAL", "()Lnet/jami/services/HardwareService$AudioState;", "STATE_SPEAKERS", "getSTATE_SPEAKERS", "TAG", "", "kotlin.jvm.PlatformType", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.services.HardwareService.AudioState getSTATE_SPEAKERS() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.services.HardwareService.AudioState getSTATE_INTERNAL() {
            return null;
        }
    }
}