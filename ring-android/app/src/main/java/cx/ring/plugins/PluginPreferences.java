/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.plugins;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import cx.ring.R;
import cx.ring.databinding.FragPluginSettingsBinding;
import cx.ring.settings.pluginssettings.PluginDetails;

public class PluginPreferences extends Preference {
    private PluginDetails mPluginDetails;

    private View.OnClickListener resetClickListener;
    private View.OnClickListener installClickListener;

    public void setResetClickListener(View.OnClickListener clickListener) {
        resetClickListener = clickListener;
    }

    public void setInstallClickListener(View.OnClickListener clickListener) {
        installClickListener = clickListener;
    }

    public PluginPreferences(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PluginPreferences(Context context, PluginDetails pluginDetails) {
        super(context);
        mPluginDetails = pluginDetails;
        setLayoutResource(R.layout.frag_plugin_settings);
    }

    public PluginPreferences(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PluginPreferences(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        FragPluginSettingsBinding binding = FragPluginSettingsBinding.bind(holder.itemView);

        if (mPluginDetails != null) {
            binding.pluginSettingIcon.setImageDrawable(mPluginDetails.getIcon());
            binding.pluginSettingTitle.setText(mPluginDetails.getName());
        }

        if (resetClickListener != null) {
            binding.pluginSettingReset.setOnClickListener(resetClickListener);
        }

        if (installClickListener != null) {
            binding.pluginSettingInstall.setOnClickListener(installClickListener);
        }
    }

}
