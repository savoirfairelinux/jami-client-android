/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import java.util.WeakHashMap;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationElement;
import cx.ring.model.DataTransfer;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryText;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
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
    private final HistoryService mHistoryService;
    private final CallService mCallService;

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

    public ConversationFacade(HistoryService historyService, CallService callService, AccountService accountService) {
        mHistoryService = historyService;
        mCallService = callService;
        mAccountService = accountService;

        currentAccountSubject = mAccountService
                .getCurrentAccountSubject()
                .switchMapSingle(this::loadConversations)
                .replay(1)
                .refCount();

        mDisposableBag.add(mCallService.getCallSubject()
                .observeOn(Schedulers.io())
                .subscribe(this::onCallStateChange));

        mDisposableBag.add(currentAccountSubject
                .switchMap(a -> a.getPendingSubject()
                        .doOnNext(p -> mNotificationService.showIncomingTrustRequestNotification(a)))
                .subscribe());

        mDisposableBag.add(mAccountService.getIncomingRequests()
                .concatMapSingle(r -> getAccountSubject(r.getAccountId()))
                .subscribe(a -> mNotificationService.showIncomingTrustRequestNotification(a),
                        e -> Log.e(TAG, "Error showing contact request")));

        mDisposableBag.add(mAccountService
                .getIncomingMessages()
                .concatMapSingle(msg -> getAccountSubject(msg.getAccount())
                        .map(a -> {
                            a.addTextMessage(msg);
                            return msg;
                        }))
                .subscribe(this::parseNewMessage,
                        e -> Log.e(TAG, "Error adding text message")));

        mDisposableBag.add(mAccountService
                .getMessageStateChanges()
                .concatMapSingle(txt -> getAccountSubject(txt.getAccount())
                        .map(a -> a.getByUri(txt.getNumberUri()))
                        .doOnSuccess(conv -> conv.updateTextMessage(txt)))
                .subscribe(c -> {
                }, e -> Log.e(TAG, "Error updating text message")));

        mDisposableBag.add(mAccountService
                .getDataTransfers()
                .subscribe(this::handleDataTransferEvent,
                        e -> Log.e(TAG, "Error adding data transfer")));
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
                account.refreshed(conversation);
                mNotificationService.cancelTextNotification(conversation.getContact().getPrimaryUri());
            }
        }
    }

    private boolean readMessages(Conversation conversation) {
        boolean updated = false;
        NavigableMap<Long, ConversationElement> messages = conversation.getRawHistory();
        for (ConversationElement e : messages.descendingMap().values()) {
            if (!(e instanceof TextMessage))
                continue;
            TextMessage message = (TextMessage) e;
            if (message.isRead()) {
                break;
            }
            message.read();
            mHistoryService.updateTextMessage(new HistoryText(message)).subscribe();
            updated = true;
        }
        return updated;
    }

    public Single<TextMessage> sendTextMessage(String account, Conversation c, Uri to, String txt) {
        return mCallService.sendAccountTextMessage(account, to.getRawUriString(), txt)
                .map(id -> {
                    TextMessage message = new TextMessage(false, txt, to, null, account);
                    message.setID(id);
                    if (c.isVisible())
                        message.read();
                    mHistoryService.insertNewTextMessage(message).subscribe();
                    c.addTextMessage(message);
                    mAccountService.getAccount(account).conversationUpdated(c);
                    return message;
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

    public Completable sendFile(String account, Uri to, File file) {
        return Completable.fromAction(() -> {
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
            mHistoryService.insertDataTransfer(transfer).blockingAwait();

            File dest = mDeviceRuntimeService.getConversationPath(peerId, transfer.getStoragePath());
            if (!FileUtils.moveFile(file, dest)) {
                Log.e(TAG, "sendFile: can't move file to " + dest);
                return;
            }

            // send file
            mAccountService.sendFile(transfer, dest);
        }).subscribeOn(Schedulers.io());
    }


    public void deleteConversationItem(ConversationElement element) {
        switch (element.getType()) {
            case TEXT:
                TextMessage message = (TextMessage) element;
                mDisposableBag.add(Completable.mergeArrayDelayError(
                        mHistoryService.deleteMessageHistory(message.getId()).subscribeOn(Schedulers.io()))
                        .andThen(startConversation(message.getAccount(), message.getContactNumber()))
                        .subscribe(c -> c.removeConversationElement(message),
                                e -> Log.e(TAG, "Can't delete message", e)));
                break;
            case FILE:
                DataTransfer transfer = (DataTransfer) element;
                File file = mDeviceRuntimeService.getConversationPath(transfer.getPeerId(), transfer.getStoragePath());
                mDisposableBag.add(Completable.mergeArrayDelayError(
                        mHistoryService.deleteFileHistory(transfer.getId()),
                        Completable.fromAction(file::delete).subscribeOn(Schedulers.io()))
                        .andThen(startConversation(transfer.getAccountId(), transfer.getContactNumber()))
                        .subscribe(c -> c.removeConversationElement(transfer),
                                e -> Log.e(TAG, "Can't delete file transfer", e)));
                break;
            case CALL:
                HistoryCall callHistory = (HistoryCall) element;
                mDisposableBag.add(Completable.mergeArrayDelayError(
                        mHistoryService.deleteCallHistory(callHistory.getCallId()).subscribeOn(Schedulers.io()))
                        .andThen(startConversation(callHistory.getAccountID(), callHistory.getContactNumber()))
                        .subscribe(c -> c.removeConversationElement(callHistory),
                                e -> Log.e(TAG, "Can't delete call history", e)));
                break;
        }
    }

    public void cancelMessage(TextMessage message) {
        mDisposableBag.add(Completable.mergeArrayDelayError(
                mCallService.cancelMessage(message.getAccount(), message.getId()).subscribeOn(Schedulers.io()))
                .andThen(startConversation(message.getAccount(), message.getContactNumber()))
                .subscribe(c -> c.removeConversationElement(message),
                        e -> Log.e(TAG, "Can't cancel message sending", e)));
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
                .merge(mHistoryService.getCallsSingle(account.getAccountID()).subscribeOn(Schedulers.io()),
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
                    if (account != null) {
                        account.clearHistory(contact);
                    }
                });
    }

    public void updateTextNotifications(String accountId, List<Conversation> conversations) {
        Log.d(TAG, "updateTextNotifications()");

        for (Conversation conversation : conversations) {
            mNotificationService.showTextNotification(accountId, conversation);
        }
    }

    private void parseNewMessage(final TextMessage txt) {
        if (txt.isRead()) {
            mHistoryService.updateTextMessage(new HistoryText(txt)).subscribe();
        }
        getAccountSubject(txt.getAccount())
                .flatMapObservable(Account::getConversationsSubject)
                .firstOrError()
                .subscribeOn(Schedulers.computation())
                .subscribe(c -> updateTextNotifications(txt.getAccount(), c));
    }

    public void acceptRequest(String accountId, Uri contactUri) {
        if (accountId == null || contactUri == null)
            return;
        mPreferencesService.removeRequestPreferences(accountId, contactUri.getRawRingId());
        mAccountService.acceptTrustRequest(accountId, contactUri);
    }

    public void discardRequest(String accountId, Uri contact) {
        mHistoryService.clearHistory(contact.getRawUriString(), accountId).subscribe();
        mPreferencesService.removeRequestPreferences(accountId, contact.getRawRingId());
        mAccountService.discardTrustRequest(accountId, contact);
    }

    private void handleDataTransferEvent(DataTransfer transfer) {
        Conversation conversation = mAccountService.getAccount(transfer.getAccountId()).onDataTransferEvent(transfer);
        if (transfer.getEventCode() == DataTransferEventCode.CREATED && !transfer.isOutgoing()) {
            if (transfer.canAutoAccept()) {
                mAccountService.acceptFileTransfer(transfer);
            }
        }
        if (conversation.isVisible())
            mNotificationService.cancelFileNotification(transfer.getId());
        else
            mNotificationService.showFileTransferNotification(transfer, conversation.getContact());
    }

    private void onCallStateChange(SipCall call) {
        SipCall.State newState = call.getCallState();
        boolean incomingCall = newState == SipCall.State.RINGING && call.isIncoming();
        mHardwareService.updateAudioState(newState, incomingCall, !call.isAudioOnly());

        Account account = mAccountService.getAccount(call.getAccount());
        CallContact contact = call.getContact();
        Conversation conversation = (contact == null || account == null) ? null : account.getByUri(contact.getPrimaryUri());
        Conference conference = null;
        if (conversation != null) {
            conference = conversation.getConference(call.getCallId());
            if (conference == null) {
                if (newState == SipCall.State.OVER)
                    return;
                conference = new Conference(call);
                conversation.addConference(conference);
                account.updated(conversation);
            }
        }

        Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + newState);
        if ((call.isRinging() || newState == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
            call.setTimestampStart(System.currentTimeMillis());
        }
        if (incomingCall) {
            mHardwareService.setPreviewSettings();
            mNotificationService.showCallNotification(conference);
        } else if ((newState == SipCall.State.CURRENT && call.isIncoming())
                || newState == SipCall.State.RINGING && call.isOutGoing()) {
            mNotificationService.showCallNotification(conference);
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

            if (conversation != null && conference.removeParticipant(call)) {
                HistoryCall historyCall = new HistoryCall(call);
                mHistoryService.insertNewEntry(historyCall).subscribe();
                conversation.addHistoryCall(historyCall);
                if (historyCall.isIncoming() && historyCall.isMissed()) {
                    mNotificationService.showMissedCallNotification(call);
                }
                account.updated(conversation);
            }
            mCallService.removeCallForId(call.getCallId());
            if (conversation != null && conference.getParticipants().isEmpty()) {
                conversation.removeConference(conference);
            }
        }
    }

    public Single<SipCall> placeCall(String accountId, String number, boolean video) {
        String rawId = new Uri(number).getRawRingId();
        return getAccountSubject(accountId).flatMap(account -> {
            CallContact contact = account.getContact(rawId);
            if (contact == null)
                mAccountService.addContact(accountId, rawId);
            return mCallService.placeCall(rawId, number, video);
        });
    }

    public void cancelFileTransfer(long id) {
        mAccountService.cancelDataTransfer(id);
        mNotificationService.cancelFileNotification(id);
        DataTransfer transfer = mAccountService.getDataTransfer(id);
        if (transfer != null)
            deleteConversationItem(transfer);
    }

    public Completable removeConversation(String accountId, Uri contact) {
        return mHistoryService
                .clearHistory(contact.getRawUriString(), accountId)
                .doOnSubscribe(s -> {
                    Account account = mAccountService.getAccount(accountId);
                    account.clearHistory(contact);
                    mAccountService.removeContact(accountId, contact.getRawRingId(), false);
                });
    }
}