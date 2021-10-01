package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\n\u0018\u0000  2\u00020\u0001:\b !\"#$%&\'B/\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0001\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u0006\u0010\u001d\u001a\u00020\u001eJ\u0006\u0010\u001f\u001a\u00020\u001eR\u001e\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\r\u001a\u00020\u000e@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0018\u00010\u0012R\u00020\u0000X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0013\u001a\b\u0018\u00010\u0014R\u00020\u0000X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0017\u001a\b\u0018\u00010\u0018R\u00020\u0000X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0019\u001a\b\u0018\u00010\u001aR\u00020\u0000X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001b\u001a\b\u0018\u00010\u001cR\u00020\u0000X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006("}, d2 = {"Lnet/jami/services/DaemonService;", "", "mSystemInfoCallbacks", "Lnet/jami/services/DaemonService$SystemInfoCallbacks;", "mExecutor", "Ljava/util/concurrent/ScheduledExecutorService;", "mCallService", "Lnet/jami/services/CallService;", "mHardwareService", "Lnet/jami/services/HardwareService;", "mAccountService", "Lnet/jami/services/AccountService;", "(Lnet/jami/services/DaemonService$SystemInfoCallbacks;Ljava/util/concurrent/ScheduledExecutorService;Lnet/jami/services/CallService;Lnet/jami/services/HardwareService;Lnet/jami/services/AccountService;)V", "<set-?>", "", "isStarted", "()Z", "mCallAndConferenceCallback", "Lnet/jami/services/DaemonService$DaemonCallAndConferenceCallback;", "mConfigurationCallback", "Lnet/jami/services/DaemonService$DaemonConfigurationCallback;", "mConversationCallback", "Lnet/jami/daemon/ConversationCallback;", "mDataCallback", "Lnet/jami/services/DaemonService$DaemonDataTransferCallback;", "mHardwareCallback", "Lnet/jami/services/DaemonService$DaemonVideoCallback;", "mPresenceCallback", "Lnet/jami/services/DaemonService$DaemonPresenceCallback;", "startDaemon", "", "stopDaemon", "Companion", "ConversationCallbackImpl", "DaemonCallAndConferenceCallback", "DaemonConfigurationCallback", "DaemonDataTransferCallback", "DaemonPresenceCallback", "DaemonVideoCallback", "SystemInfoCallbacks", "libringclient"})
public final class DaemonService {
    private final net.jami.services.DaemonService.SystemInfoCallbacks mSystemInfoCallbacks = null;
    private final java.util.concurrent.ScheduledExecutorService mExecutor = null;
    private final net.jami.services.CallService mCallService = null;
    private final net.jami.services.HardwareService mHardwareService = null;
    private final net.jami.services.AccountService mAccountService = null;
    private net.jami.services.DaemonService.DaemonVideoCallback mHardwareCallback;
    private net.jami.services.DaemonService.DaemonPresenceCallback mPresenceCallback;
    private net.jami.services.DaemonService.DaemonCallAndConferenceCallback mCallAndConferenceCallback;
    private net.jami.services.DaemonService.DaemonConfigurationCallback mConfigurationCallback;
    private net.jami.services.DaemonService.DaemonDataTransferCallback mDataCallback;
    private net.jami.daemon.ConversationCallback mConversationCallback;
    private boolean isStarted = false;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.DaemonService.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    public DaemonService(@org.jetbrains.annotations.NotNull()
    net.jami.services.DaemonService.SystemInfoCallbacks mSystemInfoCallbacks, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "DaemonExecutor")
    java.util.concurrent.ScheduledExecutorService mExecutor, @org.jetbrains.annotations.NotNull()
    net.jami.services.CallService mCallService, @org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService mHardwareService, @org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService) {
        super();
    }
    
    public final boolean isStarted() {
        return false;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void startDaemon() {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void stopDaemon() {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&J\u0010\u0010\b\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H&J\u0010\u0010\t\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\nH&\u00a8\u0006\u000b"}, d2 = {"Lnet/jami/services/DaemonService$SystemInfoCallbacks;", "", "getAppDataPath", "", "name", "", "ret", "Lnet/jami/daemon/StringVect;", "getDeviceName", "getHardwareAudioFormat", "Lnet/jami/daemon/IntVect;", "libringclient"})
    public static abstract interface SystemInfoCallbacks {
        
        public abstract void getHardwareAudioFormat(@org.jetbrains.annotations.NotNull()
        net.jami.daemon.IntVect ret);
        
        public abstract void getAppDataPath(@org.jetbrains.annotations.NotNull()
        java.lang.String name, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringVect ret);
        
        public abstract void getDeviceName(@org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringVect ret);
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0011\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0080\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016J0\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J \u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u00062\u0006\u0010\u0012\u001a\u00020\u0006H\u0016J\b\u0010\u0013\u001a\u00020\u0004H\u0016J(\u0010\u0014\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0015\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J \u0010\u0016\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u0017\u001a\u00020\u00062\u0006\u0010\u0018\u001a\u00020\u0019H\u0016J \u0010\u001a\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u0017\u001a\u00020\u00062\u0006\u0010\u001b\u001a\u00020\u0019H\u0016J \u0010\u001c\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u001d\u001a\u00020\u00062\u0006\u0010\u001e\u001a\u00020\u000fH\u0016J\u0010\u0010\u001f\u001a\u00020\u00042\u0006\u0010 \u001a\u00020\u000fH\u0016J \u0010!\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\"\u001a\u00020\u000f2\u0006\u0010#\u001a\u00020\u0006H\u0016J\u0018\u0010$\u001a\u00020\u00042\u0006\u0010\u0011\u001a\u00020\u00062\u0006\u0010%\u001a\u00020&H\u0016J\u0010\u0010\'\u001a\u00020\u00042\u0006\u0010%\u001a\u00020&H\u0016J\u0010\u0010(\u001a\u00020\u00042\u0006\u0010%\u001a\u00020)H\u0016J(\u0010*\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010+\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u00062\u0006\u0010,\u001a\u00020\bH\u0016J0\u0010-\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010+\u001a\u00020\u00062\u0006\u0010.\u001a\u00020/2\u0006\u00100\u001a\u000201H\u0016J\u0018\u00102\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u00103\u001a\u00020\bH\u0016J\u0010\u00104\u001a\u00020\u00042\u0006\u0010.\u001a\u00020\u0006H\u0016J\u0018\u00105\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u001e\u001a\u00020\u0006H\u0016J \u00106\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u001e\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u0006H\u0016J \u00107\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u00108\u001a\u00020\u00062\u0006\u00109\u001a\u00020\u0006H\u0016J(\u0010:\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u001e\u001a\u00020\u000f2\u0006\u0010;\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u0006H\u0016J(\u0010<\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010=\u001a\u00020\u00062\u0006\u0010\"\u001a\u00020\u000f2\u0006\u0010>\u001a\u00020\u0006H\u0016J\u0010\u0010?\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u0006H\u0016J(\u0010@\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u001e\u001a\u00020\u000f2\u0006\u0010A\u001a\u00020\u00062\u0006\u0010B\u001a\u00020CH\u0016J\u0018\u0010D\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016J\u0018\u0010E\u001a\u00020\u00042\u0006\u0010\u001d\u001a\u00020\u00062\u0006\u0010F\u001a\u00020\u000fH\u0016\u00a8\u0006G"}, d2 = {"Lnet/jami/services/DaemonService$DaemonConfigurationCallback;", "Lnet/jami/daemon/ConfigurationCallback;", "(Lnet/jami/services/DaemonService;)V", "accountDetailsChanged", "", "account_id", "", "details", "Lnet/jami/daemon/StringMap;", "accountMessageStatusChanged", "accountId", "conversationId", "peer", "messageId", "status", "", "accountProfileReceived", "name", "photo", "accountsChanged", "composingStatusChanged", "contactUri", "contactAdded", "uri", "confirmed", "", "contactRemoved", "banned", "deviceRevocationEnded", "device", "state", "errorAlert", "alert", "exportOnRingEnded", "code", "pin", "getAppDataPath", "ret", "Lnet/jami/daemon/StringVect;", "getDeviceName", "getHardwareAudioFormat", "Lnet/jami/daemon/IntVect;", "incomingAccountMessage", "from", "messages", "incomingTrustRequest", "message", "Lnet/jami/daemon/Blob;", "received", "", "knownDevicesChanged", "devices", "messageSend", "migrationEnded", "nameRegistrationEnded", "profileReceived", "peerId", "path", "registeredNameFound", "address", "registrationStateChanged", "newState", "detailString", "stunStatusFailure", "userSearchEnded", "query", "results", "Lnet/jami/daemon/VectMap;", "volatileAccountDetailsChanged", "volumeChanged", "value", "libringclient"})
    public final class DaemonConfigurationCallback extends net.jami.daemon.ConfigurationCallback {
        
        public DaemonConfigurationCallback() {
            super(0L, false);
        }
        
        @java.lang.Override()
        public void volumeChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String device, int value) {
        }
        
        @java.lang.Override()
        public void accountsChanged() {
        }
        
        @java.lang.Override()
        public void stunStatusFailure(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId) {
        }
        
        @java.lang.Override()
        public void registrationStateChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String newState, int code, @org.jetbrains.annotations.NotNull()
        java.lang.String detailString) {
        }
        
        @java.lang.Override()
        public void volatileAccountDetailsChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String account_id, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringMap details) {
        }
        
        @java.lang.Override()
        public void accountDetailsChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String account_id, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringMap details) {
        }
        
        @java.lang.Override()
        public void profileReceived(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String peerId, @org.jetbrains.annotations.NotNull()
        java.lang.String path) {
        }
        
        @java.lang.Override()
        public void accountProfileReceived(@org.jetbrains.annotations.NotNull()
        java.lang.String account_id, @org.jetbrains.annotations.NotNull()
        java.lang.String name, @org.jetbrains.annotations.NotNull()
        java.lang.String photo) {
        }
        
        @java.lang.Override()
        public void incomingAccountMessage(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String from, @org.jetbrains.annotations.NotNull()
        java.lang.String messageId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringMap messages) {
        }
        
        @java.lang.Override()
        public void accountMessageStatusChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        java.lang.String peer, @org.jetbrains.annotations.NotNull()
        java.lang.String messageId, int status) {
        }
        
        @java.lang.Override()
        public void composingStatusChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        java.lang.String contactUri, int status) {
        }
        
        @java.lang.Override()
        public void errorAlert(int alert) {
        }
        
        @java.lang.Override()
        public void getHardwareAudioFormat(@org.jetbrains.annotations.NotNull()
        net.jami.daemon.IntVect ret) {
        }
        
        @java.lang.Override()
        public void getAppDataPath(@org.jetbrains.annotations.NotNull()
        java.lang.String name, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringVect ret) {
        }
        
        @java.lang.Override()
        public void getDeviceName(@org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringVect ret) {
        }
        
        @java.lang.Override()
        public void knownDevicesChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringMap devices) {
        }
        
        @java.lang.Override()
        public void exportOnRingEnded(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, int code, @org.jetbrains.annotations.NotNull()
        java.lang.String pin) {
        }
        
        @java.lang.Override()
        public void nameRegistrationEnded(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, int state, @org.jetbrains.annotations.NotNull()
        java.lang.String name) {
        }
        
        @java.lang.Override()
        public void registeredNameFound(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, int state, @org.jetbrains.annotations.NotNull()
        java.lang.String address, @org.jetbrains.annotations.NotNull()
        java.lang.String name) {
        }
        
        @java.lang.Override()
        public void userSearchEnded(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, int state, @org.jetbrains.annotations.NotNull()
        java.lang.String query, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.VectMap results) {
        }
        
        @java.lang.Override()
        public void migrationEnded(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String state) {
        }
        
        @java.lang.Override()
        public void deviceRevocationEnded(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String device, int state) {
        }
        
        @java.lang.Override()
        public void incomingTrustRequest(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        java.lang.String from, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.Blob message, long received) {
        }
        
        @java.lang.Override()
        public void contactAdded(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String uri, boolean confirmed) {
        }
        
        @java.lang.Override()
        public void contactRemoved(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String uri, boolean banned) {
        }
        
        @java.lang.Override()
        public void messageSend(@org.jetbrains.annotations.NotNull()
        java.lang.String message) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0000\b\u0080\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J \u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\tH\u0016J\u0018\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\u0006H\u0016J\u0010\u0010\r\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u0006H\u0016J\u0010\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u0006H\u0016J\u0018\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\tH\u0016J \u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u0006H\u0016J \u0010\u0014\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0015\u001a\u00020\u0016H\u0016J\u0018\u0010\u0017\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0018\u001a\u00020\u0019H\u0016J\u0018\u0010\u001a\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u001b\u001a\u00020\u001cH\u0016J\u0018\u0010\u001d\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u001e\u001a\u00020\u0006H\u0016J \u0010\u001f\u001a\u00020\u00042\u0006\u0010 \u001a\u00020\u00062\u0006\u0010!\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\"H\u0016\u00a8\u0006#"}, d2 = {"Lnet/jami/services/DaemonService$DaemonCallAndConferenceCallback;", "Lnet/jami/daemon/Callback;", "(Lnet/jami/services/DaemonService;)V", "callStateChanged", "", "callId", "", "newState", "detailCode", "", "conferenceChanged", "confId", "state", "conferenceCreated", "conferenceRemoved", "connectionUpdate", "id", "incomingCall", "accountId", "from", "incomingMessage", "messages", "Lnet/jami/daemon/StringMap;", "onConferenceInfosUpdated", "infos", "Lnet/jami/daemon/VectMap;", "onRtcpReportReceived", "stats", "Lnet/jami/daemon/IntegerMap;", "recordPlaybackFilepath", "filename", "remoteRecordingChanged", "call_id", "peer_number", "", "libringclient"})
    public final class DaemonCallAndConferenceCallback extends net.jami.daemon.Callback {
        
        public DaemonCallAndConferenceCallback() {
            super(0L, false);
        }
        
        @java.lang.Override()
        public void callStateChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String callId, @org.jetbrains.annotations.NotNull()
        java.lang.String newState, int detailCode) {
        }
        
        @java.lang.Override()
        public void incomingCall(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String callId, @org.jetbrains.annotations.NotNull()
        java.lang.String from) {
        }
        
        @java.lang.Override()
        public void connectionUpdate(@org.jetbrains.annotations.NotNull()
        java.lang.String id, int state) {
        }
        
        @java.lang.Override()
        public void remoteRecordingChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String call_id, @org.jetbrains.annotations.NotNull()
        java.lang.String peer_number, boolean state) {
        }
        
        @java.lang.Override()
        public void onConferenceInfosUpdated(@org.jetbrains.annotations.NotNull()
        java.lang.String confId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.VectMap infos) {
        }
        
        @java.lang.Override()
        public void incomingMessage(@org.jetbrains.annotations.NotNull()
        java.lang.String callId, @org.jetbrains.annotations.NotNull()
        java.lang.String from, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringMap messages) {
        }
        
        @java.lang.Override()
        public void conferenceCreated(@org.jetbrains.annotations.NotNull()
        java.lang.String confId) {
        }
        
        @java.lang.Override()
        public void conferenceRemoved(@org.jetbrains.annotations.NotNull()
        java.lang.String confId) {
        }
        
        @java.lang.Override()
        public void conferenceChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String confId, @org.jetbrains.annotations.NotNull()
        java.lang.String state) {
        }
        
        @java.lang.Override()
        public void recordPlaybackFilepath(@org.jetbrains.annotations.NotNull()
        java.lang.String id, @org.jetbrains.annotations.NotNull()
        java.lang.String filename) {
        }
        
        @java.lang.Override()
        public void onRtcpReportReceived(@org.jetbrains.annotations.NotNull()
        java.lang.String callId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.IntegerMap stats) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\t\b\u0080\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J(\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0006H\u0016J\u0010\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u0006H\u0016J \u0010\r\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\u0006H\u0016J \u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\tH\u0016\u00a8\u0006\u0012"}, d2 = {"Lnet/jami/services/DaemonService$DaemonPresenceCallback;", "Lnet/jami/daemon/PresenceCallback;", "(Lnet/jami/services/DaemonService;)V", "newBuddyNotification", "", "accountId", "", "buddyUri", "status", "", "lineStatus", "newServerSubscriptionRequest", "remote", "serverError", "error", "message", "subscriptionStateChanged", "state", "libringclient"})
    public final class DaemonPresenceCallback extends net.jami.daemon.PresenceCallback {
        
        public DaemonPresenceCallback() {
            super(0L, false);
        }
        
        @java.lang.Override()
        public void newServerSubscriptionRequest(@org.jetbrains.annotations.NotNull()
        java.lang.String remote) {
        }
        
        @java.lang.Override()
        public void serverError(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String error, @org.jetbrains.annotations.NotNull()
        java.lang.String message) {
        }
        
        @java.lang.Override()
        public void newBuddyNotification(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String buddyUri, int status, @org.jetbrains.annotations.NotNull()
        java.lang.String lineStatus) {
        }
        
        @java.lang.Override()
        public void subscriptionStateChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String buddyUri, int state) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J0\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\fH\u0016J \u0010\r\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\fH\u0016J(\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u0013H\u0016J\b\u0010\u0015\u001a\u00020\u0004H\u0016J\u0018\u0010\u0016\u001a\u00020\u00042\u0006\u0010\u0017\u001a\u00020\u00062\u0006\u0010\u0018\u001a\u00020\tH\u0016J0\u0010\u0019\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00062\u0006\u0010\u001a\u001a\u00020\t2\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u001b\u001a\u00020\tH\u0016J\u0010\u0010\u001c\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0006H\u0016J\b\u0010\u001d\u001a\u00020\u0004H\u0016\u00a8\u0006\u001e"}, d2 = {"Lnet/jami/services/DaemonService$DaemonVideoCallback;", "Lnet/jami/daemon/VideoCallback;", "(Lnet/jami/services/DaemonService;)V", "decodingStarted", "", "id", "", "shmPath", "width", "", "height", "isMixer", "", "decodingStopped", "getCameraInfo", "camId", "formats", "Lnet/jami/daemon/IntVect;", "sizes", "Lnet/jami/daemon/UintVect;", "rates", "requestKeyFrame", "setBitrate", "device", "bitrate", "setParameters", "format", "rate", "startCapture", "stopCapture", "libringclient"})
    final class DaemonVideoCallback extends net.jami.daemon.VideoCallback {
        
        public DaemonVideoCallback() {
            super(0L, false);
        }
        
        @java.lang.Override()
        public void decodingStarted(@org.jetbrains.annotations.NotNull()
        java.lang.String id, @org.jetbrains.annotations.NotNull()
        java.lang.String shmPath, int width, int height, boolean isMixer) {
        }
        
        @java.lang.Override()
        public void decodingStopped(@org.jetbrains.annotations.NotNull()
        java.lang.String id, @org.jetbrains.annotations.NotNull()
        java.lang.String shmPath, boolean isMixer) {
        }
        
        @java.lang.Override()
        public void getCameraInfo(@org.jetbrains.annotations.NotNull()
        java.lang.String camId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.IntVect formats, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.UintVect sizes, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.UintVect rates) {
        }
        
        @java.lang.Override()
        public void setParameters(@org.jetbrains.annotations.NotNull()
        java.lang.String camId, int format, int width, int height, int rate) {
        }
        
        @java.lang.Override()
        public void requestKeyFrame() {
        }
        
        @java.lang.Override()
        public void setBitrate(@org.jetbrains.annotations.NotNull()
        java.lang.String device, int bitrate) {
        }
        
        @java.lang.Override()
        public void startCapture(@org.jetbrains.annotations.NotNull()
        java.lang.String camId) {
        }
        
        @java.lang.Override()
        public void stopCapture() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\b\u0080\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J0\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u000bH\u0016\u00a8\u0006\f"}, d2 = {"Lnet/jami/services/DaemonService$DaemonDataTransferCallback;", "Lnet/jami/daemon/DataTransferCallback;", "(Lnet/jami/services/DaemonService;)V", "dataTransferEvent", "", "accountId", "", "conversationId", "interactionId", "fileId", "eventCode", "", "libringclient"})
    public final class DaemonDataTransferCallback extends net.jami.daemon.DataTransferCallback {
        
        public DaemonDataTransferCallback() {
            super(0L, false);
        }
        
        @java.lang.Override()
        public void dataTransferEvent(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        java.lang.String interactionId, @org.jetbrains.annotations.NotNull()
        java.lang.String fileId, int eventCode) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0080\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J(\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u000bH\u0016J(\u0010\f\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\r\u001a\u00020\b2\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J\u0018\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\bH\u0016J\u0018\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\bH\u0016J\u0018\u0010\u0012\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\bH\u0016J \u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\u0014\u001a\u00020\u0015H\u0016J \u0010\u0016\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\u0017\u001a\u00020\u0015H\u0016\u00a8\u0006\u0018"}, d2 = {"Lnet/jami/services/DaemonService$ConversationCallbackImpl;", "Lnet/jami/daemon/ConversationCallback;", "(Lnet/jami/services/DaemonService;)V", "conversationLoaded", "", "id", "", "accountId", "", "conversationId", "messages", "Lnet/jami/daemon/VectMap;", "conversationMemberEvent", "uri", "event", "", "conversationReady", "conversationRemoved", "conversationRequestDeclined", "conversationRequestReceived", "metadata", "Lnet/jami/daemon/StringMap;", "messageReceived", "message", "libringclient"})
    public final class ConversationCallbackImpl extends net.jami.daemon.ConversationCallback {
        
        public ConversationCallbackImpl() {
            super(0L, false);
        }
        
        @java.lang.Override()
        public void conversationLoaded(long id, @org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.VectMap messages) {
        }
        
        @java.lang.Override()
        public void conversationReady(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId) {
        }
        
        @java.lang.Override()
        public void conversationRemoved(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId) {
        }
        
        @java.lang.Override()
        public void conversationRequestReceived(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringMap metadata) {
        }
        
        @java.lang.Override()
        public void conversationRequestDeclined(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId) {
        }
        
        @java.lang.Override()
        public void conversationMemberEvent(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        java.lang.String uri, int event) {
        }
        
        @java.lang.Override()
        public void messageReceived(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
        net.jami.daemon.StringMap message) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/services/DaemonService$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}