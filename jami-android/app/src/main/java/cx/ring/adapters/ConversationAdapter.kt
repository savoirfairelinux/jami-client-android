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
package cx.ring.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.TypedValue
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.client.MediaViewerActivity
import cx.ring.client.MessageEditActivity
import cx.ring.databinding.MenuConversationBinding
import cx.ring.fragments.ConversationFragment
import cx.ring.linkpreview.LinkPreview
import cx.ring.linkpreview.PreviewData
import cx.ring.utils.*
import cx.ring.utils.ActionHelper.Padding
import cx.ring.utils.ActionHelper.setPadding
import cx.ring.utils.ContentUri.getUriForFile
import cx.ring.utils.TextUtils.copyAndShow
import cx.ring.viewholders.ConversationViewHolder
import cx.ring.views.AvatarDrawable
import cx.ring.views.MessageBubble
import cx.ring.views.MessageStatusView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.linkify.LinkifyPlugin
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.conversation.ConversationPresenter
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.model.Interaction.TransferStatus
import net.jami.utils.Log
import net.jami.utils.StringUtils
import org.commonmark.node.SoftLineBreak
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ConversationAdapter(
    private val conversationFragment: ConversationFragment,
    private val presenter: ConversationPresenter,
    private val isSearch: Boolean = false
) : RecyclerView.Adapter<ConversationViewHolder>() {
    private var mInteractions: MutableList<Interaction> = ArrayList<Interaction>()
    private val res = conversationFragment.resources
    private val mPictureMaxSize =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f, res.displayMetrics).toInt()
    private var successView: ConversationViewHolder? = null

    @ColorInt
    var convColor = 0
        set(value) {
            field = value
            convColorTint = MaterialColors.compositeARGBWithAlpha(value, (MaterialColors.ALPHA_LOW * 255).toInt())
            notifyDataSetChanged()
        }

    @ColorInt
    private var convColorTint = 0

    private val formatter = Formatter(StringBuilder(64), Locale.getDefault())

    private val callPadding = Padding(
        res.getDimensionPixelSize(R.dimen.text_message_padding),
        res.getDimensionPixelSize(R.dimen.padding_call_vertical),
        res.getDimensionPixelSize(R.dimen.text_message_padding),
        res.getDimensionPixelSize(R.dimen.padding_call_vertical)
    )

    private val timestampUpdateTimer: Observable<Long> =
        Observable.interval(10, TimeUnit.SECONDS, DeviceUtils.uiScheduler).startWithItem(0L)
    private var lastMsgPos = -1
    private var isComposing = false
    var showLinkPreviews = true
    private val markwon: Markwon = Markwon.builder(conversationFragment.requireContext())
            .usePlugin(LinkifyPlugin.create())
            // Plugin to add a new line when a soft break is used.
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    builder.on(SoftLineBreak::class.java) { visitor, _ -> visitor.forceNewLine() }
                }
            })
            .build()

    /**
     * Refreshes the data and notifies the changes
     * @param list an arraylist of interactions
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateDataset(list: MutableList<Interaction>) {
        Log.d(TAG, "updateDataset: list size=" + list.size)
        when {
            mInteractions.isEmpty() -> {
                mInteractions = list
                notifyDataSetChanged()
            }
            list.size > mInteractions.size -> {
                val oldSize = mInteractions.size
                mInteractions = list
                notifyItemRangeInserted(oldSize, list.size)
            }
            else -> {
                mInteractions = list
                notifyDataSetChanged()
            }
        }
    }

    fun add(e: Interaction): Boolean {
        if (e.isSwarm) {
            if (mInteractions.isEmpty() || mInteractions[mInteractions.size - 1].messageId == e.parentId) {
                val update = mInteractions.isNotEmpty()
                mInteractions.add(e)
                val previousLast = mInteractions.size - 1
                notifyItemInserted(previousLast)
                if (update) {
                    // Find previous last not invalid.
                    getPreviousInteractionFromPosition(previousLast)?.let { interactionNotInvalid ->
                        notifyItemChanged(mInteractions.lastIndexOf(interactionNotInvalid))
                    }
                }
                return true
            }
            var i = 0
            val n = mInteractions.size
            while (i < n) {
                if (e.messageId == mInteractions[i].parentId) {
                    Log.w(TAG, "Adding message at $i previous count $n")
                    mInteractions.add(i, e)
                    notifyItemInserted(i)
                    return i == n - 1
                } else if (e.parentId == mInteractions[i].messageId) {
                    mInteractions.add(i + 1, e)
                    notifyItemInserted(i + 1)
                    return true
                }
                i++
            }
        } else {
            val update = mInteractions.isNotEmpty()
            mInteractions.add(e)
            notifyItemInserted(mInteractions.size - 1)
            if (update) notifyItemChanged(mInteractions.size - 2)
        }
        return true
    }

    fun update(editedInteraction: Interaction) {
        mInteractions.indexOfLast { it.messageId == editedInteraction.messageId }.let { position ->
            if (position == -1) return
            mInteractions[position] = editedInteraction
            notifyItemChanged(position)
        }
    }

    fun remove(e: Interaction) {
        if (e.isSwarm) {
            for (i in mInteractions.indices.reversed()) {
                if (e.messageId == mInteractions[i].messageId) {
                    mInteractions.removeAt(i)
                    notifyItemRemoved(i)
                    if (i > 0) {
                        notifyItemChanged(i - 1)
                    }
                    if (i != mInteractions.size) {
                        notifyItemChanged(i)
                    }
                    break
                }
            }
        } else {
            for (i in mInteractions.indices.reversed()) {
                if (e.id == mInteractions[i].id) {
                    mInteractions.removeAt(i)
                    notifyItemRemoved(i)
                    if (i > 0) {
                        notifyItemChanged(i - 1)
                    }
                    if (i != mInteractions.size) {
                        notifyItemChanged(i)
                    }
                    break
                }
            }
        }
    }

    fun addSearchResults(interactions: List<Interaction>) {
        val oldSize = mInteractions.size
        mInteractions.addAll(interactions)
        notifyItemRangeInserted(oldSize, interactions.size)
    }

    fun clearSearchResults() {
        mInteractions.clear()
        notifyDataSetChanged()
    }

    /**
     * Updates the contact photo to use for this conversation
     */
    fun setPhoto() {
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mInteractions.size + if (isComposing) 1 else 0

    override fun getItemId(position: Int): Long =
        if (isComposing && position == mInteractions.size) Long.MAX_VALUE else mInteractions[position].id.toLong()

    fun getMessagePosition(messageId: String): Int {
        return mInteractions.indexOfFirst { it.messageId == messageId }
    }

    override fun getItemViewType(position: Int): Int {

        // Composing indicator
        if (isComposing && position == mInteractions.size)
            return MessageType.COMPOSING_INDICATION.ordinal

        val interaction = mInteractions[position]
        return when (interaction.type) {
            Interaction.InteractionType.CONTACT -> MessageType.CONTACT_EVENT.ordinal

            Interaction.InteractionType.CALL -> {
                if ((interaction as Call).isGroupCall) {
                    MessageType.ONGOING_GROUP_CALL.ordinal
                } else if (interaction.isIncoming) {
                    MessageType.INCOMING_CALL_INFORMATION.ordinal
                } else MessageType.OUTGOING_CALL_INFORMATION.ordinal
            }

            Interaction.InteractionType.TEXT -> {
                if (interaction.isIncoming) {
                    MessageType.INCOMING_TEXT_MESSAGE.ordinal
                } else {
                    MessageType.OUTGOING_TEXT_MESSAGE.ordinal
                }
            }

            Interaction.InteractionType.DATA_TRANSFER -> {
                val file = interaction as DataTransfer
                if (file.transferStatus == TransferStatus.TRANSFER_FINISHED) {
                    if (interaction.isIncoming) {
                        when {
                            file.isPicture -> MessageType.INCOMING_IMAGE.ordinal
                            file.isAudio -> MessageType.INCOMING_AUDIO.ordinal
                            file.isVideo -> MessageType.INCOMING_VIDEO.ordinal
                            else -> MessageType.INCOMING_FILE.ordinal
                        }
                    } else {
                        when {
                            file.isPicture -> MessageType.OUTGOING_IMAGE.ordinal
                            file.isAudio -> MessageType.OUTGOING_AUDIO.ordinal
                            file.isVideo -> MessageType.OUTGOING_VIDEO.ordinal
                            else -> MessageType.OUTGOING_FILE.ordinal
                        }
                    }
                } else if (file.transferStatus == TransferStatus.FILE_REMOVED) {
                    // display msg for a user in the same way as deleted text messages
                    if (interaction.isIncoming) {
                        MessageType.INCOMING_TEXT_MESSAGE.ordinal
                    } else MessageType.OUTGOING_TEXT_MESSAGE.ordinal
                } else {
                    if (interaction.isIncoming) {
                        MessageType.INCOMING_FILE.ordinal
                    } else MessageType.OUTGOING_FILE.ordinal
                }
            }

            Interaction.InteractionType.INVALID -> MessageType.INVALID.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val type = MessageType.entries[viewType]
        val v = if (type == MessageType.INVALID) FrameLayout(parent.context)
        else (LayoutInflater.from(parent.context).inflate(type.layout, parent, false) as ViewGroup)
        return ConversationViewHolder(v, type)
    }

    private fun configureDisplayIndicator(
        conversationViewHolder: ConversationViewHolder,
        interaction: Interaction
    ) {
        val statusIcon = conversationViewHolder.mStatusIcon ?: return
        val messageToAttach = conversationViewHolder.mLayoutStatusIconId?.id ?: return

        val conversation = interaction.conversation
        if (conversation == null || conversation !is Conversation) {
            statusIcon.isVisible = false
            return
        }

        // Attach the statusIcon to the message layout.
        statusIcon.attachToMessage(messageToAttach)

        // Remove user from statusMap.
        val modifiedStatusMap = interaction.statusMap
            .filter { conversation.findContact(net.jami.model.Uri.fromId(it.key))?.isUser != true }
        val isDisplayed = modifiedStatusMap.any { it.value == Interaction.MessageStates.DISPLAYED }
        val isReceived = modifiedStatusMap.any { it.value == Interaction.MessageStates.SUCCESS }

        val contacts = conversation.lastDisplayedMessages
            .filter { it.value == interaction.messageId }
            .map { it.key }

        // Case 1: Message is sending
        if(!isDisplayed && !isReceived){
            statusIcon.updateSending()
        }
        // Case 2: Message is received by at least one contact
        else if(!isDisplayed) {
            if(interaction.messageId == conversation.lastSent)
                updateSuccess(conversationViewHolder)
            else
                statusIcon.updateNone()
        }
        // Case 3: Message is displayed
        else if (contacts.isNotEmpty()) {
            conversationViewHolder.compositeDisposable.add(
                presenter.conversationFacade
                    .getLoadedContact(
                        accountId = interaction.account!!,
                        conversation = conversation,
                        contactIds = contacts
                    )
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe { seenBy ->
                        statusIcon.updateDisplayed(seenBy)
                    })
        }
        // Case 4: Message is displayed but not the last displayed message
        else statusIcon.updateNone()
    }

    /**
     * Configure a reaction chip and logic about it.
     *
     * @param conversationViewHolder the view layout.
     * @param interaction which have reactions
     */
    private fun configureReactions(
        conversationViewHolder: ConversationViewHolder,
        interaction: Interaction
    ) {
        val context = conversationViewHolder.itemView.context
        // If reaction chip is clicked we wants to display the reaction visualiser.
        conversationViewHolder.reactionChip?.setOnClickListener {
            val adapter = ReactionVisualizerAdapter(
                context,
                presenter::removeReaction
            )
            val dialog = MaterialAlertDialogBuilder(context)
                .setAdapter(adapter) { _, _ -> }
                .show()

            val observable = interaction.reactionObservable
                .map { reactions -> reactions.groupBy { reaction -> reaction.contact!! } }
                .switchMap { contactReactions ->
                    if (contactReactions.isEmpty())
                        return@switchMap Observable.just(emptyList())
                    // Iterate on entries.
                    val entry = contactReactions.entries.map { contactReactionsEntry ->
                        // Launch observeContact and transform the result
                        presenter.contactService.observeContact(
                            interaction.account!!, contactReactionsEntry.key, false
                        ).map { contact -> Pair(contact, contactReactionsEntry.value) }
                    }
                    // Subscribe on result.
                    Observable.combineLatest(entry) { resultDictionary ->
                        resultDictionary.map {
                            it as Pair<ContactViewModel, List<Interaction>>
                        }
                    }
                }
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { reactions ->
                    // Close dialog if no reactions anymore.
                    if (reactions.isEmpty()) {
                        dialog.dismiss()
                        return@subscribe
                    }
                    // Put the account at head of the list.
                    val mutableList = reactions.toMutableList()
                    val indexOfUserItem = mutableList.indexOfFirst { it.first.contact.isUser }
                    if (indexOfUserItem != -1) {
                        mutableList.add(0, mutableList.removeAt(indexOfUserItem))
                    }
                    adapter.setValues(mutableList)
                }
            conversationViewHolder.compositeDisposable.add(observable)

            dialog.setOnDismissListener {
                conversationViewHolder.compositeDisposable.remove(observable)
            }
        }
        // Manage the display of the chip (ui element showing the emojis)
        conversationViewHolder.compositeDisposable.add(
            interaction.reactionObservable
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { reactions ->
                    conversationViewHolder.reactionChip?.let { chip ->
                        // No reaction, hide the chip.
                        if (reactions.isEmpty())
                            chip.isVisible = false
                        else {
                            chip.text = reactions.filterIsInstance<TextMessage>()
                                .groupingBy { it.body!! }
                                .eachCount()
                                .map { it }
                                .sortedByDescending { it.value }
                                .joinToString("") {
                                    if (it.value > 1) it.key + it.value else it.key
                                }

                            (chip.background as GradientDrawable).apply {
                                mutate()
                                setStroke(
                                    context.resources
                                        .getDimensionPixelSize(R.dimen.message_reaction_stroke),
                                    if (!interaction.isIncoming) convColor
                                    else context.getColor(R.color.conversation_secondary_background)
                                )
                            }

                            chip.isVisible = true
                            chip.isClickable = true
                            chip.isFocusable = true
                            //chip.isChecked = false
                        }
                    }
                }
        )
    }

    /**
     * Configure a reply Interaction.
     *
     * @param conversationViewHolder    the view layout.
     * @param interaction               the interaction (contains the message data).
     */
    private fun configureReplyIndicator(
        conversationViewHolder: ConversationViewHolder,
        interaction: Interaction
    ) {
        val context = conversationViewHolder.itemView.context
        val conversation = interaction.conversation
        if (conversation == null || conversation !is Conversation) {
            conversationViewHolder.mReplyName?.isVisible = false
            conversationViewHolder.mReplyTxt?.isVisible = false
            return
        }

        conversationViewHolder.mReplyBubble?.let { replyBubble ->
            val replyTo = interaction.replyTo

            // If currently replying to another message :
            if (replyTo != null) {
                conversationViewHolder.compositeDisposable.add(replyTo
                    .flatMapObservable { reply ->
                        presenter.contactService
                            .observeContact(interaction.account!!, reply.contact!!, false)
                            .map { contact -> Pair(reply, contact) }
                    }
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ i ->
                        conversationViewHolder.mReplyTxt!!.text = i.first.body

                        // Name of whom we are replying to.
                        conversationViewHolder.mReplyName?.text =
                            if (i.first.isIncoming) i.second.displayName
                            else res.getString(R.string.conversation_reply_you)

                        // Apply the correct color depending if message is incoming or not.
                        val textColor:Int
                        if (i.first.isIncoming){
                            textColor = context.getColor(R.color.colorOnSurface)
                            replyBubble.background.setTint(
                                context.getColor(R.color.conversation_secondary_background)
                            )
                        }
                        else {
                            textColor = context.getColor(R.color.text_color_primary_dark)
                            replyBubble.background.setTint(convColor)
                        }
                        conversationViewHolder.mReplyTxt?.setTextColor(textColor)
                        conversationViewHolder.mReplyName?.setTextColor(textColor)

                        // Load avatar drawable from contact.
                        val avatarSize = context.resources
                            .getDimensionPixelSize(R.dimen.conversation_avatar_size_small)
                        val smallAvatarDrawable = AvatarDrawable.Builder()
                            .withContact(i.second)
                            .withCircleCrop(true)
                            .build(context)
                            .setInSize(avatarSize)
                        // Update the view.
                        conversationViewHolder.mReplyName!!.setCompoundDrawablesWithIntrinsicBounds(
                            smallAvatarDrawable, null, null, null
                        )

                        replyBubble.isVisible = true
                        conversationViewHolder.mReplyTxt!!.isVisible = true

                        // User can click on mReplyTxt (replied message)
                        // or mReplyName (text above the message) to go to it.
                        listOf(conversationViewHolder.mReplyTxt,
                            replyBubble).forEach{
                            it?.setOnClickListener{
                                i.first.messageId?.let { presenter.scrollToMessage(it) }
                            }
                        }
                    }) {
                        replyBubble.isVisible = false
                        conversationViewHolder.mReplyTxt!!.isVisible = false
                    })
            } else { // Not replying to another message, we can hide reply Textview.
                replyBubble.isVisible = false
                conversationViewHolder.mReplyTxt?.isVisible = false
            }
        }
    }

    override fun onBindViewHolder(conversationViewHolder: ConversationViewHolder, position: Int) {
        if (isComposing && position == mInteractions.size) {
            configureForTypingIndicator(conversationViewHolder)
            return
        }

        val interaction = mInteractions[position]
        conversationViewHolder.compositeDisposable.clear()

        when (interaction.type) {
            Interaction.InteractionType.INVALID -> {
                conversationViewHolder.itemView.visibility = View.GONE
                return
            }
            Interaction.InteractionType.TEXT -> configureForTextMessage(
                    conversationViewHolder,
                    interaction,
                    position
            )
            Interaction.InteractionType.CALL -> configureForCallInfo(
                    conversationViewHolder,
                    interaction,
                    position
            )
            Interaction.InteractionType.CONTACT -> configureForContactEvent(
                    conversationViewHolder,
                    interaction,
                    position
            )
            Interaction.InteractionType.DATA_TRANSFER -> configureForFileInfo(
                    conversationViewHolder,
                    interaction,
                    position
            )
        }

        conversationViewHolder.mStatusIcon?.let {
            configureDisplayIndicator(conversationViewHolder, interaction)
        }

        conversationViewHolder.mReplyName?.let {
            configureReplyIndicator(conversationViewHolder, interaction)
        }

        conversationViewHolder.reactionChip?.let {
            configureReactions(conversationViewHolder, interaction)
        }

        if (isSearch)
            configureSearchResult(conversationViewHolder, interaction)
    }

    override fun onViewRecycled(holder: ConversationViewHolder) {
        holder.itemView.setOnLongClickListener(null)
        holder.mImage?.setOnLongClickListener(null)

        holder.video?.let { video ->
            video.setOnClickListener(null)
            video.surfaceTextureListener = null
        }
        holder.surface?.release()
        holder.surface = null
        holder.player?.let { player ->
            try {
                if (player.isPlaying) player.stop()
                player.reset()
            } catch (e: Exception) {
                // left blank intentionally
            }
            player.release()
            holder.player = null
        }
        holder.mMsgTxt?.setOnLongClickListener(null)
        holder.mItem?.setOnClickListener(null)
        holder.compositeDisposable.clear()
    }

    fun setComposingStatus(composingStatus: ComposingStatus) {
        val composing = composingStatus == ComposingStatus.Active
        if (isComposing != composing) {
            isComposing = composing
            if (composing)
                notifyItemInserted(mInteractions.size)
            else notifyItemRemoved(mInteractions.size)
        }
    }

    private fun configureImage(
        viewHolder: ConversationViewHolder,
        path: File,
        displayName: String?
    ) {
        val context = viewHolder.itemView.context
        val image = viewHolder.mImage ?: return
        image.clipToOutline = true
        Glide.with(context)
            .load(path)
            .transition(withCrossFade())
            .into(image)
        image.setOnClickListener { v: View ->
            try {
                val contentUri = getUriForFile(v.context, path, displayName)
                val i = Intent(context, MediaViewerActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, "image/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    conversationFragment.requireActivity(),
                    viewHolder.mImage,
                    "picture"
                )
                conversationFragment.startActivityForResult(i, 3006, options.toBundle())
            } catch (e: Exception) {
                Log.w(TAG, "Can't open picture", e)
            }
        }
    }

    private fun configureAudio(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.itemView.context
        try {
            val acceptBtn = viewHolder.btnAccept as ImageView
            val refuseBtn = viewHolder.btnRefuse!!
            acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
            val player = MediaPlayer.create(context, getUriForFile(context, path))
            viewHolder.player = player
            if (player != null) {
                player.setOnCompletionListener { mp: MediaPlayer ->
                    mp.seekTo(0)
                    acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                }
                acceptBtn.setOnClickListener {
                    if (player.isPlaying) {
                        player.pause()
                        acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                    } else {
                        player.start()
                        acceptBtn.setImageResource(R.drawable.baseline_pause_24)
                    }
                }
                refuseBtn.setOnClickListener {
                    if (player.isPlaying) player.pause()
                    player.seekTo(0)
                    acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                }
                viewHolder.compositeDisposable.add(
                    Observable.interval(1L, TimeUnit.SECONDS, DeviceUtils.uiScheduler)
                        .startWithItem(0L)
                        .subscribe {
                            val pS = player.currentPosition / 1000
                            val dS = player.duration / 1000
                            viewHolder.mMsgTxt?.text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d / %02d:%02d",
                                pS / 60,
                                pS % 60,
                                dS / 60,
                                dS % 60
                            )
                        })
            } else {
                acceptBtn.setOnClickListener(null)
                refuseBtn.setOnClickListener(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player", e)
        }
    }

    private fun configureVideo(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.itemView.context
        viewHolder.player?.let {
            viewHolder.player = null
            it.release()
        }
        val video = viewHolder.video ?: return
        val cardLayout = viewHolder.mLayout as CardView

        val contentUri = try {
            getUriForFile(context, path)
        } catch (e: Exception) {
            Log.w(TAG, "Can't open video", e)
            return
        }
        val player = MediaPlayer.create(context, contentUri) ?: return

        viewHolder.player = player
        val playBtn = ContextCompat.getDrawable(cardLayout.context, R.drawable.baseline_play_arrow_24)!!.mutate()
        DrawableCompat.setTint(playBtn, Color.WHITE)
        cardLayout.foreground = playBtn
        player.setOnCompletionListener { mp: MediaPlayer ->
            if (mp.isPlaying) mp.pause()
            mp.seekTo(1)
            cardLayout.foreground = playBtn
        }

        player.setOnVideoSizeChangedListener { _: MediaPlayer, width: Int, height: Int ->
            Log.w(TAG, "OnVideoSizeChanged " + width + "x" + height)
            val p = video.layoutParams as FrameLayout.LayoutParams
            val maxDim = max(width, height)
            if (maxDim != 0) {
                p.width = width * mPictureMaxSize / maxDim
                p.height = height * mPictureMaxSize / maxDim
            } else {
                p.width = 0
                p.height = 0
            }
            video.layoutParams = p
        }

        if (video.isAvailable) {
            if (viewHolder.surface == null) {
                viewHolder.surface = Surface(video.surfaceTexture)
            }
            player.setSurface(viewHolder.surface)
        }

        video.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (viewHolder.surface == null) {
                    viewHolder.surface = Surface(surfaceTexture).also { surface ->
                        try {
                            player.setSurface(surface)
                        } catch (e: Exception) {
                            // Left blank
                        }
                    }
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) { }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                try {
                    player.setSurface(null)
                } catch (e: Exception) {
                    // Left blank
                }
                viewHolder.surface?.let {
                    viewHolder.surface = null
                    it.release()
                }
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        video.setOnClickListener {
            try {
                if (player.isPlaying) {
                    player.pause()
                    (viewHolder.mLayout as CardView).foreground = playBtn
                } else {
                    player.start()
                    (viewHolder.mLayout as CardView).foreground = null
                }
            } catch (e: Exception) {
                // Left blank
            }
        }
        player.seekTo(1)
    }

    /**
     * Display and manage the popup that allows user to react/reply/share/edit/delete message ...
     */
    private fun openItemMenu(viewHolder: ConversationViewHolder, view: View, interaction: Interaction) {
        MenuConversationBinding.inflate(LayoutInflater.from(view.context)).apply {
            val history = interaction.historyObservable.blockingFirst()
            val isDeleted = interaction is TextMessage && interaction.body.isNullOrEmpty()
            val isFileMenu = interaction is DataTransfer && interaction.isComplete

            // Configure what should be displayed
            convActionFileOpen.isVisible = isFileMenu
            convActionFileSave.isVisible = isFileMenu
            convActionFileDelete.isVisible = isFileMenu
            convActionCopyText.isVisible = !isDeleted && interaction !is DataTransfer
            convActionEdit.isVisible = !isDeleted && !interaction.isIncoming && interaction is TextMessage
            convActionDelete.isVisible = !isDeleted && !interaction.isIncoming
            convActionHistory.isVisible = !isDeleted && history.size > 1
            root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            // The popup that display all the buttons
            val popupWindow = PopupWindow(
                root, LinearLayout.LayoutParams.WRAP_CONTENT, root.measuredHeight, false
            )
            popupWindow.isOutsideTouchable = true
            popupWindow.elevation = view.context.resources.getDimension(R.dimen.call_preview_elevation)
            popupWindow.showAsDropDown(view)

            val textViews = listOf(
                convActionEmoji1.chip, convActionEmoji2.chip,
                convActionEmoji3.chip, convActionEmoji4.chip
            )

            convActionEmoji1.chip.text = view.context.getString(R.string.default_emoji_1)
            convActionEmoji2.chip.text = view.context.getString(R.string.default_emoji_2)
            convActionEmoji3.chip.text = view.context.getString(R.string.default_emoji_3)
            convActionEmoji4.chip.text = view.context.getString(R.string.default_emoji_4)

            // Subscribe on reactions to allows user to see which reaction he already selected.
            val disposable = interaction.reactionObservable
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { reactions ->
                    textViews.forEach { textView ->
                        // Set checked reactions already sent.
                        textView.isChecked = reactions.any {
                            (textView.text == it.body) && (it.contact?.isUser == true)
                        }
                    }
                    popupWindow.update()
                }
            viewHolder.compositeDisposable.add(disposable)

            popupWindow.setOnDismissListener {
                val type = viewHolder.type.transferType
                if (convColor != 0 && (interaction.type == Interaction.InteractionType.TEXT
                            || type == MessageType.TransferType.FILE
                            || type == MessageType.TransferType.AUDIO ) && !interaction.isIncoming
                ) {
                    view.background?.setTint(convColor)
                } else view.background?.setTintList(null)

                viewHolder.compositeDisposable.remove(disposable)
            }

            // Callback executed when emoji is clicked.
            // We want to know if the reaction is already set.
            // If set we want to remove, else we want to append.
            val emojiCallback = View.OnClickListener { view ->
                // Subscribe to know which are the current reactions.
                viewHolder.compositeDisposable.add(interaction.reactionObservable
                    .observeOn(DeviceUtils.uiScheduler)
                    .firstOrError()
                    .subscribe { reactions ->
                        // Try to find a reaction having corresponding to the one clicked.
                        val reaction = reactions.firstOrNull {
                            (it.body == (view as TextView).text) && (it.contact?.isUser == true)
                        }
                        if (reaction != null)
                        // Previously, it was not forbidden to send multiple times the same
                        // reaction. Hence, we only remove the first one.
                            presenter.removeReaction(reaction)
                        else // If null, it means we didn't find anything. So let's send it.
                            presenter.sendReaction(interaction, (view as TextView).text)
                        popupWindow.dismiss()
                    }
                )
            }
            textViews.forEach { it.setOnClickListener(emojiCallback) } // Set callback

            // Configure reply
            convActionReply.setOnClickListener {
                presenter.startReplyTo(interaction)
                popupWindow.dismiss()
            }
            emojiPicker.setOnEmojiPickedListener {
                presenter.sendReaction(interaction, it.emoji)
                popupWindow.dismiss()
            }
            convActionMore.setOnClickListener {
                val newState = menuActions.isVisible
                menuActions.isVisible = !newState
                emojiPicker.isVisible = newState
                popupWindow.let {
                    root.measure(root.width, View.MeasureSpec.UNSPECIFIED)
                    it.height = root.measuredHeight
                    it.dismiss()
                    it.showAsDropDown(view)
                }
            }

            // Open file
            convActionFileOpen.setOnClickListener {
                presenter.openFile(interaction)
            }

            // Save file
            convActionFileSave.setOnClickListener {
                presenter.saveFile(interaction)
            }

            // Manage copy
            convActionCopyText.setOnClickListener {
                conversationFragment.requireContext()
                    .let { copyAndShow(it, it.getString(R.string.clip_message), interaction.body) }
                popupWindow.dismiss()
            }

            convActionFileDelete.setOnClickListener {
                presenter.deleteConversationFile(interaction)
            }

            // Manage Edit and Delete actions
            if (!interaction.isIncoming) {
                // Edit
                convActionEdit.setOnClickListener {
                    try {
                        val i = Intent(it.context, MessageEditActivity::class.java)
                            .setData(
                                Uri.withAppendedPath(
                                    ConversationPath.toUri(
                                        interaction.account!!,
                                        interaction.conversationId!!
                                    ), interaction.messageId
                                )
                            )
                            .setAction(Intent.ACTION_EDIT)
                            .putExtra(
                                Intent.EXTRA_TEXT,
                                viewHolder.mMessageBubble!!.getText().toString()
                            )
                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            conversationFragment.requireActivity(),
                            viewHolder.mMessageBubble!!,
                            "messageEdit"
                        )
                        conversationFragment.startActivityForResult(
                            i,
                            ConversationFragment.REQUEST_CODE_EDIT_MESSAGE,
                            options.toBundle()
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Can't open picture", e)
                    }
                    popupWindow.dismiss()
                }

                // Delete
                convActionDelete.setOnClickListener {
                    presenter.deleteConversationItem(interaction)
                    popupWindow.dismiss()
                }
            } else {
                convActionEdit.setOnClickListener(null)
                convActionDelete.setOnClickListener(null)
            }

            // Share
            convActionShare.setOnClickListener {
                if (interaction is DataTransfer)
                    presenter.shareFile(interaction)
                else if (interaction is TextMessage)
                    presenter.shareText(interaction)
                popupWindow.dismiss()
            }

            // Message history
            if (convActionHistory.isVisible)
                convActionHistory.setOnClickListener {
                    viewHolder.compositeDisposable.add(
                        interaction.historyObservable.firstOrError().subscribe { c ->
                            Log.w(TAG, "Message history ${c.size}")
                            c.forEach {
                                Log.w(TAG, "Message ${it.type} ${it.body} $it")
                            }
                            MaterialAlertDialogBuilder(it.context)
                                .setTitle(it.context.getString(R.string.menu_message_history))
                                .setItems(c.filterIsInstance<TextMessage>().map { it.body!! }
                                    .toTypedArray())
                                { dialog, _ -> dialog.dismiss() }
                                .create()
                                .show()
                        }
                    )
                    popupWindow.dismiss()
                }
        }
    }

    private fun configureForFileInfo(
        viewHolder: ConversationViewHolder,
        interaction: Interaction,
        position: Int
    ) {
        val context = viewHolder.itemView.context
        val file = interaction as DataTransfer
        val path = presenter.deviceRuntimeService.getConversationPath(file)
        val timeString = TextUtils.timestampToTime(context, formatter, file.timestamp)
        viewHolder.mFileTime?.text = timeString

        if (interaction.transferStatus == TransferStatus.FILE_REMOVED) {
            // handle in the same way as removed text
            configureForTextMessage(viewHolder, interaction, position)
            return
        }

        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
            viewHolder.mFileSize?.text = when (val status = file.transferStatus) {
                TransferStatus.TRANSFER_FINISHED -> Formatter.formatFileSize(context, file.totalSize)
                TransferStatus.TRANSFER_ONGOING -> String.format("%s / %s - %s",
                    Formatter.formatFileSize(context, file.bytesProgress),
                    Formatter.formatFileSize(context, file.totalSize),
                    TextUtils.getReadableFileTransferStatus(context, status)
                )
                else -> String.format(Formatter.formatFileSize(context, file.totalSize))
            }
        })

        val isDateShown = hasPermanentDateString(file, position)
        if (isDateShown) {
            viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                viewHolder.mMsgDetailTxtPerm?.text =
                    TextUtils.timestampToDate(context, formatter, file.timestamp)
            })
            viewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
        } else {
            viewHolder.mMsgDetailTxtPerm?.visibility = View.GONE
        }

        val contact = interaction.contact ?: return
        if (interaction.isIncoming && presenter.isGroup()) {
            viewHolder.mAvatar?.let { avatar ->
                avatar.setAvatar(conversationFragment.getConversationAvatar(contact.primaryNumber))
                avatar.visibility = View.VISIBLE
            }
            val account = interaction.account?: return
            // Show the name of the contact.
            viewHolder.mPeerDisplayName?.apply {
                    visibility = View.VISIBLE
                    viewHolder.compositeDisposable.add(
                        presenter.contactService
                            .observeContact(account, contact, false)
                            .observeOn(DeviceUtils.uiScheduler)
                            .subscribe { text = it.displayName }
                    )
            }
        } else {
            viewHolder.mAvatar?.visibility = View.GONE
            viewHolder.mPeerDisplayName?.visibility = View.GONE
        }
        val type = viewHolder.type.transferType
        val longPressView = when (type) {
            MessageType.TransferType.IMAGE -> viewHolder.mImage
            MessageType.TransferType.VIDEO -> viewHolder.video
            MessageType.TransferType.AUDIO -> viewHolder.mAudioInfoLayout
            else -> viewHolder.mFileInfoLayout
        } ?: return

        if (type == MessageType.TransferType.AUDIO || type == MessageType.TransferType.FILE) {
            longPressView.background?.setTintList(null)
        }

        longPressView.setOnLongClickListener { v: View ->
            if (type == MessageType.TransferType.AUDIO || type == MessageType.TransferType.FILE) {
                if (file.isIncoming) {
                    longPressView.background.setTint(context.getColor(R.color.grey_500))
                } else {
                    longPressView.background.setTint(convColorTint)
                }
            }
            openItemMenu(viewHolder, v, file)
            true
        }

        val isMessageSeparationNeeded = isMessageSeparationNeeded(isDateShown, position)
        when (type) {
            MessageType.TransferType.IMAGE -> {
                // Add margin if message need to be separated.
                viewHolder.mAnswerLayout?.updateLayoutParams<MarginLayoutParams> {
                    topMargin = if (!isMessageSeparationNeeded) 0 else context.resources
                        .getDimensionPixelSize(R.dimen.conversation_message_separation)
                }
                configureImage(viewHolder, path, file.body)
            }

            MessageType.TransferType.VIDEO -> {
                // Add margin if message need to be separated.
                viewHolder.mAnswerLayout?.updateLayoutParams<MarginLayoutParams> {
                    topMargin = if (!isMessageSeparationNeeded) 0 else context.resources
                        .getDimensionPixelSize(R.dimen.conversation_message_separation)
                }
                configureVideo(viewHolder, path)
            }

            MessageType.TransferType.AUDIO -> {
                // Add margin if message need to be separated.
                viewHolder.mAudioLayout?.updateLayoutParams<MarginLayoutParams> {
                    topMargin = if (!isMessageSeparationNeeded) 0 else context.resources
                        .getDimensionPixelSize(R.dimen.conversation_message_separation)
                }
                viewHolder.mAudioInfoLayout?.setOnClickListener(null)
                // Set the tint of the audio file background
                if (file.isOutgoing) {
                    viewHolder.mAudioInfoLayout?.background?.setTint(convColor)
                } else {
                    viewHolder.mAudioInfoLayout?.background?.setTint(
                        context.getColor(R.color.conversation_secondary_background)
                    )
                }
                configureAudio(viewHolder, path)
            }

            MessageType.TransferType.FILE -> {
                // Add margin if message need to be separated.
                viewHolder.mLayout?.updateLayoutParams<MarginLayoutParams> {
                    topMargin = if (!isMessageSeparationNeeded) 0 else context.resources
                        .getDimensionPixelSize(R.dimen.conversation_message_separation)
                }

                val status = file.transferStatus
                viewHolder.mIcon?.setPadding(res.getDimensionPixelSize(R.dimen.padding_large))
                viewHolder.mIcon?.setClipToOutline(true)
                viewHolder.mIcon?.imageTintList = context.getColorStateList(R.color.file_icon_out)
                viewHolder.mIcon?.backgroundTintList = context.getColorStateList(if (file.isOutgoing) R.color.file_icon_background_out else R.color.file_icon_background_in)
                viewHolder.mIcon?.setImageResource(
                    if (status.isError) R.drawable.baseline_warning_24
                    else R.drawable.baseline_attach_file_24
                )
                viewHolder.mFileTitle?.text = file.displayName
                viewHolder.mFileInfoLayout?.setOnClickListener(null)
                // Set the tint of the file background
                if (file.isOutgoing) viewHolder.mFileInfoLayout?.background?.setTint(convColor)
                // Show the download button
                when (status) {
                    TransferStatus.TRANSFER_AWAITING_HOST, TransferStatus.FILE_AVAILABLE -> {
                        viewHolder.mFileDownloadButton?.let {
                            it.visibility = View.VISIBLE
                            it.setOnClickListener { presenter.acceptFile(file) }
                        }
                    }

                    else -> {
                        viewHolder.mFileDownloadButton?.visibility = View.GONE
                        if (status == TransferStatus.TRANSFER_ONGOING) {
                            viewHolder.progress?.max = (file.totalSize / 1024).toInt()
                            viewHolder.progress?.setProgress((file.bytesProgress / 1024).toInt(), true)
                            viewHolder.progress?.show()
                        } else {
                            viewHolder.progress?.hide()
                            val uri = try { getUriForFile(context, path) } catch (e: Exception) { return }
                            if (context.contentResolver.getType(uri) == "application/pdf") {
                                val size = res.getDimensionPixelSize(R.dimen.conversation_file_preview)
                                viewHolder.compositeDisposable.add(Single
                                    .fromCallable { BitmapUtils.documentToBitmap(context, uri, size, size)!! }
                                    .subscribeOn(Schedulers.computation())
                                    .observeOn(DeviceUtils.uiScheduler)
                                    .subscribe({ bitmap ->
                                        viewHolder.mIcon?.setPadding(0)
                                        viewHolder.mIcon?.imageTintList = null
                                        viewHolder.mIcon?.backgroundTintList = context.getColorStateList(R.color.white)
                                        viewHolder.mIcon?.setImageBitmap(bitmap)
                                    }) {
                                        Log.w(TAG, "Error loading PDF preview", it)
                                    })
                            }
                        }
                        viewHolder.mFileInfoLayout?.setOnClickListener { presenter.openFile(file) }
                    }
                }
            }
        }
    }

    private fun configureForTypingIndicator(viewHolder: ConversationViewHolder) {
        // Set the alignment of the typing indicator.
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        if (presenter.isGroup()) {
            layoutParams.setMargins(
                res.getDimensionPixelSize(R.dimen.margin_with_avatar), 0, 0, 0
            )
        } else {
            layoutParams.setMargins(
                res.getDimensionPixelSize(R.dimen.conditional_left_conversation_margin), 0, 0, 0
            )
        }
        viewHolder.mTypingIndicatorLayout?.layoutParams = layoutParams
        //Start the animation.
        AnimatedVectorDrawableCompat.create(
            viewHolder.itemView.context, R.drawable.typing_indicator_animation
        )?.let { anim ->
            viewHolder.mIcon?.setImageDrawable(anim)
            anim.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    anim.start()
                }
            })
            anim.start()
        }
    }

    /**
     * Configures the background of the message bubble.
     * It changes if it's an incoming/outgoing message, it position or if it's an emoji.
     * ResIndex indicates how is considered the message (first, single, last, etc.).
     */
    private fun updateMessageBackground(
        context: Context, messageBubble: MessageBubble,
        messageSequenceType: SequenceType,
        isOnlyEmoji: Boolean, isReplying: Boolean, isDeleted: Boolean, isIncoming: Boolean,
    ) {
        if (isOnlyEmoji && !isReplying)
            messageBubble.background = null
        else {
            // Manage layout for standard message. Index refers to msgBGLayouts array.
            val resIndex = if (isReplying && !isDeleted) {
                // Reply message incoming, first or single.
                if (isIncoming)
                    if (messageSequenceType == SequenceType.FIRST) 11 else 10
                // Reply message outgoing, first or single
                else if (messageSequenceType == SequenceType.FIRST) 9 else 8
            } else {
                // Standard message, incoming or outgoing and first, single or last.
                messageSequenceType.ordinal + (if (isIncoming) 1 else 0) * 4
            }

            messageBubble.background = ContextCompat.getDrawable(context, msgBGLayouts[resIndex])

            if (isReplying && convColor != 0 && !isDeleted && !isIncoming) {
                // Drawable is a layer list. Only need to change the color of the main bubble.
                (messageBubble.background as LayerDrawable).apply {
                    (findDrawableByLayerId(R.id.main_bubble) as GradientDrawable)
                        .setColor(convColor)
                }
            } else if (convColor != 0 && !isIncoming) messageBubble.setBubbleColor(convColor)
        }
    }

    /** Configures the background of the LinkPreview to follow shape of MessageBubble. */
    private fun updateLinkPreviewBackground(
        linkPreviewLayout: ViewGroup,
        messageSequenceType: SequenceType,
    ) {
        when (messageSequenceType) {
            SequenceType.FIRST, SequenceType.MIDDLE -> {
                linkPreviewLayout.setBackgroundResource(R.drawable.linkpreview_bg_out_first_or_middle)
            }
            SequenceType.LAST, SequenceType.SINGLE -> {
                linkPreviewLayout.setBackgroundResource(R.drawable.linkpreview_bg_out_last_or_single)
            }
        }
    }

    /**
     * Configures the viewHolder to display a classic text message, ie. not a call info text message
     *
     * @param viewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     * @param position       The position of the viewHolder
     */
    private fun configureForTextMessage(
        viewHolder: ConversationViewHolder,
        interaction: Interaction,
        position: Int
    ) {
        val context = viewHolder.itemView.context
        val account = interaction.account ?: return
        val contact = interaction.contact ?: return
        val isDeleted = interaction.body.isNullOrEmpty() || interaction.transferStatus == TransferStatus.FILE_REMOVED
        val isEdited = interaction.history.size > 1
        val isReplying = interaction.replyToId != null

        val messageBubble = viewHolder.mMessageBubble ?: return
        val replyBubble = viewHolder.mReplyBubble
        val answerLayout = viewHolder.mAnswerLayout
        val peerDisplayName = viewHolder.mPeerDisplayName

        val isDateShown = hasPermanentDateString(interaction, position)
        val msgSequenceType = getMsgSequencing(position, isDateShown)

        val message = interaction.body?.trim() ?: ""
        val messageTime = TextUtils.timestampToTime(context, formatter, mInteractions[position].timestamp)
        val timePermanent = viewHolder.mMsgDetailTxtPerm

        // Add margin if message need to be separated.
        val isMessageSeparationNeeded = isMessageSeparationNeeded(isDateShown, position)
        viewHolder.mMessageLayout?.updateLayoutParams<MarginLayoutParams> {
            topMargin = if (!isMessageSeparationNeeded) 0 else context.resources
                .getDimensionPixelSize(R.dimen.conversation_message_separation)
        }

        messageBubble.setBubbleColor(null)
        // Manage the update of the timestamp
        if (isDateShown) {
            viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                timePermanent?.text = TextUtils.timestampToDate(context, formatter, interaction.timestamp)
            })
            viewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
        } else viewHolder.mMsgDetailTxtPerm?.visibility = View.GONE

        // If in a replying bubble, we need to overlap the message bubble
        // with the answered message bubble.
        if (interaction.replyToId != null && !isDeleted)
            messageBubble.updateLayoutParams<MarginLayoutParams> {
                topMargin = context.resources.getDimensionPixelSize(R.dimen.conversation_reply_overlap)
            }
        else messageBubble.updateLayoutParams<MarginLayoutParams> { topMargin = 0 }

        // Manage the background of the message bubble.
        updateMessageBackground(
            context, messageBubble, msgSequenceType,
            isOnlyEmoji = StringUtils.isOnlyEmoji(message),
            isReplying = isReplying,
            isDeleted = isDeleted,
            isIncoming = interaction.isIncoming
        )

        // Manage long press.
        messageBubble.setOnLongClickListener { v: View ->
            openItemMenu(viewHolder, v, interaction)
            if (interaction.isIncoming)
                messageBubble.setBubbleColor(context.getColor(R.color.grey_500))
            else messageBubble.setBubbleColor(convColor)
            true
        }

        // Manage the message content.
        answerLayout?.visibility = View.GONE
        if (StringUtils.isOnlyEmoji(message) && !isReplying) {
            messageBubble.updateEmoji(message, messageTime, isEdited)
        } else {
            messageBubble.updateStandard(markwon.toMarkdown(message), messageTime, isEdited)

            // Manage layout for message with a link inside.
            if (showLinkPreviews && !isDeleted) {
                val cachedPreview = interaction.preview as? Maybe<PreviewData> ?: LinkPreview
                        .getFirstUrl(message)
                        .flatMap { url -> LinkPreview.load(url) }
                        .cache()
                        .apply { interaction.preview = this }

                viewHolder.compositeDisposable.add(cachedPreview
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ data ->
                        val linkPreviewLayout = viewHolder.mAnswerLayout ?: return@subscribe
                        Log.w(TAG, "got preview $data")
                        val image = viewHolder.mImage ?: return@subscribe
                        if (data.imageUrl.isNotEmpty()) {
                            Glide.with(context)
                                .load(data.imageUrl)
                                .centerCrop()
                                .into(image)
                            image.visibility = View.VISIBLE
                        } else {
                            image.visibility = View.GONE
                        }
                        viewHolder.mHistTxt?.text = data.title
                        if (data.description.isNotEmpty()) {
                            viewHolder.mHistDetailTxt?.visibility = View.VISIBLE
                            viewHolder.mHistDetailTxt?.text = data.description
                        } else {
                            viewHolder.mHistDetailTxt?.visibility = View.GONE
                        }
                        updateLinkPreviewBackground(
                            linkPreviewLayout = linkPreviewLayout,
                            messageSequenceType = msgSequenceType
                        )
                        linkPreviewLayout.visibility = View.VISIBLE
                        val url = Uri.parse(data.baseUrl)
                        viewHolder.mPreviewDomain?.text = url.host
                        linkPreviewLayout.setOnClickListener {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url))
                        }
                    }) { e -> Log.e(TAG, "Can't load preview", e) })
            } else answerLayout?.visibility = View.GONE
        }

        val endOfSeq = msgSequenceType == SequenceType.LAST || msgSequenceType == SequenceType.SINGLE
        // Manage animation for avatar and name.
        val avatar = viewHolder.mAvatar
        if (presenter.isGroup() && interaction.isIncoming) {
            avatar?.let {
                if (endOfSeq) { // To only display the avatar of the last message.
                    avatar.setAvatar(conversationFragment.getConversationAvatar(contact.primaryNumber))
                    avatar.visibility = View.VISIBLE
                } else {
                    if (position == lastMsgPos - 1) {
                        ActionHelper.startFadeOutAnimation(avatar)
                    } else {
                        avatar.setAvatar(null)
                        avatar.visibility = View.INVISIBLE
                    }
                }
            }

            // Show the name of the contact.
            val startOfSeq = msgSequenceType == SequenceType.FIRST
                    || msgSequenceType == SequenceType.SINGLE
            peerDisplayName?.apply {
                if (startOfSeq) {
                    visibility = View.VISIBLE
                    viewHolder.compositeDisposable.add(
                        presenter.contactService
                            .observeContact(account, contact, false)
                            .observeOn(DeviceUtils.uiScheduler)
                            .subscribe { text = it.displayName }
                    )
                } else {
                    visibility = View.GONE
                    text = null
                }
            }
        } else {
            avatar?.visibility = View.GONE
            peerDisplayName?.visibility = View.GONE
        }

        // Manage deleted message.
        if (isDeleted) {
            replyBubble?.visibility = View.GONE
            if (interaction.isIncoming) {
                viewHolder.compositeDisposable.add(
                    presenter.contactService
                        .observeContact(account, contact, false)
                        .observeOn(DeviceUtils.uiScheduler)
                        .subscribe { username ->
                            messageBubble.updateDeleted(messageTime, username.displayName)
                        }
                )
            } else {
                messageBubble.updateDeleted(
                    messageTime,
                    context.getString(R.string.conversation_info_contact_you)
                )
            }
            messageBubble.setOnLongClickListener(null)
        }
    }

    private fun configureForContactEvent(
        viewHolder: ConversationViewHolder,
        interaction: Interaction,
        position: Int
    ) {
        val context = viewHolder.itemView.context
        val event = interaction as ContactEvent
        Log.w(TAG,
            "configureForContactEvent ${event.account} ${event.event} ${event.contact} ${event.author} "
        )
        // Manage the date of the event.
        val isDateShown = hasPermanentDateString(event, position)
        viewHolder.mMsgDetailTxt?.apply {
            if (isDateShown || getPreviousInteractionFromPosition(position) == null) {
                visibility = View.VISIBLE
                text = TextUtils.timestampToDate(context, formatter, event.timestamp)
            } else {
                visibility = View.GONE
            }
        }
        val timestamp = TextUtils.timestampToTime(context, formatter, event.timestamp)

        if (interaction.isSwarm) {
            viewHolder.compositeDisposable.add(
                presenter.contactService.observeContact(event.account!!, event.contact!!, false)
                    .observeOn(DeviceUtils.uiScheduler)
                .subscribe { vm ->
                    val eventString = context.getString(when (event.event) {
                        ContactEvent.Event.ADDED -> R.string.conversation_contact_added
                        ContactEvent.Event.INVITED -> R.string.conversation_contact_invited
                        ContactEvent.Event.REMOVED -> R.string.conversation_contact_left
                        ContactEvent.Event.BLOCKED -> R.string.conversation_contact_blocked
                        ContactEvent.Event.UNBLOCKED -> R.string.conversation_contact_unblocked
                        else -> R.string.hist_contact_added
                    }, vm.displayName)
                    viewHolder.mMsgTxt?.text = "$eventString, $timestamp"
                })
        } else {
            val eventString = when (event.event) {
                ContactEvent.Event.ADDED -> R.string.hist_contact_added
                ContactEvent.Event.INVITED -> R.string.hist_contact_invited
                ContactEvent.Event.REMOVED -> R.string.hist_contact_left
                ContactEvent.Event.BLOCKED -> R.string.hist_contact_blocked
                ContactEvent.Event.UNBLOCKED -> R.string.hist_contact_unblocked
                ContactEvent.Event.INCOMING_REQUEST -> R.string.hist_invitation_received
                else -> R.string.hist_contact_added
            }
            viewHolder.mMsgTxt?.text = "$eventString, $timestamp"
        }
    }

    /**
     * Message Separation is used to highlight two group of messages.
     * We don't need message separation if:
     * - The message is the first of the conversation
     * - The message is the first of the day (date already shown)
     */
    private fun isMessageSeparationNeeded(
        isDateShown: Boolean,
        messagePosition: Int,
    ): Boolean = getPreviousInteractionFromPosition(messagePosition)?.let { firstInteraction ->
        val secondInteraction = mInteractions[messagePosition]
        !isDateShown && isSeqBreak(firstInteraction, secondInteraction)
    } ?: false

    /**
     * Configures the viewHolder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     */
    private fun configureForCallInfo(
        convViewHolder: ConversationViewHolder,
        interaction: Interaction,
        position: Int
    ) {
        val context = convViewHolder.itemView.context
        // Reset the scale of the icon
        convViewHolder.mIcon?.scaleX = 1f

        // In the case were it is not a swarm (legacy call or SIP?)
        if (!interaction.isSwarm) {
            convViewHolder.mCallInfoLayout?.apply {
                background?.setTintList(null) // Remove the tint

                //TODO define popup menu to invoke in onLongClickListener
//                setOnLongClickListener {
//                    background?.setTint(context.getColor(R.color.grey_500))
//                    false
//                }
            }
        }

        val call = interaction as Call

        val peerDisplayName = convViewHolder.mPeerDisplayName
        val avatar = convViewHolder.mAvatar
        val timePermanent = convViewHolder.mMsgDetailTxtPerm

        val account = interaction.account ?: return
        val contact = call.contact ?: return
        val isDateShown = hasPermanentDateString(call, position)
        val msgSequenceType = getMsgSequencing(position, isDateShown)

        val endOfSeq = msgSequenceType == SequenceType.LAST
                || msgSequenceType == SequenceType.SINGLE
        val startOfSeq = msgSequenceType == SequenceType.FIRST
                || msgSequenceType == SequenceType.SINGLE
        val resIndex = msgSequenceType.ordinal + (if (call.isIncoming) 1 else 0) * 4

        // Add margin if message need to be separated.
        val isMessageSeparationNeeded = isMessageSeparationNeeded(isDateShown, position)
        convViewHolder.mCallLayout?.updateLayoutParams<MarginLayoutParams> {
            topMargin = if (!isMessageSeparationNeeded) 0 else context.resources
                .getDimensionPixelSize(R.dimen.conversation_message_separation)
        }

        // Only show the avatar if it is a group conversation
        if (presenter.isGroup()) {
            // Manage animation for avatar.
            // To only display the avatar of the last message.
            if (endOfSeq) {
                avatar?.setAvatar(conversationFragment.getConversationAvatar(contact.primaryNumber))
                avatar?.visibility = View.VISIBLE
            } else {
                if (position == lastMsgPos - 1) {
                    avatar?.let { ActionHelper.startFadeOutAnimation(avatar) }
                } else {
                    avatar?.setAvatar(null)
                    avatar?.visibility = View.INVISIBLE
                }
            }
        } else avatar?.visibility = View.GONE

        // Show the name of the contact if it is a group conversation
        peerDisplayName?.apply {
            if (presenter.isGroup() && startOfSeq) {
                visibility = View.VISIBLE
                convViewHolder.compositeDisposable.add(
                    presenter.contactService
                        .observeContact(account, contact, false)
                        .observeOn(DeviceUtils.uiScheduler)
                        .subscribe { text = it.displayName }
                )
            } else visibility = View.GONE
        }

        // Manage the update of the timestamp
        if (isDateShown) {
            convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                timePermanent?.text = TextUtils
                    .timestampToDate(context, formatter, call.timestamp)
            })
            convViewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
        } else convViewHolder.mMsgDetailTxtPerm?.visibility = View.GONE

        convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
            val timeString =
                TextUtils.timestampToTime(context, formatter, call.timestamp)
            convViewHolder.mCallTime?.text = timeString
        })
        // When a group call is occurring but you are not in it, a message is displayed
        // in conversation to inform the user about the call and invite him to join.
        if (call.isGroupCall) {
            val callAcceptLayout = convViewHolder.mCallAcceptLayout ?: return
            val callInfoText = convViewHolder.mCallInfoText ?: return
            val acceptCallAudioButton =
                convViewHolder.mAcceptCallAudioButton ?: return
            val acceptCallVideoButton =
                convViewHolder.mAcceptCallVideoButton ?: return

            callAcceptLayout.apply {
                // Accept with audio only
                convViewHolder.mAcceptCallAudioButton?.setOnClickListener {
                    call.confId?.let { presenter.goToGroupCall(false) }
                }
                // Accept call with video
                convViewHolder.mAcceptCallVideoButton?.setOnClickListener {
                    call.confId?.let { presenter.goToGroupCall(true) }
                }
            }

            // Set the background to the call started message.
            callAcceptLayout.background = ContextCompat.getDrawable(context, msgBGLayouts[resIndex])

            if (call.isIncoming) {
                // Show the avatar of the caller if last or single.

                // We can call ourselves in a group call with different devices.
                // Set the message to the left when it is incoming.
                convViewHolder.mGroupCallLayout?.gravity = Gravity.START
                // Show the name of the contact.
                peerDisplayName?.apply {
                    if (startOfSeq) {
                        visibility = View.VISIBLE
                        convViewHolder.compositeDisposable.add(
                            presenter.contactService
                                .observeContact(account, contact, false)
                                .observeOn(DeviceUtils.uiScheduler)
                                .subscribe {
                                    text = it.displayName
                                }
                        )
                    } else visibility = View.GONE
                }
                // Use the original color of the icons.
                callInfoText.setTextColor(context.getColor(R.color.colorOnSurface))
                acceptCallAudioButton.setColorFilter(context.getColor(R.color.accept_call_button))
                acceptCallVideoButton.setColorFilter(context.getColor(R.color.accept_call_button))
                callAcceptLayout.background.setTint(
                    context.getColor(R.color.conversation_secondary_background)
                )
            } else {
                // Set the message to the right because it is outgoing.
                convViewHolder.mGroupCallLayout?.gravity = Gravity.END
                // Hide the name of the contact.
                peerDisplayName?.visibility = View.GONE
                // Set the color to the call started message.
                if (convColor != 0) {
                    callAcceptLayout.background.setTint(convColor)
                    callInfoText.setTextColor(
                        context.getColor(R.color.text_color_primary_dark)
                    )
                    acceptCallAudioButton.setColorFilter(
                        context.getColor(R.color.white)
                    )
                    acceptCallVideoButton.setColorFilter(
                        context.getColor(R.color.white)
                    )
                    callAcceptLayout.background.setTint(convColor)
                }
            }
            callAcceptLayout.setPadding(callPadding)
        } else {
            val typeCall = convViewHolder.mHistTxt ?: return
            val callInfoLayout = convViewHolder.mCallInfoLayout ?: return
            val detailCall = convViewHolder.mHistDetailTxt ?: return
            val callIcon = convViewHolder.mIcon ?: return

            val typeCallTxt: String

            callInfoLayout.background = ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
            callInfoLayout.setPadding(callPadding)
            // Manage background to convColor if it is outgoing and not missed.
            if (convColor != 0 && !call.isIncoming) {
                callInfoLayout.background.setTint(convColor)
            } else {
                callInfoLayout.background.setTintList(null)
            }
            // Add the call duration if not null.
            detailCall.text = if (call.duration != 0L) {
                String.format(
                        context.getString(R.string.call_duration),
                        DateUtils.formatElapsedTime(null, call.duration!! / 1000)
                ).let { " - $it" }
            } else null

            // After a call, a message is displayed with call information.
            // Manage the call message layout.
            if (call.isIncoming) {
                // Set the color of the time duration.
                detailCall.setTextColor(context.getColor(R.color.colorOnSurface))

                // Set the call message color.
                typeCall.setTextColor(context.getColor(R.color.colorOnSurface))

                if (call.isMissed) { // Call incoming missed.
                    callIcon.setImageResource(R.drawable.baseline_missed_call_16)
                    // Set the drawable color to red because it is missed.
                    callIcon.drawable.setTint(context.getColor(R.color.call_missed))
                    typeCallTxt = context.getString(R.string.notif_missed_incoming_call)
                } else { // Call incoming not missed.
                    callIcon.setImageResource(R.drawable.baseline_incoming_call_16)
                    callIcon.drawable.setTint(context.getColor(R.color.colorOnSurface))
                    typeCallTxt = context.getString(R.string.notif_incoming_call)
                }
            } else {
                // Set the call message color.
                typeCall.setTextColor(context.getColor(R.color.call_text_outgoing_message))

                // Set the color of the time duration.
                detailCall.setTextColor(context.getColor(R.color.call_text_outgoing_message))

                if (call.isMissed) { // Outgoing call missed.
                    callIcon.setImageResource(R.drawable.baseline_missed_call_16)
                    // Set the drawable color to red because it is missed.
                    callIcon.drawable.setTint(context.getColor(R.color.call_missed))
                    typeCallTxt = context.getString(R.string.notif_missed_outgoing_call)
                    // Flip the photo upside down to show a "missed outgoing call".
                    callIcon.scaleX = -1f
                } else { // Outgoing call not missed.
                    callIcon.setImageResource(R.drawable.baseline_outgoing_call_16)
                    callIcon.drawable.setTint(context.getColor(R.color.call_drawable_color))
                    typeCallTxt = context.getString(R.string.notif_outgoing_call)
                }
            }

            typeCall.text = typeCallTxt
        }
    }

    private fun configureSearchResult(convViewHolder: ConversationViewHolder, interaction: Interaction) {
        disableClicking(convViewHolder.itemView)
        val messageId = interaction.messageId ?: return
        val clickView = convViewHolder.primaryClickableView ?: convViewHolder.itemView
        clickView.setOnClickListener { conversationFragment.goToSearchMessage(messageId) }
    }

    private fun disableClicking(v: View) {
        v.isClickable = false
        v.isLongClickable = false
        v.isFocusable = false
        if (v is ViewGroup) {
            v.children.forEach {
                disableClicking(it)
            }
        }
    }

    /**
     * Helper method to return the previous Interaction relative to an initial position.
     * @param position The initial position
     * @return the previous Interaction if any, null otherwise
     */
    private fun getPreviousInteractionFromPosition(position: Int): Interaction? {
        val startIndex = if (position > mInteractions.size) mInteractions.size - 1 else position
        for (i in startIndex - 1 downTo 0) {
            if (mInteractions[i].type != Interaction.InteractionType.INVALID) {
                return mInteractions[i]
            }
        }
        return null
    }

    /**
     * Helper method to return the next Interaction relative to an initial position.
     * @param position The initial position
     * @return the next Interaction if any, null otherwise
     */
    private fun getNextInteractionFromPosition(position: Int): Interaction? {
        val startIndex = if (position < 0) 0 else position
        for (i in startIndex + 1 ..< mInteractions.size) {
            if (mInteractions[i].type != Interaction.InteractionType.INVALID) {
                return mInteractions[i]
            }
        }
        return null
    }

    /**
     * Returns a SequenceType object which tell what type is the Interaction.
     * @param position             index of the interaction to analyse from interactions array.
     * @param isTimeShown   meta data of the interaction telling if the time is shown.
     * @return              the SequenceType of the analyzed interaction.
     */
    private fun getMsgSequencing(position: Int, isTimeShown: Boolean): SequenceType {
        val msg = mInteractions[position]

        // Manage specific interaction which are always single (ex : emoji).
        if (isAlwaysSingleMsg(msg)) {
            return SequenceType.SINGLE
        }

        val prevMsg = getPreviousInteractionFromPosition(position)
        val nextMsg = getNextInteractionFromPosition(position)

        // Check for extremes (first or last or only one interaction).
        if (prevMsg == null) {
            // Get the next interaction (if null it means there is only one interaction).
            if (nextMsg == null)
                return SequenceType.SINGLE
            // Check if sequence break needed.
            return if (isSeqBreak(msg, nextMsg) || hasPermanentDateString(nextMsg, position + 1))
                SequenceType.SINGLE
            else SequenceType.FIRST
        } else if (nextMsg == null) {
            // Get the previous interaction and if exists check if sequence break needed.
            return if (isSeqBreak(prevMsg, msg) || isTimeShown)
                SequenceType.SINGLE
            else SequenceType.LAST
        }

        // If not the first, nor the last and if there is not only one interaction.
        // Get the next and previous interactions and if exists check if sequence break needed.
        val nextMsgHasDate = hasPermanentDateString(nextMsg, position + 1)
        return if ((isSeqBreak(prevMsg, msg) || isTimeShown) && !(isSeqBreak(msg, nextMsg) || nextMsgHasDate)) {
            SequenceType.FIRST
        } else if (!isSeqBreak(prevMsg, msg) && !isTimeShown && isSeqBreak(msg, nextMsg)) {
            SequenceType.LAST
        } else if (!isSeqBreak(prevMsg, msg) && !isTimeShown && !isSeqBreak(msg, nextMsg)) {
            if (nextMsgHasDate) SequenceType.LAST else SequenceType.MIDDLE
        } else {
            SequenceType.SINGLE
        }
    }

    // Used to show the date between messages.
    private fun hasPermanentDateString(message: Interaction, position: Int): Boolean {
        val previousMessageTimestamp =
            getPreviousInteractionFromPosition(position)?.timestamp ?: return false

        // Create Calendar instances for each timestamp
        val calendar1 = Calendar.getInstance().apply { timeInMillis = message.timestamp }
        val calendar2 = Calendar.getInstance().apply { timeInMillis = previousMessageTimestamp }

        // Compare the year, month, and day of year to check if they are different days
        return calendar1.get(Calendar.YEAR) != calendar2.get(Calendar.YEAR)
                || calendar1.get(Calendar.DAY_OF_YEAR) != calendar2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateSuccess(view: ConversationViewHolder) {
        if(view.mStatusIcon == null)
            return
        if(successView?.mStatusIcon?.iconState == MessageStatusView.IconState.SUCCESS)
            successView?.mStatusIcon?.updateNone()
        view.mStatusIcon.updateSuccess()
        successView = view
    }

    private enum class SequenceType { FIRST, MIDDLE, LAST, SINGLE }

    companion object {
        private val TAG = ConversationAdapter::class.simpleName!!
        private val msgBGLayouts = intArrayOf(
            R.drawable.textmsg_bg_out_first,
            R.drawable.textmsg_bg_out_middle,
            R.drawable.textmsg_bg_out_last,
            R.drawable.textmsg_bg_out,
            R.drawable.textmsg_bg_in_first,
            R.drawable.textmsg_bg_in_middle,
            R.drawable.textmsg_bg_in_last,
            R.drawable.textmsg_bg_in,
            R.drawable.textmsg_bg_out_reply,
            R.drawable.textmsg_bg_out_reply_first,
            R.drawable.textmsg_bg_in_reply,
            R.drawable.textmsg_bg_in_reply_first
        )

        /**
         * Tells if a break should be added in the sequence.
         * The first interaction must be before the second interaction.
         *
         * @param first     first interaction
         * @param second    second interaction
         * @return          True if a sequence break is needed. Else false.
         */
        private fun isSeqBreak(first: Interaction, second: Interaction): Boolean =
            StringUtils.isOnlyEmoji(first.body) != StringUtils.isOnlyEmoji(second.body)
                    || first.isIncoming != second.isIncoming
                    || ((first.type !== Interaction.InteractionType.TEXT) && (first.type !== Interaction.InteractionType.CALL))
                    || ((second.type !== Interaction.InteractionType.TEXT) && (second.type !== Interaction.InteractionType.CALL))
                    || second.replyTo != null
                    || first.contact != second.contact
                    || (second.timestamp - first.timestamp) > (10 * DateUtils.MINUTE_IN_MILLIS)

        private fun isAlwaysSingleMsg(msg: Interaction): Boolean =
            ((msg.type !== Interaction.InteractionType.TEXT && msg.type !== Interaction.InteractionType.CALL)
                    || StringUtils.isOnlyEmoji(msg.body))
    }

}