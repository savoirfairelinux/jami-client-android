/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.utils

import android.content.*
import android.provider.ContactsContract
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import net.jami.model.Contact
import net.jami.model.Conversation.ConversationActionCallback
import net.jami.model.Uri
import java.util.*

object ActionHelper {
    val TAG = ActionHelper::class.simpleName!!
    const val ACTION_COPY = 0
    const val ACTION_CLEAR = 1
    const val ACTION_DELETE = 2
    const val ACTION_BLOCK = 3

    fun launchClearAction(context: Context, accountId: String, uri: Uri, callback: ConversationActionCallback) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.conversation_action_history_clear_title)
            .setMessage(R.string.conversation_action_history_clear_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                callback.clearConversation(accountId, uri)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    fun launchDeleteAction(context: Context, accountId: String, uri: Uri, callback: ConversationActionCallback) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.conversation_action_remove_this_title)
            .setMessage(R.string.conversation_action_remove_this_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                callback.removeConversation(accountId, uri)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    fun getAddNumberIntentForContact(contact: Contact): Intent {
        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT)
        intent.type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
        val data = ArrayList<ContentValues>()
        val values = ContentValues()
        val number = contact.uri
        if (number.isHexId) {
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
            values.put(ContactsContract.CommonDataKinds.Im.DATA, number.rawUriString)
            values.put(
                ContactsContract.CommonDataKinds.Im.PROTOCOL,
                ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM
            )
            values.put(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, "Ring")
        } else {
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE)
            values.put(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS, number.rawUriString)
        }
        data.add(values)
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data)
        return intent
    }

    fun displayContact(context: Context, contact: Contact) {
        if (contact.id != Contact.UNKNOWN_ID) {
            Log.d(TAG, "displayContact: contact is known, displaying...")
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = android.net.Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id.toString())
                intent.data = uri
                context.startActivity(intent)
            } catch (exc: ActivityNotFoundException) {
                Log.e(TAG, "Error displaying contact", exc)
            }
        }
    }

}