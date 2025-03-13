/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package net.jami.account

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.AccountService.AuthError
import net.jami.services.AccountService.AuthResult
import net.jami.services.AccountService.AuthState
import net.jami.services.DeviceRuntimeService
import net.jami.services.PreferencesService
import net.jami.utils.Log
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class AccountImportPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mPreferences: PreferencesService,
    private val mDeviceService: DeviceRuntimeService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<AccountImportView>() {
    private var mCreatingAccount = false
    private var newAccount: Observable<Account>? = null
    private var _tempAccount: Account? = null
    private val tempAccount: Account?
        get() = _tempAccount
    private var currentState: AuthState = AuthState.INIT

    fun init() {
        mAccountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
            .flatMapObservable { accountDetails: HashMap<String, String> ->
                accountDetails[ConfigKey.ARCHIVE_URL.key] = "jami-auth"
                mAccountService.addAccount(accountDetails)
            }
            .firstOrError()
            .flatMapObservable { account ->
                mAccountService.authResultObservable.filter { it.accountId == account.accountId }
                    .doOnNext { _tempAccount = account }
            }
            .observeOn(mUiScheduler)
            .subscribe { updateDeviceAuthState(it) }
            .apply { mCompositeDisposable.add(this) }
    }

    fun onAuthentication(password: String = "") {
        Log.w(TAG, "onAuthentication: $password")
        _tempAccount?.accountId?.apply {
            mAccountService.provideAccountAuthentication(this, password)
        }
    }

    fun onCancel() {
        _tempAccount?.accountId?.apply {
            mAccountService.removeAccount(this)
        }
        view?.showExit()
    }

    fun onTokenAvailableSignal(details: Map<String, String>) {
        val token = details[IMPORT_TOKEN_KEY] ?: throw IllegalStateException("Token not found")
        view?.showToken(token)
    }

    fun onConnectingSignal() {
        view?.showConnecting()
    }

    fun onAuthenticatingSignal(details: Map<String, String>) {
        val needPassword = details[IMPORT_AUTH_SCHEME_KEY] == "password"
        val authError = details[IMPORT_AUTH_ERROR_KEY]
        val jamiId = details[IMPORT_PEER_ID_KEY] ?: throw IllegalStateException("Jami ID not found")
        _tempAccount?.accountId?.apply {
            mAccountService.findRegistrationByAddress(this, "", jamiId)
                .observeOn(mUiScheduler)
                .subscribe { registeredName ->
                    Log.d(TAG, "Registered name: ${registeredName.name}")
                    view?.showAuthenticating(needPassword, jamiId, registeredName.name, authError)
                }.apply { mCompositeDisposable.add(this) }
        }
        view?.showAuthenticating(needPassword, jamiId,"", authError)
    }

    fun onInProgressSignal() {
        view?.showInProgress()
    }

    fun onDoneSignal(details: Map<String, String>) {
        //val error = details[IMPORT_ERROR_KEY]
        val error = details[IMPORT_ERROR_KEY]
            ?.let { if (it.isEmpty() || it == "none") null else AuthError.fromString(it) }

        view?.showDone(error)
    }

    private fun updateDeviceAuthState(result: AuthResult) {
        Log.d(TAG, "Processing signal: ${result.accountId}:${result.operationId}:${result.state} ${result.details}")
        if (!checkNewStateValidity(result.state)) {
            Log.e(TAG, "Invalid state transition: $currentState -> ${result.state}")
            throw IllegalStateException("Invalid state transition: $currentState -> ${result.state}")
        }
        val details = result.details
        currentState = result.state
        when (result.state) {
            AuthState.INIT -> {}//onInitSignal()
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
        val validTransitions = when (currentState) {
            AuthState.INIT -> listOf(AuthState.TOKEN_AVAILABLE, AuthState.DONE)
            AuthState.TOKEN_AVAILABLE -> listOf(AuthState.CONNECTING, AuthState.DONE)
            AuthState.CONNECTING -> listOf(AuthState.AUTHENTICATING, AuthState.DONE)
            AuthState.AUTHENTICATING -> listOf(AuthState.IN_PROGRESS, AuthState.DONE)
            AuthState.IN_PROGRESS -> listOf(AuthState.AUTHENTICATING, AuthState.IN_PROGRESS, AuthState.DONE)
            AuthState.DONE -> listOf(AuthState.DONE) // Once done, only done is valid
        }
        return newState in validTransitions
    }

    fun onCleared() {
        mCompositeDisposable.dispose()
        if (view !is AccountImportView) onCancel()
    }

    fun getTempAccountId(): String? {
        return _tempAccount?.accountId
    }

    companion object {
        val TAG = AccountImportPresenter::class.simpleName!!
        const val IMPORT_TOKEN_KEY = "token"
        const val IMPORT_PEER_ID_KEY = "peer_id"
        const val IMPORT_AUTH_SCHEME_KEY = "auth_scheme"
        const val IMPORT_AUTH_ERROR_KEY = "auth_error"
        const val IMPORT_ERROR_KEY = "error"
    }
}
