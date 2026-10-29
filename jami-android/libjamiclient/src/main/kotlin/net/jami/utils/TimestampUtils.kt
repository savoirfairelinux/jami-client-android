package net.jami.utils

object TimestampUtils {
    fun fromDetails(details: Map<String, String>, key: String): Long =
        details["${key}Ms"]?.toLong()
            ?: details[key]?.toLong()?.let { Math.multiplyExact(it, 1000L) }
            ?: 0L
}
