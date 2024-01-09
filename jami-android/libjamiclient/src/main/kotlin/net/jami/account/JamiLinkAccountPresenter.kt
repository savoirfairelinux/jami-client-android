/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
import javax.inject.Inject
import javax.inject.Named

class JamiLinkAccountPresenter @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<JamiLinkAccountView>() {
    private var mAccountCreationModel: AccountCreationModel? = null

    fun init(accountCreationModel: AccountCreationModel?) {
        mAccountCreationModel = accountCreationModel
        if (accountCreationModel == null) {
            view?.cancel()
            return
        }
        val hasArchive = accountCreationModel.archive != null
        val view = view
        if (view != null) {
            //view.showPin(!hasArchive)
            //view.enableLinkButton(hasArchive)
        }
        if (!hasArchive) {
            val newAccount = accountService.getJamiAccountTemplate("")
                .map { config ->
                    config[ConfigKey.ARCHIVE_PATH.key] = accountCreationModel.username
                    config
                }
                .flatMapObservable { config -> accountService.authenticateAccount(config) }
                .share()
            accountCreationModel.accountObservable = newAccount.map { it.second }
            mCompositeDisposable.add(newAccount
                .observeOn(uiScheduler)
                .subscribe { (state, account)  ->
                    Log.w("Link",  "state: $state, account: $account")
                    accountCreationModel.newAccount = account
                    view?.showPin(account.username!!)
                })
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
        if (isFormValid) {
            view?.createAccount()
        }
    }

    private fun showHideLinkButton() {
        view?.enableLinkButton(isFormValid)
    }

    private val isFormValid: Boolean
        get() = mAccountCreationModel?.archive != null || mAccountCreationModel!!.pin.isNotEmpty()
}