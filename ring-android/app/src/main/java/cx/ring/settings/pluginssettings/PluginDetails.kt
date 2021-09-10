/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.settings.pluginssettings

import android.graphics.drawable.Drawable
import net.jami.daemon.JamiService
import java.io.File

/**
 * Class that contains PluginDetails like name, rootPath
 */
class PluginDetails(var name: String, rootPath: String, enabled: Boolean, val handlerId: String? = null) {
    var rootPath: String = rootPath
        private set
    private val details: Map<String, String> = pluginDetails
    var icon: Drawable? = null
        private set

    /**
     * Returns the plugin activation status by the user
     * @return boolean
     */
    var isEnabled: Boolean = enabled

    fun setIcon() {
        var iconPath = details["iconPath"]
        if (iconPath != null) {
            if (iconPath.endsWith("svg"))
                iconPath = iconPath.replace(".svg", ".png")
            val file = File(iconPath)
            if (file.exists()) {
                icon = Drawable.createFromPath(iconPath)
            }
        }
    }

    private val pluginDetails: Map<String, String>
        get() = JamiService.getPluginDetails(rootPath).toNative()
    val pluginPreferences: List<Map<String, String>>
        get() = JamiService.getPluginPreferences(rootPath).toNative()
    val pluginPreferencesValues: Map<String, String>
        get() = JamiService.getPluginPreferencesValues(rootPath)

    fun setPluginPreference(key: String, value: String): Boolean {
        return JamiService.setPluginPreference(rootPath, key, value)
    }

    companion object {
        val TAG = PluginDetails::class.simpleName!!
    }

    init {
        setIcon()
    }
}