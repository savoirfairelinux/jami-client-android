/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package cx.ring.utils

import android.graphics.Color
import net.jami.utils.Log
import org.json.JSONObject

private val tag = UiCustomization::class.simpleName!!

enum class BackgroundType {
    COLOR,
    IMAGE,
    DEFAULT
}

data class UiCustomization(
    val title: String?,
    val description: String?,
    val areTipsEnabled: Boolean,
    val backgroundType: BackgroundType,
    val backgroundColor: Int?,
    val backgroundUrl: String?,
    val tipBoxAndIdColor: Int?,
    val mainBoxColor: Int?,
    val logoUrl: String?,
    val logoSize: Int?, // The logo size from 0 to 100%.
)

/**
 * Get the UiCustomization from the configuration JSON
 * @param configurationJson The JSON object containing the configuration
 * @param managerUri The URI of the manager (base url)
 */
fun getUiCustomizationFromConfigJson(
    configurationJson: JSONObject,
    managerUri: String,
): UiCustomization? {
    val logoUrl = if (configurationJson.has("logoUrl"))
        configurationJson.optString("logoUrl") else null
    val backgroundUrl = if (configurationJson.has("backgroundColorOrUrl"))
        configurationJson.optString("backgroundColorOrUrl") else null

    return try {
        UiCustomization(
            title = if (configurationJson.has("title"))
                configurationJson.optString("title") else null,
            description = if (configurationJson.has("description"))
                configurationJson.optString("description") else null,
            areTipsEnabled = configurationJson.optBoolean("areTipsEnabled", true),
            backgroundType = when (configurationJson.optString("backgroundType", "default")) {
                "color" -> BackgroundType.COLOR
                "image" -> BackgroundType.IMAGE
                else -> BackgroundType.DEFAULT
            },
            backgroundColor =
            try {
                Color.parseColor(configurationJson.optString("backgroundColorOrUrl"))
            } catch (e: Exception) {
                null
            },
            backgroundUrl = if (backgroundUrl != null) managerUri + backgroundUrl else null,
            tipBoxAndIdColor =
            try {
                Color.parseColor(configurationJson.optString("tipBoxAndIdColor"))
            } catch (e: Exception) {
                null
            },
            mainBoxColor =
            try {
                Color.parseColor(configurationJson.optString("mainBoxColor"))
            } catch (e: Exception) {
                null
            },
            logoUrl = if (logoUrl != null) managerUri + logoUrl else null,
            logoSize =
            if (configurationJson.isNull("logoSize")) null
            else configurationJson.getInt("logoSize"),
        )
    } catch (e: Exception) {
        Log.e(tag, "Error while parsing the configuration JSON")
        null
    }
}
