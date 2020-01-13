package cx.ring.settings.pluginssettings;

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
    private Map<String, String>  details;
    private Drawable icon;
    private boolean enabled;

    public PluginDetails(String name, String rootPath, boolean enabled) {
        this.name = name;
        this.rootPath = rootPath;
        this.enabled = enabled;
        details = getPluginDetails();
        setIcon();
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
        return details.get("soPath") != null ? details.get("soPath") : "";
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

    public void setIcon() {
        String iconPath = details.get("iconPath");

        if(iconPath != null) {
            File file = new File(iconPath);

            Log.i(TAG, "Plugin icon path: " + iconPath);

            if(file.exists()) {
                icon = Drawable.createFromPath(iconPath);
            }
        }
    }

    public Drawable getIcon() {
        return icon;
    }

    public Map<String, String> getPluginDetails() {
        return Ringservice.getPluginDetails(getRootPath()).toNative();
    }

    public List<Map<String,String>> getPluginPreferences() {
        return Ringservice.getPluginPreferences(getRootPath()).toNative();
    }

    public Map<String, String> getPluginPreferencesValues() {
        return Ringservice.getPluginPreferencesValues(getRootPath()).toNative();
    }

    public boolean setPluginPreference(String key, String value) {
        return Ringservice.setPluginPreference(getRootPath(), key, value);
    }

}
