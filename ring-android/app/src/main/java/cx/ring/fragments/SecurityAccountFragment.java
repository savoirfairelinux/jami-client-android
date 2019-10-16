/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.application.JamiApplication;
import cx.ring.model.AccountConfig;
import cx.ring.model.AccountCredentials;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.BasePreferenceFragment;
import cx.ring.utils.Tuple;
import cx.ring.views.CredentialPreferenceDialog;
import cx.ring.views.CredentialsPreference;

public class SecurityAccountFragment extends BasePreferenceFragment<SecurityAccountPresenter> implements SecurityAccountView {
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";
    private static final int SELECT_CA_LIST_RC = 42;
    private static final int SELECT_PRIVATE_KEY_RC = 43;
    private static final int SELECT_CERTIFICATE_RC = 44;

    private PreferenceCategory credentialsCategory;
    private PreferenceCategory tlsCategory;
    private Preference.OnPreferenceChangeListener editCredentialListener = (preference, newValue) -> {
        // We need the old and new value to correctly edit the list of credentials
        Pair<AccountCredentials, AccountCredentials> result = (Pair<AccountCredentials, AccountCredentials>) newValue;
        presenter.credentialEdited(new Tuple<>(result.first, result.second));
        return false;
    };
    private Preference.OnPreferenceChangeListener addCredentialListener = (preference, newValue) -> {
        Pair<AccountCredentials, AccountCredentials> result = (Pair<AccountCredentials, AccountCredentials>) newValue;
        presenter.credentialAdded(new Tuple<>(result.first, result.second));
        return false;
    };
    private Preference.OnPreferenceClickListener filePickerListener = preference -> {
        if (preference.getKey().contentEquals(ConfigKey.TLS_CA_LIST_FILE.key())) {
            performFileSearch(SELECT_CA_LIST_RC);
        }
        if (preference.getKey().contentEquals(ConfigKey.TLS_PRIVATE_KEY_FILE.key())) {
            performFileSearch(SELECT_PRIVATE_KEY_RC);
        }
        if (preference.getKey().contentEquals(ConfigKey.TLS_CERTIFICATE_FILE.key())) {
            performFileSearch(SELECT_CERTIFICATE_RC);
        }
        return true;
    };
    private Preference.OnPreferenceChangeListener tlsListener = (preference, newValue) -> {
        ConfigKey key = ConfigKey.fromString(preference.getKey());

        if (preference.getKey().contentEquals(ConfigKey.TLS_ENABLE.key())) {
            if ((Boolean) newValue) {
                key = ConfigKey.STUN_ENABLE;
                newValue = false;
            }
        }

        if (!(preference instanceof TwoStatePreference)) {
            preference.setSummary((String) newValue);
        }

        presenter.tlsChanged(key, newValue);

        return true;
    };

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // dependency injection
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreatePreferences(bundle, s);

        addPreferencesFromResource(R.xml.account_security_prefs);
        credentialsCategory = (PreferenceCategory) findPreference("Account.credentials");
        credentialsCategory.findPreference("Add.credentials").setOnPreferenceChangeListener(addCredentialListener);
        tlsCategory = (PreferenceCategory) findPreference("TLS.category");

