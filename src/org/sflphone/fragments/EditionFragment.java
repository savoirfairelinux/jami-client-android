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
import org.sflphone.model.Account;

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

    private boolean isDifferent = false;
    

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
        addPreferencesFromResource(R.xml.account_prefs);
        initEdition();

    }

    private void initEdition() {


        setPreferenceDetails(mCallbacks.getAccount().getBasicDetails());
        setPreferenceDetails(mCallbacks.getAccount().getAdvancedDetails());
        setPreferenceDetails(mCallbacks.getAccount().getSrtpDetails());
        setPreferenceDetails(mCallbacks.getAccount().getTlsDetails());

        addPreferenceListener(mCallbacks.getAccount().getBasicDetails(), changeBasicPreferenceListener);
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
                    mCallbacks.getAccount().getBasicDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing preference value:" + newValue);
                mCallbacks.getAccount().getBasicDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            }
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeAdvancedPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeTlsPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            mCallbacks.getAccount().getTlsDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeSrtpPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            mCallbacks.getAccount().getSrtpDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
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
