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
package cx.ring.tv.search

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.Conversation
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import javax.inject.Inject
import javax.inject.Named

class ContactSearchPresenter @Inject constructor(
    private val accountService: AccountService,
    private val conversationFacade: ConversationFacade,
    @Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<ContactSearchView>() {
    private val contactQuery = PublishSubject.create<String>()

    override fun bindView(view: ContactSearchView) {
        super.bindView(view)
        mCompositeDisposable.add(conversationFacade.getFullConversationList(accountService.currentAccountSubject, contactQuery)
            .observeOn(uiScheduler)
            .subscribe { results -> this.view?.displayResults(results, conversationFacade) })
    }

    fun queryTextChanged(query: String) {
        if (query.isEmpty()) {
            view?.clearSearch()
        } else {
            contactQuery.onNext(query)
        }
    }

    fun contactClicked(model: Conversation) {
        view?.displayContactDetails(model)
    }
}