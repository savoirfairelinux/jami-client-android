package org.sflphone.fragments;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.model.Account;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class AdvancedAccountFragment extends PreferenceFragment {

    private static final String TAG = AdvancedAccountFragment.class.getSimpleName();

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
        addPreferencesFromResource(R.xml.account_advanced_prefs);
        setPreferenceDetails(mCallbacks.getAccount().getAdvancedDetails());
        addPreferenceListener(mCallbacks.getAccount().getAdvancedDetails(), changeAdvancedPreferenceListener);

    }

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (p.mKey == AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE) {
                    ArrayList<CharSequence> entries = getNetworkInterfaces();
                    CharSequence[] display = new CharSequence[entries.size()];
                    entries.toArray(display);
                    ((ListPreference) pref).setEntries(display);
                    ((ListPreference) pref).setEntryValues(display);
                    pref.setSummary(p.mValue);
                    continue;
                }
                if (!p.isTwoState) {
                    pref.setSummary(p.mValue);
                } else if(pref.getKey().contentEquals("STUN.enable")){
                    ((CheckBoxPreference)pref).setChecked(p.mValue.contentEquals("true"));
                    findPreference("STUN.server").setEnabled(p.mValue.contentEquals("true"));
                } else if(pref.getKey().contentEquals("Account.publishedSameAsLocal")){
                    ((CheckBoxPreference)pref).setChecked(p.mValue.contentEquals("true"));
                    findPreference("Account.publishedPort").setEnabled(!p.mValue.contentEquals("true"));
                    findPreference("Account.publishedAddress").setEnabled(!p.mValue.contentEquals("true"));
                }
            } else {
                Log.w(TAG, "pref not found");
            }
        }
    }

    private ArrayList<CharSequence> getNetworkInterfaces() {
        ArrayList<CharSequence> result = new ArrayList<CharSequence>();
        
        result.add("default");
        try {

            for (Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements();) {
                NetworkInterface i = list.nextElement();
                if (i.isUp())
                    result.add(i.getDisplayName());
            }
        } catch (SocketException e) {
            Log.e(TAG, e.toString());
        }
        return result;
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

    Preference.OnPreferenceChangeListener changeAdvancedPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            setDifferent(true);
            if (preference instanceof CheckBoxPreference) {
                mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
                if (preference.getKey().contentEquals("STUN.enable")) {
                    findPreference("STUN.server").setEnabled((Boolean) newValue);
                } else if (preference.getKey().contentEquals("Account.publishedSameAsLocal")) {
                    findPreference("Account.publishedPort").setEnabled(!(Boolean) newValue);
                    findPreference("Account.publishedAddress").setEnabled(!(Boolean) newValue);
                }

            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing" + preference.getKey() + " value:" + newValue);
                mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
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
