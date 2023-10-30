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
package net.jami.smartlist

import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.ConversationFacade

interface SmartListView {
    fun displayChooseNumberDialog(numbers: Array<CharSequence>)
    fun displayNoConversationMessage()
    fun displayConversationDialog(conversationItemViewModel: Conversation)
    fun displayClearDialog(accountId: String, conversationUri: Uri)
    fun displayDeleteDialog(accountId: String, conversationUri: Uri)
    fun copyNumber(uri: Uri)
    fun setLoading(loading: Boolean)
    fun hideList()
    fun hideNoConversationMessage()
    fun updateList(conversations: ConversationFacade.ConversationList, conversationFacade: ConversationFacade, parentDisposable: CompositeDisposable)
    fun update(model: Conversation)
    fun update(position: Int)
    fun goToConversation(accountId: String, conversationUri: Uri)
    fun goToCallActivity(accountId: String, conversationUri: Uri, contactId: String)
    fun scrollToTop()
}