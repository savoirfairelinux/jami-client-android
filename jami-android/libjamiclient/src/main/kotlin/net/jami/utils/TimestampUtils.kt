package net.jami.utils

object TimestampUtils {
    fun fromDetails(details: Map<String, String>, key: String): Long {
        val millisecondsKey = "${key}Ms"
        val timestampKey = if (details.containsKey(millisecondsKey)) millisecondsKey else key
        val rawTimestamp = details[timestampKey] ?: return 0L
        val timestamp = rawTimestamp.toLongOrNull()
        if (timestamp == null || timestamp < 0L
            || (timestampKey == key && timestamp > Long.MAX_VALUE / 1000L)) {
            Log.w("TimestampUtils", "Invalid $timestampKey timestamp metadata")
            return 0L
        }
        return if (timestampKey == millisecondsKey) timestamp else timestamp * 1000L
    }
}
