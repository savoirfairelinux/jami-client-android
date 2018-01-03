/*
 *  Copyright (C) 2018 Savoir-faire Linux Inc.
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
package cx.ring.tv.contactrequest;

import javax.inject.Inject;

import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.PreferencesService;
import cx.ring.tv.model.TVContactRequestViewModel;

public class TVContactRequestPresenter extends RootPresenter<TVContactRequestView> {

    private AccountService mAccountService;
    private PreferencesService mPreferencesService;

    @Inject
    public TVContactRequestPresenter(AccountService accountService,
                                     PreferencesService sharedPreferencesService) {
        mAccountService = accountService;
        mPreferencesService = sharedPreferencesService;
    }

    public void acceptTrustRequest(TVContactRequestViewModel viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        String contactId = viewModel.getContactId();
        mAccountService.acceptTrustRequest(accountId, contactId);
        mPreferencesService.removeRequestPreferences(accountId, contactId);
        getView().finishView();
    }

    public void refuseTrustRequest(TVContactRequestViewModel viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        String contactId = viewModel.getContactId();
        mAccountService.discardTrustRequest(accountId, contactId);
        mPreferencesService.removeRequestPreferences(accountId, contactId);
        getView().finishView();

    }

    public void blockTrustRequest(TVContactRequestViewModel viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        String contactId = viewModel.getContactId();
        mAccountService.discardTrustRequest(accountId, contactId);
        mAccountService.removeContact(accountId, contactId, true);
        mPreferencesService.removeRequestPreferences(accountId, contactId);
        getView().finishView();
    }

}
