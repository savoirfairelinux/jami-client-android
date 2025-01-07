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

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.extensions.ExtensionPreferences
import cx.ring.extensions.ExtensionUtils.stringListToListString
import cx.ring.settings.SettingsFragment
import net.jami.daemon.JamiService

class ExtensionSettingsFragment : PreferenceFragmentCompat() {
    private var mPreferencesAttributes: List<Map<String, String>>? = null
    private var extensionDetails: ExtensionDetails? = null
    private var ppds: ExtensionPreferencesDataStore? = null
    private var accountId: String? = ""

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = requireArguments()
        val details = ExtensionDetails(arguments.getString("name")!!, arguments.getString("rootPath")!!, arguments.getBoolean("enabled"), null, accountId)
        mPreferencesAttributes = details.extensionPreferences
        val preferenceManager = preferenceManager
        ppds = ExtensionPreferencesDataStore(details)
        extensionDetails = details
        preferenceManager.preferenceDataStore = ppds
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        screen.addPreference(createHeadPreference())
        for (preference in createPreferences(mPreferencesAttributes)) {
            screen.addPreference(preference)
        }
        preferenceScreen = screen
        return root
    }

    /**
     * Takes a list of preferences attributes map
     * Creates a preference View for each map of attributes in the preference
     *
     * @param preferencesAttributes A list of preferences attributes
     * @return list of preferences
     */
    private fun createPreferences(preferencesAttributes: List<Map<String, String>>?): List<Preference> {
        val preferencesViews: MutableList<Preference> = ArrayList()
        if (preferencesAttributes != null) {
            for (preferenceAttributes in preferencesAttributes) {
                val type = preferenceAttributes["type"]
                // Call for each type the appropriate function member
                if (type != null) {
                    when (type) {
                        "CheckBox" -> preferencesViews.add(createCheckBoxPreference(preferenceAttributes))
                        "DropDown" -> preferencesViews.add(createDropDownPreference(preferenceAttributes))
                        "EditText" -> preferencesViews.add(createEditTextPreference(preferenceAttributes))
                        "List" -> preferencesViews.add(createListPreference(preferenceAttributes))
                        "Path" -> preferencesViews.add(createPathPreference(preferenceAttributes))
                        "MultiSelectList" -> preferencesViews.add(createMultiSelectListPreference(preferenceAttributes))
                        "SeekBar" -> preferencesViews.add(createSeekBarPreference(preferenceAttributes))
                        "Switch" -> preferencesViews.add(createSwitchPreference(preferenceAttributes))
                        else -> {
                        }
                    }
                }
            }
        }
        return preferencesViews
    }

    private fun createHeadPreference(): Preference {
        val preference = ExtensionPreferences(requireContext(), extensionDetails, accountId)
        val message = run {
            var value = R.string.extension_reset_preferences_ask
            if (accountId!!.isNotEmpty()) {
                value = R.string.extension_reset_account_preferences_ask
            }
            value
        }
        preference.setResetClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(preference.title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, id: Int ->
                    JamiService.resetPluginPreferencesValues(extensionDetails!!.rootPath, extensionDetails!!.accountId)
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, whichButton: Int -> }
                .show()
        }
        preference.setInstallClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.account_delete_dialog_message)
                .setTitle(R.string.extension_uninstall_title)
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, whichButton: Int ->
                    extensionDetails!!.isEnabled = false
                    JamiService.uninstallPlugin(extensionDetails!!.rootPath)
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        preference.setExtensionSettingsRedirect {
            if (accountId!!.isEmpty()) {
                val act = requireActivity() as HomeActivity
                val acc = act.mAccountService.currentAccount!!.accountId
                (parentFragment as SettingsFragment).goToExtensionSettings(ExtensionDetails(extensionDetails!!.name, extensionDetails!!.rootPath, extensionDetails!!.isEnabled, null, acc))
            } else {
                (parentFragment as SettingsFragment).goToExtensionSettings(ExtensionDetails(extensionDetails!!.name, extensionDetails!!.rootPath, extensionDetails!!.isEnabled))
            }
        }
        return preference
    }

    private fun createCheckBoxPreference(preferenceModel: Map<String, String>): CheckBoxPreference {
        val preference = CheckBoxPreference(requireContext())
        setPreferenceAttributes(preference, preferenceModel)
        setTwoStatePreferenceAttributes(preference, preferenceModel)
        ppds!!.addToPreferenceTypes(preferenceModel)
        return preference
    }

    private fun createDropDownPreference(preferenceModel: Map<String, String>): DropDownPreference {
        val preference = DropDownPreference(requireContext())
        setPreferenceAttributes(preference, preferenceModel)
        setListPreferenceAttributes(preference, preferenceModel)
        ppds!!.addToPreferenceTypes(preferenceModel)
        return preference
    }

    private fun createEditTextPreference(preferenceModel: Map<String, String>): EditTextPreference {
        val preference = EditTextPreference(requireContext())
        setPreferenceAttributes(preference, preferenceModel)
        setDialogPreferenceAttributes(preference, preferenceModel)
        setEditTextAttributes(preference, preferenceModel)
        ppds!!.addToPreferenceTypes(preferenceModel)
        return preference
    }

    private fun createListPreference(preferenceModel: Map<String, String>): ListPreference {
        val preference = ListPreference(requireContext())
        setPreferenceAttributes(preference, preferenceModel)
        setListPreferenceAttributes(preference, preferenceModel)
        ppds!!.addToPreferenceTypes(preferenceModel)
        return preference
    }

    private fun createPathPreference(preferenceModel: Map<String, String>): Preference {
        val preference = Preference(requireContext())
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            (parentFragment as SettingsFragment).goToExtensionPathPreference(extensionDetails!!, preferenceModel["key"]!!)
            false
        }
        setPreferenceAttributes(preference, preferenceModel)
        ppds!!.addToPreferenceTypes(preferenceModel)
        return preference
    }

    private fun createMultiSelectListPreference(preferenceModel: Map<String, String>): MultiSelectListPreference {
        val preference = MultiSelectListPreference(requireContext())
        setPreferenceAttributes(preference, preferenceModel)
        setMultiSelectListPreferenceAttributes(preference, preferenceModel)
        return preference
    }

    private fun createSwitchPreference(preferenceModel: Map<String, String>): SwitchPreference {
        val preference = SwitchPreference(requireContext())
        setPreferenceAttributes(preference, preferenceModel)
        setTwoStatePreferenceAttributes(preference, preferenceModel)
        return preference
    }

    private fun createSeekBarPreference(preferenceModel: Map<String, String>): SeekBarPreference {
        val preference = SeekBarPreference(requireContext())
        setPreferenceAttributes(preference, preferenceModel)
        setSeekBarPreferenceAttributes(preference, preferenceModel)
        return preference
    }

    /**
     * Sets attributes that are common in the Preference base class
     *
     * @param preference      the preference
     * @param preferenceModel the map of attributes
     */
    private fun setPreferenceAttributes(preference: Preference, preferenceModel: Map<String, String>) {
        val key = preferenceModel["key"]
        preference.key = key
        preference.title = preferenceModel["title"] ?: ""
        preference.summary = preferenceModel["summary"] ?: ""
    }

    private fun setDialogPreferenceAttributes(preference: DialogPreference, preferenceModel: Map<String, String>) {
        val dialogTitle = preferenceModel["dialogTitle"] ?: ""
        val dialogMessage = preferenceModel["dialogMessage"] ?: ""
        val positiveButtonText = preferenceModel["positiveButtonText"] ?: ""
        val negativeButtonText = preferenceModel["negativeButtonText"] ?: ""
        if (dialogTitle.isNotEmpty()) {
            preference.dialogTitle = dialogTitle
        }
        if (dialogMessage.isNotEmpty()) {
            preference.dialogTitle = dialogMessage
        }
        if (positiveButtonText.isNotEmpty()) {
            preference.positiveButtonText = positiveButtonText
        }
        if (negativeButtonText.isNotEmpty()) {
            preference.negativeButtonText = negativeButtonText
        }
    }

    /**
     * Sets attributes specific to EditTextPreference
     * Here we set the default value
     *
     * @param preference      EditTextPreference
     * @param preferenceModel the map of attributes
     */
    private fun setEditTextAttributes(
        preference: EditTextPreference,
        preferenceModel: Map<String, String>
    ) {
        preference.setDefaultValue(preferenceModel["defaultValue"])
    }

    /**
     * Sets specific attributes for Preference that have for a base class ListPreference
     * Sets the entries, entryValues and defaultValue
     *
     * @param preference      the list preference
     * @param preferenceModel the map of attributes
     */
    private fun setListPreferenceAttributes(preference: ListPreference, preferenceModel: Map<String, String>) {
        setDialogPreferenceAttributes(preference, preferenceModel)
        val entries = preferenceModel["entries"] ?: ""
        preference.entries = entries.split(',').toTypedArray<CharSequence>()
        val entryValues = preferenceModel["entryValues"] ?: ""
        preference.entryValues = stringListToListString(entryValues).toTypedArray<CharSequence>()
        preference.setDefaultValue(preferenceModel["defaultValue"])
    }

    /**
     * Sets specific attributes for Preference that have for a base class MultiSelectListPreference
     * Sets the entries, entryValues and defaultValues
     *
     * @param preference      the multi select list preference
     * @param preferenceModel the map of attributes
     */
    private fun setMultiSelectListPreferenceAttributes(
        preference: MultiSelectListPreference,
        preferenceModel: Map<String, String>
    ) {
        setDialogPreferenceAttributes(preference, preferenceModel)
        val entries = preferenceModel["entries"] ?: "[]"
        preference.entries = stringListToListString(entries).toTypedArray<CharSequence>()
        val entryValues = preferenceModel["entryValues"] ?: "[]"
        preference.entryValues = stringListToListString(entryValues).toTypedArray<CharSequence>()
        val defaultValues = preferenceModel["defaultValue"] ?: "[]"
        preference.entryValues = stringListToListString(entryValues).toTypedArray<CharSequence>()
        val set: Set<CharSequence> = HashSet<CharSequence>(stringListToListString(defaultValues))
        preference.setDefaultValue(set)
    }

    /**
     * Sets specific attributes for setSeekBarPreference
     *
     * @param preference      the seek bar preference
     * @param preferenceModel the map of attributes
     */
    private fun setSeekBarPreferenceAttributes(
        preference: SeekBarPreference,
        preferenceModel: Map<String, String>
    ) {
        var min = 0
        var max = 1
        var increment = 1
        var defaultValue = 0
        try {
            min = (preferenceModel["min"] ?: "0").toInt()
            max = (preferenceModel["max"]  ?: "1").toInt()
            increment = (preferenceModel["increment"] ?: "1").toInt()
            defaultValue = (preferenceModel["defaultValue"] ?: "[0").toInt()
        } catch (e: NumberFormatException) {
            Log.e(TAG, e.toString())
        }
        preference.min = min
        preference.max = max
        preference.seekBarIncrement = increment
        preference.isAdjustable = (preferenceModel["adjustable"] ?: "true").toBoolean()
        preference.setDefaultValue(defaultValue)
        preference.showSeekBarValue = true
        preference.updatesContinuously = true
    }

    /**
     * Sets specific attributes for twoStatePreference like Switch and CheckBox
     *
     * @param preference      the two state preference
     * @param preferenceModel the map of attributes
     */
    private fun setTwoStatePreferenceAttributes(preference: TwoStatePreference, preferenceModel: Map<String, String>) {
        preference.isChecked = ppds!!.getString("always", "1") == "1"
    }

    companion object {
        val TAG = ExtensionSettingsFragment::class.simpleName!!
        fun newInstance(extensionDetails: ExtensionDetails): ExtensionSettingsFragment {
            val psf = ExtensionSettingsFragment()
            psf.arguments = Bundle().apply {
                putString("name", extensionDetails.name)
                putString("rootPath", extensionDetails.rootPath)
                putBoolean("enabled", extensionDetails.isEnabled)
                psf.accountId = extensionDetails.accountId
            }
            return psf
        }
    }
}