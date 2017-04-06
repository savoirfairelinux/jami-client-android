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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationModel;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryText;
import cx.ring.model.Phone;
import cx.ring.model.ServiceEvent;
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
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.observers.ResourceSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class SmartListPresenter extends RootPresenter<SmartListView> implements Observer<ServiceEvent> {

    private static final String TAG = SmartListPresenter.class.getSimpleName();

    private AccountService mAccountService;

    private ContactService mContactService;

    private HistoryService mHistoryService;

    private PreferencesService mPreferencesService;

    private PresenceService mPresenceService;

    private DeviceRuntimeService mDeviceRuntimeService;

    private NameLookupInputHandler mNameLookupInputHandler;

    private CallService mCallService;

    private String mLastBlockchainQuery = null;
    private ArrayList<SmartListViewModel> mSmartListViewModels;

    private CallContact mCallContact;

    private Account mCurrentAccount;

    @Inject
    public SmartListPresenter(AccountService accountService, ContactService contactService,
                              HistoryService historyService, PresenceService presenceService,
                              PreferencesService sharedPreferencesService,
                              DeviceRuntimeService deviceRuntimeService, CallService callService) {
        this.mAccountService = accountService;
        this.mContactService = contactService;
        this.mHistoryService = historyService;
        this.mPreferencesService = sharedPreferencesService;
        this.mPresenceService = presenceService;
        this.mDeviceRuntimeService = deviceRuntimeService;
        this.mCallService = callService;
    }

    @Override
    public void afterInjection() {

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

    public void init() {
        mCurrentAccount = mAccountService.getCurrentAccount();
        if (mCurrentAccount != null) {
            loadConversations(mCurrentAccount.getAccountID());
        }
    }

    private void loadConversations(final String accountId) {
        if (mSmartListViewModels == null) {
            mSmartListViewModels = new ArrayList<>();
        }
        mSmartListViewModels.clear();
        compositeDisposable.add(mHistoryService.getConversationsForAccount(accountId)
                .map(new Function<List<ConversationModel>, List<SmartListViewModel>>() {
                    @Override
                    public List<SmartListViewModel> apply(@NonNull List<ConversationModel> conversationModels) throws Exception {
                        SmartListViewModel smartListViewModel;
                        for (ConversationModel conversationModel : conversationModels) {
                            CallContact callContact = mContactService.getContact(new Uri(conversationModel.getContactId()));
                            if (callContact != null) {
                                mContactService.loadContactData(callContact);
                                smartListViewModel = createViewModelFromConversation(conversationModel, callContact);
                                if (mSmartListViewModels != null && !mSmartListViewModels.contains(smartListViewModel)) {
                                    mSmartListViewModels.add(smartListViewModel);
                                    if (callContact.getUsername() == null || callContact.getUsername().isEmpty()) {
                                        mAccountService.lookupAddress("", "", smartListViewModel.getUuid());
                                    }
                                }
                            }
                        }
                        return mSmartListViewModels;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .subscribeWith(new ResourceSingleObserver<List<SmartListViewModel>>() {
                    @Override
                    public void onSuccess(@NonNull List<SmartListViewModel> smartListViewModels) {
                        if (smartListViewModels.isEmpty()) {
                            getView().setLoading(false);
                            getView().displayNoConversationMessage();
                            getView().hideEmptyList();
                        } else {
                            Collections.sort(mSmartListViewModels, new SmartListViewModel.SmartListComparator());
                            getView().updateList(mSmartListViewModels);
                            getView().setLoading(false);
                            getView().hideNoConversationMessage();
                            subscribePresence();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, e.toString());
                        getView().setLoading(false);
                        getView().displayNoConversationMessage();
                    }
                }));
    }

    private SmartListViewModel createViewModelFromConversation(final ConversationModel conversationModel, @NonNull CallContact callContact) throws SQLException {

        HistoryText lastText = mHistoryService.getLastHistoryText(conversationModel.getId());
        HistoryCall lastCall = mHistoryService.getLastHistoryCall(conversationModel.getId());
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

        SmartListViewModel smartListViewModel = new SmartListViewModel(conversationModel.getContactId(),
                callContact.getStatus(),
                callContact.getDisplayName() != null ? callContact.getDisplayName() : callContact.getUsername(),
                callContact.getPhoto(),
                lastInteractionLong,
                lastEntryType,
                lastInteraction,
                hasUnreadMessage);


        smartListViewModel.setOnline(mPresenceService.isBuddyOnline(callContact.getIds().get(0)));
        smartListViewModel.setHasOngoingCall(mCallService.getCurrentCallForContactId(conversationModel.getContactId()) != null);
        return smartListViewModel;
    }

    public void refresh() {
        refreshConnectivity();
        subscribePresence();
        init();
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

    //TODO
    public void conversationLongClicked(SmartListViewModel smartListViewModel) {
/*        Conversation conversation = getConversationByUuid(mConversations, smartListViewModel.getUuid());
        if (conversation != null) {
            getView().displayConversationDialog(conversation);
        }*/
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
        CallContact contact = mContactService.findContact(c.getPhones().get(0).getNumber());
        if (contact.getUsername() == null) {
            contact.setUsername(c.getUsername());
        }
        getView().goToConversation(c);
    }

    public void deleteConversation(Conversation conversation) {
        mHistoryService.clearHistoryForConversation(conversation);
    }

    public void clickQRSearch() {
        getView().goToQRActivity();
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

    public void removeContact(Conversation conversation) {
        Account account = mAccountService.getCurrentAccount();
        Uri contactUri = conversation.getContact().getPhones().get(0).getNumber();
        if (contactUri.isRingId()) {
            mAccountService.removeContact(account.getAccountID(), contactUri.getRawRingId(), false);
        }
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
                mCurrentAccount = mAccountService.getCurrentAccount();
                loadConversations(mCurrentAccount.getAccountID());
                break;
        }

        if (observable instanceof HistoryService) {
            switch (event.getEventType()) {
                case INCOMING_MESSAGE:
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    if (txt.getAccount().equals(mCurrentAccount.getAccountID())) {
                        loadConversations(mCurrentAccount.getAccountID());
                        getView().scrollToTop();
                    }
                    return;
                case HISTORY_MODIFIED:
                    mCurrentAccount = mAccountService.getCurrentAccount();
                    loadConversations(mCurrentAccount.getAccountID());
                    getView().scrollToTop();
                    return;
            }
        }

        if (observable instanceof CallService) {
            switch (event.getEventType()) {
                case INCOMING_CALL:
                    Uri number = new Uri(event.getEventInput(ServiceEvent.EventInput.FROM, String.class));
                    updateIncomingCall(number.getUsername());
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
