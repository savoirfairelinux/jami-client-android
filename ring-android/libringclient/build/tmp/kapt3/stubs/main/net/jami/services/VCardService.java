package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u000b\b&\u0018\u0000 \u001e2\u00020\u0001:\u0001\u001eB\u0005\u00a2\u0006\u0002\u0010\u0002J\u001e\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH&J\u0014\u0010\n\u001a\u0004\u0018\u00010\u00012\b\u0010\u000b\u001a\u0004\u0018\u00010\u0007H&J\u0016\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00050\r2\u0006\u0010\u000e\u001a\u00020\u000fH&J\u001e\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u00112\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0013\u001a\u00020\u0014H&J\u001c\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00120\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0013\u001a\u00020\u0014J\u0016\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0017\u001a\u00020\u0012H&J&\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0019\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\tH&J4\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00120\u00042\u0006\u0010\u0006\u001a\u00020\u00072\b\u0010\u001b\u001a\u0004\u0018\u00010\u00072\b\u0010\u001c\u001a\u0004\u0018\u00010\u00072\b\u0010\u001d\u001a\u0004\u0018\u00010\u0007H&\u00a8\u0006\u001f"}, d2 = {"Lnet/jami/services/VCardService;", "", "()V", "accountProfileReceived", "Lio/reactivex/rxjava3/core/Single;", "Lnet/jami/model/Profile;", "accountId", "", "vcardFile", "Ljava/io/File;", "base64ToBitmap", "base64", "loadProfile", "Lio/reactivex/rxjava3/core/Observable;", "account", "Lnet/jami/model/Account;", "loadSmallVCard", "Lio/reactivex/rxjava3/core/Maybe;", "Lezvcard/VCard;", "maxSize", "", "loadSmallVCardWithDefault", "loadVCardProfile", "vcard", "peerProfileReceived", "peerId", "saveVCardProfile", "uri", "displayName", "picture", "Companion", "libringclient"})
public abstract class VCardService {
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.VCardService.Companion Companion = null;
    public static final int MAX_SIZE_SIP = 262144;
    public static final int MAX_SIZE_REQUEST = 16384;
    
    public VCardService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Observable<net.jami.model.Profile> loadProfile(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Maybe<ezvcard.VCard> loadSmallVCard(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, int maxSize);
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<ezvcard.VCard> loadSmallVCardWithDefault(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, int maxSize) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Single<ezvcard.VCard> saveVCardProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    java.lang.String uri, @org.jetbrains.annotations.Nullable()
    java.lang.String displayName, @org.jetbrains.annotations.Nullable()
    java.lang.String picture);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Single<net.jami.model.Profile> loadVCardProfile(@org.jetbrains.annotations.NotNull()
    ezvcard.VCard vcard);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Single<net.jami.model.Profile> peerProfileReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    java.io.File vcard);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Single<net.jami.model.Profile> accountProfileReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.io.File vcardFile);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object base64ToBitmap(@org.jetbrains.annotations.Nullable()
    java.lang.String base64);
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lnet/jami/services/VCardService$Companion;", "", "()V", "MAX_SIZE_REQUEST", "", "MAX_SIZE_SIP", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}