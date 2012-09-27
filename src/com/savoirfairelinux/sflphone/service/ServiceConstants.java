package com.savoirfairelinux.sflphone.service;

public final class ServiceConstants {
    
    public static final String INTENT_SIP_SERVICE = "com.savoirfairelinux.sflphone.service.SipService";
    public static final String EXTRA_OUTGOING_ACTIVITY = "outgoing_activity";

    public static final String CONFIG_ACCOUNT_TYPE = "Account.type";
    public static final String CONFIG_ACCOUNT_ALIAS = "Account.alias";
    public static final String CONFIG_ACCOUNT_MAILBOX = "Account.mailbox";
    public static final String CONFIG_ACCOUNT_ENABLE = "Account.enable";
    public static final String CONFIG_ACCOUNT_REGISTRATION_EXPIRE = "Account.registrationExpire";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATUS = "Account.registrationStatus";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATE_CODE = "Account.registrationCode";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATE_DESC = "Account.registrationDescription";
    public static final String CONFIG_CREDENTIAL_NUMBER = "Credential.count";
    public static final String CONFIG_ACCOUNT_DTMF_TYPE = "Account.dtmfType";
    public static final String CONFIG_RINGTONE_PATH = "Account.ringtonePath";
    public static final String CONFIG_RINGTONE_ENABLED = "Account.ringtoneEnabled";
    public static final String CONFIG_KEEP_ALIVE_ENABLED = "Account.keepAliveEnabled";

    public static final String CONFIG_ACCOUNT_HOSTNAME = "Account.hostname";
    public static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    public static final String CONFIG_ACCOUNT_ROUTESET = "Account.routeset";
    public static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";
    public static final String CONFIG_ACCOUNT_REALM = "Account.realm";
    public static final String CONFIG_ACCOUNT_DEFAULT_REALM = "*";
    public static final String CONFIG_ACCOUNT_USERAGENT = "Account.useragent";
    public static final String CONFIG_ACCOUNT_AUTOANSWER = "Account.autoAnswer";

    public static final String CONFIG_LOCAL_INTERFACE = "Account.localInterface";
    public static final String CONFIG_INTERFACE = "Account.interface";
    public static final String CONFIG_PUBLISHED_SAMEAS_LOCAL = "Account.publishedSameAsLocal";
    public static final String CONFIG_LOCAL_PORT = "Account.localPort";
    public static final String CONFIG_PUBLISHED_PORT = "Account.publishedPort";
    public static final String CONFIG_PUBLISHED_ADDRESS = "Account.publishedAddress";
    public static final String CONFIG_DEFAULT_LOCAL_PORT = "5060";
    public static final String CONFIG_DEFAULT_PUBLISHED_PORT = "5060";
    public static final String CONFIG_DEFAULT_PUBLISHED_SAMEAS_LOCAL = "true";
    public static final String CONFIG_DEFAULT_INTERFACE = "default";

    public static final String CONFIG_DISPLAY_NAME = "Account.displayName";
    public static final String CONFIG_DEFAULT_ADDRESS = "0.0.0.0";

    public static final String CONFIG_STUN_SERVER = "STUN.server";
    public static final String CONFIG_STUN_ENABLE = "STUN.enable";

    // SRTP specific parameters
    public static final String CONFIG_SRTP_ENABLE = "SRTP.enable";
    public static final String CONFIG_SRTP_KEY_EXCHANGE = "SRTP.keyExchange";
    public static final String CONFIG_SRTP_ENCRYPTION_ALGO = "SRTP.encryptionAlgorithm";  // Provided by ccRTP,0=NULL,1=AESCM,2=AESF8
    public static final String CONFIG_SRTP_RTP_FALLBACK = "SRTP.rtpFallback";
    public static final String CONFIG_ZRTP_HELLO_HASH = "ZRTP.helloHashEnable";
    public static final String CONFIG_ZRTP_DISPLAY_SAS = "ZRTP.displaySAS";
    public static final String CONFIG_ZRTP_NOT_SUPP_WARNING = "ZRTP.notSuppWarning";
    public static final String CONFIG_ZRTP_DISPLAY_SAS_ONCE = "ZRTP.displaySasOnce";

    public static final String CONFIG_TLS_LISTENER_PORT = "TLS.listenerPort";
    public static final String CONFIG_TLS_ENABLE = "TLS.enable";
    public static final String CONFIG_TLS_CA_LIST_FILE = "TLS.certificateListFile";
    public static final String CONFIG_TLS_CERTIFICATE_FILE = "TLS.certificateFile";
    public static final String CONFIG_TLS_PRIVATE_KEY_FILE = "TLS.privateKeyFile";
    public static final String CONFIG_TLS_PASSWORD = "TLS.password";
    public static final String CONFIG_TLS_METHOD = "TLS.method";
    public static final String CONFIG_TLS_CIPHERS = "TLS.ciphers";
    public static final String CONFIG_TLS_SERVER_NAME = "TLS.serverName";
    public static final String CONFIG_TLS_VERIFY_SERVER = "TLS.verifyServer";
    public static final String CONFIG_TLS_VERIFY_CLIENT = "TLS.verifyClient";
    public static final String CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE = "TLS.requireClientCertificate";
    public static final String CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC = "TLS.negotiationTimeoutSec";
    public static final String CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC = "TLS.negotiationTimemoutMsec";

}
