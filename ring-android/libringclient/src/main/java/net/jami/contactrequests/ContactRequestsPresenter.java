/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.contactrequests;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.facades.ConversationFacade;
import net.jami.model.Account;
import net.jami.model.Uri;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.smartlist.SmartListViewModel;
import net.jami.utils.Log;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class ContactRequestsPresenter extends RootPresenter<net.jami.contactrequests.ContactRequestsView> {

    static private final String TAG = ContactRequestsPresenter.class.getSimpleName();

    private final Scheduler mUiScheduler;
    private final AccountService mAccountService;
    private final ConversationFacade mConversationFacade;
    private final BehaviorSubject<Account> mAccount = BehaviorSubject.create();

    @Inject
    ContactRequestsPresenter(ConversationFacade conversationFacade, AccountService accountService, @Named("UiScheduler") Scheduler scheduler) {
        mConversationFacade = conversationFacade;
        mAccountService = accountService;
        mUiScheduler = scheduler;
    }

    @Override
    public void bindView(ContactRequestsView view) {
        super.bindView(view);
        mCompositeDisposable.add(mConversationFacade.getPendingList(mAccount)
                .switchMap(viewModels -> viewModels.isEmpty() ? SmartListViewModel.EMPTY_RESULTS
                        : Observable.combineLatest(viewModels, obs -> {
                    List<SmartListViewModel> vms = new ArrayList<>(obs.length);
                    for (Object ob : obs)
                        vms.add((SmartListViewModel) ob);
                    return vms;
                }))
                .observeOn(mUiScheduler)
                .subscribe(viewModels -> getView().updateView(viewModels, mCompositeDisposable),
                        e -> Log.d(TAG, "updateList subscribe onError", e)));
    }

    @Override
    public void onDestroy() {
        mAccount.onComplete();
        super.onDestroy();
    }

    public void updateAccount(String accountId) {
        if (accountId == null) {
            mAccountService.getCurrentAccountSubject().subscribe(mAccount);
        } else {
            mAccount.onNext(mAccountService.getAccount(accountId));
        }
    }

    public void contactRequestClicked(String accountId, Uri uri) {
        getView().goToConversation(accountId, uri);
    }
}
