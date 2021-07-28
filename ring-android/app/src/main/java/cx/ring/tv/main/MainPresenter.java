/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.facades.ConversationFacade;
import net.jami.mvp.RootPresenter;
import net.jami.navigation.HomeNavigationViewModel;
import net.jami.services.AccountService;
import net.jami.smartlist.SmartListViewModel;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;

public class MainPresenter extends RootPresenter<MainView> {

    private static final String TAG = MainPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ConversationFacade mConversationFacade;
    private final Scheduler mUiScheduler;

    @Inject
    public MainPresenter(AccountService accountService,
                         ConversationFacade conversationFacade,
                         @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mConversationFacade = conversationFacade;
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
                .throttleLatest(150, TimeUnit.MILLISECONDS, mUiScheduler)
                .observeOn(mUiScheduler)
                .subscribe(viewModels -> {
                    final MainView view = getView();
                    view.showLoading(false);
                    view.showContacts(viewModels);
                }, e -> Log.w(TAG, "showConversations error ", e)));

        mCompositeDisposable.add(mConversationFacade.getPendingList()
                .switchMap(viewModels -> viewModels.isEmpty() ? SmartListViewModel.EMPTY_RESULTS
                        : Observable.combineLatest(viewModels, obs -> {
                    List<SmartListViewModel> vms = new ArrayList<>(obs.length);
                    for (Object ob : obs)
                        vms.add((SmartListViewModel) ob);
                    return vms;
                }))
                .throttleLatest(150, TimeUnit.MILLISECONDS, mUiScheduler)
                .observeOn(mUiScheduler)
                .subscribe(viewModels -> {
                    final MainView view = getView();
                    view.showContactRequests(viewModels);
                }, e -> Log.w(TAG, "showConversations error ", e)));
    }

    public void reloadAccountInfos() {
        mCompositeDisposable.add(mAccountService.getCurrentAccountSubject()
                .observeOn(mUiScheduler)
                .subscribe(
                        account -> getView().displayAccountInfos(new HomeNavigationViewModel(account, null)),
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
        getView().showExportDialog(mAccountService.getCurrentAccount().getAccountID(), mAccountService.getCurrentAccount().hasPassword());
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