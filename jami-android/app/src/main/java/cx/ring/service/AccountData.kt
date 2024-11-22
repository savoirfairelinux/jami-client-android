package cx.ring.service

class AccountData {
    companion object {
        val data = """
TURN.server turn.jami.net
Account.dhtProxyListUrl https://config.jami.net/proxyList
Account.publishedAddress 
Account.audioPortMin 16384
Account.alias Jami account
Account.videoPortMax 65534
Account.proxyEnabled true
Account.ringtoneEnabled true
Account.mailbox 
Account.useragent 
Account.sendComposing true
Account.deviceName 
Account.dhtPort 0
TLS.password 
TLS.privateKeyFile 
TURN.password ring
Account.presenceSubscribeSupported true
Account.displayName 
Account.proxyListEnabled true
Account.accountPublish false
Account.managerUsername 
RingNS.uri 
Account.videoPortMin 49152
Account.audioPortMax 32766
TURN.username ring
Account.allModeratorEnabled true
Account.allowCertFromContact true
Account.username 
Account.archiveHasPassword true
Account.managerUri 
Account.proxyServer dhtproxy.jami.net:[80-95]
Account.allowCertFromHistory true
DHT.PublicInCalls true
Account.sendReadReceipt true
TLS.certificateListFile 
Account.enable true
Account.dtmfType sipinfo
Account.localInterface default
TURN.realm ring
Account.videoEnabled true
Account.defaultModerators 
Account.upnpEnabled true
Account.hostname bootstrap.jami.net
Account.publishedSameAsLocal true
Account.accountDiscovery false
Account.activeCallLimit -1
Account.localModeratorsEnabled true
Account.type RING
Account.allowCertFromTrusted true
Account.autoAnswer false
Account.ringtonePath default.opus
TLS.certificateFile 
Account.uiCustomization 
Account.rendezVous false
Account.registeredName 
Account.peerDiscovery false
TURN.enable true
""".trimIndent()
    }
}

fun fillMap(data: String, registeredName: String): Map<String, String> {
    val map = mutableMapOf<String, String>()

    val lines = data.lines()

    for (line in lines) {
        val keyValue = line.split(" ", limit = 2)

        if (keyValue.isNotEmpty()) {
            val key = keyValue[0]
            val value =
                if (keyValue.size > 1) keyValue[1] else ""

            // Override the value for "Account.registeredName" with the input parameter
            map[key] = if (key == "Account.registeredName") registeredName else value
        }
    }

    return map
}