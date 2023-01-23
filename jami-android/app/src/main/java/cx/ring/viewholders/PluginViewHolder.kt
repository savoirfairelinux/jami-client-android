package cx.ring.viewholders

import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.settings.pluginssettings.PluginDetails
import cx.ring.adapters.PluginsAdapter.PluginListItemListener
import cx.ring.fragments.CallFragment
import net.jami.daemon.JamiService
import java.util.*

class PluginViewHolder(itemView: View, listener: PluginListItemListener) : RecyclerView.ViewHolder(itemView) {
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
        pluginItemEnableCheckbox.isChecked = details.isRunning
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
                listener.onPluginEnabled(details)
            }
        }
    }
}