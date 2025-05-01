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
package cx.ring.client

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomappbar.BottomAppBar
import cx.ring.R
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.DeviceUtils
import kotlin.math.abs
import net.jami.utils.Log

class MediaViewerFragment : Fragment() {
    private var mUri: Uri? = null
    private var videoView: android.widget.VideoView? = null
    private var mediaController: android.widget.MediaController? = null
    private var thumbWidth = 0
    private var thumbHeight = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUri = requireActivity().intent.data
        postponeEnterTransition()

        // Get thumbnail dimensions from intent
        val intent = requireActivity().intent
        thumbWidth = intent.getIntExtra("video_width", 0)
        thumbHeight = intent.getIntExtra("video_height", 0)
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_media_viewer, container, false)

        val imageView = view.findViewById<View>(R.id.image)
        videoView = view.findViewById(R.id.video_view)
        val bottomAppBar = view.findViewById<BottomAppBar>(R.id.bottomAppBar)
        val shareButton = view.findViewById<Button>(R.id.shareBtn)
        val uri = mUri ?: return view

        val mimeType = AndroidFileUtils.getMimeType(requireContext().contentResolver, uri)
        val edgeThreshold = ViewConfiguration.get(requireContext()).scaledEdgeSlop

        if (mimeType?.startsWith("video/") == true) {
            if (thumbWidth > 0 && thumbHeight > 0) {
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val aspectRatio = thumbWidth.toFloat() / thumbHeight
                val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                val (w, h) = if (isLandscape) {
                    val height = screenHeight
                    val width = (height * aspectRatio).toInt().coerceAtMost(screenWidth)
                    width to height
                } else {
                    val width = screenWidth
                    val height = (width / aspectRatio).toInt().coerceAtMost(screenHeight)
                    width to height
                }

                Log.d("MediaViewerFragment", "Thumbnail layout: $w x $h")
                videoView?.layoutParams = FrameLayout.LayoutParams(w, h).apply {
                    gravity = Gravity.CENTER
                }
            }

            imageView.visibility = View.GONE
            videoView?.visibility = View.VISIBLE

            videoView?.apply {
                mediaController = android.widget.MediaController(requireContext()).apply {
                    setAnchorView(this@apply)
                }
                setMediaController(mediaController)
                setVideoURI(uri)

                setOnPreparedListener { mp ->
                    val videoWidth = mp.videoWidth
                    val videoHeight = mp.videoHeight
                    val thumbRatio = calculateAspectRatio(thumbWidth, thumbHeight)
                    val videoRatio = calculateAspectRatio(videoWidth, videoHeight)

                    Log.d("MediaViewerFragment", "Video size: $videoWidth x $videoHeight")
                    Log.d("MediaViewerFragment", "ThumbRatio: $thumbRatio, VideoRatio: $videoRatio")

                    if (abs(thumbRatio - videoRatio) > 0.01f) {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val screenHeight = resources.displayMetrics.heightPixels
                        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                        val (w, h) = if (isLandscape) {
                            val height = screenHeight
                            val width = (height * videoRatio).toInt().coerceAtMost(screenWidth)
                            width to height
                        } else {
                            val width = screenWidth
                            val height = (width / videoRatio).toInt().coerceAtMost(screenHeight)
                            width to height
                        }

                        Log.d("MediaViewerFragment", "Adjusting layout: $w x $h")
                        layoutParams = FrameLayout.LayoutParams(w, h).apply {
                            gravity = Gravity.CENTER
                        }
                    } else {
                        Log.d("MediaViewerFragment", "Aspect ratio matches; no layout adjustment needed")
                    }

                    start()

                    view.findViewById<ViewGroup>(R.id.video_container)?.viewTreeObserver
                        ?.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                            override fun onPreDraw(): Boolean {
                                view.viewTreeObserver.removeOnPreDrawListener(this)
                                startPostponedEnterTransition()
                                return true
                            }
                        })
                }
            }
        } else {
            videoView?.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(imageView as android.widget.ImageView)
            startPostponedEnterTransition()
        }

        videoView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val screenWidth = resources.displayMetrics.widthPixels
                if (event.x < edgeThreshold || event.x > screenWidth - edgeThreshold) {
                    return@setOnTouchListener true
                }
            }
            false
        }

        bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.conv_action_share -> AndroidFileUtils.shareFile(requireContext(), uri)
                R.id.conv_action_download -> startSaveFile(uri)
                R.id.conv_action_open -> openFile(uri)
            }
            true
        }

        shareButton.setOnClickListener {
            AndroidFileUtils.shareFile(requireContext(), uri)
        }

        return view
    }

    private fun calculateAspectRatio(width: Int, height: Int): Float {
        return if (height != 0) width.toFloat() / height else 1f
    }

    override fun onPause() {
        videoView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mUri?.let { uri ->
            videoView?.apply {
                if (!isPlaying) {
                    start()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SAVE_FILE) {
            data?.data?.let { uri ->
                AndroidFileUtils.copyUri(requireContext().contentResolver, mUri!!, uri)
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ Toast.makeText(context, R.string.file_saved_successfully, Toast.LENGTH_SHORT).show() })
                    { Toast.makeText(context, R.string.generic_error, Toast.LENGTH_SHORT).show() }
            }
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun startSaveFile(uri: Uri) {
        try {
            val name = uri.getQueryParameter("displayName") ?: ""
            val downloadFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = requireContext().contentResolver.getType(uri.buildUpon().appendPath(name).build())
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, name)
            }
            startActivityForResult(downloadFileIntent, REQUEST_CODE_SAVE_FILE)
        } catch (e: Exception) {
            //Log.i(TAG, "No app detected for saving files.")
        }
    }

    private fun openFile(uri: Uri) {
        val name = uri.getQueryParameter("displayName") ?: ""
        AndroidFileUtils.openFile(requireContext(), uri, name)
    }

    companion object {
        private const val REQUEST_CODE_SAVE_FILE = 1003
    }

}
