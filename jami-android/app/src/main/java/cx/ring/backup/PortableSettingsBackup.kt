package cx.ring.backup

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import org.json.JSONTokener
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

internal object PortableSettingsBackup {
    const val RELATIVE_PATH = "backup/portable-settings.json"
    const val MAX_BYTES = 16 * 1024
    private const val VERSION = 1
    private val booleanKeys = setOf(
        "add_group", "persistent_typing", "persistent_block_record", "link_previews_enable"
    )

    fun snapshot(context: Context): ByteArray {
        val preferences = context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE)
        val settings = JSONObject()
        for (key in booleanKeys) {
            if (preferences.contains(key)) settings.put(key, preferences.getBoolean(key, false))
        }
        if (preferences.contains("persistent_notification")) {
            settings.put("persistent_notification", preferences.getInt("persistent_notification", 0))
        }
        val theme = JSONObject()
        val themePreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        if (themePreferences.contains("darkMode")) {
            theme.put("darkMode", themePreferences.getBoolean("darkMode", false))
        }
        return JSONObject().put("version", VERSION).put("settings", settings).put("theme", theme)
            .toString().toByteArray(Charsets.UTF_8).also { validate(it) }
    }

    fun validate(bytes: ByteArray): JSONObject {
        require(bytes.size <= MAX_BYTES) { "Settings snapshot is too large" }
        val parser = JSONTokener(bytes.toString(Charsets.UTF_8))
        val snapshot = parser.nextValue() as? JSONObject ?: throw IllegalArgumentException("Invalid settings snapshot")
        require(parser.nextClean() == '\u0000') { "Trailing data in settings snapshot" }
        require(snapshot.keys().asSequence().toSet() == setOf("version", "settings", "theme"))
        require(snapshot.get("version") == VERSION) { "Unsupported settings snapshot version" }
        val settings = snapshot.getJSONObject("settings")
        for (key in settings.keys()) {
            val value = settings.get(key)
            require(
                key in booleanKeys && value is Boolean ||
                    key == "persistent_notification" && value is Int && value in 0..2
            ) { "Unsupported setting" }
        }
        val theme = snapshot.getJSONObject("theme")
        for (key in theme.keys()) {
            require(key == "darkMode" && theme.get(key) is Boolean) { "Unsupported theme setting" }
        }
        return snapshot
    }

    fun stage(context: Context, input: InputStream, size: Long) {
        require(size in 1..MAX_BYTES.toLong()) { "Invalid settings snapshot size" }
        val bytes = ByteArray(size.toInt())
        DataInputStream(input).readFully(bytes)
        validate(bytes)
        write(staged(context), bytes)
    }

    fun finishRestore(context: Context) {
        val staged = staged(context)
        val bytes = read(staged) ?: return
        validate(bytes)
        write(pending(context), bytes)
        staged.delete()
    }

    fun applyPending(context: Context): Boolean {
        val pending = pending(context)
        val bytes = read(pending) ?: return false
        val snapshot = validate(bytes)
        val settings = snapshot.getJSONObject("settings")
        val settingsEditor = context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE).edit()
        for (key in settings.keys()) {
            when (val value = settings.get(key)) {
                is Boolean -> settingsEditor.putBoolean(key, value)
                is Int -> settingsEditor.putInt(key, value)
            }
        }
        if (!settingsEditor.commit()) throw IOException("Unable to restore portable settings")
        val theme = snapshot.getJSONObject("theme")
        val themeEditor = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit()
        if (theme.has("darkMode")) themeEditor.putBoolean("darkMode", theme.getBoolean("darkMode"))
        if (!themeEditor.commit()) throw IOException("Unable to restore theme")
        pending.delete()
        return true
    }

    fun discardStaged(context: Context) = staged(context).delete()

    private fun staged(context: Context) = AtomicFile(File(context.noBackupFilesDir, "restore/settings-staged.json"))
    private fun pending(context: Context) = AtomicFile(File(context.noBackupFilesDir, "restore/settings-pending.json"))

    private fun read(file: AtomicFile): ByteArray? {
        return try {
            file.openRead().use { input ->
                require(input.channel.size() <= MAX_BYTES) { "Settings snapshot is too large" }
                val bytes = input.readBytes()
                require(bytes.size <= MAX_BYTES) { "Settings snapshot is too large" }
                bytes
            }
        } catch (_: java.io.FileNotFoundException) {
            null
        }
    }

    private fun write(file: AtomicFile, bytes: ByteArray) {
        file.baseFile.parentFile?.let { directory ->
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Unable to stage settings")
        }
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (exception: Exception) {
            file.failWrite(output)
            throw exception
        }
    }
}