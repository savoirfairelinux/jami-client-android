/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 * Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
import net.jami.model.ConfigKey

class TVAboutFragment : LeanbackPreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        setPreferencesFromResource(R.xml.tv_about_pref, rootKey)
        findPreference<Preference>("About.version")?.title = version
        findPreference<Preference>("About.license")?.title = license
        findPreference<Preference>("About.rights")?.title = rights
        findPreference<Preference>("About.credits")?.title = credits
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == ConfigKey.ACCOUNT_AUTOANSWER.key()) {
        } else if (preference.key == ConfigKey.ACCOUNT_ISRENDEZVOUS.key()) {
        }
        return super.onPreferenceTreeClick(preference)
    }

    private val version: CharSequence
        get() {
            val version = SpannableString(requireContext().resources.getString(R.string.version_section))
            version.setSpan(UnderlineSpan(), 0, version.length, 0)
            return requireContext().resources.getString(R.string.app_release, BuildConfig.VERSION_NAME)
        }
    private val license: CharSequence
        get() {
            val licence = SpannableString(requireContext().resources.getString(R.string.section_license))
            licence.setSpan(UnderlineSpan(), 0, licence.length, 0)
            return requireContext().resources.getString(R.string.license)
        }
    private val rights: CharSequence
        get() {
            val licence = SpannableString(requireContext().resources.getString(R.string.copyright_section))
            licence.setSpan(UnderlineSpan(), 0, licence.length, 0)
            return requireContext().resources.getString(R.string.copyright)
        }
    private val credits: CharSequence
        get() {
            val developedby = SpannableString(requireContext().resources.getString(R.string.developed_by))
            developedby.setSpan(UnderlineSpan(), 0, developedby.length, 0)
            val developed: CharSequence =
                requireContext().resources.getString(R.string.credits_developer).replace("\n".toRegex(), "<br/>")
            return Html.fromHtml("<b><u>$developedby</u></b><br/>$developed")
        }

    companion object {
        fun newInstance(): TVAboutFragment {
            return TVAboutFragment()
        }
    }
}