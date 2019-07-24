package cx.ring.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceFragmentCompat;
import cx.ring.R;

public class VideoSettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = VideoSettingsFragment.class.getName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.video_prefs, rootKey);
        getPreferenceScreen().setOnPreferenceChangeListener((preference, newValue) -> {
            Log.w(TAG, "onPreferenceChange " + preference.getKey() + " " + newValue);
            return true;
        });
    }
}
