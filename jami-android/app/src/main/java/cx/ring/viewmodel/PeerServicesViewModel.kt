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
package cx.ring.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.services.PeerServiceInfo
import net.jami.services.PeerServicesService
import net.jami.services.PeerServicesStatus
import net.jami.services.TunnelInfo
import javax.inject.Inject
import javax.inject.Named

data class PeerServicesUiState(
    val isLoading: Boolean = false,
    val services: List<PeerServiceInfo> = emptyList(),
    val activeTunnels: Map<String, TunnelInfo> = emptyMap(),
    val pendingTunnels: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@HiltViewModel
class PeerServicesViewModel @Inject constructor(
    private val peerServicesService: PeerServicesService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val accountId: String = checkNotNull(savedState[ARG_ACCOUNT_ID])
    private val peerUri: String = checkNotNull(savedState[ARG_PEER_URI])

    private val _uiState = MutableStateFlow(PeerServicesUiState())
    val uiState: StateFlow<PeerServicesUiState> = _uiState.asStateFlow()

    private val _launchUrl = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val launchUrl: SharedFlow<String> = _launchUrl.asSharedFlow()

    private val disposables = CompositeDisposable()
    private var pendingRequestId: Long = 0L

    init {
        val existing = peerServicesService.getActiveTunnels(accountId)
            .filter { it.peerUri == peerUri }
            .associateBy { it.serviceId }
        if (existing.isNotEmpty())
            _uiState.update { it.copy(activeTunnels = existing) }

        observeSignals()
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, services = emptyList()) }
        pendingRequestId = peerServicesService.queryPeerServices(accountId, peerUri)
    }

    fun openTunnel(service: PeerServiceInfo) {
        // Register pending BEFORE calling the daemon — the daemon fires TunnelOpened
        // synchronously during openServiceTunnel (on the same UI thread), so pendingTunnels
        // must already be set when the event arrives.
        _uiState.update { state ->
            state.copy(pendingTunnels = state.pendingTunnels + service.id)
        }
        val tunnelId = peerServicesService.openServiceTunnel(
            accountId   = accountId,
            peerUri     = peerUri,
            deviceId    = service.device,
            serviceId   = service.id,
            serviceName = service.name,
        )
        if (tunnelId.isEmpty()) {
            // Daemon rejected the call immediately — clear the pending entry.
            _uiState.update { state ->
                state.copy(pendingTunnels = state.pendingTunnels - service.id)
            }
        }
    }

    fun closeTunnel(serviceId: String) {
        val tunnel = _uiState.value.activeTunnels[serviceId] ?: return
        peerServicesService.closeServiceTunnel(accountId, tunnel.tunnelId)
    }

    private fun observeSignals() {
        disposables.add(
            peerServicesService.peerServicesReceived
                .filter { it.requestId == pendingRequestId && it.accountId == accountId && it.peerId == peerUri }
                .observeOn(uiScheduler)
                .subscribe { result ->
                    if (result.status == PeerServicesStatus.OK) {
                        _uiState.update { it.copy(isLoading = false, services = result.services, errorMessage = null) }
                    } else {
                        val msg = when (result.status) {
                            PeerServicesStatus.NO_DEVICES  -> "Contact is offline"
                            PeerServicesStatus.UNREACHABLE -> "Contact is unreachable"
                            PeerServicesStatus.TIMEOUT     -> "Request timed out"
                            else                           -> "Could not retrieve services"
                        }
                        _uiState.update { it.copy(isLoading = false, services = emptyList(), errorMessage = msg) }
                    }
                }
        )

        disposables.add(
            peerServicesService.tunnelOpened
                .filter { it.accountId == accountId }
                .observeOn(uiScheduler)
                .subscribe { event ->
                    val state = _uiState.value
                    // Match by first unclaimed pending entry (same strategy as Qt client).
                    val pendingServiceId = state.pendingTunnels.firstOrNull()
                        ?: return@subscribe
                    val service = state.services.firstOrNull { it.id == pendingServiceId }
                        ?: return@subscribe
                    val newTunnel = TunnelInfo(
                        tunnelId    = event.tunnelId,
                        accountId   = accountId,
                        peerUri     = peerUri,
                        serviceId   = pendingServiceId,
                        serviceName = service.name,
                        localPort   = event.localPort,
                    )
                    _uiState.update { s ->
                        s.copy(
                            activeTunnels  = s.activeTunnels + (pendingServiceId to newTunnel),
                            pendingTunnels = s.pendingTunnels - pendingServiceId,
                        )
                    }
                    // Auto-launch browser for http/https on tunnel open.
                    val scheme = service.scheme.lowercase()
                    if (scheme == "http" || scheme == "https") {
                        _launchUrl.tryEmit("${service.scheme}://127.0.0.1:${event.localPort}")
                    }
                }
        )

        disposables.add(
            peerServicesService.tunnelClosed
                .filter { it.accountId == accountId }
                .observeOn(uiScheduler)
                .subscribe { event ->
                    _uiState.update { s ->
                        val serviceId = s.activeTunnels.entries
                            .firstOrNull { (_, t) -> t.tunnelId == event.tunnelId }?.key
                            ?: return@update s
                        s.copy(activeTunnels = s.activeTunnels - serviceId)
                    }
                }
        )
    }

    override fun onCleared() {
        disposables.clear()
        super.onCleared()
    }

    companion object {
        const val ARG_ACCOUNT_ID = "accountId"
        const val ARG_PEER_URI   = "peerUri"
    }
}
