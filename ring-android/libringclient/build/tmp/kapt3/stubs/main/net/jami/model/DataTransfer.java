package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b\u001b\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u0000 ;2\u00020\u0001:\u0001;BO\b\u0016\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0005\u0012\u0006\u0010\u0007\u001a\u00020\u0005\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\u000b\u0012\b\u0010\r\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u000eB\u000f\b\u0016\u0012\u0006\u0010\u000f\u001a\u00020\u0001\u00a2\u0006\u0002\u0010\u0010BI\b\u0016\u0012\b\u0010\r\u001a\u0004\u0018\u00010\u0005\u0012\u0006\u0010\u0011\u001a\u00020\u0005\u0012\u0006\u0010\u0012\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\u0005\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\u0013\u001a\u00020\u000b\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\u0014J\u000e\u00104\u001a\u00020\t2\u0006\u00105\u001a\u000206J\u000e\u00107\u001a\u0002082\u0006\u00109\u001a\u00020\u000bJ\u0006\u0010:\u001a\u00020\tR\u001a\u0010\f\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0015\u0010\u0016\"\u0004\b\u0017\u0010\u0018R\u001c\u0010\u0019\u001a\u0004\u0018\u00010\u001aX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001b\u0010\u001c\"\u0004\b\u001d\u0010\u001eR\u001c\u0010\u001f\u001a\u0004\u0018\u00010\u001aX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b \u0010\u001c\"\u0004\b!\u0010\u001eR\u0011\u0010\u0007\u001a\u00020\u00058F\u00a2\u0006\u0006\u001a\u0004\b\"\u0010#R\u0013\u0010$\u001a\u0004\u0018\u00010\u00058F\u00a2\u0006\u0006\u001a\u0004\b%\u0010#R\"\u0010\r\u001a\u0004\u0018\u00010\u00052\b\u0010&\u001a\u0004\u0018\u00010\u0005@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010#R\u0011\u0010(\u001a\u00020\t8F\u00a2\u0006\u0006\u001a\u0004\b(\u0010)R\u0011\u0010*\u001a\u00020\t8F\u00a2\u0006\u0006\u001a\u0004\b*\u0010)R\u0011\u0010+\u001a\u00020\t8F\u00a2\u0006\u0006\u001a\u0004\b+\u0010)R\u0011\u0010\b\u001a\u00020\t8F\u00a2\u0006\u0006\u001a\u0004\b\b\u0010)R\u0011\u0010,\u001a\u00020\t8F\u00a2\u0006\u0006\u001a\u0004\b,\u0010)R\u0011\u0010-\u001a\u00020\t8F\u00a2\u0006\u0006\u001a\u0004\b-\u0010)R\u0010\u0010.\u001a\u0004\u0018\u00010\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0013\u0010/\u001a\u0004\u0018\u00010\u001a8F\u00a2\u0006\u0006\u001a\u0004\b0\u0010\u001cR\u0011\u00101\u001a\u00020\u00058F\u00a2\u0006\u0006\u001a\u0004\b2\u0010#R\u001e\u0010\n\u001a\u00020\u000b2\u0006\u0010&\u001a\u00020\u000b@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010\u0016\u00a8\u0006<"}, d2 = {"Lnet/jami/model/DataTransfer;", "Lnet/jami/model/Interaction;", "conversation", "Lnet/jami/model/ConversationHistory;", "peer", "", "account", "displayName", "isOutgoing", "", "totalSize", "", "bytesProgress", "fileId", "(Lnet/jami/model/ConversationHistory;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;)V", "interaction", "(Lnet/jami/model/Interaction;)V", "accountId", "peerUri", "timestamp", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJJ)V", "getBytesProgress", "()J", "setBytesProgress", "(J)V", "daemonPath", "Ljava/io/File;", "getDaemonPath", "()Ljava/io/File;", "setDaemonPath", "(Ljava/io/File;)V", "destination", "getDestination", "setDestination", "getDisplayName", "()Ljava/lang/String;", "extension", "getExtension", "<set-?>", "getFileId", "isAudio", "()Z", "isComplete", "isError", "isPicture", "isVideo", "mExtension", "publicPath", "getPublicPath", "storagePath", "getStoragePath", "getTotalSize", "canAutoAccept", "maxSize", "", "setSize", "", "size", "showPicture", "Companion", "libringclient"})
public final class DataTransfer extends net.jami.model.Interaction {
    private long totalSize = 0L;
    private long bytesProgress = 0L;
    private java.lang.String mExtension;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String fileId;
    @org.jetbrains.annotations.Nullable()
    private java.io.File destination;
    @org.jetbrains.annotations.Nullable()
    private java.io.File daemonPath;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.DataTransfer.Companion Companion = null;
    private static final java.util.Set<java.lang.String> IMAGE_EXTENSIONS = null;
    private static final java.util.Set<java.lang.String> AUDIO_EXTENSIONS = null;
    private static final java.util.Set<java.lang.String> VIDEO_EXTENSIONS = null;
    private static final int MAX_SIZE = 33554432;
    private static final int UNLIMITED_SIZE = 268435456;
    
    public final long getTotalSize() {
        return 0L;
    }
    
    public final long getBytesProgress() {
        return 0L;
    }
    
    public final void setBytesProgress(long p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFileId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.io.File getDestination() {
        return null;
    }
    
    public final void setDestination(@org.jetbrains.annotations.Nullable()
    java.io.File p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.io.File getDaemonPath() {
        return null;
    }
    
    public final void setDaemonPath(@org.jetbrains.annotations.Nullable()
    java.io.File p0) {
    }
    
    public DataTransfer(@org.jetbrains.annotations.Nullable()
    net.jami.model.ConversationHistory conversation, @org.jetbrains.annotations.Nullable()
    java.lang.String peer, @org.jetbrains.annotations.Nullable()
    java.lang.String account, @org.jetbrains.annotations.NotNull()
    java.lang.String displayName, boolean isOutgoing, long totalSize, long bytesProgress, @org.jetbrains.annotations.Nullable()
    java.lang.String fileId) {
        super();
    }
    
    public DataTransfer(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
        super();
    }
    
    public DataTransfer(@org.jetbrains.annotations.Nullable()
    java.lang.String fileId, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerUri, @org.jetbrains.annotations.NotNull()
    java.lang.String displayName, boolean isOutgoing, long timestamp, long totalSize, long bytesProgress) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getExtension() {
        return null;
    }
    
    public final boolean isPicture() {
        return false;
    }
    
    public final boolean isAudio() {
        return false;
    }
    
    public final boolean isVideo() {
        return false;
    }
    
    public final boolean isComplete() {
        return false;
    }
    
    public final boolean showPicture() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStoragePath() {
        return null;
    }
    
    public final void setSize(long size) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDisplayName() {
        return null;
    }
    
    public final boolean isOutgoing() {
        return false;
    }
    
    public final boolean isError() {
        return false;
    }
    
    public final boolean canAutoAccept(int maxSize) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.io.File getPublicPath() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lnet/jami/model/DataTransfer$Companion;", "", "()V", "AUDIO_EXTENSIONS", "", "", "IMAGE_EXTENSIONS", "MAX_SIZE", "", "UNLIMITED_SIZE", "VIDEO_EXTENSIONS", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}