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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationElement;
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
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Log;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;

public class ConversationFacade {

    private final static String TAG = ConversationFacade.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;
    private final HistoryService mHistoryService;
    private final CallService mCallService;

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

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private final Observable<Account> currentAccountSubject;

    public ConversationFacade(HistoryService historyService, CallService callService, ContactService contactService, AccountService accountService) {
        mHistoryService = historyService;
        mCallService = callService;
        mContactService = contactService;
        mAccountService = accountService;

        currentAccountSubject = mAccountService
                .getCurrentAccountSubject()
                .switchMapSingle(this::loadConversations);

        mDisposableBag.add(mCallService.getCallSubject()
                .observeOn(Schedulers.io())
                .subscribe(this::onCallStateChange));

        mDisposableBag.add(currentAccountSubject
                .switchMap(a -> Observable
                        .merge(a.getConversationsSubject(), a.getPendingSubject())
                        .doOnNext(c -> {
                            for (Conversation conversation : c) {
                                Uri id = conversation.getContact().getPrimaryUri();
                                mPresenceService.subscribeBuddy(a.getAccountID(), id.getRawUriString(), true);
                                mContactService.loadContactData(conversation.getContact());
                                mAccountService.lookupAddress(a.getAccountID(), "", id.getRawRingId());
                            }
                        }))
                .subscribeOn(Schedulers.computation())
                .subscribe());

        mDisposableBag.add(mAccountService.getIncomingRequests()
                .switchMapSingle(r -> getAccountSubject(r.getAccountId()))
                .subscribe(a -> mNotificationService.showIncomingTrustRequestNotification(a)));

        mDisposableBag.add(mAccountService
                .getIncomingMessages()
                .switchMapSingle(msg -> getAccountSubject(msg.getAccount())
                        .map(a -> {
                            a.addTextMessage(msg);
                            return msg;
                        }))
                .subscribe(this::parseNewMessage));

        mDisposableBag.add(mAccountService
                .getMessageStateChanges()
                .subscribe(txt -> {
                    Conversation conv = mAccountService.getAccount(txt.getAccount()).getByUri(txt.getNumberUri());
                    if (conv != null) {
                        conv.updateTextMessage(txt);
                    }
                }));

        mDisposableBag.add(mAccountService.getDataTransfers().subscribe(this::handleDataTransferEvent));
    }

    public Single<Conversation> startConversation(String accountId, final Uri contactId) {
        return getAccountSubject(accountId)
                .map(account -> account.getByUri(contactId));
    }

    public Observable<Account> getCurrentAccountSubject() {
        return currentAccountSubject;
    }

    public Single<Account> getAccountSubject(String accountId) {
        return mAccountService
                .getAccountSingle(accountId)
                .flatMap(this::loadConversations);
    }

    public Observable<List<Conversation>> getConversationsSubject() {
        return currentAccountSubject
                .switchMap(Account::getConversationsSubject);
    }

    public void readMessages(String accountId, Uri contact) {
        Account account = mAccountService.getAccount(accountId);
        readMessages(account, account.getByUri(contact));
    }

    public void readMessages(Account account, Conversation conversation) {
        if (conversation != null) {
            if (readMessages(conversation)) {
                //account.conversationRefreshed(conversation);
                account.refreshed(conversation);
                mNotificationService.cancelTextNotification(conversation.getContact().getPrimaryUri());
            }
        }
    }

    private boolean readMessages(Conversation conversation) {
        Log.w(TAG, "readMessages");
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
        return updated;
    }

