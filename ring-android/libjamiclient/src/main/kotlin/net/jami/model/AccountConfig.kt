/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        const val STATE_REGISTERED = "REGISTERED"
        const val STATE_READY = "READY"
        const val STATE_UNREGISTERED = "UNREGISTERED"
        const val STATE_TRYING = "TRYING"
        const val STATE_ERROR = "ERROR"
        const val STATE_ERROR_GENERIC = "ERROR_GENERIC"
        const val STATE_ERROR_AUTH = "ERROR_AUTH"
        const val STATE_ERROR_NETWORK = "ERROR_NETWORK"
        const val STATE_ERROR_HOST = "ERROR_HOST"
        const val STATE_ERROR_CONF_STUN = "ERROR_CONF_STUN"
        const val STATE_ERROR_EXIST_STUN = "ERROR_EXIST_STUN"
        const val STATE_ERROR_SERVICE_UNAVAILABLE = "ERROR_SERVICE_UNAVAILABLE"
        const val STATE_ERROR_NOT_ACCEPTABLE = "ERROR_NOT_ACCEPTABLE"
        const val STATE_REQUEST_TIMEOUT = "Request Timeout"
        const val STATE_INITIALIZING = "INITIALIZING"
        const val STATE_NEED_MIGRATION = "ERROR_NEED_MIGRATION"
        const val STATE_SUCCESS = "SUCCESS"
        const val STATE_INVALID = "INVALID"
    }
}