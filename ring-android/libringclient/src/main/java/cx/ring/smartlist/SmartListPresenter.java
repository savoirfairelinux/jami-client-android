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
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HistoryService;
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.utils.NameLookupInputHandler;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class SmartListPresenter extends RootPresenter<SmartListView> implements Observer<ServiceEvent> {

    private static final String TAG = SmartListPresenter.class.getSimpleName();

    private AccountService mAccountService;

    private ContactService mContactService;

    private HistoryService mHistoryService;

    private PreferencesService mPreferencesService;

    private ConversationFacade mConversationFacade;

    private PresenceService mPresenceService;

    private DeviceRuntimeService mDeviceRuntimeService;

    private NameLookupInputHandler mNameLookupInputHandler;
    private String mLastBlockchainQuery = null;

    private final ArrayList<Conversation> mConversations = new ArrayList<>();
    private ArrayList<SmartListViewModel> mSmartListViewModels;

    private CallContact mCallContact;

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
        mPresenceService.removeObserver(this);
    }

    @Override
    public void bindView(SmartListView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
        mPresenceService.addObserver(this);
    }

    public void refresh() {
        refreshConnectivity();
        subscribePresence();
        getView().hideSearchRow();
        displayConversations();
    }

    private void refreshConnectivity() {
        boolean mobileDataAllowed = mPreferencesService.getUserSettings().isAllowMobileData();

        boolean isConnected = mDeviceRuntimeService.isConnectedWifi()
                || mDeviceRuntimeService.isConnectedEthernet()
                || mDeviceRuntimeService.isConnectedBluetooth()
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
        Conversation conversation = getConversationByUuid(mConversations, smartListViewModel.getUuid());
        if (conversation != null && conversation.getContact() != null) {
            startConversation(conversation.getContact());
        }
    }

    public void conversationLongClicked(SmartListViewModel smartListViewModel) {
        Conversation conversation = getConversationByUuid(mConversations, smartListViewModel.getUuid());
        if (conversation != null) {
            getView().displayConversationDialog(conversation);
        }
    }

    public void photoClicked(SmartListViewModel smartListViewModel) {
        Conversation conversation = getConversationByUuid(mConversations, smartListViewModel.getUuid());
        if (conversation != null && conversation.getContact() != null) {
            getView().goToContact(conversation.getContact());
        }
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

    private synchronized void displayConversations() {
        mSmartListViewModels = new ArrayList<>(mConversations.size());
        mConversations.clear();
        mConversations.addAll(mConversationFacade.getConversationsList());
        if (!mConversations.isEmpty()) {
            for (Conversation conversation : mConversations) {
                SmartListViewModel smartListViewModel;
                CallContact contact = conversation.getContact();

                mContactService.loadContactData(contact);
                smartListViewModel = new SmartListViewModel(conversation,
                            contact.getDisplayName(),
                            contact.getPhoto());
                smartListViewModel.setOnline(mPresenceService.isBuddyOnline(contact.getIds().get(0)));
                mSmartListViewModels.add(smartListViewModel);
            }
            getView().updateList(mSmartListViewModels);
            getView().hideNoConversationMessage();
            getView().setLoading(false);
        } else {
            getView().displayNoConversationMessage();
            getView().setLoading(false);
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

    private Conversation getConversationByUuid(ArrayList<Conversation> conversations, String uuid) {
        for (Conversation conversation : conversations) {
            if (conversation.getUuid().equals(uuid)) {
                return conversation;
            }
        }
        return null;
    }

    private void parseEventState(String name, String address, int state) {
        switch (state) {
            case 0:
                // on found
                if (mLastBlockchainQuery != null && mLastBlockchainQuery.equals(name)) {
                    mCallContact = CallContact.buildRingContact(new Uri(address), name);
                    getView().displayNewContactRowWithName(name);
                    mLastBlockchainQuery = null;
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
            case CONVERSATIONS_CHANGED:
                displayConversations();
                getView().scrollToTop();
                break;
            case USERNAME_CHANGED:
                displayConversations();
                break;
        }

        if (observable instanceof PresenceService) {
            switch (event.getEventType()) {
                case NEW_BUDDY_NOTIFICATION:
                    displayConversations();
                    break;
            }
        }
    }
}
