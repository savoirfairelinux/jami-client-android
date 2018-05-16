/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

package cx.ring.contactrequests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Conversation;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class ContactRequestsPresenter extends RootPresenter<ContactRequestsView> {

    static private final String TAG = ContactRequestsPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ConversationFacade mConversationFacade;
    private final NotificationService mNotificationService;

    private String mAccountID;
    private List<ContactRequestsViewModel> mContactRequestsViewModels;

    private CompositeDisposable mConversationDisposable;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public ContactRequestsPresenter(AccountService accountService,
                                    ConversationFacade conversationFacade,
                                    NotificationService notificationService) {
        mAccountService = accountService;
        mConversationFacade = conversationFacade;
        mNotificationService = notificationService;
    }

    final private List<TrustRequest> mTrustRequests = new ArrayList<>();

    @Override
    public void bindView(ContactRequestsView view) {
        super.bindView(view);
        mConversationDisposable = new CompositeDisposable();
        mCompositeDisposable.add(mConversationDisposable);
        updateList();
    }

    public void updateAccount(String accountId, boolean shouldUpdateList) {
        mAccountID = accountId;
        if (shouldUpdateList) {
            updateList();
        }
    }

    @Override
    public void unbindView() {
        super.unbindView();
    }

    public void updateList() {
        if (getView() == null) {
            return;
        }
        Log.d(TAG, "updateList");
        mConversationDisposable.clear();
        mConversationDisposable.add(
                mConversationFacade.getConversationsSubject()
                        .map(conversations -> {
                            Log.d(TAG, "updateList map " + conversations.pending.size());
                            mNotificationService.cancelTrustRequestNotification(conversations.account.getAccountID());
                            ArrayList<ContactRequestsViewModel> viewmodel = new ArrayList<>(conversations.pending.size());
                            for (Conversation c : conversations.pending.values())
                                viewmodel.add(new ContactRequestsViewModel(c.getContact()));
                            Collections.sort(viewmodel, (a, b) -> a.getAccountUsername().compareTo(b.getAccountUsername()));
                            return viewmodel;
                        })
                        .subscribeOn(Schedulers.computation())
                        .observeOn(mUiScheduler)
                        .subscribeWith(new DisposableObserver<List<ContactRequestsViewModel>>() {
                            @Override
                            public void onNext(List<ContactRequestsViewModel> smartListViewModels) {
                                Log.d(TAG, "updateList subscribe onSuccess");
                                mContactRequestsViewModels = smartListViewModels;
                                getView().updateView(mContactRequestsViewModels);
                            }
                            @Override
                            public void onError(Throwable e) {
                                //getView().setLoading(false);
                                Log.d(TAG, "updateList subscribe onError");
                            }
                            @Override
                            public void onComplete() {}
                        }));
    }

    public void contactRequestClicked(String contactId) {
        String rawUriString = new Uri(contactId).getRawUriString();
        getView().goToConversation(mAccountService.getCurrentAccount().getAccountID(), rawUriString);
    }
}
