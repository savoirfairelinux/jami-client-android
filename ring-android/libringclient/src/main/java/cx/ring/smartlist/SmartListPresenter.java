/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

    private final CompositeDisposable mConversationDisposable = new CompositeDisposable();
    private final CompositeDisposable mContactDisposable = new CompositeDisposable();

    @Inject
    public SmartListPresenter(AccountService accountService, ContactService contactService,
                              ConversationFacade conversationFacade,
                              PreferencesService sharedPreferencesService,
                              DeviceRuntimeService deviceRuntimeService,
                              HardwareService hardwareService,
                              @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mContactService = contactService;
        mConversationFacade = conversationFacade;
        mPreferencesService = sharedPreferencesService;
        mDeviceRuntimeService = deviceRuntimeService;
        mHardwareService = hardwareService;
        mUiScheduler = uiScheduler;

        accountSubject = mConversationFacade
                .getCurrentAccountSubject()
                .doOnNext(a -> mAccount = a);

        conversationViews = accountSubject
                .switchMap(Account::getConversationsViewModels)
                .subscribeOn(Schedulers.computation())
                .observeOn(mUiScheduler);
    }

    @Override
    public void bindView(SmartListView view) {
        super.bindView(view);
        mCompositeDisposable.clear();
        mCompositeDisposable.add(mConversationDisposable);
        mCompositeDisposable.add(mContactDisposable);
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
        SmartListView view = getView();
        if (view == null)
            return;
        if (StringUtils.isEmpty(query)) {
            if (mQueryDisposable != null) {
                mQueryDisposable.dispose();
                mQueryDisposable = null;
            }
            view.hideSearchRow();
            view.setLoading(false);
        } else {
            final Account currentAccount = mAccount;
            if (currentAccount == null) {
                return;
            }

            Uri uri = new Uri(query);
            if (currentAccount.isSip()) {
                // sip search
                mCallContact = mContactService.findContact(currentAccount, uri);
                mCompositeDisposable.add(mContactService.loadContactData(mCallContact)
                        .observeOn(mUiScheduler)
                        .subscribe(() -> view.displayContact(mCallContact), rt -> view.displayContact(mCallContact)));
            } else {
                if (uri.isRingId()) {
                    mCallContact = currentAccount.getContactFromCache(uri);
                    mCompositeDisposable.add(mContactService.getLoadedContact(currentAccount.getAccountID(), mCallContact)
                            .observeOn(mUiScheduler)
                            .subscribe(view::displayContact, e -> Log.e(TAG, "Can't load contact")));
                } else {
                    view.hideSearchRow();
                    view.setLoading(true);

                    // Ring search
                    if (mQueryDisposable == null || mQueryDisposable.isDisposed()) {
                        mQueryDisposable = contactQuery
                                .debounce(350, TimeUnit.MILLISECONDS)
                                .switchMapSingle(q -> mAccountService.findRegistrationByName(mAccount.getAccountID(), "", q))
                                .observeOn(mUiScheduler)
                                .subscribe(q -> parseEventState(mAccountService.getAccount(q.accountId), q.name, q.address, q.state),
                                        e -> Log.e(TAG, "Can't perform query"));
                        mCompositeDisposable.add(mQueryDisposable);
                    }
                    contactQuery.onNext(query);
                }
            }
        }

        view.updateList(filter(mSmartListViewModels, query));
    }

    public void newContactClicked() {
        if (mCallContact == null || mAccount == null) {
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
                            CompositeDisposable cd = new CompositeDisposable();
                            for (SmartListViewModel vm : viewModels) {
                                cd.add(mContactService.observeContact(vm.getAccountId(), vm.getContact())
                                        .subscribeOn(Schedulers.computation())
                                        .observeOn(mUiScheduler)
                                        .subscribe(c -> getView().update(vm)));
                            }
                            mContactDisposable.clear();
                            mContactDisposable.add(cd);
                        }, e -> {
                            getView().setLoading(false);
                            Log.d(TAG, "loadConversations subscribe onError", e);
                        }));

        Log.w(TAG, "loadConversations() subscribe");
        mConversationDisposable.add(accountSubject
                        .switchMap(Account::getConversationViewModel)
                        .observeOn(mUiScheduler)
                        .subscribe(vm -> {
                            if (mSmartListViewModels == null)
                                return;
                            for (int i=0; i<mSmartListViewModels.size(); i++)
                                if (mSmartListViewModels.get(i).getContact() == vm.getContact())
                                    mSmartListViewModels.set(i, vm);
                            getView().update(vm);
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

    public void banContact(SmartListViewModel smartListViewModel) {
        CallContact contact = smartListViewModel.getContact();
        mAccountService.removeContact(mAccount.getAccountID(), contact.getPrimaryNumber(), true);
        mSmartListViewModels.remove(smartListViewModel);
    }
}
