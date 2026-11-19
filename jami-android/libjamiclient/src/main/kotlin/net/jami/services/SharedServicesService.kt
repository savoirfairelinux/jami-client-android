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
package net.jami.services

import io.reactivex.rxjava3.core.Observable
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.utils.Log

enum class SharedServiceType(val value: String) {
    CUSTOM("custom"),
    EMBEDDED("embedded");
}

data class SharedServiceInfo(
    val id: String = "",
    val type: SharedServiceType = SharedServiceType.CUSTOM,
    val name: String = "",
    val description: String = "",
    val localHost: String = "localhost",
    val localPort: Int = 0,
    val scheme: String = "",
    val directory: String = "",
    val enabled: Boolean = true,
    val policy: String = "contacts",
    val allowedContacts: String = "",
)

open class SharedServicesService {
    open fun getSharedServices(accountId: String): List<SharedServiceInfo> {
        return try {
            val services = mutableListOf<SharedServiceInfo>()
            val vectMap = JamiService.getSharedServices(accountId)
            for (i in 0 until vectMap.size) {
                val serviceMap = vectMap.get(i)
                val service = mapToSharedServiceInfo(serviceMap)
                services.add(service)
            }
            services
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shared services", e)
            emptyList()
        }
    }

    open fun addSharedService(accountId: String, service: SharedServiceInfo): String {
        return try {
            Log.i(TAG, "addSharedService: accountId='$accountId' name='${service.name}' type=${service.type} directory='${service.directory}' localHost='${service.localHost}' localPort=${service.localPort}")
            val details = buildServiceMap(service)
            Log.i(TAG, "addSharedService: StringMap keys/values:")
            val keysToLog = listOf(TYPE_KEY, ID_KEY, NAME_KEY, LOCAL_HOST_KEY, LOCAL_PORT_KEY, SCHEME_KEY, DIRECTORY_KEY, ENABLED_KEY, POLICY_KEY)
            for (key in keysToLog) {
                val v = details.get(key)
                if (v != null) Log.i(TAG, "  $key = '$v'")
            }
            val serviceId = JamiService.addSharedService(accountId, details)
            Log.i(TAG, "addSharedService: daemon returned serviceId='$serviceId' (empty=${serviceId.isEmpty()})")
            serviceId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding shared service", e)
            ""
        }
    }

    open fun updateSharedService(accountId: String, service: SharedServiceInfo): Boolean {
        return try {
            val details = buildServiceMap(service)
            JamiService.updateSharedService(accountId, details)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shared service", e)
            false
        }
    }

    open fun removeSharedService(accountId: String, serviceId: String): Boolean {
        return try {
            JamiService.removeSharedService(accountId, serviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing shared service", e)
            false
        }
    }

    fun setServiceEnabled(accountId: String, service: SharedServiceInfo, enabled: Boolean): Boolean =
        updateSharedService(accountId, service.copy(enabled = enabled))

    /** Emits true while at least one embedded server is being hosted on this device. */
    open fun observeHostingActive(): Observable<Boolean> = Observable.just(false)

    /** Returns the currently running hosted services as (accountId, service) pairs. */
    open fun getRunningHostedServices(): List<Pair<String, SharedServiceInfo>> = emptyList()

    /** Disables every enabled embedded service across all accounts (persisted). */
    open fun disableAllHostedServices() {}

    private fun buildServiceMap(service: SharedServiceInfo): StringMap {
        val map = StringMap()
        if (service.id.isNotEmpty()) map[ID_KEY] = service.id
        map[TYPE_KEY] = service.type.value
        map[NAME_KEY] = service.name
        map[DESCRIPTION_KEY] = service.description
        if (service.type == SharedServiceType.EMBEDDED) {
            map[LOCAL_HOST_KEY] = LOCALHOST
            map[LOCAL_PORT_KEY] = service.localPort.toString()
            map[SCHEME_KEY] = "http"
            map[DIRECTORY_KEY] = service.directory
        } else {
            map[LOCAL_HOST_KEY] = service.localHost.ifEmpty { LOCALHOST }
            map[LOCAL_PORT_KEY] = service.localPort.toString()
            if (service.scheme.isNotEmpty()) map[SCHEME_KEY] = service.scheme
        }
        map[ENABLED_KEY] = if (service.enabled) "true" else "false"
        if (service.policy.isNotEmpty()) map[POLICY_KEY] = service.policy
        if (service.allowedContacts.isNotEmpty()) map[ALLOWED_CONTACTS_KEY] = service.allowedContacts
        return map
    }

    private fun mapToSharedServiceInfo(map: StringMap): SharedServiceInfo {
        return SharedServiceInfo(
            id = map[ID_KEY] ?: "",
            type = SharedServiceType.entries.firstOrNull { it.value == map[TYPE_KEY] }
                ?: SharedServiceType.CUSTOM,
            name = map[NAME_KEY] ?: "",
            description = map[DESCRIPTION_KEY] ?: "",
            localHost = map[LOCAL_HOST_KEY] ?: LOCALHOST,
            localPort = map[LOCAL_PORT_KEY]?.toIntOrNull() ?: 0,
            scheme = map[SCHEME_KEY] ?: "",
            directory = map[DIRECTORY_KEY] ?: "",
            enabled = (map[ENABLED_KEY] ?: "true").lowercase() == "true",
            policy = map[POLICY_KEY] ?: "contacts",
            allowedContacts = map[ALLOWED_CONTACTS_KEY] ?: "",
        )
    }

    companion object {
        private const val TAG = "ExposedServicesService"

        private const val TYPE_KEY = "type"
        private const val ID_KEY = "id"
        private const val NAME_KEY = "name"
        private const val DESCRIPTION_KEY = "description"
        private const val LOCAL_HOST_KEY = "localHost"
        private const val LOCAL_PORT_KEY = "localPort"
        private const val SCHEME_KEY = "scheme"
        private const val DIRECTORY_KEY = "directory"
        private const val ENABLED_KEY = "enabled"
        private const val POLICY_KEY = "policy"
        private const val ALLOWED_CONTACTS_KEY = "allowedContacts"

        private const val LOCALHOST = "localhost"
    }
}
