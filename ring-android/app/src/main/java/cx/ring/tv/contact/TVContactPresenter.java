/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
package cx.ring.tv.contact;

import javax.inject.Inject;

import cx.ring.daemon.Blob;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.VCardService;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.VCardUtils;
import io.reactivex.Scheduler;

public class TVContactPresenter extends RootPresenter<TVContactView> {

    private final AccountService mAccountService;
    private final ConversationFacade mConversationService;
    private final Scheduler mUiScheduler;
    private final VCardService mVCardService;

    private String mAccountId;
    private Uri mUri;

    @Inject
    public TVContactPresenter(AccountService accountService,
                              ConversationFacade conversationService,
                              Scheduler uiScheduler,
                              VCardService vCardService) {
        mAccountService = accountService;
        mConversationService = conversationService;
        mUiScheduler = uiScheduler;
        mVCardService = vCardService;
    }

    public void setContact(ConversationPath path) {
        mAccountId = path.getAccountId();
        mUri = new Uri(path.getContactId());
        mCompositeDisposable.clear();
        mCompositeDisposable.add(mConversationService
                .getAccountSubject(path.getAccountId())
                .map(a -> new TVListViewModel(a.getAccountID(), a.getByUri(mUri).getContact()))
                .observeOn(mUiScheduler)
                .subscribe(c -> getView().showContact(c)));
    }

    public void removeContact() {
        mConversationService.removeConversation(mAccountId, mUri).subscribe();
        getView().finishView();
    }

    public void contactClicked() {
        Account account = mAccountService.getAccount(mAccountId);
        if (account != null) {
            Conference conf = account.getByUri(mUri).getCurrentCall();
            if (conf != null
                    && !conf.getParticipants().isEmpty()
                    && conf.getParticipants().get(0).getCallStatus() != SipCall.CallStatus.INACTIVE
                    && conf.getParticipants().get(0).getCallStatus() != SipCall.CallStatus.FAILURE) {
                getView().goToCallActivity(conf.getId());
            } else {
                getView().callContact(mAccountId, mUri);
            }
        }
    }

    public void clearHistory() {
        mConversationService.clearHistory(mAccountId, mUri).subscribe();
    }

    public void onAddContact(Uri viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        sendTrustRequest(accountId, viewModel.getRawRingId());
        getView().switchToConversationView();
    }

    private void sendTrustRequest(String accountId, String ringId) {
        mVCardService.loadSmallVCard(accountId, VCardService.MAX_SIZE_REQUEST)
                .subscribe(vCard -> {
                    mAccountService.sendTrustRequest(accountId,
                            ringId,
                            Blob.fromString(VCardUtils.vcardToString(vCard)));

                });
    }

    public void acceptTrustRequest(Uri viewModel) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        mConversationService.acceptRequest(accountId, viewModel);
        getView().switchToConversationView();
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
