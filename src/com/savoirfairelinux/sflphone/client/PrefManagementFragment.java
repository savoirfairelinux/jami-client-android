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
import android.content.Context;
import android.graphics.Typeface;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.os.Bundle;
import android.util.Log;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

    Preference.OnPreferenceChangeListener changePreferenceListener = new Preference.OnPreferenceChangeListener() {
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

        // Codec List
        ListPreference codecListPref = new ListPreference(currentContext);
        codecListPref.setEntries(R.array.audio_codec_list);
        codecListPref.setEntryValues(R.array.audio_codec_list_value);
        codecListPref.setDialogTitle(R.string.dialogtitle_audio_codec_list);
        codecListPref.setPersistent(false);
        codecListPref.setTitle(R.string.title_audio_codec_list);
        codecListPref.setSummary(CURRENT_VALUE + "PCMU");
        codecListPref.setOnPreferenceChangeListener(changePreferenceListener);
        audioPrefCat.addPreference(codecListPref);

        // Ringtone
        EditTextPreference audioRingtonePref = new EditTextPreference(currentContext);
        audioRingtonePref.setDialogTitle(R.string.dialogtitle_audio_ringtone_field);
        audioRingtonePref.setPersistent(false);
        audioRingtonePref.setTitle(R.string.title_audio_ringtone_field);
        audioRingtonePref.setSummary(CURRENT_VALUE + "path/to/ringtone");
        audioRingtonePref.setOnPreferenceChangeListener(changePreferenceListener);
        audioPrefCat.addPreference(audioRingtonePref);

        // Speaker volume seekbar
        SeekBarPreference speakerSeekBarPref = new SeekBarPreference(currentContext);
        speakerSeekBarPref.setPersistent(false);
        speakerSeekBarPref.setTitle("Speaker Volume");
        speakerSeekBarPref.setSummary("Set the volume for speaker");
        audioPrefCat.addPreference(speakerSeekBarPref);

        // Capture volume seekbar
        SeekBarPreference captureSeekBarPref = new SeekBarPreference(currentContext);
        captureSeekBarPref.setPersistent(false);
        captureSeekBarPref.setTitle("Capture Volume");
        captureSeekBarPref.setSummary("Set the volume for microphone");
        audioPrefCat.addPreference(captureSeekBarPref);

        // Ringtone volume seekbar
        SeekBarPreference ringtoneSeekBarPref = new SeekBarPreference(currentContext);
        ringtoneSeekBarPref.setPersistent(false);
        ringtoneSeekBarPref.setTitle("Ringtone Volume");
        ringtoneSeekBarPref.setSummary("Set the volume for ringtone");
        audioPrefCat.addPreference(ringtoneSeekBarPref);

        return root;
    }

    public class SeekBarPreference extends Preference implements OnSeekBarChangeListener
    {
        public int MAXIMUM = 100;
        public int INTERVAL = 5;
        private float oldValue = 25;
        private TextView Indicator;

        public SeekBarPreference(Context context)
        {
            super(context);
        }

        public SeekBarPreference(Context context, AttributeSet attrs)
        {
            super(context, attrs);
        }

        public SeekBarPreference(Context context, AttributeSet attrs, int defStyle)
        {
           super(context, attrs, defStyle);
        }

        @Override
        protected View onCreateView(ViewGroup parent)
        {
            float scale = getContext().getResources().getDisplayMetrics().density;

            RelativeLayout layout = new RelativeLayout(getContext());

            RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

            RelativeLayout.LayoutParams sbarParams = new RelativeLayout.LayoutParams(
                Math.round(scale * 160),
                RelativeLayout.LayoutParams.WRAP_CONTENT);

            RelativeLayout.LayoutParams indParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

            TextView preferenceText = new TextView(getContext());
            preferenceText.setId(0);
            preferenceText.setText(getTitle());
            preferenceText.setTextSize(18);
            preferenceText.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);

            SeekBar sbar = new SeekBar(getContext());
            sbar.setId(1);
            sbar.setMax(MAXIMUM);
            sbar.setProgress((int)this.oldValue);
            sbar.setOnSeekBarChangeListener(this);

            this.Indicator = new TextView(getContext());
            this.Indicator.setTextSize(12);
            this.Indicator.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC); 
            this.Indicator.setText("" + sbar.getProgress());

            textParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            sbarParams.addRule(RelativeLayout.RIGHT_OF, preferenceText.getId());
            indParams.setMargins(Math.round(20*scale), 0, 0, 0);
            indParams.addRule(RelativeLayout.RIGHT_OF, sbar.getId());

            preferenceText.setLayoutParams(textParams);
            sbar.setLayoutParams(sbarParams);
            this.Indicator.setLayoutParams(indParams);
            layout.addView(preferenceText);
            layout.addView(this.Indicator);
            layout.addView(sbar);
            layout.setId(android.R.id.widget_frame);

            return layout;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
            progress = Math.round(((float) progress)/INTERVAL) * INTERVAL;

            if(!callChangeListener(progress)) {
                seekBar.setProgress((int) this.oldValue);
                return;
            }

            seekBar.setProgress(progress);
            this.oldValue = progress;
            this.Indicator.setText("" + progress);
            updatePreference(progress);

            notifyChanged(); 
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) 
        {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar)
        {
        }

        private void updatePreference(int newValue) {
            // SharedPreference.Editor editor = getEditor();
            // editor.putInt(getKey(), newValue);
            // editor.commit();
        }
    }
}
