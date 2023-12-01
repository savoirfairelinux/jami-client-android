package cx.ring.utils

import android.content.Context
import java.io.File

object DebugUtils {
    val TAG = DebugUtils::class.simpleName!!

    fun writeDebugLog(context: Context, fileName: String, message: String) =
        File(context?.cacheDir, fileName).apply {
            if (!exists()) createNewFile()
            printWriter().use { it.println(message) }
        }

    fun writeDebugPut(context: Context, message: String) =
        writeDebugLog(context, "debugPut", message)

}