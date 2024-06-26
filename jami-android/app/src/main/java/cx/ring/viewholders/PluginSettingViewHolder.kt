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
package cx.ring.viewholders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import cx.ring.R
import cx.ring.settings.pluginssettings.PluginDetails
import cx.ring.settings.pluginssettings.PluginsListAdapter.PluginListItemListener

class PluginSettingViewHolder(itemView: View, listener: PluginListItemListener) : RecyclerView.ViewHolder(itemView) {
    private val pluginIcon: ImageView = itemView.findViewById(R.id.plugin_item_icon)
    private val pluginNameTextView: TextView = itemView.findViewById(R.id.plugin_item_name)
    private val pluginItemEnableCheckbox: MaterialSwitch = itemView.findViewById(R.id.plugin_item_enable_checkbox)
    private var details: PluginDetails? = null

    fun setDetails(details: PluginDetails) {
        this.details = details
        update(details)
    }

    // update the viewHolder view
    fun update(details: PluginDetails) {
        pluginNameTextView.text = details.name
        if (details.accountId!!.isEmpty())
            pluginItemEnableCheckbox.isChecked = details.isEnabled
        else
            pluginItemEnableCheckbox.visibility = View.GONE

        // Set the plugin icon
        val icon = details.icon
        if (icon != null) {
            pluginIcon.setImageDrawable(icon)
        }
    }

    init {
        itemView.setOnClickListener {
            details?.let { details -> listener.onPluginItemClicked(details) }
        }
        pluginItemEnableCheckbox.setOnClickListener {
            details?.let { details ->
                details.isEnabled = !details.isEnabled
                listener.onPluginEnabled(details)
            }
        }
    }
}