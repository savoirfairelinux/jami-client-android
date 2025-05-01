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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomappbar.BottomAppBar
import cx.ring.R
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.DeviceUtils

class MediaViewerFragment : Fragment() {
    private var mUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUri = requireActivity().intent.data
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_media_viewer, container, false)
        val imageView = view.findViewById<View>(R.id.image)
        val videoView = view.findViewById<android.widget.VideoView>(R.id.video_view)
        val bottomAppBar = view.findViewById<BottomAppBar>(R.id.bottomAppBar)
        val shareButton = view.findViewById<Button>(R.id.shareBtn)
        val uri = mUri ?: return view
        val mimeType = AndroidFileUtils.getMimeType(requireContext().contentResolver, uri)

        if (mimeType?.startsWith("video/") == true) {
            imageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            videoView.setVideoURI(uri)
            videoView.setMediaController(null)
            videoView.start()
        } else {
            videoView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(imageView as android.widget.ImageView)
        }

        bottomAppBar.setOnMenuItemClickListener { l ->
            when (l.itemId) {
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
