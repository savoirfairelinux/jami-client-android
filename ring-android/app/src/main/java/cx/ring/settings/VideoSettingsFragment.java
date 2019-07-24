package cx.ring.settings;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import cx.ring.R;

public class VideoSettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = VideoSettingsFragment.class.getName();

    private static final String RING_HW_ENCODING = "video_hwenc";
    private static final String RING_BITRATE = "video_bitrate";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.video_prefs, rootKey);
        Preference pref = findPreference(RING_HW_ENCODING);
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            Log.w(TAG, "onPreferenceChange " + preference.getKey() + " " + newValue);
            getPreferenceScreen().findPreference(RING_BITRATE).setEnabled((boolean) newValue);
            return true;
        });
    }
}
