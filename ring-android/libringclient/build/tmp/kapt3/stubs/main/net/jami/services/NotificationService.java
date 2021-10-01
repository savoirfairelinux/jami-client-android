package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u0000 32\u00020\u0001:\u00013J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\u0006H&J\u0018\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH&J\u0018\u0010\r\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H&J\u0018\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0010\u001a\u00020\u0015H&J\u0010\u0010\u0016\u001a\u00020\u00062\u0006\u0010\u0017\u001a\u00020\u0014H&J\u0010\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0019\u001a\u00020\nH&J\u0018\u0010\u001a\u001a\u00020\u00062\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\fH&J \u0010\u001e\u001a\u00020\u00062\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010\u0010\u001a\u00020!2\u0006\u0010\u001d\u001a\u00020\fH&J\u0010\u0010\"\u001a\u00020\u00062\u0006\u0010#\u001a\u00020\fH&J \u0010$\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010%\u001a\u00020\u00152\u0006\u0010&\u001a\u00020\u0014H&J\u0012\u0010\'\u001a\u0004\u0018\u00010\u00012\u0006\u0010(\u001a\u00020\nH&J\u0018\u0010)\u001a\u00020\u00062\u0006\u0010*\u001a\u00020!2\u0006\u0010+\u001a\u00020 H&J\u0010\u0010,\u001a\u00020\u00062\u0006\u0010-\u001a\u00020\u000fH&J\u0018\u0010.\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H&J\u0010\u0010/\u001a\u00020\u00062\u0006\u00100\u001a\u000201H&J\u0018\u00102\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010*\u001a\u00020!H&R\u0012\u0010\u0002\u001a\u00020\u0001X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0003\u0010\u0004\u00a8\u00064"}, d2 = {"Lnet/jami/services/NotificationService;", "", "serviceNotification", "getServiceNotification", "()Ljava/lang/Object;", "cancelAll", "", "cancelCallNotification", "cancelFileNotification", "id", "", "isMigratingToService", "", "cancelLocationNotification", "first", "Lnet/jami/model/Account;", "contact", "Lnet/jami/model/Contact;", "cancelTextNotification", "accountId", "", "Lnet/jami/model/Uri;", "cancelTrustRequestNotification", "accountID", "getDataTransferNotification", "notificationId", "handleCallNotification", "conference", "Lnet/jami/model/Conference;", "remove", "handleDataTransferNotification", "transfer", "Lnet/jami/model/DataTransfer;", "Lnet/jami/model/Conversation;", "onConnectionUpdate", "b", "removeTransferNotification", "conversationUri", "fileId", "showCallNotification", "callId", "showFileTransferNotification", "conversation", "info", "showIncomingTrustRequestNotification", "account", "showLocationNotification", "showMissedCallNotification", "call", "Lnet/jami/model/Call;", "showTextNotification", "Companion", "libringclient"})
public abstract interface NotificationService {
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.NotificationService.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID = "trustRequestNotificationAccountId";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TRUST_REQUEST_NOTIFICATION_FROM = "trustRequestNotificationFrom";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_CALL_ID = "callId";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_HOLD_ID = "holdId";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_END_ID = "endId";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_NOTIFICATION_ID = "notificationId";
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object showCallNotification(int callId);
    
    public abstract void cancelCallNotification();
    
    public abstract void handleCallNotification(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conference conference, boolean remove);
    
    public abstract void showMissedCallNotification(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call call);
    
    public abstract void showTextNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation);
    
    public abstract void cancelTextNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contact);
    
    public abstract void cancelAll();
    
    public abstract void showIncomingTrustRequestNotification(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account);
    
    public abstract void cancelTrustRequestNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String accountID);
    
    public abstract void showFileTransferNotification(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer info);
    
    public abstract void cancelFileNotification(int id, boolean isMigratingToService);
    
    public abstract void handleDataTransferNotification(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer, @org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation contact, boolean remove);
    
    public abstract void removeTransferNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    java.lang.String fileId);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.Object getDataTransferNotification(int notificationId);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.Object getServiceNotification();
    
    public abstract void onConnectionUpdate(boolean b);
    
    public abstract void showLocationNotification(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account first, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact);
    
    public abstract void cancelLocationNotification(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account first, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact);
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lnet/jami/services/NotificationService$Companion;", "", "()V", "KEY_CALL_ID", "", "KEY_END_ID", "KEY_HOLD_ID", "KEY_NOTIFICATION_ID", "TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID", "TRUST_REQUEST_NOTIFICATION_FROM", "libringclient"})
    public static final class Companion {
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID = "trustRequestNotificationAccountId";
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String TRUST_REQUEST_NOTIFICATION_FROM = "trustRequestNotificationFrom";
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String KEY_CALL_ID = "callId";
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String KEY_HOLD_ID = "holdId";
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String KEY_END_ID = "endId";
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String KEY_NOTIFICATION_ID = "notificationId";
        
        private Companion() {
            super();
        }
    }
}