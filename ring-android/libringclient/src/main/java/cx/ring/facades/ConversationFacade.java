/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.facades;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ConfigKey;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationElement;
import cx.ring.model.DataTransfer;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.HistoryText;
import cx.ring.model.SecureSipCall;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.Tuple;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * This facade handles the conversations
 * - Load from the history
 * - Keep a local cache of these conversations
 * <p>
 * Events are broadcasted:
 * - CONVERSATIONS_CHANGED
 */
public class ConversationFacade {

    private final static String TAG = ConversationFacade.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;

    @Inject
    ConferenceService mConferenceService;

    private final HistoryService mHistoryService;
    private final CallService mCallService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    NotificationService mNotificationService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    public static class AccountConversations {
        public final Account account;
        public Map<String, Conversation> conversations = new HashMap<>();
        public Map<String, Conversation> pending = new HashMap<>();
        AccountConversations() {
            account = null;
        }
        AccountConversations(Account acc) {
            account = acc;
        }
        public Conversation getByUri(Uri uri) {
            Conversation conversation = null;
            if (uri != null) {
                String key = uri.getRawUriString();
                conversation = conversations.get(key);
                if (conversation != null) {
                    return conversation;
                }
                conversation = pending.get(key);
                if (conversation != null) {
                    return conversation;
                }
                if (account != null) {
                    CallContact contact = account.getContactFromCache(uri);
                    conversation = new Conversation(contact);
                    pending.put(key, conversation);
                }
            }
            return conversation;
        }
        public void contactAdded(CallContact contact) {
            String key = contact.getPrimaryUri().getRawUriString();
            Conversation pendingConversation = pending.get(key);
            if (pendingConversation == null) {
                pendingConversation = new Conversation(contact);
                conversations.put(key, pendingConversation);
            } else {
                pending.remove(key);
                pendingConversation.addContactEvent();
                conversations.put(key, pendingConversation);
            }
        }
        public void contactRemoved(Uri uri) {
            String key = uri.getRawUriString();
            pending.remove(key);
            conversations.remove(key);
        }
    }
    private AccountConversations mConversations = new AccountConversations();
    private final CompositeDisposable mConversationsDisposable = new CompositeDisposable();
    private final CompositeDisposable mDisposableBag = new CompositeDisposable(mConversationsDisposable);

    private Subject<AccountConversations> conversationSubject = BehaviorSubject.create();

    /*private Observable<AccountConversations> sortedConversationSubject = conversationSubject.doOnNext(c -> {
        c.conversations.
    });*/

    public ConversationFacade(HistoryService historyService, CallService callService, ContactService contactService, AccountService accountService) {
        mHistoryService = historyService;
        mCallService = callService;
        mDisposableBag.add(mCallService.getCallSubject()
                .observeOn(Schedulers.io())
                .subscribe(this::onCallStateChange));
        mContactService = contactService;
        mAccountService = accountService;
        mDisposableBag.add(mAccountService
                .getCurrentAccountSubject()
                .switchMapSingle(this::loadConversations)
                .subscribe(conversations -> {
                    Log.d(TAG, "refreshConversations() " + conversations.conversations.size());
                    mConversationsDisposable.clear();
                    mConversations = conversations;
                    mConversationsDisposable.add(conversations.account.getRequestsUpdates().subscribe(requests -> {
                        for (TrustRequest req : requests) {
                            String key = new Uri(req.getContactId()).getRawUriString();
                            Log.w(TAG, "Adding new trust request: "+ key);
                            Conversation conversation = conversations.pending.get(key);
                            if (conversation == null) {
                                conversation = new Conversation(conversations.account.getContactFromCache(req.getContactId()));
                                conversations.pending.put(key, conversation);
                            }
                            conversation.addRequestEvent(req);
                        }
                    }));
                    mConversationsDisposable.add(conversations.account
                            .getContactEvents()
                            .subscribe(event -> {
                                if (event.added)
                                    conversations.contactAdded(event.contact);
                                else
                                    conversations.contactRemoved(event.contact.getPrimaryUri());
                                conversationSubject.onNext(conversations);
                            }));
                    conversationSubject.onNext(conversations);
                }));

        mDisposableBag.add(mAccountService.getRegisteredNames()
                .filter(r -> r.state == 0)
                .observeOn(Schedulers.computation())
                .subscribe(name -> {
                    CallContact contact = mContactService.setRingContactName(name.accountId, new Uri(name.address), name.name);
                    if (contact != null) {
                        if (mConversations.account != null && mConversations.account.getAccountID().equals(name.accountId)) {
                            conversationSubject.onNext(mConversations);
                        }
                    }
                }));
        mDisposableBag.add(mAccountService.getIncomingMessages().subscribe(txt -> {
            parseNewMessage(txt);
            updateTextNotifications();
        }));
        mDisposableBag.add(mAccountService.getMessageStateChanges().subscribe(txt -> {
            Conversation conv = getConversationByContact(mContactService.findContact(txt.getAccount(), txt.getNumberUri()));
            if (conv != null) {
                conv.updateTextMessage(txt);
            }
        }));
        mDisposableBag.add(mAccountService.getDataTransfers().subscribe(this::handleDataTransferEvent));
    }

