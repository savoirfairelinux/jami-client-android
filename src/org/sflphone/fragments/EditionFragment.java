package org.sflphone.fragments;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.account.AccountDetailBasic;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.account.AccountDetailTls;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class EditionFragment extends PreferenceFragment {

    private static final String TAG = EditionFragment.class.getSimpleName();
    private AccountDetailBasic basicDetails = null;
    private AccountDetailAdvanced advancedDetails = null;
    private AccountDetailSrtp srtpDetails = null;
    private AccountDetailTls tlsDetails = null;
    private boolean isDifferent = false;
    private ArrayList<String> requiredFields = null;
    
    private Callbacks mCallbacks = sDummyCallbacks;
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public HashMap<String, String> getBasicDetails() {
            // TODO Stub de la méthode généré automatiquement
            return null;
        }

        @Override
        public HashMap<String, String> getAdvancedDetails() {
            // TODO Stub de la méthode généré automatiquement
            return null;
        }

        @Override
        public HashMap<String, String> getSRTPDetails() {
            // TODO Stub de la méthode généré automatiquement
            return null;
        }

        @Override
        public HashMap<String, String> getTLSDetails() {
            // TODO Stub de la méthode généré automatiquement
            return null;
        }

      
    };

    public interface Callbacks {

        public HashMap<String, String> getBasicDetails();
        public HashMap<String, String> getAdvancedDetails();
        public HashMap<String, String> getSRTPDetails();
        public HashMap<String, String> getTLSDetails();

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
        addPreferencesFromResource(R.xml.account_prefs);
        initEdition();
        requiredFields = new ArrayList<String>();
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS);
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME);
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME);
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD);

    }

    private void initEdition() {

        setBasicDetails(new AccountDetailBasic(mCallbacks.getBasicDetails()));
        setAdvancedDetails(new AccountDetailAdvanced(mCallbacks.getAdvancedDetails()));
        setSrtpDetails(new AccountDetailSrtp(mCallbacks.getSRTPDetails()));
        setTlsDetails(new AccountDetailTls(mCallbacks.getTLSDetails()));

        setPreferenceDetails(getBasicDetails());
        setPreferenceDetails(getAdvancedDetails());
        setPreferenceDetails(getSrtpDetails());
        setPreferenceDetails(getTlsDetails());

        addPreferenceListener(getBasicDetails(), changeBasicPreferenceListener);
        // addPreferenceListener(advancedDetails, changeAdvancedPreferenceListener);
        // addPreferenceListener(srtpDetails, changeSrtpPreferenceListener);
        // addPreferenceListener(tlsDetails, changeTlsPreferenceListener);
    }

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (p.mKey == AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE) {
                    ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
                    try {

                        for (Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements();) {
                            NetworkInterface i = list.nextElement();
                            Log.e("network_interfaces", "display name " + i.getDisplayName());
                            if (i.isUp())
                                entries.add(i.getDisplayName());
                        }
                    } catch (SocketException e) {
                        Log.e(TAG, e.toString());
                    }
                    CharSequence[] display = new CharSequence[entries.size()];
                    entries.toArray(display);
                    ((ListPreference) pref).setEntries(display);
                    ((ListPreference) pref).setEntryValues(display);
                    pref.setSummary(p.mValue);
                    continue;
                }
                if (!p.isTwoState) {
                    ((EditTextPreference) pref).setText(p.mValue);
                    pref.setSummary(p.mValue);
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

            setDifferent(true);
            if (preference instanceof CheckBoxPreference) {
                if ((Boolean) newValue == true)
                    getBasicDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing preference value:" + newValue);
                getBasicDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            }
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeAdvancedPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            getAdvancedDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeTlsPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            getTlsDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeSrtpPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            getSrtpDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            return true;
        }
    };

    public boolean validateAccountCreation(ArrayList<String> missingValue) {
        boolean valid = true;

        for (String s : requiredFields) {
            EditTextPreference pref = (EditTextPreference) findPreference(s);
            Log.i(TAG, "Looking for " + s);
            Log.i(TAG, "Value " + pref.getText());
            if (pref.getText().isEmpty()) {
                valid = false;
                missingValue.add(pref.getTitle().toString());
            }
        }

        return valid;
    }

    public boolean isDifferent() {
        return isDifferent;
    }

    public void setDifferent(boolean isDifferent) {
        this.isDifferent = isDifferent;
    }

    public AccountDetailAdvanced getAdvancedDetails() {
        return advancedDetails;
    }

    public void setAdvancedDetails(AccountDetailAdvanced advancedDetails) {
        this.advancedDetails = advancedDetails;
    }

    public AccountDetailBasic getBasicDetails() {
        return basicDetails;
    }

    public void setBasicDetails(AccountDetailBasic basicDetails) {
        this.basicDetails = basicDetails;
    }

    public AccountDetailSrtp getSrtpDetails() {
        return srtpDetails;
    }

    public void setSrtpDetails(AccountDetailSrtp srtpDetails) {
        this.srtpDetails = srtpDetails;
    }

    public AccountDetailTls getTlsDetails() {
        return tlsDetails;
    }

    public void setTlsDetails(AccountDetailTls tlsDetails) {
        this.tlsDetails = tlsDetails;
    }

}
