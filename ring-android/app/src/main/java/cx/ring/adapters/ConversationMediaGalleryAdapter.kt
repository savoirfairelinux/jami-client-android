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

import android.content.Intent
import android.util.Log
import android.view.*
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider.getUriForFile
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.client.MediaViewerActivity
import cx.ring.databinding.ItemConvMediaBinding
import cx.ring.fragments.ConversationGalleryFragment
import cx.ring.utils.*
import cx.ring.viewholders.ConversationMediaViewHolder
import net.jami.model.*
import net.jami.services.DeviceRuntimeService
import java.io.File
import java.util.*

class ConversationMediaGalleryAdapter(
    val fragment: ConversationGalleryFragment,
    val deviceRuntimeService: DeviceRuntimeService
) : RecyclerView.Adapter<ConversationMediaViewHolder>() {
    private val interactions = ArrayList<Interaction>()

    fun addSearchResults(results: List<Interaction>) {
        val oldSize = interactions.size
        interactions.addAll(results)
        notifyItemRangeInserted(oldSize, results.size)
    }

    fun clearSearchResults() {
        interactions.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = interactions.size

    override fun getItemId(position: Int): Long = interactions[position].id.toLong()

    override fun getItemViewType(position: Int): Int = R.layout.item_conv_media

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationMediaViewHolder =
        ConversationMediaViewHolder(ItemConvMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(conversationViewHolder: ConversationMediaViewHolder, position: Int) {
        val interaction = interactions[position]
        conversationViewHolder.compositeDisposable.clear()
        configureForFileInfo(conversationViewHolder, interaction, position)
    }

    override fun onViewRecycled(holder: ConversationMediaViewHolder) {
        holder.compositeDisposable.clear()
    }
    private fun configureImage(viewHolder: ConversationMediaViewHolder, path: File) {
        val context = viewHolder.itemView.context
        GlideApp.with(context)
            .load(path)
            .into(viewHolder.binding.image)
        viewHolder.binding.image.setOnClickListener { v: View ->
            try {
                val contentUri = getUriForFile(v.context, ContentUriHandler.AUTHORITY_FILES, path)
                val i = Intent(context, MediaViewerActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, "image/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.requireActivity(), viewHolder.binding.image, "picture")
                fragment.startActivityForResult(i, 3006, options.toBundle())
            } catch (e: Exception) {
                Log.w(TAG, "Can't open picture", e)
            }
        }
    }

    private fun configureAudio(viewHolder: ConversationMediaViewHolder, path: File) {
        /*val context = viewHolder.itemView.context
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
                    Observable.interval(1L, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
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
        }*/
    }

    private fun configureVideo(viewHolder: ConversationMediaViewHolder, path: File) {
        /*val context = viewHolder.itemView.context
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
        player.seekTo(1)*/
    }

    private fun configureForFileInfo(viewHolder: ConversationMediaViewHolder, interaction: Interaction, position: Int) {
        Log.w(TAG, "configureForFileInfo $position")
        val file = interaction as DataTransfer
        val path = deviceRuntimeService.getConversationPath(file)
        if (file.isComplete) {
            when {
                file.isPicture -> configureImage(viewHolder, path)
                file.isAudio -> configureAudio(viewHolder, path)
                file.isVideo -> configureVideo(viewHolder, path)
            }
        } else {
            Log.w(TAG, "configureForFileInfo not complete")
        }
        /*val timeString = TextUtils.timestampToDetailString(viewHolder.itemView.context, file.timestamp)
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
            if (type == MessageType.TransferType.AUDIO || type == MessageType.TransferType.FILE) {
                conversationFragment.updatePosition(viewHolder.adapterPosition)
                longPressView.background.setTint(res.getColor(R.color.grey_500))
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        viewHolder.progress?.setProgress((file.bytesProgress / 1024).toInt(), true)
                    } else {
                        viewHolder.progress?.progress = (file.bytesProgress / 1024).toInt()
                    }
                    viewHolder.progress?.show()
                } else {
                    viewHolder.progress?.hide()
                }
            }
        }*/
    }

    companion object {
        private val TAG = ConversationMediaGalleryAdapter::class.simpleName!!
    }

}