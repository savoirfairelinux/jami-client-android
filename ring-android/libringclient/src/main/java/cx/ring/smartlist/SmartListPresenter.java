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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import cx.ring.services.HistoryService;
import cx.ring.services.SettingsService;
import cx.ring.utils.BlockchainInputHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;

public class SmartListPresenter extends RootPresenter<SmartListView> implements Observer<ServiceEvent> {

    private AccountService mAccountService;

    private ContactService mContactService;

    private HistoryService mHistoryService;

    private SettingsService mSettingsService;

    private ConversationFacade mConversationFacade;

    private BlockchainInputHandler mBlockchainInputHandler;
    private String mLastBlockchainQuery = null;

    private ArrayList<Conversation> mConversations;
    private ArrayList<SmartListViewModel> mSmartListViewModels;

    @Inject
    public SmartListPresenter(AccountService accountService, ContactService contactService,
                              HistoryService historyService, ConversationFacade conversationFacade,
                              SettingsService settingsService) {
        this.mAccountService = accountService;
        this.mContactService = contactService;
        this.mHistoryService = historyService;
        this.mSettingsService = settingsService;
        this.mConversationFacade = conversationFacade;
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mConversationFacade.removeObserver(this);
    }

    @Override
    public void bindView(SmartListView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
    }

    public void refresh(boolean isConnectedWifi, boolean isConnectedMobile) {
        boolean isConnected = isConnectedWifi
                || (isConnectedMobile && mSettingsService.getUserSettings().isAllowMobileData());

        boolean isMobileAndNotAllowed = isConnectedMobile
                && !mSettingsService.getUserSettings().isAllowMobileData();

        if (isConnected) {
            getView().hideErrorPanel();
        } else {
            if (isMobileAndNotAllowed) {
                getView().displayMobileDataPanel();
            } else {
                getView().displayNetworkErrorPanel();
            }
        }

        mConversationFacade.refreshConversations();
        searchForRingIdInBlockchain();
        getView().hideSearchRow();
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
                getView().displayNewContactRowWithName(query, null);
            } else {

                Uri uri = new Uri(query);
                if (uri.isRingId()) {
                    getView().displayNewContactRowWithName(query, null);
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

        getView().updateView(filter(mSmartListViewModels, query));
        getView().setLoading(false);
    }

    public void newContactClicked(CallContact callContact) {
        if (callContact == null) {
            return;
        }
        startConversation(callContact);
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

    public void quickCallClicked(CallContact callContact) {
        if (callContact != null) {
            if (callContact.getPhones().size() > 1) {
                CharSequence numbers[] = new CharSequence[callContact.getPhones().size()];
                int i = 0;
                for (Phone p : callContact.getPhones()) {
                    numbers[i++] = p.getNumber().getRawUriString();
                }

                getView().displayChooseNumberDialog(numbers);
            } else {
                getView().goToCallActivity(callContact.getPhones().get(0).getNumber().getRawUriString());
            }
        }
    }

    public void startConversation(CallContact c) {
        mContactService.addContact(c);
        getView().goToConversation(c);
    }

    public void deleteConversation(Conversation conversation) {
        mHistoryService.clearHistoryForConversation(conversation);
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
            if (contactUri.isRingId()) {
                return;
            }

            if (contact.getPhones().isEmpty()) {
                mAccountService.lookupName("", "", contact.getDisplayName());
            } else {
                Phone phone = contact.getPhones().get(0);
                if (!phone.getNumber().isRingId()) {
                    mAccountService.lookupName("", "", contact.getDisplayName());
                }
            }
        }
    }

    private synchronized void displayConversations() {
        if (mConversations == null) {
            mConversations = new ArrayList<>();
        }
        mConversations.clear();
        mConversations.addAll(mConversationFacade.getConversationsList());
        if (mConversations != null && mConversations.size() > 0) {
            if (mSmartListViewModels == null) {
                mSmartListViewModels = new ArrayList<>();
            }
            for (int i = 0; i < mConversations.size(); i++) {
                Conversation conversation = mConversations.get(i);
                SmartListViewModel smartListViewModel = getSmartListViewModelByUuid(mSmartListViewModels, conversation.getUuid());

                if (smartListViewModel == null || i >= mSmartListViewModels.size()) {

                    if(conversation.getContact().isFromSystem()) {
                        Tuple<String, String> tuple = mContactService.loadContactDataFromSystem(conversation.getContact());
                        smartListViewModel = new SmartListViewModel(conversation, tuple.first, tuple.second, null);
                    } else {
                        Tuple<String, byte[]> tuple = mContactService.loadContactData(conversation.getContact());
                        smartListViewModel = new SmartListViewModel(conversation, tuple.first, null, tuple.second);
                    }

                    mSmartListViewModels.add(smartListViewModel);
                } else {
                    if(conversation.getContact().isFromSystem()) {
                        Tuple<String, String> tuple = mContactService.loadContactDataFromSystem(conversation.getContact());
                        smartListViewModel.update(conversation, tuple.first, tuple.second, null);
                    } else {
                        Tuple<String, byte[]> tuple = mContactService.loadContactData(conversation.getContact());
                        smartListViewModel.update(conversation, tuple.first, null, tuple.second);
                    }
                }
            }

            Collections.sort(mSmartListViewModels, new Comparator<SmartListViewModel>() {
                @Override
                public int compare(SmartListViewModel lhs, SmartListViewModel rhs) {
                    return (int) ((rhs.getLastInteractionTime() - lhs.getLastInteractionTime()) / 1000l);
                }
            });

            getView().updateView(mSmartListViewModels);
            getView().hideNoConversationMessage();
            getView().setLoading(false);
        } else {
            getView().displayNoConversationMessage();
            getView().setLoading(false);
        }
    }

    private ArrayList<SmartListViewModel> filter(ArrayList<SmartListViewModel> list, String query) {
        ArrayList<SmartListViewModel> filteredList = new ArrayList<>();
        if(list == null || list.size() == 0) {
            return filteredList;
        }
        for(SmartListViewModel smartListViewModel : list) {
            if(smartListViewModel.getContactName().toLowerCase().contains(query.toLowerCase())) {
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

    private SmartListViewModel getSmartListViewModelByUuid(ArrayList<SmartListViewModel> smartListViewModels, String uuid) {
        for (SmartListViewModel smartListViewModel : smartListViewModels) {
            if (smartListViewModel.getUuid().equals(uuid)) {
                return smartListViewModel;
            }
        }
        return null;
    }

    private void parseEventState(String name, String address, int state) {
        switch (state) {
            case 0:
                // on found
                if (mLastBlockchainQuery != null && mLastBlockchainQuery.equals(name)) {
                    getView().displayNewContactRowWithName(name, address);
                    mLastBlockchainQuery = null;
                } else {
                    if (name.equals("") || address.equals("")) {
                        return;
                    }
                    getView().hideSearchRow();
                    mConversationFacade.updateConversationContactWithRingId(name, address);
                    displayConversations();
                }
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()
                        && mLastBlockchainQuery != null
                        && mLastBlockchainQuery.equals(name)) {
                    getView().displayNewContactRowWithName(name, null);
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
                    getView().displayNewContactRowWithName(address, null);
                } else {
                    getView().hideSearchRow();
                }
                break;
        }
    }

    public void removeContact(String accountId, String contactId) {
        mContactService.removeContact(accountId, contactId);
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
            case INCOMING_MESSAGE:
            case HISTORY_LOADED:
            case CONVERSATIONS_CHANGED:
                displayConversations();
                break;
        }
    }
}
