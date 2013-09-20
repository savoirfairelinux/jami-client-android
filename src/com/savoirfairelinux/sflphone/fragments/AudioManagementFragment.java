/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package com.savoirfairelinux.sflphone.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;

public class AudioManagementFragment extends PreferenceFragment
{
    static final String TAG = "PrefManagementFragment";
    static final String CURRENT_VALUE = "Current value:: "; 

    public AudioManagementFragment()
    {
    }

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
        speakerSeekBarPref.setProgress(50);
        speakerSeekBarPref.setSummary(CURRENT_VALUE + speakerSeekBarPref.getProgress());
        audioPrefCat.addPreference(speakerSeekBarPref);

        // Capture volume seekbar
        SeekBarPreference captureSeekBarPref = new SeekBarPreference(currentContext);
        captureSeekBarPref.setPersistent(false);
        captureSeekBarPref.setTitle("Capture Volume");
        captureSeekBarPref.setProgress(50);
        captureSeekBarPref.setSummary(CURRENT_VALUE + captureSeekBarPref.getProgress());
        audioPrefCat.addPreference(captureSeekBarPref);

        // Ringtone volume seekbar
        SeekBarPreference ringtoneSeekBarPref = new SeekBarPreference(currentContext);
        ringtoneSeekBarPref.setPersistent(false);
        ringtoneSeekBarPref.setTitle("Ringtone Volume");
        ringtoneSeekBarPref.setProgress(50);
        ringtoneSeekBarPref.setSummary(CURRENT_VALUE + ringtoneSeekBarPref.getProgress());
        audioPrefCat.addPreference(ringtoneSeekBarPref);

        return root;
    }

    public class SeekBarPreference extends Preference implements OnSeekBarChangeListener
    {
        private SeekBar seekbar;
        private int progress;
        private int max = 100;
        private TextView summary;
        private boolean discard;

        public SeekBarPreference (Context context)
        {
            super( context );
        }

        public SeekBarPreference (Context context, AttributeSet attrs)
        {
            super( context, attrs );
        }

        public SeekBarPreference (Context context, AttributeSet attrs, int defStyle)
        {
            super( context, attrs, defStyle );
        }

        protected View onCreateView (ViewGroup p)
        {
            final Context ctx = getContext();

            LinearLayout layout = new LinearLayout( ctx );
            layout.setId( android.R.id.widget_frame );
            layout.setOrientation( LinearLayout.VERTICAL );
            layout.setPadding(65, 10, 15, 10);

            TextView title = new TextView( ctx );
            int textColor = title.getCurrentTextColor();
            title.setId( android.R.id.title );
            title.setSingleLine();
            title.setTextAppearance( ctx, android.R.style.TextAppearance_Medium );
            title.setTextColor( textColor );
            layout.addView( title );

            summary = new TextView( ctx );
            summary.setId( android.R.id.summary );
            summary.setSingleLine();
            summary.setTextAppearance( ctx, android.R.style.TextAppearance_Small );
            summary.setTextColor( textColor );
            layout.addView( summary );

            seekbar = new SeekBar( ctx );
            seekbar.setId( android.R.id.progress );
            seekbar.setMax( max );
            seekbar.setOnSeekBarChangeListener( this );
            layout.addView( seekbar );

            return layout;
        }

        @Override
        protected void onBindView (View view)
        {
            super.onBindView( view );

            if (seekbar != null)
                seekbar.setProgress( progress );
        }

        public void setProgress (int pcnt) {
            if (progress != pcnt) {
                persistInt( progress = pcnt );

                notifyDependencyChange( shouldDisableDependents() );
                notifyChanged();
            }
        }

        public int getProgress () {
            return progress;
        }

        public void setMax (int max) {
            this.max = max;
            if (seekbar != null)
                seekbar.setMax( max );
        }

        @Override
        protected Object onGetDefaultValue (TypedArray a, int index) {
            return a.getInt( index, progress );
        }

        @Override
        protected void onSetInitialValue (boolean restoreValue, Object defaultValue) {
            setProgress( restoreValue ? getPersistedInt( progress ) : (Integer)defaultValue );
        }

        @Override
        public boolean shouldDisableDependents () {
            return progress == 0 || super.shouldDisableDependents();
        }

        public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser) {
            discard = !callChangeListener( progress );
            summary.setText(CURRENT_VALUE + progress); 
        }

        public void onStartTrackingTouch (SeekBar seekBar) {
            discard = false;
        }

        public void onStopTrackingTouch (SeekBar seekBar) {
            if (discard)
                seekBar.setProgress( progress );
            else {
                setProgress( seekBar.getProgress() );

//                OnPreferenceChangeListener listener = getOnPreferenceChangeListener();
                //if (listener instanceof AbstractSeekBarListener)
                ////        setSummary( ((AbstractSeekBarListener)listener).toSummary( seekBar.getProgress() ) );
            }
        }
    }
}
