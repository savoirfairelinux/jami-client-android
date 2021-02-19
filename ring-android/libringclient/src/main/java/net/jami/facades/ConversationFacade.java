/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package net.jami.facades;

import net.jami.services.AccountService;
import net.jami.services.CallService;
import net.jami.services.ContactService;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;
import net.jami.services.HistoryService;
import net.jami.services.NotificationService;
import net.jami.services.PreferencesService;
import net.jami.smartlist.SmartListViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import net.jami.model.Account;
import net.jami.model.CallContact;
import net.jami.model.Conference;
import net.jami.model.Conversation;
import net.jami.model.DataTransfer;
import net.jami.model.Interaction;
import net.jami.model.Interaction.InteractionStatus;
import net.jami.model.Interaction.InteractionType;
import net.jami.model.SipCall;
import net.jami.model.TextMessage;
import net.jami.model.Uri;
import net.jami.utils.Log;
import net.jami.utils.FileUtils;
import net.jami.utils.Tuple;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class ConversationFacade {

    private final static String TAG = ConversationFacade.class.getSimpleName();

    private final net.jami.services.AccountService mAccountService;
    private final net.jami.services.HistoryService mHistoryService;
    private final net.jami.services.CallService mCallService;
    private final net.jami.services.ContactService mContactService;
    private final net.jami.services.NotificationService mNotificationService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    PreferencesService mPreferencesService;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private final Observable<net.jami.model.Account> currentAccountSubject;

    private final Subject<net.jami.model.Conversation> conversationSubject = PublishSubject.create();

    public ConversationFacade(HistoryService historyService,
                              CallService callService,
                              net.jami.services.AccountService accountService,
                              ContactService contactService,
                              NotificationService notificationService) {
        mHistoryService = historyService;
        mCallService = callService;
        mAccountService = accountService;
        mContactService = contactService;
        mNotificationService = notificationService;

        currentAccountSubject = mAccountService
                .getCurrentAccountSubject()
                .switchMapSingle(this::loadSmartlist);

        mDisposableBag.add(mCallService.getCallsUpdates()
                .subscribe(this::onCallStateChange));

        /*mDisposableBag.add(mCallService.getConnectionUpdates()
                    .subscribe(mNotificationService::onConnectionUpdate));*/

        mDisposableBag.add(mCallService.getConfsUpdates()
                .observeOn(Schedulers.io())
                .subscribe(this::onConfStateChange));

        mDisposableBag.add(currentAccountSubject
                .switchMap(a -> a.getPendingSubject()
                        .doOnNext(p -> mNotificationService.showIncomingTrustRequestNotification(a)))
                .subscribe());

        mDisposableBag.add(mAccountService.getIncomingRequests()
                .concatMapSingle(r -> getAccountSubject(r.getAccountId()))
                .subscribe(mNotificationService::showIncomingTrustRequestNotification,
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
                .getIncomingSwarmMessages()
                .subscribe(this::parseNewMessage,
                        e -> Log.e(TAG, "Error adding text message", e)));

        mDisposableBag.add(mAccountService.getLocationUpdates()
                .concatMapSingle(location -> getAccountSubject(location.getAccount())
                        .map(a -> {
                            long expiration = a.onLocationUpdate(location);
                            mDisposableBag.add(Completable.timer(expiration, TimeUnit.MILLISECONDS)
                                    .subscribe(a::maintainLocation));
                            return location;
                        }))
                .subscribe());

        mDisposableBag.add(mAccountService.getObservableAccountList()
                .switchMap(accounts -> {
                    List<Observable<Tuple<Account, net.jami.model.Account.ContactLocationEntry>>> r = new ArrayList<>(accounts.size());
                    for (Account a : accounts)
                        r.add(a.getLocationUpdates().map(s -> new Tuple<>(a, s)));
                    return Observable.merge(r);
                })
                .distinctUntilChanged()
                .subscribe(t -> {
                    net.jami.utils.Log.e(TAG, "Location reception started for " + t.second.contact);
                    mNotificationService.showLocationNotification(t.first, t.second.contact);
                    mDisposableBag.add(t.second.location.doOnComplete(() ->
                            mNotificationService.cancelLocationNotification(t.first, t.second.contact)).subscribe());
                }));

        mDisposableBag.add(mAccountService
                .getMessageStateChanges()
                .concatMapSingle(txt -> getAccountSubject(txt.getAccount())
                        .map(a -> txt.getConversation() == null ? a.getSwarm(txt.getConversationId()) : a.getByUri(txt.getConversation().getParticipant()))
                        .doOnSuccess(conversation -> conversation.updateTextMessage(txt)))
                .subscribe(c -> {
                }, e -> net.jami.utils.Log.e(TAG, "Error updating text message", e)));

        mDisposableBag.add(mAccountService
                .getDataTransfers()
                .subscribe(this::handleDataTransferEvent,
                        e -> Log.e(TAG, "Error adding data transfer", e)));
    }

    public Observable<net.jami.model.Conversation> getUpdatedConversation() {
        return conversationSubject;
    }

    public Single<net.jami.model.Conversation> startConversation(String accountId, final Uri contactId) {
        return getAccountSubject(accountId)
                .map(account -> account.getByUri(contactId));
    }

    public Observable<net.jami.model.Account> getCurrentAccountSubject() {
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

    public String readMessages(String accountId, Uri contact) {
        Account account = mAccountService.getAccount(accountId);
        return account != null ?
             readMessages(account, account.getByUri(contact), true) : null;
    }

    public String readMessages(Account account, net.jami.model.Conversation conversation, boolean cancelNotification) {
        if (conversation != null) {
            String lastMessage = readMessages(conversation);
            if (lastMessage != null) {
                account.refreshed(conversation);
                if (mPreferencesService.getSettings().isAllowReadIndicator()) {
                    mAccountService.setMessageDisplayed(account.getAccountID(), conversation.getUri().getRawRingId(), lastMessage);
                }
                if (cancelNotification) {
                    mNotificationService.cancelTextNotification(account.getAccountID(), conversation.getUri());
                }
            }
            return lastMessage;
        }
        return null;
    }

    private String readMessages(net.jami.model.Conversation conversation) {
        String lastRead = null;
        if (conversation.isSwarm()) {
            lastRead = conversation.readMessages();
            if (lastRead != null)
                mHistoryService.setMessageRead(conversation.getAccountId(), conversation.getUri(), lastRead);
        } else {
            NavigableMap<Long, Interaction> messages = conversation.getRawHistory();
            for (Interaction e : messages.descendingMap().values()) {
                if (!(e.getType().equals(InteractionType.TEXT)))
                    continue;
                if (e.isRead()) {
                    break;
                }
                e.read();
                Long did = e.getDaemonId();
                if (lastRead == null && did != null && did != 0L)
                    lastRead = Long.toString(did, 16);
                mHistoryService.updateInteraction(e, conversation.getAccountId()).subscribe();
            }
        }
        return lastRead;
    }

    public Completable sendTextMessage(Conversation c, Uri to, String txt) {
        if (c.isSwarm()) {
            mAccountService.sendConversationMessage(c.getAccountId(), c.getUri(), txt);
            return Completable.complete();
        }
        return mCallService.sendAccountTextMessage(c.getAccountId(), to.getRawUriString(), txt)
                .map(id -> {
                    net.jami.model.TextMessage message = new net.jami.model.TextMessage(null, c.getAccountId(), Long.toHexString(id), c, txt);
                    if (c.isVisible())
                        message.read();
                    mHistoryService.insertInteraction(c.getAccountId(), c, message).subscribe();
                    c.addTextMessage(message);
                    mAccountService.getAccount(c.getAccountId()).conversationUpdated(c);
                    return message;
                }).ignoreElement();
    }

    public void sendTextMessage(Conversation c, Conference conf, String txt) {
        mCallService.sendTextMessage(conf.getId(), txt);
        net.jami.model.TextMessage message = new TextMessage(null, c.getAccountId(), conf.getId(), c, txt);
        message.read();
        mHistoryService.insertInteraction(c.getAccountId(), c, message).subscribe();
        c.addTextMessage(message);
    }

    public void setIsComposing(String accountId, Uri conversationUri, boolean isComposing) {
        mCallService.setIsComposing(accountId, conversationUri.getRawRingId(), isComposing);
    }

    public Completable sendFile(Conversation conversation, Uri to, File file) {
        return Single.fromCallable(() -> {
            if (file == null || !file.exists() || !file.canRead()) {
                net.jami.utils.Log.w(TAG, "sendFile: file not found or not readable: " + file);
                return null;
            }

            DataTransfer transfer = new DataTransfer(conversation, to.getRawRingId(), conversation.getAccountId(), file.getName(), true, file.length(), 0, 0L);
            if (conversation.isSwarm()) {
                transfer.setSwarmInfo(conversation.getUri().getRawRingId(), null, null);
            } else {
                mHistoryService.insertInteraction(conversation.getAccountId(), conversation, transfer).blockingAwait();
            }

            File dest = mDeviceRuntimeService.getConversationPath(conversation.getUri().getRawRingId(), transfer.getStoragePath());
            if (!FileUtils.moveFile(file, dest)) {
                net.jami.utils.Log.e(TAG, "sendFile: can't move file to " + dest);
                return null;
            }

            transfer.destination = dest;
            return transfer;
        })
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(mAccountService::sendFile);
    }


    public void deleteConversationItem(net.jami.model.Conversation conversation, Interaction element) {
        if (element.getType() == InteractionType.DATA_TRANSFER) {
            DataTransfer transfer = (net.jami.model.DataTransfer) element;
            if (transfer.getStatus() == InteractionStatus.TRANSFER_ONGOING) {
                mAccountService.cancelDataTransfer(conversation.getAccountId(), conversation.getUri().getRawRingId(), transfer.getDaemonId());
            } else {
                File file = mDeviceRuntimeService.getConversationPath(conversation.getUri().getRawRingId(), transfer.getStoragePath());
                mDisposableBag.add(Completable.mergeArrayDelayError(
                        mHistoryService.deleteInteraction(element.getId(), element.getAccount()),
                        Completable.fromAction(file::delete)
                                .subscribeOn(Schedulers.io()))
                        .subscribe(() -> conversation.removeInteraction(transfer),
                                e -> net.jami.utils.Log.e(TAG, "Can't delete file transfer", e)));
            }
        } else {
            // handling is the same for calls and texts
            mDisposableBag.add(Completable.mergeArrayDelayError(mHistoryService.deleteInteraction(element.getId(), element.getAccount()).subscribeOn(Schedulers.io()))
                    .andThen(startConversation(element.getAccount(), Uri.fromString(element.getConversation().getParticipant())))
                    .subscribe(c -> c.removeInteraction(element),
                            e -> net.jami.utils.Log.e(TAG, "Can't delete message", e)));
        }
    }

    public void cancelMessage(Interaction message) {
        mDisposableBag.add(Completable.mergeArrayDelayError(
                mCallService.cancelMessage(message.getAccount(), message.getId()).subscribeOn(Schedulers.io()))
                .andThen(startConversation(message.getAccount(), Uri.fromString(message.getConversation().getParticipant())))
                .subscribe(c -> c.removeInteraction(message),
                        e -> net.jami.utils.Log.e(TAG, "Can't cancel message sending", e)));
    }

    /**
     * Loads the smartlist from cache or database
     *
     * @param account the user account
     * @return an account single
     */
    private Single<net.jami.model.Account> loadSmartlist(final Account account) {
        synchronized (account) {
            if (account.historyLoader == null) {
                net.jami.utils.Log.d(TAG, "loadSmartlist(): start loading");
                account.historyLoader = getSmartlist(account);
            }
            return account.historyLoader;
        }
    }

    /**
     * Loads history for a specific conversation from cache or database
     *
     * @param account    the user account
     * @param conversationUri the conversation
     * @return a conversation single
     */
    public Single<Conversation> loadConversationHistory(final Account account, final Uri conversationUri) {
        net.jami.model.Conversation conversation = account.getByUri(conversationUri);
        if (conversation == null)
            return Single.error(new RuntimeException("Can't get conversation"));
        synchronized (conversation) {
            if (conversation.isSwarm()) {
                loadMore(conversation);
                Single<net.jami.model.Conversation> ret = Single.just(conversation);
                conversation.setLoaded(ret);
                return ret;
            }
            if (conversation.getId() == null) {
                return Single.just(conversation);
            }
            Single<net.jami.model.Conversation> ret = conversation.getLoaded();
            if (ret == null) {
                ret = getConversationHistory(conversation);
                conversation.setLoaded(ret);
            }
            return ret;
        }
    }

    private Observable<net.jami.smartlist.SmartListViewModel> observeConversation(Account account, net.jami.model.Conversation conversation, boolean hasPresence) {
        return Observable.merge(account.getConversationSubject()
                .filter(c -> c == conversation)
                .startWith(conversation),
                mContactService
                        .observeContact(conversation.getAccountId(), conversation.getContacts(), hasPresence))
        .map(e -> new net.jami.smartlist.SmartListViewModel(conversation, hasPresence));
        /*return account.getConversationSubject()
                .filter(c -> c == conversation)
                .startWith(conversation)
                .switchMap(c -> mContactService
                        .observeContact(c.getAccountId(), c.getContacts(), hasPresence)
                        .map(contact -> new SmartListViewModel(c, hasPresence)));*/
    }
    public Observable<List<Observable<net.jami.smartlist.SmartListViewModel>>> getSmartList(Observable<net.jami.model.Account> currentAccount, boolean hasPresence) {
        return currentAccount.switchMap(account -> account.getConversationsSubject()
                .switchMapSingle(conversations -> Observable.fromIterable(conversations)
                        .map(conversation -> observeConversation(account, conversation, hasPresence))
                        .toList()));
    }
    public Observable<List<net.jami.smartlist.SmartListViewModel>> getContactList(Observable<Account> currentAccount) {
        return currentAccount.switchMap(account -> account.getConversationsSubject()
                .switchMapSingle(conversations -> Observable.fromIterable(conversations)
                        .filter(conversation -> !conversation.isSwarm())
                        .map(conversation -> new net.jami.smartlist.SmartListViewModel(conversation,false))
                        .toList()));
    }
    public Observable<List<Observable<net.jami.smartlist.SmartListViewModel>>> getPendingList(Observable<Account> currentAccount) {
        return currentAccount.switchMap(account -> account.getPendingSubject()
                .switchMapSingle(conversations -> Observable.fromIterable(conversations)
                        .map(conversation -> observeConversation(account, conversation, false))
                        .toList()));
    }

    public Observable<List<Observable<net.jami.smartlist.SmartListViewModel>>> getSmartList(boolean hasPresence) {
        return getSmartList(mAccountService.getCurrentAccountSubject(), hasPresence);
    }
    public Observable<List<Observable<net.jami.smartlist.SmartListViewModel>>> getPendingList() {
        return getPendingList(mAccountService.getCurrentAccountSubject());
    }
    public Observable<List<net.jami.smartlist.SmartListViewModel>> getContactList() {
        return getContactList(mAccountService.getCurrentAccountSubject());
    }

    private Single<List<Observable<net.jami.smartlist.SmartListViewModel>>> getSearchResults(Account account, String query) {
        Uri uri = Uri.fromString(query);
        if (account.isSip()) {
            CallContact contact = account.getContactFromCache(uri);
            return mContactService.loadContactData(contact, account.getAccountID())
                    .andThen(Single.just(Collections.singletonList(Observable.just(new net.jami.smartlist.SmartListViewModel(account.getAccountID(), contact, contact.getPrimaryNumber(), null)))));
        } else if (uri.isHexId()) {
            return mContactService.getLoadedContact(account.getAccountID(), account.getContactFromCache(uri))
                    .map(contact -> Collections.singletonList(Observable.just(new net.jami.smartlist.SmartListViewModel(account.getAccountID(), contact, contact.getPrimaryNumber(), null))));
        } else if (account.canSearch() && !query.contains("@")) {
            return mAccountService.searchUser(account.getAccountID(), query)
                    .map(AccountService.UserSearchResult::getResultsViewModels);
        } else {
            return mAccountService.findRegistrationByName(account.getAccountID(), "", query)
                    .map(result -> result.state == 0 ? Collections.singletonList(observeConversation(account, account.getByUri(result.address), false)) : Collections.emptyList());
        }
    }
    private Observable<List<Observable<net.jami.smartlist.SmartListViewModel>>> getSearchResults(Account account, Observable<String> query) {
        return query.switchMapSingle(q -> q.isEmpty()
                        ? net.jami.smartlist.SmartListViewModel.EMPTY_LIST
                        : getSearchResults(account, q))
                .distinctUntilChanged();
    }
    public Observable<List<Observable<net.jami.smartlist.SmartListViewModel>>> getFullList(Observable<Account> currentAccount, Observable<String> query, boolean hasPresence) {
        return currentAccount.switchMap(account -> Observable.combineLatest(
                account.getConversationsSubject(),
                getSearchResults(account, query),
                query,
                (conversations, searchResults, q) -> {
                    List<Observable<net.jami.smartlist.SmartListViewModel>> newList = new ArrayList<>(conversations.size() + searchResults.size() + 2);
                    if (!searchResults.isEmpty()) {
                        newList.add(net.jami.smartlist.SmartListViewModel.TITLE_PUBLIC_DIR);
                        newList.addAll(searchResults);
                    }
                    if (!conversations.isEmpty()) {
                        if (q.isEmpty()) {
                            for (net.jami.model.Conversation conversation : conversations)
                                newList.add(observeConversation(account, conversation, hasPresence));
                        } else {
                            String lq = q.toLowerCase();
                            newList.add(SmartListViewModel.TITLE_CONVERSATIONS);
                            int nRes = 0;
                            for (Conversation conversation : conversations) {
                                if (conversation.matches(lq)) {
                                    newList.add(observeConversation(account, conversation, hasPresence));
                                    nRes++;
                                }
                            }
                            if (nRes == 0)
                                newList.remove(newList.size() - 1);
                        }
                    }
                    return newList;
                }));
    }

    /**
     * Loads the smartlist from the database and updates the view
     *
     * @param account the user account
     */
    private Single<net.jami.model.Account> getSmartlist(final Account account) {
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
     * @param conversation a conversation object with a valid conversation ID
     * @return a conversation single
     */
    private Single<net.jami.model.Conversation> getConversationHistory(final net.jami.model.Conversation conversation) {
        Log.d(TAG, "getConversationHistory() " + conversation.getAccountId() + " " + conversation.getUri());

        return mHistoryService.getConversationHistory(conversation.getAccountId(), conversation.getId())
                .map(loadedConversation -> {
                    /*if (loadedConversation.isEmpty())
                        return conversation;*/
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

    public void updateTextNotifications(String accountId, List<net.jami.model.Conversation> conversations) {
        net.jami.utils.Log.d(TAG, "updateTextNotifications() " + accountId + " " + conversations.size());

        for (net.jami.model.Conversation conversation : conversations) {
            mNotificationService.showTextNotification(accountId, conversation);
        }
    }

    private void parseNewMessage(final net.jami.model.TextMessage txt) {
        if (txt.isRead()) {
            if (txt.getMessageId() == null) {
                mHistoryService.updateInteraction(txt, txt.getAccount()).subscribe();
            }
            if (mPreferencesService.getSettings().isAllowReadIndicator()) {
                if (txt.getMessageId() != null) {
                    mAccountService.setMessageDisplayed(txt.getAccount(), txt.getConversationId(), txt.getMessageId());
                } else {
                    mAccountService.setMessageDisplayed(txt.getAccount(), txt.getAuthor(), Long.toString(txt.getDaemonId(), 16));
                }
            }
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
        net.jami.model.Conversation conversation = mAccountService.getAccount(transfer.getAccount()).onDataTransferEvent(transfer);
        if (transfer.getStatus() == InteractionStatus.TRANSFER_CREATED && !transfer.isOutgoing()) {
            if (transfer.canAutoAccept(mPreferencesService.getMaxFileAutoAccept(transfer.getAccount()))) {
                mAccountService.acceptFileTransfer(conversation, transfer);
            }
        }
        mNotificationService.handleDataTransferNotification(transfer, conversation, conversation.isVisible());
    }

    private void onConfStateChange(Conference conference) {
        net.jami.utils.Log.d(TAG, "onConfStateChange Thread id: " + Thread.currentThread().getId());
    }

    private void onCallStateChange(SipCall call) {
        net.jami.utils.Log.d(TAG, "onCallStateChange Thread id: " + Thread.currentThread().getId());
        SipCall.CallStatus newState = call.getCallStatus();
        boolean incomingCall = newState == SipCall.CallStatus.RINGING && call.isIncoming();
        mHardwareService.updateAudioState(newState, incomingCall, !call.isAudioOnly());

        Account account = mAccountService.getAccount(call.getAccount());
        if (account == null)
            return;
        net.jami.model.CallContact contact = call.getContact();
        String conversationId = call.getConversationId();
        net.jami.utils.Log.w(TAG, "CallStateChange " + call.getId() + " conversationId:" + conversationId);

        Conversation conversation = account == null
                ? null
                : (conversationId == null
                    ? (contact == null
                        ? null
                        : account.getByUri(contact.getUri()))
                    : account.getSwarm(conversationId));
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
            if (conference != null && conference.removeParticipant(call) && !conversation.isSwarm()) {
                Log.w(TAG, "Adding call history for conversation " + conversation.getUri());
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

    public Single<SipCall> placeCall(String accountId, Uri contactUri, boolean video) {
        //String rawId = contactUri.getRawRingId();
        return getAccountSubject(accountId).flatMap(account -> {
            //CallContact contact = account.getContact(rawId);
            //if (contact == null)
            //    mAccountService.addContact(accountId, rawId);
            return mCallService.placeCall(accountId, null, contactUri, video);
        });
    }

    public void cancelFileTransfer(String accountId, Uri conversationId, long id) {
        mAccountService.cancelDataTransfer(accountId, conversationId.isSwarm() ? conversationId.getRawRingId() : "", id);
        mNotificationService.removeTransferNotification(id);
        net.jami.model.DataTransfer transfer = mAccountService.getAccount(accountId).getDataTransfer(id);
        if (transfer != null)
            deleteConversationItem((Conversation) transfer.getConversation(), transfer);
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

    public Single<net.jami.model.Conversation> createConversation(String accountId, Collection<net.jami.model.CallContact> currentSelection) {
        List<String> contactIds = new ArrayList<>(currentSelection.size());
        for (CallContact contact : currentSelection)
            contactIds.add(contact.getPrimaryNumber());
        return mAccountService.startConversation(accountId, contactIds);
    }

    public void loadMore(net.jami.model.Conversation conversation) {
        Collection<String> roots = conversation.getSwarmRoot();
        if (roots.isEmpty())
            mAccountService.loadConversationHistory(conversation.getAccountId(), conversation.getUri(), "", 16);
        else {
            for (String root : roots)
                mAccountService.loadConversationHistory(conversation.getAccountId(), conversation.getUri(), root, 16);
        }
    }
}