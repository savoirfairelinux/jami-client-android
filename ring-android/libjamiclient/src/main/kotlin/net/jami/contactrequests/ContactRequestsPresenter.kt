/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
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
        mCompositeDisposable.add(mConversationFacade.getPendingConversationList(mAccount)
            .observeOn(mUiScheduler)
            .subscribe { viewModels ->
                this.view?.updateView(viewModels, mConversationFacade, mCompositeDisposable)
            })
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

    fun removeConversation(item: Conversation) {
        mConversationFacade.discardRequest(item.accountId, item.uri)
    }

    fun banContact(item: Conversation) {
        mConversationFacade.discardRequest(item.accountId, item.uri)
        mAccountService.removeContact(item.accountId, item.uri.host, true)
    }

    fun copyNumber(item: Conversation) {
        val contact = item.contact
        if (contact != null) {
            view?.copyNumber(contact.uri)
        } else {
            view?.copyNumber(item.uri)
        }
    }

    companion object {
        private val TAG = ContactRequestsPresenter::class.simpleName!!
    }
}