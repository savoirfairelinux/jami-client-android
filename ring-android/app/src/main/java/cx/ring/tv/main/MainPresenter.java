/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.Conversation;
import cx.ring.model.Error;
import cx.ring.mvp.RootPresenter;
import cx.ring.navigation.HomeNavigationViewModel;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.HardwareService;
import cx.ring.smartlist.SmartListViewModel;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

public class MainPresenter extends RootPresenter<MainView> {

    private static final String TAG = MainPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;
    private final ConversationFacade mConversationFacade;
    private final HardwareService mHardwareService;
    private final Scheduler mUiScheduler;

    @Inject
    public MainPresenter(AccountService accountService,
                         ContactService contactService,
                         ConversationFacade conversationFacade,
                         HardwareService hardwareService,
                         @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mContactService = contactService;
        mConversationFacade = conversationFacade;
        mHardwareService = hardwareService;
        mUiScheduler = uiScheduler;
    }

    @Override
    public void bindView(MainView view) {
        super.bindView(view);
        loadConversations();
        reloadAccountInfos();
    }

    private void loadConversations() {
        getView().showLoading(true);

        mCompositeDisposable.add(mConversationFacade.getSmartList(true)
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
                    final MainView view = getView();
                    view.showLoading(false);
                    view.showContacts(viewModels);
                }, e -> Log.w(TAG, "showConversations error ", e)));

        Log.d(TAG, "getPendingSubject subscribe");
        mCompositeDisposable.add(mConversationFacade.getCurrentAccountSubject()
                .switchMap(a -> a.getPendingSubject()
                        .map(pending -> {
                            Log.d(TAG, "getPendingSubject " + pending.size());
                            ArrayList<SmartListViewModel> viewmodel = new ArrayList<>(pending.size());
                            for (Conversation c : pending) {
                                mContactService.loadContactData(c.getContact(), c.getAccountId()).subscribe(() -> {}, e -> Log.e(TAG, "Can't load contact data"));
                                viewmodel.add(new SmartListViewModel(c.getAccountId(), c.getContact(), null));
                            }
                            return viewmodel;
                        }))
                .observeOn(mUiScheduler)
                .subscribe(viewModels -> getView().showContactRequests(viewModels),
                        e -> Log.d(TAG, "updateList getPendingSubject onError", e)));

    }

    public void contactClicked(SmartListViewModel item) {
        if (!mHardwareService.isVideoAvailable() && !mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(Error.NO_INPUT);
            return;
        }

        Account account = mAccountService.getAccount(item.getAccountId());
        getView().callContact(account.getAccountID(), item.getUri().getRawUriString());
    }

    public void reloadAccountInfos() {
        mCompositeDisposable.add(mAccountService.getProfileAccountList()
                .observeOn(mUiScheduler)
                .subscribe(
                        accounts -> getView().displayAccountInfos(
                                new HomeNavigationViewModel(accounts.isEmpty() ? null : accounts.get(0), null)),
                        e-> Log.d(TAG, "reloadAccountInfos getProfileAccountList onError", e)));
        mCompositeDisposable.add(mAccountService.getObservableAccounts()
                .observeOn(mUiScheduler)
                .subscribe(account -> {
                    MainView v = getView();
                    if (v != null)
                        v.updateModel(account);
                }, e ->  Log.e(TAG, "Error loading account list !", e)));
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

    public void onShareAccountClicked() {
        getView().showAccountShare();
    }

    public void onSettingsClicked() {
        getView().showSettings();
    }
}