package cx.ring.backup

import android.content.Context
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files

internal object AccountDataBackup {
    const val MARKER_PATH = "backup/account-transfer.json"
    private const val VERSION = 1
    private val accountId = Regex("[0-9a-fA-F]{1,16}")
    private val deviceCredentials = setOf("ring_device.key", "ring_device.crt")

    fun files(directory: File): Sequence<File> = entries(directory).filter { it.isFile }

    private fun entries(directory: File): Sequence<File> = directory.walkTopDown()
        .onEnter { !Files.isSymbolicLink(it.toPath()) }
        .onFail { _, exception -> throw exception }
        .filter { (it.isFile || it.isDirectory) && !Files.isSymbolicLink(it.toPath()) }
        .filterNot { isDeviceCredential(it.relativeTo(directory).invariantSeparatorsPath) }

    fun isDeviceCredential(relativePath: String): Boolean {
        val components = relativePath.split('/')
        return components.size == 2 && components.last() in deviceCredentials
    }

    fun transferFiles(context: Context): Sequence<File> = transferEntries(context).filter { it.isFile }

    fun transferEntries(context: Context): Sequence<File> = sequence {
        yieldAll(entries(context.filesDir).filter {
            val relative = it.relativeTo(context.filesDir).invariantSeparatorsPath
            accountId.matches(relative.substringBefore('/'))
        })
        yieldAll(entries(context.getDir("config", Context.MODE_PRIVATE)))
    }

    fun marker(fileCount: Int): ByteArray = JSONObject()
        .put("version", VERSION).put("files", fileCount).toString().toByteArray(Charsets.UTF_8)

    fun readMarker(input: InputStream, size: Long): Int {
        require(size in 1..1024)
        val bytes = ByteArray(size.toInt())
        java.io.DataInputStream(input).readFully(bytes)
        val parser = JSONTokener(bytes.toString(Charsets.UTF_8))
        val marker = parser.nextValue() as? JSONObject ?: throw IOException("Invalid account transfer marker")
        require(parser.nextClean() == '\u0000')
        require(marker.keys().asSequence().toSet() == setOf("version", "files"))
        require(marker.get("version") == VERSION)
        val count = marker.get("files")
        require(count is Int && count >= 0)
        return count
    }

    fun restorePath(context: Context, destination: File): String? {
        for ((domain, root) in roots(context)) {
            val relative = destination.absoluteFile.relativeToOrNull(root.absoluteFile) ?: continue
            val name = relative.invariantSeparatorsPath
                if (relative.isAbsolute || name.split('/').any { it == ".." || it == "." }) continue
            if (destination.canonicalFile != File(root.canonicalFile, name)) continue
                if (domain == "files" && (!accountId.matches(name.substringBefore('/')) || isDeviceCredential(name))) continue
                return if (name.isEmpty()) domain else "$domain/$name"
        }
        return null
    }

    fun stage(context: Context, path: String, input: InputStream, size: Long) {
        require(size >= 0)
        require(path != "config" && (path.startsWith("config/") || path.count { it == '/' } >= 2))
        val destination = File(staged(context), path)
        require(!destination.exists()) { "Duplicate account transfer file" }
        check(destination.parentFile!!.isDirectory || destination.parentFile!!.mkdirs())
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(32 * 1024)
            var remaining = size
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (count <= 0) throw java.io.EOFException("Truncated account transfer file")
                output.write(buffer, 0, count)
                remaining -= count
            }
            output.fd.sync()
        }
    }

    fun stageDirectory(context: Context, path: String) {
        val destination = File(staged(context), path)
        check(destination.isDirectory || destination.mkdirs())
    }

    fun finishRestore(context: Context, fileCount: Int?) {
        if (fileCount == null) {
            discardStaged(context)
            return
        }
        val staged = staged(context)
        check(staged.isDirectory || staged.mkdirs())
        require(files(staged).count() == fileCount) { "Incomplete account transfer" }
        check(!pending(context).exists()) { "An account restore is already pending" }
        check(staged.renameTo(pending(context))) { "Unable to publish account restore" }
    }

    fun hasPending(context: Context): Boolean = pending(context).exists()

    fun applyPending(context: Context) {
        val pending = pending(context)
        if (!pending.exists()) return
        for ((domain, root) in roots(context)) {
            val source = File(pending, domain)
            if (!source.exists()) continue
            if (domain == "files") {
                for (account in source.listFiles() ?: throw IOException("Unable to list restored accounts")) {
                    require(account.isDirectory && accountId.matches(account.name))
                    replace(account, File(root, account.name))
                }
            } else {
                replace(source, root)
            }
        }
        delete(pending)
    }

    fun discardStaged(context: Context) = delete(staged(context))

    private fun roots(context: Context) = listOf(
        "files" to context.filesDir, "config" to context.getDir("config", Context.MODE_PRIVATE)
    )

    private fun staged(context: Context) = File(context.noBackupFilesDir, "restore/accounts-staged")
    private fun pending(context: Context) = File(context.noBackupFilesDir, "restore/accounts-pending")

    private fun replace(source: File, destination: File) {
        delete(destination)
        check(source.renameTo(destination)) { "Unable to apply account restore" }
    }

    private fun delete(directory: File) {
        if (!directory.exists() && !Files.isSymbolicLink(directory.toPath())) return
        Files.walk(directory.toPath()).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        }
    }
}