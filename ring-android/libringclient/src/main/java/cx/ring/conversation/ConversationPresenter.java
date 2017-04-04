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
package cx.ring.conversation;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.daemon.Blob;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.HistoryService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public class ConversationPresenter extends RootPresenter<ConversationView> implements Observer<ServiceEvent> {

    private ContactService mContactService;
    private AccountService mAccountService;
    private ConversationFacade mConversationFacade;
    private HistoryService mHistoryService;

    private Conversation mConversation;
    private String mConversationId;
    private Uri mPreferredNumber;

    private boolean hasContactRequestPopupShown = false;

    @Inject
    public ConversationPresenter(ContactService mContactService,
                                 AccountService mAccountService,
                                 ConversationFacade mConversationFacade,
                                 HistoryService mHistoryService) {
        this.mContactService = mContactService;
        this.mAccountService = mAccountService;
        this.mConversationFacade = mConversationFacade;
        this.mHistoryService = mHistoryService;
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

    public void init(String conversationId, Uri number) {
        mConversationId = conversationId;
        mPreferredNumber = number;

        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
    }

    public void pause() {
        if (mConversation != null) {
            mConversationFacade.readConversation(mConversation);
            mConversation.setVisible(false);
        }
    }

    public void resume() {
        if (mConversation != null) {
            mConversation.setVisible(true);
            mConversationFacade.readConversation(mConversation);
        }
        loadConversation();
        if (!hasContactRequestPopupShown) {
            checkContact();
            hasContactRequestPopupShown = true;
        }
    }

    public void prepareMenu() {
        getView().displayAddContact(mConversation != null && mConversation.getContact().getId() < 0);
    }

    public void addContact() {
        getView().goToAddContact(mConversation.getContact());
    }

    public void deleteAction() {
        getView().displayDeleteDialog(mConversation);
    }

    public void copyToClipboard() {
        getView().displayCopyToClipboard(mConversation.getContact());
    }

    public void sendTextMessage(String message, Uri number) {
        if (message != null && !message.equals("")) {
            getView().clearMsgEdit();
            Conference conference = mConversation == null ? null : mConversation.getCurrentCall();
            if (conference == null || !conference.isOnGoing()) {
                Tuple<Account, Uri> guess = guess(number);
                if (guess == null || guess.first == null) {
                    return;
                }
                mConversationFacade.sendTextMessage(guess.first.getAccountID(), guess.second, message);
            } else {
                mConversationFacade.sendTextMessage(conference, message);
            }
        }
    }

    public void sendTrustRequest(String accountId, String contactId, VCard vCard) {
        mAccountService.sendTrustRequest(accountId, contactId, Blob.fromString(VCardUtils.vcardToString(vCard)));
    }

    public void blockContact() {
        Tuple<Account, Uri> guess = guess(mPreferredNumber);
        if (guess == null || guess.first == null || guess.second == null || !guess.first.isRing() || !guess.second.isRingId()) {
            return;
        }

        String accountId = guess.first.getAccountID();
        String contactId = guess.second.getRawUriString();

        String[] split = contactId.split(":");
        if (split.length > 1) {
            contactId = split[1];
        }

        mContactService.removeContact(accountId, contactId);
        getView().goToHome();
    }

    public void clickOnGoingPane() {
        getView().goToCallActivity(mConversation.getCurrentCall().getId());
    }

    public void callWithVideo(boolean video, Uri number) {
        if (number == null) {
            number = mPreferredNumber;
        }

        Conference conf = mConversation.getCurrentCall();
        if (conf != null) {
            getView().goToCallActivity(conf.getId());
        } else {
            Tuple<Account, Uri> guess = guess(number);
            if (guess != null && guess.first != null) {
                getView().goToCallActivityWithResult(guess, video);
            }
        }
    }

    public void deleteConversation() {
        mHistoryService.clearHistoryForConversation(mConversation);
        getView().goToHome();
    }

    private void loadConversation() {
        long contactId = CallContact.contactIdFromId(mConversationId);
        CallContact contact = null;
        if (contactId >= 0) {
            contact = mContactService.findContactById(contactId);
        }
        if (contact == null) {
            Uri convUri = new Uri(mConversationId);
            if (!mPreferredNumber.isEmpty()) {
                contact = mContactService.findContactByNumber(mPreferredNumber.getRawUriString());
                if (contact == null) {
                    contact = CallContact.buildUnknown(convUri);
                }
            } else {
                contact = mContactService.findContactByNumber(convUri.getRawUriString());
                if (contact == null) {
                    contact = CallContact.buildUnknown(convUri);
                    mPreferredNumber = contact.getPhones().get(0).getNumber();
                } else {
                    mPreferredNumber = convUri;
                }
            }
        }
        mConversation = mConversationFacade.startConversation(contact);

        Tuple<String, byte[]> contactData = mContactService.loadContactData(mConversation.getContact());
        if (contactData != null) {
            getView().displayContactPhoto(contactData.second);
        }

        if (!mConversation.getContact().getPhones().isEmpty()) {
            contact = mContactService.getContact(mConversation.getContact().getPhones().get(0).getNumber());
            if (contact != null) {
                mConversation.setContact(contact);
            }
            getView().displayContactName(mConversation.getContact().getDisplayName());
        }

        getView().displayOnGoingCallPane(mConversation.getCurrentCall() == null);

        if (mConversation.getContact().getPhones().size() > 1) {
            for (Phone phone : mConversation.getContact().getPhones()) {
                if (phone.getNumber() != null && phone.getNumber().isRingId()) {
                    mAccountService.lookupAddress("", "", phone.getNumber().getRawUriString());
                }
            }
            if (mPreferredNumber == null || mPreferredNumber.isEmpty()) {
                mPreferredNumber = new Uri(
                        mConversation.getLastNumberUsed(mConversation.getLastAccountUsed())
                );
            }
            getView().displayNumberSpinner(mConversation, mPreferredNumber);
        } else {
            getView().hideNumberSpinner();
            mPreferredNumber = mConversation.getContact().getPhones().get(0).getNumber();
        }

        getView().refreshView(mConversation, mPreferredNumber);
    }

    private void checkContact() {
        Tuple<Account, Uri> guess = guess(mPreferredNumber);
        if (guess == null || guess.first == null || guess.second == null || !guess.first.isRing() || !guess.second.isRingId()) {
            return;
        }

        String accountId = guess.first.getAccountID();
        Uri contactUri = guess.second;

        String contactId = contactUri.getRawUriString();
        String[] split = contactId.split(":");
        if (split.length > 1) {
            contactId = split[1];
        }

        List<Map<String, String>> contacts = mContactService.getContacts(accountId);
        for (Map<String, String> contact : contacts) {
            if (contact.get("id").equals(contactId)
                    && contact.containsKey("confirmed")
                    && contact.get("confirmed").equals("true")) {
                return;
            }
        }

        getView().displaySendTrustRequest(accountId, contactId);
    }

    /**
     * Guess account and number to use to initiate a call
     */
    private Tuple<Account, Uri> guess(Uri number) {
        if (mConversation == null) {
            return null;
        }
        Account account = mAccountService.getAccount(mConversation.getLastAccountUsed());

        // Guess account from number
        if (account == null && number != null) {
            account = mAccountService.guessAccount(number);
        }

        // Guess number from account/call history
        if (account != null && number == null) {
            number = new Uri(mConversation.getLastNumberUsed(account.getAccountID()));
        }

        // If no account found, use first active
        if (account == null) {
            List<Account> accounts = mAccountService.getAccounts();
            if (accounts.isEmpty()) {
                return null;
            } else
                account = accounts.get(0);
        }

        // If no number found, use first from contact
        if (number == null || number.isEmpty()) {
            number = mConversation.getContact().getPhones().get(0).getNumber();
        }

        return new Tuple<>(account, number);
    }


    @Override
    public void update(Observable observable, ServiceEvent arg) {
        if (observable instanceof AccountService && arg != null) {
            if (arg.getEventType() == ServiceEvent.EventType.REGISTERED_NAME_FOUND) {
                final String name = arg.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                final String address = arg.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                final int state = arg.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);

                getView().updateView(address, name, state);
            }
        } else if (observable instanceof ConversationFacade && arg != null) {
            switch (arg.getEventType()) {
                case INCOMING_MESSAGE:
                case HISTORY_LOADED:
                case CALL_STATE_CHANGED:
                case CONVERSATIONS_CHANGED:
                    loadConversation();
                    break;
            }
        }
    }
}