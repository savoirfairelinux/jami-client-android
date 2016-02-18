/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.service.LocalService;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = SettingsFragment.class.getSimpleName();

    private String KEY_PREF_CONTACTS = null;
    private String KEY_PREF_DIALER = null;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        KEY_PREF_CONTACTS = getString(R.string.pref_systemContacts_key);
        KEY_PREF_DIALER = getString(R.string.pref_systemDialer_key);
    }

    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Activity activity = getActivity();
        if (activity instanceof HomeActivity)
            ((HomeActivity) activity).setToolbarState(false, R.string.menu_item_settings);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_PREF_CONTACTS)) {
            boolean val = sharedPreferences.getBoolean(KEY_PREF_CONTACTS, true);
            if (val && !LocalService.checkPermission(getActivity(), Manifest.permission.READ_CONTACTS)) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CONTACTS}, LocalService.PERMISSIONS_REQUEST);
            }
        } else if (key.equals(KEY_PREF_DIALER)) {
            boolean val = sharedPreferences.getBoolean(KEY_PREF_DIALER, false);
            if (val && !LocalService.checkPermission(getActivity(), Manifest.permission.WRITE_CALL_LOG)) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_CALL_LOG}, LocalService.PERMISSIONS_REQUEST);
            }
        }
    }

    private void updateContactPreference() {
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        boolean val = prefs.getBoolean(KEY_PREF_CONTACTS, true);
        if (val && !LocalService.checkPermission(getActivity(), Manifest.permission.READ_CONTACTS)) {
            prefs.edit().putBoolean(KEY_PREF_CONTACTS, false).apply();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LocalService.PERMISSIONS_REQUEST: {
                if (grantResults.length == 0) {
                    updateContactPreference();
                    return;
                }
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        updateContactPreference();
                        return;
                    }
                }
                break;
            }
        }
    }

}
