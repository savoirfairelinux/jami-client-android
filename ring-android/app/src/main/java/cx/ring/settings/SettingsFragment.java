/*
 *  Copyright (C) 2004-2017 Savoir-faire Linux Inc.
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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.Settings;
import cx.ring.mvp.BaseFragment;
import cx.ring.mvp.GenericView;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
public class SettingsFragment extends BaseFragment<SettingsPresenter> implements GenericView<Settings> {

    @BindView(R.id.settings_mobile_data)
    Switch mViewMobileData;
    @BindView(R.id.settings_push_notifications)
    Switch mViewPushNotifications;
    @BindView(R.id.settings_contacts)
    Switch mViewContacts;
    @BindView(R.id.settings_place_call)
    Switch mViewPlaceCall;
    @BindView(R.id.settings_startup)
    Switch mViewStartup;
    private boolean mIsRefreshingViewFromPresenter;

    @Override
    public int getLayout() {
        return R.layout.frag_settings;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_settings);

        // loading preferences
        presenter.loadSettings();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @OnCheckedChanged({R.id.settings_mobile_data, R.id.settings_push_notifications, R.id.settings_contacts, R.id.settings_place_call, R.id.settings_startup})
    public void onSettingsCheckedChanged(CompoundButton button, boolean isChecked) {

        String neededPermission = null;

        if (isChecked) {
            switch (button.getId()) {
                case R.id.settings_contacts:
                    neededPermission = Manifest.permission.READ_CONTACTS;
                    break;
                case R.id.settings_place_call:
                    neededPermission = Manifest.permission.WRITE_CALL_LOG;
                    break;
                default:
                    neededPermission = null;
                    break;
            }
        }

        // No specific permission needed but we known that user as triggered un settings change
        if (TextUtils.isEmpty(neededPermission) && !mIsRefreshingViewFromPresenter) {
            saveSettings();
            return;
        }

        // Some specific permissions are required, we must check for them
        if (!TextUtils.isEmpty(neededPermission)) {
            if (ContextCompat.checkSelfPermission(getActivity(), neededPermission) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //~ Ask permission to use the contacts of the device
                    if (((RingApplication) getActivity().getApplication()).canAskForPermission(neededPermission)) {
                        requestPermissions(new String[]{neededPermission}, RingApplication.PERMISSIONS_REQUEST);
                    }
                }
            } else if (!mIsRefreshingViewFromPresenter) {
                // permission is already granted
                saveSettings();
            }
        }
    }

    private void saveSettings() {
        Settings newSettings = new Settings();

        newSettings.setAllowMobileData(mViewMobileData.isChecked());
        newSettings.setAllowSystemContacts(mViewContacts.isChecked());
        newSettings.setAllowPlaceSystemCalls(mViewPlaceCall.isChecked());
        newSettings.setAllowRingOnStartup(mViewStartup.isChecked());
        newSettings.setAllowPushNotifications(mViewPushNotifications.isChecked());

        // save settings according to UI inputs
        presenter.saveSettings(newSettings);
    }

    @OnClick(R.id.settings_clear_history)
    public void onClearHistoryClick() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.clear_history_dialog_title))
                .setMessage(getString(R.string.clear_history_dialog_message))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {

                    // ask the presenter to clear history
                    presenter.clearHistory();

                    Snackbar.make(getView(),
                            getString(R.string.clear_history_completed),
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    //~ Empty
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0, n = permissions.length; i < n; ++i) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                String permission = permissions[i];
                ((RingApplication) getActivity().getApplication()).permissionHasBeenAsked(permission);
                switch (permission) {
                    case Manifest.permission.READ_CONTACTS:
                        mViewContacts.setChecked(false);
                        presentReadContactPermissionExplanationToast();
                        break;
                    case Manifest.permission.WRITE_CALL_LOG:
                        mViewPlaceCall.setChecked(false);
                        presentWriteCallLogPermissionExplanationToast();
                        break;
                }
            }
        }

        saveSettings();
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

    //region View Methods Implementation
    @Override
    public void showViewModel(Settings viewModel) {
        mIsRefreshingViewFromPresenter = true;
        mViewMobileData.setChecked(viewModel.isAllowMobileData());
        mViewPushNotifications.setChecked(viewModel.isAllowPushNotifications());
        mViewContacts.setChecked(viewModel.isAllowSystemContacts());
        mViewPlaceCall.setChecked(viewModel.isAllowPlaceSystemCalls());
        mViewStartup.setChecked(viewModel.isAllowRingOnStartup());
        mIsRefreshingViewFromPresenter = false;
    }
    //endregion
}
