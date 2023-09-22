/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.settings

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.HomeActivity
import cx.ring.client.LogsActivity
import cx.ring.databinding.FragSettingsBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.settings.pluginssettings.PluginDetails
import cx.ring.settings.pluginssettings.PluginPathPreferenceFragment
import cx.ring.settings.pluginssettings.PluginSettingsFragment
import cx.ring.settings.pluginssettings.PluginsListSettingsFragment
import cx.ring.utils.ActionHelper.openJamiDonateWebPage
import dagger.hilt.android.AndroidEntryPoint
import net.jami.daemon.JamiService
import net.jami.model.DonationSettings
import net.jami.model.Settings
import net.jami.mvp.GenericView
import net.jami.settings.SettingsPresenter
import net.jami.settings.SettingsViewModel
import net.jami.utils.DonationUtils

@AndroidEntryPoint
class SettingsFragment :
    BaseSupportFragment<SettingsPresenter, GenericView<SettingsViewModel>>(),
    GenericView<SettingsViewModel>,
    OnScrollChangedListener {
    private var binding: FragSettingsBinding? = null
    private var currentSettings: Settings? = null
    private var currentDonationSettings: DonationSettings? = null
    private var mIsRefreshingViewFromPresenter = true
    private var mNotificationVisibility = NOTIFICATION_PRIVATE
    private var fragment: Fragment? = null

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            popBackStack()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.let {
            it.addCallback(this, backPressedCallback)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragSettingsBinding.inflate(inflater, container, false).apply {

            if (System.currentTimeMillis() in startDonationTimeMillis until endDonationTimeMillis) {
                donateButton.visibility= View.VISIBLE
                settingsDonateLayout.visibility = View.VISIBLE
                donateButton.setOnClickListener {
                    openJamiDonateWebPage(requireContext())
                }
                settingsDonateSwitch.setOnCheckedChangeListener { _, _ ->
                    saveDonationSettings(binding!!)
                }
            }

            settingsPluginsLayout.setOnClickListener {
                if (JamiService.getPluginsEnabled()) {
                    goToPluginsListSettings()
                }
            }
            scrollview.viewTreeObserver.addOnScrollChangedListener(this@SettingsFragment)
            settingsDarkTheme.setOnCheckedChangeListener { _, isChecked: Boolean ->
                presenter.darkMode = isChecked
            }
            settingsPluginsSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
                JamiService.setPluginsEnabled(isChecked)
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
            settingsPluginsSwitch.isChecked = JamiService.getPluginsEnabled()
            if (TextUtils.isEmpty(JamiApplication.instance?.pushToken)) {
                settingsPushNotificationsLayout.visibility = View.GONE
            }
            binding = this
        }.root

    private fun goToVideoSettings() {
        val content = VideoSettingsFragment()
        fragment = content
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, VIDEO_SETTINGS_TAG)
            .addToBackStack(VIDEO_SETTINGS_TAG).commit()
        binding!!.fragmentContainer.isVisible = true
        backPressedCallback.isEnabled = true
    }

    private fun goToPluginsListSettings(accountId: String? = "") {
        val content = PluginsListSettingsFragment.newInstance(accountId)
        fragment = content
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, PLUGINS_LIST_SETTINGS_TAG)
            .addToBackStack(PLUGINS_LIST_SETTINGS_TAG).commit()
        binding!!.fragmentContainer.isVisible = true
        backPressedCallback.isEnabled = true
    }

    fun gotToPluginSettings(pluginDetails: PluginDetails) {
        val content = PluginSettingsFragment.newInstance(pluginDetails)
        val fragmentTransaction: FragmentTransaction = childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, PLUGIN_SETTINGS_TAG)
        if (fragment !is PluginSettingsFragment) {
            fragmentTransaction.addToBackStack(PLUGIN_SETTINGS_TAG)
        }
        fragmentTransaction.commit()
        fragment = content
        binding!!.fragmentContainer.isVisible = true
        backPressedCallback.isEnabled = true
    }

    fun gotToPluginPathPreference(pluginDetails: PluginDetails, preferenceKey: String) {
        val content = PluginPathPreferenceFragment.newInstance(pluginDetails, preferenceKey)
        fragment = content
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, PLUGIN_PATH_PREFERENCE_TAG
            )
            .addToBackStack(PLUGIN_PATH_PREFERENCE_TAG).commit()
        binding!!.fragmentContainer.isVisible = true
        backPressedCallback.isEnabled = true
    }

    fun popBackStack() {
        childFragmentManager.popBackStackImmediate()
        if (childFragmentManager.backStackEntryCount == 0) {
            backPressedCallback.isEnabled = false
            binding!!.fragmentContainer.isVisible = false
            fragment = null
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

    fun setToolbarTitle(@StringRes resId: Int) {
        binding!!.toolbar.title = getString(resId)
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

    override fun onScrollChanged() {
        binding?.let { binding ->
            val activity: Activity? = activity
            if (activity is HomeActivity)
                activity.setToolbarElevation(binding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP))
        }
    }

    companion object {
        private const val SCROLL_DIRECTION_UP = -1
        const val NOTIFICATION_PRIVATE = 0
        const val NOTIFICATION_PUBLIC = 1
        const val NOTIFICATION_SECRET = 2
        const val VIDEO_SETTINGS_TAG = "VideoPrefs"
        const val PLUGINS_LIST_SETTINGS_TAG = "PluginsListSettings"
        const val PLUGIN_SETTINGS_TAG = "PluginSettings"
        const val PLUGIN_PATH_PREFERENCE_TAG = "PluginPathPreference"
        private const val fragmentContainerId: Int = R.id.fragment_container
    }
}