package cx.ring.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.daemon.Ringservice;
import cx.ring.utils.AndroidFileUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static android.content.Context.MODE_PRIVATE;
import static cx.ring.plugins.PluginUtils.PLUGIN_ENABLED;
import static cx.ring.plugins.PluginUtils.listPlugins;

public class PluginsListSettingsFragment extends Fragment implements PluginsListAdapter.PluginListItemListener {

    public static final String TAG = PluginsListSettingsFragment.class.getSimpleName();
    private static final int ARCHIVE_REQUEST_CODE = 42;

    private Context mContext;
    private CoordinatorLayout mCoordinatorLayout;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private FloatingActionButton fab;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mContext = requireActivity();
        View pluginsSettingsList = inflater.inflate(R.layout.frag_plugins_list_settings, container, false);
        mCoordinatorLayout = pluginsSettingsList.
                findViewById(R.id.plugins_list_settings_coordinator_layout);
        mRecyclerView = pluginsSettingsList.findViewById(R.id.plugins_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)

        mAdapter = new PluginsListAdapter(listPlugins(mContext), this);
        mRecyclerView.setAdapter(mAdapter);

        //Fab
        fab = pluginsSettingsList.findViewById(R.id.plugins_list_settings_fab);

        fab.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, ARCHIVE_REQUEST_CODE);
        });

        return pluginsSettingsList;
    }

    @Override
    public void onResume() {
        ((HomeActivity) requireActivity()).
                setToolbarState(false, R.string.menu_item_settings);
        super.onResume();
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
        String status = pluginDetails.isEnabled()?"ON":"OFF";
        Toast.makeText(mContext,pluginDetails.getName() + " " + status,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(file -> {
                                        int i = Ringservice.addPlugin(file.getAbsolutePath());
                                        if (i == 0) {
                                            ((PluginsListAdapter) mAdapter).updatePluginsList(listPlugins(mContext));
                                            Toast.makeText(mContext, "Plugin: " + file.getName() +
                                                    " successfully installed", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(mContext, "Failed to install Plugin: " +
                                                    file.getName(), Toast.LENGTH_LONG).show();
                                        }
                                        // Free the cache
                                        file.delete();
                                    }
                            , e -> {
                                if (mCoordinatorLayout != null)
                                    Snackbar.make(mCoordinatorLayout,
                                            "Can't import plugin: " +
                                                    e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                }
            }
        }
    }
}

