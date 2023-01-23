package cx.ring.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.settings.pluginssettings.PluginDetails
import cx.ring.viewholders.PluginViewHolder

class PluginsAdapter(private var mList: List<PluginDetails>, private val listener: PluginListItemListener, private val accountId: String ?= "") :
        RecyclerView.Adapter<PluginViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PluginViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.frag_plugins_list_item, parent, false)
            return PluginViewHolder(view, listener)
        }

        override fun onBindViewHolder(holderSetting: PluginViewHolder, position: Int) {
            for (item in mList) {
                item.accountId = accountId
            }
            holderSetting.setDetails(mList[position])
        }

        override fun getItemCount(): Int {
            if (accountId!!.isNotEmpty()) {
                val copy = mutableListOf<PluginDetails>()
                for (item in mList) {
                    item.accountId = accountId
                    if (item.pluginPreferences.isNotEmpty())
                        copy += item
                }
                mList = copy
            }
            return mList.size
        }

        fun updatePluginsList(listPlugins: List<PluginDetails>) {
            for (item in listPlugins) {
                item.accountId = accountId
            }
            mList = listPlugins
            notifyDataSetChanged()
        }

        interface PluginListItemListener {
            fun onPluginItemClicked(pluginDetails: PluginDetails)
            fun onPluginEnabled(pluginDetails: PluginDetails)
        }

        companion object {
            val TAG = PluginsAdapter::class.simpleName!!
        }
    }