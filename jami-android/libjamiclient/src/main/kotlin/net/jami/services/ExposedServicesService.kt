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

import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.utils.Log

enum class ExposedServiceType(val value: String) {
    CUSTOM("custom"),
    EMBEDDED("embedded");
}

data class ExposedServiceInfo(
    val id: String = "",
    val type: ExposedServiceType = ExposedServiceType.CUSTOM,
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

open class ExposedServicesService {
    open fun getExposedServices(accountId: String): List<ExposedServiceInfo> {
        return try {
            val services = mutableListOf<ExposedServiceInfo>()
            val vectMap = JamiService.getExposedServices(accountId)
            for (i in 0 until vectMap.size) {
                val serviceMap = vectMap.get(i)
                val service = mapToExposedServiceInfo(serviceMap)
                services.add(service)
            }
            services
        } catch (e: Exception) {
            Log.e(TAG, "Error getting exposed services", e)
            emptyList()
        }
    }

    open fun addExposedService(accountId: String, service: ExposedServiceInfo): String {
        return try {
            Log.i(TAG, "addExposedService: accountId='$accountId' name='${service.name}' type=${service.type} directory='${service.directory}' localHost='${service.localHost}' localPort=${service.localPort}")
            val details = buildServiceMap(service)
            Log.i(TAG, "addExposedService: StringMap keys/values:")
            val keysToLog = listOf(TYPE_KEY, ID_KEY, NAME_KEY, LOCAL_HOST_KEY, LOCAL_PORT_KEY, SCHEME_KEY, DIRECTORY_KEY, ENABLED_KEY, POLICY_KEY)
            for (key in keysToLog) {
                val v = details.get(key)
                if (v != null) Log.i(TAG, "  $key = '$v'")
            }
            val serviceId = JamiService.addExposedService(accountId, details)
            Log.i(TAG, "addExposedService: daemon returned serviceId='$serviceId' (empty=${serviceId.isEmpty()})")
            serviceId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding exposed service", e)
            ""
        }
    }

    open fun updateExposedService(accountId: String, service: ExposedServiceInfo): Boolean {
        return try {
            val details = buildServiceMap(service)
            JamiService.updateExposedService(accountId, details)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating exposed service", e)
            false
        }
    }

    open fun removeExposedService(accountId: String, serviceId: String): Boolean {
        return try {
            JamiService.removeExposedService(accountId, serviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing exposed service", e)
            false
        }
    }

    fun setServiceEnabled(accountId: String, service: ExposedServiceInfo, enabled: Boolean): Boolean {
        return updateExposedService(accountId, service.copy(enabled = enabled))
    }

    private fun buildServiceMap(service: ExposedServiceInfo): StringMap {
        val map = StringMap()
        if (service.id.isNotEmpty()) map.set(ID_KEY, service.id)
        map.set(TYPE_KEY, service.type.value)
        map.set(NAME_KEY, service.name)
        map.set(DESCRIPTION_KEY, service.description)
        if (service.type == ExposedServiceType.EMBEDDED) {
            map.set(LOCAL_HOST_KEY, LOCALHOST)
            map.set(LOCAL_PORT_KEY, service.localPort.toString())
            map.set(SCHEME_KEY, "http")
            map.set(DIRECTORY_KEY, service.directory)
        } else {
            map.set(LOCAL_HOST_KEY, service.localHost.ifEmpty { LOCALHOST })
            map.set(LOCAL_PORT_KEY, service.localPort.toString())
            if (service.scheme.isNotEmpty()) map.set(SCHEME_KEY, service.scheme)
        }
        map.set(ENABLED_KEY, if (service.enabled) "true" else "false")
        if (service.policy.isNotEmpty()) map.set(POLICY_KEY, service.policy)
        if (service.allowedContacts.isNotEmpty()) map.set(ALLOWED_CONTACTS_KEY, service.allowedContacts)
        return map
    }

    private fun mapToExposedServiceInfo(map: StringMap): ExposedServiceInfo {
        return ExposedServiceInfo(
            id = map.get(ID_KEY) ?: "",
            type = ExposedServiceType.entries.firstOrNull { it.value == map.get(TYPE_KEY) }
                ?: ExposedServiceType.CUSTOM,
            name = map.get(NAME_KEY) ?: "",
            description = map.get(DESCRIPTION_KEY) ?: "",
            localHost = map.get(LOCAL_HOST_KEY) ?: LOCALHOST,
            localPort = map.get(LOCAL_PORT_KEY)?.toIntOrNull() ?: 0,
            scheme = map.get(SCHEME_KEY) ?: "",
            directory = map.get(DIRECTORY_KEY) ?: "",
            enabled = (map.get(ENABLED_KEY) ?: "true").lowercase() == "true",
            policy = map.get(POLICY_KEY) ?: "contacts",
            allowedContacts = map.get(ALLOWED_CONTACTS_KEY) ?: "",
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
