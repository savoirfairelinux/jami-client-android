/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.model.account;

import android.app.Activity;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import cx.ring.R;
import cx.ring.fragments.NestedSettingsFragment;

import java.io.File;

public class TLSManager {
    private static final String TAG = TLSManager.class.getSimpleName();
    private static final int SELECT_CA_LIST_RC = 42;
    private static final int SELECT_PRIVATE_KEY_RC = 43;
    private static final int SELECT_CERTIFICATE_RC = 44;
    private OnPreferenceClickListener filePickerListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE)) {
                performFileSearch(SELECT_CA_LIST_RC);
            }
            if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE)) {
                performFileSearch(SELECT_PRIVATE_KEY_RC);
            }
            if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE)) {
                performFileSearch(SELECT_CERTIFICATE_RC);
            }
            return true;
        }
    };
    PreferenceScreen mScreen;
    private Account mAccount;
    private NestedSettingsFragment mFrag;
    private OnPreferenceChangeListener tlsListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.i("TLS", "Setting " + preference.getKey() + " to" + newValue);

            if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_ENABLE)) {
                togglePreferenceScreen((Boolean) newValue);
                if(((Boolean)newValue)){
                    mAccount.getAdvancedDetails().setDetailString(AccountDetailAdvanced.CONFIG_STUN_ENABLE, Boolean.toString(false));
                }
            }

            if (preference instanceof CheckBoxPreference) {
                mAccount.getTlsDetails().setDetailString(preference.getKey(), Boolean.toString((Boolean) newValue));
            } else {
                preference.setSummary((String) newValue);
                mAccount.getTlsDetails().setDetailString(preference.getKey(), (String) newValue);
            }
            mAccount.notifyObservers();
            return true;
        }
    };

    public void onCreate(NestedSettingsFragment con, PreferenceScreen preferenceScreen, Account acc) {
        mFrag = con;
        mScreen = preferenceScreen;
        mAccount = acc;
        setDetails();
    }

    private void setDetails() {
        boolean activated = mAccount.getTlsDetails().getDetailBoolean(AccountDetailTls.CONFIG_TLS_ENABLE);

        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {

            Preference current = mScreen.getPreference(i);

            if (current instanceof CheckBoxPreference) {
                ((CheckBoxPreference) mScreen.getPreference(i)).setChecked(mAccount.getTlsDetails().getDetailBoolean(
                        mScreen.getPreference(i).getKey()));
            } else {
                if (current.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE)) {
                    File crt = new File(mAccount.getTlsDetails().getDetailString(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE));
                    current.setSummary(crt.getName());
                    current.setOnPreferenceClickListener(filePickerListener);
                    setFeedbackIcon(current, crt.getAbsolutePath());
                } else if (current.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE)) {
                    current.setSummary(new File(mAccount.getTlsDetails().getDetailString(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE)).getName());
                    current.setOnPreferenceClickListener(filePickerListener);
                } else if (current.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE)) {
                    File pem = new File(mAccount.getTlsDetails().getDetailString(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE));
                    current.setSummary(pem.getName());
                    current.setOnPreferenceClickListener(filePickerListener);
                    setFeedbackIcon(current, pem.getAbsolutePath());
                    checkForRSAKey(pem.getAbsolutePath());
                } else if (current.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_METHOD)) {
                    String[] values = mFrag.getTlsMethods();
                    ((ListPreference)current).setEntries(values);
                    ((ListPreference)current).setEntryValues(values);
                    current.setSummary(mAccount.getTlsDetails().getDetailString(mScreen.getPreference(i).getKey()));
                } else {
                    current.setSummary(mAccount.getTlsDetails().getDetailString(mScreen.getPreference(i).getKey()));
                }
            }

            // First Preference should remain enabled, it's the actual switch TLS.enable
            if (i > 0)
                current.setEnabled(activated);

        }
    }

    private void checkForRSAKey(String path) {
        if(mFrag.findRSAKey(path)){
            mScreen.findPreference(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE).setEnabled(false);
        }else {
            mScreen.findPreference(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE).setEnabled(true);
        }
    }

    private void setFeedbackIcon(Preference current, String crtPath) {
        if(!mFrag.checkCertificate(crtPath)){
            current.setIcon(R.drawable.ic_error);
        } else {
            current.setIcon(R.drawable.ic_good);
        }
    }

    public void setTLSListener() {
        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {
            mScreen.getPreference(i).setOnPreferenceChangeListener(tlsListener);
        }
    }

    private void togglePreferenceScreen(Boolean state) {
        for (int i = 1; i < mScreen.getPreferenceCount(); ++i) {
            mScreen.getPreference(i).setEnabled(state);
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
        mFrag.startActivityForResult(intent, requestCodeToSet);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Extract returned filed for intent and populate correct preference

        if (resultCode == Activity.RESULT_CANCELED)
            return;

        File myFile = new File(data.getData().getEncodedPath());
        Preference pref;
        switch (requestCode) {
            case SELECT_CA_LIST_RC:
                pref = mScreen.findPreference(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE);
                pref.setSummary(myFile.getName());
                mAccount.getTlsDetails().setDetailString(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE, myFile.getAbsolutePath());
                mAccount.notifyObservers();
                setFeedbackIcon(pref, myFile.getAbsolutePath());
                break;
            case SELECT_PRIVATE_KEY_RC:
                mScreen.findPreference(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE).setSummary(myFile.getName());
                mAccount.getTlsDetails().setDetailString(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE, myFile.getAbsolutePath());
                mAccount.notifyObservers();
                break;
            case SELECT_CERTIFICATE_RC:
                pref = mScreen.findPreference(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE);
                pref.setSummary(myFile.getName());
                mAccount.getTlsDetails().setDetailString(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE, myFile.getAbsolutePath());
                mAccount.notifyObservers();
                setFeedbackIcon(pref, myFile.getAbsolutePath());
                checkForRSAKey(myFile.getAbsolutePath());
                break;
        }
    }
}