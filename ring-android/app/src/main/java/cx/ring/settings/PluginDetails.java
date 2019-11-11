package cx.ring.settings;

import java.io.File;

/**
 * Class that contains PluginDetails like name, rootPath
 */
public class PluginDetails {
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

    /**
     * @return String: absolute path to the preferences.json file
     */
    public String getPreferencesPath() {
        return rootPath + File.separator + "data" + File.separator + "preferences.json";
    }

    /**
     * Icon absolute path
     * The icon should ideally be 192x192 pixels or better 512x512 pixels
     * In order to match with android specifications
     * https://developer.android.com/google-play/resources/icon-design-specifications
     * @return String: absolute path tot the icon.png file
     */
    public String getPluginIconPath() {
        return rootPath + File.separator + "data" + File.separator + "icon.png";
    }
}
