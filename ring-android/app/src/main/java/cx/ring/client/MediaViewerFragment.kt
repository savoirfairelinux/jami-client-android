/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.client

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.resource.bitmap.CenterInside
import cx.ring.R
import cx.ring.utils.GlideApp
import cx.ring.utils.GlideOptions

/**
 * A placeholder fragment containing a simple view.
 */
class MediaViewerFragment : Fragment() {
    private var mUri: Uri? = null
    private var mImage: ImageView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_media_viewer, container, false) as ViewGroup
        mImage = view.findViewById(R.id.image)
        showImage()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mImage = null
    }

    override fun onStart() {
        super.onStart()
        val activity = activity ?: return
        mUri = activity.intent.data
        showImage()
    }

    private fun showImage() {
        mUri?.let {uri ->
            activity?.let {a ->
                mImage?.let {image ->
                    GlideApp.with(a)
                        .load(uri)
                        .apply(PICTURE_OPTIONS)
                        .into(image)
                }
            }
        }
    }

    companion object {
        private val PICTURE_OPTIONS = GlideOptions().transform(CenterInside())
    }
}