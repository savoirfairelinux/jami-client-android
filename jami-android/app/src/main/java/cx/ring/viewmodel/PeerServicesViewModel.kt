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
        observeSignals()
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, services = emptyList()) }
        pendingRequestId = peerServicesService.queryPeerServices(accountId, peerUri)
    }

    fun openTunnel(service: PeerServiceInfo) {
        peerServicesService.openTunnel(accountId, peerUri, service)
    }

    fun closeTunnel(serviceId: String) {
        peerServicesService.closeTunnel(accountId, peerUri, serviceId)
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
            peerServicesService.observeTunnelState(accountId, peerUri)
                .observeOn(uiScheduler)
                .subscribe { tunnelState ->
                    val previousActive = _uiState.value.activeTunnels
                    _uiState.update { it.copy(
                        activeTunnels  = tunnelState.activeTunnels,
                        pendingTunnels = tunnelState.pendingServices,
                    )}
                    val newServiceIds = tunnelState.activeTunnels.keys - previousActive.keys
                    for (serviceId in newServiceIds) {
                        val service = _uiState.value.services.firstOrNull { it.id == serviceId } ?: continue
                        val scheme = service.scheme.lowercase()
                        if (scheme == "http" || scheme == "https") {
                            val port = tunnelState.activeTunnels[serviceId]?.localPort ?: continue
                            _launchUrl.tryEmit("${service.scheme}://127.0.0.1:$port")
                        }
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
