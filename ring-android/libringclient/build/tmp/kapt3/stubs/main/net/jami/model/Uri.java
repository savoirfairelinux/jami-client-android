package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u0011\n\u0002\u0010\u0000\n\u0002\b\u0003\u0018\u0000 !2\u00020\u0001:\u0001!B\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0002B-\b\u0016\u0012\b\u0010\u0003\u001a\u0004\u0018\u00010\u0004\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0004\u0012\u0006\u0010\u0006\u001a\u00020\u0004\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\u0002\u0010\bB\u0019\b\u0016\u0012\b\u0010\u0003\u001a\u0004\u0018\u00010\u0004\u0012\u0006\u0010\u0006\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\tJ\u0013\u0010\u001d\u001a\u00020\r2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0096\u0002J\b\u0010 \u001a\u00020\u0004H\u0016R\u0011\u0010\u0006\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\f\u001a\u00020\r8F\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\u000eR\u0011\u0010\u000f\u001a\u00020\r8F\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u000eR\u0011\u0010\u0010\u001a\u00020\r8F\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\u000eR\u0011\u0010\u0011\u001a\u00020\r8F\u00a2\u0006\u0006\u001a\u0004\b\u0011\u0010\u000eR\u000e\u0010\u0012\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u000bR\u0011\u0010\u0014\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0015\u0010\u000bR\u0011\u0010\u0016\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0017\u0010\u000bR\u0013\u0010\u0003\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u000bR\u0011\u0010\u0019\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u001a\u0010\u000bR\u0013\u0010\u001b\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u000b\u00a8\u0006\""}, d2 = {"Lnet/jami/model/Uri;", "Ljava/io/Serializable;", "()V", "scheme", "", "user", "host", "port", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "(Ljava/lang/String;Ljava/lang/String;)V", "getHost", "()Ljava/lang/String;", "isEmpty", "", "()Z", "isHexId", "isSingleIp", "isSwarm", "mHost", "getPort", "rawRingId", "getRawRingId", "rawUriString", "getRawUriString", "getScheme", "uri", "getUri", "username", "getUsername", "equals", "other", "", "toString", "Companion", "libringclient"})
public final class Uri implements java.io.Serializable {
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String scheme = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String username = null;
    private final java.lang.String mHost = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String port = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.Uri.Companion Companion = null;
    private static final java.util.regex.Pattern ANGLE_BRACKETS_PATTERN = null;
    private static final java.util.regex.Pattern HEX_ID_PATTERN = null;
    private static final java.util.regex.Pattern RING_URI_PATTERN = null;
    private static final java.util.regex.Pattern URI_PATTERN = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String RING_URI_SCHEME = "ring:";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String JAMI_URI_SCHEME = "jami:";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SWARM_SCHEME = "swarm:";
    private static final java.lang.String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final java.lang.String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
    private static final java.util.regex.Pattern VALID_IPV4_PATTERN = null;
    private static final java.util.regex.Pattern VALID_IPV6_PATTERN = null;
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getScheme() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUsername() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPort() {
        return null;
    }
    
    public Uri() {
        super();
    }
    
    public Uri(@org.jetbrains.annotations.Nullable()
    java.lang.String scheme, @org.jetbrains.annotations.Nullable()
    java.lang.String user, @org.jetbrains.annotations.NotNull()
    java.lang.String host, @org.jetbrains.annotations.Nullable()
    java.lang.String port) {
        super();
    }
    
    public Uri(@org.jetbrains.annotations.Nullable()
    java.lang.String scheme, @org.jetbrains.annotations.NotNull()
    java.lang.String host) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRawRingId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getUri() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRawUriString() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public java.lang.String toString() {
        return null;
    }
    
    public final boolean isSingleIp() {
        return false;
    }
    
    public final boolean isHexId() {
        return false;
    }
    
    public final boolean isSwarm() {
        return false;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    public final boolean isEmpty() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHost() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\bJ\u000e\u0010\u0014\u001a\u00020\u00122\u0006\u0010\u0015\u001a\u00020\bJ\u001c\u0010\u0016\u001a\u0010\u0012\u0004\u0012\u00020\u0012\u0012\u0006\u0012\u0004\u0018\u00010\b0\u00172\u0006\u0010\u0018\u001a\u00020\bJ\u000e\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\bR\u0016\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0006\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0086T\u00a2\u0006\u0002\n\u0000R\u0016\u0010\t\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\bX\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0086T\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\r\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lnet/jami/model/Uri$Companion;", "", "()V", "ANGLE_BRACKETS_PATTERN", "Ljava/util/regex/Pattern;", "kotlin.jvm.PlatformType", "HEX_ID_PATTERN", "JAMI_URI_SCHEME", "", "RING_URI_PATTERN", "RING_URI_SCHEME", "SWARM_SCHEME", "URI_PATTERN", "VALID_IPV4_PATTERN", "VALID_IPV6_PATTERN", "ipv4Pattern", "ipv6Pattern", "fromId", "Lnet/jami/model/Uri;", "conversationId", "fromString", "uri", "fromStringWithName", "Lnet/jami/utils/Tuple;", "uriString", "isIpAddress", "", "ipAddress", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Uri fromString(@org.jetbrains.annotations.NotNull()
        java.lang.String uri) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.utils.Tuple<net.jami.model.Uri, java.lang.String> fromStringWithName(@org.jetbrains.annotations.NotNull()
        java.lang.String uriString) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Uri fromId(@org.jetbrains.annotations.NotNull()
        java.lang.String conversationId) {
            return null;
        }
        
        /**
         * Determine if the given string is a valid IPv4 or IPv6 address.  This method
         * uses pattern matching to see if the given string could be a valid IP address.
         *
         * @param ipAddress A string that is to be examined to verify whether or not
         * it could be a valid IP address.
         * @return `true` if the string is a value that is a valid IP address,
         * `false` otherwise.
         */
        public final boolean isIpAddress(@org.jetbrains.annotations.NotNull()
        java.lang.String ipAddress) {
            return false;
        }
    }
}