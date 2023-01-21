/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Authors:    Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
package cx.ring.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.TextureView.SurfaceTextureListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
import androidx.core.text.bold
import androidx.core.view.isVisible
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
import cx.ring.utils.ContentUriHandler.getUriForFile
import cx.ring.viewholders.ConversationViewHolder
import cx.ring.views.AvatarDrawable
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.conversation.ConversationPresenter
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.model.Interaction.InteractionStatus
import net.jami.utils.StringUtils
import java.io.File
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max


class ConversationAdapter(
    private val conversationFragment: ConversationFragment,
    private val presenter: ConversationPresenter
) : RecyclerView.Adapter<ConversationViewHolder>() {
    private val mInteractions = ArrayList<Interaction>()
    private val res = conversationFragment.resources
    private val hPadding = res.getDimensionPixelSize(R.dimen.padding_medium)
    private val vPadding = res.getDimensionPixelSize(R.dimen.padding_small)
    private val mPictureMaxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f, res.displayMetrics).toInt()
    private val mPreviewMaxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, res.displayMetrics).toInt()
    private var currentSelectionId: String? = null
    private var mCurrentLongItem: RecyclerViewContextMenuInfo? = null
    @ColorInt private var convColor = 0
    @ColorInt private var convColorTint = 0
    private var expandedItemPosition = -1
    private var lastDeliveredPosition = -1
    private val timestampUpdateTimer: Observable<Long> = Observable.interval(10, TimeUnit.SECONDS, DeviceUtils.uiScheduler)
        .startWithItem(0L)
    private var lastMsgPos = -1
    private var isComposing = false
    private var mShowReadIndicator = true
    var showLinkPreviews = true
    private val markwon: Markwon = Markwon.builder(conversationFragment.requireContext())
        .usePlugin(LinkifyPlugin.create())
        .build()

    /**
     * Refreshes the data and notifies the changes
     *
     * @param list an arraylist of interactions
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateDataset(list: List<Interaction>) {
        Log.d(TAG, "updateDataset: list size=" + list.size)
        when {
            mInteractions.isEmpty() -> {
                mInteractions.addAll(list)
                notifyDataSetChanged()
            }
            list.size > mInteractions.size -> {
                val oldSize = mInteractions.size
                mInteractions.addAll(list.subList(oldSize, list.size))
                notifyItemRangeInserted(oldSize, list.size)
            }
            else -> {
                mInteractions.clear()
                mInteractions.addAll(list)
                notifyDataSetChanged()
            }
        }
    }

    fun add(e: Interaction): Boolean {
        if (e.isSwarm) {
            if (mInteractions.isEmpty() || mInteractions[mInteractions.size - 1].messageId == e.parentId) {
                val update = mInteractions.isNotEmpty()
                mInteractions.add(e)
                notifyItemInserted(mInteractions.size - 1)
                if (update) notifyItemChanged(mInteractions.size - 2)
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

    fun update(e: Interaction) {
        Log.w(TAG, "update " + e.messageId)
        if (!e.isIncoming && e.status == InteractionStatus.SUCCESS) {
            notifyItemChanged(lastDeliveredPosition)
        }
        for (i in mInteractions.indices.reversed()) {
            val element = mInteractions[i]
            if (e === element) {
                notifyItemChanged(i)
                break
            }
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

    override fun getItemViewType(position: Int): Int {
        if (isComposing && position == mInteractions.size) return MessageType.COMPOSING_INDICATION.ordinal
        val interaction = mInteractions[position]
        return when (interaction.type) {
            Interaction.InteractionType.CONTACT -> MessageType.CONTACT_EVENT.ordinal
            Interaction.InteractionType.CALL -> MessageType.CALL_INFORMATION.ordinal
            Interaction.InteractionType.TEXT -> if (interaction.isIncoming) {
                MessageType.INCOMING_TEXT_MESSAGE.ordinal
            } else {
                MessageType.OUTGOING_TEXT_MESSAGE.ordinal
            }
            Interaction.InteractionType.DATA_TRANSFER -> {
                val file = interaction as DataTransfer
                val out = if (interaction.isIncoming) 0 else 4
                if (file.isComplete) {
                    when {
                        file.isPicture -> return MessageType.INCOMING_IMAGE.ordinal + out
                        file.isAudio -> return MessageType.INCOMING_AUDIO.ordinal + out
                        file.isVideo -> return MessageType.INCOMING_VIDEO.ordinal + out
                    }
                }
                out
            }
            Interaction.InteractionType.INVALID -> MessageType.INVALID.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val type = MessageType.values()[viewType]
        val v = if (type == MessageType.INVALID) FrameLayout(parent.context)
        else (LayoutInflater.from(parent.context).inflate(type.layout, parent, false) as ViewGroup)
        return ConversationViewHolder(v, type)
    }

    private fun configureDisplayIndicator(conversationViewHolder: ConversationViewHolder, interaction: Interaction) {
        val conversation = interaction.conversation
        if (conversation == null || conversation !is Conversation) {
            conversationViewHolder.mStatusIcon?.isVisible = false
            return
        }
        conversationViewHolder.compositeDisposable.add(presenter.conversationFacade
            .getLoadedContact(interaction.account!!, conversation, interaction.displayedContacts)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { contacts ->
                conversationViewHolder.mStatusIcon?.isVisible = contacts.isNotEmpty()
                conversationViewHolder.mStatusIcon?.update(contacts, interaction.status, conversationViewHolder.mMsgTxt?.id ?: View.NO_ID)
            })
    }

    private fun configureReactions(
        conversationViewHolder: ConversationViewHolder,
        interaction: Interaction
    ) {
        conversationViewHolder.reactionChip?.setOnClickListener {
            presenter.removeReaction(interaction)
        }
        conversationViewHolder.compositeDisposable.add(interaction.reactionObservable
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { reactions ->
                Log.w(TAG, "reaction $reactions")
                val chip = conversationViewHolder.reactionChip ?: return@subscribe
                if (reactions.isEmpty())
                    chip.isVisible = false
                else {
                    chip.text = reactions.filterIsInstance<TextMessage>()
                        .groupingBy { it.body!! }
                        .eachCount()
                        .map { it }
                        .sortedByDescending { it.value }
                        .joinToString("") { if (it.value > 1) it.key + it.value else it.key }
                    chip.isVisible = true
                    chip.isClickable = true
                    chip.isFocusable = true
                }
            })
    }

    private fun configureReplyIndicator(conversationViewHolder: ConversationViewHolder, interaction: Interaction) {
        val conversation = interaction.conversation
        if (conversation == null || conversation !is Conversation) {
            conversationViewHolder.mReplyTo?.isVisible = false
            return
        }
        conversationViewHolder.mReplyTo?.let { replyView ->
            val replyTo = interaction.replyTo
            if (replyTo != null)  {
                conversationViewHolder.compositeDisposable.add(replyTo
                    .flatMapObservable { reply -> presenter.contactService
                        .observeContact(interaction.account!!, reply.contact!!, false)
                        .map { contact -> Pair(reply, contact) }}
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ i ->
                        replyView.text = SpannableStringBuilder()
                            .bold {
                                append(i.second.displayName)
                                append(": ")
                            }
                            .append(i.first.body)
                        replyView.isVisible = true
                    }) { replyView.isVisible = false })
            } else {
                replyView.isVisible = false
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
        /*if (position > lastMsgPos) {
            lastMsgPos = position
            val animation = AnimationUtils.loadAnimation(conversationViewHolder.itemView.context, R.anim.fade_in)
            animation.startOffset = 150
            conversationViewHolder.itemView.startAnimation(animation)
        }*/

        conversationViewHolder.mStatusIcon?.let { configureDisplayIndicator(conversationViewHolder, interaction) }
        conversationViewHolder.mReplyTo?.let { configureReplyIndicator(conversationViewHolder, interaction) }
        conversationViewHolder.reactionChip?.let { configureReactions(conversationViewHolder, interaction) }

        val type = interaction.type
        //Log.w(TAG, "onBindViewHolder $type $interaction");
        if (type == Interaction.InteractionType.INVALID) {
            conversationViewHolder.itemView.visibility = View.GONE
        } else {
            conversationViewHolder.itemView.visibility = View.VISIBLE
            when (type) {
                Interaction.InteractionType.TEXT -> configureForTextMessage(conversationViewHolder, interaction, position)
                Interaction.InteractionType.CALL -> configureForCallInfo(conversationViewHolder, interaction)
                Interaction.InteractionType.CONTACT -> configureForContactEvent(conversationViewHolder, interaction)
                Interaction.InteractionType.DATA_TRANSFER -> configureForFileInfo(conversationViewHolder, interaction, position)
                else -> {}
            }
        }
    }

    override fun onViewRecycled(holder: ConversationViewHolder) {
        holder.itemView.setOnLongClickListener(null)
        if (holder.mImage != null) {
            holder.mImage.setOnLongClickListener(null)
        }
        if (holder.video != null) {
            holder.video.setOnClickListener(null)
            holder.video.surfaceTextureListener = null
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
        if (expandedItemPosition == holder.layoutPosition) {
            holder.mMsgDetailTxt?.visibility = View.GONE
            expandedItemPosition = -1
        }
        holder.compositeDisposable.clear()
    }

    fun setPrimaryColor(@ColorInt color: Int) {
        convColor = color
        convColorTint = MaterialColors.compositeARGBWithAlpha(color, (MaterialColors.ALPHA_LOW * 255).toInt())
        notifyDataSetChanged()
    }

    fun setComposingStatus(composingStatus: ComposingStatus) {
        val composing = composingStatus == ComposingStatus.Active
        if (isComposing != composing) {
            isComposing = composing
            if (composing) notifyItemInserted(mInteractions.size) else notifyItemRemoved(
                mInteractions.size
            )
        }
    }

    fun setReadIndicatorStatus(show: Boolean) {
        mShowReadIndicator = show
    }

    private class RecyclerViewContextMenuInfo(
        val position: Int,
        val id: Long
    ) : ContextMenuInfo

    fun onContextItemSelected(item: MenuItem): Boolean {
        val info = mCurrentLongItem ?: return false
        val interaction = try {
            mInteractions[info.position]
        } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, "Interaction array may be empty or null", e)
            return false
        }
        if (interaction.type == Interaction.InteractionType.CONTACT) return false
        when (item.itemId) {
            R.id.conv_action_download -> presenter.saveFile(interaction)
            R.id.conv_action_share -> presenter.shareFile(interaction as DataTransfer)
            R.id.conv_action_open -> presenter.openFile(interaction)
            R.id.conv_action_delete -> presenter.deleteConversationItem(interaction)
            R.id.conv_action_cancel_message -> presenter.cancelMessage(interaction)
            R.id.conv_action_reply -> presenter.startReplyTo(interaction)
            R.id.conv_action_copy_text -> addToClipboard(interaction.body)
        }
        return true
    }

    private fun addToClipboard(text: String?) {
        if (text.isNullOrEmpty()) return
        val clipboard = conversationFragment.requireActivity()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Copied Message", text))
    }

    private fun configureImage(viewHolder: ConversationViewHolder, path: File, displayName: String?) {
        val context = viewHolder.itemView.context
        val image = viewHolder.mImage ?: return
        image.clipToOutline = true
        Glide.with(context)
            .load(path)
            .transition(withCrossFade())
            .into(image)
        image.setOnClickListener { v: View ->
            try {
                val contentUri = getUriForFile(v.context, ContentUriHandler.AUTHORITY_FILES, path, displayName)
                val i = Intent(context, MediaViewerActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, "image/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(conversationFragment.requireActivity(), viewHolder.mImage, "picture")
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
            val player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path))
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
                            viewHolder.mMsgTxt?.text = String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d", pS / 60, pS % 60, dS / 60, dS % 60)
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
        val player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path)) ?: return
        viewHolder.player = player
        val playBtn = ContextCompat.getDrawable(cardLayout.context, R.drawable.baseline_play_arrow_24)!!.mutate()
        DrawableCompat.setTint(playBtn, Color.WHITE)
        cardLayout.foreground = playBtn
        player.setOnCompletionListener { mp: MediaPlayer ->
            if (mp.isPlaying) mp.pause()
            mp.seekTo(1)
            cardLayout.foreground = playBtn
        }

        player.setOnVideoSizeChangedListener { mp: MediaPlayer, width: Int, height: Int ->
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
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
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

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

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

    private fun openItemMenu(cvh: ConversationViewHolder, v: View, interaction: Interaction) {
        MenuConversationBinding.inflate(LayoutInflater.from(v.context)).apply {
            convActionOpenText.isVisible = interaction is DataTransfer && interaction.isComplete
            convActionDownloadText.isVisible = interaction is DataTransfer && interaction.isComplete
            convActionEdit.isVisible = !interaction.isIncoming
            root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupWindow = WeakReference(PopupWindow(root, LinearLayout.LayoutParams.WRAP_CONTENT, root.measuredHeight, true).apply {
                setOnDismissListener {
                    if (convColor != 0
                        && interaction.type == Interaction.InteractionType.TEXT
                        && !interaction.isIncoming
                    ) {
                        v.background.setTint(convColor)
                    } else {
                        v.background.setTintList(null)
                    }
                }
                elevation = v.context.resources.getDimension(R.dimen.call_preview_elevation)
                showAsDropDown(v)
            })
            val emojiCallback = View.OnClickListener { view ->
                presenter.sendReaction(interaction, (view as TextView).text)
                popupWindow.get()?.dismiss()
            }
            convActionEmoji1.setOnClickListener(emojiCallback)
            convActionEmoji2.setOnClickListener(emojiCallback)
            convActionEmoji3.setOnClickListener(emojiCallback)
            convActionEmoji4.setOnClickListener(emojiCallback)
            convActionReply.setOnClickListener {
                presenter.startReplyTo(interaction)
                popupWindow.get()?.dismiss()
            }
            convActionMore.setOnClickListener {
                menuActions.isVisible = !menuActions.isVisible
            }
            convActionOpenText.setOnClickListener {
                presenter.openFile(interaction)
            }
            convActionDownloadText.setOnClickListener {
                presenter.saveFile(interaction)
            }
            convActionCopyText.setOnClickListener {
                addToClipboard(interaction.body)
                popupWindow.get()?.dismiss()
            }
            if (!interaction.isIncoming) {
                convActionEdit.setOnClickListener {
                    popupWindow.get()?.dismiss()
                    try {
                        val i = Intent(it.context, MessageEditActivity::class.java)
                            .setData(Uri.withAppendedPath(ConversationPath.toUri(interaction.account!!, interaction.conversationId!!), interaction.messageId))
                            .setAction(Intent.ACTION_EDIT)
                            .putExtra(Intent.EXTRA_TEXT, cvh.mMsgTxt!!.text.toString())
                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(conversationFragment.requireActivity(), cvh.mMsgTxt!!, "messageEdit")
                        conversationFragment.startActivityForResult(i, ConversationFragment.REQUEST_CODE_EDIT_MESSAGE, options.toBundle())
                    } catch (e: Exception) {
                        Log.w(TAG, "Can't open picture", e)
                    }
                }
            } else {
                convActionEdit.setOnClickListener(null)
            }
            convActionShare.setOnClickListener {
                if (interaction is DataTransfer)
                    presenter.shareFile(interaction)
                else if (interaction is TextMessage)
                    presenter.shareText(interaction)
                popupWindow.get()?.dismiss()
            }
            convActionHistory.setOnClickListener {
                Log.w(TAG, "Message history")
                cvh.compositeDisposable.add(interaction.historyObservable.firstOrError().subscribe { c ->
                    Log.w(TAG, "Message history ${c.size}")
                    c.forEach {
                        Log.w(TAG, "Message ${it.type} ${it.body} $it")
                    }
                    MaterialAlertDialogBuilder(it.context)
                        .setTitle("Message history")
                        .setItems(c.filterIsInstance<TextMessage>().map { it.body!! }.toTypedArray())
                        { dialog, which -> dialog.dismiss() }
                        .create()
                        .show()
                })
                popupWindow.get()?.dismiss()
            }
        }
    }

    @SuppressLint("RestrictedApi", "ClickableViewAccessibility")
    private fun configureForFileInfo(viewHolder: ConversationViewHolder, interaction: Interaction, position: Int) {
        val file = interaction as DataTransfer
        val path = presenter.deviceRuntimeService.getConversationPath(file)
        val timeString = TextUtils.timestampToDetailString(viewHolder.itemView.context, file.timestamp)
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
            viewHolder.mMsgDetailTxt?.text = when (val status = file.status) {
                InteractionStatus.TRANSFER_FINISHED -> String.format("%s - %s", timeString,
                    Formatter.formatFileSize(viewHolder.itemView.context, file.totalSize))
                InteractionStatus.TRANSFER_ONGOING -> String.format("%s / %s - %s",
                    Formatter.formatFileSize(viewHolder.itemView.context, file.bytesProgress),
                    Formatter.formatFileSize(viewHolder.itemView.context, file.totalSize),
                    TextUtils.getReadableFileTransferStatus(viewHolder.itemView.context, status))
                else -> String.format("%s - %s - %s", timeString,
                    Formatter.formatFileSize(viewHolder.itemView.context, file.totalSize),
                    TextUtils.getReadableFileTransferStatus(viewHolder.itemView.context, status))
            }
        })
        if (hasPermanentTimeString(file, position)) {
            viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                viewHolder.mMsgDetailTxtPerm?.text = TextUtils.timestampToDetailString(viewHolder.itemView.context, file.timestamp)
            })
            viewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
        } else {
            viewHolder.mMsgDetailTxtPerm?.visibility = View.GONE
        }
        val contact = interaction.contact
        if (interaction.isIncoming) {
            viewHolder.mAvatar?.let { avatar ->
                avatar.setImageBitmap(null)
                avatar.visibility = View.VISIBLE
                if (contact != null)
                    avatar.setImageDrawable(conversationFragment.getConversationAvatar(contact.primaryNumber))
            }
        }
        val type = viewHolder.type.transferType
        val longPressView = when (type) {
            MessageType.TransferType.IMAGE -> viewHolder.mImage
            MessageType.TransferType.VIDEO -> viewHolder.video
            MessageType.TransferType.AUDIO -> viewHolder.mAudioInfoLayout
            else -> viewHolder.mFileInfoLayout
        } ?: return
        if (type == MessageType.TransferType.AUDIO || type == MessageType.TransferType.FILE) {
            longPressView.background.setTintList(null)
        }
        longPressView.setOnLongClickListener { v: View ->
            if (type == MessageType.TransferType.AUDIO || type == MessageType.TransferType.FILE) {
                conversationFragment.updatePosition(viewHolder.bindingAdapterPosition)
                longPressView.background.setTint(res.getColor(R.color.grey_500))
            }
            openItemMenu(viewHolder,v, file)
            mCurrentLongItem = RecyclerViewContextMenuInfo(viewHolder.bindingAdapterPosition, v.id.toLong())
            true
        }
        if (type == MessageType.TransferType.IMAGE) {
            configureImage(viewHolder, path, file.body)
        } else if (type == MessageType.TransferType.VIDEO) {
            configureVideo(viewHolder, path)
        } else if (type == MessageType.TransferType.AUDIO) {
            configureAudio(viewHolder, path)
        } else {
            val status = file.status
            viewHolder.mIcon?.setImageResource(if (status.isError) R.drawable.baseline_warning_24 else R.drawable.baseline_attach_file_24)
            viewHolder.mMsgTxt?.text = file.displayName
            if (status == InteractionStatus.TRANSFER_AWAITING_HOST) {
                viewHolder.btnRefuse?.visibility = View.VISIBLE
                viewHolder.mAnswerLayout?.visibility = View.VISIBLE
                viewHolder.btnAccept?.setOnClickListener { presenter.acceptFile(file) }
                viewHolder.btnRefuse?.setOnClickListener { presenter.refuseFile(file) }
            } else if (status == InteractionStatus.FILE_AVAILABLE) {
                viewHolder.btnRefuse?.visibility = View.GONE
                viewHolder.mAnswerLayout?.visibility = View.VISIBLE
                viewHolder.btnAccept?.setOnClickListener { presenter.acceptFile(file) }
            } else {
                viewHolder.mAnswerLayout?.visibility = View.GONE
                if (status == InteractionStatus.TRANSFER_ONGOING) {
                    viewHolder.progress?.max = (file.totalSize / 1024).toInt()
                    viewHolder.progress?.setProgress((file.bytesProgress / 1024).toInt(), true)
                    viewHolder.progress?.show()
                } else {
                    viewHolder.progress?.hide()
                }
            }
        }
    }

    private fun configureForTypingIndicator(viewHolder: ConversationViewHolder) {
        AnimatedVectorDrawableCompat.create(viewHolder.itemView.context, R.drawable.typing_indicator_animation)?.let { anim ->
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
     * Configures the viewholder to display a classic text message, ie. not a call info text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     * @param position       The position of the viewHolder
     */
    private fun configureForTextMessage(convViewHolder: ConversationViewHolder, interaction: Interaction, position: Int) {
        val context = convViewHolder.itemView.context
        convViewHolder.compositeDisposable.add(interaction.lastElement
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { lastElement ->
                val textMessage = lastElement as TextMessage
                val contact = textMessage.contact ?: return@subscribe
                // Log.w(TAG, "configureForTextMessage " + position + " " + interaction.getDaemonId() + " " + interaction.getStatus());
                val message = textMessage.body?.trim() ?: ""//.trim { it <= ' ' }
                val longPressView = convViewHolder.mMsgTxt!!
                longPressView.background.setTintList(null)
                longPressView.setOnLongClickListener { v: View ->
                    openItemMenu(convViewHolder, v, interaction)
                    if (expandedItemPosition == position) {
                        expandedItemPosition = -1
                    }
                    conversationFragment.updatePosition(convViewHolder.bindingAdapterPosition)
                    if (textMessage.isIncoming) {
                        longPressView.background.setTint(res.getColor(R.color.grey_500))
                    } else {
                        longPressView.background.setTint(convColorTint)
                    }
                    mCurrentLongItem = RecyclerViewContextMenuInfo(
                        convViewHolder.bindingAdapterPosition,
                        v.id.toLong()
                    )
                    true
                }
                val isTimeShown = hasPermanentTimeString(textMessage, position)
                val msgSequenceType = getMsgSequencing(position, isTimeShown)
                convViewHolder.mAnswerLayout?.visibility = View.GONE
                val msgTxt = convViewHolder.mMsgTxt ?: return@subscribe
                if (StringUtils.isOnlyEmoji(message)) {
                    msgTxt.background.alpha = 0
                    msgTxt.textSize = 32.0f
                    msgTxt.setPadding(0, 0, 0, 0)
                } else {
                    val resIndex =
                        msgSequenceType.ordinal + (if (textMessage.isIncoming) 1 else 0) * 4
                    msgTxt.background = ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
                    if (convColor != 0 && !textMessage.isIncoming) {
                        msgTxt.background.setTint(convColor)
                    }
                    msgTxt.background.alpha = 255
                    msgTxt.textSize = 16f
                    msgTxt.setPadding(hPadding, vPadding, hPadding, vPadding)

                    if (showLinkPreviews) {
                        val cachedPreview =
                            textMessage.preview as? Maybe<PreviewData>? ?: LinkPreview.getFirstUrl(message)
                                .flatMap { url -> LinkPreview.load(url) }
                                .cache()
                                .apply { interaction.preview = this }

                        convViewHolder.compositeDisposable.add(cachedPreview
                            .observeOn(DeviceUtils.uiScheduler)
                            .subscribe({ data ->
                                Log.w(TAG, "got preview $data")
                                val image = convViewHolder.mImage ?: return@subscribe
                                if (data.imageUrl.isNotEmpty()) {
                                    Glide.with(context)
                                        .load(data.imageUrl)
                                        .centerCrop()
                                        .into(image)
                                    image.visibility = View.VISIBLE
                                } else {
                                    image.visibility = View.GONE
                                }
                                convViewHolder.mHistTxt?.text = data.title
                                if (data.description.isNotEmpty()) {
                                    convViewHolder.mHistDetailTxt?.visibility = View.VISIBLE
                                    convViewHolder.mHistDetailTxt?.text = data.description
                                } else {
                                    convViewHolder.mHistDetailTxt?.visibility = View.GONE
                                }
                                convViewHolder.mAnswerLayout?.visibility = View.VISIBLE
                                val url = Uri.parse(data.baseUrl)
                                convViewHolder.mPreviewDomain?.text = url.host
                                convViewHolder.mAnswerLayout?.setOnClickListener {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, url))
                                }
                            }) { e -> Log.e(TAG, "Can't load preview", e) })
                    }
                }
                msgTxt.movementMethod = LinkMovementMethod.getInstance()
                msgTxt.text = markwon.toMarkdown(message)
                val endOfSeq =
                    msgSequenceType == SequenceType.LAST || msgSequenceType == SequenceType.SINGLE
                if (textMessage.isIncoming) {
                    val avatar = convViewHolder.mAvatar ?: return@subscribe
                    if (endOfSeq) {
                        avatar.setImageDrawable(conversationFragment.getConversationAvatar(contact.primaryNumber))
                        avatar.visibility = View.VISIBLE
                    } else {
                        if (position == lastMsgPos - 1) {
                            avatar.startAnimation(
                                AnimationUtils.loadAnimation(
                                    avatar.context,
                                    R.anim.fade_out
                                ).apply {
                                    setAnimationListener(object : Animation.AnimationListener {
                                        override fun onAnimationStart(arg0: Animation) {}
                                        override fun onAnimationRepeat(arg0: Animation) {}
                                        override fun onAnimationEnd(arg0: Animation) {
                                            avatar.setImageBitmap(null)
                                            avatar.visibility = View.INVISIBLE
                                        }
                                    })
                                })
                        } else {
                            avatar.setImageBitmap(null)
                            avatar.visibility = View.INVISIBLE
                        }
                    }
                }
                setBottomMargin(msgTxt, if (endOfSeq) 8 else 0)
                if (isTimeShown) {
                    convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                        convViewHolder.mMsgDetailTxtPerm?.text =
                            TextUtils.timestampToDetailString(context, textMessage.timestamp)
                    })
                    convViewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
                } else {
                    convViewHolder.mMsgDetailTxtPerm?.visibility = View.GONE
                    val isExpanded = position == expandedItemPosition
                    if (isExpanded) {
                        convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                            convViewHolder.mMsgDetailTxt?.text =
                                TextUtils.timestampToDetailString(context, textMessage.timestamp)
                        })
                    }
                    setItemViewExpansionState(convViewHolder, isExpanded)
                    convViewHolder.mItem?.setOnClickListener {
                        if (convViewHolder.animator?.isRunning == true) {
                            return@setOnClickListener
                        }
                        if (expandedItemPosition >= 0) {
                            val prev = expandedItemPosition
                            notifyItemChanged(prev)
                        }
                        expandedItemPosition = if (isExpanded) -1 else position
                        notifyItemChanged(expandedItemPosition)
                    }
                }
            })
    }


    private fun configureForContactEvent(viewHolder: ConversationViewHolder, interaction: Interaction) {
        val event = interaction as ContactEvent
        Log.w(TAG, "configureForContactEvent ${event.account} ${event.event} ${event.contact} ${event.author} ")
        viewHolder.mMsgDetailTxt?.text = TextUtils.timestampToDetailString(viewHolder.itemView.context, event.timestamp)

        if (interaction.isSwarm) {
            viewHolder.compositeDisposable.add(
                presenter.contactService.observeContact(event.account!!, event.contact!!, false)
                    .observeOn(DeviceUtils.uiScheduler)
                .subscribe { vm ->
                    viewHolder.mImage?.setImageDrawable(AvatarDrawable.Builder()
                        .withContact(vm)
                        .withCircleCrop(true)
                        .build(viewHolder.itemView.context))
                    viewHolder.mMsgTxt?.text = viewHolder.itemView.context.getString(when (event.event) {
                        ContactEvent.Event.ADDED -> R.string.conversation_contact_added
                        ContactEvent.Event.INVITED -> R.string.conversation_contact_invited
                        ContactEvent.Event.REMOVED -> R.string.conversation_contact_left
                        ContactEvent.Event.BANNED -> R.string.conversation_contact_banned
                        else -> R.string.hist_contact_added
                    }, vm.displayName)
                })
            /*viewHolder.compositeDisposable.add(Observable.combineLatest(
                presenter.contactService.observeContact(event.account!!, event.contact!!, false),
                presenter.contactService.observeContact(event.account!!, Uri(null, event.author!!), false)
            ) { target, author -> Pair(target, author) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm ->
                    viewHolder.mImage?.setImageDrawable(AvatarDrawable.Builder()
                        .withContact(vm.first)
                        .withCircleCrop(true)
                        .build(viewHolder.itemView.context))
                    viewHolder.mMsgTxt?.text = viewHolder.itemView.context.getString(when (event.event) {
                        ContactEvent.Event.ADDED -> R.string.conversation_contact_added
                        ContactEvent.Event.INVITED -> R.string.conversation_contact_invited
                        ContactEvent.Event.REMOVED -> R.string.conversation_contact_left
                        ContactEvent.Event.BANNED -> R.string.conversation_contact_banned
                        else -> R.string.hist_contact_added
                    }, vm.first.displayName)
                })*/
        } else {
            viewHolder.mImage?.setImageResource(R.drawable.baseline_person_24)
            viewHolder.mMsgTxt?.setText(when (event.event) {
                ContactEvent.Event.ADDED -> R.string.hist_contact_added
                ContactEvent.Event.INVITED -> R.string.hist_contact_invited
                ContactEvent.Event.REMOVED -> R.string.hist_contact_left
                ContactEvent.Event.BANNED -> R.string.hist_contact_banned
                ContactEvent.Event.INCOMING_REQUEST -> R.string.hist_invitation_received
                else -> R.string.hist_contact_added
            })
        }
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     */
    private fun configureForCallInfo(convViewHolder: ConversationViewHolder, interaction: Interaction) {
        convViewHolder.mIcon?.scaleY = 1f
        val context = convViewHolder.itemView.context
        if (!interaction.isSwarm) {
            convViewHolder.mCallInfoLayout?.apply {
                background.setTintList(null)
                setOnCreateContextMenuListener { menu: ContextMenu, v: View, menuInfo: ContextMenuInfo? ->
                    conversationFragment.onCreateContextMenu(menu, v, menuInfo)
                    val inflater = conversationFragment.requireActivity().menuInflater
                    inflater.inflate(R.menu.conversation_item_actions_messages, menu)
                    menu.findItem(R.id.conv_action_delete).setTitle(R.string.menu_delete)
                    menu.removeItem(R.id.conv_action_cancel_message)
                    menu.removeItem(R.id.conv_action_copy_text)
                }
                setOnLongClickListener { v: View ->
                    background.setTint(res.getColor(R.color.grey_500))
                    conversationFragment.updatePosition(convViewHolder.adapterPosition)
                    mCurrentLongItem = RecyclerViewContextMenuInfo(convViewHolder.adapterPosition, v.id.toLong())
                    false
                }
            }
        }
        val pictureResID: Int
        val historyTxt: String
        val call = interaction as Call
        if (call.isMissed) {
            if (call.isIncoming) {
                pictureResID = R.drawable.baseline_call_missed_24
            } else {
                pictureResID = R.drawable.baseline_call_missed_outgoing_24
                // Flip the photo upside down to show a "missed outgoing call"
                convViewHolder.mIcon?.scaleY = -1f
            }
            historyTxt = if (call.isIncoming)
                context.getString(R.string.notif_missed_incoming_call)
            else
                context.getString(R.string.notif_missed_outgoing_call)
        } else {
            pictureResID = if (call.isIncoming) R.drawable.baseline_call_received_24 else R.drawable.baseline_call_made_24
            historyTxt = if (call.isIncoming) context.getString(R.string.notif_incoming_call) else context.getString(R.string.notif_outgoing_call)
        }
        convViewHolder.mIcon?.setImageResource(pictureResID)
        convViewHolder.mHistTxt?.text = historyTxt
        convViewHolder.mHistDetailTxt?.text = DateFormat.getDateTimeInstance()
            .format(call.timestamp) // start date
    }

    /**
     * Helper method to return the previous TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the previous TextMessage if any, null otherwise
     */
    private fun getPreviousMessageFromPosition(position: Int): Interaction? =
        if (mInteractions.isNotEmpty() && position > 0) mInteractions[position - 1]
        else null

    /**
     * Helper method to return the next TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the next TextMessage if any, null otherwise
     */
    private fun getNextMessageFromPosition(position: Int): Interaction? =
        if (mInteractions.isNotEmpty() && position < mInteractions.size - 1)
            mInteractions[position + 1]
        else null

    private fun getMsgSequencing(i: Int, isTimeShown: Boolean): SequenceType {
        val msg = mInteractions[i]
        if (isAlwaysSingleMsg(msg)) {
            return SequenceType.SINGLE
        }
        if (mInteractions.size == 1 || i == 0) {
            if (mInteractions.size == i + 1) {
                return SequenceType.SINGLE
            }
            val nextMsg = getNextMessageFromPosition(i)
            if (nextMsg != null) {
                return if (isSeqBreak(msg, nextMsg) || hasPermanentTimeString(nextMsg, i + 1)) {
                    SequenceType.SINGLE
                } else {
                    SequenceType.FIRST
                }
            }
        } else if (mInteractions.size == i + 1) {
            val prevMsg = getPreviousMessageFromPosition(i)
            if (prevMsg != null) {
                return if (isSeqBreak(msg, prevMsg) || isTimeShown) {
                    SequenceType.SINGLE
                } else {
                    SequenceType.LAST
                }
            }
        }
        val prevMsg = getPreviousMessageFromPosition(i)
        val nextMsg = getNextMessageFromPosition(i)
        if (prevMsg != null && nextMsg != null) {
            val nextMsgHasTime = hasPermanentTimeString(nextMsg, i + 1)
            return if ((isSeqBreak(msg, prevMsg) || isTimeShown) && !(isSeqBreak(msg, nextMsg) || nextMsgHasTime)) {
                 SequenceType.FIRST
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && isSeqBreak(msg, nextMsg)) {
                SequenceType.LAST
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && !isSeqBreak(msg, nextMsg)) {
                if (nextMsgHasTime) SequenceType.LAST else SequenceType.MIDDLE
            } else {
                SequenceType.SINGLE
            }
        }
        return SequenceType.SINGLE
    }

    private fun setItemViewExpansionState(viewHolder: ConversationViewHolder, expanded: Boolean) {
        val view: View = viewHolder.mMsgDetailTxt ?: return
        if (view.height == 0 && !expanded) return
        (viewHolder.animator ?: ValueAnimator().apply {
            duration = 200
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val va = animation as ValueAnimator
                    if (va.animatedValue as Int == 0) {
                        view.visibility = View.GONE
                    }
                    viewHolder.animator = null
                }
            })
            addUpdateListener { animation: ValueAnimator ->
                view.layoutParams.height = (animation.animatedValue as Int)
                view.requestLayout()
            }
            viewHolder.animator = this
        }).apply {
            if (isRunning) {
                reverse()
            } else {
                view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                setIntValues(0, view.measuredHeight)
            }
            if (expanded) {
                view.visibility = View.VISIBLE
                start()
            } else {
                reverse()
            }
        }
    }

    private fun hasPermanentTimeString(msg: Interaction, position: Int): Boolean {
        val prevMsg = getPreviousMessageFromPosition(position)
        return prevMsg != null && msg.timestamp - prevMsg.timestamp > 10 * DateUtils.MINUTE_IN_MILLIS
    }

    private fun lastOutgoingIndex(): Int {
        var i: Int = mInteractions.size - 1
        while (i >= 0) {
            if (!mInteractions[i].isIncoming)
                break
            i--
        }
        return i
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
            R.drawable.textmsg_bg_in
        )

        private fun setBottomMargin(view: View, value: Int) {
            val targetSize = (value * view.context.resources.displayMetrics.density).toInt()
            val params = view.layoutParams as MarginLayoutParams
            params.bottomMargin = targetSize
        }

        private fun isSeqBreak(first: Interaction, second: Interaction): Boolean =
            StringUtils.isOnlyEmoji(first.body) != StringUtils.isOnlyEmoji(second.body) || first.isIncoming != second.isIncoming || first.type !== Interaction.InteractionType.TEXT || second.type !== Interaction.InteractionType.TEXT

        private fun isAlwaysSingleMsg(msg: Interaction): Boolean =
            (msg.type !== Interaction.InteractionType.TEXT
                    || StringUtils.isOnlyEmoji(msg.body))
    }

}