/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.utils

import android.content.*
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.views.AvatarView
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

    data class Padding(
        val pixelsLeft: Int, val pixelsTop: Int,
        val pixelsRight: Int, val pixelsBottom: Int
    )

    fun View.setPadding(padding: Padding) =
        setPadding(padding.pixelsLeft, padding.pixelsTop, padding.pixelsRight, padding.pixelsBottom)

    fun openJamiDonateWebPage(context: Context) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(context.getString(R.string.donation_url)))
        )
    }

    /**
     * Share the given username with the system share intent.
     * @param context the context
     * @param username the username to share
     */
    fun shareAccount(context: Context, username: String?) {
        if (!username.isNullOrEmpty()) {
            val sharingIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.account_contact_me))
                    putExtra(
                        Intent.EXTRA_TEXT,
                        context.getString(
                            R.string.account_share_body,
                            username,
                            context.getString(R.string.app_website)
                        )
                    )
                }
            context.startActivity(
                Intent.createChooser(
                    sharingIntent, context.getString(R.string.share_via)
                )
            )
        }
    }

    /**
     * Copy the given string to the clipboard and show a snackbar to inform the user.
     * @param context the context
     * @param view the view to attach the snackbar to
     * @param toCopy the string to copy
     */
    fun copyAndShow(context: Context, view: View, toCopy: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(context.getString(R.string.clip_contact_uri), toCopy)
        )
        // Only show a toast for Android 12 and lower (avoid duplicate notification).
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Snackbar.make(
                view,
                context
                    .getString(R.string.conversation_action_copied_peer_number_clipboard, toCopy),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    fun launchClearAction(context: Context, accountId: String, uri: Uri, callback: ConversationActionCallback) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.conversation_action_history_clear_title)
            .setMessage(R.string.conversation_action_history_clear_message)
            .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, whichButton: Int ->
                callback.clearConversation(accountId, uri)
            }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, whichButton: Int -> }
            .show()
    }

    fun launchDeleteSwarmOneToOneAction(
        context: Context, accountId: String, uri: Uri, callback: ConversationActionCallback,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.conversation_action_remove_this_title)
            .setMessage(R.string.conversation_action_remove_this_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                callback.removeConversation(accountId, uri)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    fun launchDeleteSwarmGroupAction(
        context: Context, accountId: String, uri: Uri, callback: ConversationActionCallback,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.swarm_group_action_leave_title)
            .setMessage(R.string.swarm_group_action_leave_message)
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

    /**
     * ImageView fade-out animation for the avatar. 
     * @param imageView the image view used for the animation.
     */
    fun startFadeOutAnimation(imageView: AvatarView) {
        val fadeOutAnimation = AnimationUtils.loadAnimation(
            imageView.context,
            R.anim.fade_out
        ).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(arg0: Animation) {}
                override fun onAnimationRepeat(arg0: Animation) {}
                override fun onAnimationEnd(arg0: Animation) {
                    imageView.setAvatar(null)
                    imageView.visibility = View.INVISIBLE
                }
            })
        }
        imageView.startAnimation(fadeOutAnimation)
    }
}