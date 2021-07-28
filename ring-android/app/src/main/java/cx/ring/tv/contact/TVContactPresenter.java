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
import javax.inject.Named;

import net.jami.daemon.Blob;
import net.jami.facades.ConversationFacade;
import net.jami.model.Account;
import net.jami.model.Conference;
import net.jami.model.Conversation;
import net.jami.model.Call;
import net.jami.model.Uri;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.services.VCardService;
import net.jami.smartlist.SmartListViewModel;
import cx.ring.utils.ConversationPath;
import io.reactivex.rxjava3.core.Scheduler;

import net.jami.utils.VCardUtils;

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
                              @Named("UiScheduler") Scheduler uiScheduler,
                              VCardService vCardService) {
        mAccountService = accountService;
        mConversationService = conversationService;
        mUiScheduler = uiScheduler;
        mVCardService = vCardService;
    }

    public void setContact(ConversationPath path) {
        mAccountId = path.getAccountId();
        mUri = path.getConversationUri();
        mCompositeDisposable.clear();
        mCompositeDisposable.add(mConversationService
                .getAccountSubject(path.getAccountId())
                .map(a -> new SmartListViewModel(a.getByUri(mUri), true))
                .observeOn(mUiScheduler)
                .subscribe(c -> getView().showContact(c)));
    }

    public void contactClicked() {
        Account account = mAccountService.getAccount(mAccountId);
        if (account != null) {
            Conversation conversation = account.getByUri(mUri);
            Conference conf = conversation.getCurrentCall();
            if (conf != null
                    && !conf.getParticipants().isEmpty()
                    && conf.getParticipants().get(0).getCallStatus() != Call.CallStatus.INACTIVE
                    && conf.getParticipants().get(0).getCallStatus() != Call.CallStatus.FAILURE) {
                getView().goToCallActivity(conf.getId());
            } else {
                if (conversation.isSwarm()) {
                    getView().callContact(mAccountId, mUri, conversation.getContact().getUri());
                } else {
                    getView().callContact(mAccountId, mUri, mUri);
                }
            }
        }
    }

    public void onAddContact() {
        sendTrustRequest(mAccountId, mUri);
        getView().switchToConversationView();
    }

    private void sendTrustRequest(String accountId, Uri conversationUri) {
        Conversation conversation = mAccountService.getAccount(accountId).getByUri(conversationUri);
        mVCardService.loadSmallVCardWithDefault(accountId, VCardService.MAX_SIZE_REQUEST)
                .subscribe(vCard -> mAccountService.sendTrustRequest(conversation, conversationUri, Blob.fromString(VCardUtils.vcardToString(vCard))),
                        e -> mAccountService.sendTrustRequest(conversation, conversationUri, null));
    }

    public void acceptTrustRequest() {
        mConversationService.acceptRequest(mAccountId, mUri);
        getView().switchToConversationView();
    }

    public void refuseTrustRequest() {
        mConversationService.discardRequest(mAccountId, mUri);
        getView().finishView();
    }

    public void blockTrustRequest() {
        mConversationService.discardRequest(mAccountId, mUri);
        mAccountService.removeContact(mAccountId, mUri.getRawRingId(), true);
        getView().finishView();
    }


}
