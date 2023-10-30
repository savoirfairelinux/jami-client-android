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
package net.jami.conversation

import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.smartlist.ConversationItemViewModel
import java.io.File

interface ConversationView {
    fun refreshView(conversation: List<Interaction>)
    fun scrollToEnd()
    fun scrollToMessage(messageId: String, highlight: Boolean = true)
    fun updateContact(contact: ContactViewModel)
    fun displayContact(conversation: ConversationItemViewModel)
    fun displayOnGoingCallPane(display: Boolean)
    fun displayNumberSpinner(conversation: Conversation, number: Uri)
    fun displayErrorToast(error: Error)
    fun hideNumberSpinner()
    fun clearMsgEdit()
    fun goToHome()
    fun goToAddContact(contact: Contact)
    fun goToCallActivity(conferenceId: String, withCamera: Boolean)
    fun goToCallActivityWithResult(accountId: String, conversationUri: Uri, contactUri: Uri, withCamera: Boolean)
    fun goToContactActivity(accountId: String, uri: Uri)
    fun switchToUnknownView(name: String)
    fun switchToIncomingTrustRequestView(name: String)
    fun switchToConversationView()
    fun switchToBannedView()

    fun switchToSyncingView()
    fun switchToEndedView()
    fun openFilePicker()
    fun acceptFile(accountId: String, conversationUri: Uri, transfer: DataTransfer)
    fun goToGroupCall(conversation: Conversation, contactUri: net.jami.model.Uri, hasVideo: Boolean)
    fun refuseFile(accountId: String, conversationUri: Uri, transfer: DataTransfer)
    fun shareFile(path: File, displayName: String)
    fun openFile(path: File, displayName: String)
    fun addElement(element: Interaction)
    fun updateElement(element: Interaction)
    fun removeElement(element: Interaction)
    fun setComposingStatus(composingStatus: ComposingStatus)
    fun setConversationColor(color: Int)
    fun setConversationSymbol(symbol: CharSequence)
    fun startSaveFile(file: DataTransfer, fileAbsolutePath: String)
    fun startReplyTo(interaction: Interaction)
    fun startShareLocation(accountId: String, conversationId: String)
    fun showMap(accountId: String, contactId: String, open: Boolean)
    fun hideMap()
    fun showPluginListHandlers(accountId: String, contactId: String)
    fun hideErrorPanel()
    fun displayNetworkErrorPanel()
    fun displayAccountOfflineErrorPanel()
    fun setSettings(linkPreviews: Boolean)
    fun addSearchResults(results: List<Interaction>)
    fun shareText(body: String)
    fun goToSearchMessage(messageId: String)
}