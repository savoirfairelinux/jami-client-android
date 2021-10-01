package net.jami.call;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u00aa\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0012\n\u0002\u0018\u0002\n\u0002\b\u0011\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0000\n\u0002\b\u000b\n\u0002\u0010\r\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0017\u0018\u0000 \u0084\u00012\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0002\u0084\u0001BA\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\b\b\u0001\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\u0002\u0010\u0011J\u0006\u00100\u001a\u000201J\u0016\u00102\u001a\u0002012\u0006\u00103\u001a\u00020\u00172\u0006\u00104\u001a\u000205J\u000e\u00106\u001a\u0002012\u0006\u00107\u001a\u00020\u0013J\u0010\u00108\u001a\u0002012\u0006\u00109\u001a\u00020\u0002H\u0016J\u000e\u0010:\u001a\u0002012\u0006\u00107\u001a\u00020\u0013J\u0006\u0010;\u001a\u000201J\u0010\u0010<\u001a\u0002012\u0006\u0010=\u001a\u00020!H\u0002J\u000e\u0010>\u001a\u0002012\u0006\u0010?\u001a\u00020+J\u0010\u0010@\u001a\u0002012\u0006\u0010A\u001a\u00020!H\u0002J\u0006\u0010B\u001a\u000201J\u0006\u0010C\u001a\u000201J\b\u0010D\u001a\u000201H\u0002J\u0006\u0010E\u001a\u000201J\u000e\u0010F\u001a\u0002012\u0006\u0010G\u001a\u00020HJ\u0016\u0010I\u001a\u0002012\u0006\u0010J\u001a\u00020\u00172\u0006\u0010K\u001a\u00020\u0013J,\u0010L\u001a\u0002012\b\u00103\u001a\u0004\u0018\u00010\u00172\b\u0010M\u001a\u0004\u0018\u0001052\b\u0010N\u001a\u0004\u0018\u00010\u00172\u0006\u0010O\u001a\u00020\u0013J\u000e\u0010P\u001a\u00020\u00132\u0006\u0010G\u001a\u00020HJ\u0006\u0010Q\u001a\u000201J\u0010\u0010R\u001a\u0002012\b\u0010G\u001a\u0004\u0018\u00010HJ\u000e\u0010S\u001a\u0002012\u0006\u0010T\u001a\u00020\u0013J\u0016\u0010U\u001a\u0002012\u0006\u0010G\u001a\u00020H2\u0006\u0010V\u001a\u00020\u0013J\u0006\u0010W\u001a\u000201J\u0010\u0010X\u001a\u0002012\u0006\u0010Y\u001a\u00020ZH\u0002J\u000e\u0010[\u001a\u0002012\u0006\u0010G\u001a\u00020HJ\u000e\u0010\\\u001a\u0002012\u0006\u0010]\u001a\u00020\u0013J\u0010\u0010^\u001a\u0002012\b\u0010_\u001a\u0004\u0018\u00010`J\u0006\u0010a\u001a\u000201J\u0010\u0010b\u001a\u0002012\b\u0010c\u001a\u0004\u0018\u00010\u0017J\u0006\u0010d\u001a\u000201J\u0006\u0010e\u001a\u000201J\u0010\u0010f\u001a\u0002012\b\u0010_\u001a\u0004\u0018\u00010`J\u0006\u0010g\u001a\u000201J\u0006\u0010h\u001a\u000201J\u0006\u0010i\u001a\u000201J\u000e\u0010j\u001a\u0002012\u0006\u0010k\u001a\u00020lJ\u0016\u0010m\u001a\u0002012\f\u0010A\u001a\b\u0012\u0004\u0012\u00020!0nH\u0002J\u000e\u0010o\u001a\u0002012\u0006\u0010T\u001a\u00020\u0013J\u0006\u0010p\u001a\u000201J\u0010\u0010q\u001a\u0002012\b\u0010r\u001a\u0004\u0018\u00010\u0017J\u0010\u0010s\u001a\u00020\u00132\b\u0010t\u001a\u0004\u0018\u00010`J\u0006\u0010u\u001a\u000201J\u0006\u0010v\u001a\u000201J\u0006\u0010w\u001a\u000201J\u0006\u0010x\u001a\u000201J\u0006\u0010y\u001a\u000201J\u0016\u0010z\u001a\u0002012\u0006\u0010{\u001a\u00020\u00172\u0006\u0010|\u001a\u00020\u0013J\u000e\u0010}\u001a\u0002012\u0006\u0010~\u001a\u00020\u0013J\b\u0010\u007f\u001a\u000201H\u0016J\t\u0010\u0080\u0001\u001a\u000201H\u0002J\u0011\u0010\u0081\u0001\u001a\u0002012\b\u0010_\u001a\u0004\u0018\u00010`J\u0007\u0010\u0082\u0001\u001a\u000201J\u0011\u0010\u0083\u0001\u001a\u0002012\b\u0010c\u001a\u0004\u0018\u00010\u0017R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u001b\u001a\u00020\u00132\u0006\u0010\u001a\u001a\u00020\u0013@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u001d\u001a\u00020\u00138F\u00a2\u0006\u0006\u001a\u0004\b\u001d\u0010\u001cR\u001e\u0010\u001e\u001a\u00020\u00132\u0006\u0010\u001a\u001a\u00020\u0013@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001cR\u0011\u0010\u001f\u001a\u00020\u00138F\u00a2\u0006\u0006\u001a\u0004\b\u001f\u0010\u001cR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010 \u001a\u0004\u0018\u00010!X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010#\u001a\b\u0012\u0004\u0012\u00020%0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010&\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020%0(0\'X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010)\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010*\u001a\u00020+X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010,\u001a\u00020+X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010-\u001a\u0004\u0018\u00010\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010.\u001a\u00020+X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010/\u001a\u00020+X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0085\u0001"}, d2 = {"Lnet/jami/call/CallPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/call/CallView;", "mAccountService", "Lnet/jami/services/AccountService;", "mContactService", "Lnet/jami/services/ContactService;", "mHardwareService", "Lnet/jami/services/HardwareService;", "mCallService", "Lnet/jami/services/CallService;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mConversationFacade", "Lnet/jami/services/ConversationFacade;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lnet/jami/services/ContactService;Lnet/jami/services/HardwareService;Lnet/jami/services/CallService;Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/ConversationFacade;Lio/reactivex/rxjava3/core/Scheduler;)V", "callInitialized", "", "contactDisposable", "Lio/reactivex/rxjava3/disposables/Disposable;", "currentPluginSurfaceId", "", "currentSurfaceId", "incomingIsFullIntent", "<set-?>", "isAudioOnly", "()Z", "isMicrophoneMuted", "isPipMode", "isSpeakerphoneOn", "mConference", "Lnet/jami/model/Conference;", "mOnGoingCall", "mPendingCalls", "", "Lnet/jami/model/Call;", "mPendingSubject", "Lio/reactivex/rxjava3/subjects/Subject;", "", "permissionChanged", "previewHeight", "", "previewWidth", "timeUpdateTask", "videoHeight", "videoWidth", "acceptCall", "", "addConferenceParticipant", "accountId", "uri", "Lnet/jami/model/Uri;", "audioPermissionChanged", "isGranted", "bindView", "view", "cameraPermissionChanged", "chatClick", "confUpdate", "call", "configurationChanged", "rotation", "contactUpdate", "conference", "dialpadClick", "displayChanged", "finish", "hangupCall", "hangupParticipant", "info", "Lnet/jami/model/Conference$ParticipantInfo;", "initIncomingCall", "confId", "actionViewOnly", "initOutGoing", "conversationUri", "contactUri", "audioOnly", "isMaximized", "layoutChanged", "maximizeParticipant", "muteMicrophoneToggled", "checked", "muteParticipant", "mute", "negativeButtonClicked", "onVideoEvent", "event", "Lnet/jami/services/HardwareService$VideoEvent;", "openParticipantContact", "pipModeChanged", "pip", "pluginSurfaceCreated", "holder", "", "pluginSurfaceDestroyed", "pluginSurfaceUpdateId", "newId", "positiveButtonClicked", "prepareOptionMenu", "previewVideoSurfaceCreated", "previewVideoSurfaceDestroyed", "refuseCall", "requestPipMode", "sendDtmf", "s", "", "showConference", "Lio/reactivex/rxjava3/core/Observable;", "speakerClick", "startAddParticipant", "startPlugin", "mediaHandlerId", "startScreenShare", "mediaProjection", "stopCapture", "stopPlugin", "stopScreenShare", "switchVideoInputClick", "toggleButtonClicked", "toggleCallMediaHandler", "id", "toggle", "uiVisibilityChanged", "displayed", "unbindView", "updateTime", "videoSurfaceCreated", "videoSurfaceDestroyed", "videoSurfaceUpdateId", "Companion", "libringclient"})
public final class CallPresenter extends net.jami.mvp.RootPresenter<net.jami.call.CallView> {
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.ContactService mContactService = null;
    private final net.jami.services.HardwareService mHardwareService = null;
    private final net.jami.services.CallService mCallService = null;
    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService = null;
    private final net.jami.services.ConversationFacade mConversationFacade = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    private net.jami.model.Conference mConference;
    private final java.util.List<net.jami.model.Call> mPendingCalls = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.List<net.jami.model.Call>> mPendingSubject = null;
    private boolean mOnGoingCall = false;
    private boolean isAudioOnly = true;
    private boolean permissionChanged = false;
    private boolean isPipMode = false;
    private boolean incomingIsFullIntent = true;
    private boolean callInitialized = false;
    private int videoWidth = -1;
    private int videoHeight = -1;
    private int previewWidth = -1;
    private int previewHeight = -1;
    private java.lang.String currentSurfaceId;
    private java.lang.String currentPluginSurfaceId;
    private io.reactivex.rxjava3.disposables.Disposable timeUpdateTask;
    private io.reactivex.rxjava3.disposables.Disposable contactDisposable;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.call.CallPresenter.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public CallPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.ContactService mContactService, @org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService mHardwareService, @org.jetbrains.annotations.NotNull()
    net.jami.services.CallService mCallService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.ConversationFacade mConversationFacade, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    public final boolean isAudioOnly() {
        return false;
    }
    
