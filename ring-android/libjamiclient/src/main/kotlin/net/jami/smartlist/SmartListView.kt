/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.smartlist

import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Uri

interface SmartListView {
    fun displayChooseNumberDialog(numbers: Array<CharSequence>)
    fun displayNoConversationMessage()
    fun displayConversationDialog(conversationItemViewModel: ConversationItemViewModel)
    fun displayClearDialog(conversationUri: Uri)
    fun displayDeleteDialog(conversationUri: Uri)
    fun copyNumber(uri: Uri)
    fun setLoading(loading: Boolean)
    fun displayMenuItem()
    fun hideList()
    fun hideNoConversationMessage()
    fun updateList(conversationItemViewModels: MutableList<ConversationItemViewModel>?, parentDisposable: CompositeDisposable)
    fun update(model: ConversationItemViewModel)
    fun update(position: Int)
    fun goToConversation(accountId: String, conversationUri: Uri)
    fun goToCallActivity(accountId: String, conversationUri: Uri, contactId: String)
    fun goToQRFragment()
    fun scrollToTop()
}