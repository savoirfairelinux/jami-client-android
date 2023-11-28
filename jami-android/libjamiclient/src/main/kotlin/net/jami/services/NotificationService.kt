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
package net.jami.services

import net.jami.model.*

interface NotificationService {
    fun showCallNotification(notifId: Int): Any?
    fun cancelCallNotification()
    fun removeCallNotification(notifId: Int)
    fun handleCallNotification(conference: Conference, remove: Boolean, startScreenshare: Boolean = false)
    fun preparePendingScreenshare(conference: Conference, callback: () -> Unit)
    fun startPendingScreenshare(confId: String)
    fun showMissedCallNotification(call: Call)
    fun showGroupCallNotification(conversation: Conversation, remove: Boolean = false)
    fun showTextNotification(conversation: Conversation)
    fun cancelTextNotification(accountId: String, contact: Uri)
    fun cancelAll()
    fun showIncomingTrustRequestNotification(account: Account)
    fun cancelTrustRequestNotification(accountID: String)
    fun showFileTransferNotification(conversation: Conversation, info: DataTransfer)
    fun handleDataTransferNotification(transfer: DataTransfer, conversation: Conversation, remove: Boolean)
    fun removeTransferNotification(accountId: String, conversationUri: Uri, fileId: String)
    fun getDataTransferNotification(notificationId: Int): Any?
    fun cancelFileNotification(notificationId: Int)

    //void updateNotification(Object notification, int notificationId);
    val serviceNotification: Any
    fun onConnectionUpdate(b: Boolean)
    fun showLocationNotification(first: Account, contact: Contact, conversation: Conversation)
    fun cancelLocationNotification(first: Account, contact: Contact)
    fun processPush()

    companion object {
        const val TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID = "trustRequestNotificationAccountId"
        const val TRUST_REQUEST_NOTIFICATION_FROM = "trustRequestNotificationFrom"
        const val KEY_CALL_ID = "callId"
        const val KEY_HOLD_ID = "holdId"
        const val KEY_END_ID = "endId"
        const val KEY_NOTIFICATION_ID = "notificationId"
        const val KEY_SCREENSHARE = "screenshare"
    }
}