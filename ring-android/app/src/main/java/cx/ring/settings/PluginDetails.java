package cx.ring.settings;

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

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
