package cx.ring.settings.pluginssettings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.settings.pluginssettings.PluginsListAdapter.PluginViewHolder

class PluginsListAdapter(private var mList: List<PluginDetails>, private val listener: PluginListItemListener) :
    RecyclerView.Adapter<PluginViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PluginViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.frag_plugins_list_item, parent, false)
        return PluginViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: PluginViewHolder, position: Int) {
        holder.setDetails(mList[position])
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun updatePluginsList(listPlugins: List<PluginDetails>) {
        mList = listPlugins
        notifyDataSetChanged()
    }

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
            pluginItemEnableCheckbox.isChecked = details.isEnabled
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

    interface PluginListItemListener {
        fun onPluginItemClicked(pluginDetails: PluginDetails)
        fun onPluginEnabled(pluginDetails: PluginDetails)
    }

    companion object {
        val TAG = PluginsListAdapter::class.simpleName!!
    }
}