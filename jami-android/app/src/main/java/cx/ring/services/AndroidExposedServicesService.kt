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

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import net.jami.daemon.JamiService
import net.jami.services.AccountService
import net.jami.services.ExposedServiceInfo
import net.jami.services.ExposedServiceType
import net.jami.services.ExposedServicesService
import net.jami.utils.Log
import java.io.File
import java.io.IOException

class AndroidExposedServicesService(
    private val accountService: AccountService,
    private val context: Context,
) : ExposedServicesService() {
    /** key = "$accountId/$serviceId" */
    private val runningServers = mutableMapOf<String, NanoHTTPD>()

    private fun resolveAccountId(accountId: String): String {
        if (accountId.isNotEmpty()) return accountId
        val fallback = accountService.currentAccount?.accountId ?: ""
        if (fallback.isEmpty())
            Log.e(TAG, "resolveAccountId: no current account")
        else
            Log.i(TAG, "resolveAccountId: accountId was empty, using current account '$fallback'")
        return fallback
    }

    private fun isContentUri(path: String) = path.startsWith("content://")

    private fun resolveAbsolutePath(path: String): String {
        if (path.isEmpty() || isContentUri(path) || path.startsWith("/")) return path
        val externalRoot = android.os.Environment.getExternalStorageDirectory().absolutePath
        return "$externalRoot/$path"
    }

    private fun isDirectoryAccessible(directory: String): Boolean {
        if (directory.isEmpty()) return false
        if (isContentUri(directory)) {
            val uri = android.net.Uri.parse(directory)
            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
            return doc != null && doc.isDirectory
        }
        val f = File(resolveAbsolutePath(directory))
        return f.exists() && f.isDirectory
    }

    private fun serverKey(accountId: String, serviceId: String) = "$accountId/$serviceId"

    private fun startServer(accountId: String, serviceId: String, directory: String): NanoHTTPD? {
        val server: NanoHTTPD = if (isContentUri(directory)) {
            DocumentFileHttpServer.create(directory, context)
                ?: return run { Log.e(TAG, "startServer: invalid SAF URI '$directory'"); null }
        } else {
            val rootDir = File(resolveAbsolutePath(directory))
            if (!rootDir.exists() || !rootDir.isDirectory) {
                Log.e(TAG, "startServer: directory not found: '${rootDir.absolutePath}'")
                return null
            }
            EmbeddedHttpServer(rootDir)
        }
        return try {
            server.start()
            Log.i(TAG, "startServer: port=${server.listeningPort} dir='$directory'")
            runningServers[serverKey(accountId, serviceId)] = server
            server
        } catch (e: IOException) {
            Log.e(TAG, "startServer: failed", e)
            null
        }
    }

    private fun stopServer(accountId: String, serviceId: String) {
        runningServers.remove(serverKey(accountId, serviceId))?.let { s ->
            s.stop()
            Log.i(TAG, "stopServer: stopped '$accountId/$serviceId'")
        }
    }

    private fun stopAllServersForAccount(accountId: String) {
        val prefix = "$accountId/"
        runningServers.keys.filter { it.startsWith(prefix) }.forEach { key ->
            runningServers.remove(key)?.stop()
        }
        Log.i(TAG, "stopAllServersForAccount: cleared all servers for '$accountId'")
    }

    private fun syncEmbeddedServers(accountId: String) {
        if (accountId.isEmpty()) return
        val services = super.getExposedServices(accountId)
        val desiredKeys = mutableSetOf<String>()

        for (service in services) {
            val serviceId = service.id
            if (serviceId.isEmpty()) continue

            if (service.type != ExposedServiceType.EMBEDDED || !service.enabled
                || !isDirectoryAccessible(service.directory)) {
                stopServer(accountId, serviceId)
                continue
            }

            desiredKeys += serverKey(accountId, serviceId)

            val existing = runningServers[serverKey(accountId, serviceId)]
            val server = if (existing != null && existing.isAlive) {
                existing
            } else {
                stopServer(accountId, serviceId)
                startServer(accountId, serviceId, service.directory) ?: continue
            }

            // If the port stored in the daemon is stale (e.g. after app restart),
            // update it so peers receive the correct tunnelling address.
            if (service.localPort != server.listeningPort) {
                Log.i(TAG, "syncEmbeddedServers: updating stale port ${service.localPort} → ${server.listeningPort} for '$serviceId'")
                super.updateExposedService(accountId, service.copy(localPort = server.listeningPort))
            }
        }

        // Prune servers whose services were removed from the daemon externally
        val prefix = "$accountId/"
        val stale = runningServers.keys.filter { it.startsWith(prefix) && it !in desiredKeys }
        stale.forEach { key ->
            runningServers.remove(key)?.stop()
            Log.i(TAG, "syncEmbeddedServers: pruned stale server '$key'")
        }
    }

    override fun addExposedService(accountId: String, service: ExposedServiceInfo): String {
        val id = resolveAccountId(accountId)

        if (service.type != ExposedServiceType.EMBEDDED) {
            val serviceId = super.addExposedService(id, service)
            if (serviceId.isNotEmpty()) syncEmbeddedServers(id)
            return serviceId
        }

        // Start server first to obtain a real port for the daemon
        val server = startServer(id, "", service.directory)
            ?: return run {
                Log.e(TAG, "addExposedService: could not start server for '${service.directory}'")
                ""
            }

        val serviceId = super.addExposedService(id, service.copy(localPort = server.listeningPort))

        if (serviceId.isEmpty()) {
            Log.e(TAG, "addExposedService: daemon rejected — stopping server")
            server.stop()
            runningServers.remove(serverKey(id, ""))
        } else {
            runningServers.remove(serverKey(id, ""))
            runningServers[serverKey(id, serviceId)] = server
            Log.i(TAG, "addExposedService: success id='$serviceId' port=${server.listeningPort}")
        }
        syncEmbeddedServers(id)
        return serviceId
    }

    override fun updateExposedService(accountId: String, service: ExposedServiceInfo): Boolean {
        val id = resolveAccountId(accountId)

        if (service.type != ExposedServiceType.EMBEDDED) {
            val ok = super.updateExposedService(id, service)
            if (ok) syncEmbeddedServers(id)
            return ok
        }

        if (!service.enabled) {
            val lastPort = runningServers[serverKey(id, service.id)]?.listeningPort
                ?: service.localPort
            stopServer(id, service.id)
            val ok = super.updateExposedService(id, service.copy(localPort = lastPort))
            Log.i(TAG, "updateExposedService: disabled id='${service.id}' ok=$ok port=$lastPort")
            syncEmbeddedServers(id)
            return ok
        }

        // Enabled: ensure server is running
        val existing = runningServers[serverKey(id, service.id)]
        val server = if (existing != null && existing.isAlive) {
            existing
        } else {
            stopServer(id, service.id)
            startServer(id, service.id, service.directory) ?: return run {
                Log.e(TAG, "updateExposedService: could not start server for '${service.directory}'")
                false
            }
        }

        val ok = super.updateExposedService(id, service.copy(localPort = server.listeningPort))
        Log.i(TAG, "updateExposedService: id='${service.id}' enabled=true ok=$ok port=${server.listeningPort}")
        syncEmbeddedServers(id)
        return ok
    }

    override fun removeExposedService(accountId: String, serviceId: String): Boolean {
        val id = resolveAccountId(accountId)
        stopServer(id, serviceId)
        val ok = super.removeExposedService(id, serviceId)
        // Sync to prune any other stale entries
        syncEmbeddedServers(id)
        return ok
    }

    override fun getExposedServices(accountId: String): List<ExposedServiceInfo> {
        val id = resolveAccountId(accountId)
        // Sync first so callers (and peers) always see up-to-date port/state
        syncEmbeddedServers(id)
        return super.getExposedServices(id)
    }

    fun syncAllAccounts() {
        val accounts = JamiService.getAccountList()
        Log.i(TAG, "syncAllAccounts: syncing ${accounts.size} account(s)")
        for (i in 0 until accounts.size) {
            val accountId = accounts[i]
            if (accountId.isNotEmpty()) syncEmbeddedServers(accountId)
        }
    }

    companion object {
        private const val TAG = "AndroidExposedServicesService"
    }
}
