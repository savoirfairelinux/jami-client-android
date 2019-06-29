/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.tv.model.TVListViewModel;
import io.reactivex.Scheduler;

public class TVContactRequestPresenter extends RootPresenter<TVContactRequestView> {

    private final AccountService mAccountService;
    private final ConversationFacade mConversationService;
    private final Scheduler mUiScheduler;

    @Inject
    public TVContactRequestPresenter(AccountService accountService,
                                     ConversationFacade conversationService,
                                     Scheduler uiScheduler) {
        mAccountService = accountService;
        mConversationService = conversationService;
        mUiScheduler = uiScheduler;
    }

    public void setContact(Uri uri) {
        mCompositeDisposable.add(mConversationService
                .getCurrentAccountSubject()
                .map(a -> new TVListViewModel(a.getAccountID(), a.getByUri(uri).getContact()))
                .observeOn(mUiScheduler)
                .subscribe(c -> getView().showRequest(c)));
    }

    public void acceptTrustRequest(Uri viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        mConversationService.acceptRequest(accountId, viewModel);
        getView().finishView();
    }

    public void refuseTrustRequest(Uri viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        mConversationService.discardRequest(accountId, viewModel);
        getView().finishView();
    }

    public void blockTrustRequest(Uri viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        mConversationService.discardRequest(accountId, viewModel);
        mAccountService.removeContact(accountId, viewModel.getRawRingId(), true);
        getView().finishView();
    }

}
