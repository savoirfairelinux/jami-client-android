/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.settings

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.LogsActivity
import cx.ring.databinding.FragSettingsBinding
import cx.ring.interfaces.AppBarStateListener
import cx.ring.mvp.BaseSupportFragment
import cx.ring.settings.extensionssettings.ExtensionDetails
import cx.ring.settings.extensionssettings.ExtensionPathPreferenceFragment
import cx.ring.settings.extensionssettings.ExtensionSettingsFragment
import cx.ring.settings.extensionssettings.ExtensionsListSettingsFragment
import cx.ring.utils.ActionHelper.openJamiDonateWebPage
import dagger.hilt.android.AndroidEntryPoint
import net.jami.daemon.JamiService
import net.jami.model.DonationSettings
import net.jami.model.Settings
import net.jami.mvp.GenericView
import net.jami.settings.SettingsPresenter
import net.jami.settings.SettingsViewModel
import net.jami.utils.DonationUtils
import net.jami.utils.DonationUtils.endDonationTimeMillis
import net.jami.utils.DonationUtils.startDonationTimeMillis

@AndroidEntryPoint
class SettingsFragment :
    BaseSupportFragment<SettingsPresenter, GenericView<SettingsViewModel>>(),
    GenericView<SettingsViewModel>,
    AppBarStateListener {
    private var binding: FragSettingsBinding? = null
    private var currentSettings: Settings? = null
    private var currentDonationSettings: DonationSettings? = null
    private var mIsRefreshingViewFromPresenter = true
    private var mNotificationVisibility = NOTIFICATION_PRIVATE

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() { popBackStack() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragSettingsBinding.inflate(inflater, container, false).apply {

            if (DonationUtils.isDonationPeriod()) {
                donateButton.visibility= View.VISIBLE
                settingsDonateLayout.visibility = View.VISIBLE
                donateButton.setOnClickListener {
                    openJamiDonateWebPage(requireContext())
                }
                settingsDonateSwitch.setOnCheckedChangeListener { _, _ ->
                    saveDonationSettings(binding!!)
                }
            }

            settingsExtensionsLayout.setOnClickListener {
                if (JamiService.getExtensionsEnabled()) {
                    goToExtensionsListSettings()
                }
            }
            settingsDarkTheme.setOnCheckedChangeListener { _, isChecked: Boolean ->
                presenter.darkMode = isChecked
            }
            settingsExtensionsSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
                JamiService.setExtensionsEnabled(isChecked)
            }
            val save = CompoundButton.OnCheckedChangeListener { _, isChecked: Boolean ->
                if (!mIsRefreshingViewFromPresenter) saveSettings(this)
            }
            settingsPushNotifications.setOnCheckedChangeListener(save)
            settingsStartup.setOnCheckedChangeListener(save)
            settingsPersistNotification.setOnCheckedChangeListener(save)
            settingsTyping.setOnCheckedChangeListener(save)
            settingsBlockRecord.setOnCheckedChangeListener(save)
            settingsLinkPreview.setOnCheckedChangeListener(save)
            settingsVideoLayout.setOnClickListener {
                goToVideoSettings()
            }

            val singleItems = arrayOf(
                getString(R.string.notification_private),
                getString(R.string.notification_public),
                getString(R.string.notification_secret)
            )
            val checkedItem = intArrayOf(mNotificationVisibility)
            settingsNotification.setOnClickListener { v ->
                MaterialAlertDialogBuilder(v.context)
                    .setTitle(getString(R.string.pref_notification_title))
                    .setSingleChoiceItems(singleItems, mNotificationVisibility) { _, i: Int -> checkedItem[0] = i }
                    .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, id: Int ->
                        mNotificationVisibility = checkedItem[0]
                        saveSettings(this)
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, id: Int -> }
                    .show()
            }
            settingsLogs.setOnClickListener { v: View ->
                startActivity(Intent(v.context, LogsActivity::class.java))
            }
            toolbar.setNavigationOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            settingsDarkTheme.isChecked = presenter.darkMode
            settingsExtensionsSwitch.isChecked = JamiService.getExtensionsEnabled()
            if (TextUtils.isEmpty(JamiApplication.instance?.pushToken)) {
                settingsPushNotificationsLayout.visibility = View.GONE
            }
            binding = this
        }.root

    private fun goToVideoSettings() {
        val binding = binding ?: return
        val content = VideoSettingsFragment()
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, content, VIDEO_SETTINGS_TAG)
            .addToBackStack(VIDEO_SETTINGS_TAG).commit()
        binding.fragmentContainer.isVisible = true
        binding.donateButton.isVisible = false
        backPressedCallback.isEnabled = true
    }

    private fun goToExtensionsListSettings(accountId: String? = "") {
        val binding = binding ?: return
        val content = ExtensionsListSettingsFragment.newInstance(accountId)
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, content, EXTENSIONS_LIST_SETTINGS_TAG)
            .addToBackStack(EXTENSIONS_LIST_SETTINGS_TAG).commit()
        binding.fragmentContainer.isVisible = true
        binding.donateButton.isVisible = false
        backPressedCallback.isEnabled = true
    }

    fun goToExtensionSettings(extensionDetails: ExtensionDetails) {
        val content = ExtensionSettingsFragment.newInstance(extensionDetails)
        val fragmentTransaction = childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, content, EXTENSION_SETTINGS_TAG)
        val backStackEntryCount = childFragmentManager.backStackEntryCount
        if(backStackEntryCount > 0) {
            val topBackStackEntry = childFragmentManager.getBackStackEntryAt(backStackEntryCount-1)
            if (topBackStackEntry.name != EXTENSION_SETTINGS_TAG)
                fragmentTransaction.addToBackStack(EXTENSION_SETTINGS_TAG)
        }
        fragmentTransaction.commit()
        binding!!.fragmentContainer.isVisible = true
        backPressedCallback.isEnabled = true
    }

    fun goToExtensionPathPreference(extensionDetails: ExtensionDetails, preferenceKey: String) {
        val content = ExtensionPathPreferenceFragment.newInstance(extensionDetails, preferenceKey)
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, content, EXTENSION_PATH_PREFERENCE_TAG)
            .addToBackStack(EXTENSION_PATH_PREFERENCE_TAG).commit()
        binding!!.fragmentContainer.isVisible = true
        backPressedCallback.isEnabled = true
    }

    fun popBackStack() {
        childFragmentManager.popBackStackImmediate()
        if (childFragmentManager.backStackEntryCount == 0) {
            val binding = binding ?: return
            binding.donateButton.isVisible = DonationUtils.isDonationPeriod()
            onAppBarScrollTargetViewChanged(binding.scrollview)
            onToolbarTitleChanged(getString(R.string.menu_item_advanced_settings))
            backPressedCallback.isEnabled = false
            binding.fragmentContainer.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // loading preferences
        presenter.loadSettings()
    }

    private fun saveDonationSettings(binding: FragSettingsBinding) {
        presenter.saveDonationSettings(
            currentDonationSettings!!.copy(
                donationReminderVisibility = binding.settingsDonateSwitch.isChecked,
                lastDismissed =
                if (!binding.settingsDonateSwitch.isChecked)
                    currentDonationSettings!!.lastDismissed else 0
            )
        )
    }

    private fun saveSettings(binding: FragSettingsBinding) {
        // save settings according to UI inputs
        presenter.saveSettings(
            currentSettings!!.copy(
                runOnStartup = binding.settingsStartup.isChecked,
                enablePushNotifications = binding.settingsPushNotifications.isChecked,
                enablePermanentService = binding.settingsPersistNotification.isChecked,
                enableTypingIndicator = binding.settingsTyping.isChecked,
                isRecordingBlocked = binding.settingsBlockRecord.isChecked,
                enableLinkPreviews = binding.settingsLinkPreview.isChecked,
                notificationVisibility = mNotificationVisibility,
            )
        )
    }

    /**
     * Presents a Toast explaining why the Read Contacts permission is required to display the devi-
     * ces contacts in Ring.
     */
    private fun presentReadContactPermissionExplanationToast() {
        val activity: Activity? = activity
        if (null != activity) {
            val toastMessage = getString(R.string.permission_dialog_read_contacts_message)
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Presents a Toast explaining why the Write Call Log permission is required to enable the cor-
     * responding feature.
     */
    private fun presentWriteCallLogPermissionExplanationToast() {
        val activity: Activity? = activity
        if (null != activity) {
            val toastMessage = getString(R.string.permission_dialog_write_call_log_message)
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun showViewModel(viewModel: SettingsViewModel) {
        val settings = viewModel.settings
        val donationSettings = viewModel.donationSettings
        currentDonationSettings = donationSettings
        currentSettings = settings
        mIsRefreshingViewFromPresenter = true

        binding?.apply {
            settingsPushNotifications.isChecked = settings.enablePushNotifications
            settingsPersistNotification.isChecked = settings.enablePermanentService
            settingsStartup.isChecked = settings.runOnStartup
            settingsTyping.isChecked = settings.enableTypingIndicator
            settingsBlockRecord.isChecked = settings.isRecordingBlocked
            settingsLinkPreview.isChecked = settings.enableLinkPreviews
            settingsDonateSwitch.isChecked = donationSettings.donationReminderVisibility
        }
        mIsRefreshingViewFromPresenter = false
        mNotificationVisibility = settings.notificationVisibility
    }

    //================= AppBar management =====================
    override fun onAppBarScrollTargetViewChanged(v: View?) {
        binding?.appBar?.setLiftOnScrollTargetView(v)
    }

    override fun onToolbarTitleChanged(title: String) {
        binding?.toolbar?.title = title
    }
    //=============== AppBar management end ===================

    companion object {
        val TAG = SettingsFragment::class.simpleName!!
        const val NOTIFICATION_PRIVATE = 0
        const val NOTIFICATION_PUBLIC = 1
        const val NOTIFICATION_SECRET = 2
        const val VIDEO_SETTINGS_TAG = "VideoPrefs"
        const val EXTENSIONS_LIST_SETTINGS_TAG = "ExtensionsListSettings"
        const val EXTENSION_SETTINGS_TAG = "ExtensionSettings"
        const val EXTENSION_PATH_PREFERENCE_TAG = "ExtensionPathPreference"
    }
}