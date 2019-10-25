package cx.ring.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

public class PluginSettingsFragment extends PreferenceFragmentCompat {
    public static final String TAG = PluginsListSettingsFragment.class.getSimpleName();
    private Context mContext;
    private PluginDetails pluginDetails;

    public static PluginSettingsFragment newInstance(PluginDetails pluginDetails) {
        Bundle args = new Bundle();
        args.putString("name", pluginDetails.getName());
        args.putString("rootPath", pluginDetails.getRootPath());
        args.putBoolean("enabled", pluginDetails.isEnabled());
        PluginSettingsFragment psf = new PluginSettingsFragment();
        psf.setArguments(args);
        return psf;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = requireActivity();

        Bundle arguments = getArguments();

        if (arguments != null) {
            this.pluginDetails = new PluginDetails(arguments.getString("name"),
                    arguments.getString("rootPath"), arguments.getBoolean("enabled"));
        }
    }
}
