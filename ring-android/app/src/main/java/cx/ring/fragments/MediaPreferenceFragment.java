/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.fragments;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;

import cx.ring.R;
import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.Account;
import cx.ring.model.Codec;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

public class MediaPreferenceFragment extends PreferenceFragment {
    static final String TAG = MediaPreferenceFragment.class.getSimpleName();

    public interface Callbacks extends LocalService.Callbacks {
        Account getAccount();
    }

    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public IDRingService getRemoteService() {
            return null;
        }

        @Override
        public LocalService getService() {
            return null;
        }

        @Override
        public Account getAccount() {
            return null;
        }
    };
    protected Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    private static final int SELECT_RINGTONE_PATH = 40;
    private Preference.OnPreferenceClickListener filePickerListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            performFileSearch(SELECT_RINGTONE_PATH);
            return true;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED)
            return;

        File myFile = new File(data.getData().getPath());
        Log.i(TAG, "file selected:" + data.getData());
        if (requestCode == SELECT_RINGTONE_PATH) {
            findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setSummary(myFile.getName());
            mCallbacks.getAccount().getAdvancedDetails().setDetailString(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, myFile.getAbsolutePath());
            mCallbacks.getAccount().notifyObservers();
        }

    }

    public void performFileSearch(int requestCodeToSet) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, requestCodeToSet);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        final Account acc = mCallbacks.getAccount();
        if (acc == null)
            return;
        addPreferencesFromResource(R.xml.account_media_prefs);
        setPreferenceDetails(acc.getBasicDetails());
        setPreferenceDetails(acc.getAdvancedDetails());
        findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled(
                ((TwoStatePreference) findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED)).isChecked());
        addPreferenceListener(acc.getAdvancedDetails(), changeAudioPreferenceListener);
        addPreferenceListener(AccountDetailBasic.CONFIG_VIDEO_ENABLED, new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference instanceof TwoStatePreference) {
                    acc.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
                }
                acc.notifyObservers();
                return true;
            }
        });

        final CodecPreference pref = (CodecPreference) findPreference("Ring.codecs");
        try {
            pref.setCodecs((ArrayList<Codec>) mCallbacks.getRemoteService().getCodecList(acc.getAccountID()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Log.i(TAG, "Changing" + preference.getKey());
                try {
                    mCallbacks.getRemoteService().setActiveCodecList(pref.getActiveCodecList(), acc.getAccountID());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                acc.notifyObservers();
                return true;
            }
        });

    }

    private final Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account acc = mCallbacks.getAccount();
            String key = preference.getKey();
            if (preference instanceof TwoStatePreference) {
                if (key.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED))
                    getPreferenceScreen().findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled((Boolean) newValue);
                acc.getAdvancedDetails().setDetailString(key, newValue.toString());
            } else  if (key.contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                preference.setSummary(((String)newValue).contentEquals("overrtp") ? "RTP" : "SIP");
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing" + key + " value:" + newValue);
                acc.getAdvancedDetails().setDetailString(key, newValue.toString());
            }
            acc.notifyObservers();

            return true;
        }
    };

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref == null) {
                Log.w(TAG, "pref not found");
            } else if (pref instanceof TwoStatePreference) {
                ((TwoStatePreference) pref).setChecked(p.mValue.contentEquals("true"));
            } else if (p.mKey.contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                pref.setDefaultValue(p.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
                pref.setSummary(p.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
            } else if (p.mKey.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH)) {
                File tmp = new File(p.mValue);
                pref.setSummary(tmp.getName());
            } else
                pref.setSummary(p.mValue);
        }
    }

    private void addPreferenceListener(AccountDetail details, Preference.OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "addPreferenceListener: pref " + p.mKey + p.mValue);
            addPreferenceListener(p.mKey, listener);
        }
    }

    private void addPreferenceListener(String key, Preference.OnPreferenceChangeListener listener) {
        Log.i(TAG, "addPreferenceListener: pref " + key);
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(listener);
            if (key.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH))
                pref.setOnPreferenceClickListener(filePickerListener);
        } else {
            Log.w(TAG, "addPreferenceListener: pref not found");
        }
    }


}
