/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.settings.extensionssettings

import android.graphics.drawable.Drawable
import android.util.Log
import cx.ring.fragments.CallFragment
import cx.ring.utils.ConversationPath
import net.jami.daemon.JamiService
import java.io.File
import java.util.*

/**
 * Class that contains ExtensionDetails like name, rootPath
 */
class ExtensionDetails(val name: String, val rootPath: String, var isEnabled: Boolean, var handlerId: String?= null, var accountId: String? = "") {
    private val details: Map<String, String> = extensionDetails
    var icon: Drawable? = null
        private set
    var isRunning: Boolean = false

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

    private val extensionDetails: Map<String, String>
        get() = JamiService.getPluginDetails(rootPath).toNative()
    val extensionPreferences: List<Map<String, String>>
        get() = JamiService.getPluginPreferences(rootPath, accountId).toNative()
    val extensionPreferencesValues: Map<String, String>
        get() = JamiService.getPluginPreferencesValues(rootPath, accountId)

    fun setExtensionPreference(key: String, value: String): Boolean {
        return JamiService.setPluginPreference(rootPath, accountId, key, value)
    }

    companion object {
        val TAG = ExtensionDetails::class.simpleName!!
    }

    init {
        setIcon()
    }
}