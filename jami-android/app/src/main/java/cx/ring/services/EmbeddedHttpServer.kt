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
            target.isFile -> serveFile(target)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }

    private fun serveFile(file: File): Response {
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        return try {
            val length = file.length()
            val response = newFixedLengthResponse(Response.Status.OK, mime, FileInputStream(file), length)
            // Allow browsers/proxies to cache static assets
            val lastModified = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("GMT")
            }.format(java.util.Date(file.lastModified()))
            response.addHeader("Last-Modified", lastModified)
            response.addHeader("Cache-Control", "public, max-age=300")
            response
        } catch (e: IOException) {
            Log.e(TAG, "Error reading file: ${file.path}", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error")
        }
    }

    private fun serveDirectory(dir: File, urlPath: String): Response {
        // Try index.html first
        val index = File(dir, "index.html")
        if (index.isFile) return serveFile(index)

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
