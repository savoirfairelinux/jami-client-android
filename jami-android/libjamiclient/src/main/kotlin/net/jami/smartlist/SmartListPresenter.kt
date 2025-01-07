/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package net.jami.smartlist

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class SmartListPresenter @Inject constructor(
    private val conversationFacade: ConversationFacade,
    private val mAccountService: AccountService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<SmartListView>() {

    override fun bindView(view: SmartListView) {
        super.bindView(view)
        view.setLoading(true)
        mCompositeDisposable.add(conversationFacade.getConversationList(conversationFacade.currentAccountSubject)
            .throttleLatest(150, TimeUnit.MILLISECONDS, uiScheduler)
            .observeOn(uiScheduler)
            .subscribe { list ->
                val v = this.view ?: return@subscribe
                v.setLoading(false)
                if (list.isEmpty()) {
                    v.hideList()
                    v.displayNoConversationMessage()
                } else {
                    v.hideNoConversationMessage()
                    v.updateList(list, conversationFacade, mCompositeDisposable)
                }
            })
    }

    fun copyNumber(conversation: Conversation) {
        val contact = conversation.contact
        if (contact != null) {
            view?.copyNumber(contact.uri)
        } else {
            view?.copyNumber(conversation.uri)
        }
    }

    fun clearConversation(conversation: Conversation) {
        view?.displayClearDialog(conversation.accountId, conversation.uri)
    }

    fun clearConversation(accountId: String, uri: Uri) {
        mCompositeDisposable.add(conversationFacade
            .clearHistory(accountId, uri)
            .subscribeOn(Schedulers.computation()).subscribe())
    }

    fun blockContact(conversation: Conversation) =
        view?.displayBlockDialog(conversation.accountId, conversation.contact!!)

    fun blockContact(accountId: String, contact: Contact) =
        mAccountService.removeContact(accountId, contact.uri.uri, true)

    fun removeConversation(conversation: Conversation) =
        view?.displayDeleteDialog(
            conversation.accountId,
            conversation.uri,
            conversation.isSwarmGroup()
        )

    fun removeConversation(accountId: String, uri: Uri) {
        mCompositeDisposable.add(conversationFacade.removeConversation(accountId, uri)
            .subscribe())
    }
}