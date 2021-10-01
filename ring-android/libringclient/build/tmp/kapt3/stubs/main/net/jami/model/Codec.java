package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0010\n\u0002\u0010\r\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001:\u0001*B)\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u0006\u0010\u0007\u001a\u00020\b\u00a2\u0006\u0002\u0010\tJ\u0013\u0010%\u001a\u00020\b2\b\u0010&\u001a\u0004\u0018\u00010\u0001H\u0096\u0002J\b\u0010\'\u001a\u00020\u0006H\u0016J\u0006\u0010(\u001a\u00020)R\u001c\u0010\n\u001a\u0004\u0018\u00010\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001c\u0010\u000f\u001a\u0004\u0018\u00010\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\f\"\u0004\b\u0011\u0010\u000eR\u001a\u0010\u0012\u001a\u00020\bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0016\u001a\u00020\b8F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u0013R\u0010\u0010\u0017\u001a\u0004\u0018\u00010\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0013\u0010\u0018\u001a\u0004\u0018\u00010\u00198F\u00a2\u0006\u0006\u001a\u0004\b\u001a\u0010\u001bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u001c\u0010\u001e\u001a\u0004\u0018\u00010\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001f\u0010\f\"\u0004\b \u0010\u000eR\u0011\u0010!\u001a\u00020\"\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010$\u00a8\u0006+"}, d2 = {"Lnet/jami/model/Codec;", "", "payload", "", "audioCodecDetails", "", "", "enabled", "", "(JLjava/util/Map;Z)V", "bitRate", "getBitRate", "()Ljava/lang/String;", "setBitRate", "(Ljava/lang/String;)V", "channels", "getChannels", "setChannels", "isEnabled", "()Z", "setEnabled", "(Z)V", "isSpeex", "mName", "name", "", "getName", "()Ljava/lang/CharSequence;", "getPayload", "()J", "sampleRate", "getSampleRate", "setSampleRate", "type", "Lnet/jami/model/Codec$Type;", "getType", "()Lnet/jami/model/Codec$Type;", "equals", "other", "toString", "toggleState", "", "Type", "libringclient"})
public final class Codec {
    private final long payload = 0L;
    private final java.lang.String mName = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.model.Codec.Type type = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String sampleRate;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String bitRate;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String channels;
    private boolean isEnabled;
    
    public Codec(long payload, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> audioCodecDetails, boolean enabled) {
        super();
    }
    
    public final long getPayload() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Codec.Type getType() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSampleRate() {
        return null;
    }
    
    public final void setSampleRate(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getBitRate() {
        return null;
    }
    
    public final void setBitRate(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getChannels() {
        return null;
    }
    
    public final void setChannels(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean isEnabled() {
        return false;
    }
    
    public final void setEnabled(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public java.lang.String toString() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.CharSequence getName() {
        return null;
    }
    
    public final void toggleState() {
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    public final boolean isSpeex() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/model/Codec$Type;", "", "(Ljava/lang/String;I)V", "AUDIO", "VIDEO", "libringclient"})
    public static enum Type {
        /*public static final*/ AUDIO /* = new AUDIO() */,
        /*public static final*/ VIDEO /* = new VIDEO() */;
        
        Type() {
        }
    }
}