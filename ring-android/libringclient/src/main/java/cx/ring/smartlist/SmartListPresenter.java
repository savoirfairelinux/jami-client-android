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
import java.util.Comparator;
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
import io.reactivex.observers.DisposableCompletableObserver;
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

    private String currentAccount;

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
        currentAccount = mAccountService.getCurrentAccount().getAccountID();
        loadConversations(currentAccount);
    }

    private void loadConversations(final String accountId) {
        if (mSmartListViewModels == null) {
            mSmartListViewModels = new ArrayList<>();
        }
        mSmartListViewModels.clear();

        Account account = mAccountService.getAccount(accountId);
        boolean acceptAllMessages = account.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN);

        compositeDisposable.add(mHistoryService.getConversationsForAccount(accountId)
                .zipWith(mContactService.loadContacts(acceptAllMessages),
                        new BiFunction<List<ConversationModel>, List<CallContact>, List<SmartListViewModel>>() {
                            @Override
                            public List<SmartListViewModel> apply(@NonNull List<ConversationModel> conversationModels, @NonNull List<CallContact> callContacts) throws Exception {
                                for (CallContact callContact : callContacts) {
                                    boolean found = false;
                                    SmartListViewModel smartListViewModel;
                                    for (ConversationModel conversationModel : conversationModels) {
                                        if (callContact.getPhones().get(0).getNumber().toString().equals(conversationModel.getContactId())) {
                                            smartListViewModel = createViewModelFromConversation(conversationModel);
                                            if (mSmartListViewModels != null && !mSmartListViewModels.contains(smartListViewModel)) {
                                                mSmartListViewModels.add(smartListViewModel);
                                            }
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        smartListViewModel = createViewModelFromContact(callContact);
                                        if (mSmartListViewModels != null && !mSmartListViewModels.contains(smartListViewModel)) {
                                            mSmartListViewModels.add(smartListViewModel);
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
                            Collections.sort(mSmartListViewModels, new Comparator<SmartListViewModel>() {
                                @Override
                                public int compare(SmartListViewModel lhs, SmartListViewModel rhs) {
                                    return (int) ((rhs.getLastInteractionTime() - lhs.getLastInteractionTime()) / 1000l);
                                }
                            });

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

    private SmartListViewModel createViewModelFromContact(CallContact callContact) {
        Tuple<String, byte[]> tuple = mContactService.loadContactData(callContact);

        SmartListViewModel smartListViewModel = new SmartListViewModel(callContact.getIds().get(0),
                callContact,
                tuple.first,
                tuple.second);

        smartListViewModel.setOnline(mPresenceService.isBuddyOnline(callContact.getIds().get(0)));
        return smartListViewModel;
    }

    private SmartListViewModel createViewModelFromConversation(final ConversationModel conversationModel) throws SQLException {
        CallContact callContact = mContactService.getContact(new Uri(conversationModel.getContactId()));
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

        SmartListViewModel smartListViewModel = new SmartListViewModel(conversationModel.getContactId(),
                conversationModel.getId(),
                callContact,
                tuple.first,
                tuple.second,
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
        searchForRingIdInBlockchain();
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
        mContactService.addContact(c);
        getView().goToConversation(c);
    }

    public void copyNumber(SmartListViewModel smartListViewModel) {
        CallContact callContact = mContactService.getContact(new Uri(smartListViewModel.getUuid()));
        getView().copyNumber(callContact);
    }

    public void deleteConversation(SmartListViewModel smartListViewModel) {
        CallContact callContact = mContactService.getContact(new Uri(smartListViewModel.getUuid()));
        getView().displayDeleteDialog(callContact);
    }

    public void deleteConversation(CallContact callContact) {
        compositeDisposable.add(mHistoryService.clearHistoryForContactAndAccount(callContact.getIds().get(0), currentAccount)
                .subscribeOn(Schedulers.computation())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        //TODO
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

    private void searchForRingIdInBlockchain() {
        List<Conversation> conversations = mConversationFacade.getConversationsList();
        for (Conversation conversation : conversations) {
            CallContact contact = conversation.getContact();
            if (contact == null) {
                continue;
            }

            Uri contactUri = new Uri(contact.getIds().get(0));
            if (!contactUri.isRingId()) {
                continue;
            }

            if (contact.getPhones().isEmpty()) {
                mAccountService.lookupName("", "", contact.getDisplayName());
            } else {
                Phone phone = contact.getPhones().get(0);
                if (phone.getNumber().isRingId()) {
                    mAccountService.lookupAddress("", "", phone.getNumber().getHost());
                }
            }
        }
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
        for (SmartListViewModel smartListViewModel : mSmartListViewModels) {
            if (smartListViewModel.getUuid().contains(contactId)) {
                if (!smartListViewModel.getContactName().equals(contactName)) {
                    smartListViewModel.setContactName(contactName);
                    getView().updateList(mSmartListViewModels);
                }
                break;
            }
        }
    }

    private void updatePresence() {
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

    public void removeContact(SmartListViewModel smartListViewModel) {
        String contactId = smartListViewModel.getUuid();
        String[] split = contactId.split(":");
        if (split.length > 1 && split[0].equals("ring")) {
            contactId = split[1];
        }

        mContactService.removeContact(currentAccount, contactId);
        mSmartListViewModels.remove(smartListViewModel);
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
            case HISTORY_LOADED:
                searchForRingIdInBlockchain();
                break;
            case CONVERSATIONS_CHANGED:
            case INCOMING_CALL:
                currentAccount = mAccountService.getCurrentAccount().getAccountID();
                loadConversations(currentAccount);
                getView().scrollToTop();
                break;
            case CONTACTS_CHANGED:
                currentAccount = mAccountService.getCurrentAccount().getAccountID();
                loadConversations(currentAccount);
                break;
        }

        if (observable instanceof HistoryService) {
            switch (event.getEventType()) {
                case INCOMING_MESSAGE:
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    if (txt.getAccount().equals(currentAccount)) {
                        loadConversations(currentAccount);
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
