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

import com.google.gson.JsonParser
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.JamiService
import net.jami.utils.Log
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

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
    val scheme: String,
    val localPort: Int,
)

data class PeerTunnelState(
    val activeTunnels: Map<String, TunnelInfo> = emptyMap(),
    val pendingServices: Set<String> = emptySet(),
)

data class PeerServicesResult(
    val requestId: Long,
    val accountId: String,
    val peerId: String,
    val status: PeerServicesStatus,
    val services: List<PeerServiceInfo>,
)

class PeerServicesService {

    private val peerServicesSubject: Subject<PeerServicesResult> = PublishSubject.create()
    val peerServicesReceived: Observable<PeerServicesResult> = peerServicesSubject
    private val tunnelStates = ConcurrentHashMap<String, BehaviorSubject<PeerTunnelState>>()
    private val tunnelOpenedSubject: Subject<TunnelInfo> = PublishSubject.create()

    private val activeTunnelIds: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val anyActiveTunnelSubject: Subject<Boolean> = BehaviorSubject.createDefault(false).toSerialized()

    // Emits true when at least one tunnel is active across any account/peer
    fun observeAnyActiveTunnel(): Observable<Boolean> = anyActiveTunnelSubject.distinctUntilChanged()

    // Pending-open queue for matching synchronous TunnelOpened callbacks to the service that was
    // requested. openTunnel() enqueues an entry BEFORE calling into the daemon because the daemon
    // fires TunnelOpened synchronously on the caller's thread.
    private data class PendingOpen(
        val accountId: String,
        val peerUri: String,
        val serviceId: String,
        val serviceName: String,
        val scheme: String,
    )
    private val pendingOpens = ArrayDeque<PendingOpen>()


    fun observeTunnelState(accountId: String, peerUri: String): Observable<PeerTunnelState> =
        getOrCreateState(accountId, peerUri)

    fun observeTunnelUrl(accountId: String, peerUri: String): Observable<String> =
        tunnelOpenedSubject
            .filter { it.accountId == accountId && it.peerUri == peerUri }
            .filter { it.scheme.equals("http", ignoreCase = true) || it.scheme.equals("https", ignoreCase = true) }
            .map { "${it.scheme}://127.0.0.1:${it.localPort}" }

    private fun stateKey(accountId: String, peerUri: String) = "$accountId\u0000$peerUri"

    private fun getOrCreateState(accountId: String, peerUri: String): BehaviorSubject<PeerTunnelState> =
        tunnelStates.getOrPut(stateKey(accountId, peerUri)) {
            BehaviorSubject.createDefault(loadTunnelState(accountId, peerUri))
        }

    private fun loadTunnelState(accountId: String, peerUri: String): PeerTunnelState {
        val active = JamiService.getActiveTunnels(accountId)
            .filter { it["peerUri"] == peerUri }
            .mapNotNull { map ->
                val tunnelId  = map["id"]?.takeIf       { it.isNotEmpty() } ?: return@mapNotNull null
                val serviceId = map["serviceId"]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                serviceId to TunnelInfo(
                    tunnelId    = tunnelId,
                    accountId   = accountId,
                    peerUri     = peerUri,
                    serviceId   = serviceId,
                    serviceName = map["serviceName"] ?: "",
                    scheme      = map["scheme"] ?: "",
                    localPort   = map["localPort"]?.toIntOrNull() ?: 0,
                )
            }
            .toMap()
        active.values.forEach { activeTunnelIds.add(it.tunnelId) }
        if (activeTunnelIds.isNotEmpty()) anyActiveTunnelSubject.onNext(true)
        return PeerTunnelState(activeTunnels = active)
    }

    fun openTunnel(accountId: String, peerUri: String, service: PeerServiceInfo) {
        val subject = getOrCreateState(accountId, peerUri)
        val current = subject.value!!
        pendingOpens.addLast(PendingOpen(accountId, peerUri, service.id, service.name, service.scheme))
        subject.onNext(current.copy(pendingServices = current.pendingServices + service.id))

        val tunnelId = JamiService.openServiceTunnel(accountId, peerUri, service.device, service.id, service.name, 0)
        if (tunnelId.isEmpty()) {
            pendingOpens.removeLastOrNull()
            subject.onNext(subject.value!!.copy(pendingServices = subject.value!!.pendingServices - service.id))
        }
    }

    fun closeTunnel(accountId: String, peerUri: String, serviceId: String) {
        val tunnelId = tunnelStates[stateKey(accountId, peerUri)]
            ?.value?.activeTunnels?.get(serviceId)?.tunnelId ?: return
        JamiService.closeServiceTunnel(accountId, tunnelId)
    }

