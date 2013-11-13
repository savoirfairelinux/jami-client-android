package org.sflphone.fragments;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.sflphone.R;
import org.sflphone.account.AccountCredentials;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NestedSettingsFragment extends PreferenceFragment {

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

        Log.e(TAG, "Attaching Adavnced");
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
        switch (getArguments().getInt("MODE")) {
        case 0:

            addPreferencesFromResource(R.xml.account_credentials);
            addAllCredentials();
            break;
        case 1:
            break;
        case 2:
            break;
        }

        // setPreferenceDetails(mCallbacks.getAccount().getAdvancedDetails());
        // addPreferenceListener(mCallbacks.getAccount().getAdvancedDetails(), changeAdvancedPreferenceListener);

    }

    private void addAllCredentials() {

        ArrayList<AccountCredentials> credentials = mCallbacks.getAccount().getCredentials();
        for (AccountCredentials cred : credentials) {
            Preference toAdd = new Preference(getActivity());
            toAdd.setTitle(cred.getDetailString(AccountCredentials.CONFIG_ACCOUNT_USERNAME));
            toAdd.setSummary(cred.getDetailString(AccountCredentials.CONFIG_ACCOUNT_PASSWORD));
            getPreferenceScreen().addPreference(toAdd);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.white));

        return view;
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

    Preference.OnPreferenceChangeListener changeAdvancedPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            setDifferent(true);
            if (preference instanceof CheckBoxPreference) {
                mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
                if (preference.getKey().contentEquals("STUN.enable")) {
                    findPreference("STUN.server").setEnabled((Boolean) newValue);
                } else if (preference.getKey().contentEquals("Account.publishedSameAsLocal")) {
                    findPreference("Account.publishedPort").setEnabled((Boolean) newValue);
                    findPreference("Account.publishedAddress").setEnabled((Boolean) newValue);
                }

            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing preference value:" + newValue);
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