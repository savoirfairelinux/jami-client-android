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

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import cx.ring.R
import cx.ring.settings.extensionssettings.ExtensionDetails
import cx.ring.adapters.ExtensionsAdapter.ExtensionListItemListener

class ExtensionViewHolder(itemView: View, listener: ExtensionListItemListener) : RecyclerView.ViewHolder(itemView) {
    private val extensionIcon: ImageView = itemView.findViewById(R.id.extension_item_icon)
    private val extensionNameTextView: TextView = itemView.findViewById(R.id.extension_item_name)
    private val extensionItemEnableCheckbox: MaterialSwitch = itemView.findViewById(R.id.extension_item_enable_checkbox)
    private var details: ExtensionDetails? = null

    fun setDetails(details: ExtensionDetails) {
        this.details = details
        update(details)
    }

    // update the viewHolder view
    fun update(details: ExtensionDetails) {
        extensionNameTextView.text = details.name
        extensionItemEnableCheckbox.isChecked = details.isRunning
        // Set the extension icon
        val icon = details.icon
        if (icon != null) {
            extensionIcon.setImageDrawable(icon)
        }
    }

    init {
        itemView.setOnClickListener {
            details?.let { details -> listener.onExtensionItemClicked(details) }
        }

        extensionItemEnableCheckbox.setOnClickListener {
            details?.let { details ->
                listener.onExtensionEnabled(details)
            }
        }
    }
}