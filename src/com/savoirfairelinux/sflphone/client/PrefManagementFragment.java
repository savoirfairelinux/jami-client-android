/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.savoirfairelinux.sflphone.R;

public class PrefManagementFragment extends PreferenceFragment
{
    static final String TAG = "PrefManagementFragment";
    static final String CURRENT_VALUE = "Current value:: "; 

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setPreferenceScreen(getAudioPreferenceScreen()); 
    }

    Preference.OnPreferenceChangeListener changeListListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(CURRENT_VALUE + (CharSequence)newValue);
            return true;
        }
    };

    public PreferenceScreen getAudioPreferenceScreen()
    {
        Activity currentContext = getActivity();

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(currentContext);

        PreferenceCategory audioPrefCat = new PreferenceCategory(currentContext);
        audioPrefCat.setTitle(R.string.audio_preferences);
        root.addPreference(audioPrefCat);

        ListPreference codecListPref = new ListPreference(currentContext);
        codecListPref.setEntries(R.array.audio_codec_list);
        codecListPref.setEntryValues(R.array.audio_codec_list_value);
        codecListPref.setDialogTitle(R.string.dialogtitle_audio_codec_list);
        codecListPref.setPersistent(false);
        codecListPref.setTitle(R.string.title_audio_codec_list);
        codecListPref.setSummary(CURRENT_VALUE + "PCMU");
        codecListPref.setOnPreferenceChangeListener(changeListListener);
        audioPrefCat.addPreference(codecListPref);

        return root;
    }
}