    private Tuple<Conference, SipCall> getCall(String id) {
        for (Conversation conv : mConversations.conversations.values()) {
            ArrayList<Conference> confs = conv.getCurrentCalls();
            for (Conference c : confs) {
                SipCall call = c.getCallById(id);
                if (call != null) {
                    return new Tuple<>(c, call);
                }
            }
        }
        return new Tuple<>(null, null);
    }

    /**
     * @return the local cache of conversations
     */
    /*public Map<String, Conversation> getConversations() {
        return mConversationMap;
    }*/

    public Conversation getConversationByContact(CallContact contact) {
        if (contact != null) {
            ArrayList<String> keys = contact.getIds();
            for (String key : keys) {
                Conversation conversation = mConversations.conversations.get(key);
                if (conversation != null) {
                    return conversation;
                }
                conversation = mConversations.pending.get(key);
                if (conversation != null) {
                    return conversation;
                }
            }
        }
        return null;
    }

    public Conversation getConversationByCallId(String callId) {
        for (Conversation conversation : mConversations.conversations.values()) {
            Conference conf = conversation.getConference(callId);
            if (conf != null) {
                return conversation;
            }
        }
        return null;
    }

    /**
     * @return the started new conversation
     */
    public Conversation startConversation(CallContact contact) {
        Conversation conversation = getConversationByContact(contact);
        if (conversation == null) {
            conversation = new Conversation(contact);
            Log.w(TAG, "Adding pending conversation: "+ contact.getIds().get(0));
            mConversations.pending.put(contact.getIds().get(0), conversation);
            Account account = mConversations.account;
            if (account != null && account.isRing()) {
                Uri number = contact.getPhones().get(0).getNumber();
                if (number.isRingId()) {
                    mAccountService.lookupAddress(account.getAccountID(), "", number.getRawRingId());
                }
            }
            conversationSubject.onNext(mConversations);
            updateTextNotifications();
        }
        return conversation;
    }

    public Single<Conversation> startConversation(String accountId, final Uri contactId) {
        return getConversationSubject()
                .firstOrError()
                .map(conversations -> {
                    if (!conversations.account.getAccountID().equals(accountId))
                        throw new IllegalStateException();
                    return conversations.getByUri(contactId);
                });
    }

    public void readMessages(Conversation conversation) {
        for (HistoryEntry h : conversation.getRawHistory().values()) {
            NavigableMap<Long, TextMessage> messages = h.getTextMessages();
            for (TextMessage message : messages.descendingMap().values()) {
                if (message.isRead()) {
                    break;
                }
                message.read();
                mHistoryService.updateTextMessage(new HistoryText(message)).subscribe();
            }
        }
    }

    /**
     * @return the conversation from the local cache
     */
    public Conversation getConversationById(String id) {
        return mConversations.conversations.get(id);
    }

    public void sendTextMessage(String account, Conversation c, Uri to, String txt) {
        long id = mCallService.sendAccountTextMessage(account, to.getRawUriString(), txt);
        Log.i(TAG, "sendAccountTextMessage " + txt + " got id " + id);
        TextMessage message = new TextMessage(false, txt, to, null, account);
        message.setID(id);
        message.read();
        mHistoryService.insertNewTextMessage(message).subscribe();
        c.addTextMessage(message);
    }

