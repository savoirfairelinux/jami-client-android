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
package net.jami.account

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.AccountConfig
import net.jami.model.AccountCreationModel
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.utils.Log
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named

/**
 * This is the new device side.
 */
class LinkAccountImportPresenter @Inject constructor(
    private val accountService: AccountService,
    @Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<LinkAccountImportView>() {
    private var mAccountCreationModel: AccountCreationModel? = null

    fun init(accountCreationModel: AccountCreationModel?) {
        mAccountCreationModel = accountCreationModel
        if (mAccountCreationModel == null) {
            view?.cancel()
            return
        }
        val hasArchive = mAccountCreationModel?.archive != null
        val view = view
        if (view != null) {
//            view.showPin(!hasArchive)
            view.enableLinkButton(hasArchive)
        }
    }

    fun passwordChanged(password: String) {
        mAccountCreationModel?.password = password
        showHideLinkButton()
    }

    fun pinChanged(pin: String) {
        mAccountCreationModel?.pin = pin
        showHideLinkButton()
    }

    fun resetPin() {
        mAccountCreationModel?.pin = ""
        showHideLinkButton()
    }

    fun linkClicked() {
        Log.w("devdebug", "JamiLinkAccountPresenter linkClicked")


        accountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
            .flatMapObservable { accountDetails: HashMap<String, String> ->
                Log.w("devdebug", "JamiLinkAccountPresenter raw details $accountDetails")
                accountDetails[ConfigKey.ARCHIVE_URL.key] = "jami-auth"
                accountService.addAccount(accountDetails)
            }
            .firstOrError()
            .flatMapObservable { account ->
                Log.w("devdebug", "JamiLinkAccountPresenter linkClicked switchMapSingle ${account.accountId}")
                accountService.deviceAuthStateObservable.filter { it.accountId == account.accountId }
            }
            .observeOn(uiScheduler)
            .subscribe{ deviceAuthResult: AccountService.DeviceAuthResult ->
                Log.w("devdebug", "JamiLinkAccountPresenter linkClicked subscribe state=${deviceAuthResult.state} details=${deviceAuthResult.details}")
                updateDeviceAuthState(deviceAuthResult)
            }.apply { mCompositeDisposable.add(this) }

//        if (isFormValid) {
//            view?.createAccount()
//        }
    }

    private fun updateDeviceAuthState(deviceAuthResult: AccountService.DeviceAuthResult) {
        // TOdo: implement timeout
        Log.w("devdebug", "JamiLinkAccountPresenter updateDeviceAuthState state=${deviceAuthResult.state} details=${deviceAuthResult.details}")
        when(deviceAuthResult.state) {
            AccountService.DeviceAuthState.NONE -> { // Todo: verify but I think there is nothing to do
                view?.showLoadingToken()
                }
            AccountService.DeviceAuthState.TOKEN_AVAILABLE -> {
                view?.showTokenAvailable(deviceAuthResult.details)
            }
            AccountService.DeviceAuthState.CONNECTING -> {
                view?.showConnecting()
            }
            AccountService.DeviceAuthState.AUTHENTICATING -> {
                view?.showAuthenticating()
            }
            AccountService.DeviceAuthState.DONE -> {
                view?.showDone()
            }
            AccountService.DeviceAuthState.ERROR -> {
                view?.showError(AccountService.DeviceAuthStateError.fromString(deviceAuthResult.details))
            }
        }
    }

    private fun showHideLinkButton() {
        view?.enableLinkButton(isFormValid)
    }

    private val isFormValid: Boolean
        get() = mAccountCreationModel?.archive != null || mAccountCreationModel!!.pin.isNotEmpty()
}