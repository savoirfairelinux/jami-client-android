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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
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
    @ApplicationContext private val context: Context,
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
    // Replaced on every refresh() to cancel any in-flight query subscription.
    private var queryDisposable = Disposable.disposed()

    init {
        observeTunnelState()
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, services = emptyList()) }
        queryDisposable.dispose()
        queryDisposable = peerServicesService.queryPeerServices(accountId, peerUri)
            .observeOn(uiScheduler)
            .subscribe { result ->
                if (result.status == PeerServicesStatus.OK) {
                    _uiState.update { it.copy(isLoading = false, services = result.services, errorMessage = null) }
                } else {
                    val msg = context.getString(when (result.status) {
                        PeerServicesStatus.NO_DEVICES  -> cx.ring.R.string.peer_services_error_offline
                        PeerServicesStatus.UNREACHABLE -> cx.ring.R.string.peer_services_error_unreachable
                        PeerServicesStatus.TIMEOUT     -> cx.ring.R.string.peer_services_error_timeout
                        else                           -> cx.ring.R.string.peer_services_error_unknown
                    })
                    _uiState.update { it.copy(isLoading = false, services = emptyList(), errorMessage = msg) }
                }
            }
        disposables.add(queryDisposable)
    }

    fun openTunnel(service: PeerServiceInfo) {
        peerServicesService.openTunnel(accountId, peerUri, service)
    }

    fun closeTunnel(serviceId: String) {
        peerServicesService.closeTunnel(accountId, peerUri, serviceId)
    }

    private fun observeTunnelState() {
        disposables.add(
            peerServicesService.observeTunnelState(accountId, peerUri)
                .observeOn(uiScheduler)
                .subscribe { tunnelState ->
                    _uiState.update { it.copy(
                        activeTunnels  = tunnelState.activeTunnels,
                        pendingTunnels = tunnelState.pendingServices,
                    )}
                }
        )
        disposables.add(
            peerServicesService.observeTunnelUrl(accountId, peerUri)
                .observeOn(uiScheduler)
                .subscribe { url -> _launchUrl.tryEmit(url) }
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
