/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.account;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.preference.TwoStatePreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.BaseFragment;
import cx.ring.utils.Log;
import cx.ring.views.EditTextIntegerPreference;
import cx.ring.views.EditTextPreferenceDialog;
import cx.ring.views.PasswordPreference;

public class GeneralAccountFragment extends BaseFragment<GeneralAccountPresenter> implements GeneralAccountView {

    private static final String TAG = GeneralAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";
    private String mAccountID;
    private boolean mIsRing;

    @BindView(R.id.account_alias)
    TextView mAlias;

    @BindView(R.id.account_hostname)
    TextView mHostname;

    @BindView(R.id.account_useragent)
    TextView mUseragent;

    @BindView(R.id.account_autoanswer)
    Switch mAutoAnswer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView;
        if((savedInstanceState != null && savedInstanceState.getBoolean(AccountEditionActivity.ACCOUNT_IS_RING_KEY))
                || getArguments().getBoolean(AccountEditionActivity.ACCOUNT_IS_RING_KEY)) {
            inflatedView = inflater.inflate(R.layout.frag_ring_account, container, false);
            mIsRing = true;
        } else {
            inflatedView = inflater.inflate(R.layout.frag_general_account, container, false);
            mIsRing = false;
        }

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        if(getArguments() != null && getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY) != null) {
            mAccountID = getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY);
        }

        return inflatedView;
    }

    @Override
    protected void initPresenter(GeneralAccountPresenter presenter) {
        super.initPresenter(presenter);
        presenter.setAccountId(mAccountID);
        presenter.setIsRing(mIsRing);
        presenter.loadPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.loadPreferences();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AccountEditionActivity.ACCOUNT_IS_RING_KEY, mIsRing);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        if (preference instanceof EditTextIntegerPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_NUMBER);
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else if (preference instanceof PasswordPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }



    private void setPreferenceListener(AccountConfig details, Preference.OnPreferenceChangeListener listener) {
        for (ConfigKey confKey : details.getKeys()) {
            Preference pref = findPreference(confKey.key());
            if (pref != null) {
                pref.setOnPreferenceChangeListener(listener);
            }
        }
    }

    private final Preference.OnPreferenceChangeListener changeAccountStatusListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mAccountService.getAccount(mAccountID);
            if (account != null) {
                account.setEnabled((Boolean) newValue);
                mAccountService.setCredentials(mAccountID, account.getCredentialsHashMapList());
                mAccountService.setAccountDetails(mAccountID, account.getDetails());
            }
            return false;
        }
    };

    private final Preference.OnPreferenceChangeListener changeBasicPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.i(TAG, "Changing preference " + preference.getKey() + " to value:" + newValue);
            final Account account = mAccountService.getAccount(mAccountID);
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            if (preference instanceof TwoStatePreference) {
                account.setDetail(key, newValue.toString());
            } else {
                if (preference instanceof PasswordPreference) {
                    String tmp = "";
                    for (int i = 0; i < ((String) newValue).length(); ++i) {
                        tmp += "*";
                    }
                    if (account.isSip())
                        account.getCredentials().get(0).setDetail(key, newValue.toString());
                    preference.setSummary(tmp);
                } else if (key == ConfigKey.ACCOUNT_USERNAME) {
                    if (account.isSip()) {
                        account.getCredentials().get(0).setDetail(key, newValue.toString());
                    }
                    preference.setSummary((CharSequence) newValue);
                } else {
                    preference.setSummary((CharSequence) newValue);
                }

                account.setDetail(key, newValue.toString());
            }

            mAccountService.setCredentials(mAccountID, account.getCredentialsHashMapList());
            mAccountService.setAccountDetails(mAccountID, account.getDetails());
            return true;
        }
    };
}
