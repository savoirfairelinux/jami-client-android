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
package cx.ring.tv.conversation

import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import android.util.TypedValue
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.android.material.color.MaterialColors
import cx.ring.R
import cx.ring.adapters.MessageType
import cx.ring.client.MediaViewerActivity
import cx.ring.utils.ActionHelper
import cx.ring.utils.ActionHelper.setPadding
import cx.ring.utils.ContentUri.getUriForFile
import cx.ring.utils.DeviceUtils
import cx.ring.utils.TextUtils
import cx.ring.viewholders.ConversationViewHolder
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.conversation.ConversationPresenter
import net.jami.model.Call
import net.jami.model.ContactEvent
import net.jami.model.DataTransfer
import net.jami.model.Interaction
import net.jami.model.Interaction.InteractionStatus
import net.jami.model.Interaction.TransferStatus
import net.jami.model.TextMessage
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
    private val res = conversationFragment.resources
    private val mInteractions = ArrayList<Interaction>()
    private val hPadding = res.getDimensionPixelSize(R.dimen.padding_medium)
    private val vPadding = res.getDimensionPixelSize(R.dimen.padding_small)
    private val mPictureMaxSize =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f, res.displayMetrics).toInt()
    private val pictureOptions = RequestOptions()
        .transform(CenterInside())
        .fitCenter()
        .override(mPictureMaxSize)
        .transform(RoundedCorners(res.getDimension(R.dimen.conversation_message_radius).toInt()))
    private val timestampUpdateTimer = Observable.interval(10, TimeUnit.SECONDS)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .startWithItem(0L)
    private var mCurrentLongItem: RecyclerViewContextMenuInfo? = null
    @ColorInt private var convColor = 0
    @ColorInt private var convColorTint = 0
    private val callPadding = ActionHelper.Padding(
        res.getDimensionPixelSize(R.dimen.text_message_padding),
        res.getDimensionPixelSize(R.dimen.padding_call_vertical),
        res.getDimensionPixelSize(R.dimen.text_message_padding),
        res.getDimensionPixelSize(R.dimen.padding_call_vertical)
    )
    private var lastMsgPos = -1
    private var expandedItemPosition = -1
    private var lastDeliveredPosition = -1
    private val markwon: Markwon = Markwon.builder(conversationFragment.requireContext())
        .usePlugin(LinkifyPlugin.create())
        .build()
    private val formatter = Formatter(StringBuilder(64), Locale.getDefault())

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
        val previousLast = mInteractions.size - 1
        notifyItemInserted(previousLast)
        if (update) {
            // Find previous last not invalid.
            getPreviousInteractionFromPosition(previousLast)?.let { interactionNotInvalid ->
                notifyItemChanged(mInteractions.lastIndexOf(interactionNotInvalid))
            }
        }
    }

    fun update(editedInteraction: Interaction) {
        if (!editedInteraction.isIncoming && editedInteraction.status === InteractionStatus.SUCCESS)
            notifyItemChanged(lastDeliveredPosition)

        mInteractions.indexOfLast { it.messageId == editedInteraction.messageId }.let {
            if (it == -1) return
            mInteractions[it] = editedInteraction
            notifyItemChanged(it)
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

        val interaction = mInteractions[position] // Get the interaction
        return when (interaction.type) {
            Interaction.InteractionType.CONTACT -> return MessageType.CONTACT_EVENT.ordinal
            Interaction.InteractionType.CALL ->
                if ((interaction as Call).isGroupCall) {
                    MessageType.ONGOING_GROUP_CALL.ordinal
                } else if (interaction.isIncoming) {
                    MessageType.INCOMING_CALL_INFORMATION.ordinal
                } else MessageType.OUTGOING_CALL_INFORMATION.ordinal
            Interaction.InteractionType.TEXT ->
                if (interaction.isIncoming) {
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
            else -> MessageType.INVALID.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val type = MessageType.values()[viewType]
        val v = if (type == MessageType.INVALID) FrameLayout(parent.context)
        else LayoutInflater.from(parent.context).inflate(type.tvLayout, parent, false) as ViewGroup
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
                Interaction.InteractionType.TEXT -> configureForTextMessage(
                    holder,
                    interaction,
                    position
                )
                Interaction.InteractionType.CALL -> configureForCallInfoTextMessage(
                    holder,
                    interaction,
                    position
                )
                Interaction.InteractionType.CONTACT -> configureForContactEvent(holder, interaction)
                Interaction.InteractionType.DATA_TRANSFER -> configureForFileInfo(
                    holder,
                    interaction,
                    position
                )
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
        convColorTint = MaterialColors.compositeARGBWithAlpha(
            color, (MaterialColors.ALPHA_LOW * 255).toInt()
        )
        notifyDataSetChanged()
    }

    private class RecyclerViewContextMenuInfo constructor(val position: Int, val id: Long) :
        ContextMenuInfo

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
        Glide.with(context)
            .load(path)
            .apply(pictureOptions)
            .into(DrawableImageViewTarget(viewHolder.mImage).waitForLayout())
        viewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewHolder.itemView.setBackgroundResource(if (hasFocus) R.drawable.tv_item_selected_background else R.drawable.tv_item_unselected_background)
            viewHolder.mAnswerLayout!!.animate().scaleY(if (hasFocus) 1.1f else 1f)
                .scaleX(if (hasFocus) 1.1f else 1f)
        }

        viewHolder.itemView.setOnClickListener { v: View ->
            try {
                val contentUri = getUriForFile(v.context, path)
                val i = Intent(context, MediaViewerActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, "image/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    conversationFragment.requireActivity(),
                    viewHolder.mImage!!,
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
        viewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewHolder.itemView.setBackgroundResource(
                if (hasFocus) R.drawable.tv_item_selected_background
                else R.drawable.tv_item_unselected_background
            )
            viewHolder.mAudioInfoLayout?.animate()?.scaleY(if (hasFocus) 1.1f else 1f)
                ?.scaleX(if (hasFocus) 1.1f else 1f)
        }

        try {
            val acceptBtn = viewHolder.btnAccept as ImageView
            val refuseBtn = viewHolder.btnRefuse
            acceptBtn.setImageResource(R.drawable.baseline_play_arrow_24)
            val player = MediaPlayer.create(
                context,
                getUriForFile(context, path)
            )
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
                refuseBtn?.setOnClickListener(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player", e)
        }
    }

    private fun configureVideo(viewHolder: ConversationViewHolder, path: File) {
        val context = viewHolder.itemView.context
        viewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewHolder.itemView.setBackgroundResource(
                if (hasFocus) R.drawable.tv_item_selected_background
                else R.drawable.tv_item_unselected_background
            )
            viewHolder.mLayout?.animate()?.scaleY(if (hasFocus) 1.1f else 1f)
                ?.scaleX(if (hasFocus) 1.1f else 1f)
        }

        viewHolder.player?.let {
            viewHolder.player = null
            it.release()
        }
        val video = viewHolder.video ?: return
        val cardLayout = viewHolder.mLayout as CardView
        val player = MediaPlayer.create(context, getUriForFile(context, path)) ?: return
        viewHolder.player = player
        val playBtn =
            ContextCompat.getDrawable(cardLayout.context, R.drawable.baseline_play_arrow_24)!!
                .mutate()
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
            ) {
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

    private fun configureForFileInfo(
        viewHolder: ConversationViewHolder,
        interaction: Interaction,
        position: Int
    ) {
        val context = viewHolder.itemView.context
        val file = interaction as DataTransfer
        val path = presenter.deviceRuntimeService.getConversationPath(file)

        val timeString = TextUtils.timestampToDetailString(context, formatter, file.timestamp)
        viewHolder.mFileTime?.text = timeString
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
            viewHolder.mFileSize?.text = when (val status = file.transferStatus) {
                TransferStatus.TRANSFER_FINISHED -> String.format("%s - %s",
                    Formatter.formatFileSize(context, file.totalSize),
                    TextUtils.getReadableFileTransferStatus(context, status)
                )
                TransferStatus.TRANSFER_ONGOING -> String.format("%s / %s - %s",
                    Formatter.formatFileSize(context, file.bytesProgress),
                    Formatter.formatFileSize(context, file.totalSize),
                    TextUtils.getReadableFileTransferStatus(context, status))
                else -> String.format(
                    Formatter.formatFileSize(context, file.totalSize)
                )
            }
        })
        if (hasPermanentTimeString(file, position)) {
            viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                viewHolder.mMsgDetailTxtPerm?.text =
                    TextUtils.timestampToDetailString(context, formatter, file.timestamp)
            })
            viewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
        } else {
            viewHolder.mMsgDetailTxtPerm?.visibility = View.GONE
        }
        val contact = interaction.contact ?:return
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
        val longPressView = viewHolder.itemView
        longPressView.setOnCreateContextMenuListener { menu: ContextMenu, v: View, menuInfo: ContextMenuInfo? ->
            menu.setHeaderTitle(file.displayName)
            conversationFragment.onCreateContextMenu(menu, v, menuInfo)
            val inflater = conversationFragment.requireActivity().menuInflater
            inflater.inflate(R.menu.conversation_item_actions_file_tv, menu)
            if (!file.isComplete) {
                menu.removeItem(R.id.conv_action_download)
                menu.removeItem(R.id.conv_action_share)
                menu.removeItem(R.id.conv_action_open)
            }
            if (file.isIncoming) menu.removeItem(R.id.conv_action_delete)
        }
        longPressView.setOnLongClickListener { v: View ->
            if (type == MessageType.TransferType.AUDIO || type == MessageType.TransferType.FILE) {
                conversationFragment.updatePosition(viewHolder.bindingAdapterPosition)
                if (file.isIncoming) {
                    longPressView.background.setTint(context.getColor(R.color.grey_500))
                } else {
                    longPressView.background.setTint(convColorTint)
                }
            }
            mCurrentLongItem =
                RecyclerViewContextMenuInfo(viewHolder.bindingAdapterPosition, v.id.toLong())
            false
        }
        when (type) {
            MessageType.TransferType.IMAGE -> { configureImage(viewHolder, path) }
            MessageType.TransferType.VIDEO -> { configureVideo(viewHolder, path) }
            MessageType.TransferType.AUDIO -> { configureAudio(viewHolder, path) }
            else -> {
                viewHolder.itemView.onFocusChangeListener =
                    View.OnFocusChangeListener { v, hasFocus ->
                        viewHolder.itemView.setBackgroundResource(
                            if (hasFocus) R.drawable.tv_item_selected_background
                            else R.drawable.tv_item_unselected_background
                        )
                        viewHolder.mFileInfoLayout?.animate()
                            ?.scaleY(if (hasFocus) 1.1f else 1f)
                            ?.scaleX(if (hasFocus) 1.1f else 1f)
                    }
                val status = file.transferStatus
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
                    TransferStatus.FILE_AVAILABLE, TransferStatus.TRANSFER_AWAITING_HOST -> {
                        viewHolder.mFileDownloadButton?.let {
                            it.visibility = View.VISIBLE
                            it.setOnClickListener { presenter.acceptFile(file) }
                        }
                    }
                    else -> {
                        viewHolder.mFileDownloadButton?.visibility = View.GONE
                        if (status == TransferStatus.TRANSFER_ONGOING) {
                            viewHolder.progress?.max = (file.totalSize / 1024).toInt()
                            viewHolder.progress?.setProgress(
                                (file.bytesProgress / 1024).toInt(), true
                            )
                            viewHolder.progress?.show()
                        } else {
                            viewHolder.progress?.hide()
                        }
                        viewHolder.mFileInfoLayout?.setOnClickListener { presenter.openFile(file) }
                    }
                }
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
    private fun configureForTextMessage(
        convViewHolder: ConversationViewHolder,
        interaction: Interaction,
        position: Int
    ) {
        val textMessage = interaction as TextMessage
        val context = convViewHolder.itemView.context
        val contact = textMessage.contact ?: return
        val account = interaction.account ?: return
        val isDeleted = textMessage.body.isNullOrEmpty()
        val message = textMessage.body?.trim() ?: ""
        val longPressView = convViewHolder.itemView
        val isTimeShown = hasPermanentTimeString(textMessage, position)
        val msgSequenceType = getMsgSequencing(position, isTimeShown)
        val msgTxt = convViewHolder.mMsgTxt ?: return
        val answerLayout = convViewHolder.mAnswerLayout
        val avatar = convViewHolder.mAvatar
        val peerDisplayName = convViewHolder.mPeerDisplayName

        convViewHolder.itemView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                convViewHolder.itemView.setBackgroundResource(
                    if (hasFocus) R.drawable.tv_item_selected_background
                    else R.drawable.tv_item_unselected_background
                )
                msgTxt.animate().scaleY(if (hasFocus) 1.1f else 1f)
                    .scaleX(if (hasFocus) 1.1f else 1f)
            }
        // Manage long press.
        longPressView.background.setTintList(null)
        longPressView.setOnCreateContextMenuListener {
            menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo? ->
            val date = Date(interaction.timestamp)
            val dateFormat =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            menu.setHeaderTitle(dateFormat.format(date))
            conversationFragment.onCreateContextMenu(menu, v!!, menuInfo)
            val inflater = conversationFragment.requireActivity().menuInflater
            inflater.inflate(R.menu.conversation_item_actions_messages_tv, menu)
            if (interaction.status === InteractionStatus.SENDING) {
                menu.removeItem(R.id.conv_action_delete)
            } else if (textMessage.isIncoming) {
                menu.removeItem(R.id.conv_action_delete)
                menu.removeItem(R.id.conv_action_cancel_message)
            } else {
                // Message is sent and outgoing
                menu.findItem(R.id.conv_action_delete).setTitle(R.string.menu_message_delete)
                menu.removeItem(R.id.conv_action_cancel_message)
            }
        }
        longPressView.setOnLongClickListener { v: View ->
            if (expandedItemPosition == position) {
                expandedItemPosition = -1
            }
            conversationFragment.updatePosition(convViewHolder.bindingAdapterPosition)
            mCurrentLongItem = RecyclerViewContextMenuInfo(
                convViewHolder.bindingAdapterPosition, v.id.toLong()
            )
            false
        }
        // Manage background.
        // Standard message, incoming or outgoing and first, single or last.
        val resIndex =
            msgSequenceType.ordinal + (if (textMessage.isIncoming) 1 else 0) * 4
        msgTxt.background = ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
        if (convColor != 0 && !textMessage.isIncoming) {
            msgTxt.background.setTint(convColor)
        }
        // Manage classic message
        msgTxt.background.alpha = 255
        msgTxt.textSize = 16f
        msgTxt.setPadding(hPadding, vPadding, hPadding, vPadding)
        msgTxt.text = markwon.toMarkdown(message)
        val endOfSeq =
            msgSequenceType == SequenceType.LAST || msgSequenceType == SequenceType.SINGLE
        // Only show the peer avatar if it is a group conversation
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
        } // Do not show the avatar if it is a one to one conversation.
        else avatar?.visibility = View.GONE
        // Apply a bottom margin to the global layout if end of sequence needed.
        val startOfSeq =
            msgSequenceType == SequenceType.FIRST || msgSequenceType == SequenceType.SINGLE
        convViewHolder.mItem?.let { setBottomMargin(it, if (startOfSeq) 8 else 0) }
        // Show the name of the contact if it is a group conversation
        peerDisplayName?.apply {
            if (presenter.isGroup() && endOfSeq) {
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
        // Manage deleted message.
        if (isDeleted) {
            if (textMessage.isIncoming) {
                convViewHolder.compositeDisposable.add(
                    presenter.contactService
                        .observeContact(account, contact, false)
                        .observeOn(DeviceUtils.uiScheduler)
                        .subscribe { username ->
                            msgTxt.text = String.format(
                                context.getString(R.string.conversation_message_deleted),
                                username
                            )
                        }
                )
            } else {
                msgTxt.text = String.format(
                    context.getString(R.string.conversation_message_deleted),
                    context.getString(R.string.conversation_info_contact_you)
                )
            }
            // Hide the link preview
            answerLayout?.visibility = View.GONE
            if (convColor != 0 && !textMessage.isIncoming) {
                msgTxt.background.setTint(convColor)
            }
            msgTxt.textSize = 14f
            longPressView.setOnLongClickListener(null)
            return
        }
        // Manage emoji message
        if (isOnlyEmoji(message)) {
            msgTxt.background.alpha = 0
            msgTxt.textSize = 32.0f
            msgTxt.setPadding(0, 0, 0, 0)
        }
        if (isTimeShown) {
            convViewHolder.compositeDisposable.add(
                timestampUpdateTimer.subscribe { t: Long? ->
                val timeSeparationString =
                    TextUtils.timestampToDetailString(
                        context,
                        formatter,
                        textMessage.timestamp
                    )
                convViewHolder.mMsgDetailTxtPerm?.text = timeSeparationString
            })
            convViewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
        } else {
            convViewHolder.mMsgDetailTxtPerm?.visibility = View.GONE
            val isExpanded = position == expandedItemPosition
            if (isExpanded) {
                convViewHolder.compositeDisposable.add(
                    timestampUpdateTimer.subscribe { t: Long? ->
                    val timeSeparationString = TextUtils.timestampToDetailString(
                        context,
                        formatter,
                        textMessage.timestamp
                    )
                    convViewHolder.mMsgDetailTxt?.text = timeSeparationString
                })
            }
            convViewHolder.setItemViewExpansionState(isExpanded)
            convViewHolder.itemView.setOnClickListener { v: View? ->
                if (convViewHolder.animator != null && convViewHolder.animator!!.isRunning)
                {
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

    private fun configureForContactEvent(
        viewHolder: ConversationViewHolder,
        interaction: Interaction
    ) {
        val event = interaction as ContactEvent
        viewHolder.mMsgTxt?.setText(
            when (event.event) {
                ContactEvent.Event.ADDED -> R.string.hist_contact_added
                ContactEvent.Event.INVITED -> R.string.hist_contact_invited
                ContactEvent.Event.REMOVED -> R.string.hist_contact_left
                ContactEvent.Event.BLOCKED -> R.string.hist_contact_blocked
                ContactEvent.Event.INCOMING_REQUEST -> R.string.hist_invitation_received
                else -> R.string.hist_contact_added
            }
        )
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe { t: Long? ->
            val timeSeparationString = TextUtils.timestampToDetailString(
                viewHolder.itemView.context,
                formatter,
                event.timestamp
            )
            viewHolder.mMsgDetailTxt?.text = timeSeparationString
        })
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     */
    private fun configureForCallInfoTextMessage(
        convViewHolder: ConversationViewHolder,
        interaction: Interaction,
        position: Int
    ) {
        val recycle: StringBuilder = StringBuilder()
        val context = convViewHolder.itemView.context
        val call = interaction as Call

        // Reset the scale of the icon
        convViewHolder.mIcon?.scaleX = 1f
        // Set the call message focusable.
        convViewHolder.itemView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            convViewHolder.itemView.setBackgroundResource(
                if (hasFocus) R.drawable.tv_item_selected_background
                else R.drawable.tv_item_unselected_background
            )
            convViewHolder.mCallInfoLayout!!.animate()
                .scaleY(if (hasFocus) 1.1f else 1f)
                .scaleX(if (hasFocus) 1.1f else 1f)
        }
        // When a call is occurring (between members) but you are not in it, a message is
        // displayed in conversation to inform the user about the call and invite him to join.
        if (call.isGroupCall) {
            val callStartedMsg = interaction as Call
            val isTimeShown = hasPermanentTimeString(callStartedMsg, position)
            val msgSequenceType = getMsgSequencing(position, isTimeShown)
            val peerDisplayName = convViewHolder.mPeerDisplayName
            val account = interaction.account ?: return
            val contact = callStartedMsg.contact ?: return
            val callAcceptLayout = convViewHolder.mCallAcceptLayout ?: return
            val avatar = convViewHolder.mAvatar
            val callInfoText = convViewHolder.mCallInfoText ?: return
            val acceptCallAudioButton =
                convViewHolder.mAcceptCallAudioButton ?: return
            val acceptCallVideoButton =
                convViewHolder.mAcceptCallVideoButton ?: return

            if (callStartedMsg.isIncoming) {
                // Show the avatar of the caller
                // depending on the position of the message.
                val endOfSeq = msgSequenceType == SequenceType.LAST
                            || msgSequenceType == SequenceType.SINGLE
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
                // We can call ourselves in a group call with different devices.
                // Set the message to the left when it is incoming.
                convViewHolder.mGroupCallLayout?.gravity = Gravity.START
                // Show the name of the contact.
                peerDisplayName?.apply {
                    if (presenter.isGroup() && (msgSequenceType == SequenceType.SINGLE
                                || msgSequenceType == SequenceType.LAST)
                    ) {
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
                acceptCallAudioButton.setColorFilter(
                    context.getColor(R.color.accept_call_button)
                )
                acceptCallVideoButton.setColorFilter(
                    context.getColor(R.color.accept_call_button)
                )
                // Set the background to the call started message.
                val resIndex = msgSequenceType.ordinal + 4
                callAcceptLayout.background =
                    ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
                callAcceptLayout.background.setTint(
                    context.getColor(R.color.conversation_secondary_background)
                )
            } else {
                // Set the message to the right because it is outgoing.
                convViewHolder.mGroupCallLayout?.gravity = Gravity.END
                // Hide the name of the contact.
                peerDisplayName?.visibility = View.GONE
                avatar?.visibility = View.GONE
                callInfoText.setTextColor(
                    context.getColor(
                        R.color.text_color_primary_dark
                    )
                )
                acceptCallAudioButton.setColorFilter(
                    context.getColor(R.color.white)
                )
                acceptCallVideoButton.setColorFilter(
                    context.getColor(R.color.white)
                )
                // Set the background to the call started message.
                val resIndex = msgSequenceType.ordinal
                callAcceptLayout.background =
                    ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
                callAcceptLayout.background.setTint(convColor)
            }
            callAcceptLayout.setPadding(callPadding)
            callAcceptLayout.apply {
                // Accept with audio only
                acceptCallAudioButton.setOnClickListener {
                    call.confId?.let { presenter.goToGroupCall(false) }
                }
                // Accept call with video
                acceptCallVideoButton.setOnClickListener {
                    call.confId?.let { presenter.goToGroupCall(true) }
                }
            }
            // Apply a bottom margin to the global layout if end of sequence needed.
            val startOfSeq =
                msgSequenceType == SequenceType.FIRST ||
                        msgSequenceType == SequenceType.SINGLE
            convViewHolder.mItem?.let { setBottomMargin(it, if (startOfSeq) 8 else 0) }
        } else {
            // When it is not a group call
            // Remove the tint
            convViewHolder.mCallInfoLayout?.background?.setTintList(null)

                val callMessage = interaction as Call
                val isTimeShown = hasPermanentTimeString(callMessage, position)
                val msgSequenceType = getMsgSequencing(position, isTimeShown)
                val callInfoLayout = convViewHolder.mCallInfoLayout ?: return
                callInfoLayout.background?.setTintList(null)
                val callIcon = convViewHolder.mIcon ?: return
                callIcon.drawable?.setTintList(null)
                val typeCall = convViewHolder.mHistTxt ?: return
                val detailCall = convViewHolder.mHistDetailTxt ?: return
                val resIndex: Int
                val typeCallTxt: String
                val peerDisplayName = convViewHolder.mPeerDisplayName
                val account = interaction.account ?: return
                val contact = callMessage.contact ?: return

                // Manage the update of the timestamp
                if (isTimeShown) {
                    convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe {
                        convViewHolder.mMsgDetailTxtPerm?.text =
                            TextUtils.timestampToDetailString(
                                context, formatter, call.timestamp
                            )
                    })
                    convViewHolder.mMsgDetailTxtPerm?.visibility = View.VISIBLE
                } else convViewHolder.mMsgDetailTxtPerm?.visibility = View.GONE

                // After a call, a message is displayed with call information.
                // Manage the call message layout.
                if (callMessage.isIncoming) {
                    // Same background for incoming calls.
                    resIndex = msgSequenceType.ordinal + 4
                    // Set the color of the time duration.
                    detailCall.setTextColor(
                        convViewHolder.itemView.context.getColor(R.color.colorOnSurface)
                    )
                    // Set the call message color.
                    typeCall.setTextColor(
                        convViewHolder.itemView.context.getColor(R.color.colorOnSurface)
                    )
                    if (callMessage.isMissed) { // Call incoming missed.
                        callIcon.setImageResource(R.drawable.baseline_missed_call_16)
                        // Set the drawable color to red because it is missed.
                        callIcon.drawable.setTint(context.getColor(R.color.call_missed))
                        typeCallTxt = context.getString(R.string.notif_missed_incoming_call)
                    } else { // Call incoming not missed.
                        callIcon.setImageResource(R.drawable.baseline_incoming_call_16)
                        callIcon.drawable.setTint(context.getColor(R.color.colorOnSurface))
                        typeCallTxt = context.getString(R.string.notif_incoming_call)
                    }
                    // Put the message to the left because it is incoming.
                    (convViewHolder.mCallLayout as LinearLayout?)?.gravity = Gravity.START
                    // Show the name of the contact if it is a group conversation
                    val endOfSeq = msgSequenceType == SequenceType.SINGLE
                            || msgSequenceType == SequenceType.LAST
                    peerDisplayName?.apply {
                        if (presenter.isGroup() && endOfSeq) {
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
                } else {
                    // Same background for outgoing calls.
                    resIndex = msgSequenceType.ordinal
                    typeCall.setTextColor(
                        convViewHolder.itemView.context.getColor(
                            R.color.call_text_outgoing_message
                        )
                    )
                    // Set the color of the time duration.
                    detailCall.setTextColor(
                        convViewHolder.itemView.context.getColor(
                            R.color.call_text_outgoing_message
                        )
                    )
                    if (callMessage.isMissed) { // Outgoing call missed.
                        callIcon.setImageResource(R.drawable.baseline_missed_call_16)
                        // Set the drawable color to red because it is missed.
                        callIcon.drawable.setTint(context.getColor(R.color.call_missed))
                        typeCallTxt = context.getString(R.string.notif_missed_outgoing_call)
                        // Flip the photo upside down to show a "missed outgoing call".
                        callIcon.scaleX = -1f
                    } else { // Outgoing call not missed.
                        callIcon.setImageResource(R.drawable.baseline_outgoing_call_16)
                        callIcon.drawable.setTint(
                            context.getColor(R.color.call_drawable_color)
                        )
                        typeCallTxt = context.getString(R.string.notif_outgoing_call)
                    }
                    // Put the message to the right because it is outgoing.
                    (convViewHolder.mCallLayout as LinearLayout?)?.gravity = Gravity.END
                }
                callInfoLayout.background =
                    ContextCompat.getDrawable(context, msgBGLayouts[resIndex])
                callInfoLayout.setPadding(callPadding)
                // Manage background to convColor if it is outgoing.
                if (convColor != 0 && !callMessage.isIncoming) {
                    callInfoLayout.background.setTint(convColor)
                }
                typeCall.text = typeCallTxt
                // Add the call duration if not null.
                detailCall.text =
                    if (callMessage.duration != 0L) {
                        String.format(
                            context.getString(R.string.call_duration),
                            DateUtils.formatElapsedTime(
                                recycle, callMessage.duration!! / 1000
                            )
                        ).let { " - $it" }
                    } else null
                // Apply a bottom margin to the global layout if end of sequence needed.
                val startOfSeq =
                    msgSequenceType == SequenceType.FIRST ||
                            msgSequenceType == SequenceType.SINGLE
                convViewHolder.mItem?.let { setBottomMargin(it, if (startOfSeq) 8 else 0) }

                // Do not show the avatar if it is a one to one conversation.
                val avatar = convViewHolder.mAvatar
                avatar?.visibility = View.GONE
                // Only show the peer avatar if it is a group conversation
                if (presenter.isGroup()) {
                    val endOfSeq =
                        msgSequenceType == SequenceType.LAST
                                || msgSequenceType == SequenceType.SINGLE
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

    private fun getMsgSequencing(i: Int, isTimeShown: Boolean): SequenceType {
        val msg = mInteractions[i]
        if (isAlwaysSingleMsg(msg)) {
            return SequenceType.SINGLE
        }

        val prevMsg = getPreviousInteractionFromPosition(i)
        val nextMsg = getNextInteractionFromPosition(i)

        if (mInteractions.size == 1 || i == 0) {
            if (mInteractions.size == i + 1) {
                return SequenceType.SINGLE
            }

            if (nextMsg != null) {
                return if (isSeqBreak(msg, nextMsg) || hasPermanentTimeString(nextMsg, i + 1)) {
                    SequenceType.SINGLE
                } else {
                    SequenceType.FIRST
                }
            }
        } else if (nextMsg == null) { // If this is the last interaction.
            if (prevMsg != null) {
                return if (isSeqBreak(msg, prevMsg) || isTimeShown) {
                    SequenceType.SINGLE
                } else SequenceType.LAST
            }
        }

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

    private fun hasPermanentTimeString(msg: Interaction, position: Int): Boolean {
        val prevMsg = getPreviousInteractionFromPosition(position)
        return prevMsg != null && msg.timestamp - prevMsg.timestamp > 10 * DateUtils.MINUTE_IN_MILLIS
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
            R.drawable.textmsg_bg_in,
            R.drawable.textmsg_bg_out_reply,
            R.drawable.textmsg_bg_out_reply_first,
            R.drawable.textmsg_bg_in_reply,
            R.drawable.textmsg_bg_in_reply_first,
            R.drawable.call_bg_missed_in_first,
            R.drawable.call_bg_missed_in_middle,
            R.drawable.call_bg_missed_in_last,
            R.drawable.call_bg_missed,
            R.drawable.call_bg_missed_out_first,
            R.drawable.call_bg_missed_out_middle,
            R.drawable.call_bg_missed_out_last,
            R.drawable.call_bg_missed
        )

        private fun setBottomMargin(view: View, value: Int) {
            val targetSize = (value * view.context.resources.displayMetrics.density).toInt()
            val params = view.layoutParams as MarginLayoutParams
            params.bottomMargin = targetSize
        }

        private fun isSeqBreak(first: Interaction, second: Interaction): Boolean =
            isOnlyEmoji(first.body) != isOnlyEmoji(second.body)
                    || first.isIncoming != second.isIncoming
                    || (first.type !== Interaction.InteractionType.TEXT
                        && (first.type !== Interaction.InteractionType.CALL))
                    || (second.type !== Interaction.InteractionType.TEXT
                        && (second.type !== Interaction.InteractionType.CALL))
                    || first.contact != second.contact

        private fun isAlwaysSingleMsg(msg: Interaction): Boolean =
            (msg.type !== Interaction.InteractionType.TEXT
                    && msg.type !== Interaction.InteractionType.CALL || isOnlyEmoji(msg.body))
    }

}