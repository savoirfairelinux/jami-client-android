/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.jami.account

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Named

class LinkDevicePresenter @Inject constructor(
    private val mAccountService: AccountService,
    @Named("UiScheduler")
    private var mUiScheduler: Scheduler
) : RootPresenter<LinkDeviceView>() {
    private var mAccountID: String? = null

    fun startAccountExport(password: String?) {
        if (view == null) {
            return
        }
        view?.showExportingProgress()
        mCompositeDisposable.add(mAccountService
            .exportOnRing(mAccountID!!, password!!)
            .observeOn(mUiScheduler)
            .subscribe({ pin: String -> view?.showPIN(pin) })
            { error: Throwable ->
                view?.dismissExportingProgress()
                when (error) {
                    is IllegalArgumentException -> view?.showPasswordError()
                    is SocketException -> view?.showNetworkError()
                    else -> view?.showGenericError()
                }
            })
    }

    fun setAccountId(accountID: String) {
        mCompositeDisposable.clear()
        mAccountID = accountID
        val account = mAccountService.getAccount(accountID)
        if (account != null)
            view?.accountChanged(account)
        mCompositeDisposable.add(mAccountService.getObservableAccountUpdates(accountID)
            .observeOn(mUiScheduler)
            .subscribe { a: Account -> view?.accountChanged(a) })
    }

    companion object {
        private val TAG = LinkDevicePresenter::class.simpleName!!
    }
}