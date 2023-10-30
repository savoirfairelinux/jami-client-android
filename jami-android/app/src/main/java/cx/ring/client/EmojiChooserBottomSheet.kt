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
package cx.ring.client

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R

class EmojiChooserBottomSheet(val onEmojiSelected: ((String?) -> Unit)? = null) : BottomSheetDialogFragment() {
    private inner class EmojiView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view: TextView = itemView as TextView
        var emoji: String? = null

        init {
            itemView.setOnClickListener {
                onEmojiSelected?.invoke(emoji)
                dismiss()
            }
        }
    }

    private inner class ColorAdapter(@ArrayRes arrayResId: Int) : RecyclerView.Adapter<EmojiView>() {
        private val emojis: Array<String> = resources.getStringArray(arrayResId)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            EmojiView(LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false))

        override fun onBindViewHolder(holder: EmojiView, position: Int) {
            holder.emoji = emojis[position]
            holder.view.text = holder.emoji
        }

        override fun getItemCount() = emojis.size
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        (inflater.inflate(R.layout.frag_color_chooser, container) as RecyclerView)
            .apply { adapter = ColorAdapter(R.array.conversation_emojis) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
    }
}