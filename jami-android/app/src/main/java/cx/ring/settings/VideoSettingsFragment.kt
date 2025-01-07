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
package cx.ring.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cx.ring.R
import cx.ring.interfaces.AppBarStateListener
import cx.ring.services.SharedPreferencesServiceImpl

class VideoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val pm = preferenceManager
        pm.sharedPreferencesMode = Context.MODE_PRIVATE
        pm.sharedPreferencesName = SharedPreferencesServiceImpl.PREFS_VIDEO
        setPreferencesFromResource(R.xml.video_prefs, rootKey)
        val resolutionPref = findPreference<Preference>(SharedPreferencesServiceImpl.PREF_RESOLUTION)
        if (resolutionPref != null) {
            handleResolutionIcon(resolutionPref, pm.sharedPreferences!!
                    .getString(SharedPreferencesServiceImpl.PREF_RESOLUTION, "720"))
            resolutionPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any? ->
                    handleResolutionIcon(preference, newValue as String?)
                    true
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appbarHostFragment = parentFragment as? AppBarStateListener
        appbarHostFragment?.onToolbarTitleChanged(getString(R.string.video_setting_title))
        appbarHostFragment?.onAppBarScrollTargetViewChanged(listView)
    }

    companion object {
        private fun handleResolutionIcon(resolutionPref: Preference, resolution: String?) {
            resolutionPref.setIcon(if(resolution == null || resolution == "480")
                R.drawable.baseline_videocam_24 else R.drawable.baseline_hd_24)
        }
    }
}