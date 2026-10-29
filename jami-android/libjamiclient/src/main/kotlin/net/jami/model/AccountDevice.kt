package net.jami.model

import net.jami.utils.Log

data class AccountDevice(val id: String, val name: String, val lastSync: Long = 0) {
    companion object {
        fun fromMap(details: Map<String, String>): AccountDevice {
            val id = details.getValue("id")
            val rawLastSync = details["lastSync"]
            val lastSync = rawLastSync?.toLongOrNull()
            if (rawLastSync != null && lastSync == null) {
                Log.w("AccountDevice", "Invalid lastSync metadata for device $id")
            }
            return AccountDevice(id, details.getValue("label"), lastSync ?: 0L)
        }
    }
}
