package cx.ring.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.utils.Log;

public class PluginsListSettingsFragment extends Fragment {

    public static final String TAG = PluginsListSettingsFragment.class.getSimpleName();
    private Context mContext;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mContext = requireActivity();
        View pluginsSettingsList = inflater.inflate(R.layout.frag_plugins_list_settings, container, false);
        recyclerView = (RecyclerView) pluginsSettingsList.findViewById(R.id.plugins_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)

        mAdapter = new PluginsListAdapter(listPlugins());
        recyclerView.setAdapter(mAdapter);

        return pluginsSettingsList;
    }


    private List<String> listPlugins(){
        List<String> pluginsList = new ArrayList<>();
        File pluginsDir = new File(mContext.getFilesDir(),"plugins");
        Log.i(TAG, "Plugins dir: " + pluginsDir.getAbsolutePath());
        if(pluginsDir.isDirectory()) {
            File[] pluginsFolders = pluginsDir.listFiles();
            if(pluginsFolders != null) {
                for(File pluginFolder : pluginsFolders) {
                    if(pluginFolder.isDirectory()){
                        pluginsList.add(pluginFolder.getName());
                    }
                }
            }
        }
        return pluginsList;
    }
}
