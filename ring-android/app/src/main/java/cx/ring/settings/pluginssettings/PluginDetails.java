/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Aline Gondim Santos <aline.gondimsantos@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.settings.pluginssettings;

import android.graphics.drawable.Drawable;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.jami.daemon.Ringservice;

/**
 * Class that contains PluginDetails like name, rootPath
 */
public class PluginDetails {
    public static final String TAG = PluginDetails.class.getSimpleName();
    private String name;
    private String rootPath;
    private final Map<String, String>  details;
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
        if (iconPath != null) {
            File file = new File(iconPath);
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
        return Ringservice.getPluginPreferencesValues(getRootPath());
    }

    public boolean setPluginPreference(String key, String value) {
        return Ringservice.setPluginPreference(getRootPath(), key, value);
    }

}
