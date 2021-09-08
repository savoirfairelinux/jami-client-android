/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.os.Build
import android.text.TextUtils
import android.text.format.DateUtils
import android.text.format.Formatter
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
import androidx.annotation.LayoutRes
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.DrawableImageViewTarget
import cx.ring.R
import cx.ring.client.MediaViewerActivity
import cx.ring.fragments.ConversationFragment
import cx.ring.utils.ContentUriHandler
import cx.ring.utils.ContentUriHandler.getUriForFile
import cx.ring.utils.GlideApp
import cx.ring.utils.GlideOptions
import cx.ring.utils.ResourceMapper
import cx.ring.views.ConversationViewHolder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import net.jami.conversation.ConversationPresenter
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.model.Interaction.InteractionStatus
import net.jami.utils.StringUtils
import java.io.File
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ConversationAdapter(
    private val conversationFragment: ConversationFragment,
    private val presenter: ConversationPresenter
) : RecyclerView.Adapter<ConversationViewHolder>() {

    private val mInteractions = ArrayList<Interaction>()
    private val hPadding: Int
    private val vPadding: Int
    private val mPictureMaxSize: Int
    private val PICTURE_OPTIONS: GlideOptions
    private var mCurrentLongItem: RecyclerViewContextMenuInfo? = null
    private var convColor = 0
    private var expandedItemPosition = -1
    private var lastDeliveredPosition = -1
    private var lastDisplayedPosition = -1
    private val timestampUpdateTimer: Observable<Long>
    private var lastMsgPos = -1
    private var isComposing = false
    private var mShowReadIndicator = true

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
        if (!TextUtils.isEmpty(e.messageId)) {
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
        if (e.messageId != null) {
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

    /**
     * Updates the contact photo to use for this conversation
     */
    fun setPhoto() {
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mInteractions.size + if (isComposing) 1 else 0
    }

    override fun getItemId(position: Int): Long {
        return if (isComposing && position == mInteractions.size) Long.MAX_VALUE else mInteractions[position].id.toLong()
    }

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

    override fun onBindViewHolder(conversationViewHolder: ConversationViewHolder, position: Int) {
        if (isComposing && position == mInteractions.size) {
            configureForTypingIndicator(conversationViewHolder)
            return
        }
        val interaction = mInteractions[position]
        conversationViewHolder.compositeDisposable.clear()
        if (position > lastMsgPos) {
            //lastMsgPos = position
            val animation = AnimationUtils.loadAnimation(conversationViewHolder.itemView.context, R.anim.fade_in)
            animation.startOffset = 150
            conversationViewHolder.itemView.startAnimation(animation)
        }

        //Log.w(TAG, "onBindViewHolder " + interaction.getType() + " " + interaction);
        if (interaction.type == Interaction.InteractionType.INVALID) {
            conversationViewHolder.itemView.visibility = View.GONE
        } else {
            conversationViewHolder.itemView.visibility = View.VISIBLE
            if (interaction.type == Interaction.InteractionType.TEXT) {
                configureForTextMessage(conversationViewHolder, interaction, position)
            } else if (interaction.type == Interaction.InteractionType.CALL) {
                configureForCallInfo(conversationViewHolder, interaction)
            } else if (interaction.type == Interaction.InteractionType.CONTACT) {
                configureForContactEvent(conversationViewHolder, interaction)
            } else if (interaction.type == Interaction.InteractionType.DATA_TRANSFER) {
                configureForFileInfo(conversationViewHolder, interaction, position)
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
        if (holder.surface != null) {
            holder.surface.release()
            holder.surface = null
        }
        if (holder.player != null) {
            try {
                if (holder.player.isPlaying) holder.player.stop()
                holder.player.reset()
            } catch (e: Exception) {
                // left blank intentionally
            }
            holder.player.release()
            holder.player = null
        }
        if (holder.mMsgTxt != null) {
            holder.mMsgTxt.setOnLongClickListener(null)
        }
        if (holder.mItem != null) {
            holder.mItem.setOnClickListener(null)
        }
        if (expandedItemPosition == holder.layoutPosition) {
            if (holder.mMsgDetailTxt != null) holder.mMsgDetailTxt.visibility = View.GONE
            expandedItemPosition = -1
        }
        holder.compositeDisposable.clear()
    }

    fun setPrimaryColor(color: Int) {
        convColor = color
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

    fun setLastDisplayed(interaction: Interaction) {
        Log.w(TAG, "setLastDisplayed " + interaction.daemonId)
        for (i in mInteractions.indices.reversed()) {
            val element = mInteractions[i]
            if (interaction.id == element.id) {
                if (lastDisplayedPosition != -1) notifyItemChanged(lastDisplayedPosition)
                lastDisplayedPosition = i
                notifyItemChanged(i)
                Log.w(TAG, "new displayed item $i")
                break
            }
        }
    }

    private class RecyclerViewContextMenuInfo(
        val position: Int,
        val id: Long
    ) : ContextMenuInfo

    fun onContextItemSelected(item: MenuItem): Boolean {
        val info = mCurrentLongItem ?: return false
        var interaction: Interaction? = null
        try {
            interaction = mInteractions[info.position]
        } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, "Interaction array may be empty or null", e)
        }
        if (interaction == null) return false
        if (interaction.type == Interaction.InteractionType.CONTACT) return false
        when (item.itemId) {
            R.id.conv_action_download -> presenter.saveFile(interaction)
            R.id.conv_action_share -> presenter.shareFile(interaction)
            R.id.conv_action_open -> presenter.openFile(interaction)
            R.id.conv_action_delete -> presenter.deleteConversationItem(interaction)
            R.id.conv_action_cancel_message -> presenter.cancelMessage(interaction)
            R.id.conv_action_copy_text -> addToClipboard(interaction.body)
        }
        return true
    }

    private fun addToClipboard(text: String?) {
        if (text == null || text.isEmpty()) return
        val clipboard = conversationFragment.requireActivity()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Message", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun configureImage(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.mImage.context
        GlideApp.with(context)
            .load(path)
            .apply(PICTURE_OPTIONS)
            .into(DrawableImageViewTarget(viewHolder.mImage).waitForLayout())
        viewHolder.mImage.setOnClickListener { v: View ->
            try {
                val contentUri = getUriForFile(v.context, ContentUriHandler.AUTHORITY_FILES, path)
                val i = Intent(context, MediaViewerActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, "image/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(conversationFragment.requireActivity(), viewHolder.mImage, "picture")
                conversationFragment.startActivityForResult(i, 3006, options.toBundle())
            } catch (e: Exception) {
                Log.w(TAG, "Can't open picture", e);
            }
        }
    }

    private fun configureAudio(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.itemView.context
        try {
            (viewHolder.btnAccept as ImageView).setImageResource(R.drawable.baseline_play_arrow_24)
            val player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path))
            viewHolder.player = player
            if (player != null) {
                player.setOnCompletionListener { mp: MediaPlayer ->
                    mp.seekTo(0)
                    (viewHolder.btnAccept as ImageView).setImageResource(R.drawable.baseline_play_arrow_24)
                }
                viewHolder.btnAccept.setOnClickListener {
                    if (player.isPlaying) {
                        player.pause()
                        (viewHolder.btnAccept as ImageView).setImageResource(R.drawable.baseline_play_arrow_24)
                    } else {
                        player.start()
                        (viewHolder.btnAccept as ImageView).setImageResource(R.drawable.baseline_pause_24)
                    }
                }
                viewHolder.btnRefuse.setOnClickListener {
                    if (player.isPlaying) player.pause()
                    player.seekTo(0)
                    (viewHolder.btnAccept as ImageView).setImageResource(R.drawable.baseline_play_arrow_24)
                }
                viewHolder.compositeDisposable.add(
                    Observable.interval(1L, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .startWithItem(0L)
                        .subscribe { t: Long? ->
                            val pS = player.currentPosition / 1000
                            val dS = player.duration / 1000
                            viewHolder.mMsgTxt.text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d / %02d:%02d", pS / 60, pS % 60, dS / 60, dS % 60
                            )
                        })
            } else {
                viewHolder.btnAccept.setOnClickListener(null)
                viewHolder.btnRefuse.setOnClickListener(null)
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
        val player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path)) ?: return
        viewHolder.player = player
        val playBtn = ContextCompat.getDrawable(viewHolder.mLayout.context, R.drawable.baseline_play_arrow_24)!!.mutate()
        DrawableCompat.setTint(playBtn, Color.WHITE)
        (viewHolder.mLayout as CardView).foreground = playBtn
        player.setOnCompletionListener { mp: MediaPlayer ->
            if (mp.isPlaying) mp.pause()
            mp.seekTo(1)
            (viewHolder.mLayout as CardView).foreground = playBtn
        }
        player.setOnVideoSizeChangedListener { mp: MediaPlayer, width: Int, height: Int ->
            Log.w(TAG, "OnVideoSizeChanged " + width + "x" + height)
            val p = viewHolder.video.layoutParams as FrameLayout.LayoutParams
            val maxDim = max(width, height)
            p.width = width * mPictureMaxSize / maxDim
            p.height = height * mPictureMaxSize / maxDim
            viewHolder.video.layoutParams = p
        }
        if (viewHolder.video.isAvailable) {
            if (viewHolder.surface == null) {
                viewHolder.surface = Surface(viewHolder.video.surfaceTexture)
            }
            player.setSurface(viewHolder.surface)
        }
        viewHolder.video.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                if (viewHolder.surface == null) {
                    viewHolder.surface = Surface(surface)
                    try {
                        player.setSurface(viewHolder.surface)
                    } catch (e: Exception) {
                        // Left blank
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
                player.release()
                viewHolder.surface?.let {
                    viewHolder.surface = null
                    it.release()
                }
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        viewHolder.video.setOnClickListener {
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

    private fun configureForFileInfo(viewHolder: ConversationViewHolder, interaction: Interaction, position: Int) {
        val file = interaction as DataTransfer
        val path = presenter.deviceRuntimeService.getConversationPath(file)
        //if (file.isComplete())
        //    file.setSize(path.length());
        val timeString = timestampToDetailString(viewHolder.itemView.context, file.timestamp)
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
            when (val status = file.status) {
                InteractionStatus.TRANSFER_FINISHED -> viewHolder.mMsgDetailTxt.text = String.format("%s - %s", timeString,
                    Formatter.formatFileSize(viewHolder.itemView.context, file.totalSize))
                InteractionStatus.TRANSFER_ONGOING -> viewHolder.mMsgDetailTxt.text = String.format("%s / %s - %s",
                    Formatter.formatFileSize(viewHolder.itemView.context, file.bytesProgress),
                    Formatter.formatFileSize(viewHolder.itemView.context, file.totalSize),
                    ResourceMapper.getReadableFileTransferStatus(viewHolder.itemView.context, status))
                else -> viewHolder.mMsgDetailTxt.text = String.format("%s - %s - %s", timeString,
                    Formatter.formatFileSize(viewHolder.itemView.context, file.totalSize),
                    ResourceMapper.getReadableFileTransferStatus(viewHolder.itemView.context, status))
            }
        })
        val type = viewHolder.type.transferType
        viewHolder.compositeDisposable.clear()
        if (hasPermanentTimeString(file, position)) {
            viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                viewHolder.mMsgDetailTxtPerm.text = timestampToDetailString(viewHolder.itemView.context, file.timestamp)
            })
            viewHolder.mMsgDetailTxtPerm.visibility = View.VISIBLE
        } else {
            viewHolder.mMsgDetailTxtPerm.visibility = View.GONE
        }
        val contact = interaction.contact
        if (interaction.isIncoming) {
            viewHolder.mAvatar.setImageBitmap(null)
            viewHolder.mAvatar.visibility = View.VISIBLE
            if (contact != null) {
                viewHolder.mAvatar.setImageDrawable(conversationFragment.getConversationAvatar(contact.primaryNumber))
            }
        } else {
            when (interaction.status) {
                InteractionStatus.SENDING -> {
                    viewHolder.mStatusIcon.visibility = View.VISIBLE
                    viewHolder.mStatusIcon.setImageResource(R.drawable.baseline_circle_24)
                }
                InteractionStatus.FAILURE -> {
                    viewHolder.mStatusIcon.visibility = View.VISIBLE
                    viewHolder.mStatusIcon.setImageResource(R.drawable.round_highlight_off_24)
                }
                InteractionStatus.DISPLAYED -> {
                    viewHolder.mStatusIcon.visibility = if (mShowReadIndicator) View.VISIBLE else View.GONE
                    viewHolder.mStatusIcon.setImageDrawable(conversationFragment.getSmallConversationAvatar(contact!!.primaryNumber))
                }
                else -> {
                    viewHolder.mStatusIcon.visibility = View.VISIBLE
                    viewHolder.mStatusIcon.setImageResource(R.drawable.baseline_check_circle_24)
                    lastDeliveredPosition = position
                }
            }
        }
        val longPressView =
            (if (type == TransferMsgType.IMAGE) viewHolder.mImage else if (type == TransferMsgType.VIDEO) viewHolder.video else if (type == TransferMsgType.AUDIO) viewHolder.mAudioInfoLayout else viewHolder.mFileInfoLayout)
                ?: return
        if (type == TransferMsgType.AUDIO || type == TransferMsgType.FILE) {
            longPressView.background.setTintList(null)
        }
        longPressView.setOnCreateContextMenuListener { menu: ContextMenu, v: View, menuInfo: ContextMenuInfo? ->
            menu.setHeaderTitle(file.displayName)
            MenuInflater(v.context).inflate(R.menu.conversation_item_actions_file, menu)
            if (file.status == InteractionStatus.TRANSFER_ONGOING) {
                menu.findItem(R.id.conv_action_delete).setTitle(android.R.string.cancel)
                menu.removeItem(R.id.conv_action_download)
                menu.removeItem(R.id.conv_action_share)
                menu.removeItem(R.id.conv_action_open)
            } else {
                if (!file.isComplete) {
                    menu.removeItem(R.id.conv_action_download)
                    menu.removeItem(R.id.conv_action_share)
                }
            }
            conversationFragment.onCreateContextMenu(menu, v, menuInfo)
        }
        longPressView.setOnLongClickListener { v: View ->
            if (type == TransferMsgType.AUDIO || type == TransferMsgType.FILE) {
                conversationFragment.updatePosition(viewHolder.adapterPosition)
                longPressView.background.setTint(conversationFragment.resources.getColor(R.color.grey_500))
            }
            mCurrentLongItem = RecyclerViewContextMenuInfo(viewHolder.adapterPosition, v.id.toLong())
            false
        }
        if (type == TransferMsgType.IMAGE) {
            configureImage(viewHolder, path)
        } else if (type == TransferMsgType.VIDEO) {
            configureVideo(viewHolder, path)
        } else if (type == TransferMsgType.AUDIO) {
            configureAudio(viewHolder, path)
        } else {
            val status = file.status
            if (status.isError) {
                viewHolder.mIcon.setImageResource(R.drawable.baseline_warning_24)
            } else {
                viewHolder.mIcon.setImageResource(R.drawable.baseline_attach_file_24)
            }
            viewHolder.mMsgTxt.text = file.displayName
            if (status == InteractionStatus.TRANSFER_AWAITING_HOST) {
                viewHolder.btnRefuse.visibility = View.VISIBLE
                viewHolder.mAnswerLayout.visibility = View.VISIBLE
                viewHolder.btnAccept.setOnClickListener { presenter.acceptFile(file) }
                viewHolder.btnRefuse.setOnClickListener { presenter.refuseFile(file) }
            } else if (status == InteractionStatus.FILE_AVAILABLE) {
                viewHolder.btnRefuse.visibility = View.GONE
                viewHolder.mAnswerLayout.visibility = View.VISIBLE
                viewHolder.btnAccept.setOnClickListener { presenter.acceptFile(file) }
            } else {
                viewHolder.mAnswerLayout.visibility = View.GONE
                if (status == InteractionStatus.TRANSFER_ONGOING) {
                    viewHolder.progress.max = (file.totalSize / 1024).toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        viewHolder.progress.setProgress((file.bytesProgress / 1024).toInt(), true)
                    } else {
                        viewHolder.progress.progress = (file.bytesProgress / 1024).toInt()
                    }
                    viewHolder.progress.show()
                } else {
                    viewHolder.progress.hide()
                }
            }
        }
    }

    private fun configureForTypingIndicator(viewHolder: ConversationViewHolder) {
        AnimatedVectorDrawableCompat.create(viewHolder.itemView.context, R.drawable.typing_indicator_animation)?.let { anim ->
            viewHolder.mStatusIcon.setImageDrawable(anim)
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
        val textMessage = interaction as TextMessage
        val contact = textMessage.contact  ?: return
        // Log.w(TAG, "configureForTextMessage " + position + " " + interaction.getDaemonId() + " " + interaction.getStatus());
        val message = textMessage.body!!.trim { it <= ' ' }
        val longPressView: View = convViewHolder.mMsgTxt
        longPressView.background.setTintList(null)
        longPressView.setOnCreateContextMenuListener { menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo? ->
            val date = Date(interaction.timestamp)
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            menu.setHeaderTitle(dateFormat.format(date))
            conversationFragment.onCreateContextMenu(menu, v!!, menuInfo)
            val inflater = conversationFragment.requireActivity().menuInflater
            inflater.inflate(R.menu.conversation_item_actions_messages, menu)
            if (interaction.status == InteractionStatus.SENDING) {
                menu.removeItem(R.id.conv_action_delete)
            } else {
                menu.findItem(R.id.conv_action_delete).setTitle(R.string.menu_message_delete)
                menu.removeItem(R.id.conv_action_cancel_message)
            }
        }
        longPressView.setOnLongClickListener { v: View ->
            if (expandedItemPosition == position) {
                expandedItemPosition = -1
            }
            conversationFragment.updatePosition(convViewHolder.bindingAdapterPosition)
            if (textMessage.isIncoming) {
                longPressView.background.setTint(conversationFragment.resources.getColor(R.color.grey_500))
            } else {
                longPressView.background.setTint(conversationFragment.resources.getColor(R.color.blue_900))
            }
            mCurrentLongItem = RecyclerViewContextMenuInfo(convViewHolder.bindingAdapterPosition, v.id.toLong())
            false
        }
        val isTimeShown = hasPermanentTimeString(textMessage, position)
        val msgSequenceType = getMsgSequencing(position, isTimeShown)
        if (StringUtils.isOnlyEmoji(message)) {
            convViewHolder.mMsgTxt.background.alpha = 0
            convViewHolder.mMsgTxt.textSize = 32.0f
            convViewHolder.mMsgTxt.setPadding(0, 0, 0, 0)
        } else {
            val resIndex = msgSequenceType.ordinal + (if (textMessage.isIncoming) 1 else 0) * 4
            convViewHolder.mMsgTxt.background = ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
            if (convColor != 0 && !textMessage.isIncoming) {
                convViewHolder.mMsgTxt.background.setTint(convColor)
            }
            convViewHolder.mMsgTxt.background.alpha = 255
            convViewHolder.mMsgTxt.textSize = 16f
            convViewHolder.mMsgTxt.setPadding(hPadding, vPadding, hPadding, vPadding)
        }
        convViewHolder.mMsgTxt.text = message
        val endOfSeq =
            msgSequenceType == SequenceType.LAST || msgSequenceType == SequenceType.SINGLE
        if (textMessage.isIncoming) {
            if (endOfSeq) {
                convViewHolder.mAvatar.setImageDrawable(conversationFragment.getConversationAvatar(contact.primaryNumber))
                convViewHolder.mAvatar.visibility = View.VISIBLE
            } else {
                if (position == lastMsgPos - 1 && convViewHolder.mAvatar != null) {
                    val animation = AnimationUtils.loadAnimation(convViewHolder.mAvatar.context, R.anim.fade_out)
                    animation.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(arg0: Animation) {}
                        override fun onAnimationRepeat(arg0: Animation) {}
                        override fun onAnimationEnd(arg0: Animation) {
                            convViewHolder.mAvatar.setImageBitmap(null)
                            convViewHolder.mAvatar.visibility = View.INVISIBLE
                        }
                    })
                    convViewHolder.mAvatar.startAnimation(animation)
                } else {
                    if (convViewHolder.mAvatar != null) {
                        convViewHolder.mAvatar.setImageBitmap(null)
                        convViewHolder.mAvatar.visibility = View.INVISIBLE
                    }
                }
            }
        } else {
            when (textMessage.status) {
                InteractionStatus.SENDING -> {
                    convViewHolder.mStatusIcon.visibility = View.VISIBLE
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.baseline_circle_24)
                }
                InteractionStatus.FAILURE -> {
                    convViewHolder.mStatusIcon.visibility = View.VISIBLE
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.round_highlight_off_24)
                }
                InteractionStatus.DISPLAYED -> if (lastDisplayedPosition == position) {
                    convViewHolder.mStatusIcon.visibility = if (mShowReadIndicator) View.VISIBLE else View.GONE
                    convViewHolder.mStatusIcon.setImageDrawable(conversationFragment.getSmallConversationAvatar(contact.primaryNumber))
                } else {
                    convViewHolder.mStatusIcon.visibility = View.GONE
                    convViewHolder.mStatusIcon.setImageDrawable(null)
                }
                else -> if (position == lastOutgoingIndex()) {
                    convViewHolder.mStatusIcon.visibility = View.VISIBLE
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.baseline_check_circle_24)
                    lastDeliveredPosition = position
                } else {
                    convViewHolder.mStatusIcon.visibility = View.GONE
                    convViewHolder.mStatusIcon.setImageDrawable(null)
                }
            }
        }
        setBottomMargin(convViewHolder.mMsgTxt, if (endOfSeq) 8 else 0)
        if (isTimeShown) {
            convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                val timeSeparationString = timestampToDetailString(context, textMessage.timestamp)
                convViewHolder.mMsgDetailTxtPerm.text = timeSeparationString
            })
            convViewHolder.mMsgDetailTxtPerm.visibility = View.VISIBLE
        } else {
            convViewHolder.mMsgDetailTxtPerm.visibility = View.GONE
            val isExpanded = position == expandedItemPosition
            if (isExpanded) {
                convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                    val timeSeparationString =
                        timestampToDetailString(context, textMessage.timestamp)
                    convViewHolder.mMsgDetailTxt.text = timeSeparationString
                })
            }
            setItemViewExpansionState(convViewHolder, isExpanded)
            convViewHolder.mItem.setOnClickListener {
                if (convViewHolder.animator != null && convViewHolder.animator.isRunning) {
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
    }

    private fun configureForContactEvent(viewHolder: ConversationViewHolder, interaction: Interaction) {
        val event = interaction as ContactEvent
        viewHolder.mMsgTxt.setText(when (event.event) {
            ContactEvent.Event.ADDED -> R.string.hist_contact_added
            ContactEvent.Event.INVITED -> R.string.hist_contact_invited
            ContactEvent.Event.REMOVED -> R.string.hist_contact_left
            ContactEvent.Event.BANNED -> R.string.hist_contact_banned
            ContactEvent.Event.INCOMING_REQUEST -> R.string.hist_invitation_received
            else -> R.string.hist_contact_added
        })
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
            val timeSeparationString = timestampToDetailString(viewHolder.itemView.context, event.timestamp)
            viewHolder.mMsgDetailTxt.text = timeSeparationString
        })
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     */
    private fun configureForCallInfo(convViewHolder: ConversationViewHolder, interaction: Interaction) {
        convViewHolder.mIcon.scaleY = 1f
        val context = convViewHolder.itemView.context
        val longPressView: View = convViewHolder.mCallInfoLayout
        longPressView.background.setTintList(null)
        longPressView.setOnCreateContextMenuListener { menu: ContextMenu, v: View, menuInfo: ContextMenuInfo? ->
            conversationFragment.onCreateContextMenu(menu, v, menuInfo)
            val inflater = conversationFragment.requireActivity().menuInflater
            inflater.inflate(R.menu.conversation_item_actions_messages, menu)
            menu.findItem(R.id.conv_action_delete).setTitle(R.string.menu_delete)
            menu.removeItem(R.id.conv_action_cancel_message)
            menu.removeItem(R.id.conv_action_copy_text)
        }
        longPressView.setOnLongClickListener { v: View ->
            longPressView.background.setTint(conversationFragment.resources.getColor(R.color.grey_500))
            conversationFragment.updatePosition(convViewHolder.adapterPosition)
            mCurrentLongItem = RecyclerViewContextMenuInfo(convViewHolder.adapterPosition, v.id.toLong())
            false
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
                convViewHolder.mIcon.scaleY = -1f
            }
            historyTxt =
                if (call.isIncoming) context.getString(R.string.notif_missed_incoming_call) else context.getString(
                    R.string.notif_missed_outgoing_call
                )
        } else {
            pictureResID =
                if (call.isIncoming) R.drawable.baseline_call_received_24 else R.drawable.baseline_call_made_24
            historyTxt =
                if (call.isIncoming) context.getString(R.string.notif_incoming_call) else context.getString(
                    R.string.notif_outgoing_call
                )
        }
        convViewHolder.mIcon.setImageResource(pictureResID)
        convViewHolder.mHistTxt.text = historyTxt
        convViewHolder.mHistDetailTxt.text = DateFormat.getDateTimeInstance()
            .format(call.timestamp) // start date
    }

    /**
     * Computes the string to set in text details between messages, indicating time separation.
     *
     * @param timestamp The timestamp used to launch the computation with Date().getTime().
     * Can be the last received message timestamp for example.
     * @return The string to display in the text details between messages.
     */
    private fun timestampToDetailString(context: Context, timestamp: Long): String {
        val diff = Date().time - timestamp
        val timeStr: String = if (diff < DateUtils.WEEK_IN_MILLIS) {
            if (diff < DateUtils.DAY_IN_MILLIS && DateUtils.isToday(timestamp)) { // 11:32 A.M.
                DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME)
            } else {
                DateUtils.formatDateTime(context, timestamp,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_NO_YEAR or
                            DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME)
            }
        } else if (diff < DateUtils.YEAR_IN_MILLIS) { // JAN. 7, 11:02 A.M.
            DateUtils.formatDateTime(context, timestamp,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or
                        DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME)
        } else {
            DateUtils.formatDateTime(context, timestamp,
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY or
                        DateUtils.FORMAT_ABBREV_ALL)
        }
        return timeStr.uppercase(Locale.getDefault())
    }

    /**
     * Helper method to return the previous TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the previous TextMessage if any, null otherwise
     */
    private fun getPreviousMessageFromPosition(position: Int): Interaction? {
        return if (mInteractions.isNotEmpty() && position > 0) {
            mInteractions[position - 1]
        } else null
    }

    /**
     * Helper method to return the next TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the next TextMessage if any, null otherwise
     */
    private fun getNextMessageFromPosition(position: Int): Interaction? {
        return if (mInteractions.isNotEmpty() && position < mInteractions.size - 1) {
            mInteractions[position + 1]
        } else null
    }

    private fun isSeqBreak(first: Interaction, second: Interaction): Boolean {
        return StringUtils.isOnlyEmoji(first.body) != StringUtils.isOnlyEmoji(second.body) || first.isIncoming != second.isIncoming || first.type != Interaction.InteractionType.TEXT || second.type != Interaction.InteractionType.TEXT
    }

    private fun isAlwaysSingleMsg(msg: Interaction): Boolean {
        return (msg.type != Interaction.InteractionType.TEXT
                || StringUtils.isOnlyEmoji(msg.body))
    }

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
            if ((isSeqBreak(msg, prevMsg) || isTimeShown) && !(isSeqBreak(msg, nextMsg) || nextMsgHasTime)) {
                return SequenceType.FIRST
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && isSeqBreak(msg, nextMsg)) {
                return SequenceType.LAST
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && !isSeqBreak(msg, nextMsg)) {
                return if (nextMsgHasTime) SequenceType.LAST else SequenceType.MIDDLE
            }
        }
        return SequenceType.SINGLE
    }

    private fun setItemViewExpansionState(viewHolder: ConversationViewHolder, expanded: Boolean) {
        val view: View = viewHolder.mMsgDetailTxt
        if (viewHolder.animator == null) {
            if (view.height == 0 && !expanded) {
                return
            }
            viewHolder.animator = ValueAnimator()
        }
        if (viewHolder.animator.isRunning) {
            viewHolder.animator.reverse()
            return
        }
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        viewHolder.animator.setIntValues(0, view.measuredHeight)
        if (expanded) {
            view.visibility = View.VISIBLE
        }
        viewHolder.animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val va = animation as ValueAnimator
                if (va.animatedValue as Int == 0) {
                    view.visibility = View.GONE
                }
                viewHolder.animator = null
            }
        })
        viewHolder.animator.duration = 200
        viewHolder.animator.addUpdateListener { animation: ValueAnimator ->
            view.layoutParams.height = (animation.animatedValue as Int)
            view.requestLayout()
        }
        if (!expanded) {
            viewHolder.animator.reverse()
        } else {
            viewHolder.animator.start()
        }
    }

    private fun hasPermanentTimeString(msg: Interaction?, position: Int): Boolean {
        if (msg == null) {
            return false
        }
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

    private enum class SequenceType {
        FIRST, MIDDLE, LAST, SINGLE
    }

    enum class TransferMsgType {
        FILE, IMAGE, AUDIO, VIDEO
    }

    enum class MessageType(@LayoutRes val layout: Int) {
        INCOMING_FILE(R.layout.item_conv_file_peer),
        INCOMING_IMAGE(R.layout.item_conv_image_peer),
        INCOMING_AUDIO(R.layout.item_conv_audio_peer),
        INCOMING_VIDEO(R.layout.item_conv_video_peer),
        OUTGOING_FILE(R.layout.item_conv_file_me),
        OUTGOING_IMAGE(R.layout.item_conv_image_me),
        OUTGOING_AUDIO(R.layout.item_conv_audio_me),
        OUTGOING_VIDEO(R.layout.item_conv_video_me),
        CONTACT_EVENT(R.layout.item_conv_contact),
        CALL_INFORMATION(R.layout.item_conv_call),
        INCOMING_TEXT_MESSAGE(R.layout.item_conv_msg_peer),
        OUTGOING_TEXT_MESSAGE(R.layout.item_conv_msg_me),
        COMPOSING_INDICATION(R.layout.item_conv_composing),
        INVALID(-1);

        val isFile: Boolean
            get() = this == INCOMING_FILE || this == OUTGOING_FILE
        val isAudio: Boolean
            get() = this == INCOMING_AUDIO || this == OUTGOING_AUDIO
        val isVideo: Boolean
            get() = this == INCOMING_VIDEO || this == OUTGOING_VIDEO
        val isImage: Boolean
            get() = this == INCOMING_IMAGE || this == OUTGOING_IMAGE
        val transferType: TransferMsgType
            get() = if (isFile) TransferMsgType.FILE else if (isImage) TransferMsgType.IMAGE else if (isAudio) TransferMsgType.AUDIO else if (isVideo) TransferMsgType.VIDEO else TransferMsgType.FILE
    }

    companion object {
        private val TAG = ConversationAdapter::class.java.simpleName
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
    }

    init {
        val res = conversationFragment.resources
        hPadding = res.getDimensionPixelSize(R.dimen.padding_medium)
        vPadding = res.getDimensionPixelSize(R.dimen.padding_small)
        mPictureMaxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f, res.displayMetrics).toInt()
        val corner = res.getDimension(R.dimen.conversation_message_radius).toInt()
        PICTURE_OPTIONS = GlideOptions()
            .transform(CenterInside())
            .fitCenter()
            .override(mPictureMaxSize)
            .transform(RoundedCorners(corner))
        timestampUpdateTimer = Observable.interval(10, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .startWithItem(0L)
    }
}