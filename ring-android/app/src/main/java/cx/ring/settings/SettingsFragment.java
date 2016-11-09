/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Unbinder;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;
import cx.ring.model.Settings;
import cx.ring.mvp.GenericView;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
public class SettingsFragment extends Fragment implements GenericView<SettingsViewModel> {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    Unbinder mUnbinder;

    @Inject
    SettingsPresenter mSettingsPresenter;

    @BindView(R.id.settings_mobile_data)
    Switch mViewMobileData;

    @BindView(R.id.settings_contacts)
    Switch mViewContacts;

    @BindView(R.id.settings_place_call)
    Switch mViewPlaceCall;

    @BindView(R.id.settings_startup)
    Switch mViewStartup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_settings, parent, false);

        // views injection
        mUnbinder = ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_settings);

        // view binding
        mSettingsPresenter.bindView(this);

        // loading preferences
        mSettingsPresenter.loadSettings();

        //this.checkAndResolveCorrectSync();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Butterknife unbinding
        mUnbinder.unbind();

        // view unbinding
        mSettingsPresenter.unbindView();
    }

    @OnCheckedChanged({R.id.settings_mobile_data, R.id.settings_contacts, R.id.settings_place_call, R.id.settings_startup})
    public void onSettingsCheckedChanged() {
        Settings newSettings = new Settings();
        newSettings.setAllowMobileData(mViewMobileData.isChecked());
        newSettings.setAllowSystemContacts(mViewContacts.isChecked());
        newSettings.setAllowPlaceSystemCalls(mViewPlaceCall.isChecked());
        newSettings.setAllowRingOnStartup(mViewStartup.isChecked());

        // save settings according to UI inputs
        mSettingsPresenter.saveSettings(newSettings);
    }

    @OnClick(R.id.settings_clear_history)
    public void onClearHistoryClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.clear_history_dialog_title))
                .setMessage(getString(R.string.clear_history_dialog_message))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // ask the presenter to clear history
                        mSettingsPresenter.clearHistory();

                        Snackbar.make(getView(),
                                getString(R.string.clear_history_completed),
                                Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //~ Empty
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
/*
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String neededPermission = this.neededPermissionForFeature(key);
        this.handlePermissionsForFeaturePreference(sharedPreferences,
                key,
                neededPermission);
    }

    /**
     * Check if all the features are in a good state of activation.
     *
     * @see SettingsFragment#checkAndResolveCorrectSyncFeatureAndPermission(String)
     */
  /*  private void checkAndResolveCorrectSync() {
        this.checkAndResolveCorrectSyncFeatureAndPermission(FEATURE_KEY_PREF_CONTACTS);
        this.checkAndResolveCorrectSyncFeatureAndPermission(FEATURE_KEY_PREF_DIALER);
    }*/


  /*  private void checkAndResolveCorrectSyncFeatureAndPermission(String feature) {
        if (TextUtils.isEmpty(feature)) {
            return;
        }
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        boolean useFeature = sharedPreferences.getBoolean(feature, false);
        String neededPermission = this.neededPermissionForFeature(feature);
        if (!TextUtils.isEmpty(neededPermission) && useFeature) {
            boolean hasPermission = LocalService.checkPermission(getActivity(), neededPermission);
            if (!hasPermission) {
                this.enableFeature(false, feature);
            }
        }
    }*/


  /*  private String neededPermissionForFeature(String feature) {
        String neededPermission = null;
        if (FEATURE_KEY_PREF_CONTACTS.equals(feature)) {
            neededPermission = Manifest.permission.READ_CONTACTS;
        } else if (FEATURE_KEY_PREF_DIALER.equals(feature)) {
            neededPermission = Manifest.permission.WRITE_CALL_LOG;
        }
        return neededPermission;
    }*/


  /*  private void handlePermissionsForFeaturePreference(SharedPreferences sharedPreferences,
                                                       String feature,
                                                       String neededPermission) {
        if (null == sharedPreferences ||
                TextUtils.isEmpty(feature) ||
                TextUtils.isEmpty(neededPermission)) {
            Log.d(TAG, "No permission to handle for feature");
            return;
        }
        //~ Checking if the user wants to use the feature
        boolean useFeature = sharedPreferences.getBoolean(feature, true);
        //~ Checking if a permission is required to use the enabled feature
        if (useFeature && !TextUtils.isEmpty(neededPermission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //~ Must ask permission to use the feature
                if (!LocalService.checkPermission(getActivity(),
                        neededPermission)) {
                    //~ Ask permission to use the contacts of the device
                    requestPermissions(new String[]{neededPermission},
                            LocalService.PERMISSIONS_REQUEST);
                }
            }
        }
    }*/

    /*@Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0, n = permissions.length; i < n; ++i) {
            boolean granted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            switch (permissions[i]) {
                case Manifest.permission.READ_CONTACTS: {
                    this.enableFeature(granted, FEATURE_KEY_PREF_CONTACTS);
                    Activity activity = getActivity();
                    if (activity instanceof HomeActivity) {
                        HomeActivity homeActivity = (HomeActivity) getActivity();
                        homeActivity.getService().refreshContacts();
                    }
                }
                break;
                case Manifest.permission.WRITE_CALL_LOG: {
                    this.enableFeature(granted, FEATURE_KEY_PREF_DIALER);
                }
                break;
            }
        }
    }*/

    /**
     * Enables or disables a feature
     *
     * @param enable  boolean true if enabled, false otherwise
     * @param feature FEATURE_KEY_PREF_CONTACTS or FEATURE_KEY_PREF_DIALER
     */
 /*   private void enableFeature(boolean enable, String feature) {
        if (TextUtils.isEmpty(feature)) {
            return;
        }
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.edit().putBoolean(feature, enable).apply();
        SwitchPreference pref = (SwitchPreference) findPreference(feature);
        pref.setChecked(enable);
        if (!enable) {
            this.presentPermissionExplanationToastForFeature(feature);
        }
    }*/

    /**
     * Presents the right explanation toast for the denied permission of the corresponding feature
     *
     * @param feature FEATURE_KEY_PREF_CONTACTS or FEATURE_KEY_PREF_DIALER
     */
 /*   private void presentPermissionExplanationToastForFeature(String feature) {
        if (!TextUtils.isEmpty(feature)) {
            if (feature.equals(FEATURE_KEY_PREF_CONTACTS)) {
                this.presentReadContactPermissionExplanationToast();
            } else if (feature.equals(FEATURE_KEY_PREF_DIALER)) {
                this.presentWriteCallLogPermissionExplanationToast();
            }
        }
    }
*/

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
    public void showViewModel(SettingsViewModel viewModel) {
        mViewMobileData.setChecked(viewModel.isAllowMobileData());
        mViewContacts.setChecked(viewModel.isAllowSystemContacts());
        mViewPlaceCall.setChecked(viewModel.isAllowPlaceSystemCalls());
        mViewStartup.setChecked(viewModel.isAllowRingOnStartup());
    }
    //endregion
}
