/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
import cx.ring.model.Phone;
import cx.ring.model.RingError;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class SmartListPresenter extends RootPresenter<SmartListView> {

    private static final String TAG = SmartListPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;
    private final ConversationFacade mConversationFacade;
    private final PreferencesService mPreferencesService;
    private final PresenceService mPresenceService;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final HardwareService mHardwareService;

    private Disposable mQueryDisposable;

    private String mCurrentQuery = null;
    private Account mAccount;
    private List<SmartListViewModel> mSmartListViewModels;
    private CallContact mCallContact;

    private final PublishSubject<String> contactQuery = PublishSubject.create();
    private final Observable<Account> accountSubject;
    private final Observable<List<SmartListViewModel>> conversationViews;

    private final Scheduler mUiScheduler;

    private CompositeDisposable mConversationDisposable;

    @Inject
    public SmartListPresenter(AccountService accountService, ContactService contactService,
                              ConversationFacade conversationFacade,
                              PresenceService presenceService, PreferencesService sharedPreferencesService,
                              DeviceRuntimeService deviceRuntimeService,
                              HardwareService hardwareService,
                              @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mContactService = contactService;
        mConversationFacade = conversationFacade;
        mPreferencesService = sharedPreferencesService;
        mPresenceService = presenceService;
        mDeviceRuntimeService = deviceRuntimeService;
        mHardwareService = hardwareService;
        mUiScheduler = uiScheduler;

        accountSubject = mConversationFacade
                .getCurrentAccountSubject()
                .doOnNext(a -> mAccount = a)
                .share();

        conversationViews = accountSubject
                .switchMap(Account::getConversationsViewModels)
                .observeOn(mUiScheduler);
    }

    @Override
    public void bindView(SmartListView view) {
        super.bindView(view);
        mConversationDisposable = new CompositeDisposable();
        mCompositeDisposable.add(mConversationDisposable);
        loadConversations();
    }

    public void refresh() {
        refreshConnectivity();
        getView().hideSearchRow();
    }

    private void refreshConnectivity() {
        boolean isConnected = mPreferencesService.hasNetworkConnected();

        if (isConnected) {
            getView().hideErrorPanel();
        } else {
            boolean isMobileAndNotAllowed = mDeviceRuntimeService.isConnectedMobile()
                    && !mPreferencesService.getSettings().isAllowMobileData();
            if (isMobileAndNotAllowed) {
                getView().displayMobileDataPanel();
            } else {
                getView().displayNetworkErrorPanel();
            }
        }
    }

    public void queryTextChanged(String query) {
        mCurrentQuery = query;
        if (query.equals("")) {
            if (mQueryDisposable != null) {
                mQueryDisposable.dispose();
                mQueryDisposable = null;
            }
            getView().hideSearchRow();
            getView().setLoading(false);
        } else {
            final Account currentAccount = mAccount;
            if (currentAccount == null) {
                return;
            }

            Uri uri = new Uri(query);
            if (currentAccount.isSip()) {
                // sip search
                mCallContact = mContactService.findContact(currentAccount, uri);
                mContactService.loadContactData(mCallContact);
                getView().displayContact(mCallContact);
            } else {
                if (uri.isRingId()) {
                    mCallContact = currentAccount.getContactFromCache(uri);
                    getView().displayContact(mCallContact);
                } else {
                    getView().hideSearchRow();
                    getView().setLoading(true);

                    // Ring search
                    if (mQueryDisposable == null || mQueryDisposable.isDisposed()) {
                        mQueryDisposable = contactQuery
                                .debounce(350, TimeUnit.MILLISECONDS)
                                .switchMapSingle(q -> mAccountService.findRegistrationByName(mAccount.getAccountID(), "", q))
                                .observeOn(mUiScheduler)
                                .subscribe(q -> parseEventState(mAccountService.getAccount(q.accountId), q.name, q.address, q.state));
                        mCompositeDisposable.add(mQueryDisposable);
                    }
                    contactQuery.onNext(query);
                }
            }
        }

        getView().updateList(filter(mSmartListViewModels, query));
    }

    public void newContactClicked() {
        if (mCallContact == null) {
            return;
        }
        startConversation(mAccount.getAccountID(), mCallContact);
    }

    public void conversationClicked(SmartListViewModel smartListViewModel) {
        startConversation(smartListViewModel.getAccountId(), smartListViewModel.getContact());
    }

    public void conversationLongClicked(SmartListViewModel smartListViewModel) {
        getView().displayConversationDialog(smartListViewModel);
    }

    public void quickCallClicked() {
        if (mCallContact != null) {
            if (mCallContact.getPhones().size() > 1) {
                CharSequence numbers[] = new CharSequence[mCallContact.getPhones().size()];
                int i = 0;
                for (Phone p : mCallContact.getPhones()) {
                    numbers[i++] = p.getNumber().getRawUriString();
                }

                getView().displayChooseNumberDialog(numbers);
            } else {
                if (!mHardwareService.isVideoAvailable() && !mHardwareService.hasMicrophone()) {
                    getView().displayErrorToast(RingError.NO_INPUT);
                    return;
                }

                getView().goToCallActivity(mAccount.getAccountID(),
                        mCallContact.getPrimaryUri().getRawUriString());
            }
        }
    }

    public void fabButtonClicked() {
        getView().displayMenuItem();
    }

    public void startConversation(String accountId, CallContact c) {
        getView().goToConversation(accountId, c.getPrimaryUri());
    }

    public void startConversation(Uri uri) {
        getView().goToConversation(mAccount.getAccountID(), uri);
    }

    public void copyNumber(SmartListViewModel smartListViewModel) {
        getView().copyNumber(smartListViewModel.getContact());
    }

    public void clearHistory(SmartListViewModel smartListViewModel) {
        getView().displayDeleteDialog(smartListViewModel.getContact());
    }

    public void clearHistory(final CallContact callContact) {
        mConversationDisposable.add(mConversationFacade
                .clearHistory(mAccount.getAccountID(), callContact.getPrimaryUri())
                .subscribeOn(Schedulers.computation()).subscribe());
    }

    public void clickQRSearch() {
        getView().goToQRActivity();
    }

    private void loadConversations() {
        mConversationDisposable.clear();
        getView().setLoading(true);

        mConversationDisposable.add(conversationViews
                        .subscribe(viewModels -> {
                            final SmartListView view = getView();
                            view.setLoading(false);
                            mSmartListViewModels = viewModels;
                            if (viewModels.size() > 0) {
                                view.updateList(filter(viewModels, mCurrentQuery));
                                view.hideNoConversationMessage();
                            } else {
                                view.hideList();
                                view.displayNoConversationMessage();
                            }
                        }, e -> {
                            getView().setLoading(false);
                            Log.d(TAG, "loadConversations subscribe onError", e);
                        }));

        Log.w(TAG, "loadConversations() subscribe");
        mConversationDisposable.add(accountSubject
                        .switchMap(Account::getConversationViewModel)
                        .observeOn(mUiScheduler)
                        .subscribe(vm -> {
                            Log.d(TAG, "getConversationSubject " + vm);
                            getView().update(vm);
                        }));

        mConversationDisposable.add(mPresenceService.getPresenceUpdates()
                .observeOn(mUiScheduler)
                .subscribe(contact -> {
                    if (mSmartListViewModels == null)
                        return;
                    for (int i=0; i<mSmartListViewModels.size(); i++) {
                        SmartListViewModel vm = mSmartListViewModels.get(i);
                        if (vm.getContact() == contact) {
                            if (vm.isOnline() != contact.isOnline()) {
                                vm.setOnline(contact.isOnline());
                                getView().update(i);
                            }
                            break;
                        }
                    }
                }));
    }

    private List<SmartListViewModel> filter(List<SmartListViewModel> list, String query) {
        if (StringUtils.isEmpty(query))
            return list;
        ArrayList<SmartListViewModel> filteredList = new ArrayList<>();
        if (list == null || list.size() == 0) {
            return filteredList;
        }
        query = query.toLowerCase();
        for (SmartListViewModel smartListViewModel : list) {
            if (smartListViewModel.getContact().matches(query)) {
                filteredList.add(smartListViewModel);
            }
        }
        return filteredList;
    }

    private void parseEventState(Account account, String name, String address, int state) {
        Log.w(TAG, "parseEventState " + name + " " + address);
        switch (state) {
            case 0:
                // on found
                mCallContact = account.getContactFromCache(address);
                mCallContact.setUsername(name);
                getView().displayContact(mCallContact);
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()) {
                    mCallContact = account.getContactFromCache(uriName);
                    getView().displayContact(mCallContact);
                } else {
                    getView().hideSearchRow();
                }
                break;
            default:
                // on error
                Uri uriAddress = new Uri(address);
                if (uriAddress.isRingId()) {
                    mCallContact = account.getContactFromCache(uriAddress);
                    getView().displayContact(mCallContact);
                } else {
                    getView().hideSearchRow();
                }
                break;
        }
        getView().setLoading(false);
    }

    public void removeContact(SmartListViewModel smartListViewModel) {
        CallContact contact = smartListViewModel.getContact();
        mAccountService.removeContact(mAccount.getAccountID(), contact.getPrimaryNumber(), true);
        mSmartListViewModels.remove(smartListViewModel);
    }
}