    public void sendTextMessage(String account, Conversation c, Uri to, String txt) {
        mCallService.sendAccountTextMessage(account, to.getRawUriString(), txt)
                .subscribe(id -> {
                    TextMessage message = new TextMessage(false, txt, to, null, account);
                    message.setID(id);
                    message.read();
                    mHistoryService.insertNewTextMessage(message).subscribe();
                    c.addTextMessage(message);
                    mAccountService.getAccount(account).conversationUpdated(c);
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
        Completable.mergeArrayDelayError(
                        mHistoryService.deleteFileHistory(transfer.getId()),
                        Completable.fromAction(file::delete).subscribeOn(Schedulers.io()))
                .andThen(startConversation(transfer.getAccountId(), transfer.getContactNumber()))
                .subscribe(c -> c.removeFileTransfer(transfer));
    }

    private Single<Account> loadConversations(final Account account) {
        synchronized (account) {
            if (account.isHistoryLoaded()) {
                Log.d(TAG, "loadConversations(): just");
                return Single.just(account);
            }
            if (account.historyLoader == null) {
                Log.d(TAG, "loadConversations(): start loading");
                account.historyLoader = AsyncSubject.create();
                loadConversationHistory(account);
            } else {
                Log.d(TAG, "loadConversations(): already loading");
            }
            return account.historyLoader.singleOrError();
        }
    }

    private Disposable loadConversationHistory(final Account account) {
        Log.d(TAG, "loadConversationHistory()");
        return Single
                .merge( mHistoryService.getCallsSingle(account.getAccountID()).subscribeOn(Schedulers.io()),
                        mHistoryService.getTransfersSingle(account.getAccountID()).subscribeOn(Schedulers.io()),
                        mHistoryService.getMessagesSingle(account.getAccountID()).subscribeOn(Schedulers.io()))
                .observeOn(Schedulers.computation())
                .reduce(new HashSet<Conversation>(), (h, c) -> {
                    for (ConversationElement e : c) {
                        Conversation conversation = account.getByUri(e.getContactNumber());
                        if (conversation == null)
                            continue;
                        conversation.addElement(e, conversation.getContact());
                        h.add(conversation);
                    }
                    return h;
                })
                .subscribe(account::setHistoryLoaded);
    }

    public Completable clearHistory(final String accountId, final Uri contact) {
        return mHistoryService
                .clearHistory(contact.getRawUriString(), accountId)
                .doOnSubscribe(s -> {
                    Account account = mAccountService.getAccount(accountId);
                    account.clearHistory(contact);
                });
    }

    public void updateTextNotifications(List<Conversation> conversations) {
        Log.d(TAG, "updateTextNotifications()");

        for (Conversation conversation : conversations) {
            TreeMap<Long, TextMessage> texts = conversation.getUnreadTextMessages();

            CallContact contact = conversation.getContact();
            if (texts.isEmpty() || conversation.isVisible()) {
                mNotificationService.cancelTextNotification(contact.getPrimaryUri());
                continue;
            }
            if (texts.lastEntry().getValue().isNotified()) {
                continue;
            }
            mNotificationService.showTextNotification(contact, conversation, texts);
        }
    }

    private void parseNewMessage(TextMessage txt) {
        if (txt.isRead()) {
            mHistoryService.updateTextMessage(new HistoryText(txt)).subscribe();
        }
        getAccountSubject(txt.getAccount())
                .flatMapObservable(Account::getConversationsSubject)
                .firstOrError()
                .subscribeOn(Schedulers.computation())
                .subscribe(this::updateTextNotifications);
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
        mAccountService.discardTrustRequest(accountId, contact);
    }

    private void handleDataTransferEvent(DataTransfer transfer) {
        Conversation conversation = mAccountService.getAccount(transfer.getAccountId()).onDataTransferEvent(transfer);
        if (transfer.getEventCode() == DataTransferEventCode.CREATED) {
            if (transfer.isPicture() && !transfer.isOutgoing()) {
                mAccountService.acceptFileTransfer(transfer);
            }
        }
        mNotificationService.showFileTransferNotification(transfer, conversation.getContact());
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
            if (newState == SipCall.State.OVER)
                return;
            conference = new Conference(call);
            conversation.addConference(conference);
            account.updated(conversation);
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
            if (conference.removeParticipant(call)) {
                HistoryCall historyCall = new HistoryCall(call);
                mHistoryService.insertNewEntry(historyCall).subscribe();
                conversation.addHistoryCall(historyCall);
                account.updated(conversation);
            }
            mCallService.removeCallForId(call.getCallId());
            if (conference.getParticipants().isEmpty()) {
                conversation.removeConference(conference);
            }
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