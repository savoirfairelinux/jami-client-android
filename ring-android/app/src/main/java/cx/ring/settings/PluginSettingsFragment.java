package cx.ring.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PluginSettingsFragment extends PreferenceFragmentCompat {
    public static final String TAG = PluginsListSettingsFragment.class.getSimpleName();
    private Context mContext;
    private PluginDetails mPluginDetails;

    public PluginSettingsFragment(PluginDetails pluginDetails){
        this.mPluginDetails = pluginDetails;
    }
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mContext = requireActivity();
    }
}
