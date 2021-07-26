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
package cx.ring.tv.settings;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;

import androidx.leanback.preference.LeanbackPreferenceFragmentCompat;
import androidx.preference.Preference;

import net.jami.model.ConfigKey;

import cx.ring.BuildConfig;
import cx.ring.R;

public class TVAboutFragment extends LeanbackPreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.tv_about_pref, rootKey);
        Preference version = findPreference("About.version");
        Preference license = findPreference("About.license");
        Preference rights = findPreference("About.rights");
        Preference credits = findPreference("About.credits");
        version.setTitle(getVersion());
        license.setTitle(getLicense());
        rights.setTitle(getRights());
        credits.setTitle(getCredits());
    }

    public static TVAboutFragment newInstance() {
        return new TVAboutFragment();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(ConfigKey.ACCOUNT_AUTOANSWER.key())) {

        } else if (preference.getKey().equals(ConfigKey.ACCOUNT_ISRENDEZVOUS.key())) {

        }
        return super.onPreferenceTreeClick(preference);
    }

    private CharSequence getVersion() {
        SpannableString version = new SpannableString(requireContext().getResources().getString(R.string.version_section));
        version.setSpan(new UnderlineSpan(), 0, version.length(), 0);
        return requireContext().getResources().getString(R.string.app_release, BuildConfig.VERSION_NAME);
    }

    private CharSequence getLicense() {
        SpannableString licence = new SpannableString(requireContext().getResources().getString(R.string.section_license));
        licence.setSpan(new UnderlineSpan(), 0, licence.length(), 0);
        return requireContext().getResources().getString(R.string.license);
    }

    private CharSequence getRights() {
        SpannableString licence = new SpannableString(requireContext().getResources().getString(R.string.copyright_section));
        licence.setSpan(new UnderlineSpan(), 0, licence.length(), 0);
        return requireContext().getResources().getString(R.string.copyright);
    }

    private CharSequence getCredits() {
        SpannableString developedby = new SpannableString(requireContext().getResources().getString(R.string.developed_by));
        developedby.setSpan(new UnderlineSpan(), 0, developedby.length(), 0);
        CharSequence developed = requireContext().getResources().getString(R.string.credits_developer).replaceAll("\n", "<br/>");
        return Html.fromHtml("<b><u>" + developedby + "</u></b><br/>" + developed);
    }

}
