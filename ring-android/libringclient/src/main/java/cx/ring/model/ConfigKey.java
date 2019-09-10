/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ConfigKey {
    MAILBOX("Account.mailbox"),
    REGISTRATION_EXPIRE("Account.registrationExpire"),
    CREDENTIAL_NUMBER("Credential.count"),
    ACCOUNT_DTMF_TYPE("Account.dtmfType"),
    RINGTONE_PATH("Account.ringtonePath"),
    RINGTONE_ENABLED("Account.ringtoneEnabled"),
    RINGTONE_CUSTOM("Account.ringtoneCustom"),
    KEEP_ALIVE_ENABLED("Account.keepAliveEnabled"),
    LOCAL_INTERFACE("Account.localInterface"),
    PUBLISHED_SAMEAS_LOCAL("Account.publishedSameAsLocal"),
    LOCAL_PORT("Account.localPort"),
    PUBLISHED_PORT("Account.publishedPort"),
    PUBLISHED_ADDRESS("Account.publishedAddress"),
    STUN_SERVER("STUN.server"),
    STUN_ENABLE("STUN.enable"),
    TURN_ENABLE("TURN.enable"),
    TURN_SERVER("TURN.server"),
    TURN_USERNAME("TURN.username"),
    TURN_PASSWORD("TURN.password"),
    TURN_REALM("TURN.realm"),
    AUDIO_PORT_MIN("Account.audioPortMin"),
    AUDIO_PORT_MAX("Account.audioPortMax"),
    ACCOUNT_USERAGENT("Account.useragent"),
    ACCOUNT_UPNP_ENABLE("Account.upnpEnabled"),
    ACCOUNT_ROUTESET("Account.routeset"),
    ACCOUNT_AUTOANSWER("Account.autoAnswer"),
    ACCOUNT_ALIAS("Account.alias"),
    ACCOUNT_HOSTNAME("Account.hostname"),
    ACCOUNT_USERNAME("Account.username"),
    ACCOUNT_PASSWORD("Account.password"),
    ACCOUNT_REALM("Account.realm"),
    ACCOUNT_TYPE("Account.type"),
    ACCOUNT_ENABLE("Account.enable"),
    ACCOUNT_ACTIVE("Account.active"),
    ACCOUNT_DEVICE_ID("Account.deviceID"),
    ACCOUNT_DEVICE_NAME("Account.deviceName"),
    VIDEO_ENABLED("Account.videoEnabled"),
    VIDEO_PORT_MIN("Account.videoPortMin"),
    VIDEO_PORT_MAX("Account.videoPortMax"),
    PRESENCE_ENABLE("Account.presenceEnabled"),
    ARCHIVE_PASSWORD("Account.archivePassword"),
    ARCHIVE_HAS_PASSWORD("Account.archiveHasPassword"),
    ARCHIVE_PIN("Account.archivePIN"),
    ARCHIVE_PATH("Account.archivePath"),
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
    ACCOUNT_REGISTERED_NAME("Account.registredName"),
    ACCOUNT_REGISTRATION_STATUS("Account.registrationStatus"),
    ACCOUNT_REGISTRATION_STATE_CODE("Account.registrationCode"),
    ACCOUNT_REGISTRATION_STATE_DESC("Account.registrationDescription"),
    SRTP_ENABLE("SRTP.enable"),
    SRTP_KEY_EXCHANGE("SRTP.keyExchange"),
    SRTP_ENCRYPTION_ALGO("SRTP.encryptionAlgorithm"),
    SRTP_RTP_FALLBACK("SRTP.rtpFallback"),
    RINGNS_ACCOUNT("RingNS.account"),
    RINGNS_HOST("RingNS.host"),
    DHT_PORT("DHT.port"),
    DHT_PUBLIC_IN("DHT.PublicInCalls"),
    PROXY_ENABLED("Account.proxyEnabled"),
    PROXY_SERVER("Account.proxyServer"),
    PROXY_PUSH_TOKEN("Account.proxyPushToken"),
    MANAGER_URI("Account.managerUri");

    private static final Set<ConfigKey> TWO_STATES = new HashSet<>(Arrays.asList(
            ACCOUNT_ENABLE,
            ACCOUNT_ACTIVE,
            VIDEO_ENABLED,
            RINGTONE_ENABLED,
            KEEP_ALIVE_ENABLED,
            PUBLISHED_SAMEAS_LOCAL,
            STUN_ENABLE, TURN_ENABLE,
            ACCOUNT_AUTOANSWER,
            ACCOUNT_UPNP_ENABLE,
            DHT_PUBLIC_IN,
            PROXY_ENABLED));

    private final String mKey;

    ConfigKey(String key) {
        mKey = key;
    }

    public String key() {
        return mKey;
    }

    public boolean equals(ConfigKey other) {
        return other != null && mKey.equals(other.mKey);
    }

    public boolean isTwoState() {
        return TWO_STATES.contains(this);
    }

    public static ConfigKey fromString(String stringKey) {
        for (ConfigKey confKey : ConfigKey.values()) {
            if (stringKey.contentEquals(confKey.mKey) || stringKey.equals(confKey.mKey)) {
                return confKey;
            }
        }
        return null;
    }
}
