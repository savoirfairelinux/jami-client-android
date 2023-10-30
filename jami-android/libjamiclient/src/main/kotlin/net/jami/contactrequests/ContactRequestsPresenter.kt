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
package net.jami.contactrequests

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class ContactRequestsPresenter @Inject internal constructor(
    private val conversationFacade: ConversationFacade,
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<ContactRequestsView>() {
    override fun bindView(view: ContactRequestsView) {
        super.bindView(view)
        mCompositeDisposable.add(conversationFacade.getPendingConversationList()
            .observeOn(uiScheduler)
            .subscribe { viewModels ->
                this.view?.updateView(ArrayList(viewModels), conversationFacade, mCompositeDisposable)
            })
    }

    fun contactRequestClicked(accountId: String, uri: Uri) {
        view?.goToConversation(accountId, uri)
    }

    fun removeConversation(item: Conversation) {
        conversationFacade.discardRequest(item.accountId, item.uri)
    }

    fun banContact(item: Conversation) {
        conversationFacade.discardRequest(item.accountId, item.uri)
        conversationFacade.banConversation(item.accountId, item.uri)
    }

    fun copyNumber(item: Conversation) {
        val contact = item.contact
        if (contact != null) {
            view?.copyNumber(contact.uri)
        } else {
            view?.copyNumber(item.uri)
        }
    }

}