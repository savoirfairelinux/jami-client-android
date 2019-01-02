/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.mvp.RootPresenter;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.Log;
import io.reactivex.Scheduler;
import io.reactivex.subjects.BehaviorSubject;

public class ContactRequestsPresenter extends RootPresenter<ContactRequestsView> {

    static private final String TAG = ContactRequestsPresenter.class.getSimpleName();

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private final ConversationFacade mConversationFacade;
    private final BehaviorSubject<String> mAccount = BehaviorSubject.create();

    @Inject
    public ContactRequestsPresenter(ConversationFacade facade) {
        mConversationFacade = facade;
    }

    @Override
    public void bindView(ContactRequestsView view) {
        super.bindView(view);
        mCompositeDisposable.add(mAccount
                .distinctUntilChanged()
                .flatMapSingle(mConversationFacade::getAccountSubject)
                .switchMap(a -> a
                        .getPendingSubject()
                        .map(pending -> {
                            ArrayList<SmartListViewModel> viewmodel = new ArrayList<>(pending.size());
                            for (Conversation c : pending)
                                viewmodel.add(new SmartListViewModel(a.getAccountID(), c.getContact(), c.getContact().getPrimaryNumber(), c.getLastEvent()));
                            return viewmodel;
                        }))
                .observeOn(mUiScheduler)
                .subscribe(viewModels -> getView().updateView(viewModels),
                        e -> Log.d(TAG, "updateList subscribe onError", e)));
    }

    public void updateAccount(String accountId) {
        mAccount.onNext(accountId);
    }

    public void contactRequestClicked(String accountId, CallContact contactId) {
        getView().goToConversation(accountId, contactId.getPrimaryNumber());
    }
}
