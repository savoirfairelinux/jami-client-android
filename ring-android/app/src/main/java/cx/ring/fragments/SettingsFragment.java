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
    public static final String KEY_PREF_MOBILE = "pref_mobileData";
    public static final String KEY_PREF_CONTACTS = "pref_systemContacts";
    public static final String KEY_PREF_DIALER = "pref_systemDialer";

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
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
