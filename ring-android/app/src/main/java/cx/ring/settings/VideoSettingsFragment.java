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
    private static final String RING_RESOLUTION = "video_resolution";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.video_prefs, rootKey);
        Preference encodingPref = findPreference(RING_HW_ENCODING);
        Preference resolutionPref = findPreference(RING_RESOLUTION);
        handleResolutionIcon(getPreferenceManager().getSharedPreferences().getString(RING_RESOLUTION, "720"));
        encodingPref.setOnPreferenceChangeListener((preference, newValue) -> {
            Log.w(TAG, "onPreferenceChange " + preference.getKey() + " " + newValue);
            getPreferenceScreen().findPreference(RING_BITRATE).setEnabled((boolean) newValue);
            return true;
        });
        resolutionPref.setOnPreferenceChangeListener((preference, newValue) -> {
            Log.w(TAG, "onPreferenceChange " + preference.getKey() + " " + newValue);
            handleResolutionIcon((String) newValue);
            return true;
        });
    }


    private void handleResolutionIcon(String resolution) {
        if (resolution.equals("480"))
            getPreferenceScreen().findPreference(RING_RESOLUTION).setIcon(R.drawable.ic_videocam_black_24dp);
        else
            getPreferenceScreen().findPreference(RING_RESOLUTION).setIcon(R.drawable.ic_hd_black_24dp);
    }
}
