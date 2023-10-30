/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.settings.pluginssettings

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.utils.AndroidFileUtils
import java.io.File

class PathListAdapter internal constructor(
    private var mList: List<String>,
    private val mListener: PathListItemListener
) : RecyclerView.Adapter<PathListAdapter.PathViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PathViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.frag_path_list_item, parent, false)
        return PathViewHolder(view, mListener)
    }

    override fun onBindViewHolder(holder: PathViewHolder, position: Int) {
        holder.setDetails(mList[position])
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun updatePluginsList(listPaths: List<String>) {
        mList = listPaths
        notifyDataSetChanged()
    }

    inner class PathViewHolder(itemView: View, listener: PathListItemListener) : RecyclerView.ViewHolder(itemView) {
        private val pathIcon: ImageView = itemView.findViewById(R.id.path_item_icon)
        private val pathTextView: TextView = itemView.findViewById(R.id.path_item_name)
        private var path: String? = null

        // update the viewHolder view
        fun update(s: String) {
            // Set the plugin icon
            val file = File(s)
            if (file.exists()) {
                if (AndroidFileUtils.isImage(s)) {
                    pathTextView.visibility = View.GONE
                    Drawable.createFromPath(s)?.let { icon -> pathIcon.setImageDrawable(icon) }
                } else {
                    pathTextView.visibility = View.VISIBLE
                    pathTextView.text = file.name
                }
            }
        }

        fun setDetails(path: String) {
            this.path = path
            update(path)
        }

        init {
            itemView.setOnClickListener { path?.let { path -> listener.onPathItemClicked(path) }}
        }
    }

    interface PathListItemListener {
        fun onPathItemClicked(path: String)
    }

    companion object {
        val TAG = PathListAdapter::class.simpleName!!
    }
}