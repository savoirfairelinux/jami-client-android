package cx.ring.utils

import android.graphics.Color
import org.json.JSONObject

enum class BackgroundType(val backgroundType: String) {
    COLOR("color"),
    IMAGE("image"),
    DEFAULT("default")
}

enum class LogoFileType(val logoFileType: String) {
    PNG("png"),
    GIF("gif"),
    JPG("jpg"),
    SVG("svg"),
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
    val logoSize: Int?, // The logo size from 0 to 100. 100% if left undefined.
)

fun getUiCustomizationFromConfigJson(configurationJson: JSONObject): UiCustomization {
    return UiCustomization(
        title = configurationJson.optString("title", null),
        description = configurationJson.optString("description", null),
        areTipsEnabled = configurationJson.optBoolean("areTipsEnabled", true),
        backgroundType =
        when (configurationJson.optString("backgroundType", "default")) {
            "color" -> BackgroundType.COLOR
            "image" -> BackgroundType.IMAGE
            else -> BackgroundType.DEFAULT
        },
        backgroundColor =
        try {
            Color.parseColor(configurationJson.optString("backgroundColorOrUrl", null))
        } catch (e: Exception) {
            null
        },
        backgroundUrl = configurationJson.optString("backgroundColorOrUrl", null),
        tipBoxAndIdColor = Color.parseColor(configurationJson.optString("tipBoxAndIdColor", null)),
//        if (configurationJson.isNull("tipBoxAndIdColor")) null
//        else configurationJson.getInt("tipBoxAndIdColor"),
        mainBoxColor =  Color.parseColor(configurationJson.optString("mainBoxColor", null)),
//        if (configurationJson.isNull("mainBoxColor")) null
//        else configurationJson.getInt("mainBoxColor"),
        logoUrl = configurationJson.optString("logoUrl", null),
        logoSize =
        if (configurationJson.isNull("logoSize")) null
        else configurationJson.getInt("logoSize"),
    )
}
