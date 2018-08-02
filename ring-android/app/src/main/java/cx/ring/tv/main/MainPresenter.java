/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.tv.main;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.RingError;
import cx.ring.mvp.RootPresenter;
import cx.ring.navigation.RingNavigationViewModel;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.HardwareService;
import cx.ring.services.PresenceService;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

public class MainPresenter extends RootPresenter<MainView> {

    private static final String TAG = MainPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;
    private final PresenceService mPresenceService;
    private final HardwareService mHardwareService;
    private List<TVListViewModel> mTvListViewModels;

    private final Scheduler mUiScheduler;

    private final Observable<Account> accountSubject;
    private final Observable<ArrayList<TVListViewModel>> conversationViews;

    @Inject
    public MainPresenter(AccountService accountService,
                         ContactService contactService,
                         ConversationFacade conversationFacade,
                         PresenceService presenceService,
                         HardwareService hardwareService,
                         @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mContactService = contactService;
        mPresenceService = presenceService;
        mHardwareService = hardwareService;
        mUiScheduler = uiScheduler;

        accountSubject = conversationFacade
                .getCurrentAccountSubject();

        conversationViews = accountSubject
                .switchMap(a -> a
                        .getConversationsSubject()
                        .map(conversations -> {
                            ArrayList<TVListViewModel> viewModel = new ArrayList<>(conversations.size());
                            for (Conversation c : conversations)
                                viewModel.add(modelToViewModel(c.getContact()));
                            return viewModel;
                        }))
                .observeOn(mUiScheduler);
    }

    @Override
    public void bindView(MainView view) {
        super.bindView(view);
        loadConversations();
    }

    private void refreshContact(CallContact buddy) {
        Log.w(TAG, "refreshContact() " + buddy.isOnline());

        for (int i = 0; i < mTvListViewModels.size(); i++) {
            TVListViewModel viewModel = mTvListViewModels.get(i);
            CallContact callContact = viewModel.getContact();
            if (callContact == buddy) {
                if (viewModel.isOnline() != callContact.isOnline()) {
                    viewModel.setOnline(callContact.isOnline());
                    getView().refreshContact(i, viewModel);
                }
                break;
            }
        }
    }

    private void loadConversations() {
        mCompositeDisposable.clear();
        getView().showLoading(true);

        mCompositeDisposable.add(conversationViews
                .subscribe(viewModels -> {
                    Log.w(TAG, "loadConversations() update");
                    final MainView view = getView();
                    view.showLoading(false);
                    mTvListViewModels = viewModels;
                    getView().showContacts(viewModels);
                }, e -> {
                    getView().showLoading(false);
                    Log.d(TAG, "loadConversations subscribe onError", e);
                }));

        Log.w(TAG, "loadConversations() subscribe");
        mCompositeDisposable.add(accountSubject
                .switchMap(a -> a
                        .getConversationSubject()
                        .map(c -> modelToViewModel(c.getContact())))
                .observeOn(mUiScheduler)
                .subscribe(vm -> {
                    Log.d(TAG, "getConversationSubject " + vm);
                    if (mTvListViewModels == null)
                        return;
                    for (int i=0; i<mTvListViewModels.size(); i++) {
                        if (mTvListViewModels.get(i).getContact() == vm.getContact()) {
                            getView().refreshContact(i, vm);
                            break;
                        }
                    }
                }));
        mCompositeDisposable.add(mPresenceService.getPresenceUpdates()
                .observeOn(mUiScheduler)
                .subscribe(this::refreshContact));

        Log.d(TAG, "getPendingSubject subscribe");
        mCompositeDisposable.add(accountSubject
                .switchMap(a -> a
                        .getPendingSubject()
                        .map(pending -> {
                            Log.d(TAG, "getPendingSubject " + pending.size());
                            ArrayList<TVListViewModel> viewmodel = new ArrayList<>(pending.size());
                            for (Conversation c : pending) {
                                mContactService.loadContactData(c.getContact());
                                viewmodel.add(modelToViewModel(c.getContact()));
                            }
                            return viewmodel;
                        }))
                .observeOn(mUiScheduler)
                .subscribe(viewModels -> getView().showContactRequests(viewModels),
                        e -> Log.d(TAG, "updateList subscribe onError", e)));

    }

    private TVListViewModel modelToViewModel(CallContact callContact) {
        mContactService.loadContactData(callContact);
        return new TVListViewModel(callContact);
    }

    public void contactClicked(TVListViewModel item) {
        if (!mHardwareService.isVideoAvailable() && !mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(RingError.NO_INPUT);
            return;
        }

        Account account = mAccountService.getCurrentAccount();
        String ringID = item.getContact().getPrimaryNumber();
        getView().callContact(account.getAccountID(), ringID);
    }

    public void reloadAccountInfos() {
        if (mAccountService == null) {
            Log.e(TAG, "reloadAccountInfos: No account service available");
            return;
        }
        mCompositeDisposable.add(mAccountService.getProfileAccountList()
                .observeOn(mUiScheduler)
                .subscribe(accounts -> {
                    Account account = accounts.isEmpty() ? null : accounts.get(0);
                    RingNavigationViewModel viewModel = new RingNavigationViewModel(account, accounts);
                    getView().displayAccountInfos(account == null ? null : account.getDisplayUri(), viewModel);
                }));
    }

    public void onExportClicked() {
        getView().showExportDialog(mAccountService.getCurrentAccount().getAccountID());
    }

    public void onLicenceClicked(int aboutType) {
        getView().showLicence(aboutType);
    }

    public void onEditProfileClicked() {
        getView().showProfileEditing();
    }

    public void onSettingsClicked() {
        getView().showSettings();
    }
}