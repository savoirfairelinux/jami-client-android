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

    public static final String TAG = ConversationPresenter.class.getSimpleName();

    private ContactService mContactService;
    private AccountService mAccountService;
    private HistoryService mHistoryService;
    private CallService mCallService;

    private Conversation mConversation;

    private String mAccountId;
    private String mContactId;
    private long mConversationId;
    private Uri mPreferredNumber;

    private boolean hasContactRequestPopupShown = false;

    @Inject
    public ConversationPresenter(ContactService contactService,
                                 AccountService accountService,
                                 HistoryService historyService,
                                 CallService callService) {
        this.mContactService = contactService;
        this.mAccountService = accountService;
        this.mHistoryService = historyService;
        this.mCallService = callService;
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void bindView(ConversationView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mCallService.addObserver(this);
        mHistoryService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mCallService.removeObserver(this);
        mHistoryService.removeObserver(this);
    }

    public void init(String accountId, String contactId, long conversationId) {
        mAccountId = accountId;
        mContactId = contactId;
        if (conversationId != 0L) {
            mConversationId = conversationId;
        } else {
            mConversationId = mHistoryService.getConversationID(accountId, contactId);
        }
    }

    public void pause() {
        if (mConversation != null) {
            mConversation.setVisible(false);
        }
    }

    public void resume() {
        loadHistory(mConversationId);
        if (mConversation != null) {
            mConversation.setVisible(true);
        }
    }

    public void prepareMenu() {
        getView().displayAddContact(mConversation != null && mConversation.getContact().getId() < 0);
    }

    public void addContact() {
        getView().goToAddContact(mConversation.getContact());
    }

    public void deleteAction() {
        getView().displayDeleteDialog(mConversation.getContact());
    }

    public void copyToClipboard() {
        getView().displayCopyToClipboard(mConversation.getContact());
    }

    public void sendTextMessage(String message) {
        if (message != null && !message.equals("")) {
            getView().clearMsgEdit();
            Conference conference = mConversation == null ? null : mConversation.getCurrentCall();
            TextMessage txtMessage;
            if (conference == null || !conference.isOnGoing()) {
                long id = mCallService.sendAccountTextMessage(mAccountId, mContactId, message);
                txtMessage = new TextMessage(false, message, new Uri(mContactId), null, mAccountId, mConversationId);
                txtMessage.setID(id);
            } else {
                mCallService.sendTextMessage(conference.getId(), message);
                SipCall call = conference.getParticipants().get(0);
                long conversationID = mHistoryService.getConversationID(call.getAccount(), call.getNumber());
                txtMessage = new TextMessage(false, message, call.getNumberUri(), conference.getId(), call.getAccount(), conversationID);
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

        String[] split = mContactId.split(":");
        String splitId = mContactId;
        if (split.length > 1) {
            splitId = split[1];
        }

        mContactService.removeContact(mAccountId, splitId, true);
        getView().goToHome();
    }

    public void clickOnGoingPane() {
        getView().goToCallActivity(mConversation.getCurrentCall().getId());
    }

    public void callWithVideo(boolean video) {
        Conference conf = mConversation.getCurrentCall();

        if (conf != null && conf.getParticipants().get(0).getCallState() == SipCall.State.INACTIVE) {
            mConversation.removeConference(conf);
            conf = null;
        }

        if (conf != null) {
            getView().goToCallActivity(conf.getId());
        } else {
            Tuple<Account, Uri> guess = new Tuple<>(mAccountService.getAccount(mAccountId), new Uri(mContactId));
            if (guess != null && guess.first != null) {
                getView().goToCallActivityWithResult(guess, video);
            }
        }
    }

    public void deleteConversation() {
        mHistoryService.clearHistoryForConversation(mConversation);
        getView().goToHome();
    }

    private void loadHistory(long conversationId) {
        compositeDisposable.add(mHistoryService.getHistoryTextsFromConversationId(conversationId)
                .zipWith(mHistoryService.getHistoryCallsFromConversationId(conversationId),
                        new BiFunction<List<HistoryText>, List<HistoryCall>, Conversation>() {
                            @Override
                            public Conversation apply(@NonNull List<HistoryText> historyTexts, @NonNull List<HistoryCall> historyCalls) throws Exception {
                                CallContact callContact = mContactService.getContact(new Uri(mContactId));
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

                        SipCall sipCall = mCallService.getCurrentCallForContactId(mContactId);
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

                        Tuple<String, byte[]> contactData = mContactService.loadContactData(mConversation.getContact());
                        if (contactData != null) {
                            if (contactData.second != null) {
                                getView().displayContactPhoto(contactData.second);
                            }
                            if (contactData.first != null && !contactData.first.contains(CallContact.PREFIX_RING)) {
                                getView().displayContactName(contactData.first);
                            } else {
                                if (!mConversation.getContact().getPhones().isEmpty()) {
                                    if (mConversation.getContact().getUserName() != null) {
                                        getView().displayContactName(mConversation.getContact().getUserName());
                                    } else {
                                        getView().displayContactName(mConversation.getContact().getDisplayName());
                                    }
                                }
                            }
                        }

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
                }));
    }

    private void checkContact() {
        long time = System.currentTimeMillis();

        CallContact contact = mContactService.getContact(new Uri(mContactId));
        if (contact != null && CallContact.Status.CONFIRMED.equals(contact.getStatus())) {
            return;
        }

        mConversation.setLastContactRequest(time);
        getView().displaySendTrustRequest(mAccountId, mContactId);
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
        } else if (observable instanceof CallService && event != null) {
            switch (event.getEventType()) {
                case INCOMING_CALL:
                case CALL_STATE_CHANGED:
                    loadHistory(mConversationId);
                    break;
            }
        }
    }
}