    public void sendTextMessage(Conversation c, Conference conf, String txt) {
        mCallService.sendTextMessage(conf.getId(), txt);
        SipCall call = conf.getParticipants().get(0);
        TextMessage message = new TextMessage(false, txt, call.getNumberUri(), conf.getId(), call.getAccount());
        message.read();
        mHistoryService.insertNewTextMessage(message).subscribe();
        c.addTextMessage(message);
    }

    public void sendFile(String account, Uri to, File file) {
        if (file == null) {
            return;
        }

        // check file
        if (!file.exists()) {
            Log.w(TAG, "sendFile: file not found");
            return;
        }

        if (!file.canRead()) {
            Log.w(TAG, "sendFile: file not readable");
            return;
        }
        String peerId = to.getRawRingId();
        DataTransfer transfer = new DataTransfer(0L, file.getName(), true, file.length(), 0, peerId, account);
        // get generated ID
        mHistoryService.insertDataTransfer(transfer);

        File dest = mDeviceRuntimeService.getConversationPath(peerId, transfer.getStoragePath());
        if (!FileUtils.moveFile(file, dest)) {
            Log.e(TAG, "sendFile: can't move file to " + dest);
            return;
        }

        // send file
        mAccountService.sendFile(transfer, dest);
    }

    public io.reactivex.Observable<AccountConversations> getConversationSubject() {
        return conversationSubject;
    }

    private Single<AccountConversations> loadConversations(final Account account) {
        String accountId = account.getAccountID();
        return loadConversationHistory(accountId).map(conversations -> {
            Log.d(TAG, "loadConversations() onSubscribe " + account.getAccountID());
            AccountConversations ret = new AccountConversations(account);
            Map<String, CallContact> contacts = account.getContacts();
            List<TrustRequest> requests = account.getRequests();
            for (Map.Entry<String, Conversation> c : conversations.entrySet()) {
                CallContact contact = contacts.get(c.getValue().getContact().getPrimaryNumber());
                if (contact == null) {
                    Log.w(TAG, "loadConversations: Adding new trust request: "+ c.getKey());
                    ret.pending.put(c.getKey(), c.getValue());
                } else if (!contact.isBanned()) {
                    ret.conversations.put(c.getKey(), c.getValue());
                }
            }
            for (CallContact contact : contacts.values()) {
                String key = contact.getIds().get(0);
                if (!contact.isBanned()) {
                    Conversation conversation = ret.conversations.get(key);
                    if (conversation == null) {
                        conversation = new Conversation(contact);
                        ret.conversations.put(key, conversation);
                    }
                    conversation.addContactEvent();
                }
            }
            for (TrustRequest req : requests) {
                String key = new Uri(req.getContactId()).getRawUriString();
                Conversation conversation = ret.pending.get(key);
                if (conversation == null) {
                    conversation = new Conversation(account.getContactFromCache(req.getContactId()));
                    ret.pending.put(key, conversation);
                }
                conversation.addRequestEvent(req);
            }
            Log.d(TAG, "loadConversations() onSubscribe END convs:"+ret.conversations.size() + " pending:" + ret.pending.size());
            return ret;
        });
    }

