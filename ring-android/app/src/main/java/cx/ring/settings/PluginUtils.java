package cx.ring.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cx.ring.utils.Log;

import static android.content.Context.MODE_PRIVATE;

public class PluginUtils {

    public static final String TAG = PluginUtils.class.getSimpleName();

    public static final String PLUGIN_ENABLED = "enabled";
    /**
     * Fetches the plugins folder in the internal storage for plugins subfolder
     * Gathers the details of each plugin in a PluginDetails instance
     * @return
     */
    public static List<PluginDetails> listPlugins(Context mContext){
        List<PluginDetails> pluginsList = new ArrayList<>();
        File pluginsDir = new File(mContext.getFilesDir(),"plugins");
        Log.i(TAG, "Plugins dir: " + pluginsDir.getAbsolutePath());
        if(pluginsDir.isDirectory()) {
            File[] pluginsFolders = pluginsDir.listFiles();
            if(pluginsFolders != null) {
                for(File pluginFolder : pluginsFolders) {
                    if(pluginFolder.isDirectory()){
                        //We use the absolute path of a plugin as a preference name for uniqueness
                        SharedPreferences sp = mContext.getSharedPreferences(
                                pluginFolder.getName(), MODE_PRIVATE);

                        boolean enabled = sp.getBoolean(PLUGIN_ENABLED,false);

                        pluginsList.add(new PluginDetails(
                                pluginFolder.getName(),
                                pluginFolder.getAbsolutePath(),enabled));
                    }
                }
            }
        }
        return pluginsList;
    }

    public static Drawable getPluginIcon(PluginDetails pluginDetails) {
        String iconPath = pluginDetails.getRootPath() + File.separator + "data"+
                File.separator + "icon.png";

        Drawable icon = null;

        File file = new File(iconPath);

        Log.i(TAG, "Plugin icon path: " + iconPath);

        if(file.exists()) {
            icon = Drawable.createFromPath(iconPath);
        }

        return icon;
    }
}
