package org.sflphone.fragments;

import java.util.Locale;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.model.Account;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

public class SecurityAccountFragment extends PreferenceFragment {

    private static final String TAG = SecurityAccountFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public Account getAccount() {
            return null;
        }

        @Override
        public void displayCredentialsScreen() {
        }

        @Override
        public void displaySRTPScreen() {
        }

        @Override
        public void displayTLSScreen() {
        }

    };

    public interface Callbacks {

        public Account getAccount();

        public void displayCredentialsScreen();

        public void displaySRTPScreen();

        public void displayTLSScreen();

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

        findPreference("Credential.count").setSummary("" + mCallbacks.getAccount().getCredentials().size());
        findPreference("Credential.count").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                mCallbacks.displayCredentialsScreen();
                return false;
            }
        });

        setSrtpPreferenceDetails(mCallbacks.getAccount().getSrtpDetails());
        addPreferenceListener(mCallbacks.getAccount().getSrtpDetails(), changeSrtpModeListener);

        findPreference("TLS.details").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                mCallbacks.displayTLSScreen();
                return false;
            }
        });

    }

    private void setSrtpPreferenceDetails(AccountDetailSrtp details) {

        if (details.getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE)) {
            findPreference("SRTP.enable").setSummary(
                    details.getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE).toUpperCase(Locale.getDefault()));

        } else {
            findPreference("SRTP.enable").setSummary(getResources().getString(R.string.account_srtp_deactivated));

        }

        findPreference("SRTP.details").setEnabled(details.getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE));
    }

    private void addPreferenceListener(AccountDetail details, OnPreferenceChangeListener listener) {

        findPreference("SRTP.enable").setOnPreferenceChangeListener(changeSrtpModeListener);
        findPreference("SRTP.details").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                mCallbacks.displaySRTPScreen();
                return false;
            }
        });

    }

    Preference.OnPreferenceChangeListener changeSrtpModeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            if (((String) newValue).contentEquals("NONE")) {
                mCallbacks.getAccount().getSrtpDetails().setDetailString(AccountDetailSrtp.CONFIG_SRTP_ENABLE, AccountDetail.FALSE_STR);
                preference.setSummary(getResources().getString(R.string.account_srtp_deactivated));
            } else {
                mCallbacks.getAccount().getSrtpDetails().setDetailString(AccountDetailSrtp.CONFIG_SRTP_ENABLE, AccountDetail.TRUE_STR);
                mCallbacks.getAccount().getSrtpDetails()
                        .setDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE, ((String) newValue).toLowerCase(Locale.getDefault()));
                preference.setSummary(((String) newValue));
            }
            findPreference("SRTP.details").setEnabled(!((String) newValue).contentEquals("NONE"));
            mCallbacks.getAccount().notifyObservers();
            return true;
        }
    };

}