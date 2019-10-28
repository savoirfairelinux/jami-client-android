package cx.ring.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import cx.ring.R;
import cx.ring.client.HomeActivity;

import static android.content.Context.MODE_PRIVATE;
import static cx.ring.Plugins.PluginUtils.PLUGIN_ENABLED;
import static cx.ring.Plugins.PluginUtils.listPlugins;

public class PluginsListSettingsFragment extends Fragment implements PluginsListAdapter.PluginListItemListener {

    public static final String TAG = PluginsListSettingsFragment.class.getSimpleName();
    private Context mContext;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private FloatingActionButton fab;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mContext = requireActivity();
        View pluginsSettingsList = inflater.inflate(R.layout.frag_plugins_list_settings, container, false);
        recyclerView = pluginsSettingsList.findViewById(R.id.plugins_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)

        mAdapter = new PluginsListAdapter(listPlugins(mContext), this);
        recyclerView.setAdapter(mAdapter);

        //Fab
        fab = pluginsSettingsList.findViewById(R.id.plugins_list_settings_fab);

        fab.setOnClickListener(view -> Snackbar.make(view, "More plugins soon on the store !",
                Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        return pluginsSettingsList;
    }

    /**
     * Implements PluginListItemListener.onPluginItemClicked which is called when we click on
     * a plugin list item
     * @param pluginDetails instance of a plugin details that is sent to PluginSettingsFragment
     */
    @Override
    public void onPluginItemClicked(PluginDetails pluginDetails) {
        ((HomeActivity) mContext).gotToPluginSettings(pluginDetails);
    }

    /**
     * Implements PluginListItemListener.onPluginEnabled which is called when the checkbox
     * associated with the plugin list item is called
     * @param pluginDetails instance of a plugin details that is sent to PluginSettingsFragment
     */
    @Override
    public void onPluginEnabled(PluginDetails pluginDetails) {
        SharedPreferences sp = mContext.getSharedPreferences(
                pluginDetails.getName(), MODE_PRIVATE);

        SharedPreferences.Editor preferencesEditor = sp.edit();
        preferencesEditor.putBoolean(PLUGIN_ENABLED, pluginDetails.isEnabled());
        preferencesEditor.apply();

        Toast.makeText(mContext,pluginDetails.getName() + " " + pluginDetails.isEnabled(),
                Toast.LENGTH_SHORT).show();
    }
}
