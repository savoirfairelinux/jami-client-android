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
import cx.ring.linkdevice.viewmodel.ImportSideViewModel.InputError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.asStateFlow
import net.jami.services.AccountService.AuthState
import net.jami.services.AccountService.AuthResult
import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.services.AccountService
import net.jami.services.AccountService.AuthError
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named


sealed class AddDeviceImportState {
    data object Init : AddDeviceImportState()
    data class TokenAvailable(val token: String) : AddDeviceImportState()
    data object Connecting : AddDeviceImportState()
    data class Authenticating(
        val id: String,
        val needPassword: Boolean,
        val registeredName: String? = null,
        val error: InputError?
    ) : AddDeviceImportState()

    data object InProgress : AddDeviceImportState()
    data class Done(val error: AuthError? = null) : AddDeviceImportState()
}

@HiltViewModel
class ImportSideViewModel @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : AuthStateListener, ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow<AddDeviceImportState>(AddDeviceImportState.Init)
    val uiState: StateFlow<AddDeviceImportState> = _uiState.asStateFlow()

    private var _tempAccount: Account? = null
    val tempAccount: Account?
        get() = _tempAccount

    private var compositeDisposable = CompositeDisposable()

    init {
        accountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
            .flatMapObservable { accountDetails: HashMap<String, String> ->
                accountDetails[ConfigKey.ARCHIVE_URL.key] = "jami-auth"
                accountService.addAccount(accountDetails)
            }
            .firstOrError()
            .flatMapObservable { account ->
                _tempAccount = account
                accountService.authResultObservable.filter { it.accountId == account.accountId }
            }
            .observeOn(mUiScheduler)
            .subscribe(this::updateDeviceAuthState)
            .apply { compositeDisposable.add(this) }
    }

    fun onAuthentication(password: String = "") {
        _tempAccount?.accountId?.apply {
            accountService.provideAccountAuthentication(this, password)
        }
    }

    fun onCancel() {
        _tempAccount?.accountId?.apply {
            accountService.removeAccount(this)
        }
    }

    override fun onTokenAvailableSignal(details: Map<String, String>) {
        val token = details[IMPORT_TOKEN_KEY] ?: throw IllegalStateException("Token not found")
        _uiState.value = AddDeviceImportState.TokenAvailable(token)
    }

    override fun onConnectingSignal() {
        _uiState.value = AddDeviceImportState.Connecting
    }

    override fun onAuthenticatingSignal(details: Map<String, String>) {
        val needPassword = details[IMPORT_AUTH_SCHEME_KEY] == "password"
        val authError = details[IMPORT_AUTH_ERROR_KEY]?.let {
            InputError.fromString(it)
        }
        val peerId = details[IMPORT_PEER_ID_KEY] ?: throw IllegalStateException("Jami ID not found")
        _tempAccount?.accountId?.apply {
            accountService.findRegistrationByAddress(this, "", peerId)
                .observeOn(mUiScheduler)
                .subscribe { registeredName ->
                    Log.d(TAG, "Registered name: ${registeredName.name}")
                    _uiState.value = AddDeviceImportState.Authenticating(
                        id = peerId,
                        needPassword = needPassword,
                        registeredName = registeredName.name,
                        error = authError
                    )
                }.apply { compositeDisposable.add(this) }
        }
        _uiState.value = AddDeviceImportState.Authenticating(
            id = peerId,
            needPassword = needPassword,
            error = authError
        )
    }

    override fun onInProgressSignal() {
        _uiState.value = AddDeviceImportState.InProgress
    }

    override fun onDoneSignal(details: Map<String, String>) {
        val error = details[IMPORT_ERROR_KEY]
            ?.let { if (it.isEmpty() || it == "none") null else AuthError.fromString(it) }
        _uiState.value = AddDeviceImportState.Done(error)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
        if (_uiState.value !is AddDeviceImportState.Done) onCancel()
    }

    private fun updateDeviceAuthState(result: AuthResult) {
        Log.d(TAG, "Processing signal: ${result.accountId}:${result.operationId}:${result.state} ${result.details}")
        if (!checkNewStateValidity(result.state)) {
            Log.e(TAG, "Invalid state transition: ${_uiState.value}->${result.state}")
            throw IllegalStateException("Invalid state transition")
        }
        val details = result.details
        when (result.state) {
            AuthState.INIT -> onInitSignal()
            AuthState.TOKEN_AVAILABLE -> onTokenAvailableSignal(details)
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
            is AddDeviceImportState.Init -> listOf(AuthState.TOKEN_AVAILABLE, AuthState.DONE)
            is AddDeviceImportState.TokenAvailable -> listOf(
                AuthState.TOKEN_AVAILABLE,
                AuthState.CONNECTING,
                AuthState.DONE
            )
            is AddDeviceImportState.Connecting -> listOf(AuthState.AUTHENTICATING, AuthState.DONE)
            is AddDeviceImportState.Authenticating -> listOf(AuthState.IN_PROGRESS, AuthState.DONE)
            is AddDeviceImportState.InProgress -> listOf(AuthState.IN_PROGRESS, AuthState.AUTHENTICATING, AuthState.DONE)
            is AddDeviceImportState.Done -> listOf(AuthState.DONE)
        }
    }

    // To differentiate with AuthError.
    // AuthError is bind with state `Done`.
    enum class InputError {
        BAD_PASSWORD,
        UNKNOWN;

        companion object {
            fun fromString(value: String) = when (value) {
                "bad_password" -> BAD_PASSWORD
                else -> UNKNOWN
            }
        }
    }

    companion object {
        private val TAG = ImportSideViewModel::class.simpleName!!
        const val IMPORT_TOKEN_KEY = "token"
        const val IMPORT_PEER_ID_KEY = "peer_id"
        const val IMPORT_AUTH_SCHEME_KEY = "auth_scheme"
        const val IMPORT_AUTH_ERROR_KEY = "auth_error"
        const val IMPORT_ERROR_KEY = "error"
    }
}