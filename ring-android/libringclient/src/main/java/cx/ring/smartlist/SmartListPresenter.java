/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryText;
import cx.ring.model.Phone;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HistoryService;
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.utils.Log;
import cx.ring.utils.NameLookupInputHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class SmartListPresenter extends RootPresenter<SmartListView> implements Observer<ServiceEvent> {

    private static final String TAG = SmartListPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private ContactService mContactService;
    private HistoryService mHistoryService;
    private PreferencesService mPreferencesService;
    private PresenceService mPresenceService;
    private DeviceRuntimeService mDeviceRuntimeService;
    private CallService mCallService;
    private Scheduler mMainScheduler;

    private NameLookupInputHandler mNameLookupInputHandler;
    private String mLastBlockchainQuery = null;

    private ArrayList<SmartListViewModel> mSmartListViewModels;

    private CallContact mCallContact;

    @Inject
    public SmartListPresenter(AccountService accountService, ContactService contactService,
                              HistoryService historyService,
                              PresenceService presenceService, PreferencesService sharedPreferencesService,
                              DeviceRuntimeService deviceRuntimeService, CallService callService,
                              Scheduler mainScheduler) {
        this.mAccountService = accountService;
        this.mContactService = contactService;
        this.mHistoryService = historyService;
        this.mPreferencesService = sharedPreferencesService;
        this.mPresenceService = presenceService;
        this.mDeviceRuntimeService = deviceRuntimeService;
        this.mCallService = callService;
        this.mMainScheduler = mainScheduler;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mHistoryService.removeObserver(this);
        mPresenceService.removeObserver(this);
        mContactService.removeObserver(this);
        mCallService.removeObserver(this);
    }

    @Override
    public void bindView(SmartListView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mHistoryService.addObserver(this);
        mPresenceService.addObserver(this);
        mContactService.addObserver(this);
        mCallService.addObserver(this);
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
        if (query.equals("")) {
            getView().hideSearchRow();
            getView().setLoading(false);
        } else {
            Account currentAccount = mAccountService.getCurrentAccount();
            if (currentAccount == null) {
                return;
            }

            if (currentAccount.isSip()) {
                // sip search
                mCallContact = CallContact.buildUnknown(query, null);
                getView().displayNewContactRowWithName(query);
            } else {
                Uri uri = new Uri(query);
                if (uri.isRingId()) {
                    mCallContact = CallContact.buildUnknown(uri);
                    getView().displayNewContactRowWithName(query);
                } else {
                    getView().hideSearchRow();

                    // Ring search
                    if (mNameLookupInputHandler == null) {
                        mNameLookupInputHandler = new NameLookupInputHandler(mAccountService, currentAccount.getAccountID());
                    }

                    mLastBlockchainQuery = query;
                    mNameLookupInputHandler.enqueueNextLookup(query);
                    getView().setLoading(true);
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
        getView().goToContact(mContactService.getContact(new Uri(smartListViewModel.getUuid())));
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
                getView().goToCallActivity(mCallContact.getPhones().get(0).getNumber().getRawUriString());
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
        final String accountId = mAccountService.getCurrentAccount().getAccountID();

        mCompositeDisposable.add(mHistoryService.clearHistoryForContactAndAccount(callContact.getIds().get(0), accountId)
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
        mSmartListViewModels = new ArrayList<>(mContactService.getContactsDaemon().size());
        final String accountId = mAccountService.getCurrentAccount().getAccountID();

        io.reactivex.Observable.fromIterable(mAccountService.getCurrentAccount().getContacts().values()).filter(new Predicate<CallContact>() {
            @Override
            public boolean test(CallContact callContact) throws Exception {
                return !callContact.isBanned();
            }
        }).flatMap(new Function<CallContact, io.reactivex.Observable<SmartListViewModel>>() {
            @Override
            public io.reactivex.Observable<SmartListViewModel> apply(final CallContact callContact) throws Exception {
                final String ringId = callContact.getPhones().get(0).getNumber().toString();

                return mHistoryService.getLastMessagesForAccountAndContactRingId(accountId, ringId)
                        .zipWith(mHistoryService.getLastCallsForAccountAndContactRingId(accountId, ringId),
                                new BiFunction<List<HistoryText>, List<HistoryCall>, SmartListViewModel>() {
                                    @Override
                                    public SmartListViewModel apply(@NonNull List<HistoryText> lastTexts, @NonNull List<HistoryCall> lastCalls) throws Exception {
                                        return modelToViewModel(ringId, callContact, lastTexts, lastCalls);
                                    }
                                }).toObservable();
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(mMainScheduler)
                .subscribe(new DisposableObserver<SmartListViewModel>() {
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
                            Collections.sort(mSmartListViewModels, new SmartListViewModel.SmartListComparator());
                            getView().updateList(mSmartListViewModels);
                            getView().hideNoConversationMessage();
                            getView().setLoading(false);
                        } else {
                            getView().hideList();
                            getView().displayNoConversationMessage();
                            getView().setLoading(false);
                        }
                    }
                });
    }

    private SmartListViewModel modelToViewModel(String ringId, CallContact callContact, @NonNull List<HistoryText> lastTexts, @NonNull List<HistoryCall> lastCalls) {
        HistoryText lastText = lastTexts.size() > 0 ? lastTexts.get(0) : null;
        HistoryCall lastCall = lastCalls.size() > 0 ? lastCalls.get(0) : null;

        long lastInteractionLong = 0;
        int lastEntryType = 0;
        String lastInteraction = "";
        boolean hasUnreadMessage = lastText != null && !lastText.isRead();

        long lastTextTimestamp = lastText != null ? lastText.getDate().getTime() : 0;
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
        }

        SmartListViewModel smartListViewModel;

        mContactService.loadContactData(callContact);
        smartListViewModel = new SmartListViewModel(ringId,
                callContact.getStatus(),
                callContact.getDisplayName() != null ? callContact.getDisplayName() : callContact.getUsername(),
                callContact.getPhoto(),
                lastInteractionLong,
                lastEntryType,
                lastInteraction,
                hasUnreadMessage);
        smartListViewModel.setOnline(mPresenceService.isBuddyOnline(callContact.getIds().get(0)));

        return smartListViewModel;
    }

    private ArrayList<SmartListViewModel> filter(ArrayList<SmartListViewModel> list, String query) {
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
        Iterator<SmartListViewModel> it = mSmartListViewModels.iterator();
        while (it.hasNext()) {
            SmartListViewModel smartListViewModel = it.next();
            if (smartListViewModel.getUuid().contains(contactId)) {
                if (!smartListViewModel.getContactName().contains(CallContact.PREFIX_RING)) {
                    break;
                }
                mContactService.updateContactUserName(new Uri(smartListViewModel.getUuid()), contactName);
                if (!smartListViewModel.getContactName().equals(contactName)) {
                    it.remove();
                    SmartListViewModel newViewModel = new SmartListViewModel(smartListViewModel);
                    newViewModel.setContactName(contactName);
                    mSmartListViewModels.add(newViewModel);

                    Collections.sort(mSmartListViewModels, new SmartListViewModel.SmartListComparator());
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

                Collections.sort(mSmartListViewModels, new SmartListViewModel.SmartListComparator());
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

                Collections.sort(mSmartListViewModels, new SmartListViewModel.SmartListComparator());
                getView().updateList(mSmartListViewModels);
                break;
            }
        }
    }

    private void parseEventState(String name, String address, int state) {
        switch (state) {
            case 0:
                // on found
                if (mLastBlockchainQuery != null && mLastBlockchainQuery.equals(name)) {
                    mCallContact = CallContact.buildRingContact(new Uri(address), name);
                    getView().displayNewContactRowWithName(name);
                    mLastBlockchainQuery = null;
                } else {
                    if (name.equals("") || address.equals("")) {
                        return;
                    }
                    getView().hideSearchRow();
                    updateContactName(name, address);
                }
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()
                        && mLastBlockchainQuery != null
                        && mLastBlockchainQuery.equals(name)) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayNewContactRowWithName(name);
                } else {
                    getView().hideSearchRow();
                }
                break;
            default:
                // on error
                Uri uriAddress = new Uri(address);
                if (uriAddress.isRingId()
                        && mLastBlockchainQuery != null
                        && mLastBlockchainQuery.equals(name)) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayNewContactRowWithName(name);
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

        mAccountService.removeContact(mAccountService.getCurrentAccount().getAccountID(), contactId, false);
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

    @Override
    public void update(Observable observable, ServiceEvent event) {
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

        if (observable instanceof HistoryService) {
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
            }
        }
    }
}
