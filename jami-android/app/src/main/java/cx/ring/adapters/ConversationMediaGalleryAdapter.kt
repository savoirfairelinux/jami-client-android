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

import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.text.format.Formatter
import android.util.Log
import android.view.*
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import cx.ring.R
import cx.ring.client.MediaViewerActivity
import cx.ring.databinding.ItemMediaFileBinding
import cx.ring.databinding.ItemMediaImageBinding
import cx.ring.databinding.ItemMediaVideoBinding
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
    private val interactions = ArrayList<DataTransfer>()

    fun addSearchResults(results: List<DataTransfer>) {
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

    override fun getItemViewType(position: Int): Int {
        val interaction = interactions[position]
        return when {
            !interaction.isComplete -> R.layout.item_media_file
            interaction.isPicture -> R.layout.item_media_image
            interaction.isVideo -> R.layout.item_media_video
            interaction.isAudio -> R.layout.item_media_video
            else -> R.layout.item_media_file
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationMediaViewHolder =
        when (viewType) {
            R.layout.item_media_image -> ConversationMediaViewHolder(image=ItemMediaImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_media_video -> ConversationMediaViewHolder(video= ItemMediaVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> ConversationMediaViewHolder(file= ItemMediaFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(conversationViewHolder: ConversationMediaViewHolder, position: Int) {
        val interaction = interactions[position]
        conversationViewHolder.compositeDisposable.clear()
        configureForFileInfo(conversationViewHolder, interaction, position)
    }

    override fun onViewRecycled(holder: ConversationMediaViewHolder) {
        holder.compositeDisposable.clear()
    }
    private fun configureImage(binding: ItemMediaImageBinding, path: File, displayName: String) {
        Glide.with(fragment)
            .load(path)
            .into(binding.image)
        binding.image.setOnClickListener { v: View ->
            try {
                val contentUri = getUriForFile(v.context, ContentUri.AUTHORITY_FILES, path, displayName)
                val i = Intent(v.context, MediaViewerActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, "image/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.requireActivity(), binding.image, "picture")
                fragment.startActivityForResult(i, 3006, options.toBundle())
            } catch (e: Exception) {
                Log.w(TAG, "Can't open picture", e)
            }
        }
    }

    private fun configureAudio(viewHolder: ConversationMediaViewHolder, path: File) {
        val context = viewHolder.itemView.context
        val b = viewHolder.video!!
        try {
            b.playBtn.setImageResource(R.drawable.baseline_play_arrow_24)
            val player = MediaPlayer.create(context, getUriForFile(context, ContentUri.AUTHORITY_FILES, path))
            viewHolder.player = player
            if (player != null) {
                player.setOnCompletionListener { mp: MediaPlayer ->
                    mp.seekTo(0)
                    b.playBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                }
                b.playBtn.setOnClickListener {
                    if (player.isPlaying) {
                        player.pause()
                        b.playBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                    } else {
                        player.start()
                        b.playBtn.setImageResource(R.drawable.baseline_pause_24)
                    }
                }
            } else {
                b.playBtn.setOnClickListener(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player", e)
        }
    }

    private fun configureVideo(viewHolder: ConversationMediaViewHolder, path: File) {
        val context = viewHolder.itemView.context
        viewHolder.player?.let {
            viewHolder.player = null
            it.release()
        }
        val video = viewHolder.video ?: return
        val player = MediaPlayer.create(context, getUriForFile(context, ContentUri.AUTHORITY_FILES, path)) ?: return
        viewHolder.player = player
        player.setOnPreparedListener { mediaPlayer ->
            val videoRatio = mediaPlayer.videoWidth / mediaPlayer.videoHeight.toFloat()
            val screenRatio = video.video.width / video.video.height.toFloat()
            val scaleX = videoRatio / screenRatio
            if (scaleX.isNaN() || scaleX.isInfinite() || scaleX <= 0f) {
                video.video.scaleX = 1f
                video.video.scaleY = 1f
            } else if (scaleX >= 1f) {
                video.video.scaleX = scaleX
            } else {
                video.video.scaleY = 1f / scaleX
            }
        }
        player.setOnCompletionListener { mp: MediaPlayer ->
            if (mp.isPlaying) mp.pause()
            mp.seekTo(1)
            video.playBtn.isVisible = true
        }
        if (video.video.isAvailable) {
            if (viewHolder.surface == null) {
                viewHolder.surface = Surface(video.video.surfaceTexture)
            }
            player.setSurface(viewHolder.surface)
        }
        video.video.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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
        video.root.setOnClickListener {
            try {
                if (player.isPlaying) {
                    player.pause()
                    video.playBtn.isVisible = true
                } else {
                    player.start()
                    video.playBtn.isVisible = false
                }
            } catch (e: Exception) {
                // Left blank
            }
        }
        player.seekTo(1)
    }

    private fun configureFile(viewHolder: ConversationMediaViewHolder, file: DataTransfer) {
        val fileBinding = viewHolder.file ?: return
        fileBinding.name.text = file.body
        fileBinding.size.text = Formatter.formatFileSize(fileBinding.root.context, file.totalSize)
    }

    private fun configureForFileInfo(viewHolder: ConversationMediaViewHolder, file: DataTransfer, position: Int) {
        Log.w(TAG, "configureForFileInfo $position")
        val path = deviceRuntimeService.getConversationPath(file)
        if (file.isComplete) {
            when {
                file.isPicture -> configureImage(viewHolder.image!!, path, file.body!!)
                file.isAudio -> configureAudio(viewHolder, path)
                file.isVideo -> configureVideo(viewHolder, path)
                else -> configureFile(viewHolder, file)
            }
        } else {
            configureFile(viewHolder, file)
            Log.w(TAG, "configureForFileInfo not complete")
        }
    }

    companion object {
        private val TAG = ConversationMediaGalleryAdapter::class.simpleName!!
    }

}