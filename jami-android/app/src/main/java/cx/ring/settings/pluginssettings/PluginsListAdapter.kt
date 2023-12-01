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

    fun notifyItemChanged(pluginDetails: PluginDetails) =
        mList.indexOf(pluginDetails).let { if (it != -1) notifyItemChanged(it) }

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