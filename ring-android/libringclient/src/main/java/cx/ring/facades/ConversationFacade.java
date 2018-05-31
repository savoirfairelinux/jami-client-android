/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.HistoryText;
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
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.Tuple;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class ConversationFacade {

    private final static String TAG = ConversationFacade.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;
    private final HistoryService mHistoryService;
    private final CallService mCallService;

    @Inject
    ConferenceService mConferenceService;

    @Inject
    PresenceService mPresenceService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    NotificationService mNotificationService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    PreferencesService mPreferencesService;

    private Account mConversations = null;
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private Subject<Account> conversationsSubject = BehaviorSubject.create();

    public ConversationFacade(HistoryService historyService, CallService callService, ContactService contactService, AccountService accountService) {
        mHistoryService = historyService;
        mCallService = callService;
        mContactService = contactService;
        mAccountService = accountService;

        mDisposableBag.add(mCallService.getCallSubject()
                .observeOn(Schedulers.io())
                .subscribe(this::onCallStateChange));

        mDisposableBag.add(mAccountService
                .getCurrentAccountSubject()
                .switchMapSingle(this::loadConversations)
                .subscribe(account -> {
                    Log.d(TAG, "refreshConversations() " + account);
                    mConversations = account;
                    for (Conversation conversation : account.getConversations()) {
                        mPresenceService.subscribeBuddy(account.getAccountID(), conversation.getContact().getPrimaryUri().getRawUriString(), true);
                    }
                    conversationsSubject.onNext(account);
                }));

        mDisposableBag.add(mAccountService.getRegisteredNames()
                .filter(r -> r.state == 0)
                .observeOn(Schedulers.computation())
                .subscribe(name -> {
                    CallContact contact = mContactService.setRingContactName(name.accountId, new Uri(name.address), name.name);
                    if (contact != null) {
                        if (mConversations != null && mConversations.getAccountID().equals(name.accountId)) {
                            conversationsSubject.onNext(mConversations);
                        }
                    }
                }));
        mDisposableBag.add(mAccountService.getIncomingMessages().subscribe(this::parseNewMessage));
        mDisposableBag.add(mAccountService.getMessageStateChanges().subscribe(txt -> {
            Conversation conv = mAccountService.getAccount(txt.getAccount()).getByUri(txt.getNumberUri());
            if (conv != null) {
                conv.updateTextMessage(txt);
            }
        }));
        mDisposableBag.add(mAccountService.getDataTransfers().subscribe(this::handleDataTransferEvent));
    }

    private Conversation getConversationByCallId(String callId) {
        if (mConversations != null) {
            return mConversations.getConversationByCallId(callId);
        }
        return null;
    }

    public Conversation startConversation(String accountId, final Uri contactId) {
        return mAccountService.getAccount(accountId).getByUri(contactId);
    }

    public void readMessages(Conversation conversation) {
        boolean updated = false;
        for (HistoryEntry h : conversation.getRawHistory().values()) {
            NavigableMap<Long, TextMessage> messages = h.getTextMessages();
            for (TextMessage message : messages.descendingMap().values()) {
                if (message.isRead()) {
                    break;
                }
                message.read();
                mHistoryService.updateTextMessage(new HistoryText(message)).subscribe();
                updated = true;
            }
        }
        if (updated) {
            conversationsSubject.onNext(mConversations);
        }
    }

    /**
     * @return the conversation from the local cache
     */
    public Conversation getConversationById(String accountId, String id) {
        return mAccountService.getAccount(accountId).getConversationById(id);
    }

    public void sendTextMessage(String account, Conversation c, Uri to, String txt) {
        final TextMessage message = new TextMessage(false, txt, to, null, account);
        mCallService.sendAccountTextMessage(account, to.getRawUriString(), txt)
                .subscribe(id -> {
                    message.setID(id);
                    message.read();
                    mHistoryService.insertNewTextMessage(message).subscribe();
                    c.addTextMessage(message);
                });
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

    public void deleteFile(DataTransfer transfer) {
        File file = mDeviceRuntimeService.getConversationPath(transfer.getPeerId(), transfer.getStoragePath());
        file.delete();
        mHistoryService.deleteFileHistory(transfer.getId());
        Conversation conversation = startConversation(transfer.getAccountId(), transfer.getContactNumber());
        conversation.removeFileTransfer(transfer);
    }

    public Observable<Account> getConversationsSubject() {
        return conversationsSubject;
    }

    private Single<Account> loadConversations(final Account account) {
        if (account.isHistoryLoaded()) {
            return Single.just(account);
        }
        return loadConversationHistory(account.getAccountID())
                .map(conversations -> {
                    Log.d(TAG, "loadConversations() onSubscribe " + account.getAccountID());
                    account.loadHistory(conversations);
                    return account;
                });
    }

    private Single<HashMap<String, Conversation>> loadConversationHistory(final String accountId) {
        Log.d(TAG, "loadConversationHistory()");
        return Observable.merge(Arrays.asList(
                mHistoryService.getCalls(accountId),
                mHistoryService.getTransfers(accountId),
                mHistoryService.getMessages(accountId)))
                .reduce(new HashMap<String, Conversation>(), (conversationMap, e) -> {
                    CallContact contact = mContactService.findContact(accountId, e.getContactNumber());
                    String key = contact.getIds().get(0);
                    Conversation conversation = conversationMap.get(key);
                    if (conversation == null) {
                        conversation = new Conversation(contact);
                        conversationMap.put(key, conversation);
                    }
                    conversation.addElement(e, contact);
                    return conversationMap;
                })
                .subscribeOn(Schedulers.io());
    }

    public Completable clearHistory(final String accountId, final Uri contact) {
        return mHistoryService
                .clearHistory(contact.getRawUriString(), accountId)
                .doOnSubscribe(s -> {
                    if (accountId.equals(mConversations.getAccountID())) {
                        Conversation conversation = mConversations.getByUri(contact);
                        conversation.clearHistory();
                        conversationsSubject.onNext(mConversations);
                    }
                });
    }

    public void updateTextNotifications() {
        Log.d(TAG, "updateTextNotifications()");

        for (Conversation conversation : mConversations.getConversations()) {
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
        Account account = mAccountService.getAccount(txt.getAccount());
        Conversation conversation;
        if (!StringUtils.isEmpty(txt.getCallId())) {
            conversation = getConversationByCallId(txt.getCallId());
        } else {
            conversation = account.getByUri(txt.getNumberUri());
            txt.setContact(conversation.getContact());
        }
        conversation.addTextMessage(txt);
        if (txt.isRead()) {
            mHistoryService.updateTextMessage(new HistoryText(txt)).subscribe();
        }
        conversationsSubject.onNext(mConversations);
        updateTextNotifications();
    }

    public void acceptRequest(String accountId, Uri contactUri) {
        String contactId = contactUri.getRawRingId();
        Account account = mAccountService.getAccount(accountId);
        mAccountService.acceptTrustRequest(accountId, contactUri);
        mPreferencesService.removeRequestPreferences(accountId, contactId);
        for (Iterator<TrustRequest> it = account.getRequests().iterator(); it.hasNext(); ) {
            TrustRequest request = it.next();
            if (accountId.equals(request.getAccountId()) && contactId.equals(request.getContactId())) {
                VCard vCard = request.getVCard();
                if (vCard != null) {
                    VCardUtils.savePeerProfileToDisk(vCard, contactId + ".vcf", mDeviceRuntimeService.provideFilesDir());
                }
                it.remove();
            }
        }
    }

    public void discardRequest(String accountId, Uri contact) {
        mHistoryService.clearHistory(contact.getRawUriString(), accountId).subscribe();
        mPreferencesService.removeRequestPreferences(accountId, contact.getRawRingId());
        if (mAccountService.discardTrustRequest(accountId, contact)) {
            conversationsSubject.onNext(mConversations);
        }
    }

    private void handleDataTransferEvent(DataTransfer transfer) {
        Conversation conversation = mAccountService.getAccount(transfer.getAccountId()).getByUri(new Uri(transfer.getPeerId()));
        DataTransferEventCode transferEventCode = transfer.getEventCode();
        if (transferEventCode == DataTransferEventCode.CREATED) {
            if (transfer.isPicture() && !transfer.isOutgoing()) {
                mAccountService.acceptFileTransfer(transfer);
            }
            conversation.addFileTransfer(transfer);
            conversationsSubject.onNext(mConversations);
        } else {
            conversation.updateFileTransfer(transfer, transferEventCode);
            //conversationSubject.onNext(conversation);
        }
        mNotificationService.showFileTransferNotification(transfer, transferEventCode, conversation.getContact());
    }

    private void onCallStateChange(SipCall call) {
        if (call == null) {
            Log.w(TAG, "CALL_STATE_CHANGED : call is null");
            return;
        }
        int newState = call.getCallState();
        boolean incomingCall = newState == SipCall.State.RINGING && call.isIncoming();
        mHardwareService.updateAudioState(newState, !call.isAudioOnly());

        Account account = mAccountService.getAccount(call.getAccount());
        Conversation conversation = account.getByUri(call.getContact().getPrimaryUri());
        Conference conference = conversation.getConference(call.getCallId());
        if (conference == null) {
            conference = new Conference(call);
            conversation.addConference(conference);
            if (account == mConversations)
                conversationsSubject.onNext(account);
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
            long now = System.currentTimeMillis();
            if (call.getTimestampStart() == 0) {
                call.setTimestampStart(now);
            }
            if (newState == SipCall.State.HUNGUP || call.getTimestampEnd() == 0) {
                call.setTimestampEnd(now);
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