/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.RingError;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.navigation.RingNavigationViewModel;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.HardwareService;
import cx.ring.services.PresenceService;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;


public class MainPresenter extends RootPresenter<MainView> implements Observer<ServiceEvent> {

    private static final String TAG = MainPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private ConversationFacade mConversationFacade;
    private ContactService mContactService;
    private PresenceService mPresenceService;
    private HardwareService mHardwareService;
    private Scheduler mMainScheduler;

    private ArrayList<TVListViewModel> mTvListViewModels;

    @Inject
    public MainPresenter(AccountService accountService,
                         ContactService contactService,
                         PresenceService presenceService,
                         HardwareService hardwareService,
                         ConversationFacade conversationfacade,
                         Scheduler mainScheduler) {
        mAccountService = accountService;
        mContactService = contactService;
        mPresenceService = presenceService;
        mConversationFacade = conversationfacade;
        mHardwareService = hardwareService;
        this.mMainScheduler = mainScheduler;
    }

    @Override
    public void bindView(MainView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
        mContactService.addObserver(this);
        mPresenceService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mConversationFacade.removeObserver(this);
        mContactService.removeObserver(this);
        mPresenceService.removeObserver(this);
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case CONVERSATIONS_CHANGED:
            case ACCOUNTS_CHANGED:
            case NAME_REGISTRATION_ENDED:
                reloadConversations();
                reloadAccountInfos();
                break;
        }
        if (observable instanceof PresenceService) {
            switch (event.getEventType()) {
                case NEW_BUDDY_NOTIFICATION:
                    refreshContact(
                            event.getString(ServiceEvent.EventInput.BUDDY_URI));
                    break;
            }
        }
    }

    private void refreshContact(String buddy) {
        for (TVListViewModel tvListViewModel : mTvListViewModels) {
            CallContact callContact = tvListViewModel.getCallContact();
            if (callContact.getIds().get(0).equals("ring:" + buddy)) {
                TVListViewModel smartListViewModel = new TVListViewModel(
                        callContact,
                        mPresenceService.isBuddyOnline(callContact.getIds().get(0)));
                getView().refreshContact(smartListViewModel);
            }
        }
    }

    public void reloadConversations() {
        getView().showLoading(true);

        Account currentAccount = mAccountService.getCurrentAccount();
        if (currentAccount == null) {
            android.util.Log.e(TAG, "reloadConversations: Not able to get currentAccount");
            return;
        }

        final Collection<CallContact> contacts = currentAccount.getContacts().values();

        //Get all non-ban contact and then get last message and last call to create a smartList entry
        mCompositeDisposable.add(io.reactivex.Observable.fromIterable(contacts)
                .subscribeOn(Schedulers.computation())
                .observeOn(mMainScheduler)
                .filter(new Predicate<CallContact>() {
                    @Override
                    public boolean test(CallContact callContact) throws Exception {
                        return !callContact.isBanned();
                    }
                })
                .map(new Function<CallContact, TVListViewModel>() {
                    @Override
                    public TVListViewModel apply(CallContact callContact) throws Exception {
                        return modelToViewModel(callContact);
                    }
                })
                .toSortedList()
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d(TAG, throwable.toString());
                        getView().showLoading(false);
                    }
                })
                .subscribe(new Consumer<List<TVListViewModel>>() {
                    @Override
                    public void accept(List<TVListViewModel> tvListViewModels) throws Exception {
                        getView().showContacts(tvListViewModels);
                        getView().showLoading(false);
                    }
                }));

        subscribePresence();
    }

    private TVListViewModel modelToViewModel(CallContact callContact) {
        mContactService.loadContactData(callContact);

        return new TVListViewModel(
                callContact,
                mPresenceService.isBuddyOnline(callContact.getIds().get(0)));
    }

    public void contactClicked(TVListViewModel item) {
        if (!mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(RingError.NO_MICROPHONE);
            return;
        }

        String accountID = mAccountService.getCurrentAccount().getAccountID();

        String ringID = item.getCallContact().getPhones().get(0).getNumber().toString();
        getView().callContact(accountID, ringID);
    }

    public void reloadAccountInfos() {
        if (mAccountService == null) {
            return;
        }
        String displayableAddress = null;
        List<Account> accounts = mAccountService.getAccounts();
        for (Account account : accounts) {
            displayableAddress = account.getDisplayUri();
        }

        RingNavigationViewModel viewModel = new RingNavigationViewModel(mAccountService.getCurrentAccount(), accounts);
        getView().displayAccountInfos(displayableAddress, viewModel);
    }

    public void onExportClicked() {
        getView().showExportDialog(mAccountService.getCurrentAccount().getAccountID());
    }

    private void subscribePresence() {
        if (mAccountService.getCurrentAccount() == null) {
            return;
        }
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        Set<String> keys = mConversationFacade.getConversations().keySet();
        for (String key : keys) {
            Uri uri = new Uri(key);
            if (uri.isRingId()) {
                mPresenceService.subscribeBuddy(accountId, key, true);
            }
        }
    }
}