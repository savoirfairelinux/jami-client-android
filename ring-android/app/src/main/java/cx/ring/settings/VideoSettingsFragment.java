/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.services.SharedPreferencesServiceImpl;

public class VideoSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
        pm.setSharedPreferencesName(SharedPreferencesServiceImpl.PREFS_VIDEO);

        setPreferencesFromResource(R.xml.video_prefs, rootKey);
        Preference resolutionPref = findPreference(SharedPreferencesServiceImpl.PREF_RESOLUTION);
        if (resolutionPref != null) {
            handleResolutionIcon(resolutionPref, pm.getSharedPreferences().getString(SharedPreferencesServiceImpl.PREF_RESOLUTION, "720"));
            resolutionPref.setOnPreferenceChangeListener((preference, newValue) -> {
                handleResolutionIcon(preference, (String) newValue);
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        ((HomeActivity) requireActivity()).setToolbarTitle(R.string.menu_item_settings);
    }

    private static void handleResolutionIcon(Preference resolutionPref, String resolution) {
        if (resolution == null || resolution.equals("480"))
            resolutionPref.setIcon(R.drawable.baseline_videocam_24);
        else
            resolutionPref.setIcon(R.drawable.baseline_hd_24);
    }
}
