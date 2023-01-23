package cx.ring.settings.pluginssettings

import androidx.preference.PreferenceDataStore
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class PluginPreferencesDataStore(private val mPluginDetails: PluginDetails) : PreferenceDataStore() {
    private val mPreferenceTypes: MutableMap<String, String> = HashMap()

    private var preferencesValues: Map<String, String> = mPluginDetails.pluginPreferencesValues
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
        if (mPluginDetails.setPluginPreference(key, if (value) "1" else "0")) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putString(key: String, value: String?) {
        if (mPluginDetails.setPluginPreference(key, value ?: "")) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        if (values != null) {
            if (mPluginDetails.setPluginPreference(key, values.joinToString(","))) {
                notifyPreferencesValuesChange()
            }
        }
    }

    override fun putInt(key: String, value: Int) {
        if (mPluginDetails.setPluginPreference(key, value.toString())) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putLong(key: String, value: Long) {
        if (mPluginDetails.setPluginPreference(key, value.toString())) {
            notifyPreferencesValuesChange()
        }
    }

    override fun putFloat(key: String, value: Float) {
        if (mPluginDetails.setPluginPreference(key, value.toString())) {
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
            mPluginDetails.pluginPreferencesValues
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