    private Single<Map<String, Conversation>> loadConversationHistory(final String accountId) {
        Log.d(TAG, "loadConversationHistory()");
        return Single.create(l -> {
            final Map<String, Conversation> conversationMap = new HashMap<>();
            io.reactivex.Observable.merge(Arrays.asList(
                    mHistoryService.getCalls(accountId),
                    mHistoryService.getTransfers(accountId),
                    mHistoryService.getMessages(accountId)))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribeWith(new DisposableObserver<ConversationElement>() {
                        @Override
                        public void onNext(ConversationElement e) {
                            CallContact contact = mContactService.findContact(accountId, e.getContactNumber());
                            String key = contact.getIds().get(0);
                            Conversation conversation = conversationMap.get(key);
                            if (conversation == null) {
                                conversation = new Conversation(contact);
                                conversationMap.put(key, conversation);
                            }
                            if (e instanceof HistoryCall) {
                                HistoryCall call = (HistoryCall) e;
                                conversation.addHistoryCall(call);
                            } else if (e instanceof TextMessage) {
                                TextMessage msg = (TextMessage) e;
                                msg.setContact(contact);
                                conversation.addTextMessage(msg);
                            } else if (e instanceof DataTransfer) {
                                DataTransfer t = (DataTransfer) e;
                                conversation.addFileTransfer(t);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "loadConversationHistory() onError", e);
                            l.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "loadConversationHistory() onComplete");
                            lookupNames(accountId, conversationMap.values());
                            l.onSuccess(conversationMap);
                        }
                    });
        });
    }

    public Completable clearHistoryForContactAndAccount(final String contactId) {
        return mHistoryService.clearHistoryForContactAndAccount(contactId, mAccountService.getCurrentAccount().getAccountID());
    }

    public void updateTextNotifications() {
        Log.d(TAG, "updateTextNotifications()");

        for (Conversation conversation : mConversations.conversations.values()) {
            TreeMap<Long, TextMessage> texts = conversation.getUnreadTextMessages();

            if (texts.isEmpty() || conversation.isVisible()) {
                mNotificationService.cancelTextNotification(conversation.getContact());
                continue;
            }
            if (texts.lastEntry().getValue().isNotified()) {
                continue;
            }
            CallContact contact = conversation.getContact();
            mNotificationService.showTextNotification(contact, conversation, texts);
        }
    }

    private void parseNewMessage(TextMessage txt) {
        Conversation conversation;
        if (!StringUtils.isEmpty(txt.getCallId())) {
            conversation = getConversationByCallId(txt.getCallId());
        } else {
            conversation = startConversation(mContactService.findContact(txt.getAccount(), txt.getNumberUri()));
            txt.setContact(conversation.getContact());
        }
        conversation.addTextMessage(txt);
        if (txt.isRead()) {
            mHistoryService.updateTextMessage(new HistoryText(txt)).subscribe();
        }
    }

    public void discardRequest(String accountId, Uri contact) {
        mAccountService.discardTrustRequest(accountId, contact.getRawRingId());
        mHistoryService.clearHistoryForContactAndAccount(contact.getRawUriString(), accountId).subscribe();
        if (mConversations.account.getAccountID().equals(accountId)) {
            mConversations.pending.remove(contact.getRawUriString());
            conversationSubject.onNext(mConversations);
        }
    }

    private void aggregateHistory() {
        Map<String, ArrayList<String>> conferences = mConferenceService.getConferenceList();

        for (Map.Entry<String, ArrayList<String>> conferenceEntry : conferences.entrySet()) {
            Conference conference = new Conference(conferenceEntry.getKey());
            for (String callId : conferenceEntry.getValue()) {
                SipCall call = getCall(callId).second;
                if (call == null) {
                    call = new SipCall(callId, mCallService.getCallDetails(callId));
                }
                Account account = mAccountService.getAccount(call.getAccount());
                if (account.isRing()
                        || account.getDetailBoolean(ConfigKey.SRTP_ENABLE)
                        || account.getDetailBoolean(ConfigKey.TLS_ENABLE)) {
                    call = new SecureSipCall(call, account.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
                }
                conference.addParticipant(call);
            }
            List<SipCall> calls = conference.getParticipants();
            if (calls.size() == 1) {
                SipCall call = calls.get(0);
                CallContact contact = call.getContact();
                if (call.getContact() == null) {
                    contact = mContactService.findContact(call.getAccount(), call.getNumberUri());
                    call.setContact(contact);
                }
                Conversation conv = null;
                ArrayList<String> ids = contact.getIds();
                for (String id : ids) {
                    conv = mConversations.conversations.get(id);
                    if (conv != null) {
                        break;
                    }
                }
                if (conv != null) {
                    conv.addConference(conference);
                } else {
                    conv = new Conversation(contact);
                    conv.addConference(conference);
                    mConversations.conversations.put(ids.get(0), conv);
                }
            }
        }
    }

    private void lookupNames(String currentAccountId, Collection<Conversation> conversations) {
        for (Conversation conversation : conversations) {
            CallContact contact = conversation.getContact();
            if (contact == null) {
                continue;
            }
            Uri contactUri = contact.getPhones().get(0).getNumber();
            if (!contactUri.isRingId() || !StringUtils.isEmpty(contact.getUsername())) {
                continue;
            }
            boolean currentAccountChecked = false;
            for (String accountId : conversation.getAccountsUsed()) {
                Account account = mAccountService.getAccount(accountId);
                if (account == null || !account.isRing()) {
                    continue;
                }
                if (accountId.equals(currentAccountId)) {
                    currentAccountChecked = true;
                }
                mAccountService.lookupAddress(accountId, "", contactUri.getRawRingId());
            }
            if (!currentAccountChecked) {
                mAccountService.lookupAddress(currentAccountId, "", contactUri.getRawRingId());
            }
        }
    }

    private void handleDataTransferEvent(DataTransfer transfer) {
        CallContact contact = mContactService.findContactByNumber(transfer.getPeerId());
        Conversation conversation = startConversation(contact);
        DataTransferEventCode transferEventCode = transfer.getEventCode();
        if (transferEventCode == DataTransferEventCode.CREATED) {
            if (transfer.isPicture() && !transfer.isOutgoing()) {
                File path = mDeviceRuntimeService.getConversationPath(transfer.getPeerId(), transfer.getStoragePath());
                mAccountService.acceptFileTransfer(transfer.getDataTransferId(), path.getAbsolutePath(), 0);
            }
            conversation.addFileTransfer(transfer);
            //setChanged();
            //notifyObservers(new ServiceEvent(ServiceEvent.EventType.DATA_TRANSFER));
        } else {
            conversation.updateFileTransfer(transfer, transferEventCode);
            /*setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.DATA_TRANSFER_UPDATE);
            event.addEventInput(ServiceEvent.EventInput.TRANSFER_INFO, transfer);
            notifyObservers(event);*/
        }
        mNotificationService.showFileTransferNotification(transfer, transferEventCode, contact);
    }

    private void onCallStateChange(SipCall call) {
        Conversation conversation = null;
        Conference conference = null;
        if (call == null) {
            Log.w(TAG, "CALL_STATE_CHANGED : call is null");
            return;
        }
        int newState = call.getCallState();
        boolean incomingCall = newState == SipCall.State.RINGING && call.isIncoming();
        mHardwareService.updateAudioState(newState, !call.isAudioOnly());

        for (Conversation conv : mConversations.conversations.values()) {
            conference = conv.getConference(call.getCallId());
            if (conference != null) {
                conversation = conv;
                Log.w(TAG, "CALL_STATE_CHANGED : found conversation " + call.getCallId());
                break;
            }
        }

        if (conversation == null) {
            conversation = startConversation(call.getContact());
            conference = new Conference(call);
            conversation.addConference(conference);
        }

        Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + newState);
        if ((call.isRinging() || newState == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
            call.setTimestampStart(System.currentTimeMillis());
        }
        if (incomingCall) {
            //mNotificationService.showCallNotification(conference);
            mHardwareService.setPreviewSettings();
        } else if ((newState == SipCall.State.CURRENT && call.isIncoming())
                || newState == SipCall.State.RINGING && call.isOutGoing()) {
            mAccountService.sendProfile(call.getCallId(), call.getAccount());
        } else if (newState == SipCall.State.HUNGUP
                || newState == SipCall.State.BUSY
                || newState == SipCall.State.FAILURE
                || newState == SipCall.State.OVER) {
            mNotificationService.cancelCallNotification(call.getCallId().hashCode());
            mHardwareService.closeAudioState();

            if (newState == SipCall.State.HUNGUP) {
                call.setTimestampEnd(System.currentTimeMillis());
            }
            if (call.getTimestampStart() == 0) {
                call.setTimestampStart(System.currentTimeMillis());
            }
            if (call.getTimestampEnd() == 0) {
                call.setTimestampEnd(System.currentTimeMillis());
            }

            mHistoryService.insertNewEntry(conference);
            conference.removeParticipant(call);
            conversation.addHistoryCall(new HistoryCall(call));
            mCallService.removeCallForId(call.getCallId());
        }
        if (conference.getParticipants().isEmpty()) {
            conversation.removeConference(conference);
        }
    }

    public Single<SipCall> placeCall(String accountId, String number, boolean video) {
        String rawId = new Uri(number).getRawRingId();
        Account account = mAccountService.getAccount(accountId);
        if (account != null) {
            CallContact contact = account.getContact(rawId);
            if (contact == null)
                mAccountService.addContact(accountId, rawId);
        }
        return mCallService.placeCall(rawId, number, video);
    }
}