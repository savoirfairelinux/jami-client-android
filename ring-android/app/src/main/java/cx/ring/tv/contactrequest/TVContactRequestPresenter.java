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

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import cx.ring.model.TrustRequest;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.PreferencesService;
import cx.ring.tv.model.TVContactRequestViewModel;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public class TVContactRequestPresenter extends RootPresenter<TVContactRequestView> {

    private AccountService mAccountService;
    private PreferencesService mPreferencesService;
    private DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    public TVContactRequestPresenter(AccountService accountService,
                                     PreferencesService sharedPreferencesService,
                                     DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mPreferencesService = sharedPreferencesService;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    public void acceptTrustRequest(TVContactRequestViewModel viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        String contactId = viewModel.getContactId();

        List<TrustRequest> trustRequests = mAccountService.getCurrentAccount().getRequests();
        for (Iterator<TrustRequest> it = trustRequests.iterator(); it.hasNext(); ) {
            TrustRequest request = it.next();
            if (accountId.equals(request.getAccountId()) && contactId.equals(request.getContactId())) {
                VCard vCard = request.getVCard();
                if (vCard != null) {
                    VCardUtils.savePeerProfileToDisk(vCard, contactId + ".vcf", mDeviceRuntimeService.provideFilesDir());
                }
                it.remove();
            }
        }

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
