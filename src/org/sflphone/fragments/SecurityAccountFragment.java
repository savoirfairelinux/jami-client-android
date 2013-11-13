package org.sflphone.fragments;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.model.Account;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class SecurityAccountFragment extends PreferenceFragment {

    private static final String TAG = SecurityAccountFragment.class.getSimpleName();

    private boolean isDifferent = false;
    private Callbacks mCallbacks = sDummyCallbacks;
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public Account getAccount() {
            return null;
        }

        @Override
        public void displayCredentialsScreen() {
        }

    };

    public interface Callbacks {

        public Account getAccount();

        public void displayCredentialsScreen();

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
        addPreferencesFromResource(R.xml.account_security_prefs);
        setPreferenceDetails(mCallbacks.getAccount().getTlsDetails());
        // setPreferenceDetails(mCallbacks.getAccount().getSrtpDetails());
        // addPreferenceListener(mCallbacks.getAccount().getTlsDetails(), changeTlsPreferenceListener);
        // addPreferenceListener(mCallbacks.getAccount().getSrtpDetails(), changeSrtpPreferenceListener);

    }

    private void setPreferenceDetails(AccountDetail details) {

        findPreference("Credential.count").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                mCallbacks.displayCredentialsScreen();
                return false;
            }
        });

        // for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
        // Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
        // Preference pref = findPreference(p.mKey);
        // if (pref != null) {
        // if (!p.isTwoState) {
        // ((EditTextPreference) pref).setText(p.mValue);
        // pref.setSummary(p.mValue);
        // }
        // } else {
        // Log.w(TAG, "pref not found");
        // }
        // }
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

    Preference.OnPreferenceChangeListener changeTlsPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            setDifferent(true);
            if (preference instanceof CheckBoxPreference) {
                if ((Boolean) newValue == true)
                    mCallbacks.getAccount().getTlsDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing preference value:" + newValue);
                mCallbacks.getAccount().getTlsDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            }
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeSrtpPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            setDifferent(true);
            if (preference instanceof CheckBoxPreference) {
                if ((Boolean) newValue == true)
                    mCallbacks.getAccount().getSrtpDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing preference value:" + newValue);
                mCallbacks.getAccount().getSrtpDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            }
            return true;
        }
    };

    public boolean isDifferent() {
        return isDifferent;
    }

    public void setDifferent(boolean isDifferent) {
        this.isDifferent = isDifferent;
    }

}