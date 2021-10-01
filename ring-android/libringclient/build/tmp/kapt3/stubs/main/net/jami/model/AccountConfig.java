package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\u0010&\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010%\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\u0018\u0000 \u001b2\u00020\u0001:\u0001\u001bB\u0019\u0012\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\u0002\u0010\u0005J\u0011\u0010\u0014\u001a\u00020\u00042\u0006\u0010\u0015\u001a\u00020\rH\u0086\u0002J\u000e\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0015\u001a\u00020\rJ\u0016\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u0015\u001a\u00020\r2\u0006\u0010\u001a\u001a\u00020\u0017J\u0018\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u0015\u001a\u00020\r2\b\u0010\u001a\u001a\u0004\u0018\u00010\u0004R\u001d\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u00078F\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\tR#\u0010\n\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u00040\f0\u000b8F\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\u000fR\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\r0\u000b8F\u00a2\u0006\u0006\u001a\u0004\b\u0011\u0010\u000fR\u001a\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u00040\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lnet/jami/model/AccountConfig;", "", "details", "", "", "(Ljava/util/Map;)V", "all", "Ljava/util/HashMap;", "getAll", "()Ljava/util/HashMap;", "entries", "", "", "Lnet/jami/model/ConfigKey;", "getEntries", "()Ljava/util/Set;", "keys", "getKeys", "mValues", "", "get", "key", "getBool", "", "put", "", "value", "Companion", "libringclient"})
public final class AccountConfig {
    private final java.util.Map<net.jami.model.ConfigKey, java.lang.String> mValues = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.AccountConfig.Companion Companion = null;
    private static final java.lang.String TAG = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TRUE_STR = "true";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String FALSE_STR = "false";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACCOUNT_TYPE_RING = "RING";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACCOUNT_TYPE_SIP = "SIP";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_REGISTERED = "REGISTERED";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_READY = "READY";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_UNREGISTERED = "UNREGISTERED";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_TRYING = "TRYING";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR = "ERROR";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_GENERIC = "ERROR_GENERIC";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_AUTH = "ERROR_AUTH";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_NETWORK = "ERROR_NETWORK";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_HOST = "ERROR_HOST";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_CONF_STUN = "ERROR_CONF_STUN";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_EXIST_STUN = "ERROR_EXIST_STUN";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_SERVICE_UNAVAILABLE = "ERROR_SERVICE_UNAVAILABLE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_ERROR_NOT_ACCEPTABLE = "ERROR_NOT_ACCEPTABLE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_REQUEST_TIMEOUT = "Request Timeout";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_INITIALIZING = "INITIALIZING";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_NEED_MIGRATION = "ERROR_NEED_MIGRATION";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_SUCCESS = "SUCCESS";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STATE_INVALID = "INVALID";
    
    public AccountConfig(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> details) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String get(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key) {
        return null;
    }
    
    public final boolean getBool(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.HashMap<java.lang.String, java.lang.String> getAll() {
        return null;
    }
    
    public final void put(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key, @org.jetbrains.annotations.Nullable()
    java.lang.String value) {
    }
    
    public final void put(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key, boolean value) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<net.jami.model.ConfigKey> getKeys() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.util.Map.Entry<net.jami.model.ConfigKey, java.lang.String>> getEntries() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0018\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0019\u001a\n \u001a*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lnet/jami/model/AccountConfig$Companion;", "", "()V", "ACCOUNT_TYPE_RING", "", "ACCOUNT_TYPE_SIP", "FALSE_STR", "STATE_ERROR", "STATE_ERROR_AUTH", "STATE_ERROR_CONF_STUN", "STATE_ERROR_EXIST_STUN", "STATE_ERROR_GENERIC", "STATE_ERROR_HOST", "STATE_ERROR_NETWORK", "STATE_ERROR_NOT_ACCEPTABLE", "STATE_ERROR_SERVICE_UNAVAILABLE", "STATE_INITIALIZING", "STATE_INVALID", "STATE_NEED_MIGRATION", "STATE_READY", "STATE_REGISTERED", "STATE_REQUEST_TIMEOUT", "STATE_SUCCESS", "STATE_TRYING", "STATE_UNREGISTERED", "TAG", "kotlin.jvm.PlatformType", "TRUE_STR", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}