    public final boolean isPipMode() {
        return false;
    }
    
    public final void cameraPermissionChanged(boolean isGranted) {
    }
    
    public final void audioPermissionChanged(boolean isGranted) {
    }
    
    @java.lang.Override()
    public void unbindView() {
    }
    
    @java.lang.Override()
    public void bindView(@org.jetbrains.annotations.NotNull()
    net.jami.call.CallView view) {
    }
    
    public final void initOutGoing(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.Nullable()
    java.lang.String contactUri, boolean audioOnly) {
    }
    
    /**
     * Returns to or starts an incoming call
     *
     * @param confId         the call id
     * @param actionViewOnly true if only returning to call or if using full screen intent
     */
    public final void initIncomingCall(@org.jetbrains.annotations.NotNull()
    java.lang.String confId, boolean actionViewOnly) {
    }
    
    private final void showConference(io.reactivex.rxjava3.core.Observable<net.jami.model.Conference> conference) {
    }
    
    public final void prepareOptionMenu() {
    }
    
    public final void chatClick() {
    }
    
    public final boolean isSpeakerphoneOn() {
        return false;
    }
    
    public final void speakerClick(boolean checked) {
    }
    
    public final void muteMicrophoneToggled(boolean checked) {
    }
    
    public final boolean isMicrophoneMuted() {
        return false;
    }
    
