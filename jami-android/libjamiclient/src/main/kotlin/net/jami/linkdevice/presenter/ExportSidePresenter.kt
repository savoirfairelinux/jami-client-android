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
import net.jami.linkdevice.view.ExportSideResult
import net.jami.linkdevice.view.ExportSideView
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

class ExportSidePresenter @Inject constructor(
    private val accountService: AccountService,
    @Suppress("unused") @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : AuthStateListener, RootPresenter<ExportSideView>() {
    private var _currentState = AuthState.INIT
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
            view?.showInput(ExportSideInputError.INVALID_INPUT)
            return
        }

        val operationId = accountService
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
        accountService.confirmAddDevice(accountService.currentAccount!!.accountId)
    }

    fun onCancel() {
        accountService.cancelAddDevice(accountService.currentAccount!!.accountId)
    }

    // There is no token to be received in export side.
    override fun onTokenAvailableSignal(details: Map<String, String>) {
        throw UnsupportedOperationException()
    }

    override fun onConnectingSignal() {
        _currentState = AuthState.CONNECTING
        // Todo: What to do here?
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
        view?.showResult(result = ExportSideResult.IN_PROGRESS)
    }

    override fun onDoneSignal(details: Map<String, String>) {
        _currentState = AuthState.DONE
        val error = details[EXPORT_ERROR_KEY]?.let {
            AuthError.fromString(it)
        }
        val result = if (error == null) ExportSideResult.SUCCESS else ExportSideResult.FAILURE
        view?.showResult(result = result, error = error)
    }

    private fun updateAuthState(authenticationResult: AuthResult) {
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

    companion object {
        @Suppress("unused") private val TAG = ExportSidePresenter::class.simpleName!!

        const val EXPORT_PEER_ADDRESS_KEY = "peer_address"
        const val EXPORT_ERROR_KEY = "error"
    }
}