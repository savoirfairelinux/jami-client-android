/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R

class ColorChooserBottomSheet(val onColorSelected: ((Int) -> Unit)? = null) : BottomSheetDialogFragment() {
    inner class ColorView(itemView: View) : RecyclerView.ViewHolder(itemView)

    private inner class ColorAdapter : RecyclerView.Adapter<ColorView>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ColorView(LayoutInflater.from(parent.context).inflate(R.layout.item_color, parent, false))

        override fun onBindViewHolder(holder: ColorView, position: Int) {
            val color = resources.getColor(colors[position])
            holder.itemView.setOnClickListener {
                onColorSelected?.invoke(color)
                dismiss()
            }
            ImageViewCompat.setImageTintList(holder.itemView as ImageView, ColorStateList.valueOf(color))
        }

        override fun getItemCount() = colors.size
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        (inflater.inflate(R.layout.frag_color_chooser, container) as RecyclerView)
            .apply {
                id = R.id.color_chooser // Used in tests.
                adapter = ColorAdapter()
            }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
    }

    companion object {
        private val colors = intArrayOf(
            R.color.conversation_palette_pink,
            R.color.conversation_palette_purple, R.color.conversation_palette_indigo,
            R.color.conversation_palette_dark_blue, R.color.conversation_palette_blue,
            R.color.conversation_palette_blue_jean, R.color.conversation_palette_cyan,
            R.color.conversation_palette_teal, R.color.conversation_palette_green,
            R.color.conversation_palette_lime, R.color.conversation_palette_yellow,
            R.color.conversation_palette_orange, R.color.conversation_palette_red,
            R.color.conversation_palette_brown, R.color.conversation_palette_grey
        )
    }
}