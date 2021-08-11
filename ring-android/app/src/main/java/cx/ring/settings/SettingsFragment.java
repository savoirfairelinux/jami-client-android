/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.Toast;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.client.LogsActivity;
import cx.ring.databinding.FragSettingsBinding;

import net.jami.daemon.JamiService;
import net.jami.model.Settings;
import cx.ring.mvp.BaseSupportFragment;
import dagger.hilt.android.AndroidEntryPoint;

import net.jami.mvp.GenericView;
import net.jami.settings.SettingsPresenter;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
@AndroidEntryPoint
public class SettingsFragment extends BaseSupportFragment<SettingsPresenter, GenericView<Settings>> implements GenericView<Settings>, ViewTreeObserver.OnScrollChangedListener {

    private static final int SCROLL_DIRECTION_UP = -1;

    public static final int NOTIFICATION_PRIVATE = 0;
    public static final int NOTIFICATION_PUBLIC = 1;
    public static final int NOTIFICATION_SECRET = 2;

    private FragSettingsBinding binding;
    private Settings currentSettings = null;

    private boolean mIsRefreshingViewFromPresenter = true;
    private int mNotificationVisibility = NOTIFICATION_PRIVATE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);
        binding.settingsDarkTheme.setChecked(presenter.getDarkMode());
        binding.settingsPluginsSwitch.setChecked(JamiService.getPluginsEnabled());
        if (TextUtils.isEmpty(JamiApplication.getInstance().getPushToken())) {
            binding.settingsPushNotificationsLayout.setVisibility(View.GONE);
        }
        // loading preferences
        presenter.loadSettings();
        ((HomeActivity) getActivity()).setToolbarTitle(R.string.menu_item_settings);

        binding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        binding.settingsDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> presenter.setDarkMode(isChecked));
        binding.settingsPluginsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> JamiService.setPluginsEnabled(isChecked));

        CompoundButton.OnCheckedChangeListener save = (buttonView, isChecked) -> {
            if (!mIsRefreshingViewFromPresenter)
                saveSettings();
        };
        binding.settingsPushNotifications.setOnCheckedChangeListener(save);
        binding.settingsStartup.setOnCheckedChangeListener(save);
        binding.settingsPersistNotification.setOnCheckedChangeListener(save);
        binding.settingsTyping.setOnCheckedChangeListener(save);
        binding.settingsRead.setOnCheckedChangeListener(save);
        binding.settingsBlockRecord.setOnCheckedChangeListener(save);

        binding.settingsVideoLayout.setOnClickListener(v -> {
            HomeActivity activity = (HomeActivity) getActivity();
            if (activity != null)
                activity.goToVideoSettings();
        });

        binding.settingsClearHistory.setOnClickListener(v -> new MaterialAlertDialogBuilder(view.getContext())
                .setTitle(getString(R.string.clear_history_dialog_title))
                .setMessage(getString(R.string.clear_history_dialog_message))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    // ask the presenter to clear history
                    presenter.clearHistory();
                    Snackbar.make(view,
                            getString(R.string.clear_history_completed),
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    //~ Empty
                })
                .show());
        binding.settingsPluginsLayout.setOnClickListener(v -> {
            HomeActivity activity = (HomeActivity) getActivity();
            if (activity != null && JamiService.getPluginsEnabled()){
                activity.goToPluginsListSettings();
            }
        });

        String[] singleItems = {getString(R.string.notification_private), getString(R.string.notification_public), getString(R.string.notification_secret)};
        final int[] checkedItem = {mNotificationVisibility};

        binding.settingsNotification.setOnClickListener(v -> new MaterialAlertDialogBuilder(view.getContext())
                .setTitle(getString(R.string.pref_notification_title))
                .setSingleChoiceItems(singleItems, mNotificationVisibility, (dialogInterface, i) -> checkedItem[0] = i)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    mNotificationVisibility = checkedItem[0];
                    saveSettings();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {})
                .show());

        binding.settingsLogs.setOnClickListener(v -> startActivity(new Intent(v.getContext(), LogsActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarTitle(R.string.menu_item_settings);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    private void saveSettings() {
        Settings newSettings = new Settings(currentSettings);
        newSettings.setAllowRingOnStartup(binding.settingsStartup.isChecked());
        newSettings.setAllowPushNotifications(binding.settingsPushNotifications.isChecked());
        newSettings.setAllowPersistentNotification(binding.settingsPersistNotification.isChecked());
        newSettings.setAllowPersistentNotification(binding.settingsPersistNotification.isChecked());
        newSettings.setAllowTypingIndicator(binding.settingsTyping.isChecked());
        newSettings.setAllowReadIndicator(binding.settingsRead.isChecked());
        newSettings.setBlockRecordIndicator(binding.settingsBlockRecord.isChecked());
		newSettings.setNotificationVisibility(mNotificationVisibility);

        // save settings according to UI inputs
        presenter.saveSettings(newSettings);
    }

    /**
     * Presents a Toast explaining why the Read Contacts permission is required to display the devi-
     * ces contacts in Ring.
     */
    private void presentReadContactPermissionExplanationToast() {
        Activity activity = getActivity();
        if (null != activity) {
            String toastMessage = getString(R.string.permission_dialog_read_contacts_message);
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Presents a Toast explaining why the Write Call Log permission is required to enable the cor-
     * responding feature.
     */
    private void presentWriteCallLogPermissionExplanationToast() {
        Activity activity = getActivity();
        if (null != activity) {
            String toastMessage = getString(R.string.permission_dialog_write_call_log_message);
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void showViewModel(Settings viewModel) {
        currentSettings = viewModel;
        mIsRefreshingViewFromPresenter = true;
        binding.settingsPushNotifications.setChecked(viewModel.isAllowPushNotifications());
        binding.settingsPersistNotification.setChecked(viewModel.isAllowPersistentNotification());
        binding.settingsStartup.setChecked(viewModel.isAllowOnStartup());
        binding.settingsTyping.setChecked(viewModel.isAllowTypingIndicator());
        binding.settingsRead.setChecked(viewModel.isAllowReadIndicator());
        binding.settingsBlockRecord.setChecked(viewModel.isRecordingBlocked());
        mIsRefreshingViewFromPresenter = false;
        mNotificationVisibility = viewModel.getNotificationVisibility();
    }

    @Override
    public void onScrollChanged() {
        if (binding != null) {
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).setToolbarElevation(binding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

}
