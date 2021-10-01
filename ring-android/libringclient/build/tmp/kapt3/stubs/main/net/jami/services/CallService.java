package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0098\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010%\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0013\n\u0002\u0010 \n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b2\u0018\u0000 \u0095\u00012\u00020\u0001:\u0004\u0095\u0001\u0096\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u000e\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J(\u0010-\u001a\u00020\u000b2\u0006\u0010.\u001a\u00020\u00142\u0006\u0010,\u001a\u00020\u00142\u0006\u0010/\u001a\u0002002\u0006\u00101\u001a\u000202H\u0002J\u0010\u00103\u001a\u00020\u00192\u0006\u00104\u001a\u00020\u000bH\u0002J\u000e\u00105\u001a\u00020+2\u0006\u00106\u001a\u00020\u0014J\u0016\u00107\u001a\u00020+2\u0006\u0010,\u001a\u00020\u00142\u0006\u00106\u001a\u00020\u0014J\u0016\u00108\u001a\u00020+2\u0006\u00109\u001a\u00020\u00142\u0006\u0010:\u001a\u00020\u0014J\u001e\u0010;\u001a\u00020+2\u0006\u0010,\u001a\u00020\u00142\u0006\u0010<\u001a\u00020\u00142\u0006\u0010=\u001a\u00020>J\u0016\u0010?\u001a\u00020@2\u0006\u0010.\u001a\u00020\u00142\u0006\u0010A\u001a\u00020BJ\u0016\u0010C\u001a\u00020+2\u0006\u00106\u001a\u00020\u00142\u0006\u0010D\u001a\u00020\u0014J\u000e\u0010E\u001a\u00020+2\u0006\u00106\u001a\u00020\u0014J\u000e\u0010F\u001a\u00020+2\u0006\u00106\u001a\u00020\u0014J\u0018\u0010G\u001a\u00020+2\b\u0010H\u001a\u0004\u0018\u00010\u00142\u0006\u0010D\u001a\u00020>J\u000e\u0010I\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J\u001c\u0010J\u001a\u0010\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u0014\u0018\u00010\u00132\u0006\u0010,\u001a\u00020\u0014J\u0016\u0010K\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f2\u0006\u00104\u001a\u00020\u000bH\u0002J\u0016\u0010L\u001a\b\u0012\u0004\u0012\u00020\u00190\u000f2\u0006\u0010M\u001a\u00020\u0019H\u0002J\u0014\u0010N\u001a\b\u0012\u0004\u0012\u00020\u00190\u000f2\u0006\u00106\u001a\u00020\u0014J\u0014\u0010N\u001a\b\u0012\u0004\u0012\u00020\u00190\u000f2\u0006\u00104\u001a\u00020\u000bJ\u0016\u0010N\u001a\b\u0012\u0004\u0012\u00020\u00190\u000f2\u0006\u0010O\u001a\u00020\u0019H\u0002J\u0010\u0010P\u001a\u0004\u0018\u00010\u00192\u0006\u0010H\u001a\u00020\u0014J\u000e\u0010P\u001a\u00020\u00192\u0006\u00104\u001a\u00020\u000bJ\u001c\u0010Q\u001a\u0010\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u0014\u0018\u00010\u00132\u0006\u0010H\u001a\u00020\u0014J\u000e\u0010R\u001a\u00020\u00142\u0006\u0010,\u001a\u00020\u0014J\u0010\u0010S\u001a\u0004\u0018\u00010\u00142\u0006\u0010,\u001a\u00020\u0014J\u0012\u0010T\u001a\u0004\u0018\u00010\u000b2\u0006\u0010,\u001a\u00020\u0014H\u0002J\u0018\u0010U\u001a\n\u0012\u0004\u0012\u00020\u0014\u0018\u00010V2\b\u00106\u001a\u0004\u0018\u00010\u0014J\u000e\u0010W\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J\u000e\u0010X\u001a\u00020+2\u0006\u00106\u001a\u00020\u0014J\u0018\u0010Y\u001a\u00020+2\b\u00106\u001a\u0004\u0018\u00010\u00142\u0006\u0010Z\u001a\u00020\u0014J\u000e\u0010[\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J\u000e\u0010\\\u001a\u00020+2\u0006\u00106\u001a\u00020\u0014J\u001e\u0010]\u001a\u00020+2\u0006\u0010.\u001a\u00020\u00142\u0006\u0010,\u001a\u00020\u00142\u0006\u0010/\u001a\u00020\u0014J*\u0010^\u001a\u00020+2\u0006\u0010,\u001a\u00020\u00142\u0006\u0010/\u001a\u00020\u00142\u0012\u0010_\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00140\u0013J\u000e\u0010`\u001a\u00020#2\u0006\u0010,\u001a\u00020\u0014J\u0016\u0010a\u001a\u00020+2\u0006\u0010b\u001a\u00020\u00142\u0006\u0010c\u001a\u00020\u0014J\u001c\u0010d\u001a\b\u0012\u0004\u0012\u00020#0e2\u0006\u0010f\u001a\u00020\u00142\u0006\u0010g\u001a\u00020\u0014J\u001e\u0010h\u001a\u00020+2\u0006\u00106\u001a\u00020\u00142\u0006\u0010Z\u001a\u00020\u00142\u0006\u0010i\u001a\u00020#J\u000e\u0010j\u001a\u00020+2\u0006\u0010i\u001a\u00020#J(\u0010k\u001a\u00020+2\u0006\u00106\u001a\u00020\u00142\u0018\u0010l\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00140\u00130VJ\u000e\u0010m\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J\u001a\u0010n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010,\u001a\u00020\u00142\u0006\u0010<\u001a\u00020\u0014H\u0002J.\u0010o\u001a\b\u0012\u0004\u0012\u00020\u000b0e2\u0006\u0010p\u001a\u00020\u00142\b\u0010q\u001a\u0004\u0018\u0001002\u0006\u0010r\u001a\u0002002\u0006\u0010s\u001a\u00020#J.\u0010t\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f2\u0006\u0010.\u001a\u00020\u00142\b\u0010q\u001a\u0004\u0018\u0001002\u0006\u0010r\u001a\u0002002\u0006\u0010s\u001a\u00020#J\u000e\u0010u\u001a\u00020+2\u0006\u0010v\u001a\u00020\u0014J\u0016\u0010w\u001a\u00020+2\u0006\u0010H\u001a\u00020\u00142\u0006\u0010x\u001a\u00020\u0014J\u000e\u0010y\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J\u001e\u0010z\u001a\u00020+2\u0006\u0010,\u001a\u00020\u00142\u0006\u0010{\u001a\u0002002\u0006\u0010D\u001a\u00020#J\u000e\u0010|\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J\u000e\u0010}\u001a\u00020+2\u0006\u00106\u001a\u00020\u0014J\u0006\u0010~\u001a\u00020+J&\u0010\u007f\u001a\b\u0012\u0004\u0012\u00020B0e2\u0006\u0010.\u001a\u00020\u00142\u0007\u0010\u0080\u0001\u001a\u00020\u00142\u0007\u0010\u0081\u0001\u001a\u00020\u0014J\u0018\u0010\u0082\u0001\u001a\u00020+2\u0006\u0010,\u001a\u00020\u00142\u0007\u0010\u0081\u0001\u001a\u00020\u0014J\u0010\u0010\u0083\u0001\u001a\u00020+2\u0007\u0010\u0084\u0001\u001a\u00020\u0014J\u0011\u0010\u0085\u0001\u001a\u00020+2\b\u00106\u001a\u0004\u0018\u00010\u0014J\u0018\u0010\u0086\u0001\u001a\u00020+2\u0006\u00106\u001a\u00020\u00142\u0007\u0010\u0087\u0001\u001a\u000200J%\u0010\u0088\u0001\u001a\u00020+2\b\u0010.\u001a\u0004\u0018\u00010\u00142\t\u0010\u0087\u0001\u001a\u0004\u0018\u00010\u00142\u0007\u0010\u0089\u0001\u001a\u00020#J \u0010\u008a\u0001\u001a\u00020+2\u0006\u0010,\u001a\u00020\u00142\u0007\u0010\u008b\u0001\u001a\u00020\u00142\u0006\u0010i\u001a\u00020#J\u000f\u0010\u008c\u0001\u001a\u00020+2\u0006\u0010i\u001a\u00020#J\u0010\u0010\u008d\u0001\u001a\u00020#2\u0007\u0010\u008e\u0001\u001a\u00020\u0014J\u0007\u0010\u008f\u0001\u001a\u00020+J\u000f\u0010\u0090\u0001\u001a\u00020#2\u0006\u0010H\u001a\u00020\u0014J\u0018\u0010\u0091\u0001\u001a\u00020+2\u0006\u0010,\u001a\u00020\u00142\u0007\u0010\u0080\u0001\u001a\u00020\u0014J\u000f\u0010\u0092\u0001\u001a\u00020+2\u0006\u0010,\u001a\u00020\u0014J\u000f\u0010\u0093\u0001\u001a\u00020+2\u0006\u00106\u001a\u00020\u0014J\t\u0010\u0094\u0001\u001a\u00020+H\u0002R<\u0010\t\u001a0\u0012\f\u0012\n \f*\u0004\u0018\u00010\u000b0\u000b \f*\u0017\u0012\f\u0012\n \f*\u0004\u0018\u00010\u000b0\u000b\u0018\u00010\n\u00a2\u0006\u0002\b\r0\n\u00a2\u0006\u0002\b\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\u0011R%\u0010\u0012\u001a\u0016\u0012\u0004\u0012\u00020\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u0015\u0018\u00010\u00138F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u0017R<\u0010\u0018\u001a0\u0012\f\u0012\n \f*\u0004\u0018\u00010\u00190\u0019 \f*\u0017\u0012\f\u0012\n \f*\u0004\u0018\u00010\u00190\u0019\u0018\u00010\n\u00a2\u0006\u0002\b\r0\n\u00a2\u0006\u0002\b\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00190\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\u001b\u0010\u0011R\u0013\u0010\u001c\u001a\u0004\u0018\u00010\u00148F\u00a2\u0006\u0006\u001a\u0004\b\u001d\u0010\u001eR\u001a\u0010\u001f\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u000b0 X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010!\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00190 X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\"\u001a\u00020#8F\u00a2\u0006\u0006\u001a\u0004\b\"\u0010$R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R(\u0010&\u001a\u0004\u0018\u00010\u00142\b\u0010%\u001a\u0004\u0018\u00010\u00148F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b\'\u0010\u001e\"\u0004\b(\u0010)\u00a8\u0006\u0097\u0001"}, d2 = {"Lnet/jami/services/CallService;", "", "mExecutor", "Ljava/util/concurrent/ScheduledExecutorService;", "mContactService", "Lnet/jami/services/ContactService;", "mAccountService", "Lnet/jami/services/AccountService;", "(Ljava/util/concurrent/ScheduledExecutorService;Lnet/jami/services/ContactService;Lnet/jami/services/AccountService;)V", "callSubject", "Lio/reactivex/rxjava3/subjects/PublishSubject;", "Lnet/jami/model/Call;", "kotlin.jvm.PlatformType", "Lio/reactivex/rxjava3/annotations/NonNull;", "callsUpdates", "Lio/reactivex/rxjava3/core/Observable;", "getCallsUpdates", "()Lio/reactivex/rxjava3/core/Observable;", "conferenceList", "", "", "Ljava/util/ArrayList;", "getConferenceList", "()Ljava/util/Map;", "conferenceSubject", "Lnet/jami/model/Conference;", "confsUpdates", "getConfsUpdates", "currentAudioOutputPlugin", "getCurrentAudioOutputPlugin", "()Ljava/lang/String;", "currentCalls", "", "currentConferences", "isCaptureMuted", "", "()Z", "path", "recordPath", "getRecordPath", "setRecordPath", "(Ljava/lang/String;)V", "accept", "", "callId", "addCall", "accountId", "from", "Lnet/jami/model/Uri;", "direction", "Lnet/jami/model/Call$Direction;", "addConference", "call", "addMainParticipant", "confId", "addParticipant", "attendedTransfer", "transferId", "targetID", "callStateChanged", "newState", "detailCode", "", "cancelMessage", "Lio/reactivex/rxjava3/core/Completable;", "messageID", "", "conferenceChanged", "state", "conferenceCreated", "conferenceRemoved", "connectionUpdate", "id", "detachParticipant", "getCallDetails", "getCallUpdates", "getConfCallUpdates", "conf", "getConfUpdates", "conference", "getConference", "getConferenceDetails", "getConferenceId", "getConferenceState", "getCurrentCallForId", "getParticipantList", "", "hangUp", "hangUpConference", "hangupParticipant", "peerId", "hold", "holdConference", "incomingCall", "incomingMessage", "messages", "isConferenceParticipant", "joinConference", "selConfId", "dragConfId", "joinParticipant", "Lio/reactivex/rxjava3/core/Single;", "selCallId", "dragCallId", "muteParticipant", "mute", "muteRingTone", "onConferenceInfoUpdated", "info", "onRtcpReportReceived", "parseCallState", "placeCall", "account", "conversationUri", "number", "audioOnly", "placeCallObservable", "playDtmf", "key", "recordPlaybackFilepath", "filename", "refuse", "remoteRecordingChanged", "peerNumber", "removeCallForId", "removeConference", "restartAudioLayer", "sendAccountTextMessage", "to", "msg", "sendTextMessage", "setAudioPlugin", "audioPlugin", "setConfGridLayout", "setConfMaximizedParticipant", "uri", "setIsComposing", "isComposing", "setLocalMediaMuted", "mediaType", "setMuted", "startRecordedFilePlayback", "filepath", "stopRecordedFilePlayback", "toggleRecordingCall", "transfer", "unhold", "unholdConference", "updateConnectionCount", "Companion", "ConferenceEntity", "libringclient"})
public final class CallService {
    private final java.util.concurrent.ScheduledExecutorService mExecutor = null;
    private final net.jami.services.ContactService mContactService = null;
    private final net.jami.services.AccountService mAccountService = null;
    private final java.util.Map<java.lang.String, net.jami.model.Call> currentCalls = null;
    private final java.util.Map<java.lang.String, net.jami.model.Conference> currentConferences = null;
    private final io.reactivex.rxjava3.subjects.PublishSubject<net.jami.model.Call> callSubject = null;
    private final io.reactivex.rxjava3.subjects.PublishSubject<net.jami.model.Conference> conferenceSubject = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.CallService.Companion Companion = null;
    private static final java.lang.String TAG = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String MIME_TEXT_PLAIN = "text/plain";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String MIME_GEOLOCATION = "application/geo";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String MEDIA_TYPE_AUDIO = "MEDIA_TYPE_AUDIO";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String MEDIA_TYPE_VIDEO = "MEDIA_TYPE_VIDEO";
    
    public CallService(@org.jetbrains.annotations.NotNull()
    java.util.concurrent.ScheduledExecutorService mExecutor, @org.jetbrains.annotations.NotNull()
    net.jami.services.ContactService mContactService, @org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Conference> getConfsUpdates() {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Conference> getConfCallUpdates(net.jami.model.Conference conf) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Conference> getConfUpdates(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
        return null;
    }
    
    private final void updateConnectionCount() {
    }
    
    public final void setIsComposing(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    java.lang.String uri, boolean isComposing) {
    }
    
    public final void onConferenceInfoUpdated(@org.jetbrains.annotations.NotNull()
    java.lang.String confId, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> info) {
    }
    
    public final void setConfMaximizedParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String confId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
    }
    
    public final void setConfGridLayout(@org.jetbrains.annotations.Nullable()
    java.lang.String confId) {
    }
    
    public final void remoteRecordingChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri peerNumber, boolean state) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Conference> getConfUpdates(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call call) {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Conference> getConfUpdates(net.jami.model.Conference conference) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Call> getCallsUpdates() {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Call> getCallUpdates(net.jami.model.Call call) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Call> placeCallObservable(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri number, boolean audioOnly) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Call> placeCall(@org.jetbrains.annotations.NotNull()
    java.lang.String account, @org.jetbrains.annotations.Nullable()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri number, boolean audioOnly) {
        return null;
    }
    
    public final void refuse(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    public final void accept(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    public final void hangUp(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    public final void muteParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String confId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerId, boolean mute) {
    }
    
    public final void hangupParticipant(@org.jetbrains.annotations.Nullable()
    java.lang.String confId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
    }
    
    public final void hold(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    public final void unhold(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Map<java.lang.String, java.lang.String> getCallDetails(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
        return null;
    }
    
    public final void muteRingTone(boolean mute) {
    }
    
    public final void restartAudioLayer() {
    }
    
    public final void setAudioPlugin(@org.jetbrains.annotations.NotNull()
    java.lang.String audioPlugin) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCurrentAudioOutputPlugin() {
        return null;
    }
    
    public final void playDtmf(@org.jetbrains.annotations.NotNull()
    java.lang.String key) {
    }
    
    public final void setMuted(boolean mute) {
    }
    
    public final void setLocalMediaMuted(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String mediaType, boolean mute) {
    }
    
    public final boolean isCaptureMuted() {
        return false;
    }
    
    public final void transfer(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String to) {
    }
    
    public final void attendedTransfer(@org.jetbrains.annotations.NotNull()
    java.lang.String transferId, @org.jetbrains.annotations.NotNull()
    java.lang.String targetID) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getRecordPath() {
        return null;
    }
    
    public final void setRecordPath(@org.jetbrains.annotations.Nullable()
    java.lang.String path) {
    }
    
    public final boolean toggleRecordingCall(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
        return false;
    }
    
    public final boolean startRecordedFilePlayback(@org.jetbrains.annotations.NotNull()
    java.lang.String filepath) {
        return false;
    }
    
    public final void stopRecordedFilePlayback() {
    }
    
    public final void sendTextMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String msg) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.lang.Long> sendAccountTextMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String to, @org.jetbrains.annotations.NotNull()
    java.lang.String msg) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable cancelMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, long messageID) {
        return null;
    }
    
    private final net.jami.model.Call getCurrentCallForId(java.lang.String callId) {
        return null;
    }
    
    public final void removeCallForId(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    private final net.jami.model.Call addCall(java.lang.String accountId, java.lang.String callId, net.jami.model.Uri from, net.jami.model.Call.Direction direction) {
        return null;
    }
    
    private final net.jami.model.Conference addConference(net.jami.model.Call call) {
        return null;
    }
    
    private final net.jami.model.Call parseCallState(java.lang.String callId, java.lang.String newState) {
        return null;
    }
    
    public final void connectionUpdate(@org.jetbrains.annotations.Nullable()
    java.lang.String id, int state) {
    }
    
    public final void callStateChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String newState, int detailCode) {
    }
    
    public final void incomingCall(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String from) {
    }
    
    public final void incomingMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String from, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> messages) {
    }
    
    public final void recordPlaybackFilepath(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String filename) {
    }
    
    public final void onRtcpReportReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    public final void removeConference(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.lang.Boolean> joinParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String selCallId, @org.jetbrains.annotations.NotNull()
    java.lang.String dragCallId) {
        return null;
    }
    
    public final void addParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    public final void addMainParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    public final void detachParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
    }
    
    public final void joinConference(@org.jetbrains.annotations.NotNull()
    java.lang.String selConfId, @org.jetbrains.annotations.NotNull()
    java.lang.String dragConfId) {
    }
    
    public final void hangUpConference(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    public final void holdConference(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    public final void unholdConference(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    public final boolean isConferenceParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Map<java.lang.String, java.util.ArrayList<java.lang.String>> getConferenceList() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.List<java.lang.String> getParticipantList(@org.jetbrains.annotations.Nullable()
    java.lang.String confId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Conference getConference(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call call) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getConferenceId(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getConferenceState(@org.jetbrains.annotations.NotNull()
    java.lang.String callId) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Conference getConference(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Map<java.lang.String, java.lang.String> getConferenceDetails(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
        return null;
    }
    
    public final void conferenceCreated(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    public final void conferenceRemoved(@org.jetbrains.annotations.NotNull()
    java.lang.String confId) {
    }
    
    public final void conferenceChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String confId, @org.jetbrains.annotations.NotNull()
    java.lang.String state) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0002\u0018\u00002\u00020\u0001B\u000f\b\u0000\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u001a\u0010\u0002\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\u0004\u00a8\u0006\b"}, d2 = {"Lnet/jami/services/CallService$ConferenceEntity;", "", "conference", "Lnet/jami/model/Conference;", "(Lnet/jami/model/Conference;)V", "getConference", "()Lnet/jami/model/Conference;", "setConference", "libringclient"})
    static final class ConferenceEntity {
        @org.jetbrains.annotations.NotNull()
        private net.jami.model.Conference conference;
        
        public ConferenceEntity(@org.jetbrains.annotations.NotNull()
        net.jami.model.Conference conference) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Conference getConference() {
            return null;
        }
        
        public final void setConference(@org.jetbrains.annotations.NotNull()
        net.jami.model.Conference p0) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lnet/jami/services/CallService$Companion;", "", "()V", "MEDIA_TYPE_AUDIO", "", "MEDIA_TYPE_VIDEO", "MIME_GEOLOCATION", "MIME_TEXT_PLAIN", "TAG", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}