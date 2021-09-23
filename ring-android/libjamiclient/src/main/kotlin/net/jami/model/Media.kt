package net.jami.model

import net.jami.daemon.StringMap
import java.util.*

data class Media(val source: String?,
                 val mediaType: MediaType?,
                 val label: String?,
                 val isEnabled: Boolean,
                 val isOnHold: Boolean,
                 val isMuted: Boolean
) {
    constructor(mediaMap: Map<String, String>) : this(
            source = mediaMap[SOURCE_KEY],
            mediaType = MediaType.parseMediaType(mediaMap[MEDIA_TYPE_KEY]!!),
            label = mediaMap[LABEL_KEY],
            isEnabled = java.lang.Boolean.parseBoolean(mediaMap[ENABLED_KEY]),
            isOnHold = java.lang.Boolean.parseBoolean(mediaMap[ON_HOLD_KEY]),
            isMuted = java.lang.Boolean.parseBoolean(mediaMap[MUTED_KEY])
    )

    constructor(type: MediaType, label: String) : this(
            source = "",
            mediaType = type,
            label = label,
            isEnabled = true,
            isOnHold = false,
            isMuted = false
    )

    enum class MediaType {
        MEDIA_TYPE_AUDIO,
        MEDIA_TYPE_VIDEO;

        companion object {
            fun parseMediaType(mediaType: String): MediaType? {
                for (mt in values()) {
                    if (mt.name.contains(mediaType)) {
                        return mt
                    }
                }
                return null
            }

            fun getMediaTypeString(mediaType: MediaType?): String {
                return mediaType?.name ?: "NULL"
            }
        }
    }

    fun toMap(): StringMap {
        val map = StringMap()
        if (source != null) map[SOURCE_KEY] = source
        map[MEDIA_TYPE_KEY] = MediaType.getMediaTypeString(mediaType)
        if (label != null) map[LABEL_KEY] = label
        map[ENABLED_KEY] = java.lang.Boolean.toString(isEnabled)
        map[ON_HOLD_KEY] = java.lang.Boolean.toString(isOnHold)
        map[MUTED_KEY] = java.lang.Boolean.toString(isMuted)
        return map
    }

    companion object {
        private const val SOURCE_KEY = "SOURCE"
        const val MEDIA_TYPE_KEY = "MEDIA_TYPE"
        private const val LABEL_KEY = "LABEL"
        private const val ENABLED_KEY = "ENABLED"
        private const val ON_HOLD_KEY = "ON_HOLD"
        const val MUTED_KEY = "MUTED"
        val DEFAULT_AUDIO = Media(MediaType.MEDIA_TYPE_AUDIO, "audio_0")
        val DEFAULT_VIDEO = Media(MediaType.MEDIA_TYPE_VIDEO, "video_0")
    }
}
