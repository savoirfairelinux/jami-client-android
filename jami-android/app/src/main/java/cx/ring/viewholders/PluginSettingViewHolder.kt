package cx.ring.viewholders

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.settings.pluginssettings.PluginDetails
import cx.ring.settings.pluginssettings.PluginsListAdapter.PluginListItemListener

class PluginSettingViewHolder(itemView: View, listener: PluginListItemListener) : RecyclerView.ViewHolder(itemView) {
    private val pluginIcon: ImageView = itemView.findViewById(R.id.plugin_item_icon)
    private val pluginNameTextView: TextView = itemView.findViewById(R.id.plugin_item_name)
    private val pluginItemEnableCheckbox: CheckBox = itemView.findViewById(R.id.plugin_item_enable_checkbox)
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