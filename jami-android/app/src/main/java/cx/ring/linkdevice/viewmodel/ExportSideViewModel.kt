/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.linkdevice.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.jami.services.AccountService
import net.jami.services.AccountService.AuthError
import net.jami.services.AccountService.AuthState
import net.jami.services.AccountService.AuthResult
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named


sealed class AddDeviceExportState {
    data class Init(val error: ExportSideInputError? = null) : AddDeviceExportState()
    data object TokenAvailable : AddDeviceExportState()
    data object Connecting : AddDeviceExportState()
    data class Authenticating(val peerAddress: String?) : AddDeviceExportState()
    data object InProgress : AddDeviceExportState()
    data class Done(val error: AuthError? = null) : AddDeviceExportState()
}

enum class ExportSideInputError {
    INVALID_INPUT,
}

@HiltViewModel
class ExportSideViewModel @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : AuthStateListener, ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow<AddDeviceExportState>(AddDeviceExportState.Init())
    val uiState: StateFlow<AddDeviceExportState> = _uiState.asStateFlow()

    private val accountId = accountService.currentAccount!!.accountId
    private var operationId: Long? = null

    private var compositeDisposable = CompositeDisposable()

    fun onAuthenticationUri(jamiAuthentication: String) {
        // Verify the input.
        if (jamiAuthentication.isEmpty()
            || !jamiAuthentication.startsWith("jami-auth://")
            || (jamiAuthentication.length != 59)
        ) {
            Log.w(TAG, "Invalid input: $jamiAuthentication")
            // If state is unchanged, error is not emitted again.
            _uiState.value = AddDeviceExportState.Init()
            _uiState.value = AddDeviceExportState.Init(ExportSideInputError.INVALID_INPUT)
            return
        }

        val operationId = accountService.addDevice(accountId, jamiAuthentication)
        if (operationId < 0) {
            Log.e(TAG, "Failed to add device: $jamiAuthentication $operationId")
            _uiState.value = when (operationId) {
                -1L -> AddDeviceExportState.Init(ExportSideInputError.INVALID_INPUT)
                else -> AddDeviceExportState.Done(AuthError.UNKNOWN)
            }
            return
        }
        this.operationId = operationId

        assert(compositeDisposable.size() == 0) // Should not have any subscription.

        accountService.authResultObservable
            .filter { it.accountId == accountId && it.operationId == operationId }
            .observeOn(mUiScheduler)
            .subscribe(this::updateAuthState)
            .apply { compositeDisposable.add(this) }
    }

    fun onIdentityConfirmation() {
        accountService.confirmAddDevice(accountId, operationId!!)
    }

    fun onCancel() {
        val operationId = operationId ?: return
        accountService.cancelAddDevice(accountId, operationId)
    }

    // There is no token to be received in export side.
    override fun onTokenAvailableSignal(details: Map<String, String>) {
        throw UnsupportedOperationException()
    }

    override fun onConnectingSignal() {
        _uiState.value = AddDeviceExportState.Connecting
    }

    override fun onAuthenticatingSignal(details: Map<String, String>) {
        val peerAddress = details[EXPORT_PEER_ADDRESS_KEY]
        _uiState.value = AddDeviceExportState.Authenticating(peerAddress)
    }

    override fun onInProgressSignal() {
        _uiState.value = AddDeviceExportState.InProgress
    }

    override fun onDoneSignal(details: Map<String, String>) {
        val error = details[EXPORT_ERROR_KEY]
            ?.let { if (it.isEmpty() || it == "none") null else AuthError.fromString(it) }
        _uiState.value = AddDeviceExportState.Done(error)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        if (_uiState.value !is AddDeviceExportState.Done) onCancel()
    }

    private fun updateAuthState(authenticationResult: AuthResult) {
        Log.d(TAG, "Processing signal: ${authenticationResult.accountId}:${authenticationResult.operationId}:${authenticationResult.state} ${authenticationResult.details}")
        if (!checkNewStateValidity(authenticationResult.state)) {
            Log.e(TAG, "Invalid state transition: ${_uiState.value}->${authenticationResult.state}")
            throw IllegalStateException("Invalid state transition")
        }
        val details = authenticationResult.details
        when (authenticationResult.state) {
            AuthState.INIT -> onInitSignal()
            AuthState.TOKEN_AVAILABLE -> throw UnsupportedOperationException()
            AuthState.CONNECTING -> onConnectingSignal()
            AuthState.AUTHENTICATING -> onAuthenticatingSignal(details)
            AuthState.IN_PROGRESS -> onInProgressSignal()
            AuthState.DONE -> onDoneSignal(details)
        }
    }

    /**
     * Check if the new state is valid from the current state.
     */
    private fun checkNewStateValidity(newState: AuthState): Boolean {
        return newState in when (_uiState.value) {
            is AddDeviceExportState.Init -> listOf(AuthState.CONNECTING, AuthState.DONE)
            is AddDeviceExportState.TokenAvailable -> emptyList()
            is AddDeviceExportState.Connecting -> listOf(AuthState.AUTHENTICATING, AuthState.DONE)
            is AddDeviceExportState.Authenticating -> listOf(AuthState.IN_PROGRESS, AuthState.DONE)
            is AddDeviceExportState.InProgress -> listOf(AuthState.DONE, AuthState.DONE)
            is AddDeviceExportState.Done -> listOf(AuthState.DONE)
        }
    }

    companion object {
        private val TAG = ExportSideViewModel::class.simpleName!!
        const val EXPORT_PEER_ADDRESS_KEY = "peer_address"
        const val EXPORT_ERROR_KEY = "error"
    }
}