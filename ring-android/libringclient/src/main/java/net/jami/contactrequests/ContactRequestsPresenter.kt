/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
package net.jami.contactrequests

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.SmartListViewModel
import net.jami.utils.Log
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class ContactRequestsPresenter @Inject internal constructor(
    private val mConversationFacade: ConversationFacade,
    private val mAccountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<ContactRequestsView>() {
    private val mAccount = BehaviorSubject.create<Account>()
    override fun bindView(view: ContactRequestsView) {
        super.bindView(view)
        mCompositeDisposable.add(mConversationFacade.getPendingList(mAccount)
            .switchMap { viewModels: List<Observable<SmartListViewModel>> ->
                if (viewModels.isEmpty()) SmartListViewModel.EMPTY_RESULTS
                else Observable.combineLatest(viewModels) { obs: Array<Any> -> obs.mapTo(ArrayList(obs.size))
                    { ob -> ob as SmartListViewModel } }
            }
            .observeOn(mUiScheduler)
            .subscribe({ viewModels -> view?.updateView(viewModels, mCompositeDisposable) })
            { e: Throwable -> Log.d(TAG, "updateList subscribe onError", e) })
    }

    override fun onDestroy() {
        mAccount.onComplete()
        super.onDestroy()
    }

    fun updateAccount(accountId: String?) {
        mAccountService.getAccount(accountId).let { account ->
            if (account == null) {
                mAccountService.currentAccountSubject.subscribe(mAccount)
            } else {
                mAccount.onNext(account)
            }
        }
    }

    fun contactRequestClicked(accountId: String, uri: Uri) {
        view?.goToConversation(accountId, uri)
    }

    companion object {
        private val TAG = ContactRequestsPresenter::class.simpleName!!
    }
}