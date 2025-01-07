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
package cx.ring.tv.settings

import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.style.UnderlineSpan
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.Preference
import cx.ring.BuildConfig
import cx.ring.R

class TVAboutFragment : LeanbackPreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tv_about_pref, rootKey)
        findPreference<Preference>("About.version")?.title = getString(R.string.app_release, BuildConfig.VERSION_NAME)
        findPreference<Preference>("About.license")?.title = getString(R.string.license)
        findPreference<Preference>("About.rights")?.title = getString(R.string.copyright)
        findPreference<Preference>("About.credits")?.title = credits
    }

    private val credits: CharSequence
        get() {
            val developers = SpannableString(getText(R.string.developers))
            developers.setSpan(UnderlineSpan(), 0, developers.length, 0)
            val developed = getString(R.string.credits_developer).replace("\n", "<br/>")
            return Html.fromHtml("<b><u>$developers</u></b><br/>$developed")
        }

    companion object {
        fun newInstance() = TVAboutFragment()
    }
}