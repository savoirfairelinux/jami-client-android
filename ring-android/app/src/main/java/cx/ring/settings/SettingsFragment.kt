/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author:     Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.HomeActivity
import cx.ring.client.LogsActivity
import cx.ring.databinding.FragSettingsBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.daemon.JamiService
import net.jami.model.Settings
import net.jami.mvp.GenericView
import net.jami.settings.SettingsPresenter

@AndroidEntryPoint
class SettingsFragment : BaseSupportFragment<SettingsPresenter, GenericView<Settings>>(), GenericView<Settings>,
    OnScrollChangedListener {
    private var binding: FragSettingsBinding? = null
    private var currentSettings: Settings? = null
    private var mIsRefreshingViewFromPresenter = true
    private var mNotificationVisibility = NOTIFICATION_PRIVATE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragSettingsBinding.inflate(inflater, container, false).apply {
            settingsPluginsLayout.setOnClickListener {
                val activity = activity as HomeActivity?
                if (activity != null && JamiService.getPluginsEnabled()) {
                    activity.goToPluginsListSettings()
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
            settingsNewGroup.setOnCheckedChangeListener(save)
            settingsTyping.setOnCheckedChangeListener(save)
            settingsRead.setOnCheckedChangeListener(save)
            settingsBlockRecord.setOnCheckedChangeListener(save)
            settingsLinkPreview.setOnCheckedChangeListener(save)
            settingsVideoLayout.setOnClickListener {
                (activity as HomeActivity?)?.goToVideoSettings()
            }
            settingsClearHistory.setOnClickListener {
                MaterialAlertDialogBuilder(inflater.context)
                    .setTitle(getString(R.string.clear_history_dialog_title))
                    .setMessage(getString(R.string.clear_history_dialog_message))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        // ask the presenter to clear history
                        presenter.clearHistory()
                        Snackbar.make(root, getString(R.string.clear_history_completed), Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, id: Int -> }
                    .show()
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
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.settingsDarkTheme.isChecked = presenter.darkMode
        binding!!.settingsPluginsSwitch.isChecked = JamiService.getPluginsEnabled()
        if (TextUtils.isEmpty(JamiApplication.instance?.pushToken)) {
            binding!!.settingsPushNotificationsLayout.visibility = View.GONE
        }
        // loading preferences
        presenter.loadSettings()
        (activity as HomeActivity?)?.setToolbarTitle(R.string.menu_item_advanced_settings)
    }

    private fun saveSettings(binding: FragSettingsBinding) {
        // save settings according to UI inputs
        presenter.saveSettings(currentSettings!!.copy(
            runOnStartup = binding.settingsStartup.isChecked,
            enablePushNotifications = binding.settingsPushNotifications.isChecked,
            enablePermanentService = binding.settingsPersistNotification.isChecked,
            enableAddGroup = binding.settingsNewGroup.isChecked,
            enableTypingIndicator = binding.settingsTyping.isChecked,
            enableReadIndicator = binding.settingsRead.isChecked,
            isRecordingBlocked = binding.settingsBlockRecord.isChecked,
            enableLinkPreviews = binding.settingsLinkPreview.isChecked,
            notificationVisibility = mNotificationVisibility
        ))
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

    override fun showViewModel(viewModel: Settings) {
        currentSettings = viewModel
        mIsRefreshingViewFromPresenter = true
        binding?.apply {
            settingsPushNotifications.isChecked = viewModel.enablePushNotifications
            settingsPersistNotification.isChecked = viewModel.enablePermanentService
            settingsNewGroup.isChecked = viewModel.enableAddGroup
            settingsStartup.isChecked = viewModel.runOnStartup
            settingsTyping.isChecked = viewModel.enableTypingIndicator
            settingsRead.isChecked = viewModel.enableReadIndicator
            settingsBlockRecord.isChecked = viewModel.isRecordingBlocked
            settingsLinkPreview.isChecked = viewModel.enableLinkPreviews
        }
        mIsRefreshingViewFromPresenter = false
        mNotificationVisibility = viewModel.notificationVisibility
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
    }
}