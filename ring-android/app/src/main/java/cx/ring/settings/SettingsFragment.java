/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragSettingsBinding;
import cx.ring.model.Settings;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.GenericView;
import cx.ring.utils.DeviceUtils;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
public class SettingsFragment extends BaseSupportFragment<SettingsPresenter> implements GenericView<Settings>, ViewTreeObserver.OnScrollChangedListener {

    private static final int SCROLL_DIRECTION_UP = -1;

    private FragSettingsBinding binding;

    private boolean mIsRefreshingViewFromPresenter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragSettingsBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
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
        if (TextUtils.isEmpty(JamiApplication.getInstance().getPushToken())) {
            binding.settingsPushNotificationsLayout.setVisibility(View.GONE);
        }
        // loading preferences
        presenter.loadSettings();
        if (DeviceUtils.isTablet(getContext())) {
            Toolbar toolbar = getActivity().findViewById(R.id.main_toolbar);
            TextView title = toolbar.findViewById(R.id.contact_title);
            ImageView logo = toolbar.findViewById(R.id.contact_image);

            logo.setVisibility(View.GONE);
            title.setText(R.string.menu_item_settings);
            title.setTextSize(19);
            title.setTypeface(null, Typeface.BOLD);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) title.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_TOP);
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            title.setLayoutParams(params);
        } else {
            ((HomeActivity) getActivity()).setToolbarState(R.string.menu_item_settings);
        }

        binding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        binding.settingsDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> presenter.setDarkMode(isChecked));

        CompoundButton.OnCheckedChangeListener save = (buttonView, isChecked) -> {
            if (!mIsRefreshingViewFromPresenter)
                saveSettings();
        };
        binding.settingsPushNotifications.setOnCheckedChangeListener(save);
        binding.settingsStartup.setOnCheckedChangeListener(save);
        binding.settingsPersistNotification.setOnCheckedChangeListener(save);

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
    }

    @Override
    public void onResume() {
        super.onResume();
        FragmentManager fragmentManager = requireFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG);
        if (existingFragment == null) {
            ((HomeActivity) getActivity()).goToSettings();
        }
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
        Settings newSettings = new Settings();
        newSettings.setAllowRingOnStartup(binding.settingsStartup.isChecked());
        newSettings.setAllowPushNotifications(binding.settingsPushNotifications.isChecked());
        newSettings.setAllowPersistentNotification(binding.settingsPersistNotification.isChecked());

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
        mIsRefreshingViewFromPresenter = true;
        binding.settingsPushNotifications.setChecked(viewModel.isAllowPushNotifications());
        binding.settingsPersistNotification.setChecked(viewModel.isAllowPersistentNotification());
        binding.settingsStartup.setChecked(viewModel.isAllowRingOnStartup());
        mIsRefreshingViewFromPresenter = false;
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