    fun queryPeerServices(accountId: String, peerUri: String): Observable<PeerServicesResult> {
        val requestId = JamiService.queryPeerServices(accountId, peerUri)
        return peerServicesSubject
            .filter { it.requestId == requestId && it.accountId == accountId && it.peerId == peerUri }
            .take(1)
    }

    fun onPeerServicesReceived(requestId: Long, accountId: String, peerId: String, status: Int, servicesJson: String) {
        peerServicesSubject.onNext(
            PeerServicesResult(requestId, accountId, peerId, PeerServicesStatus.fromCode(status), parseServicesJson(servicesJson))
        )
    }

    fun onTunnelOpened(accountId: String, tunnelId: String, localPort: Int) {
        activeTunnelIds.add(tunnelId)
        anyActiveTunnelSubject.onNext(true)
        val pending = pendingOpens.removeFirstOrNull() ?: return
        val subject = tunnelStates[stateKey(pending.accountId, pending.peerUri)] ?: return
        val current = subject.value!!
        val info = TunnelInfo(
            tunnelId    = tunnelId,
            accountId   = pending.accountId,
            peerUri     = pending.peerUri,
            serviceId   = pending.serviceId,
            serviceName = pending.serviceName,
            scheme      = pending.scheme,
            localPort   = localPort,
        )
        subject.onNext(current.copy(
            activeTunnels   = current.activeTunnels + (pending.serviceId to info),
            pendingServices = current.pendingServices - pending.serviceId,
        ))
        tunnelOpenedSubject.onNext(info)
    }

    fun refreshActiveTunnels() {
        try {
            val accounts = JamiService.getAccountList()
            for (i in 0 until accounts.size) {
                val account = accounts[i]
                if (account.isEmpty()) continue
                JamiService.getActiveTunnels(account).forEach { map ->
                    map["id"]?.takeIf { it.isNotEmpty() }?.let { activeTunnelIds.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshActiveTunnels failed", e)
        }
        anyActiveTunnelSubject.onNext(activeTunnelIds.isNotEmpty())
    }

    fun getAllActiveTunnels(): List<TunnelInfo> {
        val result = mutableListOf<TunnelInfo>()
        try {
            val accounts = JamiService.getAccountList()
            for (i in 0 until accounts.size) {
                val account = accounts[i]
                if (account.isEmpty()) continue
                JamiService.getActiveTunnels(account).forEach { map ->
                    val tunnelId = map["id"]?.takeIf { it.isNotEmpty() } ?: return@forEach
                    result.add(TunnelInfo(
                        tunnelId    = tunnelId,
                        accountId   = account,
                        peerUri     = map["peerUri"] ?: "",
                        serviceId   = map["serviceId"] ?: "",
                        serviceName = map["serviceName"] ?: "",
                        scheme      = map["scheme"] ?: "",
                        localPort   = map["localPort"]?.toIntOrNull() ?: 0,
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllActiveTunnels failed", e)
        }
        return result
    }

    fun closeAllTunnels() {
        try {
            val accounts = JamiService.getAccountList()
            for (i in 0 until accounts.size) {
                val account = accounts[i]
                if (account.isEmpty()) continue
                JamiService.getActiveTunnels(account).forEach { map ->
                    val tunnelId = map["id"]?.takeIf { it.isNotEmpty() } ?: return@forEach
                    JamiService.closeServiceTunnel(account, tunnelId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "closeAllTunnels failed", e)
        }
    }

    fun onTunnelClosed(accountId: String, tunnelId: String, @Suppress("UNUSED_PARAMETER") reason: String) {
        activeTunnelIds.remove(tunnelId)
        anyActiveTunnelSubject.onNext(activeTunnelIds.isNotEmpty())
        for (subject in tunnelStates.values) {
            val current = subject.value ?: continue
            val serviceId = current.activeTunnels.entries
                .firstOrNull { it.value.tunnelId == tunnelId }?.key ?: continue
            subject.onNext(current.copy(activeTunnels = current.activeTunnels - serviceId))
            break
        }
    }

    private fun parseServicesJson(json: String): List<PeerServiceInfo> {
        if (json.isBlank()) return emptyList()
        return try {
            JsonParser.parseString(json).asJsonArray.map { el ->
                val obj = el.asJsonObject
                PeerServiceInfo(
                    id          = obj["id"]?.asString          ?: "",
                    name        = obj["name"]?.asString        ?: "",
                    description = obj["description"]?.asString ?: "",
                    scheme      = obj["scheme"]?.asString      ?: "",
                    device      = obj["device"]?.asString      ?: "",
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        const val TAG =  "PeerServicesService"
    }
}
