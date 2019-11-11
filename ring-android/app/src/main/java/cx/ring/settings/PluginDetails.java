package cx.ring.settings;

import android.graphics.drawable.Drawable;

import java.io.File;
import java.util.List;
import java.util.Map;

import cx.ring.daemon.Ringservice;
import cx.ring.utils.Log;

/**
 * Class that contains PluginDetails like name, rootPath
 */
public class PluginDetails {
    public static final String TAG = PluginDetails.class.getSimpleName();
    private String name;
    private String rootPath;
    private boolean enabled;

    public PluginDetails(String name, String rootPath, boolean enabled) {
        this.name = name;
        this.rootPath = rootPath;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRootPath() {
        return rootPath;
    }

    /**
     * @return String: absolute path to the so file
     */
    public String getSoPath() {
        return rootPath + "/lib" + name + ".so";
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Returns the plugin activation status by the user
     * @return boolean
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Drawable getPluginIcon() {
        String iconPath = Ringservice.getPluginIconPath(getSoPath());
        Drawable icon = null;

        File file = new File(iconPath);

        Log.i(TAG, "Plugin icon path: " + iconPath);

        if(file.exists()) {
            icon = Drawable.createFromPath(iconPath);
        }

        return icon;
    }

    public List<Map<String,String>> getPluginPreferences() {
        return Ringservice.getPluginPreferences(getSoPath()).toNative();
    }

    public Map<String, String> getPluginPreferencesValues() {
        return Ringservice.getPluginPreferencesValuesMap(getSoPath()).toNative();
    }

    public boolean setPluginPreference(String key, String value) {
        return Ringservice.setPluginPreference(getSoPath(), key, value);
    }

}
