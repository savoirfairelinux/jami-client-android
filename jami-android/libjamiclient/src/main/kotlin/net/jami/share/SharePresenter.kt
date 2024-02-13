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
package net.jami.share

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.mvp.GenericView
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import javax.inject.Inject
import javax.inject.Named

class SharePresenter @Inject constructor(
    private val mAccountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<GenericView<ShareViewModel>>() {
    override fun bindView(view: GenericView<ShareViewModel>) {
        super.bindView(view)
        mCompositeDisposable.add(mAccountService
            .currentAccountSubject
            .map { account: Account -> ShareViewModel(account) }
            .subscribeOn(Schedulers.computation())
            .observeOn(mUiScheduler)
            .subscribe { model: ShareViewModel -> loadContactInformation(model) })
    }

    private fun loadContactInformation(model: ShareViewModel) {
        view?.showViewModel(model)
    }
}