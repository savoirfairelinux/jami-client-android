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
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.Interaction;
import cx.ring.model.Interaction.InteractionStatus;
import cx.ring.model.Interaction.InteractionType;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Log;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class ConversationFacade {

    private final static String TAG = ConversationFacade.class.getSimpleName();

    private final AccountService mAccountService;
    private final HistoryService mHistoryService;
    private final CallService mCallService;
    private final ContactService mContactService;

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

    private final Subject<Conversation> conversationSubject = PublishSubject.create();

    public ConversationFacade(HistoryService historyService, CallService callService, AccountService accountService, ContactService contactService) {
        mHistoryService = historyService;
        mCallService = callService;
        mAccountService = accountService;
        mContactService = contactService;

        currentAccountSubject = mAccountService
                .getCurrentAccountSubject()
                .switchMapSingle(this::loadSmartlist);

        mDisposableBag.add(mCallService.getCallsUpdates()
                .observeOn(Schedulers.io())
                .subscribe(this::onCallStateChange));

        mDisposableBag.add(mCallService.getConfsUpdates()
                .observeOn(Schedulers.io())
                .subscribe(this::onConfStateChange));

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
                        e -> Log.e(TAG, "Error adding text message", e)));

        mDisposableBag.add(mAccountService
                .getMessageStateChanges()
                .concatMapSingle(txt -> getAccountSubject(txt.getAccount())
                        .map(a -> a.getByUri(txt.getConversation().getParticipant()))
                        .doOnSuccess(conv -> conv.updateTextMessage(txt)))
                .subscribe(c -> {
                }, e -> Log.e(TAG, "Error updating text message", e)));

        mDisposableBag.add(mAccountService
                .getDataTransfers()
                .subscribe(this::handleDataTransferEvent,
                        e -> Log.e(TAG, "Error adding data transfer")));
    }

    public Observable<Conversation> getUpdatedConversation() {
        return conversationSubject;
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
                .flatMap(this::loadSmartlist);
    }

    public Observable<List<Conversation>> getConversationsSubject() {
        return currentAccountSubject
                .switchMap(Account::getConversationsSubject);
    }

    public void readMessages(String accountId, Uri contact) {
        Account account = mAccountService.getAccount(accountId);
        if (account != null)
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
        NavigableMap<Long, Interaction> messages = conversation.getRawHistory();
        for (Interaction e : messages.descendingMap().values()) {
            if (!(e.getType().equals(InteractionType.TEXT)))
                continue;
            if (e.isRead()) {
                break;
            }
            e.read();
            mHistoryService.updateInteraction(e, conversation.getAccountId()).subscribe();
            updated = true;
        }
        return updated;
    }

    public Single<TextMessage> sendTextMessage(String account, Conversation c, Uri to, String txt) {
        return mCallService.sendAccountTextMessage(account, to.getRawUriString(), txt)
                .map(id -> {
                    TextMessage message = new TextMessage(null, account, Long.toString(id), c, txt);
                    if (c.isVisible())
                        message.read();
                    mHistoryService.insertInteraction(account, c, message).subscribe();
                    c.addTextMessage(message);
                    mAccountService.getAccount(account).conversationUpdated(c);
                    return message;
                });
    }

    public void sendTextMessage(Conversation c, Conference conf, String txt) {
        mCallService.sendTextMessage(conf.getId(), txt);
        TextMessage message = new TextMessage(null, c.getAccountId(), conf.getId(), c, txt);
        message.read();
        mHistoryService.insertInteraction(c.getAccountId(), c, message).subscribe();
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
            String peerId = to.getUri();
            Conversation conversation = mAccountService.getAccount(account).getByUri(to);
            DataTransfer transfer = new DataTransfer(conversation, account, file.getName(), true, file.length(), 0, 0L);
            mHistoryService.insertInteraction(account, conversation, transfer).blockingAwait();
            File dest = mDeviceRuntimeService.getConversationPath(peerId, transfer.getStoragePath());
            if (!FileUtils.moveFile(file, dest)) {
                Log.e(TAG, "sendFile: can't move file to " + dest);
                return;
            }

            // send file
            mAccountService.sendFile(transfer, dest);
        }).subscribeOn(Schedulers.io());
    }


    public void deleteConversationItem(Interaction element) {
        if (element.getType() == InteractionType.DATA_TRANSFER) {
            DataTransfer transfer = (DataTransfer) element;
            File file = mDeviceRuntimeService.getConversationPath(transfer.getPeerId(), transfer.getStoragePath());
            mDisposableBag.add(Completable.mergeArrayDelayError(
                    mHistoryService.deleteInteraction(element.getId(), element.getAccount()),
                    Completable.fromAction(file::delete).subscribeOn(Schedulers.io()))
                    .andThen(startConversation(transfer.getAccount(), new Uri(transfer.getConversation().getParticipant())))
                    .subscribe(c -> c.removeInteraction(transfer),
                            e -> Log.e(TAG, "Can't delete file transfer", e)));
        } else {
            // handling is the same for calls and texts
            mDisposableBag.add(Completable.mergeArrayDelayError(mHistoryService.deleteInteraction(element.getId(), element.getAccount()).subscribeOn(Schedulers.io()))
                    .andThen(startConversation(element.getAccount(), new Uri(element.getConversation().getParticipant())))
                    .subscribe(c -> c.removeInteraction(element),
                            e -> Log.e(TAG, "Can't delete message", e)));
        }
    }

    public void cancelMessage(Interaction message) {
        mDisposableBag.add(Completable.mergeArrayDelayError(
                mCallService.cancelMessage(message.getAccount(), message.getId()).subscribeOn(Schedulers.io()))
                .andThen(startConversation(message.getAccount(), new Uri(message.getConversation().getParticipant())))
                .subscribe(c -> c.removeInteraction(message),
                        e -> Log.e(TAG, "Can't cancel message sending", e)));
    }

    /**
     * Loads the smartlist from cache or database
     *
     * @param account the user account
     * @return an account single
     */
    private Single<Account> loadSmartlist(final Account account) {
        synchronized (account) {
            if (account.historyLoader == null) {
                Log.d(TAG, "loadSmartlist(): start loading");
                account.historyLoader = getSmartlist(account);
            }
            return account.historyLoader;
        }
    }

    /**
     * Loads history for a specific conversation from cache or database
     *
     * @param account    the user account
     * @param contactUri the conversation participant
     * @return a conversation single
     */
    public Single<Conversation> loadConversationHistory(final Account account, final Uri contactUri) {
        Conversation conversation = account.getByUri(contactUri);
        synchronized (conversation) {
            if (conversation.getId() == null) {
                return Single.just(conversation);
            }
            Single<Conversation> ret = conversation.getLoaded();
            if (ret == null) {
                ret = getConversationHistory(account, conversation);
                conversation.setLoaded(ret);
            }
            return ret;
        }
    }

    private Observable<SmartListViewModel> observeConversation(Account account, Conversation conversation) {
        return account.getConversationSubject()
                .filter(c -> c == conversation)
                .startWith(conversation)
                .switchMap(c -> mContactService
                        .observeContact(conversation.getAccountId(), conversation.getContact())
                        .map(contact -> new SmartListViewModel(conversation.getAccountId(), contact, conversation.getLastEvent())));
    }

    public Observable<List<Observable<SmartListViewModel>>> getSmartList(Observable<Account> currentAccount) {
        return currentAccount
                .switchMap(account -> account.getConversationsSubject()
                        .switchMapSingle(conversations -> Observable.fromIterable(conversations)
                                .map(conv -> observeConversation(account, conv))
                                .toList()));
    }

    /**
     * Loads the smartlist from the database and updates the view
     *
     * @param account the user account
     */
    private Single<Account> getSmartlist(final Account account) {
        return mHistoryService.getSmartlist(account.getAccountID())
                .map(conversationHistoryList -> {
                    List<Conversation> conversations = new ArrayList<>();
                    for (Interaction e : conversationHistoryList) {
                        Conversation conversation = account.getByUri(e.getConversation().getParticipant());
                        if (conversation == null)
                            continue;
                        conversation.setId(e.getConversation().getId());
                        conversation.addElement(e);
                        conversations.add(conversation);
                    }
                    account.setHistoryLoaded(conversations);
                    return account;
                })
                .cache();
    }

    /**
     * Loads a conversation's history from the database
     *
     * @param account      the user account
     * @param conversation a conversation object with a valid conversation ID
     * @return a conversation single
     */
    private Single<Conversation> getConversationHistory(final Account account, final Conversation conversation) {
        Log.d(TAG, "getConversationHistory()");

        return mHistoryService.getConversationHistory(account.getAccountID(), conversation.getId())
                .map(loadedConversation -> {
                    if (loadedConversation.isEmpty())
                        return null;
                    conversation.clearHistory(true);
                    conversation.setHistory(loadedConversation);
                    return conversation;
                })
                .cache();
    }

    public Completable clearHistory(final String accountId, final Uri contact) {
        return mHistoryService
                .clearHistory(contact.getUri(), accountId, false)
                .doOnSubscribe(s -> {
                    Account account = mAccountService.getAccount(accountId);
                    if (account != null) {
                        account.clearHistory(contact, false);
                    }
                });
    }

    public Completable clearAllHistory() {
        List<Account> accounts = mAccountService.getAccounts();
        return mHistoryService
                .clearHistory(accounts)
                .doOnSubscribe(s -> {
                    for (Account account : accounts) {
                        if (account != null) {
                            account.clearAllHistory();
                        }
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
            mHistoryService.updateInteraction(txt, txt.getAccount()).subscribe();
        }
        getAccountSubject(txt.getAccount())
                .flatMapObservable(Account::getConversationsSubject)
                .firstOrError()
                .subscribeOn(Schedulers.io())
                .subscribe(c -> updateTextNotifications(txt.getAccount(), c), e -> Log.e(TAG, e.getMessage()));
    }

    public void acceptRequest(String accountId, Uri contactUri) {
        if (accountId == null || contactUri == null)
            return;
        mPreferencesService.removeRequestPreferences(accountId, contactUri.getRawRingId());
        mAccountService.acceptTrustRequest(accountId, contactUri);
    }

    public void discardRequest(String accountId, Uri contact) {
        mHistoryService.clearHistory(contact.getUri(), accountId, true).subscribe();
        mPreferencesService.removeRequestPreferences(accountId, contact.getRawRingId());
        mAccountService.discardTrustRequest(accountId, contact);
    }

    private void handleDataTransferEvent(DataTransfer transfer) {
        Conversation conversation = mAccountService.getAccount(transfer.getAccount()).onDataTransferEvent(transfer);
        if (transfer.getStatus() == (InteractionStatus.TRANSFER_CREATED) && !transfer.isOutgoing()) {
            if (transfer.canAutoAccept()) {
                mAccountService.acceptFileTransfer(transfer);
            }
        }
        mNotificationService.handleDataTransferNotification(transfer, conversation.getContact(), conversation.isVisible());
    }

    private void onConfStateChange(Conference conference) {
        Log.d(TAG, "onConfStateChange Thread id: " + Thread.currentThread().getId());
    }

    private void onCallStateChange(SipCall call) {
        Log.d(TAG, "onCallStateChange Thread id: " + Thread.currentThread().getId());
        SipCall.CallStatus newState = call.getCallStatus();
        boolean incomingCall = newState == SipCall.CallStatus.RINGING && call.isIncoming();
        mHardwareService.updateAudioState(newState, incomingCall, !call.isAudioOnly());

        Account account = mAccountService.getAccount(call.getAccount());
        CallContact contact = call.getContact();
        Conversation conversation = (contact == null || account == null) ? null : account.getByUri(contact.getPrimaryUri());
        Conference conference = null;
        if (conversation != null) {
            conference = conversation.getConference(call.getDaemonIdString());
            if (conference == null) {
                if (newState == SipCall.CallStatus.OVER)
                    return;
                conference = new Conference(call);
                conversation.addConference(conference);
                account.updated(conversation);
            }
        }

        Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + newState);
        if ((call.isRinging() || newState == SipCall.CallStatus.CURRENT) && call.getTimestamp() == 0) {
            call.setTimestamp(System.currentTimeMillis());
        }

        if (incomingCall) {
            mNotificationService.handleCallNotification(conference, false);
            mHardwareService.setPreviewSettings();
        } else if ((newState == SipCall.CallStatus.CURRENT && call.isIncoming())
                || newState == SipCall.CallStatus.RINGING && !call.isIncoming()) {
            mNotificationService.handleCallNotification(conference, false);
            mAccountService.sendProfile(call.getDaemonIdString(), call.getAccount());
        } else if (newState == SipCall.CallStatus.HUNGUP
                || newState == SipCall.CallStatus.BUSY
                || newState == SipCall.CallStatus.FAILURE
                || newState == SipCall.CallStatus.OVER) {
            mNotificationService.handleCallNotification(conference, true);
            mHardwareService.closeAudioState();
            long now = System.currentTimeMillis();
            if (call.getTimestamp() == 0) {
                call.setTimestamp(now);
            }
            if (newState == SipCall.CallStatus.HUNGUP || call.getTimestampEnd() == 0) {
                call.setTimestampEnd(now);
            }
            if (conversation != null && conference.removeParticipant(call)) {
                mHistoryService.insertInteraction(account.getAccountID(), conversation, call).subscribe();
                conversation.addCall(call);
                if (call.isIncoming() && call.isMissed()) {
                    mNotificationService.showMissedCallNotification(call);
                }
                account.updated(conversation);
            }
            mCallService.removeCallForId(call.getDaemonIdString());
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
        mNotificationService.removeTransferNotification(id);
        DataTransfer transfer = mAccountService.getDataTransfer(id);
        if (transfer != null)
            deleteConversationItem(transfer);
    }

    public Completable removeConversation(String accountId, Uri contact) {
        return mHistoryService
                .clearHistory(contact.getUri(), accountId, true)
                .doOnSubscribe(s -> {
                    Account account = mAccountService.getAccount(accountId);
                    account.clearHistory(contact, true);
                    mAccountService.removeContact(accountId, contact.getRawRingId(), false);
                });
    }
}