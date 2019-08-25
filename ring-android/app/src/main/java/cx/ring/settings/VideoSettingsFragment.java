package cx.ring.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import cx.ring.R;
import cx.ring.services.SharedPreferencesServiceImpl;

public class VideoSettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = VideoSettingsFragment.class.getName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
        pm.setSharedPreferencesName(SharedPreferencesServiceImpl.PREFS_VIDEO);
        setPreferencesFromResource(R.xml.video_prefs, rootKey);
        Preference resolutionPref = findPreference(SharedPreferencesServiceImpl.RING_RESOLUTION);
        handleResolutionIcon(pm.getSharedPreferences().getString(SharedPreferencesServiceImpl.RING_RESOLUTION, "720"));
        resolutionPref.setOnPreferenceChangeListener((preference, newValue) -> {
            Log.w(TAG, "onPreferenceChange " + preference.getKey() + " " + newValue);
            handleResolutionIcon((String) newValue);
            return true;
        });
    }

    private void handleResolutionIcon(String resolution) {
        if (resolution.equals("480"))
            getPreferenceScreen().findPreference(SharedPreferencesServiceImpl.RING_RESOLUTION).setIcon(R.drawable.baseline_videocam_24);
        else
            getPreferenceScreen().findPreference(SharedPreferencesServiceImpl.RING_RESOLUTION).setIcon(R.drawable.baseline_hd_24);
    }

}
