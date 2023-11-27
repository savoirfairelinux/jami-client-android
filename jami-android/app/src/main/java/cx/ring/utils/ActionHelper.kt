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
package cx.ring.utils

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.net.toUri
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.adapters.MessageType
import cx.ring.views.AvatarView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Conversation.ConversationActionCallback
import net.jami.model.Uri
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

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
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, context.getString(R.string.donation_url).toUri())
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_open_no_app_found, context.getString(R.string.donation_url)),
                Toast.LENGTH_LONG).show()
        }
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

    fun launchClearAction(context: Context, accountId: String, uri: Uri, callback: ConversationActionCallback) {
        val title = if (uri.isSwarm) {
            R.string.conversation_action_remove_title
        } else {
            R.string.conversation_action_history_clear_title
        }
        val message = if (uri.isSwarm) {
            R.string.conversation_action_remove_message
        } else {
            R.string.conversation_action_history_clear_message
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
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

    fun launchAddContactAction(
        context: Context,
        accountId: String,
        contact: Contact,
        callback: (accountId: String, uri: Uri) -> Unit,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.ab_action_contact_add_question)
            .setMessage(R.string.conversation_action_add_this_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> callback(accountId, contact.uri) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
    }

    fun launchClearAction(
        context: Context,
        accountId: String,
        uri: Uri,
        callback: (accountId: String, uri: Uri) -> Unit,
    ) {
        val title = if (uri.isSwarm) {
            R.string.conversation_action_remove_title
        } else {
            R.string.conversation_action_history_clear_title
        }
        val message = if (uri.isSwarm) {
            R.string.conversation_action_remove_message
        } else {
            R.string.conversation_action_history_clear_message
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> callback(accountId, uri) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
    }

    fun launchDeleteSwarmOneToOneAction(
        context: Context,
        accountId: String,
        uri: Uri,
        callback: (accountId: String, uri: Uri) -> Unit,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.conversation_action_remove_this_title)
            .setMessage(R.string.conversation_action_remove_this_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> callback(accountId, uri) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
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

    fun launchDeleteSwarmGroupAction(
        context: Context,
        accountId: String,
        uri: Uri,
        callback: (accountId: String, uri: Uri) -> Unit,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.swarm_group_action_leave_title)
            .setMessage(R.string.swarm_group_action_leave_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> callback(accountId, uri) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
    }

    fun launchBlockContactAction(
        context: Context,
        accountId: String,
        contact: Contact,
        callback: (accountId: String, uri: Uri) -> Unit,
    ): Disposable =
        (contact.username ?: Single.just(""))
            .map { name -> name.takeIf { it.isNotEmpty() } ?: contact.uri.uri }
            .onErrorReturn { contact.uri.uri }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { displayName ->
                MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.block_contact_dialog_title, displayName))
                    .setMessage(context.getString(R.string.block_contact_dialog_message, displayName))
                    .setPositiveButton(android.R.string.ok) { _, _ -> callback(accountId, contact.uri) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
            }

    fun launchAcceptInvitation(
        context: Context,
        conversation: Conversation,
        callback: (conversation: Conversation) -> Unit,
    ): Disposable {
        val contact = conversation.contact
            ?: return Disposable.empty()
        return (contact.username ?: Single.just(""))
            .map { name -> name.takeIf { it.isNotEmpty() } ?: contact.uri.uri }
            .onErrorReturn { contact.uri.uri }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { displayName ->
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.accept_invitation)
                    .setMessage(context.getString(R.string.accept_invitation_body, displayName))
                    .setPositiveButton(android.R.string.ok) { _, _ -> callback(conversation) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
            }
    }

    fun launchUnblockContactAction(
        context: Context,
        accountId: String,
        contact: Contact,
        callback: (accountId: String, uri: Uri) -> Unit,
    ): Disposable =
        (contact.username ?: Single.just(""))
            .map { name -> name.takeIf { it.isNotEmpty() } ?: contact.uri.uri }
            .onErrorReturn { contact.uri.uri }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { displayName ->
                MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.unblock_contact_dialog_title, displayName))
                    .setMessage(context.getString(R.string.unblock_contact_dialog_message, displayName))
                    .setPositiveButton(android.R.string.ok) { _, _ -> callback(accountId, contact.uri) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
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

    fun shareAuthenticationToken(context: Context, authenticationToken: String) {
        if (authenticationToken.isEmpty()) {
            Log.e("ShareUtils", "authenticationToken is null or empty")
            return
        }

        val sharingIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.link_device_share_title))
                putExtra(Intent.EXTRA_TEXT, authenticationToken)
            }
        context.startActivity(
            Intent.createChooser(
                sharingIntent, context.getString(R.string.share_via)
            )
        )
    }

    class MessageSwipeController(
        private val context: Context,
        private val onSwipe: (Int) -> Unit,
    ) : ItemTouchHelper.Callback() {

        private lateinit var imageDrawable: Drawable
        private var currentItemViewHolder: RecyclerView.ViewHolder? = null
        private lateinit var mView: View
        private var dX = 0f
        private var replyButtonProgress = 0f
        private var lastReplyButtonAnimationTime: Long = 0
        private var swipeBack = false
        private var isVibrate = false
        private var startTracking = false
        private val swipeLengthLimit = 130.dp
        private val swipeTrigger = 80.dp

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ): Int {
            mView = viewHolder.itemView
            imageDrawable = context.getDrawable(R.drawable.baseline_reply_24) ?: return 0

            val messageType = MessageType.values()[viewHolder.itemViewType]

            val incomingMessageType = listOf(
                MessageType.INCOMING_TEXT_MESSAGE,
                MessageType.INCOMING_AUDIO,
                MessageType.INCOMING_FILE,
                MessageType.INCOMING_IMAGE,
                MessageType.INCOMING_VIDEO
            )
            val outgoingMessageType = listOf(
                MessageType.OUTGOING_TEXT_MESSAGE,
                MessageType.OUTGOING_AUDIO,
                MessageType.OUTGOING_FILE,
                MessageType.OUTGOING_IMAGE,
                MessageType.OUTGOING_VIDEO
            )

            // Only allows swipe to incoming and outgoing messages.
            if (messageType in incomingMessageType)
                return makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.RIGHT)
            else if (messageType in outgoingMessageType)
                return makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.LEFT)
            return 0
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
            if (swipeBack) {
                swipeBack = false
                return 0
            }
            return super.convertToAbsoluteDirection(flags, layoutDirection)
        }

        override fun onChildDraw(
            canvas: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE)
                setTouchListener(recyclerView, viewHolder)

            // Allows to limit the swipe length.
            if (abs(mView.translationX) < swipeLengthLimit || abs(dX) < this.dX) {
                super.onChildDraw(
                    canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                )
                this.dX = abs(dX)
                startTracking = true
            }
            currentItemViewHolder = viewHolder
            drawReplyButton(canvas)
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setTouchListener(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ) {
            recyclerView.setOnTouchListener { _, event ->
                swipeBack = event.action == MotionEvent.ACTION_CANCEL
                        || event.action == MotionEvent.ACTION_UP
                if (swipeBack) {
                    // Define the swipe distance to trigger reply.
                    if (abs(mView.translationX) >= swipeTrigger) {
                        onSwipe(viewHolder.bindingAdapterPosition)
                    }
                }
                false
            }
        }

        private fun drawReplyButton(canvas: Canvas) {
            this.currentItemViewHolder ?: return

            val absoluteTranslationX = abs(mView.translationX)
            val newTime = System.currentTimeMillis()
            val dt = 17L.coerceAtMost(newTime - lastReplyButtonAnimationTime)
            lastReplyButtonAnimationTime = newTime

            // Animate reply drawable coming on/off.
            val showing = absoluteTranslationX >= 30.dp
            if (showing) {
                if (replyButtonProgress < 1.0f) {
                    replyButtonProgress += dt / 180.0f
                    if (replyButtonProgress > 1.0f) replyButtonProgress = 1.0f
                    else mView.invalidate()
                }
            } else if (absoluteTranslationX == 0.0f) { // Reset.
                replyButtonProgress = 0f
                startTracking = false
                isVibrate = false
            } else {
                if (replyButtonProgress > 0.0f) {
                    replyButtonProgress -= dt / 180.0f
                    if (replyButtonProgress < 0.1f) replyButtonProgress = 0f
                    else mView.invalidate()
                }
            }
            val scale: Float = if (showing) {
                if (replyButtonProgress <= 0.8f) 1.2f * (replyButtonProgress / 0.8f)
                else 1.2f - 0.2f * ((replyButtonProgress - 0.8f) / 0.2f)
            } else replyButtonProgress

            if (startTracking) { // Makes device vibrate when reply is triggered.
                if (!isVibrate && absoluteTranslationX >= swipeTrigger) {
                    mView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    isVibrate = true
                }
            }

            val ratio = 3 // Reply drawable would be drawn at 1/3 of the swipe length.
            val x: Int = if (absoluteTranslationX > swipeLengthLimit) {
                if (mView.translationX > 0) swipeLengthLimit / ratio
                else mView.width - (swipeLengthLimit / ratio)
            } else {
                if (mView.translationX > 0) (mView.translationX / ratio).toInt()
                else mView.width - (absoluteTranslationX / ratio).toInt()
            }
            val y = (mView.top + mView.measuredHeight / 2).toFloat()
            imageDrawable.setBounds(
                (x - 12.dp * scale).toInt(),
                (y - 11.dp * scale).toInt(),
                (x + 12.dp * scale).toInt(),
                (y + 10.dp * scale).toInt()
            )
            imageDrawable.draw(canvas)
        }

        private val Int.dp
            get() = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                toFloat(), context.resources.displayMetrics
            ).roundToInt()
    }
}