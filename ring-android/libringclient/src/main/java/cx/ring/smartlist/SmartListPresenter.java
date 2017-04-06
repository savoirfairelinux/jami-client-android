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


import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.ConfigKey;
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
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HistoryService;
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.utils.BlockchainInputHandler;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.observers.ResourceSingleObserver;
import io.reactivex.schedulers.Schedulers;


public class SmartListPresenter extends RootPresenter<SmartListView> implements Observer<ServiceEvent> {

    private static final String TAG = SmartListPresenter.class.getSimpleName();

    private AccountService mAccountService;

    private ContactService mContactService;

    private HistoryService mHistoryService;

    private PreferencesService mPreferencesService;

    private ConversationFacade mConversationFacade;

    private PresenceService mPresenceService;

    private DeviceRuntimeService mDeviceRuntimeService;

    private BlockchainInputHandler mBlockchainInputHandler;
    private String mLastBlockchainQuery = null;

    private ArrayList<SmartListViewModel> mSmartListViewModels;

    private CallContact mCallContact;

    private Account mCurrentAccount;

    @Inject
    public SmartListPresenter(AccountService accountService, ContactService contactService,
                              HistoryService historyService, ConversationFacade conversationFacade,
                              PresenceService presenceService, PreferencesService sharedPreferencesService,
                              DeviceRuntimeService deviceRuntimeService) {
        this.mAccountService = accountService;
        this.mContactService = contactService;
        this.mHistoryService = historyService;
        this.mPreferencesService = sharedPreferencesService;
        this.mConversationFacade = conversationFacade;
        this.mPresenceService = presenceService;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mConversationFacade.removeObserver(this);
        mHistoryService.removeObserver(this);
        mPresenceService.removeObserver(this);
        mContactService.removeObserver(this);
    }

    @Override
    public void bindView(SmartListView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
        mHistoryService.addObserver(this);
        mPresenceService.addObserver(this);
        mContactService.addObserver(this);
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

        final Account account = mAccountService.getAccount(accountId);
        boolean acceptAllMessages = account.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN);

