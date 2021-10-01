package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\bZ\b\u0086\u0001\u0018\u0000 _2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001_B\u000f\b\u0012\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004B\u0017\b\u0012\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\u0010\u0010\u000b\u001a\u00020\u00062\b\u0010\f\u001a\u0004\u0018\u00010\u0000J\u0006\u0010\u0002\u001a\u00020\u0003R\u0011\u0010\b\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u000e\u0010\n\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000j\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013j\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016j\u0002\b\u0017j\u0002\b\u0018j\u0002\b\u0019j\u0002\b\u001aj\u0002\b\u001bj\u0002\b\u001cj\u0002\b\u001dj\u0002\b\u001ej\u0002\b\u001fj\u0002\b j\u0002\b!j\u0002\b\"j\u0002\b#j\u0002\b$j\u0002\b%j\u0002\b&j\u0002\b\'j\u0002\b(j\u0002\b)j\u0002\b*j\u0002\b+j\u0002\b,j\u0002\b-j\u0002\b.j\u0002\b/j\u0002\b0j\u0002\b1j\u0002\b2j\u0002\b3j\u0002\b4j\u0002\b5j\u0002\b6j\u0002\b7j\u0002\b8j\u0002\b9j\u0002\b:j\u0002\b;j\u0002\b<j\u0002\b=j\u0002\b>j\u0002\b?j\u0002\b@j\u0002\bAj\u0002\bBj\u0002\bCj\u0002\bDj\u0002\bEj\u0002\bFj\u0002\bGj\u0002\bHj\u0002\bIj\u0002\bJj\u0002\bKj\u0002\bLj\u0002\bMj\u0002\bNj\u0002\bOj\u0002\bPj\u0002\bQj\u0002\bRj\u0002\bSj\u0002\bTj\u0002\bUj\u0002\bVj\u0002\bWj\u0002\bXj\u0002\bYj\u0002\bZj\u0002\b[j\u0002\b\\j\u0002\b]j\u0002\b^\u00a8\u0006`"}, d2 = {"Lnet/jami/model/ConfigKey;", "", "key", "", "(Ljava/lang/String;ILjava/lang/String;)V", "isBool", "", "(Ljava/lang/String;ILjava/lang/String;Z)V", "isTwoState", "()Z", "mKey", "equals", "other", "MAILBOX", "REGISTRATION_EXPIRE", "CREDENTIAL_NUMBER", "ACCOUNT_DTMF_TYPE", "RINGTONE_PATH", "RINGTONE_ENABLED", "RINGTONE_CUSTOM", "KEEP_ALIVE_ENABLED", "LOCAL_INTERFACE", "PUBLISHED_SAMEAS_LOCAL", "LOCAL_PORT", "PUBLISHED_PORT", "PUBLISHED_ADDRESS", "STUN_SERVER", "STUN_ENABLE", "TURN_ENABLE", "TURN_SERVER", "TURN_USERNAME", "TURN_PASSWORD", "TURN_REALM", "AUDIO_PORT_MIN", "AUDIO_PORT_MAX", "ACCOUNT_USERAGENT", "ACCOUNT_UPNP_ENABLE", "ACCOUNT_ROUTESET", "ACCOUNT_AUTOANSWER", "ACCOUNT_ISRENDEZVOUS", "ACCOUNT_ALIAS", "ACCOUNT_HOSTNAME", "ACCOUNT_USERNAME", "ACCOUNT_PASSWORD", "ACCOUNT_REALM", "ACCOUNT_TYPE", "ACCOUNT_ENABLE", "ACCOUNT_ACTIVE", "ACCOUNT_DEVICE_ID", "ACCOUNT_DEVICE_NAME", "ACCOUNT_PEER_DISCOVERY", "ACCOUNT_DISCOVERY", "ACCOUNT_PUBLISH", "ACCOUNT_DISPLAYNAME", "VIDEO_ENABLED", "VIDEO_PORT_MIN", "VIDEO_PORT_MAX", "PRESENCE_ENABLE", "ARCHIVE_PASSWORD", "ARCHIVE_HAS_PASSWORD", "ARCHIVE_PIN", "ARCHIVE_PATH", "DISPLAY_NAME", "ETH_ACCOUNT", "TLS_LISTENER_PORT", "TLS_ENABLE", "TLS_CA_LIST_FILE", "TLS_CERTIFICATE_FILE", "TLS_PRIVATE_KEY_FILE", "TLS_PASSWORD", "TLS_METHOD", "TLS_CIPHERS", "TLS_SERVER_NAME", "TLS_VERIFY_SERVER", "TLS_VERIFY_CLIENT", "TLS_REQUIRE_CLIENT_CERTIFICATE", "TLS_NEGOTIATION_TIMEOUT_SEC", "ACCOUNT_REGISTERED_NAME", "ACCOUNT_REGISTRATION_STATUS", "ACCOUNT_REGISTRATION_STATE_CODE", "ACCOUNT_REGISTRATION_STATE_DESC", "SRTP_ENABLE", "SRTP_KEY_EXCHANGE", "SRTP_ENCRYPTION_ALGO", "SRTP_RTP_FALLBACK", "RINGNS_ACCOUNT", "RINGNS_HOST", "DHT_PORT", "DHT_PUBLIC_IN", "PROXY_ENABLED", "PROXY_SERVER", "PROXY_SERVER_LIST", "PROXY_PUSH_TOKEN", "MANAGER_URI", "MANAGER_USERNAME", "Companion", "libringclient"})
public enum ConfigKey {
    /*public static final*/ MAILBOX /* = new MAILBOX(null) */,
    /*public static final*/ REGISTRATION_EXPIRE /* = new REGISTRATION_EXPIRE(null) */,
    /*public static final*/ CREDENTIAL_NUMBER /* = new CREDENTIAL_NUMBER(null) */,
    /*public static final*/ ACCOUNT_DTMF_TYPE /* = new ACCOUNT_DTMF_TYPE(null) */,
    /*public static final*/ RINGTONE_PATH /* = new RINGTONE_PATH(null) */,
    /*public static final*/ RINGTONE_ENABLED /* = new RINGTONE_ENABLED(null) */,
    /*public static final*/ RINGTONE_CUSTOM /* = new RINGTONE_CUSTOM(null) */,
    /*public static final*/ KEEP_ALIVE_ENABLED /* = new KEEP_ALIVE_ENABLED(null) */,
    /*public static final*/ LOCAL_INTERFACE /* = new LOCAL_INTERFACE(null) */,
    /*public static final*/ PUBLISHED_SAMEAS_LOCAL /* = new PUBLISHED_SAMEAS_LOCAL(null) */,
    /*public static final*/ LOCAL_PORT /* = new LOCAL_PORT(null) */,
    /*public static final*/ PUBLISHED_PORT /* = new PUBLISHED_PORT(null) */,
    /*public static final*/ PUBLISHED_ADDRESS /* = new PUBLISHED_ADDRESS(null) */,
    /*public static final*/ STUN_SERVER /* = new STUN_SERVER(null) */,
    /*public static final*/ STUN_ENABLE /* = new STUN_ENABLE(null) */,
    /*public static final*/ TURN_ENABLE /* = new TURN_ENABLE(null) */,
    /*public static final*/ TURN_SERVER /* = new TURN_SERVER(null) */,
    /*public static final*/ TURN_USERNAME /* = new TURN_USERNAME(null) */,
    /*public static final*/ TURN_PASSWORD /* = new TURN_PASSWORD(null) */,
    /*public static final*/ TURN_REALM /* = new TURN_REALM(null) */,
    /*public static final*/ AUDIO_PORT_MIN /* = new AUDIO_PORT_MIN(null) */,
    /*public static final*/ AUDIO_PORT_MAX /* = new AUDIO_PORT_MAX(null) */,
    /*public static final*/ ACCOUNT_USERAGENT /* = new ACCOUNT_USERAGENT(null) */,
    /*public static final*/ ACCOUNT_UPNP_ENABLE /* = new ACCOUNT_UPNP_ENABLE(null) */,
    /*public static final*/ ACCOUNT_ROUTESET /* = new ACCOUNT_ROUTESET(null) */,
    /*public static final*/ ACCOUNT_AUTOANSWER /* = new ACCOUNT_AUTOANSWER(null) */,
    /*public static final*/ ACCOUNT_ISRENDEZVOUS /* = new ACCOUNT_ISRENDEZVOUS(null) */,
    /*public static final*/ ACCOUNT_ALIAS /* = new ACCOUNT_ALIAS(null) */,
    /*public static final*/ ACCOUNT_HOSTNAME /* = new ACCOUNT_HOSTNAME(null) */,
    /*public static final*/ ACCOUNT_USERNAME /* = new ACCOUNT_USERNAME(null) */,
    /*public static final*/ ACCOUNT_PASSWORD /* = new ACCOUNT_PASSWORD(null) */,
    /*public static final*/ ACCOUNT_REALM /* = new ACCOUNT_REALM(null) */,
    /*public static final*/ ACCOUNT_TYPE /* = new ACCOUNT_TYPE(null) */,
    /*public static final*/ ACCOUNT_ENABLE /* = new ACCOUNT_ENABLE(null) */,
    /*public static final*/ ACCOUNT_ACTIVE /* = new ACCOUNT_ACTIVE(null) */,
    /*public static final*/ ACCOUNT_DEVICE_ID /* = new ACCOUNT_DEVICE_ID(null) */,
    /*public static final*/ ACCOUNT_DEVICE_NAME /* = new ACCOUNT_DEVICE_NAME(null) */,
    /*public static final*/ ACCOUNT_PEER_DISCOVERY /* = new ACCOUNT_PEER_DISCOVERY(null) */,
    /*public static final*/ ACCOUNT_DISCOVERY /* = new ACCOUNT_DISCOVERY(null) */,
    /*public static final*/ ACCOUNT_PUBLISH /* = new ACCOUNT_PUBLISH(null) */,
    /*public static final*/ ACCOUNT_DISPLAYNAME /* = new ACCOUNT_DISPLAYNAME(null) */,
    /*public static final*/ VIDEO_ENABLED /* = new VIDEO_ENABLED(null) */,
    /*public static final*/ VIDEO_PORT_MIN /* = new VIDEO_PORT_MIN(null) */,
    /*public static final*/ VIDEO_PORT_MAX /* = new VIDEO_PORT_MAX(null) */,
    /*public static final*/ PRESENCE_ENABLE /* = new PRESENCE_ENABLE(null) */,
    /*public static final*/ ARCHIVE_PASSWORD /* = new ARCHIVE_PASSWORD(null) */,
    /*public static final*/ ARCHIVE_HAS_PASSWORD /* = new ARCHIVE_HAS_PASSWORD(null) */,
    /*public static final*/ ARCHIVE_PIN /* = new ARCHIVE_PIN(null) */,
    /*public static final*/ ARCHIVE_PATH /* = new ARCHIVE_PATH(null) */,
    /*public static final*/ DISPLAY_NAME /* = new DISPLAY_NAME(null) */,
    /*public static final*/ ETH_ACCOUNT /* = new ETH_ACCOUNT(null) */,
    /*public static final*/ TLS_LISTENER_PORT /* = new TLS_LISTENER_PORT(null) */,
    /*public static final*/ TLS_ENABLE /* = new TLS_ENABLE(null) */,
    /*public static final*/ TLS_CA_LIST_FILE /* = new TLS_CA_LIST_FILE(null) */,
    /*public static final*/ TLS_CERTIFICATE_FILE /* = new TLS_CERTIFICATE_FILE(null) */,
    /*public static final*/ TLS_PRIVATE_KEY_FILE /* = new TLS_PRIVATE_KEY_FILE(null) */,
    /*public static final*/ TLS_PASSWORD /* = new TLS_PASSWORD(null) */,
    /*public static final*/ TLS_METHOD /* = new TLS_METHOD(null) */,
    /*public static final*/ TLS_CIPHERS /* = new TLS_CIPHERS(null) */,
    /*public static final*/ TLS_SERVER_NAME /* = new TLS_SERVER_NAME(null) */,
    /*public static final*/ TLS_VERIFY_SERVER /* = new TLS_VERIFY_SERVER(null) */,
    /*public static final*/ TLS_VERIFY_CLIENT /* = new TLS_VERIFY_CLIENT(null) */,
    /*public static final*/ TLS_REQUIRE_CLIENT_CERTIFICATE /* = new TLS_REQUIRE_CLIENT_CERTIFICATE(null) */,
    /*public static final*/ TLS_NEGOTIATION_TIMEOUT_SEC /* = new TLS_NEGOTIATION_TIMEOUT_SEC(null) */,
    /*public static final*/ ACCOUNT_REGISTERED_NAME /* = new ACCOUNT_REGISTERED_NAME(null) */,
    /*public static final*/ ACCOUNT_REGISTRATION_STATUS /* = new ACCOUNT_REGISTRATION_STATUS(null) */,
    /*public static final*/ ACCOUNT_REGISTRATION_STATE_CODE /* = new ACCOUNT_REGISTRATION_STATE_CODE(null) */,
    /*public static final*/ ACCOUNT_REGISTRATION_STATE_DESC /* = new ACCOUNT_REGISTRATION_STATE_DESC(null) */,
    /*public static final*/ SRTP_ENABLE /* = new SRTP_ENABLE(null) */,
    /*public static final*/ SRTP_KEY_EXCHANGE /* = new SRTP_KEY_EXCHANGE(null) */,
    /*public static final*/ SRTP_ENCRYPTION_ALGO /* = new SRTP_ENCRYPTION_ALGO(null) */,
    /*public static final*/ SRTP_RTP_FALLBACK /* = new SRTP_RTP_FALLBACK(null) */,
    /*public static final*/ RINGNS_ACCOUNT /* = new RINGNS_ACCOUNT(null) */,
    /*public static final*/ RINGNS_HOST /* = new RINGNS_HOST(null) */,
    /*public static final*/ DHT_PORT /* = new DHT_PORT(null) */,
    /*public static final*/ DHT_PUBLIC_IN /* = new DHT_PUBLIC_IN(null) */,
    /*public static final*/ PROXY_ENABLED /* = new PROXY_ENABLED(null) */,
    /*public static final*/ PROXY_SERVER /* = new PROXY_SERVER(null) */,
    /*public static final*/ PROXY_SERVER_LIST /* = new PROXY_SERVER_LIST(null) */,
    /*public static final*/ PROXY_PUSH_TOKEN /* = new PROXY_PUSH_TOKEN(null) */,
    /*public static final*/ MANAGER_URI /* = new MANAGER_URI(null) */,
    /*public static final*/ MANAGER_USERNAME /* = new MANAGER_USERNAME(null) */;
    private final java.lang.String mKey = null;
    private final boolean isTwoState = false;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.ConfigKey.Companion Companion = null;
    
    public final boolean isTwoState() {
        return false;
    }
    
    ConfigKey(java.lang.String key) {
    }
    
    ConfigKey(java.lang.String key, boolean isBool) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String key() {
        return null;
    }
    
    public final boolean equals(@org.jetbrains.annotations.Nullable()
    net.jami.model.ConfigKey other) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    @kotlin.jvm.JvmStatic()
    public static final net.jami.model.ConfigKey fromString(@org.jetbrains.annotations.NotNull()
    java.lang.String stringKey) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/model/ConfigKey$Companion;", "", "()V", "fromString", "Lnet/jami/model/ConfigKey;", "stringKey", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        @kotlin.jvm.JvmStatic()
        public final net.jami.model.ConfigKey fromString(@org.jetbrains.annotations.NotNull()
        java.lang.String stringKey) {
            return null;
        }
    }
}