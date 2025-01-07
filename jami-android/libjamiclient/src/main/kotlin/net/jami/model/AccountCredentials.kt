/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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

import java.io.Serializable
import java.util.HashMap

class AccountCredentials : Serializable {
    var username: String? = null
    var password: String? = null
    var realm: String? = null

    constructor(pref: Map<String, String>) {
        username = pref[ConfigKey.ACCOUNT_USERNAME.key]
        password = pref[ConfigKey.ACCOUNT_PASSWORD.key]
        realm = pref[ConfigKey.ACCOUNT_REALM.key]
    }

    constructor(username: String?, password: String?, realm: String?) {
        this.username = username
        this.password = password
        this.realm = realm
    }

    val details: HashMap<String, String>
        get() {
            val details = HashMap<String, String>()
            details[ConfigKey.ACCOUNT_USERNAME.key] = username ?: ""
            details[ConfigKey.ACCOUNT_PASSWORD.key] = password ?: ""
            details[ConfigKey.ACCOUNT_REALM.key] = realm ?: ""
            return details
        }

    fun setDetail(key: ConfigKey, value: String?) {
        when (key) {
            ConfigKey.ACCOUNT_USERNAME -> username = value
            ConfigKey.ACCOUNT_PASSWORD -> password = value
            ConfigKey.ACCOUNT_REALM -> realm = value
            else -> {}
        }
    }

    companion object {
        private val TAG = AccountCredentials::class.java.simpleName
    }
}