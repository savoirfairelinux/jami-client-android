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
package cx.ring.settings.extensionssettings

import androidx.preference.PreferenceDataStore
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class ExtensionPreferencesDataStore(private val mExtensionDetails: ExtensionDetails) : PreferenceDataStore() {
    private val mPreferenceTypes: MutableMap<String, String> = HashMap()

    private var preferencesValues: Map<String, String> = mExtensionDetails.extensionPreferencesValues
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    fun addToPreferenceTypes(preferenceModel: Map<String, String>) {
        val writeLock = lock.writeLock()
        try {
            writeLock.lock()
            mPreferenceTypes[preferenceModel["key"]!!] = preferenceModel["type"]!!
        } finally {
            writeLock.unlock()
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        if (mExtensionDetails.setExtensionPreference(key, if (value) "1" else "0")) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putString(key: String, value: String?) {
        if (mExtensionDetails.setExtensionPreference(key, value ?: "")) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        if (values != null) {
            if (mExtensionDetails.setExtensionPreference(key, values.joinToString(","))) {
                notifyPreferencesValuesChange()
            }
        }
    }

    override fun putInt(key: String, value: Int) {
        if (mExtensionDetails.setExtensionPreference(key, value.toString())) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putLong(key: String, value: Long) {
        if (mExtensionDetails.setExtensionPreference(key, value.toString())) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putFloat(key: String, value: Float) {
        if (mExtensionDetails.setExtensionPreference(key, value.toString())) {
            notifyPreferencesValuesChange()
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        return getPreferencesValues()[key] ?: defValue
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return getPreferencesValues()[key]?.split(',')?.toSet() ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        return getPreferencesValues()[key]?.toInt() ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return getPreferencesValues()[key]?.toLong() ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return getPreferencesValues()[key]?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        var returnValue = defValue
        val value = getPreferencesValues()[key]
        if (value != null) {
            returnValue = value == "1"
        }
        return returnValue
    }

    /**
     * Updates the preferencesValues map
     * Use locks since the PreferenceInteraction is asynchronous
     */
    private fun notifyPreferencesValuesChange() {
        val writeLock = lock.writeLock()
        preferencesValues = try {
            writeLock.lock()
            mExtensionDetails.extensionPreferencesValues
        } finally {
            writeLock.unlock()
        }
    }

    /**
     * Returns the preferencesValues
     * Use locks since the PreferenceInteraction is asynchronous
     * @return preferencesValues
     */
    private fun getPreferencesValues(): Map<String, String> {
        val readLock = lock.readLock()
        return try {
            readLock.lock()
            preferencesValues
        } finally {
            readLock.unlock()
        }
    }
}