    public final void switchVideoInputClick() {
    }
    
    public final void configurationChanged(int rotation) {
    }
    
    public final void dialpadClick() {
    }
    
    public final void acceptCall() {
    }
    
    public final void hangupCall() {
    }
    
    public final void refuseCall() {
    }
    
    public final void videoSurfaceCreated(@org.jetbrains.annotations.Nullable()
    java.lang.Object holder) {
    }
    
    public final void videoSurfaceUpdateId(@org.jetbrains.annotations.Nullable()
    java.lang.String newId) {
    }
    
    public final void pluginSurfaceCreated(@org.jetbrains.annotations.Nullable()
    java.lang.Object holder) {
    }
    
    public final void pluginSurfaceUpdateId(@org.jetbrains.annotations.Nullable()
    java.lang.String newId) {
    }
    
    public final void previewVideoSurfaceCreated(@org.jetbrains.annotations.Nullable()
    java.lang.Object holder) {
    }
    
    public final void videoSurfaceDestroyed() {
    }
    
    public final void pluginSurfaceDestroyed() {
    }
    
    public final void previewVideoSurfaceDestroyed() {
    }
    
    public final void displayChanged() {
    }
    
    public final void layoutChanged() {
    }
    
    public final void uiVisibilityChanged(boolean displayed) {
    }
    
    private final void finish() {
    }
    
    private final void contactUpdate(net.jami.model.Conference conference) {
    }
    
    private final void confUpdate(net.jami.model.Conference call) {
    }
    
    public final void maximizeParticipant(@org.jetbrains.annotations.Nullable()
    net.jami.model.Conference.ParticipantInfo info) {
    }
    
    private final void updateTime() {
    }
    
    private final void onVideoEvent(net.jami.services.HardwareService.VideoEvent event) {
    }
    
    public final void positiveButtonClicked() {
    }
    
    public final void negativeButtonClicked() {
    }
    
    public final void toggleButtonClicked() {
    }
    
    public final void requestPipMode() {
    }
    
    public final void pipModeChanged(boolean pip) {
    }
    
    public final void toggleCallMediaHandler(@org.jetbrains.annotations.NotNull()
    java.lang.String id, boolean toggle) {
    }
    
    public final void sendDtmf(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence s) {
    }
    
    public final void addConferenceParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
    }
    
    public final void startAddParticipant() {
    }
    
    public final void hangupParticipant(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conference.ParticipantInfo info) {
    }
    
    public final void muteParticipant(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conference.ParticipantInfo info, boolean mute) {
    }
    
    public final void openParticipantContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conference.ParticipantInfo info) {
    }
    
    public final void stopCapture() {
    }
    
    public final boolean startScreenShare(@org.jetbrains.annotations.Nullable()
    java.lang.Object mediaProjection) {
        return false;
    }
    
    public final void stopScreenShare() {
    }
    
    public final boolean isMaximized(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conference.ParticipantInfo info) {
        return false;
    }
    
    public final void startPlugin(@org.jetbrains.annotations.Nullable()
    java.lang.String mediaHandlerId) {
    }
    
    public final void stopPlugin() {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/call/CallPresenter$Companion;", "", "()V", "TAG", "", "getTAG", "()Ljava/lang/String;", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTAG() {
            return null;
        }
    }
}