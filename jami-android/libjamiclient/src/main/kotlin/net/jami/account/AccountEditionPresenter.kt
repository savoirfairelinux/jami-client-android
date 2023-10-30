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
import net.jami.model.Account
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import javax.inject.Inject
import javax.inject.Named

class AccountEditionPresenter @Inject constructor(
    private val mAccountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<AccountEditionView>() {
    private var mAccount: Account? = null
    fun init(accountId: String) {
        val account = mAccountService.getAccount(accountId)
        account?.let { init(it) }
        mCompositeDisposable.add(mAccountService
            .currentAccountSubject
            .observeOn(mUiScheduler)
            .subscribe { a: Account ->
                if (mAccount != a) {
                    init(a)
                }
            })
    }

    fun init(account: Account) {
        mAccount = account
        val view = view ?: return
        if (account.isJami) {
            view.displaySummary(account.accountId)
        } else {
            view.displaySIPView(account.accountId)
        }
        view.initViewPager(account.accountId, account.isJami)
    }
}