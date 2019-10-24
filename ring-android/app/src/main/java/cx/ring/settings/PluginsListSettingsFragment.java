package cx.ring.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

import cx.ring.R;
import cx.ring.utils.Log;

import static cx.ring.utils.AndroidFileUtils.assetTree;

public class PluginsListSettingsFragment extends Fragment {

    public static final String TAG = PluginsListSettingsFragment.class.getSimpleName();
    private Context mContext;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mContext = requireActivity();
        View pluginsSettingsList = inflater.inflate(R.layout.frag_plugins_list_settings, container, false);
        TextView psl = pluginsSettingsList.findViewById(R.id.plugins_list);
        listPlugins(psl);
        assetTree(mContext.getAssets(),"","plugins",0);
        return pluginsSettingsList;
    }


    private void listPlugins(TextView display){
        File pluginsDir = new File(mContext.getFilesDir(),"plugins");
        Log.i(TAG, "Plugins dir: " + pluginsDir.getAbsolutePath());
        if(pluginsDir.isDirectory()) {
            File[] pluginsFolders = pluginsDir.listFiles();
            if(pluginsFolders != null) {
                for(File pluginFolder : pluginsFolders) {
                    Log.i(TAG, "    Plugin: "+ pluginFolder.getName());
                    if(pluginFolder.isDirectory()){
                        display.append("    Plugin: "+ pluginFolder.getName() + " ABS PATH: " +
                                pluginFolder.getAbsolutePath());
                    }
                }
            }
        }
    }
}
