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
import net.jami.linkdevice.view.ExportSideInputError
import net.jami.linkdevice.view.ExportSideView
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

class ExportSidePresenter @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : AuthStateListener, RootPresenter<ExportSideView>() {
    private var _currentState = AuthState.INIT
    private var operationId: Long? = null
    val currentState: AuthState
        get() = _currentState

    init {
        view?.showInput()
    }

    fun onAuthenticationUri(jamiAuthentication: String) {
        // Verify the input.
        if (jamiAuthentication.isEmpty()
            || !jamiAuthentication.startsWith("jami-auth://")
            || (jamiAuthentication.length != 59)
        ) {
            Log.w(TAG, "Invalid input: $jamiAuthentication")
            view?.showInput(ExportSideInputError.INVALID_INPUT)
            return
        }

        operationId = accountService
            .addDevice(accountService.currentAccount!!.accountId, jamiAuthentication)

        assert(mCompositeDisposable.size() == 0) // Should not have any subscription.

        accountService.authResultObservable
            .filter {
                it.accountId == accountService.currentAccount?.accountId
                        && it.operationId == operationId
            }
            .observeOn(mUiScheduler)
            .subscribe { it: AuthResult ->
                updateAuthState(it)
            }.apply { mCompositeDisposable.add(this) }
    }

    fun onIdentityConfirmation() {
        accountService.confirmAddDevice(accountService.currentAccount!!.accountId, operationId!!)
    }

    fun onCancel() {
        accountService.cancelAddDevice(accountService.currentAccount!!.accountId, operationId!!)
    }

    // There is no token to be received in export side.
    override fun onTokenAvailableSignal(details: Map<String, String>) {
        throw UnsupportedOperationException()
    }

    override fun onConnectingSignal() {
        // Nothing to do here.
        _currentState = AuthState.CONNECTING
    }

    override fun onAuthenticatingSignal(details: Map<String, String>) {
        _currentState = AuthState.AUTHENTICATING
        val peerAddress = details[EXPORT_PEER_ADDRESS_KEY]
        if (!peerAddress.isNullOrEmpty()) {
            view?.showIP(ip = peerAddress)
            return
        }
        view?.showPasswordProtection()
    }

    override fun onInProgressSignal() {
        _currentState = AuthState.IN_PROGRESS
        view?.showInProgress()
    }

    override fun onDoneSignal(details: Map<String, String>) {
        _currentState = AuthState.DONE
        val error = details[EXPORT_ERROR_KEY]
            ?.let { if (it.isEmpty() || it == "none") null else AuthError.fromString(it) }
        view?.showResult(error = error)
    }

    private fun updateAuthState(authenticationResult: AuthResult) {
        Log.d(TAG, "Processing signal: ${authenticationResult.accountId}:${authenticationResult.operationId}:${authenticationResult.state} ${authenticationResult.details}")
        if (!checkNewStateValidity(authenticationResult.state)) {
            Log.e(TAG, "Invalid state transition: $_currentState->${authenticationResult.state}")
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
        return newState in when (_currentState) {
            AuthState.INIT -> listOf(AuthState.CONNECTING, AuthState.DONE)
            AuthState.TOKEN_AVAILABLE -> emptyList()
            AuthState.CONNECTING -> listOf(AuthState.AUTHENTICATING, AuthState.DONE)
            AuthState.AUTHENTICATING -> listOf(AuthState.IN_PROGRESS, AuthState.DONE)
            AuthState.IN_PROGRESS -> listOf(AuthState.DONE, AuthState.DONE)
            AuthState.DONE -> listOf(AuthState.DONE)
        }
    }

    companion object {
        private val TAG = ExportSidePresenter::class.simpleName!!
        const val EXPORT_PEER_ADDRESS_KEY = "peer_address"
        const val EXPORT_ERROR_KEY = "error"
    }
}