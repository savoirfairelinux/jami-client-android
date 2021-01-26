/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.subjects.BehaviorSubject;

public class ContactRequestsPresenter extends RootPresenter<ContactRequestsView> {

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
                .subscribe(viewModels -> getView().updateView(viewModels),
                        e -> Log.d(TAG, "updateList subscribe onError", e)));
    }

    @Override
    public void unbindView() {
        mAccount.onComplete();
        super.unbindView();
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
