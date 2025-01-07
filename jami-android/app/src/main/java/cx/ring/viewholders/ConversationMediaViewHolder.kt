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
package cx.ring.viewholders

import android.media.MediaPlayer
import android.view.Surface
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemMediaFileBinding
import cx.ring.databinding.ItemMediaImageBinding
import cx.ring.databinding.ItemMediaVideoBinding
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ConversationMediaViewHolder(
    val image: ItemMediaImageBinding? = null,
    val video: ItemMediaVideoBinding? = null,
    val file: ItemMediaFileBinding? = null,
) : RecyclerView.ViewHolder(image?.root ?: video?.root ?: file!!.root) {
    var surface: Surface? = null
    var player: MediaPlayer? = null
    val compositeDisposable = CompositeDisposable()
}
