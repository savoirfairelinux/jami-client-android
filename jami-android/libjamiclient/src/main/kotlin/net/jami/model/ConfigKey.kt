/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.model

enum class ConfigKey(val key: String, val isBool: Boolean = false) {
    MAILBOX("Account.mailbox"),
    REGISTRATION_EXPIRE("Account.registrationExpire"),
    CREDENTIAL_NUMBER("Credential.count"),
    ACCOUNT_DTMF_TYPE("Account.dtmfType"),
    RINGTONE_PATH("Account.ringtonePath"),
    RINGTONE_ENABLED("Account.ringtoneEnabled", true),
    RINGTONE_CUSTOM("Account.ringtoneCustom"),
    KEEP_ALIVE_ENABLED("Account.keepAliveEnabled", true),
    LOCAL_INTERFACE("Account.localInterface"),
    PUBLISHED_SAMEAS_LOCAL("Account.publishedSameAsLocal", true),
    LOCAL_PORT("Account.localPort"),
    PUBLISHED_PORT("Account.publishedPort"),
    PUBLISHED_ADDRESS("Account.publishedAddress"),
    STUN_SERVER("STUN.server"),
    STUN_ENABLE("STUN.enable", true),
    TURN_ENABLE("TURN.enable", true),
    TURN_SERVER("TURN.server"),
    TURN_USERNAME("TURN.username"),
    TURN_PASSWORD("TURN.password"),
    TURN_REALM("TURN.realm"),
    AUDIO_PORT_MIN("Account.audioPortMin"),
    AUDIO_PORT_MAX("Account.audioPortMax"),
    ACCOUNT_USERAGENT("Account.useragent"),
    ACCOUNT_UPNP_ENABLE("Account.upnpEnabled", true),
    ACCOUNT_ROUTESET("Account.routeset"),
    ACCOUNT_AUTOANSWER("Account.autoAnswer", true),
    ACCOUNT_ISRENDEZVOUS("Account.rendezVous", true),
    ACCOUNT_ALIAS("Account.alias"),
    ACCOUNT_HOSTNAME("Account.hostname"),
    ACCOUNT_USERNAME("Account.username"),
    ACCOUNT_PASSWORD("Account.password"),
    ACCOUNT_REALM("Account.realm"),
    ACCOUNT_TYPE("Account.type"),
    ACCOUNT_ENABLE("Account.enable", true),
    ACCOUNT_ACTIVE("Account.active", true),
    ACCOUNT_DEVICE_ID("Account.deviceID"),
    ACCOUNT_DEVICE_NAME("Account.deviceName"),
    ACCOUNT_PEER_DISCOVERY("Account.peerDiscovery", true),
    ACCOUNT_DISCOVERY("Account.accountDiscovery", true),
    ACCOUNT_PUBLISH("Account.accountPublish", true),
    ACCOUNT_DISPLAYNAME("Account.displayName"),
    VIDEO_ENABLED("Account.videoEnabled", true),
    VIDEO_PORT_MIN("Account.videoPortMin"),
    VIDEO_PORT_MAX("Account.videoPortMax"),
    PRESENCE_ENABLE("Account.presenceEnabled", true),
    ARCHIVE_PASSWORD("Account.archivePassword"),
    ARCHIVE_HAS_PASSWORD("Account.archiveHasPassword", true),
    ARCHIVE_PIN("Account.archivePIN"),
    ARCHIVE_PATH("Account.archivePath"),
    ARCHIVE_URL("Account.archiveURL"),
    DISPLAY_NAME("Account.displayName"),
    ETH_ACCOUNT("ETH.account"),
    TLS_LISTENER_PORT("TLS.listenerPort"),
    TLS_ENABLE("TLS.enable"),
    TLS_CA_LIST_FILE("TLS.certificateListFile"),
    TLS_CERTIFICATE_FILE("TLS.certificateFile"),
    TLS_PRIVATE_KEY_FILE("TLS.privateKeyFile"),
    TLS_PASSWORD("TLS.password"),
    TLS_METHOD("TLS.method"),
    TLS_CIPHERS("TLS.ciphers"),
    TLS_SERVER_NAME("TLS.serverName"),
    TLS_VERIFY_SERVER("TLS.verifyServer"),
    TLS_VERIFY_CLIENT("TLS.verifyClient"),
    TLS_REQUIRE_CLIENT_CERTIFICATE("TLS.requireClientCertificate"),
    TLS_NEGOTIATION_TIMEOUT_SEC("TLS.negotiationTimeoutSec"),
    ACCOUNT_REGISTERED_NAME("Account.registeredName"),
    ACCOUNT_REGISTRATION_STATUS("Account.registrationStatus"),
    ACCOUNT_REGISTRATION_STATE_CODE("Account.registrationCode"),
    ACCOUNT_REGISTRATION_STATE_DESC("Account.registrationDescription"),
    SRTP_ENABLE("SRTP.enable"),
    SRTP_KEY_EXCHANGE("SRTP.keyExchange"),
    SRTP_ENCRYPTION_ALGO("SRTP.encryptionAlgorithm"),
    SRTP_RTP_FALLBACK("SRTP.rtpFallback"),
    RINGNS_HOST("RingNS.uri"),
    DHT_PORT("DHT.port"),
    DHT_PUBLIC_IN("DHT.PublicInCalls", true),
    PROXY_ENABLED("Account.proxyEnabled", true),
    PROXY_LIST_ENABLED("Account.proxyListEnabled", true),
    PROXY_SERVER("Account.proxyServer"),
    PROXY_SERVER_LIST("Account.dhtProxyListUrl"),
    PROXY_PUSH_TOKEN("Account.proxyPushToken"),
    PROXY_PUSH_PLATFORM("proxyPushPlatform"),
    PROXY_PUSH_TOPIC("proxyPushTopic"),
    MANAGER_URI("Account.managerUri"),
    MANAGER_USERNAME("Account.managerUsername"),
    UI_CUSTOMIZATION("Account.uiCustomization");

    companion object {
        private val keyMap = entries.associateByTo(HashMap(entries.size)) { it.key }
        fun fromString(stringKey: String): ConfigKey? = keyMap[stringKey]
    }
}
