package cx.ring.settings.pluginssettings;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.daemon.Ringservice;
import cx.ring.plugins.PluginPreferences;

import static cx.ring.plugins.PluginUtils.getOrElse;
import static cx.ring.plugins.PluginUtils.stringListToListString;

public class PluginSettingsFragment extends PreferenceFragmentCompat {
    public static final String TAG = PluginSettingsFragment.class.getSimpleName();
    private Context mContext;
    private List<Map<String, String>> mPreferencesAttributes;
    private PluginDetails pluginDetails;
    private PluginPreferencesDataStore ppds;

    public static PluginSettingsFragment newInstance(PluginDetails pluginDetails) {
        Bundle args = new Bundle();
        args.putString("name", pluginDetails.getName());
        args.putString("rootPath", pluginDetails.getRootPath());
        args.putBoolean("enabled", pluginDetails.isEnabled());
        PluginSettingsFragment psf = new PluginSettingsFragment();
        psf.setArguments(args);
        return psf;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = requireActivity();

        Bundle arguments = getArguments();

        if (arguments != null && arguments.getString("name") != null
                && arguments.getString("rootPath") != null) {
            pluginDetails = new PluginDetails(arguments.getString("name"),
                    arguments.getString("rootPath"), arguments.getBoolean("enabled"));

            mPreferencesAttributes = pluginDetails.getPluginPreferences();

            PreferenceManager preferenceManager = getPreferenceManager();
            ppds = new PluginPreferencesDataStore(pluginDetails);
            preferenceManager.setPreferenceDataStore(ppds);
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mContext);
        screen.addPreference(createHeadPreference());
        for (Preference preference : createPreferences(mPreferencesAttributes)) {
            screen.addPreference(preference);
        }
        setPreferenceScreen(screen);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.plugin_edition, menu);
        MenuItem item = menu.findItem(R.id.menuitem_delete);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Takes a list of preferences attributes map
     * Creates a preference View for each map of attributes in the preference
     *
     * @param preferencesAttributes A list of preferences attributes
     * @return list of preferences
     */
    private List<Preference> createPreferences(List<Map<String, String>> preferencesAttributes) {
        List<Preference> preferencesViews = new ArrayList<>();
        if (preferencesAttributes != null) {
            for (Map<String, String> preferenceAttributes : preferencesAttributes) {
                String type = preferenceAttributes.get("type");
                // Call for each type the appropriate function member
                if (type != null) {
                    switch (type) {
                        case "CheckBox":
                            preferencesViews.add(createCheckBoxPreference(preferenceAttributes));
                            break;
                        case "DropDown":
                            preferencesViews.add(createDropDownPreference(preferenceAttributes));
                            break;
                        case "EditText":
                            preferencesViews.add(createEditTextPreference(preferenceAttributes));
                            break;
                        case "List":
                            preferencesViews.add(createListPreference(preferenceAttributes));
                            break;
                        case "Path":
                            preferencesViews.add(createPathPreference(preferenceAttributes));
                            break;
                        case "MultiSelectList":
                            preferencesViews.
                                    add(createMultiSelectListPreference(preferenceAttributes));
                            break;
                        case "SeekBar":
                            preferencesViews.add(createSeekBarPreference(preferenceAttributes));
                            break;
                        case "Switch":
                            preferencesViews.add(createSwitchPreference(preferenceAttributes));
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return preferencesViews;
    }

    private Preference createHeadPreference()
    {
        PluginPreferences preference = new PluginPreferences(mContext, pluginDetails);
        preference.setResetClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                .setTitle(preference.getTitle())
                .setMessage(R.string.plugin_reset_preferences_ask)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    Ringservice.resetPluginPreferencesValues(pluginDetails.getRootPath());
                    ((HomeActivity) requireActivity()).popFragmentImmediate();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                    /* Terminate with no action */
                })
                .show());
        preference.setInstallClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                .setMessage(R.string.account_delete_dialog_message)
                .setTitle(R.string.plugin_uninstall_title)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    pluginDetails.setEnabled(false);
                    Ringservice.uninstallPlugin(pluginDetails.getRootPath());
                    ((HomeActivity) requireActivity()).popFragmentImmediate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
        return preference;
    }

    private CheckBoxPreference createCheckBoxPreference(Map<String, String> preferenceModel) {
        CheckBoxPreference preference = new CheckBoxPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setTwoStatePreferenceAttributes(preference, preferenceModel);
        ppds.addTomPreferenceTypes(preferenceModel);
        return preference;
    }

    private DropDownPreference createDropDownPreference(Map<String, String> preferenceModel) {
        DropDownPreference preference = new DropDownPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setListPreferenceAttributes(preference, preferenceModel);
        ppds.addTomPreferenceTypes(preferenceModel);
        return preference;
    }

    private EditTextPreference createEditTextPreference(Map<String, String> preferenceModel) {
        EditTextPreference preference = new EditTextPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setDialogPreferenceAttributes(preference, preferenceModel);
        setEditTextAttributes(preference, preferenceModel);
        ppds.addTomPreferenceTypes(preferenceModel);
        return preference;
    }

    private ListPreference createListPreference(Map<String, String> preferenceModel) {
        ListPreference preference = new ListPreference(mContext);
        setPreferenceAttributes(preference, preferenceModel);
        setListPreferenceAttributes(preference, preferenceModel);
        ppds.addTomPreferenceTypes(preferenceModel);
        return preference;
    }

    private Preference createPathPreference(Map<String, String> preferenceModel){
        Preference preference = new Preference(mContext);
        preference.setOnPreferenceClickListener(p -> {
            ((HomeActivity) mContext).gotToPluginPathPreference(pluginDetails, preferenceModel.get("key"));
            return false;
        });
        setPreferenceAttributes(preference, preferenceModel);
        ppds.addTomPreferenceTypes(preferenceModel);
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

        if (!dialogTitle.isEmpty()) {
            preference.setDialogTitle(dialogTitle);
        }

        if (!dialogMessage.isEmpty()) {
            preference.setDialogTitle(dialogMessage);
        }

        if (!positiveButtonText.isEmpty()) {
            preference.setPositiveButtonText(positiveButtonText);
        }

        if (!negativeButtonText.isEmpty()) {
            preference.setNegativeButtonText(negativeButtonText);
        }
    }

    /**
     * Sets attributes specific to EditTextPreference
     * Here we set the default value
     *
     * @param preference      EditTextPreference
     * @param preferenceModel the map of attributes
     */
    private void setEditTextAttributes(EditTextPreference preference,
                                       Map<String, String> preferenceModel) {
        preference.setDefaultValue(preferenceModel.get("defaultValue"));
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
        String entries = getOrElse(preferenceModel.get("entries"), "");
        preference.setEntries(stringListToListString(entries).
                toArray(new CharSequence[ 0 ]));
        String entryValues = getOrElse(preferenceModel.get("entryValues"), "");
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
        String defaultValues = getOrElse(preferenceModel.get("defaultValue"), "[]");
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
        preference.setAdjustable(Boolean.parseBoolean(getOrElse(preferenceModel.get("adjustable"), "true")));
        preference.setDefaultValue(defaultValue);
        preference.setShowSeekBarValue(true);
        preference.setUpdatesContinuously(true);
    }

    /**
     * Sets specific attributes for twoStatePreference like Switch and CheckBox
     *
     * @param preference      the two state preference
     * @param preferenceModel the map of attributes
     */
    private void setTwoStatePreferenceAttributes(TwoStatePreference preference,
                                                 Map<String, String> preferenceModel) {
        preference.setChecked(ppds.getString("always", "1").equals("1"));
    }
}
