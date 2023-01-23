package cx.ring.settings.pluginssettings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.viewholders.PluginSettingViewHolder

class PluginsListAdapter(private var mList: List<PluginDetails>, private val listener: PluginListItemListener, private val accountId: String? = "") :
    RecyclerView.Adapter<PluginSettingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PluginSettingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.frag_plugins_list_item, parent, false)
        return PluginSettingViewHolder(view, listener)
    }

    override fun onBindViewHolder(holderSetting: PluginSettingViewHolder, position: Int) {
        for (item in mList) {
            item.accountId = accountId
        }
        holderSetting.setDetails(mList[position])
    }

    override fun getItemCount(): Int {
        if (!accountId!!.isEmpty()) {
            var copy: List<PluginDetails> = ArrayList()
            for (item in mList) {
                item.accountId = accountId
                if (!item.pluginPreferences.isEmpty())
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
        val TAG = PluginsListAdapter::class.simpleName!!
    }
}