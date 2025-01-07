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
package cx.ring.extensions

import android.content.Context
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import cx.ring.R
import cx.ring.databinding.FragExtensionSettingsBinding
import cx.ring.settings.extensionssettings.ExtensionDetails

class ExtensionPreferences : Preference {
    private var mExtensionDetails: ExtensionDetails? = null
    private var resetClickListener: View.OnClickListener? = null
    private var installClickListener: View.OnClickListener? = null
    private var openExtensionSettingsListener: View.OnClickListener? = null
    private var mAccountId: String? = ""
    fun setResetClickListener(clickListener: View.OnClickListener?) {
        resetClickListener = clickListener
    }

    fun setInstallClickListener(clickListener: View.OnClickListener?) {
        installClickListener = clickListener
    }

    fun setExtensionSettingsRedirect(clickListener: View.OnClickListener?) {
        openExtensionSettingsListener = clickListener
    }

    constructor(context: Context?, extensionDetails: ExtensionDetails?, accountId: String? = "") : super(
        context!!
    ) {
        mExtensionDetails = extensionDetails
        mAccountId = accountId
        layoutResource = R.layout.frag_extension_settings
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = FragExtensionSettingsBinding.bind(holder.itemView)
        if (mExtensionDetails != null) {
            binding.extensionSettingIcon.setImageDrawable(mExtensionDetails!!.icon)
            binding.extensionSettingTitle.text = mExtensionDetails!!.name
        }
        if (resetClickListener != null) {
            binding.extensionSettingReset.setOnClickListener(resetClickListener)
        }
        if (mAccountId!!.isEmpty()) {
            if (installClickListener != null) {
                binding.extensionSettingInstall.setOnClickListener(installClickListener)
            }
        } else {
            binding.extensionSettingButtons.weightSum = 1.0F
            binding.extensionSettingInstall.visibility = View.GONE
        }
        if (mAccountId!!.isEmpty()) {
            binding.extensionAccountSettingRedirect.setText(R.string.open_account_extension_settings)
        } else {
            binding.extensionAccountSettingRedirect.setText(R.string.open_general_extension_settings)
        }
        if (openExtensionSettingsListener != null)
            binding.extensionAccountSettingRedirect.setOnClickListener(openExtensionSettingsListener)
    }
}