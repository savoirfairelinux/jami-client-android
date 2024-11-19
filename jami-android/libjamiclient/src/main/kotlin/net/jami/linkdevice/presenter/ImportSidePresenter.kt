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
package net.jami.linkdevice.presenter

import io.reactivex.rxjava3.core.Scheduler
import net.jami.linkdevice.view.ImportSideView
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

class ImportSidePresenter @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : AuthStateListener, RootPresenter<ImportSideView>() {
    private var _currentState = AuthState.INIT
    val currentState: AuthState
        get() = _currentState

    private var tempAccount: Account? = null

    init {
        accountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
            .flatMapObservable { accountDetails: HashMap<String, String> ->
                accountDetails[ConfigKey.ARCHIVE_URL.key] = "jami-auth"
                accountService.addAccount(accountDetails)
            }
            .firstOrError()
            .flatMapObservable { account ->
                tempAccount = account
                accountService.authResultObservable.filter { it.accountId == account.accountId }
            }
            .observeOn(mUiScheduler)
            .subscribe { it: AuthResult ->
                updateDeviceAuthState(it)
            }.apply { mCompositeDisposable.add(this) }
        view?.showAuthenticationUri(null)
    }

    fun onAuthentication(password: String = "") {
        tempAccount?.accountId?.apply {
            accountService.provideAccountAuthentication(this, password)
        }
    }

    fun onCancel() {
        tempAccount?.accountId?.apply {
            accountService.removeAccount(this)
        }
    }

    override fun onTokenAvailableSignal(details: Map<String, String>) {
        _currentState = AuthState.TOKEN_AVAILABLE
        val token = details[IMPORT_TOKEN_KEY] ?: throw IllegalStateException("Token not found")
        view?.showAuthenticationUri(token)
    }

    override fun onConnectingSignal() {
        _currentState = AuthState.CONNECTING
        view?.showActionRequired()
    }

    override fun onAuthenticatingSignal(details: Map<String, String>) {
        _currentState = AuthState.AUTHENTICATING
        val needPassword = details[IMPORT_AUTH_SCHEME_KEY] == "password"
        val authError = details[IMPORT_AUTH_ERROR_KEY]?.let {
            InputError.fromString(it)
        }
        val jamiId = details[IMPORT_PEER_ID_KEY] ?: throw IllegalStateException("Jami ID not found")
        tempAccount?.accountId?.apply {
            accountService.findRegistrationByAddress(this, "", jamiId)

        }
        view?.showAuthentication(needPassword, jamiId, error = authError)
    }

    override fun onInProgressSignal() {
        _currentState = AuthState.IN_PROGRESS
        view?.showInProgress()
    }

    override fun onDoneSignal(details: Map<String, String>) {
        _currentState = AuthState.DONE
        val error = details[IMPORT_ERROR_KEY]?.let {
            AuthError.fromString(it)
        }
        view?.showResult(error)
    }

    private fun updateDeviceAuthState(result: AuthResult) {
        Log.d(TAG, "Processing signal: ${result.accountId}:${result.operationId}:${result.state} ${result.details}")
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
        private val TAG = ImportSidePresenter::class.simpleName!!
        const val IMPORT_TOKEN_KEY = "token"
        const val IMPORT_PEER_ID_KEY = "peer_id"
        const val IMPORT_AUTH_SCHEME_KEY = "auth_scheme"
        const val IMPORT_AUTH_ERROR_KEY = "auth_error"
        const val IMPORT_ERROR_KEY = "error"
    }

}