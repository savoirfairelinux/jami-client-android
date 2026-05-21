/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package net.jami.services

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.JamiService

enum class PeerServicesStatus(val code: Int) {
    OK(0),
    NO_DEVICES(1),
    UNREACHABLE(2),
    TIMEOUT(3),
    INTERNAL_ERROR(4);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: INTERNAL_ERROR
    }
}

data class PeerServiceInfo(
    val id: String,
    val name: String,
    val description: String,
    val scheme: String,
    val device: String,
)

data class TunnelInfo(
    val tunnelId: String,
    val accountId: String,
    val peerUri: String,
    val serviceId: String,
    val serviceName: String,
    val localPort: Int,
)

data class PeerServicesResult(
    val requestId: Long,
    val accountId: String,
    val peerId: String,
    val status: PeerServicesStatus,
    val services: List<PeerServiceInfo>,
)

data class TunnelOpenedEvent(val accountId: String, val tunnelId: String, val localPort: Int)
data class TunnelClosedEvent(val accountId: String, val tunnelId: String, val reason: String)

class PeerServicesService {

    private val peerServicesSubject: Subject<PeerServicesResult> = PublishSubject.create()
    private val tunnelOpenedSubject: Subject<TunnelOpenedEvent> = PublishSubject.create()
    private val tunnelClosedSubject: Subject<TunnelClosedEvent> = PublishSubject.create()

    val peerServicesReceived: Observable<PeerServicesResult> = peerServicesSubject
    val tunnelOpened: Observable<TunnelOpenedEvent> = tunnelOpenedSubject
    val tunnelClosed: Observable<TunnelClosedEvent> = tunnelClosedSubject


    fun onPeerServicesReceived(requestId: Long, accountId: String, peerId: String, status: Int, servicesJson: String) {
        val services = parseServicesJson(servicesJson)
        peerServicesSubject.onNext(
            PeerServicesResult(requestId, accountId, peerId, PeerServicesStatus.fromCode(status), services)
        )
    }

    fun onTunnelOpened(accountId: String, tunnelId: String, localPort: Int) {
        tunnelOpenedSubject.onNext(TunnelOpenedEvent(accountId, tunnelId, localPort))
    }

    fun onTunnelClosed(accountId: String, tunnelId: String, reason: String) {
        tunnelClosedSubject.onNext(TunnelClosedEvent(accountId, tunnelId, reason))
    }

    fun queryPeerServices(accountId: String, peerUri: String): Long =
        JamiService.queryPeerServices(accountId, peerUri)

    fun openServiceTunnel(
        accountId: String,
        peerUri: String,
        deviceId: String,
        serviceId: String,
        serviceName: String,
        localPort: Int = 0,
    ): String = JamiService.openServiceTunnel(accountId, peerUri, deviceId, serviceId, serviceName, localPort)

    fun closeServiceTunnel(accountId: String, tunnelId: String): Boolean =
        JamiService.closeServiceTunnel(accountId, tunnelId)

    fun getActiveTunnels(accountId: String): List<TunnelInfo> =
        JamiService.getActiveTunnels(accountId).toNative().map { map ->
            TunnelInfo(
                tunnelId    = map["tunnelId"] ?: "",
                accountId   = accountId,
                peerUri     = map["peerUri"] ?: "",
                serviceId   = map["serviceId"] ?: "",
                serviceName = map["serviceName"] ?: "",
                localPort   = map["localPort"]?.toIntOrNull() ?: 0,
            )
        }

    private fun parseServicesJson(json: String): List<PeerServiceInfo> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val result = mutableListOf<PeerServiceInfo>()
            var pos = json.indexOf('[')
            if (pos < 0) return emptyList()
            pos++
            while (pos < json.length) {
                val objStart = json.indexOf('{', pos)
                if (objStart < 0) break
                val objEnd = json.indexOf('}', objStart)
                if (objEnd < 0) break
                val obj = json.substring(objStart + 1, objEnd)
                result.add(
                    PeerServiceInfo(
                        id          = extractField(obj, "id"),
                        name        = extractField(obj, "name"),
                        description = extractField(obj, "description"),
                        scheme      = extractField(obj, "scheme"),
                        device      = extractField(obj, "device"),
                    )
                )
                pos = objEnd + 1
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractField(obj: String, key: String): String {
        val pattern = "\"$key\""
        val keyIdx = obj.indexOf(pattern)
        if (keyIdx < 0) return ""
        val colonIdx = obj.indexOf(':', keyIdx + pattern.length)
        if (colonIdx < 0) return ""
        val valueStart = obj.indexOf('"', colonIdx + 1)
        if (valueStart < 0) return ""
        val valueEnd = obj.indexOf('"', valueStart + 1)
        if (valueEnd < 0) return ""
        return obj.substring(valueStart + 1, valueEnd)
    }
}
