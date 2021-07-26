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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R

class EmojiChooserBottomSheet : BottomSheetDialogFragment() {
    interface IEmojiSelected {
        fun onEmojiSelected(emoji: String?)
    }

    private var callback: IEmojiSelected? = null
    fun setCallback(cb: IEmojiSelected?) {
        callback = cb
    }

    private inner class EmojiView constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val view: TextView = itemView as TextView
        var emoji: String? = null

        init {
            itemView.setOnClickListener { v: View? ->
                if (callback != null) callback!!.onEmojiSelected(emoji)
                dismiss()
            }
        }
    }

    private inner class ColorAdapter(@ArrayRes arrayResId: Int) :
        RecyclerView.Adapter<EmojiView>() {
        private val emojis: Array<String> = resources.getStringArray(arrayResId)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiView {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
            return EmojiView(v)
        }

        override fun onBindViewHolder(holder: EmojiView, position: Int) {
            holder.emoji = emojis[position]
            holder.view.text = holder.emoji
        }

        override fun getItemCount(): Int {
            return emojis.size
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frag_color_chooser, container) as RecyclerView
        view.adapter = ColorAdapter(R.array.conversation_emojis)
        return view
    }
}