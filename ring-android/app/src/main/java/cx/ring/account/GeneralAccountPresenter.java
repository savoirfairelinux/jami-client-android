package cx.ring.account;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.model.Preferences;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.PreferencesService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.views.PasswordPreference;

public class GeneralAccountPresenter  extends RootPresenter<GeneralAccountView> implements Observer<ServiceEvent>{

    private AccountService mAccountService;
    private PreferencesService mPreferenceService;
    private String mAccountID;
    private boolean isRing;
    private Preferences mPreferences;

    @Inject
    public GeneralAccountPresenter(AccountService accountService, PreferencesService preferencesService) {
        mAccountService = accountService;
        mPreferenceService = preferencesService;
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void bindView(GeneralAccountView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    public void loadPreferences() {
        if(isRing) {
            mPreferences = mPreferenceService.loadRingPreferences(mAccountID);
        } else {
            mPreferences = mPreferenceService.loadGeneralPreferences(mAccountID);
        }
    }

    public void setAccountId(String accountId) {
        mAccountID = accountId;
    }

    public void setIsRing(boolean isRing) {
        this.isRing = isRing;
    }

    void accountChanged(Account account) {
        if (account == null) {
            return;
        }
        this.isRing = account.isRing();
        mAccountID = account.getAccountID();

        Preferences prefs;
        if(isRing) {
            prefs = mPreferenceService.loadRingPreferences(mAccountID);
        } else {
            prefs = mPreferenceService.loadGeneralPreferences(mAccountID);
        }


        setPreferenceDetails(account.getConfig());

        if (!isRing) {
            String status;
            pref.setTitle(account.getAlias());
            if (account.isEnabled()) {
                if (account.isTrying()) {
                    status = getString(R.string.account_status_connecting);
                } else if (account.needsMigration()) {
                    status = getString(R.string.account_update_needed);
                } else if (account.isInError()) {
                    status = getString(R.string.account_status_connection_error);
                } else if (account.isRegistered()) {
                    status = getString(R.string.account_status_online);
                } else {
                    status = getString(R.string.account_status_unknown);
                }
            } else {
                status = getString(R.string.account_status_offline);
            }
            pref.setSummary(status);
            pref.setChecked(account.isEnabled());

            // An ip2ip account is always ready
            pref.setEnabled(!account.isIP2IP());

            pref.setOnPreferenceChangeListener(changeAccountStatusListener);
        }

        setPreferenceListener(account.getConfig(), changeBasicPreferenceListener);
        getView().updateView();
    }

    private void setPreferenceDetails(AccountConfig details) {
        for (ConfigKey confKey : details.getKeys()) {
            Preference pref = findPreference(confKey.key());
            if (pref == null) {
                continue;
            }
            if (!confKey.isTwoState()) {
                String val = details.get(confKey);
                ((EditTextPreference) pref).setText(val);
                if (pref instanceof PasswordPreference) {
                    String tmp = "";
                    for (int i = 0; i < val.length(); ++i) {
                        tmp += "*";
                    }
                    pref.setSummary(tmp);
                } else {
                    pref.setSummary(val);
                }
            } else {
                ((TwoStatePreference) pref).setChecked(details.getBool(confKey));
            }
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null || getView() == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case REGISTRATION_STATE_CHANGED:
                accountChanged(mAccountService.getAccount(mAccountID));
                break;
            default:
                break;
        }
    }
}