        compositeDisposable.add(mHistoryService.getConversationsForAccount(accountId)
                .zipWith(mContactService.loadContacts(acceptAllMessages),
                        new BiFunction<List<ConversationModel>, List<CallContact>, List<SmartListViewModel>>() {
                            @Override
                            public List<SmartListViewModel> apply(@NonNull List<ConversationModel> conversationModels, @NonNull List<CallContact> callContacts) throws Exception {
                                SmartListViewModel smartListViewModel;
                                for (ConversationModel conversationModel : conversationModels) {
                                    boolean allowAllContacts = account.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN);
                                    CallContact callContact = mContactService.getContact(new Uri(conversationModel.getContactId()), allowAllContacts);
                                    if (callContact == null && allowAllContacts) {
                                        callContact = mContactService.findContactByNumber(conversationModel.getContactId());
                                    }
                                    if (callContact != null) {
                                        smartListViewModel = createViewModelFromConversation(conversationModel, callContact);
                                        if (mSmartListViewModels != null && !mSmartListViewModels.contains(smartListViewModel)) {
                                            mSmartListViewModels.add(smartListViewModel);
                                            if (callContact.getUserName() == null || callContact.getUserName().isEmpty()) {
                                                mAccountService.lookupAddress("", "", smartListViewModel.getUuid());
                                            }
                                        }
                                    }
                                }
                                for (CallContact callContact : callContacts) {
                                    boolean found = false;
                                    for (ConversationModel conversationModel : conversationModels) {
                                        if (callContact.getPhones().get(0).getNumber().toString().equals(conversationModel.getContactId())) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        smartListViewModel = createViewModelFromContact(callContact);
                                        if (mSmartListViewModels != null && !mSmartListViewModels.contains(smartListViewModel)) {
                                            mSmartListViewModels.add(smartListViewModel);
                                            if (callContact.getUserName() == null || callContact.getUserName().isEmpty()) {
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

    private SmartListViewModel createViewModelFromContact(@NonNull CallContact callContact) {
        Tuple<String, byte[]> tuple = mContactService.loadContactData(callContact);

        String userName = callContact.getUserName();
        if (userName == null || userName.isEmpty()) {
            userName = callContact.getDisplayName();
        }

        SmartListViewModel smartListViewModel = new SmartListViewModel(callContact.getIds().get(0),
                callContact.getStatus(),
                tuple != null && !tuple.first.contains(CallContact.PREFIX_RING) ? tuple.first : userName,
                tuple != null ? tuple.second : null,
                0,
                0,
                "",
                false);

        smartListViewModel.setOnline(mPresenceService.isBuddyOnline(callContact.getIds().get(0)));
        return smartListViewModel;
    }

    private SmartListViewModel createViewModelFromConversation(final ConversationModel conversationModel, @NonNull CallContact callContact) throws SQLException {
        Tuple<String, byte[]> tuple = mContactService.loadContactData(callContact);

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

        String userName = callContact.getUserName();
        if (userName == null || userName.isEmpty()) {
            userName = callContact.getDisplayName();
        }

        SmartListViewModel smartListViewModel = new SmartListViewModel(conversationModel.getContactId(),
                callContact.getStatus(),
                tuple != null && !tuple.first.contains(CallContact.PREFIX_RING) ? tuple.first : userName,
                tuple != null ? tuple.second : null,
                lastInteractionLong,
                lastEntryType,
                lastInteraction,
                hasUnreadMessage);


        smartListViewModel.setOnline(mPresenceService.isBuddyOnline(callContact.getIds().get(0)));
        return smartListViewModel;
    }

    public void refresh() {
        refreshConnectivity();
        init();
        getView().hideSearchRow();
    }

    private void refreshConnectivity() {
        boolean mobileDataAllowed = mPreferencesService.getUserSettings().isAllowMobileData();

        boolean isConnected = mDeviceRuntimeService.isConnectedWifi()
                || (mDeviceRuntimeService.isConnectedMobile() && mobileDataAllowed);

        boolean isMobileAndNotAllowed = mDeviceRuntimeService.isConnectedMobile()
                && !mobileDataAllowed;

        if (isConnected) {
            getView().hideErrorPanel();
        } else {
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
                    mCallContact = CallContact.buildUnknown(query, null);
                    getView().displayNewContactRowWithName(query);
                } else {
                    getView().hideSearchRow();
                }

                // Ring search
                if (mBlockchainInputHandler == null) {
                    mBlockchainInputHandler = new BlockchainInputHandler(new WeakReference<>(mAccountService));
                }

                // searching for a ringId or a blockchained username
                if (!mBlockchainInputHandler.isAlive()) {
                    mBlockchainInputHandler = new BlockchainInputHandler(new WeakReference<>(mAccountService));
                }

                mBlockchainInputHandler.enqueueNextLookup(query);
                mLastBlockchainQuery = query;
            }
        }

        getView().updateList(filter(mSmartListViewModels, query));
        getView().setLoading(false);
    }

    public void newContactClicked() {
        if (mCallContact == null) {
            return;
        }
        startConversation(mCallContact);
    }

    public void conversationClicked(SmartListViewModel smartListViewModel) {
        startConversation(mContactService.getContact(new Uri(smartListViewModel.getUuid()),
                mCurrentAccount.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN)));
    }

    //TODO
    public void conversationLongClicked(SmartListViewModel smartListViewModel) {
/*        Conversation conversation = getConversationByUuid(mConversations, smartListViewModel.getUuid());
        if (conversation != null) {
            getView().displayConversationDialog(conversation);
        }*/
    }

    public void photoClicked(SmartListViewModel smartListViewModel) {
        getView().goToContact(mContactService.getContact(new Uri(smartListViewModel.getUuid()),
                mCurrentAccount.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN)));
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

        for (SmartListViewModel smartListViewModel : mSmartListViewModels) {
            boolean isOnline = mPresenceService.isBuddyOnline(smartListViewModel.getUuid());
            if (smartListViewModel.isOnline() != isOnline) {
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
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayNewContactRowWithName(name);
                    mLastBlockchainQuery = null;
                } else {
                    if (name.equals("") || address.equals("")) {
                        return;
                    }
                    getView().hideSearchRow();
                    mConversationFacade.updateConversationContactWithRingId(name, address);
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
    }

    public void removeContact(String accountId, String contactId) {
        String[] split = contactId.split(":");
        if (split.length > 1 && split[0].equals("ring")) {
            mContactService.removeContact(accountId, split[1]);
        }
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
            } else {
                Log.i(TAG, "Trying to subscribe to an invalid uri " + key);
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
            case CONVERSATIONS_CHANGED:
            case INCOMING_CALL:
                mCurrentAccount = mAccountService.getCurrentAccount();
                loadConversations(mCurrentAccount.getAccountID());
                getView().scrollToTop();
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
                    break;
            }
        }

        if (observable instanceof PresenceService) {
            switch (event.getEventType()) {
                case NEW_BUDDY_NOTIFICATION:
                    updatePresence();
                    break;
            }
        }
    }
}
