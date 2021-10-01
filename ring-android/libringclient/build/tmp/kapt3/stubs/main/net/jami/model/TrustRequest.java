package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\u0018\u0000 /2\u00020\u0001:\u0001/B3\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\nB#\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0012\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\f\u00a2\u0006\u0002\u0010\rB!\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u000e\u001a\u00020\u0005\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u000fJ\u0010\u0010,\u001a\u00020-2\b\u0010.\u001a\u0004\u0018\u00010\u0003R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u001c\u0010\t\u001a\u0004\u0018\u00010\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0011\"\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0015\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u0011R\u0011\u0010\u0017\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\b\u0018\u0010\u0011R\u001e\u0010\u001b\u001a\u00020\u001a2\u0006\u0010\u0019\u001a\u00020\u001a@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u001e\u001a\u0004\u0018\u00010\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001f\u0010\u0011\"\u0004\b \u0010\u0014R\u0011\u0010!\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010%R\u001c\u0010&\u001a\u0004\u0018\u00010\'X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b(\u0010)\"\u0004\b*\u0010+\u00a8\u00060"}, d2 = {"Lnet/jami/model/TrustRequest;", "", "accountId", "", "uri", "Lnet/jami/model/Uri;", "received", "", "payload", "conversationId", "(Ljava/lang/String;Lnet/jami/model/Uri;JLjava/lang/String;Ljava/lang/String;)V", "info", "", "(Ljava/lang/String;Ljava/util/Map;)V", "contactUri", "(Ljava/lang/String;Lnet/jami/model/Uri;Ljava/lang/String;)V", "getAccountId", "()Ljava/lang/String;", "getConversationId", "setConversationId", "(Ljava/lang/String;)V", "displayname", "getDisplayname", "fullname", "getFullname", "<set-?>", "", "isNameResolved", "()Z", "mContactUsername", "message", "getMessage", "setMessage", "timestamp", "getTimestamp", "()J", "getUri", "()Lnet/jami/model/Uri;", "vCard", "Lezvcard/VCard;", "getVCard", "()Lezvcard/VCard;", "setVCard", "(Lezvcard/VCard;)V", "setUsername", "", "username", "Companion", "libringclient"})
public final class TrustRequest {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String accountId = null;
    private java.lang.String mContactUsername;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.model.Uri uri = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String conversationId;
    @org.jetbrains.annotations.Nullable()
    private ezvcard.VCard vCard;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String message;
    private final long timestamp = 0L;
    private boolean isNameResolved = false;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.TrustRequest.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAccountId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Uri getUri() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getConversationId() {
        return null;
    }
    
    public final void setConversationId(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final ezvcard.VCard getVCard() {
        return null;
    }
    
    public final void setVCard(@org.jetbrains.annotations.Nullable()
    ezvcard.VCard p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getMessage() {
        return null;
    }
    
    public final void setMessage(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    public final boolean isNameResolved() {
        return false;
    }
    
    public TrustRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri, long received, @org.jetbrains.annotations.Nullable()
    java.lang.String payload, @org.jetbrains.annotations.Nullable()
    java.lang.String conversationId) {
        super();
    }
    
    public TrustRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> info) {
        super();
    }
    
    public TrustRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactUri, @org.jetbrains.annotations.Nullable()
    java.lang.String conversationId) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFullname() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDisplayname() {
        return null;
    }
    
    public final void setUsername(@org.jetbrains.annotations.Nullable()
    java.lang.String username) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/model/TrustRequest$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}