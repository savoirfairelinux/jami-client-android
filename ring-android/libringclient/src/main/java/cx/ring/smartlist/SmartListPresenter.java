/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package cx.ring.smartlist;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class SmartListPresenter extends RootPresenter<SmartListView> {

    private static final String TAG = SmartListPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;
    private final ConversationFacade mConversationFacade;

    private Account mAccount;
    private final Subject<String> mCurrentQuery = BehaviorSubject.createDefault("");
    private final Subject<String> mQuery = PublishSubject.create();
    private final Observable<String> mDebouncedQuery = mQuery.debounce(350, TimeUnit.MILLISECONDS);

    private final Observable<Account> accountSubject;

    private final Scheduler mUiScheduler;

    private final CompositeDisposable mConversationDisposable = new CompositeDisposable();
    private Disposable mQueryDisposable = null;

    @Inject
    public SmartListPresenter(AccountService accountService,
                              ContactService contactService,
                              ConversationFacade conversationFacade,
                              @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mContactService = contactService;
        mConversationFacade = conversationFacade;
        mUiScheduler = uiScheduler;

        accountSubject = mConversationFacade
                .getCurrentAccountSubject()
                .doOnNext(a -> mAccount = a);
    }

    @Override
    public void bindView(SmartListView view) {
        super.bindView(view);
        mCompositeDisposable.clear();
        mCompositeDisposable.add(mConversationDisposable);
        loadConversations();
    }

    public void queryTextChanged(String query) {
        if (query.isEmpty()) {
            if (mQueryDisposable != null)  {
                mQueryDisposable.dispose();
                mQueryDisposable = null;
            }
            mCurrentQuery.onNext(query);
        } else {
            if (mQueryDisposable == null)  {
                mQueryDisposable = mDebouncedQuery.subscribe(mCurrentQuery::onNext);
            }
            mQuery.onNext(query);
        }
    }

    public void conversationClicked(SmartListViewModel viewModel) {
        startConversation(viewModel.getAccountId(), viewModel.getContact());
    }

    public void conversationLongClicked(SmartListViewModel smartListViewModel) {
        getView().displayConversationDialog(smartListViewModel);
    }

    public String getAccountID() {
        return mAccount.getAccountID();
    }

    public void fabButtonClicked() {
        getView().displayMenuItem();
    }

    private void startConversation(String accountId, CallContact c) {
        SmartListView view = getView();
        if (view != null && c != null) {
            view.goToConversation(accountId, c.getPrimaryUri());
        }
    }

    public void startConversation(Uri uri) {
        getView().goToConversation(mAccount.getAccountID(), uri);
    }

    public void copyNumber(SmartListViewModel smartListViewModel) {
        getView().copyNumber(smartListViewModel.getContact());
    }

    public void clearConversation(SmartListViewModel smartListViewModel) {
        getView().displayClearDialog(smartListViewModel.getContact());
    }

    public void clearConversation(final CallContact callContact) {
        mConversationDisposable.add(mConversationFacade
                .clearHistory(mAccount.getAccountID(), callContact.getPrimaryUri())
                .subscribeOn(Schedulers.computation()).subscribe());
    }

    public void removeConversation(SmartListViewModel smartListViewModel) {
        getView().displayDeleteDialog(smartListViewModel.getContact());
    }

    public void removeConversation(CallContact callContact) {
        mConversationDisposable.add(mConversationFacade
                .removeConversation(mAccount.getAccountID(), callContact.getPrimaryUri())
                .subscribeOn(Schedulers.computation()).subscribe());
    }

    public void clickQRSearch() {
        getView().goToQRFragment();
    }

    void showConversations(Observable<List<Observable<SmartListViewModel>>> conversations) {
        mConversationDisposable.clear();
        getView().setLoading(true);

        mConversationDisposable.add(conversations
                .switchMap(viewModels -> viewModels.isEmpty() ? SmartListViewModel.EMPTY_RESULTS
                        : Observable.combineLatest(viewModels, obs -> {
                            List<SmartListViewModel> vms = new ArrayList<>(obs.length);
                            for (Object ob : obs)
                                vms.add((SmartListViewModel) ob);
                            return vms;
                        }))
                //.throttleLatest(150, TimeUnit.MILLISECONDS, mUiScheduler)
                .observeOn(mUiScheduler)
                .subscribe(viewModels -> {
                    final SmartListView view = getView();
                    view.setLoading(false);
                    if (viewModels.isEmpty()) {
                        view.hideList();
                        view.displayNoConversationMessage();
                        return;
                    }
                    view.hideNoConversationMessage();
                    view.updateList(viewModels);
                }, e -> Log.w(TAG, "showConversations error ", e)));
    }

    private void loadConversations() {
        showConversations(mConversationFacade.getFullList(accountSubject, mCurrentQuery, true));
    }

    public void banContact(SmartListViewModel smartListViewModel) {
        CallContact contact = smartListViewModel.getContact();
        mAccountService.removeContact(mAccount.getAccountID(), contact.getPrimaryNumber(), true);
    }
}
