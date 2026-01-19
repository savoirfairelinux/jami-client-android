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

package cx.ring.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R

class ActionListBottomSheet(
    @ArrayRes private val arrayResId: Int = 0,
    @ArrayRes private val iconArrayResId: Int = 0,
    private val onActionSelected: ((Int) -> Unit)? = null
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frag_action_list_bottom_sheet, container, false)
        view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ActionAdapter()
        }
        return view
    }

    private inner class ActionAdapter : RecyclerView.Adapter<ActionViewHolder>() {
        private val actions: Array<String> = if (arrayResId != 0) resources.getStringArray(arrayResId) else emptyArray()
        private val iconIds: IntArray = if (iconArrayResId != 0) {
            val ta = resources.obtainTypedArray(iconArrayResId)
            val ids = IntArray(ta.length()) { i -> ta.getResourceId(i, 0) }
            ta.recycle()
            ids
        } else {
            IntArray(0)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bottom_sheet_action, parent, false)
            return ActionViewHolder(view)
        }

        override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
            val iconId = if (position < iconIds.size) iconIds[position] else 0
            holder.bind(actions[position], iconId, position)
        }

        override fun getItemCount(): Int = actions.size
    }

    private inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView as TextView

        fun bind(action: String, iconId: Int, position: Int) {
            textView.text = action
            if (iconId != 0) {
                textView.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0)
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            itemView.setOnClickListener {
                onActionSelected?.invoke(position)
                dismiss()
            }
        }
    }
}
