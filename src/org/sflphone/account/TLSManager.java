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

package org.sflphone.account;

import org.sflphone.fragments.FileExplorerDFragment;
import org.sflphone.fragments.FileExplorerDFragment.onFileSelectedListener;
import org.sflphone.model.Account;

import android.app.Activity;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;

public class TLSManager implements onFileSelectedListener {
    PreferenceScreen mScreen;
    private Account mAccount;
    static Activity mContext;

    public void onCreate(Activity con, PreferenceScreen preferenceScreen, Account acc) {
        mContext = con;
        mScreen = preferenceScreen;
        mAccount = acc;

        setDetails();
    }

    private void setDetails() {
        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {

            if (mScreen.getPreference(i) instanceof CheckBoxPreference) {
                ((CheckBoxPreference) mScreen.getPreference(i)).setChecked(mAccount.getTlsDetails().getDetailBoolean(
                        mScreen.getPreference(i).getKey()));
            } else {
                mScreen.getPreference(i).setSummary(mAccount.getTlsDetails().getDetailString(mScreen.getPreference(i).getKey()));
            }

            // ((CheckBoxPreference)
            // mScreen.getPreference(i)).setChecked(mAccount.getSrtpDetails().getDetailBoolean(mScreen.getPreference(i).getKey()));
            mScreen.getPreference(i).setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE)
                            || preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE)
                            || preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE)) {
                        FileExplorerDFragment dialog = FileExplorerDFragment.newInstance();
                        dialog.show(mContext.getFragmentManager(), "explorerdialog");
                        dialog.setOnFileSelectedListener(TLSManager.this, preference.getKey());
                    }

                    return false;
                }
            });
        }
    }

    public void setTLSListener() {
        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {
            mScreen.getPreference(i).setOnPreferenceChangeListener(tlsListener);
        }
    }

    private OnPreferenceChangeListener tlsListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.i("TLS", "Setting " + preference.getKey() + " to" + (Boolean) newValue);
            mAccount.getTlsDetails().setDetailString(preference.getKey(), Boolean.toString((Boolean) newValue));
            mAccount.notifyObservers();
            return true;
        }
    };

    @Override
    public void onFileSelected(String path, String prefKey) {
        mScreen.findPreference(prefKey).setSummary(path);
        mAccount.getTlsDetails().setDetailString(prefKey, path);
        mAccount.notifyObservers();
    }

}