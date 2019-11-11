package cx.ring.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DialogPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cx.ring.plugins.PluginUtils;

public class PluginSettingsFragment extends PreferenceFragmentCompat {
    public static final String TAG = PluginsListSettingsFragment.class.getSimpleName();
    private Context mContext;
    private List<Map<String, String>> preferences;

    public static PluginSettingsFragment newInstance(PluginDetails pluginDetails) {
        Bundle args = new Bundle();
        args.putString("name", pluginDetails.getName());
        args.putString("rootPath", pluginDetails.getRootPath());
        args.putBoolean("enabled", pluginDetails.isEnabled());
        PluginSettingsFragment psf = new PluginSettingsFragment();
        psf.setArguments(args);
        return psf;
    }

    /**
     * Useful Util method that is available android24
     * We emulate it here for strings
     *
     * @param input        input object that can be null
     * @param defaultValue default NonNull object of the same type as input
     * @return input if not null, defaultValue otherwise
     */
    private static <T> T getOrElse(T input, @NonNull T defaultValue) {
        if (input == null) {
            return defaultValue;
        } else {
            return input;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = requireActivity();

        Bundle arguments = getArguments();

        if (arguments != null) {
            PluginDetails pluginDetails = new PluginDetails(arguments.getString("name"),
                    arguments.getString("rootPath"), arguments.getBoolean("enabled"));

            String pluginPreferencesPath = pluginDetails.getPreferencesPath();
            preferences = PluginUtils.
                    getPluginPreferences(pluginPreferencesPath);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mContext);
        for (Map<String, String> preference : preferences) {
            String type = preference.get("type");
            // Call for each type the appropriate function member
            if (type != null) {
                switch (type) {
                    case "CheckBox":
                        screen.addPreference(createCheckBoxPreference(preference));
                        break;
                    case "DropDown":
                        screen.addPreference(createDropDownPreference(preference));
                        break;
                    case "EditText":
                        screen.addPreference(createEditTextPreference(preference));
                        break;
                    case "List":
                        screen.addPreference(createListPreference(preference));
                        break;
                    case "MultiSelectList":
                        screen.addPreference(createMultiSelectListPreference(preference));
                        break;
                    case "SeekBar":
                        screen.addPreference(createSeekBarPreference(preference));
                        break;
                    case "Switch":
                        screen.addPreference(createSwitchPreference(preference));
                        break;
                    default:
                        break;
                }
            }
        }
        setPreferenceScreen(screen);
        return root;
    }

    private CheckBoxPreference createCheckBoxPreference(Map<String, String> preferenceModel) {
        CheckBoxPreference preference = new CheckBoxPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setTwoStatePreferenceAttributes(preference, preferenceModel);
        return preference;
    }

    private DropDownPreference createDropDownPreference(Map<String, String> preferenceModel) {
        DropDownPreference preference = new DropDownPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setListPreferenceAttributes(preference, preferenceModel);
        return preference;
    }

    private EditTextPreference createEditTextPreference(Map<String, String> preferenceModel) {
        EditTextPreference preference = new EditTextPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setDialogPreferenceAttributes(preference, preferenceModel);
        preference.setDefaultValue(preferenceModel.get("defaultValue"));
        return preference;
    }

    private ListPreference createListPreference(Map<String, String> preferenceModel) {
        ListPreference preference = new ListPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setListPreferenceAttributes(preference, preferenceModel);
        return preference;
    }

    private MultiSelectListPreference createMultiSelectListPreference(
            Map<String, String> preferenceModel) {
        MultiSelectListPreference preference = new MultiSelectListPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setMultiSelectListPreferenceAttributes(preference, preferenceModel);
        return preference;
    }

    private SwitchPreference createSwitchPreference(Map<String, String> preferenceModel) {
        SwitchPreference preference = new SwitchPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setTwoStatePreferenceAttributes(preference, preferenceModel);
        return preference;
    }

    private SeekBarPreference createSeekBarPreference(Map<String, String> preferenceModel) {
        SeekBarPreference preference = new SeekBarPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setSeekBarPreferenceAttributes(preference, preferenceModel);
        return preference;
    }

    /**
     * Converts a string that contains a list to a java List<String>
     * E.g: String entries = "["AAA","BBB","CCC"]" to List<String> l, where l.get(0) = "AAA"
     *
     * @param stringList a string in the form "["AAA","BBB","CCC"]"
     */
    private List<String> stringListToListString(String stringList) {
        List<String> listString = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        for (int i = 1; i < stringList.length() - 1; i++) {
            char currentChar = stringList.charAt(i);
            if (currentChar != ',') {
                currentWord.append(currentChar);
            } else {
                listString.add(currentWord.toString());
                currentWord = new StringBuilder();
            }

            if (i == stringList.length() - 2) {
                listString.add(currentWord.toString());
                break;
            }
        }
        return listString;
    }

    /**
     * Sets attributes that are common in the Preference base class
     *
     * @param preference      the preference
     * @param preferenceModel the map of attributes
     */
    private void setPreferenceAttributes(Preference preference,
                                         Map<String, String> preferenceModel) {
        String key = preferenceModel.get("key");
        preference.setKey(key);
        preference.setTitle(getOrElse(preferenceModel.get("title"), ""));
        preference.setSummary(getOrElse(preferenceModel.get("summary"), ""));
    }

    //TODO : add drawable icon
    private void setDialogPreferenceAttributes(DialogPreference preference,
                                               Map<String, String> preferenceModel) {
        String dialogTitle = getOrElse(preferenceModel.get("dialogTitle"), "");
        String dialogMessage = getOrElse(preferenceModel.get("dialogMessage"), "");
        String positiveButtonText = getOrElse(preferenceModel.get("positiveButtonText"), "");
        String negativeButtonText = getOrElse(preferenceModel.get("negativeButtonText"), "");

        if(!dialogTitle.isEmpty()) {
            preference.setDialogTitle(dialogTitle);
        }

        if(!dialogMessage.isEmpty()) {
            preference.setDialogTitle(dialogMessage);
        }

        if(!positiveButtonText.isEmpty()) {
            preference.setPositiveButtonText(positiveButtonText);
        }

        if(!negativeButtonText.isEmpty()) {
            preference.setNegativeButtonText(negativeButtonText);
        }
    }

    /**
     * Sets specific attributes for Preference that have for a base class ListPreference
     * Sets the entries, entryValues and defaultValue
     *
     * @param preference      the list preference
     * @param preferenceModel the map of attributes
     */
    private void setListPreferenceAttributes(ListPreference preference,
                                             Map<String, String> preferenceModel) {
        setDialogPreferenceAttributes(preference, preferenceModel);
        String entries = getOrElse(preferenceModel.get("entries"), "[]");
        preference.setEntries(stringListToListString(entries).
                toArray(new CharSequence[ 0 ]));
        String entryValues = getOrElse(preferenceModel.get("entryValues"), "[]");
        preference.setEntryValues(stringListToListString(entryValues).
                toArray(new CharSequence[ 0 ]));
        preference.setDefaultValue(preferenceModel.get("defaultValue"));
    }

    /**
     * Sets specific attributes for Preference that have for a base class MultiSelectListPreference
     * Sets the entries, entryValues and defaultValues
     *
     * @param preference      the multi select list preference
     * @param preferenceModel the map of attributes
     */
    private void setMultiSelectListPreferenceAttributes(MultiSelectListPreference preference,
                                                        Map<String, String> preferenceModel) {
        setDialogPreferenceAttributes(preference, preferenceModel);
        String entries = getOrElse(preferenceModel.get("entries"), "[]");
        preference.setEntries(stringListToListString(entries).
                toArray(new CharSequence[ 0 ]));
        String entryValues = getOrElse(preferenceModel.get("entryValues"), "[]");
        preference.setEntryValues(stringListToListString(entryValues).
                toArray(new CharSequence[ 0 ]));
        String defaultValues = getOrElse(preferenceModel.get("defaultValues"), "[]");
        preference.setEntryValues(stringListToListString(entryValues).
                toArray(new CharSequence[ 0 ]));
        Set<CharSequence> set = new HashSet<>(stringListToListString(defaultValues));
        preference.setDefaultValue(set);
    }

    /**
     * Sets specific attributes for setSeekBarPreference
     *
     * @param preference      the seek bar preference
     * @param preferenceModel the map of attributes
     */
    private void setSeekBarPreferenceAttributes(SeekBarPreference preference,
                                                Map<String, String> preferenceModel) {
        int min = 0, max = 1, increment = 1;
        int defaultValue = 0;
        try {
            min = Integer.parseInt(getOrElse(preferenceModel.get("min"), "0"));
            max = Integer.parseInt(getOrElse(preferenceModel.get("max"), "1"));
            increment = Integer.parseInt(getOrElse(preferenceModel.get("increment"), "1"));
            defaultValue = Integer.parseInt(getOrElse(preferenceModel.get("defaultValue"), "[0"));
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString());
        }
        preference.setMin(min);
        preference.setMax(max);
        preference.setSeekBarIncrement(increment);
        preference.setAdjustable(Boolean.valueOf(getOrElse(preferenceModel.get("adjustable"),
                "true")));
        preference.setDefaultValue(defaultValue);
    }

    /**
     * Sets specific attributes for twoStatePreference like Switch and CheckBox
     *
     * @param preference      the two state preference
     * @param preferenceModel the map of attributes
     */
    private void setTwoStatePreferenceAttributes(TwoStatePreference preference,
                                                 Map<String, String> preferenceModel) {
        preference.setDefaultValue(Boolean.valueOf(preferenceModel.get("defaultValue")));
    }
}
