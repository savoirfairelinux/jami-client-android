package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\u0002\n\u0002\b\u0002\b&\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0013\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0014\u001a\u00020\u000eH&J\u0018\u0010\u0015\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0016\u001a\u00020\u000eH&J \u0010\u0015\u001a\u00020\u00042\u0006\u0010\u0017\u001a\u00020\u000e2\u0006\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0016\u001a\u00020\u000eH&J\u000e\u0010\u0015\u001a\u00020\u00042\u0006\u0010\u0018\u001a\u00020\u0019J\u0010\u0010\u001a\u001a\u00020\u00042\u0006\u0010\u0016\u001a\u00020\u000eH&J\u001e\u0010\u001b\u001a\u00020\u00042\u0006\u0010\u0017\u001a\u00020\u000e2\u0006\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0016\u001a\u00020\u000eJ\u0018\u0010\u001c\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0016\u001a\u00020\u000eH&J\u0018\u0010\u001d\u001a\u00020\b2\u0006\u0010\u001e\u001a\u00020\u00042\u0006\u0010\u001f\u001a\u00020\u0004H&J\b\u0010 \u001a\u00020\bH&J\b\u0010!\u001a\u00020\bH&J\b\u0010\"\u001a\u00020\bH&J\b\u0010#\u001a\u00020\bH&J\b\u0010$\u001a\u00020\bH&J\b\u0010%\u001a\u00020\bH&J\b\u0010&\u001a\u00020\'H&J\b\u0010(\u001a\u00020\u0004H&R\u0012\u0010\u0003\u001a\u00020\u0004X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006R\u0012\u0010\u0007\u001a\u00020\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\tR\u0012\u0010\n\u001a\u00020\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\tR\u0012\u0010\u000b\u001a\u00020\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000b\u0010\tR\u0012\u0010\f\u001a\u00020\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\tR\u0014\u0010\r\u001a\u0004\u0018\u00010\u000eX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010R\u0014\u0010\u0011\u001a\u0004\u0018\u00010\u000eX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u0010\u00a8\u0006)"}, d2 = {"Lnet/jami/services/DeviceRuntimeService;", "Lnet/jami/services/DaemonService$SystemInfoCallbacks;", "()V", "cacheDir", "Ljava/io/File;", "getCacheDir", "()Ljava/io/File;", "isConnectedBluetooth", "", "()Z", "isConnectedEthernet", "isConnectedMobile", "isConnectedWifi", "profileName", "", "getProfileName", "()Ljava/lang/String;", "pushToken", "getPushToken", "getConversationDir", "conversationId", "getConversationPath", "name", "accountId", "interaction", "Lnet/jami/model/DataTransfer;", "getFilePath", "getNewConversationPath", "getTemporaryPath", "hardLinkOrCopy", "source", "dest", "hasAudioPermission", "hasCallLogPermission", "hasContactPermission", "hasGalleryPermission", "hasVideoPermission", "hasWriteExternalStoragePermission", "loadNativeLibrary", "", "provideFilesDir", "libringclient"})
public abstract class DeviceRuntimeService implements net.jami.services.DaemonService.SystemInfoCallbacks {
    
    public DeviceRuntimeService() {
        super();
    }
    
    public abstract void loadNativeLibrary();
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File provideFilesDir();
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File getCacheDir();
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File getFilePath(@org.jetbrains.annotations.NotNull()
    java.lang.String name);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File getConversationPath(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String name);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File getConversationPath(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String name);
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getConversationPath(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer interaction) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getNewConversationPath(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File getTemporaryPath(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String name);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.io.File getConversationDir(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.String getPushToken();
    
    public abstract boolean isConnectedMobile();
    
    public abstract boolean isConnectedEthernet();
    
    public abstract boolean isConnectedWifi();
    
    public abstract boolean isConnectedBluetooth();
    
    public abstract boolean hasVideoPermission();
    
    public abstract boolean hasAudioPermission();
    
    public abstract boolean hasContactPermission();
    
    public abstract boolean hasCallLogPermission();
    
    public abstract boolean hasGalleryPermission();
    
    public abstract boolean hasWriteExternalStoragePermission();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.String getProfileName();
    
    public abstract boolean hardLinkOrCopy(@org.jetbrains.annotations.NotNull()
    java.io.File source, @org.jetbrains.annotations.NotNull()
    java.io.File dest);
}