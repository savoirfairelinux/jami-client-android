/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package net.jami.model

import java.util.*
import kotlin.collections.HashMap

class AccountConfig(details: Map<String, String>) {
    private val mValues = EnumMap<ConfigKey, String>(ConfigKey::class.java).apply {
        for ((key, value) in details)
            ConfigKey.fromString(key)?.let { this[it] = value }
    }

    operator fun get(key: ConfigKey): String = mValues[key] ?: ""

    fun getBool(key: ConfigKey): Boolean = TRUE_STR == get(key)

    val all: HashMap<String, String>
        get() = mValues.mapKeysTo(HashMap(mValues.size)) { e -> e.key.key }

    fun put(key: ConfigKey, value: String?) {
        if (value == null)
            mValues.remove(key)
        else
            mValues[key] = value
    }

    fun put(key: ConfigKey, value: Boolean) {
        mValues[key] = if (value) TRUE_STR else FALSE_STR
    }

    val keys: Set<ConfigKey>
        get() = mValues.keys
    val entries: Set<Map.Entry<ConfigKey, String>>
        get() = mValues.entries

    companion object {
        const val TRUE_STR = "true"
        const val FALSE_STR = "false"
        const val ACCOUNT_TYPE_JAMI = "RING"
        const val ACCOUNT_TYPE_SIP = "SIP"
        const val STATE_REQUEST_TIMEOUT = "Request Timeout"
        const val STATE_SUCCESS = "SUCCESS"
        const val STATE_INVALID = "INVALID"
    }

    enum class RegistrationState {
        UNLOADED,
        UNREGISTERED,
        TRYING,
        READY,
        REGISTERED,
        ERROR_GENERIC,
        ERROR_AUTH,
        ERROR_NETWORK,
        ERROR_HOST,
        ERROR_SERVICE_UNAVAILABLE,
        ERROR_NEED_MIGRATION,
        INITIALIZING
    }
}