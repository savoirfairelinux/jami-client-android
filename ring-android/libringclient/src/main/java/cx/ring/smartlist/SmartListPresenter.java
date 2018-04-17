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
    private CallService mCallService;
    private Scheduler mMainScheduler;
    private String mCurrentQuery = null;
    private PublishSubject<String> contactQuery = PublishSubject.create();
    private Disposable mQueryDisposable;

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
        this.mCallService = callService;
        this.mMainScheduler = mainScheduler;
        this.mHardwareService = hardwareService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        /*mPresenceService.removeObserver(this);
        mContactService.removeObserver(this);
        mCallService.removeObserver(this);*/
    }

    @Override
    public void bindView(SmartListView view) {
        Log.w(TAG, "bindView");
        super.bindView(view);
        mConversationDisposable = new CompositeDisposable();
        mCompositeDisposable.add(mConversationDisposable);
        /*mCompositeDisposable.add(mAccountService.getRegisteredNames()
                .observeOn(mUiScheduler)
                .subscribe(r -> {
                    Log.w(TAG, "getRegisteredNames onNext");
                    if (mLastBlockchainQuery != null
                                && (mLastBlockchainQuery.equals("") || !mLastBlockchainQuery.equals(r.name))) {
                        return;
                    }
                    parseEventState(r.name, r.address, r.state);
                }));*/
        /*mPresenceService.addObserver(this);
        mContactService.addObserver(this);
        mCallService.addObserver(this);*/
        contactQuery = PublishSubject.create();
    }

    public void refresh() {
        refreshConnectivity();
        subscribePresence();
        getView().hideSearchRow();
        loadConversations();
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
            Account currentAccount = mAccountService.getCurrentAccount();
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

                    if (mQueryDisposable == null || mQueryDisposable.isDisposed()) {
                        mQueryDisposable = contactQuery
                                .debounce(350, TimeUnit.MILLISECONDS)
                                .switchMapSingle(q -> mAccountService.findRegistrationByName(mAccountService.getCurrentAccount().getAccountID(), "", q))
                                .observeOn(mUiScheduler)
                                .subscribe(q -> parseEventState(q.name, q.address, q.state));
                        mCompositeDisposable.add(mQueryDisposable);
                    }

                    // Ring search
                    /*if (mNameLookupInputHandler == null) {
                        mNameLookupInputHandler = new NameLookupInputHandler(mAccountService, currentAccount.getAccountID());
                    }

                    mLastBlockchainQuery = query;
                    mNameLookupInputHandler.enqueueNextLookup(query);*/
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
        mConversationDisposable.add(
                mConversationFacade.getConversationSubject()
                        .concatMapSingle(conversations -> {
                            Log.d(TAG, "loadConversations map");
                            return Observable.fromIterable(conversations.conversations.values())
                                    .map(conversation -> modelToViewModel(conversations.account.getAccountID(), conversation))
                                    .toSortedList(new SmartListViewModel.Comparator());
                        })
                        //.subscribeOn(Schedulers.computation())
                        .observeOn(mUiScheduler)
                        .subscribeWith(new DisposableObserver<List<SmartListViewModel>>() {
                            @Override
                            public void onNext(List<SmartListViewModel> smartListViewModels) {
                                getView().setLoading(false);
                                Log.d(TAG, "loadConversations subscribe onSuccess");
                                mSmartListViewModels = smartListViewModels;
                                if (smartListViewModels.size() > 0) {
                                    getView().updateList(filter(smartListViewModels, mCurrentQuery));
                                    getView().hideNoConversationMessage();
                                } else {
                                    getView().hideList();
                                    getView().displayNoConversationMessage();
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
    }

    private void loadContacts() {
        mSmartListViewModels = new ArrayList<>(mContactService.getContactsDaemon().size());

        mCompositeDisposable.add(
                Single.fromCallable(() -> mContactService.loadContactsFromSystem(false, true))
                .flatMapObservable(longCallContactMap ->
                        Observable.fromIterable(longCallContactMap.values()))
                        .map(this::modelToViewModel).subscribeOn(Schedulers.computation())
                .observeOn(mMainScheduler)
                .subscribeWith(new DisposableObserver<SmartListViewModel>() {
                    @Override
                    public void onNext(SmartListViewModel smartListViewModel) {
                        mSmartListViewModels.add(smartListViewModel);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, e.toString());
                    }

                    @Override
                    public void onComplete() {
                        if (mSmartListViewModels.size() > 0) {
                            Collections.sort(mSmartListViewModels, new SmartListViewModel.Comparator());
                            getView().updateList(mSmartListViewModels);
                            getView().hideNoConversationMessage();
                            getView().setLoading(false);
                        } else {
                            getView().hideList();
                            getView().displayNoConversationMessage();
                            getView().setLoading(false);
                        }
                    }
                }));

    }

    private SmartListViewModel modelToViewModel(String accountId, Conversation conversation) {
        Log.d(TAG, "modelToViewModel");

        /*HistoryText lastText = lastTexts.size() > 0 ? lastTexts.get(0) : null;
        HistoryCall lastCall = lastCalls.size() > 0 ? lastCalls.get(0) : null;

        long lastInteractionLong = 0;
        int lastEntryType = 0;
        String lastInteraction = "";
        boolean hasUnreadMessage = lastText != null && !lastText.isRead();

        long lastTextTimestamp = lastText != null ? lastText.getDate() : 0;
        long lastCallTimestamp = lastCall != null ? lastCall.getEndDate().getTime() : 0;
        if (lastTextTimestamp > 0 && lastTextTimestamp > lastCallTimestamp) {
            String msgString = lastText.getMessage();
            if (msgString != null && !msgString.isEmpty() && msgString.contains("\n")) {
                int lastIndexOfChar = msgString.lastIndexOf("\n");
                if (lastIndexOfChar + 1 < msgString.length()) {
                    msgString = msgString.substring(msgString.lastIndexOf("\n") + 1);
                }
            }
            lastInteractionLong = lastTextTimestamp;
            lastEntryType = lastText.isIncoming() ? SmartListViewModel.TYPE_INCOMING_MESSAGE : SmartListViewModel.TYPE_OUTGOING_MESSAGE;
            lastInteraction = msgString;

        } else if (lastCallTimestamp > 0) {
            lastInteractionLong = lastCallTimestamp;
            lastEntryType = lastCall.isIncoming() ? SmartListViewModel.TYPE_INCOMING_CALL : SmartListViewModel.TYPE_OUTGOING_CALL;
            lastInteraction = lastCall.getDurationString();
        }*/

        CallContact contact = conversation.getContact();
        //HistoryEntry h = conversation.getHistory(accountId);
        //h.getLast();
        //conversation.getLastElement();
        //conversation.getUnreadTextMessages();

        SmartListViewModel smartListViewModel;

        mContactService.loadContactData(contact);
        smartListViewModel = new SmartListViewModel(contact.getIds().get(0),
                contact.getStatus(),
                contact.getDisplayName() != null ? contact.getDisplayName() : contact.getUsername(),
                contact.getPhoto(),
                0,
                SmartListViewModel.TYPE_INCOMING_CALL,
                "",
                false);
        smartListViewModel.setOnline(mPresenceService.isBuddyOnline(contact.getIds().get(0)));

        return smartListViewModel;
    }

    private SmartListViewModel modelToViewModel(CallContact callContact) {
        SmartListViewModel smartListViewModel;

        callContact.setUsername(callContact.getDisplayName());

        mContactService.loadContactData(callContact);
        smartListViewModel = new SmartListViewModel(callContact.getPhones().get(0).getNumber().toString(),
                callContact.getStatus(),
                callContact.getDisplayName() != null ? callContact.getDisplayName() : callContact.getUsername(),
                callContact.getPhoto(),
                0,
                0,
                "",
                false);

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

    private void updateContactName(String contactName, String contactId) {
        if (mSmartListViewModels == null)
            return;
        Iterator<SmartListViewModel> it = mSmartListViewModels.iterator();
        while (it.hasNext()) {
            SmartListViewModel smartListViewModel = it.next();
            if (smartListViewModel.getUuid() != null && smartListViewModel.getUuid().contains(contactId)) {
                if (smartListViewModel.getContactName() != null && !smartListViewModel.getContactName().contains(CallContact.PREFIX_RING)) {
                    break;
                }
                mContactService.updateContactUserName(new Uri(smartListViewModel.getUuid()), contactName);
                if (!smartListViewModel.getContactName().equals(contactName)) {
                    it.remove();
                    SmartListViewModel newViewModel = new SmartListViewModel(smartListViewModel);
                    newViewModel.setContactName(contactName);
                    mSmartListViewModels.add(newViewModel);

                    Collections.sort(mSmartListViewModels, new SmartListViewModel.Comparator());
                    getView().updateList(mSmartListViewModels);
                }
                break;
            }
        }
    }

    private void updatePresence() {
        Iterator<SmartListViewModel> it = mSmartListViewModels.iterator();
        while (it.hasNext()) {
            SmartListViewModel smartListViewModel = it.next();
            boolean isOnline = mPresenceService.isBuddyOnline(smartListViewModel.getUuid());
            if (smartListViewModel.isOnline() != isOnline) {
                it.remove();
                SmartListViewModel newViewModel = new SmartListViewModel(smartListViewModel);
                newViewModel.setOnline(isOnline);
                mSmartListViewModels.add(newViewModel);

                Collections.sort(mSmartListViewModels, new SmartListViewModel.Comparator());
                getView().updateList(mSmartListViewModels);
                break;
            }
        }
    }

    private void updateIncomingCall(String from) {
        Iterator<SmartListViewModel> it = mSmartListViewModels.iterator();
        Log.d(TAG, from);
        while (it.hasNext()) {
            SmartListViewModel smartListViewModel = it.next();

            if (smartListViewModel.getUuid().contains(from)) {
                it.remove();
                Log.d(TAG, from + smartListViewModel.getContactName());
                SmartListViewModel newViewModel = new SmartListViewModel(smartListViewModel);
                newViewModel.setHasOngoingCall(true);
                mSmartListViewModels.add(newViewModel);

                Collections.sort(mSmartListViewModels, new SmartListViewModel.Comparator());
                getView().updateList(mSmartListViewModels);
                break;
            }
        }
    }

    private void parseEventState(String name, String address, int state) {
        Log.w(TAG, "parseEventState " + name + " " + address);
        switch (state) {
            case 0:
                // on found
                //if (mLastBlockchainQuery != null && mLastBlockchainQuery.equals(name)) {
                    mCallContact = CallContact.buildRingContact(new Uri(address), name);
                    getView().displayContact(mCallContact);
                /*    mLastBlockchainQuery = null;
                } else {
                    if (("").equals(name) || ("").equals(address)) {
                        return;
                    }
                    getView().hideSearchRow();
                    updateContactName(name, address);
                }*/
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()
                        /*&& mLastBlockchainQuery != null
                        && mLastBlockchainQuery.equals(name)*/) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayContact(mCallContact);
                } else {
                    getView().hideSearchRow();
                }
                break;
            default:
                // on error
                Uri uriAddress = new Uri(address);
                if (uriAddress.isRingId()
                        /*&& mLastBlockchainQuery != null
                        && mLastBlockchainQuery.equals(name)*/) {
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

    private void subscribePresence() {
        if (mAccountService.getCurrentAccount() == null || mSmartListViewModels == null || mSmartListViewModels.isEmpty()) {
            return;
        }
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        for (SmartListViewModel smartListViewModel : mSmartListViewModels) {
            String ringId = smartListViewModel.getUuid();
            Uri uri = new Uri(ringId);
            if (uri.isRingId()) {
                mPresenceService.subscribeBuddy(accountId, ringId, true);
            } else {
                Log.i(TAG, "Trying to subscribe to an invalid uri " + ringId);
            }
        }
    }

   /* @Override
    public void update(cx.ring.utils.Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTERED_NAME_FOUND:
                String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                if (mLastBlockchainQuery != null
                        && (mLastBlockchainQuery.equals("") || !mLastBlockchainQuery.equals(name))) {
                    return;
                }
                String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                parseEventState(name, address, state);
                break;
            case REGISTRATION_STATE_CHANGED:
                refreshConnectivity();
                break;
            case CONTACTS_CHANGED:
                loadConversations();
                break;
        }
*/
        /*if (observable instanceof HistoryService) {
            switch (event.getEventType()) {
                case INCOMING_MESSAGE:
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    if (txt.getAccount().equals(mAccountService.getCurrentAccount().getAccountID())) {
                        loadConversations();
                        getView().scrollToTop();
                    }
                    return;
                case HISTORY_MODIFIED:
                    loadConversations();
                    getView().scrollToTop();
                    return;
            }
        }

        if (observable instanceof CallService) {
            switch (event.getEventType()) {
                case INCOMING_CALL:
                    SipCall call = event.getEventInput(ServiceEvent.EventInput.CALL, SipCall.class);
                    updateIncomingCall(call.getNumber());
                    return;
            }
        }


        if (observable instanceof PresenceService) {
            switch (event.getEventType()) {
                case NEW_BUDDY_NOTIFICATION:
                    updatePresence();
                    return;
                default:
            }
        }
    }*/
}
