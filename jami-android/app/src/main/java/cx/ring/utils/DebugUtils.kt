package cx.ring.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

object DebugUtils {
    val TAG = DebugUtils::class.simpleName!!

    fun appendLog(context: Context, fileName: String, message: String) =
        File(context?.cacheDir, fileName).apply {
            if (!exists()) createNewFile()
            FileOutputStream(this, true).bufferedWriter().use { writer ->
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    .let { timestamp -> writer.append("\n$timestamp $message") }
            }
        }

    fun appendDebugPut(context: Context, message: String) = appendLog(context, "debugPut", message)
}