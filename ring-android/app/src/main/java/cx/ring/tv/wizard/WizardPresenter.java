/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

package cx.ring.tv.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class WizardPresenter extends RootPresenter<WizardView> implements Observer<ServiceEvent> {

    public static final String TAG = WizardPresenter.class.getSimpleName();
    private static final String ACCOUNT_SIP_INFO = "sipinfo";

    private AccountService mAccountService;
    private DeviceRuntimeService mDeviceRuntimeService;

    private String mAccountType;
    private Account mAccount;

    @Inject
    public WizardPresenter(AccountService accountService, DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void bindView(WizardView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    public void createAccount() {
        String mDeviceName = android.os.Build.MODEL;
        Random r = new Random();
        mDeviceName = "androidtv-" + r.nextInt(1000) + mDeviceName.replace(' ','_').toLowerCase();

        Log.d(TAG, "account name : " + mDeviceName);
        initRingAccountCreation(mDeviceName, "123456");
    }

    public void initRingAccountCreation(String username, String password) {
        HashMap<String, String> accountDetails = initRingAccountDetails();
        if (accountDetails != null) {
            if (username != null && !username.isEmpty()) {
                accountDetails.put(ConfigKey.ACCOUNT_REGISTERED_NAME.key(), username);
            }
            if (password != null && !password.isEmpty()) {
                accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), password);
            }
            for (Map.Entry<String, String> e : accountDetails.entrySet()) {
                Log.d(TAG, "Default account detail: " + e.getKey() + " -> " + e.getValue());
            }
            createNewAccount(accountDetails);
        }
    }

    private HashMap<String, String> initRingAccountDetails() {
        HashMap<String, String> accountDetails = initAccountDetails();
        if (accountDetails != null) {
            accountDetails.put(ConfigKey.ACCOUNT_ALIAS.key(), mAccountService.getNewAccountName("Ring Account"));
            accountDetails.put(ConfigKey.ACCOUNT_HOSTNAME.key(), "bootstrap.ring.cx");
            accountDetails.put(ConfigKey.ACCOUNT_UPNP_ENABLE.key(), AccountConfig.TRUE_STR);
        }
        return accountDetails;
    }

    private HashMap<String, String> initAccountDetails() {
        if (mAccountType == null) {
            mAccountType = AccountConfig.ACCOUNT_TYPE_RING;
        }
        try {
            Log.d(TAG, "ici " + mAccountType);
            HashMap<String, String> accountDetails = (HashMap<String, String>) mAccountService.getAccountTemplate(mAccountType);

            boolean hasCameraPermission = mDeviceRuntimeService.hasVideoPermission();
            accountDetails.put(ConfigKey.VIDEO_ENABLED.key(), Boolean.toString(hasCameraPermission));

            //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
            //~ This will have to be removed when it will be supported.
            accountDetails.put(ConfigKey.ACCOUNT_DTMF_TYPE.key(), ACCOUNT_SIP_INFO);
            return accountDetails;
        } catch (Exception e) {
            Log.e(TAG, "Error creating account", e);
            return null;
        }
    }

    private void createNewAccount(HashMap<String, String>... accs) {
        if (mAccountType.equals(AccountConfig.ACCOUNT_TYPE_RING) || mAccount == null) {
            mAccount = mAccountService.addAccount(accs[0]);
        }
    }

    private void handleCreationState(final ServiceEvent event) {
        String accountID = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
        String newState = event.getEventInput(ServiceEvent.EventInput.STATE, String.class);

        if (accountID == null || newState == null || accountID.isEmpty() || newState.isEmpty()) {
            return;
        }

        mAccount = mAccountService.getAccount(accountID);
        if (mAccount.isRing() && (newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING))) {
            return;
        }

        switch (newState) {
            case AccountConfig.STATE_REGISTERED:
                getView().endCreation();
                break;
            default:
                getView().errorCreation(newState);
                break;
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNT_ADDED:
            case REGISTRATION_STATE_CHANGED:
                handleCreationState(event);
                break;
            default:
                Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }
}
