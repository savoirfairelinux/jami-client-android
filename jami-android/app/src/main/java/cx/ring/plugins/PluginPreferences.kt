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
package cx.ring.plugins

import android.content.Context
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import cx.ring.R
import cx.ring.databinding.FragPluginSettingsBinding
import cx.ring.settings.pluginssettings.PluginDetails

class PluginPreferences : Preference {
    private var mPluginDetails: PluginDetails? = null
    private var resetClickListener: View.OnClickListener? = null
    private var installClickListener: View.OnClickListener? = null
    private var openPluginSettingsListener: View.OnClickListener? = null
    private var mAccountId: String? = ""
    fun setResetClickListener(clickListener: View.OnClickListener?) {
        resetClickListener = clickListener
    }

    fun setInstallClickListener(clickListener: View.OnClickListener?) {
        installClickListener = clickListener
    }

    fun setPluginSettingsRedirect(clickListener: View.OnClickListener?) {
        openPluginSettingsListener = clickListener
    }

    constructor(context: Context?, pluginDetails: PluginDetails?, accountId: String? = "") : super(
        context!!
    ) {
        mPluginDetails = pluginDetails
        mAccountId = accountId
        layoutResource = R.layout.frag_plugin_settings
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = FragPluginSettingsBinding.bind(holder.itemView)
        if (mPluginDetails != null) {
            binding.pluginSettingIcon.setImageDrawable(mPluginDetails!!.icon)
            binding.pluginSettingTitle.text = mPluginDetails!!.name
        }
        if (resetClickListener != null) {
            binding.pluginSettingReset.setOnClickListener(resetClickListener)
        }
        if (mAccountId!!.isEmpty()) {
            if (installClickListener != null) {
                binding.pluginSettingInstall.setOnClickListener(installClickListener)
            }
        } else {
            binding.pluginSettingButtons.weightSum = 1.0F
            binding.pluginSettingInstall.visibility = View.GONE
        }
        if (mAccountId!!.isEmpty()) {
            binding.pluginAccountSettingRedirect.setText(R.string.open_account_plugin_settings)
        } else {
            binding.pluginAccountSettingRedirect.setText(R.string.open_general_plugin_settings)
        }
        if (openPluginSettingsListener != null)
            binding.pluginAccountSettingRedirect.setOnClickListener(openPluginSettingsListener)
    }
}