        presenter.init(getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY));
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        if (preference instanceof CredentialsPreference) {
            CredentialPreferenceDialog preferenceDialog = CredentialPreferenceDialog.newInstance(preference.getKey());
            preferenceDialog.setTargetFragment(this, 0);
            preferenceDialog.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void addAllCredentials(ArrayList<AccountCredentials> credentials) {
        int i = 0;
        for (AccountCredentials cred : credentials) {
            CredentialsPreference toAdd = new CredentialsPreference(getPreferenceManager().getContext());
            toAdd.setKey("credential" + i);
            toAdd.setPersistent(false);
            toAdd.setCreds(cred);
            toAdd.setIcon(null);
            credentialsCategory.addPreference(toAdd);
            i++;
            toAdd.setOnPreferenceChangeListener(editCredentialListener);
        }
    }

    @Override
    public void removeAllCredentials() {
        int i = 0;
        while (true) {
            Preference toRemove = credentialsCategory.findPreference("credential" + i);
            if (toRemove == null) {
                break;
            }
            credentialsCategory.removePreference(toRemove);
            i++;
        }
    }

    @Override
    public void setDetails(AccountConfig config, String[] tlsMethods) {
        for (int i = 0; i < tlsCategory.getPreferenceCount(); ++i) {
            final Preference current = tlsCategory.getPreference(i);
            final ConfigKey key = ConfigKey.fromString(current.getKey());

            if (current instanceof TwoStatePreference) {
                ((TwoStatePreference) current).setChecked(config.getBool(key));
            } else {
                if (key == ConfigKey.TLS_CA_LIST_FILE) {
                    File crt = new File(config.get(ConfigKey.TLS_CA_LIST_FILE));
                    current.setSummary(crt.getName());
                    setFeedbackIcon(current);
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (key == ConfigKey.TLS_PRIVATE_KEY_FILE) {
                    current.setSummary(new File(config.get(ConfigKey.TLS_PRIVATE_KEY_FILE)).getName());
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (key == ConfigKey.TLS_CERTIFICATE_FILE) {
                    File pem = new File(config.get(ConfigKey.TLS_CERTIFICATE_FILE));
                    current.setSummary(pem.getName());
                    setFeedbackIcon(current);
                    checkForRSAKey();
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (key == ConfigKey.TLS_METHOD) {
                    ListPreference listPref = (ListPreference) current;
                    String curVal = config.get(key);
                    listPref.setEntries(tlsMethods);
                    listPref.setEntryValues(tlsMethods);
                    listPref.setValue(curVal);
                    current.setSummary(curVal);
                } else if (current instanceof EditTextPreference) {
                    String val = config.get(key);
                    ((EditTextPreference) current).setText(val);
                    current.setSummary(val);
                } else {
                    current.setSummary(config.get(key));
                }
            }

            current.setOnPreferenceChangeListener(tlsListener);
        }
    }

    public boolean checkCertificate() {
        // Not implemented
        return false;
    }

    public boolean findRSAKey() {
        // Not implemented
        return false;
    }

    private void checkForRSAKey() {
        if (findRSAKey()) {
            tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setEnabled(false);
        } else {
            tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setEnabled(true);
        }
    }

    private void setFeedbackIcon(Preference current) {
        Context c = current.getContext();
        if (!checkCertificate()) {
            Drawable icon = c.getDrawable(R.drawable.baseline_error_24);
            icon.setTint(c.getResources().getColor(R.color.error_red));
            current.setIcon(icon);
        } else {
            Drawable icon = c.getDrawable(R.drawable.baseline_check_circle_24);
            icon.setTint(c.getResources().getColor(R.color.green_500));
            current.setIcon(icon);
        }
    }

    public void performFileSearch(int requestCodeToSet) {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");
        startActivityForResult(intent, requestCodeToSet);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Extract returned filed for intent and populate correct preference

        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        File myFile = new File(uri.getEncodedPath());
        ConfigKey key = null;
        Preference preference;
        switch (requestCode) {
            case SELECT_CA_LIST_RC:
                preference = tlsCategory.findPreference(ConfigKey.TLS_CA_LIST_FILE.key());
                preference.setSummary(myFile.getName());
                key = ConfigKey.TLS_CA_LIST_FILE;
                setFeedbackIcon(preference);
                break;
            case SELECT_PRIVATE_KEY_RC:
                tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setSummary(myFile.getName());
                key = ConfigKey.TLS_PRIVATE_KEY_FILE;
                break;
            case SELECT_CERTIFICATE_RC:
                preference = tlsCategory.findPreference(ConfigKey.TLS_CERTIFICATE_FILE.key());
                preference.setSummary(myFile.getName());
                key = ConfigKey.TLS_CERTIFICATE_FILE;
                setFeedbackIcon(preference);
                checkForRSAKey();
                break;
            default:
                break;
        }

        presenter.fileActivityResult(key, myFile.getAbsolutePath());
    }
}
