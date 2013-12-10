package org.sflphone.fragments;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.model.Account;
import org.sflphone.views.PasswordPreference;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class GeneralAccountFragment extends PreferenceFragment {

    private static final String TAG = GeneralAccountFragment.class.getSimpleName();
    private Callbacks mCallbacks = sDummyCallbacks;
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public Account getAccount() {
            return null;
        }

    };

    public interface Callbacks {

        public Account getAccount();

    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_general_prefs);
        setPreferenceDetails(mCallbacks.getAccount().getBasicDetails());
        addPreferenceListener(mCallbacks.getAccount().getBasicDetails(), changeBasicPreferenceListener);

    }

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (!p.isTwoState) {
                    ((EditTextPreference) pref).setText(p.mValue);
                    if (pref instanceof PasswordPreference) {
                        String tmp = new String();
                        for (int i = 0; i < p.mValue.length(); ++i) {
                            tmp += "*";
                           
                        }
                        pref.setSummary(tmp);
                    } else {
                        pref.setSummary(p.mValue);
                    }
                } else {
                    ((CheckBoxPreference) pref).setChecked(p.isChecked());
                }
            } else {
                Log.w(TAG, "pref not found");
            }
        }
    }

    private void addPreferenceListener(AccountDetail details, OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "addPreferenceListener: pref " + p.mKey + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                pref.setOnPreferenceChangeListener(listener);
            } else {
                Log.w(TAG, "addPreferenceListener: pref not found");
            }
        }
    }

    Preference.OnPreferenceChangeListener changeBasicPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof CheckBoxPreference) {

                Log.i(TAG, "Changing preference value:" + newValue);
                mCallbacks.getAccount().getBasicDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
                mCallbacks.getAccount().notifyObservers();

            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing preference value:" + newValue);
                mCallbacks.getAccount().getBasicDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
                mCallbacks.getAccount().notifyObservers();

                Log.i(TAG, "Observer count:" + mCallbacks.getAccount().countObservers());
            }
            return true;
        }
    };

}
