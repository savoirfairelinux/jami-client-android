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

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.services.AccountService
import net.jami.services.ExposedServiceInfo
import net.jami.services.ExposedServicesService
import net.jami.services.PeerServicesService
import net.jami.services.TunnelInfo
import net.jami.utils.Log
import javax.inject.Inject

@HiltViewModel
class ExposedServicesViewModel @Inject constructor(
    private val exposedServicesService: ExposedServicesService,
    private val peerServicesService: PeerServicesService,
    private val accountService: AccountService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExposedServicesUiState())
    val uiState: StateFlow<ExposedServicesUiState> = _uiState.asStateFlow()

    fun loadServices(accountId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val services = exposedServicesService.getExposedServices(accountId)
            _uiState.update { it.copy(services = services, isLoading = false) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading services", e)
            _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
        }
    }

    fun addService(accountId: String, service: ExposedServiceInfo) {
        try {
            Log.i(TAG, "addService: accountId='$accountId' service=$service")
            val serviceId = exposedServicesService.addExposedService(accountId, service)
            if (serviceId.isNotEmpty()) {
                Log.i(TAG, "addService: success, serviceId='$serviceId', reloading list")
                loadServices(accountId)
            } else {
                Log.e(TAG, "addService: daemon returned empty serviceId — service was NOT created")
                _uiState.update { it.copy(errorMessage = "Failed to add service") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding service", e)
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun updateService(accountId: String, service: ExposedServiceInfo) {
        try {
            val success = exposedServicesService.updateExposedService(accountId, service)
            if (success) {
                loadServices(accountId)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to update service") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating service", e)
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun removeService(accountId: String, serviceId: String) {
        try {
            val success = exposedServicesService.removeExposedService(accountId, serviceId)
            if (success) {
                loadServices(accountId)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to remove service") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing service", e)
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun toggleEnabled(accountId: String, service: ExposedServiceInfo) {
        try {
            val success = exposedServicesService.setServiceEnabled(accountId, service, !service.enabled)
            if (success) {
                loadServices(accountId)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to toggle service") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling service", e)
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun getActiveConnections(): Pair<List<HostedServiceItem>, List<TunnelInfo>> {
        val hosted = exposedServicesService.getRunningHostedServices().map { (accId, service) ->
            HostedServiceItem(service.name, accountLabel(accId))
        }
        val tunnels = peerServicesService.getAllActiveTunnels()
        return hosted to tunnels
    }

    private fun accountLabel(accountId: String): String {
        val account = accountService.getAccount(accountId) ?: return accountId
        return account.displayname.ifEmpty { account.displayUsername ?: accountId }
    }

    fun disconnectAll(accountId: String) {
        try {
            peerServicesService.closeAllTunnels()
            exposedServicesService.disableAllHostedServices()
            loadServices(accountId)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting all", e)
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    companion object {
        private const val TAG = "ExposedServicesViewModel"
    }
}

data class ExposedServicesUiState(
    val isLoading: Boolean = false,
    val services: List<ExposedServiceInfo> = emptyList(),
    val errorMessage: String? = null,
)

data class HostedServiceItem(
    val serviceName: String,
    val accountLabel: String,
)
