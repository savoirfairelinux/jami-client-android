/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
import android.content.Intent;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.AccountCredentials;
import cx.ring.model.ConfigKey;
import cx.ring.services.AccountService;
import cx.ring.views.CredentialPreferenceDialog;
import cx.ring.views.CredentialsPreference;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;

public class SecurityAccountFragment extends PreferenceFragment implements AccountChangedListener {
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";
    private static final int SELECT_CA_LIST_RC = 42;
    private static final int SELECT_PRIVATE_KEY_RC = 43;
    private static final int SELECT_CERTIFICATE_RC = 44;

    private static String[] TLS_METHODS = null;

    @Inject
    AccountService mAccountService;

    @SuppressWarnings("unused")
    private static final String TAG = SecurityAccountFragment.class.getSimpleName();

    private PreferenceCategory credentialsCategory;
    private PreferenceCategory tlsCategory;

    private AccountCallbacks mCallbacks = DUMMY_CALLBACKS;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (AccountCallbacks) activity;
        mCallbacks.addOnAccountChanged(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mCallbacks != null) {
            mCallbacks.removeOnAccountChanged(this);
        }
        mCallbacks = DUMMY_CALLBACKS;
    }

    @Override
    public void accountChanged(Account account) {
        if (account != null) {
            reloadCredentials();
            setDetails();
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        addPreferencesFromResource(R.xml.account_security_prefs);
        credentialsCategory = (PreferenceCategory) findPreference("Account.credentials");
        credentialsCategory.findPreference("Add.credentials").setOnPreferenceChangeListener(addCredentialListener);
        tlsCategory = (PreferenceCategory) findPreference("TLS.category");

        Account acc = mCallbacks.getAccount();
        if (acc != null) {
            accountChanged(acc);
        }
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

    public void reloadCredentials() {
        removeAllCredentials();
        addAllCredentials();
    }

    private void addAllCredentials() {
        ArrayList<AccountCredentials> credentials = mCallbacks.getAccount().getCredentials();
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

    private void removeAllCredentials() {
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

    private Preference.OnPreferenceChangeListener editCredentialListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Account account = mCallbacks.getAccount();
            // We need the old and new value to correctly edit the list of credentials
            Pair<AccountCredentials, AccountCredentials> result = (Pair<AccountCredentials, AccountCredentials>) newValue;
            account.removeCredential(result.first);
            if (result.second != null) {
                // There is a new value for this credentials it means it has been edited (otherwise deleted)
                account.addCredential(result.second);
            }
            mCallbacks.saveAccount();
            reloadCredentials();
            return false;
        }
    };

    private Preference.OnPreferenceChangeListener addCredentialListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Account account = mCallbacks.getAccount();
            Pair<AccountCredentials, AccountCredentials> result = (Pair<AccountCredentials, AccountCredentials>) newValue;
            account.addCredential(result.second);
            mCallbacks.saveAccount();
            reloadCredentials();
            return false;
        }
    };

    private Preference.OnPreferenceClickListener filePickerListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
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
        }
    };

    private Preference.OnPreferenceChangeListener tlsListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Account account = mCallbacks.getAccount();
            Log.i("TLS", "Setting " + preference.getKey() + " to " + newValue);
            if (preference.getKey().contentEquals(ConfigKey.TLS_ENABLE.key())) {
                if ((Boolean) newValue) {
                    account.setDetail(ConfigKey.STUN_ENABLE, false);
                }
            }

            if (preference instanceof TwoStatePreference) {
                account.setDetail(ConfigKey.fromString(preference.getKey()), (Boolean) newValue);
            } else {
                preference.setSummary((String) newValue);
                account.setDetail(ConfigKey.fromString(preference.getKey()), (String) newValue);
            }
            mCallbacks.saveAccount();
            return true;
        }
    };

    public String[] getTlsMethods() {
        if (TLS_METHODS == null) {
            List<String> methods = mAccountService.getTlsSupportedMethods();
            TLS_METHODS = methods.toArray(new String[methods.size()]);
        }
        return TLS_METHODS;
    }

    private void setDetails() {
        final AccountConfig details = mCallbacks.getAccount().getConfig();

        for (int i = 0; i < tlsCategory.getPreferenceCount(); ++i) {
            final Preference current = tlsCategory.getPreference(i);
            final ConfigKey key = ConfigKey.fromString(current.getKey());

            if (current instanceof TwoStatePreference) {
                ((TwoStatePreference) current).setChecked(details.getBool(key));
            } else {
                if (key == ConfigKey.TLS_CA_LIST_FILE) {
                    File crt = new File(details.get(ConfigKey.TLS_CA_LIST_FILE));
                    current.setSummary(crt.getName());
                    setFeedbackIcon(current, crt.getAbsolutePath());
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (key == ConfigKey.TLS_PRIVATE_KEY_FILE) {
                    current.setSummary(new File(details.get(ConfigKey.TLS_PRIVATE_KEY_FILE)).getName());
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (key == ConfigKey.TLS_CERTIFICATE_FILE) {
                    File pem = new File(details.get(ConfigKey.TLS_CERTIFICATE_FILE));
                    current.setSummary(pem.getName());
                    setFeedbackIcon(current, pem.getAbsolutePath());
                    checkForRSAKey(pem.getAbsolutePath());
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (key == ConfigKey.TLS_METHOD) {
                    String[] values = getTlsMethods();
                    ListPreference listPref = (ListPreference) current;
                    String curVal = details.get(key);
                    listPref.setEntries(values);
                    listPref.setEntryValues(values);
                    listPref.setValue(curVal);
                    current.setSummary(curVal);
                } else if (current instanceof EditTextPreference) {
                    String val = details.get(key);
                    ((EditTextPreference) current).setText(val);
                    current.setSummary(val);
                } else {
                    current.setSummary(details.get(key));
                }
            }

            current.setOnPreferenceChangeListener(tlsListener);
        }
    }

    public boolean checkCertificate(String crt) {
        // Not implemented
        return false;
    }

    public boolean findRSAKey(String pemPath) {
        // Not implemented
        return false;
    }

    private void checkForRSAKey(String path) {
        if (findRSAKey(path)) {
            tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setEnabled(false);
        } else {
            tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setEnabled(true);
        }
    }

    private void setFeedbackIcon(Preference current, String crtPath) {
        if (!checkCertificate(crtPath)) {
            current.setIcon(R.drawable.ic_error);
        } else {
            current.setIcon(R.drawable.ic_good);
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

        Account account = mCallbacks.getAccount();
        File myFile = new File(data.getData().getEncodedPath());
        Preference preference;
        switch (requestCode) {
            case SELECT_CA_LIST_RC:
                preference = tlsCategory.findPreference(ConfigKey.TLS_CA_LIST_FILE.key());
                preference.setSummary(myFile.getName());
                account.setDetail(ConfigKey.TLS_CA_LIST_FILE, myFile.getAbsolutePath());
                mCallbacks.saveAccount();
                setFeedbackIcon(preference, myFile.getAbsolutePath());
                break;
            case SELECT_PRIVATE_KEY_RC:
                tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setSummary(myFile.getName());
                account.setDetail(ConfigKey.TLS_PRIVATE_KEY_FILE, myFile.getAbsolutePath());
                mCallbacks.saveAccount();
                break;
            case SELECT_CERTIFICATE_RC:
                preference = tlsCategory.findPreference(ConfigKey.TLS_CERTIFICATE_FILE.key());
                preference.setSummary(myFile.getName());
                account.setDetail(ConfigKey.TLS_CERTIFICATE_FILE, myFile.getAbsolutePath());
                mCallbacks.saveAccount();
                setFeedbackIcon(preference, myFile.getAbsolutePath());
                checkForRSAKey(myFile.getAbsolutePath());
                break;
            default:
                break;
        }
    }
}
