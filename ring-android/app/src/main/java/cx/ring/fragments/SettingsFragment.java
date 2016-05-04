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
package cx.ring.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.service.LocalService;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;

    private String FEATURE_KEY_PREF_CONTACTS = null;
    private String FEATURE_KEY_PREF_DIALER = null;

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
        mCallbacks = LocalService.DUMMY_CALLBACKS;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        FEATURE_KEY_PREF_CONTACTS = getString(R.string.pref_systemContacts_key);
        FEATURE_KEY_PREF_DIALER = getString(R.string.pref_systemDialer_key);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Activity activity = getActivity();
        if (activity instanceof HomeActivity) {
            ((HomeActivity) activity).setToolbarState(false, R.string.menu_item_settings);
        }
        this.checkAndResolveCorrectSync();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (null != preference) {
            if (getString(R.string.pref_clearHistory_key).equals(preference.getKey())) {
                this.clearHistory();
                return true;
            }
        }
        return false;
    }

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
    private void checkAndResolveCorrectSync() {
        this.checkAndResolveCorrectSyncFeatureAndPermission(FEATURE_KEY_PREF_CONTACTS);
        this.checkAndResolveCorrectSyncFeatureAndPermission(FEATURE_KEY_PREF_DIALER);
    }

    /**
     * Checks if a feature has the correct permission if any. If not, the feature is disable and the
     * layout reestablished to a proper state.
     *
     * @param feature FEATURE_KEY_PREF_CONTACTS or FEATURE_KEY_PREF_DIALER
     */
    private void checkAndResolveCorrectSyncFeatureAndPermission(String feature) {
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
    }

    /**
     * Provides the permission associated to a feature
     *
     * @param feature FEATURE_KEY_PREF_CONTACTS or FEATURE_KEY_PREF_DIALER
     * @return the permission as a String
     */
    private String neededPermissionForFeature(String feature) {
        String neededPermission = null;
        if (FEATURE_KEY_PREF_CONTACTS.equals(feature)) {
            neededPermission = Manifest.permission.READ_CONTACTS;
        } else if (FEATURE_KEY_PREF_DIALER.equals(feature)) {
            neededPermission = Manifest.permission.WRITE_CALL_LOG;
        }
        return neededPermission;
    }

    /**
     * Handles the permission managements for the key feature of the fragment
     *
     * @param sharedPreferences Shared Preferences, such as those from onSharedPreferenceChanged
     * @param feature           FEATURE_KEY_PREF_CONTACTS or FEATURE_KEY_PREF_DIALER
     * @param neededPermission  if any, the permission to manage
     */
    private void handlePermissionsForFeaturePreference(SharedPreferences sharedPreferences,
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
    }

    @Override
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
    }

    /**
     * Enables or disables a feature
     *
     * @param enable  boolean true if enabled, false otherwise
     * @param feature FEATURE_KEY_PREF_CONTACTS or FEATURE_KEY_PREF_DIALER
     */
    private void enableFeature(boolean enable, String feature) {
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
    }

    /**
     * Presents the right explanation toast for the denied permission of the corresponding feature
     *
     * @param feature FEATURE_KEY_PREF_CONTACTS or FEATURE_KEY_PREF_DIALER
     */
    private void presentPermissionExplanationToastForFeature(String feature) {
        if (!TextUtils.isEmpty(feature)) {
            if (feature.equals(FEATURE_KEY_PREF_CONTACTS)) {
                this.presentReadContactPermissionExplanationToast();
            } else if (feature.equals(FEATURE_KEY_PREF_DIALER)) {
                this.presentWriteCallLogPermissionExplanationToast();
            }
        }
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

    //region History

    /**
     * Clears all the conversations history. Ask the user to confirm first as it is a critical
     * action.
     */
    private void clearHistory() {
        final Activity activity = getActivity();
        if (activity == null || getView() == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.clear_history_dialog_title))
                .setMessage(getString(R.string.clear_history_dialog_message))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mCallbacks.getService().clearHistory();
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

    //endregion
}
