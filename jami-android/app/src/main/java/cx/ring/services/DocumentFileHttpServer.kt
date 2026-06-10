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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import fi.iki.elonen.NanoHTTPD
import net.jami.utils.Log
import java.io.IOException
import java.net.ServerSocket

class DocumentFileHttpServer private constructor(
    private val rootDoc: DocumentFile,
    private val contentResolver: ContentResolver,
    private val context: Context,
    port: Int,
) : NanoHTTPD("localhost", port) {
    private val docCache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, DocumentFile>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DocumentFile>) = size > 256
        }
    )

    val boundPort: Int get() = listeningPort

    override fun serve(session: IHTTPSession): Response {
        val urlPath = session.uri.trimStart('/').trimEnd('/')
        val segments = if (urlPath.isEmpty()) emptyList() else urlPath.split('/')

        val target = resolveWithCache(urlPath, segments)

        return when {
            target == null -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            target.isDirectory -> serveDirectory(target, urlPath)
            target.isFile -> serveFile(target)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }

    private fun resolveWithCache(urlPath: String, segments: List<String>): DocumentFile? {
        // Cache hit: return stored TreeDocumentFile directly — no IPC needed
        docCache[urlPath]?.let { return it }
        val result = navigateTo(rootDoc, segments) ?: return null
        docCache[urlPath] = result
        return result
    }

    private fun navigateTo(root: DocumentFile, segments: List<String>): DocumentFile? {
        var current = root
        for (segment in segments) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }

    private fun serveFile(file: DocumentFile): Response {
        val ext = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: file.type ?: "application/octet-stream"
        return try {
            val stream = contentResolver.openInputStream(file.uri)
                ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open file")
            val length = file.length()
            // Set Content-Length so browsers can show progress and pipeline requests.
            // Falls back to chunked if length is unknown (0 means unknown for SAF).
            val response = if (length > 0) {
                newFixedLengthResponse(Response.Status.OK, mime, stream, length)
            } else {
                newChunkedResponse(Response.Status.OK, mime, stream)
            }
            response.addHeader("Cache-Control", "public, max-age=300")
            response
        } catch (e: IOException) {
            Log.e(TAG, "serveFile: error reading '${file.name}'", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error")
        }
    }

    private fun serveDirectory(dir: DocumentFile, urlPath: String): Response {
        // Prefer index.html if present
        val index = dir.findFile("index.html")
        if (index != null && index.isFile) return serveFile(index)

        val displayPath = "/$urlPath".ifEmpty { "/" }
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        sb.append("<title>Index of $displayPath</title>")
        sb.append("<style>body{font-family:sans-serif;margin:2em}a{text-decoration:none;color:#0366d6}a:hover{text-decoration:underline}table{border-collapse:collapse;width:100%}th,td{text-align:left;padding:4px 12px}tr:hover{background:#f6f8fa}</style>")
        sb.append("</head><body><h1>Index of $displayPath</h1><table><tr><th>Name</th><th>Size</th></tr>")

        if (urlPath.isNotEmpty()) {
            val parent = urlPath.substringBeforeLast('/', "")
            sb.append("<tr><td><a href=\"/$parent\">..</a></td><td></td></tr>")
        }

        val children = dir.listFiles()
        children
            .sortedWith(compareBy({ !it.isDirectory }, { it.name?.lowercase() ?: "" }))
            .forEach { entry ->
                val name = entry.name ?: return@forEach
                val isDir = entry.isDirectory
                val href = if (urlPath.isEmpty()) "/$name${if (isDir) "/" else ""}"
                           else "/$urlPath/$name${if (isDir) "/" else ""}"
                val size = if (entry.isFile) formatSize(entry.length()) else ""
                sb.append("<tr><td><a href=\"$href\">$name${if (isDir) "/" else ""}</a></td><td>$size</td></tr>")
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
        private const val TAG = "DocumentFileHttpServer"

        private fun pickFreePort(): Int = try {
            ServerSocket(0).use { it.localPort }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to pick free port", e)
            0
        }

        fun create(treeUri: String, context: Context, port: Int = 0): DocumentFileHttpServer? {
            val uri = Uri.parse(treeUri)
            val doc = DocumentFile.fromTreeUri(context, uri)
            if (doc == null || !doc.isDirectory) {
                Log.e(TAG, "create: URI is not a valid directory tree: $treeUri")
                return null
            }
            val boundPort = if (port == 0) pickFreePort() else port
            Log.i(TAG, "create: root='${doc.name}' uri='${doc.uri}' port=$boundPort")
            return DocumentFileHttpServer(doc, context.contentResolver, context, boundPort)
        }
    }
}
