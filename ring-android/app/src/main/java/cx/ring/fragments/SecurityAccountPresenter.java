/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments;

import java.util.List;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.AccountCredentials;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;

public class SecurityAccountPresenter extends RootPresenter<SecurityAccountView> implements Observer<ServiceEvent> {

    protected AccountService mAccountService;

    private Account mAccount;

    @Inject
    public SecurityAccountPresenter(AccountService accountService) {
        this.mAccountService = accountService;
    }

    public void init(String accountId) {
        mAccount = mAccountService.getAccount(accountId);
        if (mAccount != null) {
            getView().removeAllCredentials();
            getView().addAllCredentials(mAccount.getCredentials());

            List<String> methods = mAccountService.getTlsSupportedMethods();
            String[] tlsMethods = methods.toArray(new String[methods.size()]);

            getView().setDetails(mAccount.getConfig(), tlsMethods);
        }
    }

    public void credentialEdited(Tuple<AccountCredentials, AccountCredentials> result) {
        mAccount.removeCredential(result.first);
        if (result.second != null) {
            // There is a new value for this credentials it means it has been edited (otherwise deleted)
            mAccount.addCredential(result.second);
        }

        mAccountService.setCredentials(mAccount.getAccountID(), mAccount.getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());

        getView().removeAllCredentials();
        getView().addAllCredentials(mAccount.getCredentials());
    }

    public void credentialAdded(Tuple<AccountCredentials, AccountCredentials> result) {
        mAccount.addCredential(result.second);

        mAccountService.setCredentials(mAccount.getAccountID(), mAccount.getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());

        getView().removeAllCredentials();
        getView().addAllCredentials(mAccount.getCredentials());
    }

    public void tlsChanged(ConfigKey key, Object newValue) {
        if (newValue instanceof Boolean) {
            mAccount.setDetail(key, (Boolean) newValue);
        } else {
            mAccount.setDetail(key, (String) newValue);
        }

        mAccountService.setCredentials(mAccount.getAccountID(), mAccount.getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());
    }

    public void fileActivityResult(ConfigKey key, String filePath) {
        mAccount.setDetail(key, filePath);
        mAccountService.setCredentials(mAccount.getAccountID(), mAccount.getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null || getView() == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case REGISTRATION_STATE_CHANGED:
                getView().removeAllCredentials();
                getView().addAllCredentials(mAccount.getCredentials());
                break;
            default:
                break;
        }
    }
}
