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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.RingError;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class SmartListPresenter extends RootPresenter<SmartListView> {

    private static final String TAG = SmartListPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private ContactService mContactService;
    private ConversationFacade mConversationFacade;

    private PreferencesService mPreferencesService;
    private PresenceService mPresenceService;
    private DeviceRuntimeService mDeviceRuntimeService;
    private HardwareService mHardwareService;
    private Scheduler mMainScheduler;
    private String mCurrentQuery = null;
    private PublishSubject<String> contactQuery = PublishSubject.create();
    private Disposable mQueryDisposable;

    private Account mAccount;
    private List<SmartListViewModel> mSmartListViewModels;
    private CallContact mCallContact;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private CompositeDisposable mConversationDisposable;

    @Inject
    public SmartListPresenter(AccountService accountService, ContactService contactService,
                              ConversationFacade conversationFacade,
                              PresenceService presenceService, PreferencesService sharedPreferencesService,
                              DeviceRuntimeService deviceRuntimeService, CallService callService,
                              Scheduler mainScheduler, HardwareService hardwareService) {
        this.mAccountService = accountService;
        this.mContactService = contactService;
        this.mConversationFacade = conversationFacade;
        this.mPreferencesService = sharedPreferencesService;
        this.mPresenceService = presenceService;
        this.mDeviceRuntimeService = deviceRuntimeService;
        this.mMainScheduler = mainScheduler;
        this.mHardwareService = hardwareService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
    }

    @Override
    public void bindView(SmartListView view) {
        Log.w(TAG, "bindView");
        super.bindView(view);
        mConversationDisposable = new CompositeDisposable();
        mCompositeDisposable.add(mConversationDisposable);
        contactQuery = PublishSubject.create();
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
                    && !mPreferencesService.getUserSettings().isAllowMobileData();
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
                getView().displayContact(mCallContact);
            } else {
                if (uri.isRingId()) {
                    mCallContact = mContactService.findContact(currentAccount, uri);
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
                                .subscribe(q -> parseEventState(q.name, q.address, q.state));
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
        startConversation(mCallContact);
    }

    public void conversationClicked(SmartListViewModel smartListViewModel) {
        startConversation(mContactService.getContact(new Uri(smartListViewModel.getUuid())));
    }

    public void conversationLongClicked(SmartListViewModel smartListViewModel) {
        getView().displayConversationDialog(smartListViewModel);
    }

    public void photoClicked(SmartListViewModel smartListViewModel) {
        // ignored
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

                getView().goToCallActivity(mAccountService.getCurrentAccount().getAccountID(),
                        mCallContact.getPhones().get(0).getNumber().getRawUriString());
            }
        }
    }

    public void fabButtonClicked() {
        getView().displayMenuItem();
    }

    public void startConversation(CallContact c) {
        getView().goToConversation(mAccountService.getCurrentAccount().getAccountID(), c.getPhones().get(0).getNumber().toString());
    }

    public void copyNumber(SmartListViewModel smartListViewModel) {
        CallContact callContact = mContactService.getContact(new Uri(smartListViewModel.getUuid()));
        getView().copyNumber(callContact);
    }

    public void deleteConversation(SmartListViewModel smartListViewModel) {
        CallContact callContact = mContactService.getContact(new Uri(smartListViewModel.getUuid()));
        getView().displayDeleteDialog(callContact);
    }

    public void deleteConversation(final CallContact callContact) {
        mConversationDisposable.add(mConversationFacade.clearHistoryForContactAndAccount(callContact.getIds().get(0))
                .subscribeOn(Schedulers.computation())
                .observeOn(mMainScheduler)
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        loadConversations();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, e.toString());
                    }
                }));
    }

    public void clickQRSearch() {
        getView().goToQRActivity();
    }

    private void loadConversations() {
        Log.d(TAG, "loadConversations");
        mConversationDisposable.clear();
        getView().setLoading(true);
        final AtomicReference<Account> account = new AtomicReference<>();
        mConversationDisposable.add(
                mConversationFacade.getConversationSubject()
                        .map(conversations -> {
                            ArrayList<SmartListViewModel> viewModel = new ArrayList<>(conversations.conversations.size());
                            account.set(conversations.account);
                            for (Conversation c : conversations.conversations.values()) {
                                viewModel.add(modelToViewModel(c));
                            }
                            Collections.sort(viewModel, (a, b) -> Long.compare(b.getLastInteractionTime(), a.getLastInteractionTime()));
                            return viewModel;
                        })
                        .observeOn(mUiScheduler)
                        .subscribeWith(new DisposableObserver<List<SmartListViewModel>>() {
                            @Override
                            public void onNext(List<SmartListViewModel> smartListViewModels) {
                                final SmartListView view = getView();
                                view.setLoading(false);
                                Log.d(TAG, "loadConversations subscribe onSuccess");
                                mSmartListViewModels = smartListViewModels;
                                mAccount = account.get();
                                if (smartListViewModels.size() > 0) {
                                    view.updateList(filter(smartListViewModels, mCurrentQuery));
                                    view.hideNoConversationMessage();
                                } else {
                                    view.hideList();
                                    view.displayNoConversationMessage();
                                }
                            }
                            @Override
                            public void onError(Throwable e) {
                                getView().setLoading(false);
                                Log.d(TAG, "loadConversations subscribe onError");
                            }
                            @Override
                            public void onComplete() {}
                        }));
        mConversationDisposable.add(mPresenceService.getPresenceUpdates()
                .observeOn(mUiScheduler)
                .subscribe(contact -> {
                    if (mSmartListViewModels == null)
                        return;
                    String cid = contact.getIds().get(0);
                    for (int i=0; i<mSmartListViewModels.size(); i++) {
                        SmartListViewModel vm = mSmartListViewModels.get(i);
                        if (vm.getUuid().equals(cid)) {
                            vm.setOnline(contact.isOnline());
                            getView().update(i);
                            break;
                        }
                    }
                }));
    }

    private SmartListViewModel modelToViewModel(Conversation conversation) {
        CallContact contact = conversation.getContact();
        String primaryId = contact.getIds().get(0);
        mContactService.loadContactData(contact);
        SmartListViewModel smartListViewModel = new SmartListViewModel(primaryId,
                contact.getStatus(),
                contact.getDisplayName() != null ? contact.getDisplayName() : contact.getUsername(),
                contact.getPhoto(),
                conversation.getLastEvent());
        smartListViewModel.setOnline(mPresenceService.isBuddyOnline(primaryId));
        return smartListViewModel;
    }

    private List<SmartListViewModel> filter(List<SmartListViewModel> list, String query) {
        if (StringUtils.isEmpty(query))
            return list;
        ArrayList<SmartListViewModel> filteredList = new ArrayList<>();
        if (list == null || list.size() == 0) {
            return filteredList;
        }
        for (SmartListViewModel smartListViewModel : list) {
            if (smartListViewModel.getContactName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(smartListViewModel);
            }
        }
        return filteredList;
    }

    private void parseEventState(String name, String address, int state) {
        Log.w(TAG, "parseEventState " + name + " " + address);
        switch (state) {
            case 0:
                // on found
                mCallContact = CallContact.buildRingContact(new Uri(address), name);
                getView().displayContact(mCallContact);
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayContact(mCallContact);
                } else {
                    getView().hideSearchRow();
                }
                break;
            default:
                // on error
                Uri uriAddress = new Uri(address);
                if (uriAddress.isRingId()) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayContact(mCallContact);
                } else {
                    getView().hideSearchRow();
                }
                break;
        }
        getView().setLoading(false);
    }

    public void removeContact(SmartListViewModel smartListViewModel) {
        String contactId = smartListViewModel.getUuid();
        String[] split = contactId.split(":");
        if (split.length > 1 && split[0].equals("ring")) {
            contactId = split[1];
        }

        mAccountService.removeContact(mAccountService.getCurrentAccount().getAccountID(), contactId, true);
        mSmartListViewModels.remove(smartListViewModel);
    }
}
