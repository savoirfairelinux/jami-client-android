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

import javax.inject.Inject;

import cx.ring.daemon.Blob;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
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
import cx.ring.services.HistoryService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.observers.ResourceSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class ConversationPresenter extends RootPresenter<ConversationView> implements Observer<ServiceEvent> {

    private static final String TAG = ConversationPresenter.class.getSimpleName();
    private ContactService mContactService;
    private AccountService mAccountService;
    private ConversationFacade mConversationFacade;
    private HistoryService mHistoryService;
    private CallService mCallService;

    private Conversation mConversation;
    private String mContactRingId;
    private Uri mPreferredNumber;

    private boolean hasContactRequestPopupShown = false;

    @Inject
    public ConversationPresenter(ContactService mContactService,
                                 AccountService mAccountService,
                                 ConversationFacade mConversationFacade,
                                 HistoryService mHistoryService,
                                 CallService callService) {
        this.mContactService = mContactService;
        this.mAccountService = mAccountService;
        this.mConversationFacade = mConversationFacade;
        this.mHistoryService = mHistoryService;
        this.mCallService = callService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mConversationFacade.removeObserver(this);
        mHistoryService.removeObserver(this);
    }

    public void init(String conversationId, Uri number) {
        mContactRingId = conversationId;
        mPreferredNumber = number;

        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
        mHistoryService.addObserver(this);
    }

    public void pause() {
        if (mConversation != null) {
            mConversationFacade.readConversation(mConversation);
            mConversation.setVisible(false);
        }
    }

    public void resume() {
        loadHistory();
        if (mConversation != null) {
            mConversation.setVisible(true);
            mConversationFacade.readConversation(mConversation);
        }
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

    public void sendTextMessage(String message) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();

        if (message != null && !message.equals("")) {
            getView().clearMsgEdit();
            Conference conference = mConversation == null ? null : mConversation.getCurrentCall();
            TextMessage txtMessage;
            if (conference == null || !conference.isOnGoing()) {
                long id = mCallService.sendAccountTextMessage(accountId, mContactRingId, message);
                txtMessage = new TextMessage(false, message, new Uri(mContactRingId), null, accountId);
                txtMessage.setID(id);
            } else {
                mCallService.sendTextMessage(conference.getId(), message);
                SipCall call = conference.getParticipants().get(0);
                txtMessage = new TextMessage(false, message, call.getNumberUri(), conference.getId(), call.getAccount());
            }
            txtMessage.read();
            mHistoryService.insertNewTextMessage(txtMessage);
            mConversation.addTextMessage(txtMessage);
            getView().refreshView(mConversation);
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
        String contactId = guess.second.getRawRingId();

        mAccountService.removeContact(accountId, contactId, true);
        getView().goToHome();
    }

    public void clickOnGoingPane() {
        Conference conf = mConversation.getCurrentCall();
        if (conf != null) {
            getView().goToCallActivity(conf.getId());
        } else {
            resume();
        }
    }

    public void callWithVideo(boolean video, Uri number) {
        if (number == null) {
            number = mPreferredNumber;
        }

        Conference conf = mConversation.getCurrentCall();

        if (conf != null && (conf.getParticipants().get(0).getCallState() == SipCall.State.INACTIVE
                || conf.getParticipants().get(0).getCallState() == SipCall.State.FAILURE)) {
            mConversation.removeConference(conf);
            conf = null;
        }

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

    private void loadHistory() {
        String accountId = mAccountService.getCurrentAccount().getAccountID();


        mHistoryService.getAllTextMessagesForAccountAndContactRingId(accountId, mContactRingId)
                .zipWith(mHistoryService.getAllCallsForAccountAndContactRingId(accountId, mContactRingId),
                        new BiFunction<List<HistoryText>, List<HistoryCall>, Conversation>() {
                            @Override
                            public Conversation apply(@NonNull List<HistoryText> historyTexts, @NonNull List<HistoryCall> historyCalls) throws Exception {
                                CallContact callContact = mContactService.getContact(new Uri(mContactRingId));
                                Conversation conversation = new Conversation(callContact);

                                for (HistoryCall call : historyCalls) {
                                    conversation.addHistoryCall(call);
                                }

                                for (HistoryText htext : historyTexts) {
                                    TextMessage msg = new TextMessage(htext);
                                    conversation.addTextMessage(msg);
                                }

                                return conversation;
                            }
                        })
                .subscribeOn(Schedulers.computation())
                .subscribeWith(new ResourceSingleObserver<Conversation>() {
                    @Override
                    public void onSuccess(@NonNull Conversation conversation) {
                        mConversation = conversation;

                        SipCall sipCall = mCallService.getCurrentCallForContactId(mContactRingId);
                        if (sipCall != null && sipCall.getCallState() != SipCall.State.INACTIVE) {
                            mConversation.addConference(new Conference(sipCall));
                            getView().displayOnGoingCallPane(true);
                        } else {
                            mConversation.removeConference(mConversation.getCurrentCall());
                            getView().displayOnGoingCallPane(false);
                        }

                        getView().refreshView(conversation);
                        if (!hasContactRequestPopupShown) {
                            checkContact();
                            hasContactRequestPopupShown = true;
                        }

                        getView().displayContactPhoto(mConversation.getContact().getPhoto());
                        getView().displayContactName(mConversation.getContact());

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
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, e.toString());
                    }
                });
    }

    private void checkContact() {
        Tuple<Account, Uri> guess = guess(mPreferredNumber);
        long time = System.currentTimeMillis();
        if (guess == null
                || guess.first == null || guess.second == null
                || !guess.first.isRing() || !guess.second.isRingId()
                || guess.first.getContact(guess.second.getRawRingId()) != null
                || mConversation.getLastContactRequest() + Conversation.PERIOD > time) {
            return;
        }

        String accountId = guess.first.getAccountID();
        Uri contactUri = guess.second;

        CallContact contact = mContactService.findContact(contactUri);
        if (contact != null && CallContact.Status.CONFIRMED.equals(contact.getStatus())) {
            return;
        }

        mConversation.setLastContactRequest(time);
        getView().displaySendTrustRequest(accountId, contactUri.getRawRingId());
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
    public void update(Observable observable, ServiceEvent event) {
        if (observable instanceof AccountService && event != null) {
            if (event.getEventType() == ServiceEvent.EventType.REGISTERED_NAME_FOUND) {
                final String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                final String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                final int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);

                getView().updateView(address, name, state);
            }
        } else if (observable instanceof HistoryService && event != null) {
            switch (event.getEventType()) {
                case INCOMING_MESSAGE:
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    mConversation.addTextMessage(txt);
                    getView().refreshView(mConversation);
                    break;
            }
        } else if (observable instanceof ConversationFacade && event != null) {
            switch (event.getEventType()) {
                case INCOMING_CALL:
                case CALL_STATE_CHANGED:
                    loadHistory();
                    break;
            }
        }
    }
}