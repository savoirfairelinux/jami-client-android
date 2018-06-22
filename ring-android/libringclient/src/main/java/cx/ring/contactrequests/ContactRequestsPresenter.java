/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.contactrequests;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.Conversation;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.utils.Log;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;

public class ContactRequestsPresenter extends RootPresenter<ContactRequestsView> {

    static private final String TAG = ContactRequestsPresenter.class.getSimpleName();

    private String mAccountId;
    private final ConversationFacade mConversationFacade;
    private CompositeDisposable mConversationDisposable;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public ContactRequestsPresenter(ConversationFacade conversationFacade) {
        mConversationFacade = conversationFacade;
    }

    @Override
    public void bindView(ContactRequestsView view) {
        super.bindView(view);
        mConversationDisposable = new CompositeDisposable();
        mCompositeDisposable.add(mConversationDisposable);
    }

    public void updateAccount(String accountId) {
        mAccountId = accountId;
        mConversationDisposable.clear();
        mConversationDisposable.add(
                mConversationFacade.getAccountSubject(accountId)
                        .flatMapObservable(Account::getPendingSubject)
                        .map(pending -> {
                            ArrayList<ContactRequestsViewModel> viewmodel = new ArrayList<>(pending.size());
                            for (Conversation c : pending)
                                viewmodel.add(new ContactRequestsViewModel(c.getContact()));
                            return viewmodel;
                        })
                        .observeOn(mUiScheduler)
                        .subscribe(smartListViewModels -> {
                                Log.d(TAG, "updateList subscribe onSuccess");
                                getView().updateView(smartListViewModels);
                            }, e -> {
                                //getView().setLoading(false);
                                Log.d(TAG, "updateList subscribe onError", e);
                            }));
    }

    public void contactRequestClicked(String contactId) {
        String rawUriString = new Uri(contactId).getRawUriString();
        getView().goToConversation(mAccountId, rawUriString);
    }
}
