/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
package cx.ring.tv.conversation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.TextureView.SurfaceTextureListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.DrawableImageViewTarget
import cx.ring.R
import cx.ring.adapters.MessageType
import cx.ring.client.MediaViewerActivity
import cx.ring.utils.*
import cx.ring.utils.ContentUriHandler.getUriForFile
import cx.ring.viewholders.ConversationViewHolder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.conversation.ConversationPresenter
import net.jami.model.*
import net.jami.model.Interaction.InteractionStatus
import net.jami.utils.StringUtils.isOnlyEmoji
import java.io.File
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class TvConversationAdapter(
    private val conversationFragment: TvConversationFragment,
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
    private val timestampUpdateTimer: Observable<Long>

    /**
     * Refreshes the data and notifies the changes
     *
     * @param list an arraylist of interactions
     */
    fun updateDataset(list: List<Interaction>) {
        Log.d(TAG, "updateDataset: list size=" + list.size)
        if (mInteractions.isEmpty()) {
            mInteractions.addAll(list)
        } else if (list.size > mInteractions.size) {
            mInteractions.addAll(list.subList(mInteractions.size, list.size))
        } else {
            mInteractions.clear()
            mInteractions.addAll(list)
        }
        notifyDataSetChanged()
    }

    fun add(e: Interaction) {
        val update = mInteractions.isNotEmpty()
        mInteractions.add(e)
        notifyItemInserted(mInteractions.size - 1)
        if (update) notifyItemChanged(mInteractions.size - 2)
    }

    fun update(e: Interaction) {
        if (!e.isIncoming && e.status === InteractionStatus.SUCCESS) {
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
        for (i in mInteractions.indices.reversed()) {
            val element = mInteractions[i]
            if (e.id == element.id) {
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

    /**
     * Updates the contact photo to use for this conversation
     */
    fun setPhoto() {
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mInteractions.size + 1
    }

    override fun getItemId(position: Int): Long {
        return mInteractions[position].id.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        if (position == mInteractions.size) {
            return MessageType.HEADER.ordinal
        }
        val interaction = mInteractions[position]
        if (interaction != null) {
            when (interaction.type) {
                Interaction.InteractionType.CONTACT -> return MessageType.CONTACT_EVENT.ordinal
                Interaction.InteractionType.CALL -> return MessageType.CALL_INFORMATION.ordinal
                Interaction.InteractionType.TEXT -> return if (interaction.isIncoming) {
                    MessageType.INCOMING_TEXT_MESSAGE.ordinal
                } else {
                    MessageType.OUTGOING_TEXT_MESSAGE.ordinal
                }
                Interaction.InteractionType.DATA_TRANSFER -> {
                    val file = interaction as DataTransfer
                    val out = if (interaction.isIncoming) 0 else 4
                    if (file.isComplete) {
                        if (file.isPicture) {
                            return MessageType.INCOMING_IMAGE.ordinal + out
                        } else if (file.isAudio) {
                            return MessageType.INCOMING_AUDIO.ordinal + out
                        } else if (file.isVideo) {
                            return MessageType.INCOMING_VIDEO.ordinal + out
                        }
                    }
                    return out
                }
            }
        }
        return MessageType.CALL_INFORMATION.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val type = MessageType.values()[viewType]
        val v = LayoutInflater.from(parent.context).inflate(type.tvLayout, parent, false) as ViewGroup
        return ConversationViewHolder(v, type)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        if (position >= mInteractions.size)
            return
        val interaction = mInteractions[position]
        holder.compositeDisposable.clear()
        val type = interaction.type
        if (type == Interaction.InteractionType.INVALID) {
            holder.itemView.visibility = View.GONE
        } else {
            holder.itemView.visibility = View.VISIBLE
            when (type) {
                Interaction.InteractionType.TEXT -> configureForTextMessage(holder, interaction, position)
                Interaction.InteractionType.CALL -> configureForCallInfoTextMessage(holder, interaction)
                Interaction.InteractionType.CONTACT -> configureForContactEvent(holder, interaction)
                Interaction.InteractionType.DATA_TRANSFER -> configureForFileInfo(holder, interaction, position)
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
        holder.mMsgTxt?.setOnClickListener(null)
        holder.mMsgTxt?.setOnLongClickListener(null)
        if (expandedItemPosition == holder.layoutPosition) {
            holder.mMsgDetailTxt?.visibility = View.GONE
            expandedItemPosition = -1
        }
        holder.compositeDisposable.clear()
        super.onViewRecycled(holder)
    }

    fun setPrimaryColor(color: Int) {
        convColor = color
        notifyDataSetChanged()
    }

    private class RecyclerViewContextMenuInfo constructor(val position: Int, val id: Long) : ContextMenuInfo

    fun onContextItemSelected(item: MenuItem): Boolean {
        val info = mCurrentLongItem ?: return false
        val interaction = try {
            mInteractions[info.position]
        } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, "Interaction array may be empty or null", e)
            return false
        }
        if (interaction.type === Interaction.InteractionType.CONTACT) return false
        when (item.itemId) {
            R.id.conv_action_download -> presenter.saveFile(interaction)
            R.id.conv_action_open -> presenter.openFile(interaction)
            R.id.conv_action_delete -> presenter.deleteConversationItem(interaction)
            R.id.conv_action_cancel_message -> presenter.cancelMessage(interaction)
        }
        return true
    }

    private fun configureImage(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.itemView.context
        GlideApp.with(context)
            .load(path)
            .apply(PICTURE_OPTIONS)
            .into(DrawableImageViewTarget(viewHolder.mImage).waitForLayout())
        viewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewHolder.itemView.setBackgroundResource(if (hasFocus) R.drawable.tv_item_selected_background else R.drawable.tv_item_unselected_background)
            viewHolder.mAnswerLayout!!.animate().scaleY(if (hasFocus) 1.1f else 1f)
                .scaleX(if (hasFocus) 1.1f else 1f)
        }

        viewHolder.itemView.setOnClickListener { v: View ->
            try {
                val contentUri = getUriForFile(v.context, ContentUriHandler.AUTHORITY_FILES, path)
                val i = Intent(context, MediaViewerActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, "image/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(conversationFragment.requireActivity(), viewHolder.mImage!!, "picture")
                conversationFragment.startActivityForResult(i, 3006, options.toBundle())
            } catch (e: Exception) {
                Log.w(TAG, "Can't open picture", e)
            }
        }
    }

    private fun configureAudio(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.itemView.context
        viewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewHolder.itemView.setBackgroundResource(if (hasFocus) R.drawable.tv_item_selected_background else R.drawable.tv_item_unselected_background)
            viewHolder.mAudioInfoLayout?.animate()?.scaleY(if (hasFocus) 1.1f else 1f)?.scaleX(if (hasFocus) 1.1f else 1f)
        }

        try {
            val acceptBtn = viewHolder.btnAccept as ImageView
            val refuseBtn = viewHolder.btnRefuse
            acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
            val player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path))
            viewHolder.player = player
            if (player != null) {
                player.setOnCompletionListener { mp: MediaPlayer ->
                    mp.seekTo(0)
                    acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                }
                viewHolder.itemView.setOnClickListener {
                    if (player.isPlaying) {
                        player.pause()
                        acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                    } else {
                        player.start()
                        acceptBtn.setImageResource(R.drawable.baseline_pause_24)
                    }
                }
                refuseBtn?.setOnClickListener {
                    if (player.isPlaying) player.pause()
                    player.seekTo(0)
                    acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                }
                viewHolder.compositeDisposable.add(
                    Observable.interval(1L, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .startWithItem(0L)
                        .subscribe {
                            val pS = player.currentPosition / 1000
                            val dS = player.duration / 1000
                            viewHolder.mMsgTxt?.text = String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d", pS / 60, pS % 60, dS / 60, dS % 60)
                        })
            } else {
                acceptBtn.setOnClickListener(null)
                refuseBtn?.setOnClickListener(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player", e)
        }
    }

    private fun configureVideo(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.itemView.context
        viewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewHolder.itemView.setBackgroundResource(if (hasFocus) R.drawable.tv_item_selected_background else R.drawable.tv_item_unselected_background)
            viewHolder.mLayout?.animate()?.scaleY(if (hasFocus) 1.1f else 1f)?.scaleX(if (hasFocus) 1.1f else 1f)
        }

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
            if (maxDim != 0)  {
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
                player.release()
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
        viewHolder.compositeDisposable.clear()
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
        } else {
            when (interaction.status) {
                InteractionStatus.SENDING -> {
                    viewHolder.mStatusIcon?.visibility = View.VISIBLE
                    viewHolder.mStatusIcon?.setImageResource(R.drawable.baseline_circle_24)
                }
                InteractionStatus.FAILURE -> {
                    viewHolder.mStatusIcon?.visibility = View.VISIBLE
                    viewHolder.mStatusIcon?.setImageResource(R.drawable.round_highlight_off_24)
                }
                else -> {
                    viewHolder.mStatusIcon?.visibility = View.VISIBLE
                    viewHolder.mStatusIcon?.setImageResource(R.drawable.baseline_check_circle_24)
                    lastDeliveredPosition = position
                }
            }
        }
        val type = viewHolder.type.transferType

        val longPressView = viewHolder.itemView
        longPressView.setOnCreateContextMenuListener { menu: ContextMenu, v: View, menuInfo: ContextMenuInfo? ->
            menu.setHeaderTitle(file.displayName)
            conversationFragment.onCreateContextMenu(menu, v, menuInfo)
            val inflater = conversationFragment.requireActivity().menuInflater
            inflater.inflate(R.menu.conversation_item_actions_file_tv, menu)
            if (!file.isComplete) {
                menu.removeItem(R.id.conv_action_download)
                menu.removeItem(R.id.conv_action_share)
            }
        }
        longPressView.setOnLongClickListener { v: View ->
            if (type == MessageType.TransferType.AUDIO || type == MessageType.TransferType.FILE) {
                conversationFragment.updatePosition(viewHolder.adapterPosition)
            }
            mCurrentLongItem = RecyclerViewContextMenuInfo(viewHolder.adapterPosition, v.id.toLong())
            false
        }
        if (type == MessageType.TransferType.IMAGE) {
            configureImage(viewHolder, path)
        } else if (type == MessageType.TransferType.VIDEO) {
            configureVideo(viewHolder, path)
        } else if (type == MessageType.TransferType.AUDIO) {
            configureAudio(viewHolder, path)
        } else {
            viewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                viewHolder.itemView.setBackgroundResource(if (hasFocus) R.drawable.tv_item_selected_background else R.drawable.tv_item_unselected_background)
                viewHolder.mFileInfoLayout?.animate()
                    ?.scaleY(if (hasFocus) 1.1f else 1f)
                    ?.scaleX(if (hasFocus) 1.1f else 1f)
            }
            val status = file.status
            viewHolder.mIcon?.setImageResource(if (status.isError) R.drawable.baseline_warning_24 else R.drawable.baseline_attach_file_24)
            viewHolder.mMsgTxt?.text = file.displayName
            if (status === InteractionStatus.TRANSFER_AWAITING_HOST) {
                viewHolder.mAnswerLayout?.visibility = View.VISIBLE
                viewHolder.btnAccept?.setOnClickListener { presenter.acceptFile(file) }
                viewHolder.btnRefuse?.setOnClickListener { presenter.refuseFile(file) }
            } else if (status == InteractionStatus.FILE_AVAILABLE) {
                viewHolder.btnRefuse?.visibility = View.GONE
                viewHolder.mAnswerLayout?.visibility = View.VISIBLE
                viewHolder.btnAccept?.setOnClickListener { presenter.acceptFile(file) }
            } else {
                viewHolder.mAnswerLayout?.visibility = View.GONE
            }
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
        val contact = textMessage.contact
        if (contact == null) {
            Log.e(TAG, "Invalid contact, not able to display message correctly")
            return
        }
        convViewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            convViewHolder.itemView.setBackgroundResource(if (hasFocus) R.drawable.tv_item_selected_background else R.drawable.tv_item_unselected_background)
            convViewHolder.mMsgTxt!!.animate().scaleY(if (hasFocus) 1.1f else 1f).scaleX(if (hasFocus) 1.1f else 1f)
        }
        val message = textMessage.body!!.trim { it <= ' ' }
        val longPressView = convViewHolder.itemView
        longPressView.background.setTintList(null)
        longPressView.setOnCreateContextMenuListener { menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo? ->
            val date = Date(interaction.timestamp)
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            menu.setHeaderTitle(dateFormat.format(date))
            conversationFragment.onCreateContextMenu(menu, v!!, menuInfo)
            val inflater = conversationFragment.requireActivity().menuInflater
            inflater.inflate(R.menu.conversation_item_actions_messages_tv, menu)
            if (interaction.status === InteractionStatus.SENDING) {
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
            conversationFragment.updatePosition(convViewHolder.adapterPosition)
            mCurrentLongItem = RecyclerViewContextMenuInfo(
                convViewHolder.adapterPosition, v.id
                    .toLong()
            )
            false
        }
        val isTimeShown = hasPermanentTimeString(textMessage, position)
        val msgSequenceType = getMsgSequencing(position, isTimeShown)
        if (isOnlyEmoji(message)) {
            convViewHolder.mMsgTxt!!.background.alpha = 0
            convViewHolder.mMsgTxt!!.textSize = 32.0f
            convViewHolder.mMsgTxt!!.setPadding(0, 0, 0, 0)
        } else {
            val resIndex = msgSequenceType.ordinal + (if (textMessage.isIncoming) 1 else 0) * 4
            convViewHolder.mMsgTxt!!.background = ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
            if (convColor != 0 && !textMessage.isIncoming) {
                convViewHolder.mMsgTxt!!.background.setTint(convColor)
            }
            convViewHolder.mMsgTxt!!.background.alpha = 255
            convViewHolder.mMsgTxt!!.textSize = 16f
            convViewHolder.mMsgTxt!!.setPadding(hPadding, vPadding, hPadding, vPadding)
        }
        convViewHolder.mMsgTxt!!.text = message
        if (textMessage.isIncoming) {
            convViewHolder.mAvatar!!.setImageBitmap(null)
            convViewHolder.mAvatar.visibility = View.VISIBLE
        } else {
            when (textMessage.status) {
                InteractionStatus.SENDING -> {
                    convViewHolder.mStatusIcon!!.visibility = View.VISIBLE
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.baseline_circle_24)
                }
                InteractionStatus.FAILURE -> {
                    convViewHolder.mStatusIcon!!.visibility = View.VISIBLE
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.round_highlight_off_24)
                }
                else -> if (position == lastOutgoingIndex()) {
                    convViewHolder.mStatusIcon!!.visibility = View.VISIBLE
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.baseline_check_circle_24)
                    lastDeliveredPosition = position
                } else {
                    convViewHolder.mStatusIcon!!.visibility = View.GONE
                }
            }
        }
        if (msgSequenceType == SequenceType.LAST || msgSequenceType == SequenceType.SINGLE) {
            setBottomMargin(convViewHolder.mMsgTxt, 8)
            if (textMessage.isIncoming) {
                convViewHolder.mAvatar!!.setImageDrawable(
                    conversationFragment.getConversationAvatar(contact.primaryNumber)
                )
            }
        } else {
            setBottomMargin(convViewHolder.mMsgTxt, 0)
        }
        if (isTimeShown) {
            convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe { t: Long? ->
                val timeSeparationString = TextUtils.timestampToDetailString(context, textMessage.timestamp)
                convViewHolder.mMsgDetailTxtPerm!!.text = timeSeparationString
            })
            convViewHolder.mMsgDetailTxtPerm!!.visibility = View.VISIBLE
        } else {
            convViewHolder.mMsgDetailTxtPerm!!.visibility = View.GONE
            val isExpanded = position == expandedItemPosition
            if (isExpanded) {
                convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe { t: Long? ->
                    val timeSeparationString = TextUtils.timestampToDetailString(context, textMessage.timestamp)
                    convViewHolder.mMsgDetailTxt!!.text = timeSeparationString
                })
            }
            setItemViewExpansionState(convViewHolder, isExpanded)
            convViewHolder.itemView.setOnClickListener { v: View? ->
                if (convViewHolder.animator != null && convViewHolder.animator!!.isRunning) {
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
        viewHolder.mMsgTxt?.setText(when (event.event) {
            ContactEvent.Event.ADDED -> R.string.hist_contact_added
            ContactEvent.Event.INVITED -> R.string.hist_contact_invited
            ContactEvent.Event.REMOVED -> R.string.hist_contact_left
            ContactEvent.Event.BANNED -> R.string.hist_contact_banned
            ContactEvent.Event.INCOMING_REQUEST -> R.string.hist_invitation_received
            else -> R.string.hist_contact_added
        })
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe { t: Long? ->
            val timeSeparationString = TextUtils.timestampToDetailString(viewHolder.itemView.context, event.timestamp)
            viewHolder.mMsgDetailTxt?.text = timeSeparationString
        })
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     */
    private fun configureForCallInfoTextMessage(convViewHolder: ConversationViewHolder, interaction: Interaction) {
        val pictureResID: Int
        val historyTxt: String
        convViewHolder.mIcon?.scaleY = 1f
        val context = convViewHolder.itemView.context
        val longPressView: View = convViewHolder.mCallInfoLayout!!
        longPressView.background.setTintList(null)
        val call = interaction as Call
        if (call.isMissed) {
            if (call.isIncoming) {
                pictureResID = R.drawable.baseline_call_missed_24
            } else {
                pictureResID = R.drawable.baseline_call_missed_outgoing_24
                // Flip the photo upside down to show a "missed outgoing call"
                convViewHolder.mIcon?.scaleY = -1f
            }
            historyTxt = if (call.isIncoming) context.getString(R.string.notif_missed_incoming_call) else context.getString(R.string.notif_missed_outgoing_call)
        } else {
            pictureResID = if (call.isIncoming) R.drawable.baseline_call_received_24 else R.drawable.baseline_call_made_24
            historyTxt = if (call.isIncoming) context.getString(R.string.notif_incoming_call) else context.getString(R.string.notif_outgoing_call)
        }
        convViewHolder.mIcon?.setImageResource(pictureResID)
        convViewHolder.mHistTxt?.text = historyTxt
        convViewHolder.mHistDetailTxt?.text = DateFormat.getDateTimeInstance().format(call.timestamp) // start date
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
        return if (!mInteractions.isEmpty() && position < mInteractions.size - 1) {
            mInteractions[position + 1]
        } else null
    }

    private fun isSeqBreak(first: Interaction, second: Interaction): Boolean {
        return isOnlyEmoji(first.body) != isOnlyEmoji(second.body) || first.isIncoming != second.isIncoming || first.type !== Interaction.InteractionType.TEXT || second.type !== Interaction.InteractionType.TEXT
    }

    private fun isAlwaysSingleMsg(msg: Interaction): Boolean {
        return (msg.type !== Interaction.InteractionType.TEXT
                || isOnlyEmoji(msg.body))
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
            if (!mInteractions[i].isIncoming) {
                break
            }
            i--
        }
        return i
    }

    private enum class SequenceType {
        FIRST, MIDDLE, LAST, SINGLE
    }

    companion object {
        private val TAG = TvConversationAdapter::class.java.simpleName
        private val msgBGLayouts = intArrayOf(
            R.drawable.textmsg_bg_out_last,
            R.drawable.textmsg_bg_out_middle,
            R.drawable.textmsg_bg_out_first,
            R.drawable.textmsg_bg_out,
            R.drawable.textmsg_bg_in_last,
            R.drawable.textmsg_bg_in_middle,
            R.drawable.textmsg_bg_in_first,
            R.drawable.textmsg_bg_in
        )

        private fun setBottomMargin(view: View?, value: Int) {
            val targetSize = (value * view!!.context.resources.displayMetrics.density).toInt()
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
        timestampUpdateTimer = Observable.interval(10, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .startWithItem(0L)
    }
}