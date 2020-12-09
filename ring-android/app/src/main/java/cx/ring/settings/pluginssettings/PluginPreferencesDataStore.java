package cx.ring.settings.pluginssettings;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cx.ring.plugins.PluginUtils.listStringToStringList;
import static cx.ring.plugins.PluginUtils.stringListToListString;

public class PluginPreferencesDataStore extends PreferenceDataStore {

    private PluginDetails mPluginDetails;
    private Map<String, String> mPreferenceTypes = new HashMap<>();
    private Map<String, String> preferencesValues;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public PluginPreferencesDataStore(PluginDetails pluginDetails) {
        mPluginDetails = pluginDetails;
        preferencesValues = mPluginDetails.getPluginPreferencesValues();
    }

    public void addTomPreferenceTypes(Map<String, String> preferenceModel) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            mPreferenceTypes.put(preferenceModel.get("key"), preferenceModel.get("type"));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        Boolean boxedValue = value;
        boolean success = mPluginDetails.setPluginPreference(key, boxedValue.toString());
        if(success) {
            notifyPreferencesValuesChange();
        }
    }


    @Override
    public void putString(String key, @Nullable String value) {
        boolean success = mPluginDetails.setPluginPreference(key, value);
        if(success) {
            notifyPreferencesValuesChange();
        }
    }

    @Override
    public void putStringSet(String key, @Nullable Set<String> values) {
        if(values != null) {
            boolean success = mPluginDetails.setPluginPreference(key,
                    listStringToStringList(new ArrayList<>(values)));
            if(success) {
                notifyPreferencesValuesChange();
            }
        }
    }

    @Override
    public void putInt(String key, int value) {
        Integer boxedValue = value;
        boolean success = mPluginDetails.setPluginPreference(key, boxedValue.toString());
        if(success) {
            notifyPreferencesValuesChange();
        }
    }

    @Override
    public void putLong(String key, long value) {
        Long boxedValue = value;
        boolean success = mPluginDetails.setPluginPreference(key, boxedValue.toString());
        if(success) {
            notifyPreferencesValuesChange();
        }
    }

    @Override
    public void putFloat(String key, float value) {
        Float boxedValue = value;
        boolean success = mPluginDetails.setPluginPreference(key, boxedValue.toString());
        if(success) {
            notifyPreferencesValuesChange();
        }
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        String returnValue = defValue;
        String value = getPreferencesValues().get(key);
        if (value != null) {
            returnValue = value;
        }
        return returnValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        Set<String> returnValue =  defValues;

        String value = getPreferencesValues().get(key);

        if(value != null) {
            returnValue = new HashSet<>(stringListToListString(value));
        }

        return returnValue;
    }

    @Override
    public int getInt(String key, int defValue) {
        int returnValue = defValue;
        String value = getPreferencesValues().get(key);
        if (value != null) {
            returnValue = Integer.parseInt(value);
        }
        return returnValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        long returnValue = defValue;
        String value = getPreferencesValues().get(key);
        if (value != null) {
            returnValue = Long.parseLong(value);
        }
        return returnValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        float returnValue = defValue;
        String value = getPreferencesValues().get(key);
        if (value != null) {
            returnValue = Float.parseFloat(value);
        }
        return returnValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        boolean returnValue = defValue;
        String value = getPreferencesValues().get(key);
        if (value != null) {
            returnValue = value.equals("1");
        }
        return returnValue;
    }

    /**
     *  Updates the preferencesValues map
     *  Use locks since the PreferenceInteraction is asynchronous
     */
    public void notifyPreferencesValuesChange() {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            preferencesValues = mPluginDetails.getPluginPreferencesValues();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the preferencesValues
     * Use locks since the PreferenceInteraction is asynchronous
     * @return preferencesValues
     */
    private Map<String, String>  getPreferencesValues() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            return preferencesValues;
        } finally {
            readLock.unlock();
        }
    }
}
