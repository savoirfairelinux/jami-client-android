/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.services

import android.webkit.MimeTypeMap
import fi.iki.elonen.NanoHTTPD
import net.jami.utils.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.ServerSocket

// Parses "Range: bytes=start-end", "bytes=start-", "bytes=-suffix".
// Returns Pair(start, end) inclusive, or null if absent/invalid.
private fun parseRangeHeader(header: String?, fileSize: Long): Pair<Long, Long>? {
    if (header == null || !header.startsWith("bytes=")) return null
    val spec = header.removePrefix("bytes=").trim()
    val dash = spec.indexOf('-')
    if (dash < 0) return null
    val startStr = spec.substring(0, dash).trim()
    val endStr = spec.substring(dash + 1).trim()
    return try {
        when {
            startStr.isEmpty() -> {
                // bytes=-N: last N bytes
                val suffix = endStr.toLong()
                if (suffix <= 0) return null
                Pair(maxOf(0L, fileSize - suffix), fileSize - 1)
            }
            endStr.isEmpty() -> Pair(startStr.toLong(), fileSize - 1)
            else -> {
                val s = startStr.toLong(); val e = endStr.toLong()
                if (s > e) return null
                Pair(s, minOf(e, fileSize - 1))
            }
        }.takeIf { (s, e) -> s >= 0 && s < fileSize && s <= e }
    } catch (_: NumberFormatException) { null }
}

class EmbeddedHttpServer(
    private val rootDirectory: File,
    port: Int = 0,
) : NanoHTTPD("localhost", if (port == 0) pickFreePort() else port) {
    val boundPort: Int get() = listeningPort

    override fun serve(session: IHTTPSession): Response {
        val uriPath = session.uri.trimStart('/')

        // Resolve the requested path relative to the root, rejecting traversal
        val target = File(rootDirectory, uriPath).canonicalFile
        if (!target.absolutePath.startsWith(rootDirectory.canonicalPath)) {
            Log.w(TAG, "Path traversal rejected: ${session.uri}")
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
        }

        return when {
            target.isDirectory -> serveDirectory(target, uriPath)
            target.isFile -> serveFile(target, session.headers["range"])
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }

    private fun serveFile(file: File, rangeHeader: String?): Response {
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        val lastModifiedFmt = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("GMT") }
            .format(java.util.Date(file.lastModified()))
        return try {
            val fileSize = file.length()
            val range = parseRangeHeader(rangeHeader, fileSize)

            if (range != null) {
                val (start, end) = range
                if (start >= fileSize || start > end) {
                    return newFixedLengthResponse(
                        Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Range Not Satisfiable"
                    ).also { it.addHeader("Content-Range", "bytes */$fileSize") }
                }
                val length = end - start + 1
                val fis = FileInputStream(file)
                fis.channel.position(start)   // efficient lseek — no byte-by-byte skipping
                newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, fis, length).also { r ->
                    r.addHeader("Content-Range", "bytes $start-$end/$fileSize")
                    r.addHeader("Accept-Ranges", "bytes")
                    r.addHeader("Last-Modified", lastModifiedFmt)
                    r.addHeader("Cache-Control", "public, max-age=300")
                }
            } else {
                newFixedLengthResponse(Response.Status.OK, mime, FileInputStream(file), fileSize).also { r ->
                    r.addHeader("Accept-Ranges", "bytes")
                    r.addHeader("Last-Modified", lastModifiedFmt)
                    r.addHeader("Cache-Control", "public, max-age=300")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading file: ${file.path}", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error")
        }
    }

    private fun serveDirectory(dir: File, urlPath: String): Response {
        // Try index.html first
        val index = File(dir, "index.html")
        if (index.isFile) return serveFile(index, null)

        // Generate a directory listing
        val displayPath = "/$urlPath".trimEnd('/').ifEmpty { "/" }
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        sb.append("<title>Index of $displayPath</title>")
        sb.append("<style>body{font-family:sans-serif;margin:2em}a{text-decoration:none;color:#0366d6}a:hover{text-decoration:underline}table{border-collapse:collapse;width:100%}th,td{text-align:left;padding:4px 12px}tr:hover{background:#f6f8fa}</style>")
        sb.append("</head><body><h1>Index of $displayPath</h1><table><tr><th>Name</th><th>Size</th></tr>")

        if (urlPath.isNotEmpty()) {
            val parent = urlPath.trimEnd('/').substringBeforeLast('/', "")
            sb.append("<tr><td><a href=\"/${parent}\">..</a></td><td></td></tr>")
        }

        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.forEach { entry ->
                val name = entry.name
                val href = "/$urlPath".trimEnd('/') + "/$name" + if (entry.isDirectory) "/" else ""
                val size = if (entry.isFile) formatSize(entry.length()) else ""
                sb.append("<tr><td><a href=\"$href\">$name${if (entry.isDirectory) "/" else ""}</a></td><td>$size</td></tr>")
            }

        sb.append("</table></body></html>")
        return newFixedLengthResponse(Response.Status.OK, "text/html", sb.toString())
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }

    companion object {
        private const val TAG = "EmbeddedHttpServer"
        private fun pickFreePort(): Int {
            return try {
                ServerSocket(0).use { it.localPort }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to pick free port", e)
                0
            }
        }
    }
}
