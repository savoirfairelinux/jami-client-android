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

import java.io.File;
import java.util.ArrayList;

import cx.ring.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import android.util.Pair;

import cx.ring.client.AccountCallbacks;
import cx.ring.client.AccountChangedListener;
import cx.ring.model.account.AccountConfig;
import cx.ring.model.account.AccountCredentials;
import cx.ring.model.account.Account;
import cx.ring.model.account.ConfigKey;
import cx.ring.views.CredentialPreferenceDialog;
import cx.ring.views.CredentialsPreference;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;

public class SecurityAccountFragment extends PreferenceFragment implements AccountChangedListener {
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";
    private static final int SELECT_CA_LIST_RC = 42;
    private static final int SELECT_PRIVATE_KEY_RC = 43;
    private static final int SELECT_CERTIFICATE_RC = 44;

    private static String[] TLS_METHODS = null;

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
    public void accountChanged(Account acc) {
        if (acc != null) {
            reloadCredentials();
            setDetails();
        }
    }

    @Override
    public void accountUpdated(Account acc) {

    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
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
            CredentialPreferenceDialog f = CredentialPreferenceDialog.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
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
            toAdd.setKey("credential"+i);
            toAdd.setPersistent(false);
            toAdd.setCreds(cred);
            toAdd.setOnPreferenceChangeListener(editCredentialListener);
            toAdd.setIcon(null);
            credentialsCategory.addPreference(toAdd);
            i++;
        }

    }

    private void removeAllCredentials() {
        int i = 0;
        while (true) {
            Preference toRemove = credentialsCategory.findPreference("credential" + i);
            if (toRemove == null)
                break;
            credentialsCategory.removePreference(toRemove);
            i++;
        }
    }

    private Preference.OnPreferenceChangeListener editCredentialListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Account acc = mCallbacks.getAccount();
            // We need the old and new value to correctly edit the list of credentials
            Pair<AccountCredentials, AccountCredentials> result = (Pair<AccountCredentials, AccountCredentials>) newValue;
            acc.removeCredential(result.first);
            if(result.second != null) {
                // There is a new value for this credentials it means it has been edited (otherwise deleted)
                acc.addCredential(result.second);
            }
            mCallbacks.saveAccount();
            reloadCredentials();
            return false;
        }
    };

    private Preference.OnPreferenceChangeListener addCredentialListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Account acc = mCallbacks.getAccount();
            Pair<AccountCredentials, AccountCredentials> result = (Pair<AccountCredentials, AccountCredentials>) newValue;
            acc.addCredential(result.second);
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
            Account acc = mCallbacks.getAccount();

            Log.i("TLS", "Setting " + preference.getKey() + " to " + newValue);

            if (preference.getKey().contentEquals(ConfigKey.TLS_ENABLE.key())) {
                if(((Boolean)newValue)){
                    acc.setDetail(ConfigKey.STUN_ENABLE, false);
                }
            }

            if (preference instanceof TwoStatePreference) {
                acc.setDetail(ConfigKey.fromString(preference.getKey()), (Boolean) newValue);
            } else {
                preference.setSummary((String) newValue);
                acc.setDetail(ConfigKey.fromString(preference.getKey()), (String) newValue);
            }
            mCallbacks.saveAccount();
            return true;
        }
    };

    public String[] getTlsMethods() {
        if (TLS_METHODS == null) {
            try {
                ArrayList<String> methods = (ArrayList<String>) mCallbacks.getRemoteService().getTlsSupportedMethods();
                TLS_METHODS = methods.toArray(new String[methods.size()]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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
                    current.setOnPreferenceClickListener(filePickerListener);
                    setFeedbackIcon(current, crt.getAbsolutePath());
                } else if (key == ConfigKey.TLS_PRIVATE_KEY_FILE) {
                    current.setSummary(new File(details.get(ConfigKey.TLS_PRIVATE_KEY_FILE)).getName());
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (key == ConfigKey.TLS_CERTIFICATE_FILE) {
                    File pem = new File(details.get(ConfigKey.TLS_CERTIFICATE_FILE));
                    current.setSummary(pem.getName());
                    current.setOnPreferenceClickListener(filePickerListener);
                    setFeedbackIcon(current, pem.getAbsolutePath());
                    checkForRSAKey(pem.getAbsolutePath());
                } else if (key == ConfigKey.TLS_METHOD) {
                    String[] values = getTlsMethods();
                    ListPreference lp = (ListPreference)current;
                    String cur_val = details.get(key);
                    lp.setEntries(values);
                    lp.setEntryValues(values);
                    lp.setValue(cur_val);
                    current.setSummary(cur_val);
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
        /*try {
             return mCallbacks.getService().validateCertificate(crt);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
        return false;
    }

    public boolean findRSAKey(String pemPath) {
        /*try {
            return mCallbacks.getService().checkForPrivateKey(pemPath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
        return false;
    }

    private void checkForRSAKey(String path) {
        if(findRSAKey(path)){
            tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setEnabled(false);
        }else {
            tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setEnabled(true);
        }
    }

    private void setFeedbackIcon(Preference current, String crtPath) {
        if(!checkCertificate(crtPath)){
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

        if (resultCode == Activity.RESULT_CANCELED)
            return;

        Account acc = mCallbacks.getAccount();
        File myFile = new File(data.getData().getEncodedPath());
        Preference pref;
        switch (requestCode) {
            case SELECT_CA_LIST_RC:
                pref = tlsCategory.findPreference(ConfigKey.TLS_CA_LIST_FILE.key());
                pref.setSummary(myFile.getName());
                acc.setDetail(ConfigKey.TLS_CA_LIST_FILE, myFile.getAbsolutePath());
                mCallbacks.saveAccount();
                setFeedbackIcon(pref, myFile.getAbsolutePath());
                break;
            case SELECT_PRIVATE_KEY_RC:
                tlsCategory.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key()).setSummary(myFile.getName());
                acc.setDetail(ConfigKey.TLS_PRIVATE_KEY_FILE, myFile.getAbsolutePath());
                mCallbacks.saveAccount();
                break;
            case SELECT_CERTIFICATE_RC:
                pref = tlsCategory.findPreference(ConfigKey.TLS_CERTIFICATE_FILE.key());
                pref.setSummary(myFile.getName());
                acc.setDetail(ConfigKey.TLS_CERTIFICATE_FILE, myFile.getAbsolutePath());
                mCallbacks.saveAccount();
                setFeedbackIcon(pref, myFile.getAbsolutePath());
                checkForRSAKey(myFile.getAbsolutePath());
                break;
        }
    }

}
