/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: Raphaël Brulé <raphael.brule@savoirfairelinux.com>
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
package net.jami.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.jami.daemon.Blob;
import net.jami.daemon.DataTransferInfo;
import net.jami.daemon.JamiService;
import net.jami.daemon.StringMap;
import net.jami.daemon.UintVect;
import net.jami.model.Account;
import net.jami.model.AccountConfig;
import net.jami.model.Call;
import net.jami.model.Codec;
import net.jami.model.ConfigKey;
import net.jami.model.Contact;
import net.jami.model.ContactEvent;
import net.jami.model.Conversation;
import net.jami.model.DataTransfer;
import net.jami.model.DataTransferError;
import net.jami.model.Interaction;
import net.jami.model.Interaction.InteractionStatus;
import net.jami.model.TextMessage;
import net.jami.model.TrustRequest;
import net.jami.model.Uri;
import net.jami.smartlist.SmartListViewModel;
import net.jami.utils.FileUtils;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import net.jami.utils.SwigNativeConverter;
import net.jami.utils.VCardUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * This service handles the accounts
 * - Load and manage the accounts stored in the daemon
 * - Keep a local cache of the accounts
 * - handle the callbacks that are send by the daemon
 */
public class AccountService {
    private static final String TAG = AccountService.class.getSimpleName();

    private static final int VCARD_CHUNK_SIZE = 1000;
    private static final long DATA_TRANSFER_REFRESH_PERIOD = 500;

    private static final int PIN_GENERATION_SUCCESS = 0;
    private static final int PIN_GENERATION_WRONG_PASSWORD = 1;
    private static final int PIN_GENERATION_NETWORK_ERROR = 2;

    public AccountService(ScheduledExecutorService executor, HistoryService historyService, DeviceRuntimeService deviceRuntimeService, VCardService vCardService)
    {
        mExecutor = executor;
        mHistoryService = historyService;
        mDeviceRuntimeService = deviceRuntimeService;
        mVCardService = vCardService;
    }

    private final ScheduledExecutorService mExecutor;
    private HistoryService mHistoryService;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final VCardService mVCardService;

    private Account mCurrentAccount;
    private List<Account> mAccountList = new ArrayList<>();
    private boolean mHasSipAccount;
    private boolean mHasRingAccount;

    private DataTransfer mStartingTransfer = null;

    private final BehaviorSubject<List<Account>> accountsSubject = BehaviorSubject.create();
    private final Subject<Account> accountSubject = PublishSubject.create();
    private final Observable<Account> currentAccountSubject = accountsSubject
            .filter(l -> !l.isEmpty())
            .map(l -> l.get(0))
            .distinctUntilChanged();

    public static class Message {
        String accountId;
        String messageId;
        String callId;
        String author;
        Map<String, String> messages;
    }
    public static class Location {
        public enum Type {
            position,
            stop
        }
        Type type;
        String accountId;
        String callId;
        Uri peer;
        long time;
        double latitude;
        double longitude;

        public Type getType() {
            return type;
        }

        public String getAccount() {
            return accountId;
        }

        public Uri getPeer() {
            return peer;
        }

        public long getDate() {
            return time;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    private final Subject<Message> incomingMessageSubject = PublishSubject.create();
    private final Subject<Interaction> incomingSwarmMessageSubject = PublishSubject.create();

    private final Observable<TextMessage> incomingTextMessageSubject = incomingMessageSubject
            .flatMapMaybe(msg -> {
                String message = msg.messages.get(CallService.MIME_TEXT_PLAIN);
                if (message != null) {
                    return mHistoryService
                            .incomingMessage(msg.accountId, msg.messageId, msg.author, message)
                            .toMaybe();
                }
                return Maybe.empty();
            })
            .share();

    private final Observable<Location> incomingLocationSubject = incomingMessageSubject
            .flatMapMaybe(msg -> {
                try {
                    String loc = msg.messages.get(CallService.MIME_GEOLOCATION);
                    if (loc == null)
                        return Maybe.empty();

                    JsonObject obj = JsonParser.parseString(loc).getAsJsonObject();
                    if (obj.size() < 2)
                        return Maybe.empty();
                    Location l = new Location();

                    JsonElement type = obj.get("type");
                    if (type == null || type.getAsString().equals(Location.Type.position.toString())) {
                        l.type = Location.Type.position;
                        l.latitude = obj.get("lat").getAsDouble();
                        l.longitude = obj.get("long").getAsDouble();
                    } else if (type.getAsString().equals(Location.Type.stop.toString())) {
                        l.type = Location.Type.stop;
                    }
                    l.time = obj.get("time").getAsLong();
                    l.accountId = msg.accountId;
                    l.callId = msg.callId;
                    l.peer = Uri.fromId(msg.author);
                    return Maybe.just(l);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to receive geolocation", e);
                    return Maybe.empty();
                }
            })
            .share();

    private final Subject<Interaction> messageSubject = PublishSubject.create();
    private final Subject<DataTransfer> dataTransferSubject = PublishSubject.create();
    private final Subject<TrustRequest> incomingRequestsSubject = PublishSubject.create();

    public void refreshAccounts() {
        accountsSubject.onNext(mAccountList);
    }

    public static class RegisteredName {
        public String accountId;
        public String name;
        public String address;
        public int state;
    }
    public static class UserSearchResult {
        private final String accountId;
        private final String query;

        public int state;
        public List<Contact> results;

        public UserSearchResult(String account, String query) {
            accountId = account;
            this.query = query;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getQuery() {
            return query;
        }

        public List<Observable<SmartListViewModel>> getResultsViewModels() {
            List<Observable<SmartListViewModel>> vms = new ArrayList<>(results.size());
            for (Contact user : results) {
                vms.add(Observable.just(new SmartListViewModel(accountId, user, null)));
            }
            return vms;
        }
    }

    private final Subject<RegisteredName> registeredNameSubject = PublishSubject.create();
    private final Subject<UserSearchResult> searchResultSubject = PublishSubject.create();

    private static class ExportOnRingResult {
        String accountId;
        int code;
        String pin;
    }

    private static class DeviceRevocationResult {
        String accountId;
        String deviceId;
        int code;
    }

    private static class MigrationResult {
        String accountId;
        String state;
    }

    private final Subject<ExportOnRingResult> mExportSubject = PublishSubject.create();
    private final Subject<DeviceRevocationResult> mDeviceRevocationSubject = PublishSubject.create();
    private final Subject<MigrationResult> mMigrationSubject = PublishSubject.create();

    public Observable<RegisteredName> getRegisteredNames() {
        return registeredNameSubject;
    }
    public Observable<UserSearchResult> getSearchResults() {
        return searchResultSubject;
    }

    public Observable<TextMessage> getIncomingMessages() {
        return incomingTextMessageSubject;
    }

    public Observable<TextMessage> getIncomingSwarmMessages() {
        return incomingSwarmMessageSubject
                .filter(i -> i instanceof TextMessage)
                .map(i -> (TextMessage) i);
    }

    public Observable<Location> getLocationUpdates() {
        return incomingLocationSubject;
    }

    public Observable<Interaction> getMessageStateChanges() {
        return messageSubject;
    }

    public Observable<TrustRequest> getIncomingRequests() {
        return incomingRequestsSubject;
    }

    /**
     * @return true if at least one of the loaded accounts is a SIP one
     */
    public boolean hasSipAccount() {
        return mHasSipAccount;
    }

    /**
     * @return true if at least one of the loaded accounts is a Ring one
     */
    public boolean hasRingAccount() {
        return mHasRingAccount;
    }

    /**
     * Loads the accounts from the daemon and then builds the local cache (also sends ACCOUNTS_CHANGED event)
     *
     * @param isConnected sets the initial connection state of the accounts
     */
    public void loadAccountsFromDaemon(final boolean isConnected) {
        mExecutor.execute(() -> {
            refreshAccountsCacheFromDaemon();
            setAccountsActive(isConnected);
        });
    }

    private static Account findAccount(List<Account> accounts, String accountId) {
        for (Account account : accounts)
            if (accountId.equals(account.getAccountID()))
                return account;
        return null;
    }

    private void refreshAccountsCacheFromDaemon() {
        Log.w(TAG, "refreshAccountsCacheFromDaemon");
        boolean hasSip = false, hasJami = false;
        List<Account> curList = mAccountList;
        List<String> accountIds = new ArrayList<>(JamiService.getAccountList());
        List<Account> newAccounts = new ArrayList<>(accountIds.size());
        for (String id : accountIds) {
            for (Account acc : curList)
                if (acc.getAccountID().equals(id)) {
                    newAccounts.add(acc);
                    break;
                }
        }

        // Cleanup removed accounts
        for (Account acc : curList)
            if (!newAccounts.contains(acc))
                acc.cleanup();

        for (String accountId : accountIds) {
            Account account = findAccount(newAccounts, accountId);
            Map<String, String> details = JamiService.getAccountDetails(accountId).toNative();
            List<Map<String, String>> credentials = JamiService.getCredentials(accountId).toNative();
            Map<String, String> volatileAccountDetails = JamiService.getVolatileAccountDetails(accountId).toNative();
            if (account == null) {
                account = new Account(accountId, details, credentials, volatileAccountDetails);
                newAccounts.add(account);
            } else {
                account.setDetails(details);
                account.setCredentials(credentials);
                account.setVolatileDetails(volatileAccountDetails);
            }
        }

        mAccountList = newAccounts;

        synchronized (newAccounts) {
            for (Account account : newAccounts) {
                String accountId = account.getAccountID();
                if (account.isSip()) {
                    hasSip = true;
                } else if (account.isJami()) {
                    hasJami = true;
                    boolean enabled = account.isEnabled();

                    account.setDevices(JamiService.getKnownRingDevices(accountId).toNative());
                    account.setContacts(JamiService.getContacts(accountId).toNative());
                    List<Map<String, String>> requests = JamiService.getTrustRequests(accountId).toNative();
                    for (Map<String, String> requestInfo : requests) {
                        TrustRequest request = new TrustRequest(accountId, requestInfo);
                        account.addRequest(request);
                    }
                    List<String> conversations = JamiService.getConversations(account.getAccountID());
                    Log.w(TAG, accountId + " loading conversations: " + conversations.size());
                    for (String conversationId : conversations) {
                        try {
                            Map<String, String> info = JamiService.conversationInfos(accountId, conversationId).toNative();
                            /*for (Map.Entry<String, String> i : info.entrySet()) {
                                Log.w(TAG, "conversation info: " + i.getKey() + " " + i.getValue());
                            }*/
                            Conversation.Mode mode = "true".equals(info.get("syncing")) ? Conversation.Mode.Syncing : Conversation.Mode.values()[Integer.parseInt(info.get("mode"))];
                            /*if ("true".equals(info.get("syncing"))) {
                                continue;
                            }*/
                            Conversation conversation = account.newSwarm(conversationId, mode);
                            //conversation.setLastMessageRead(mHistoryService.getLastMessageRead(accountId, conversation.getUri()));
                            if (mode != Conversation.Mode.Syncing) {
                                for (Map<String, String> member : JamiService.getConversationMembers(accountId, conversationId)) {
                                /*for (Map.Entry<String, String> i : member.entrySet()) {
                                    Log.w(TAG, "conversation member: " + i.getKey() + " " + i.getValue());
                                }*/
                                    Uri uri = Uri.fromId(member.get("uri"));
                                    //String role = member.get("role");
                                    String lastDisplayed = member.get("lastDisplayed");
                                    Contact contact = conversation.findContact(uri);
                                    if (contact == null) {
                                        contact = account.getContactFromCache(uri);
                                        conversation.addContact(contact);
                                    }
                                    if (!StringUtils.isEmpty(lastDisplayed) && contact.isUser()) {
                                        conversation.setLastMessageRead(lastDisplayed);
                                    }
                                }
                            }
                            conversation.setLastElementLoaded(Completable.defer(() -> loadMore(conversation, 2).ignoreElement()).cache());
                            account.conversationStarted(conversation);
                        } catch (Exception e) {
                            Log.w(TAG, "Error loading conversation", e);
                        }
                    }
                    for (Map<String, String> requestData : JamiService.getConversationRequests(account.getAccountID()).toNative()) {
                    /*for (Map.Entry<String, String> e : requestData.entrySet()) {
                        Log.e(TAG, "Request: " + e.getKey() + " " + e.getValue());
                    }*/
                        String conversationId = requestData.get("id");
                        Uri from = Uri.fromString(requestData.get("from"));
                        TrustRequest request = account.getRequest(from);
                        if (request != null) {
                            request.setConversationId(conversationId);
                        } else {
                            account.addRequest(new TrustRequest(account.getAccountID(), from, conversationId));
                        }
                    }

                    if (enabled) {
                        for (Contact contact : account.getContacts().values()) {
                            if (!contact.isUsernameLoaded())
                                JamiService.lookupAddress(accountId, "", contact.getUri().getRawRingId());
                        }
                    }
                }
            }

            mHasSipAccount = hasSip;
            mHasRingAccount = hasJami;
            if (!newAccounts.isEmpty()) {
                Account newAccount = newAccounts.get(0);
                if (mCurrentAccount != newAccount) {
                    mCurrentAccount = newAccount;
                }
            }
        }

        accountsSubject.onNext(newAccounts);
    }

    private Account getAccountByName(final String name) {
        synchronized (mAccountList) {
            for (Account acc : mAccountList) {
                if (acc.getAlias().equals(name))
                    return acc;
            }
        }
        return null;
    }

    public String getNewAccountName(final String prefix) {
        String name = String.format(prefix, "").trim();
        if (getAccountByName(name) == null) {
            return name;
        }
        int num = 1;
        do {
            num++;
            name = String.format(prefix, num).trim();
        } while (getAccountByName(name) != null);
        return name;
    }

    /**
     * Adds a new Account in the Daemon (also sends an ACCOUNT_ADDED event)
     * Sets the new account as the current one
     *
     * @param map the account details
     * @return the created Account
     */
    public Observable<Account> addAccount(final Map<String, String> map) {
        return Observable.fromCallable(() -> {
            String accountId = JamiService.addAccount(StringMap.toSwig(map));
            if (StringUtils.isEmpty(accountId)) {
                throw new RuntimeException("Can't create account.");
            }
            Account account = getAccount(accountId);
            if (account == null) {
                Map<String, String> accountDetails = JamiService.getAccountDetails(accountId).toNative();
                List<Map<String, String>> accountCredentials = JamiService.getCredentials(accountId).toNative();
                Map<String, String> accountVolatileDetails = JamiService.getVolatileAccountDetails(accountId).toNative();
                Map<String, String> accountDevices = JamiService.getKnownRingDevices(accountId).toNative();
                account = new Account(accountId, accountDetails, accountCredentials, accountVolatileDetails);
                account.setDevices(accountDevices);
                if (account.isSip()) {
                    account.setRegistrationState(AccountConfig.STATE_READY, -1);
                }
                mAccountList.add(account);
                accountsSubject.onNext(mAccountList);
            }
            return account;
        })
                .flatMap(account -> accountSubject
                        .filter(acc -> acc.getAccountID().equals(account.getAccountID()))
                        .startWithItem(account))
                .subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * @return the current Account from the local cache
     */
    public Account getCurrentAccount() {
        return mCurrentAccount;
    }
    public int getCurrentAccountIndex() {
        return mAccountList.indexOf(mCurrentAccount);
    }

    /**
     * Sets the current Account in the local cache (also sends a ACCOUNTS_CHANGED event)
     */
    public void setCurrentAccount(Account currentAccount) {
        if (mCurrentAccount == currentAccount)
            return;
        mCurrentAccount = currentAccount;

        // the account order is changed
        // the current Account is now on the top of the list
        final List<Account> accounts = mAccountList;
        List<String> orderedAccountIdList = new ArrayList<>(accounts.size());
        String selectedID = mCurrentAccount.getAccountID();
        orderedAccountIdList.add(selectedID);
        for (Account account : accounts) {
            if (account.getAccountID().contentEquals(selectedID)) {
                continue;
            }
            orderedAccountIdList.add(account.getAccountID());
        }

        setAccountOrder(orderedAccountIdList);
    }

    /**
     * @return the Account from the local cache that matches the accountId
     */
    public Account getAccount(String accountId) {
        if (!StringUtils.isEmpty(accountId)) {
            synchronized (mAccountList) {
                for (Account account : mAccountList)
                    if (accountId.equals(account.getAccountID()))
                        return account;
            }
        }
        return null;
    }

    public Single<Account> getAccountSingle(final String accountId) {
        return accountsSubject
                .firstOrError()
                .map(accounts -> {
                    for (Account account : accounts) {
                        if (account.getAccountID().equals(accountId)) {
                            return account;
                        }
                    }
                    Log.d(TAG, "getAccountSingle() can't find account " + accountId);
                    throw new IllegalArgumentException();
                });
    }

    public Observable<List<Account>> getObservableAccountList() {
        return accountsSubject;
    }

    public Subject<Account> getObservableAccounts() {
        return accountSubject;
    }

    public Observable<Account> getObservableAccountUpdates(String accountId) {
        return accountSubject.filter(acc -> acc.getAccountID().equals(accountId));
    }

    public Observable<Account> getObservableAccount(String accountId) {
        return Observable.fromCallable(() -> getAccount(accountId))
                .concatWith(getObservableAccountUpdates(accountId));
    }
    public Observable<Account> getObservableAccount(Account account) {
        return Observable.just(account)
                .concatWith(accountSubject.filter(acc -> acc == account));
    }

    public Observable<Account> getCurrentAccountSubject() {
        return currentAccountSubject;
    }

    public Observable<Account> getCurrentProfileAccountSubject() {
        return currentAccountSubject.flatMapSingle(a -> mVCardService.loadProfile(a).firstOrError().map(p -> a));
    }

    public void subscribeBuddy(final String accountID, final String uri, final boolean flag) {
        mExecutor.execute(() -> JamiService.subscribeBuddy(accountID, uri, flag));
    }

    /**
     * Send profile through SIP
     */
    public void sendProfile(final String callId, final String accountId) {
        mVCardService.loadSmallVCard(accountId, VCardService.MAX_SIZE_SIP)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.from(mExecutor))
                .subscribe(vcard -> {
                    String stringVCard = VCardUtils.vcardToString(vcard);
                    int nbTotal = stringVCard.length() / VCARD_CHUNK_SIZE + (stringVCard.length() % VCARD_CHUNK_SIZE != 0 ? 1 : 0);
                    int i = 1;
                    Random r = new Random(System.currentTimeMillis());
                    int key = Math.abs(r.nextInt());

                    Log.d(TAG, "sendProfile, vcard " + callId);

                    while (i <= nbTotal) {
                        HashMap<String, String> chunk = new HashMap<>();
                        Log.d(TAG, "length vcard " + stringVCard.length() + " id " + key + " part " + i + " nbTotal " + nbTotal);
                        String keyHashMap = VCardUtils.MIME_PROFILE_VCARD + "; id=" + key + ",part=" + i + ",of=" + nbTotal;
                        String message = stringVCard.substring(0, Math.min(VCARD_CHUNK_SIZE, stringVCard.length()));
                        chunk.put(keyHashMap, message);
                        JamiService.sendTextMessage(callId, StringMap.toSwig(chunk), "Me", false);
                        if (stringVCard.length() > VCARD_CHUNK_SIZE) {
                            stringVCard = stringVCard.substring(VCARD_CHUNK_SIZE);
                        }
                        i++;
                    }
                }, e -> Log.w(TAG, "Not sending empty profile", e));
    }

    public void setMessageDisplayed(String accountId, Uri conversationUri, String messageId) {
        mExecutor.execute(() -> JamiService.setMessageDisplayed(accountId, conversationUri.getUri(), messageId, 3));
    }

    public Single<Conversation> startConversation(String accountId, Collection<String> initialMembers) {
        Account account = getAccount(accountId);
        return Single.fromCallable(() -> {
            Log.w(TAG, "startConversation");
            String id = JamiService.startConversation(accountId);
            Conversation conversation = account.getSwarm(id);//new Conversation(accountId, new Uri(id));
            for (String member : initialMembers) {
                Log.w(TAG, "addConversationMember " + member);
                JamiService.addConversationMember(accountId, id, member);
                conversation.addContact(account.getContactFromCache(member));
            }
            account.conversationStarted(conversation);
            Log.w(TAG, "loadConversationMessages");
            //loadMore(conversation);
            //JamiService.loadConversationMessages(accountId, id, id, 2);
            return conversation;
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    public Completable removeConversation(String accountId, Uri conversationUri) {
        return Completable.fromAction(() -> JamiService.removeConversation(accountId, conversationUri.getRawRingId()))
                .subscribeOn(Schedulers.from(mExecutor));
    }

    public void loadConversationHistory(String accountId, Uri conversationUri, String root, long n) {
        JamiService.loadConversationMessages(accountId, conversationUri.getRawRingId(), root, n);
    }

    public Single<Conversation> loadMore(Conversation conversation) {
        return loadMore(conversation, 16);
    }
    public Single<Conversation> loadMore(Conversation conversation, int n) {
        synchronized (conversation) {
            if (conversation.isLoaded()) {
                Log.w(TAG, "loadMore: conversation already fully loaded");
                return Single.just(conversation);
            }
            if (conversation.getMode().blockingFirst() == Conversation.Mode.Syncing) {
                Log.w(TAG, "loadMore: conversation is syncing");
                return Single.just(conversation);
            }

            SingleSubject<Conversation> ret = conversation.getLoading();
            if (ret != null)
                return ret;
            ret = SingleSubject.create();
            Collection<String> roots = conversation.getSwarmRoot();
            Log.w(TAG, "loadMore " + conversation.getUri() + " " + roots);

            conversation.setLoading(ret);
            if (roots.isEmpty())
                loadConversationHistory(conversation.getAccountId(), conversation.getUri(), "", n);
            else {
                for (String root : roots)
                    loadConversationHistory(conversation.getAccountId(), conversation.getUri(), root, n);
            }
            return ret;
        }
    }

    public void sendConversationMessage(String accountId, Uri conversationUri, String txt) {
        mExecutor.execute(() -> {
            Log.w(TAG, "sendConversationMessages " + conversationUri.getRawRingId() + " : " + txt);
            JamiService.sendMessage(accountId, conversationUri.getRawRingId(), txt, "");
        });
    }

    /**
     * @return Account Ids list from Daemon
     */
    /*public Single<List<String>> getAccountList() {
        return Single.fromCallable(() -> (List<String>)new ArrayList<>(JamiService.getAccountList()))
                .subscribeOn(Schedulers.from(mExecutor));
    }*/

    /**
     * Sets the order of the accounts in the Daemon
     *
     * @param accountOrder The ordered list of account ids
     */
    public void setAccountOrder(final List<String> accountOrder) {
        mExecutor.execute(() -> {
            final StringBuilder order = new StringBuilder();
            for (String accountId : accountOrder) {
                order.append(accountId);
                order.append(File.separator);
            }
            JamiService.setAccountsOrder(order.toString());
        });
    }

    /**
     * @return the account details from the Daemon
     */
    public Map<String, String> getAccountDetails(final String accountId) {
        try {
            return mExecutor.submit(() -> JamiService.getAccountDetails(accountId).toNative()).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getAccountDetails()", e);
        }
        return null;
    }

    /**
     * Sets the account details in the Daemon
     */
    public void setAccountDetails(final String accountId, final Map<String, String> map) {
        Log.i(TAG, "setAccountDetails() " + accountId);
        mExecutor.execute(() -> JamiService.setAccountDetails(accountId, StringMap.toSwig(map)));
    }

    public Single<String> migrateAccount(String accountId, String password) {
        return mMigrationSubject
                .filter(r -> r.accountId.equals(accountId))
                .map(r -> r.state)
                .firstOrError()
                .doOnSubscribe(s -> {
                    final Account account = getAccount(accountId);
                    HashMap<String, String> details = account.getDetails();
                    details.put(ConfigKey.ARCHIVE_PASSWORD.key(), password);
                    mExecutor.execute(() -> JamiService.setAccountDetails(accountId, StringMap.toSwig(details)));
                })
                .subscribeOn(Schedulers.from(mExecutor));
    }

    public void setAccountEnabled(final String accountId, final boolean active) {
        mExecutor.execute(() -> JamiService.sendRegister(accountId, active));
    }

    /**
     * Sets the activation state of the account in the Daemon
     */
    public void setAccountActive(final String accountId, final boolean active) {
        mExecutor.execute(() -> JamiService.setAccountActive(accountId, active));
    }

    /**
     * Sets the activation state of all the accounts in the Daemon
     */
    public void setAccountsActive(final boolean active) {
        mExecutor.execute(() -> {
            Log.i(TAG, "setAccountsActive() running... " + active);
            synchronized (mAccountList) {
                for (Account a : mAccountList) {
                    // If the proxy is enabled we can considered the account
                    // as always active
                    if (a.isDhtProxyEnabled()) {
                        JamiService.setAccountActive(a.getAccountID(), true);
                    } else {
                        JamiService.setAccountActive(a.getAccountID(), active);
                    }
                }
            }
        });
    }

    /**
     * Sets the video activation state of all the accounts in the local cache
     */
    public void setAccountsVideoEnabled(boolean isEnabled) {
        synchronized (mAccountList) {
            for (Account account : mAccountList) {
                account.setDetail(ConfigKey.VIDEO_ENABLED, isEnabled);
            }
        }
    }

    /**
     * @return the account volatile details from the Daemon
     */
    public Map<String, String> getVolatileAccountDetails(final String accountId) {
        try {
            return mExecutor.submit(() -> JamiService.getVolatileAccountDetails(accountId).toNative()).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getVolatileAccountDetails()", e);
        }
        return null;
    }

    /**
     * @return the default template (account details) for a type of account
     */
    public Single<HashMap<String, String>> getAccountTemplate(final String accountType) {
        Log.i(TAG, "getAccountTemplate() " + accountType);
        return Single.fromCallable(() -> JamiService.getAccountTemplate(accountType).toNative())
                .subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * Removes the account in the Daemon as well as local history
     */
    public void removeAccount(final String accountId) {
        Log.i(TAG, "removeAccount() " + accountId);
        mExecutor.execute(() -> JamiService.removeAccount(accountId));
        mHistoryService.clearHistory(accountId).subscribe();
    }

    /**
     * Exports the account on the DHT (used for multi-devices feature)
     */
    public Single<String> exportOnRing(final String accountId, final String password) {
        return mExportSubject
                .filter(r -> r.accountId.equals(accountId))
                .firstOrError()
                .map(result -> {
                    switch (result.code) {
                        case PIN_GENERATION_SUCCESS:
                            return result.pin;
                        case PIN_GENERATION_WRONG_PASSWORD:
                            throw new IllegalArgumentException();
                        case PIN_GENERATION_NETWORK_ERROR:
                            throw new SocketException();
                        default:
                            throw new UnsupportedOperationException();
                    }
                })
                .doOnSubscribe(l -> {
                    Log.i(TAG, "exportOnRing() " + accountId);
                    mExecutor.execute(() -> JamiService.exportOnRing(accountId, password));
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * @return the list of the account's devices from the Daemon
     */
    public Map<String, String> getKnownRingDevices(final String accountId) {
        Log.i(TAG, "getKnownRingDevices() " + accountId);
        try {
            return mExecutor.submit(() -> JamiService.getKnownRingDevices(accountId).toNative()).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getKnownRingDevices()", e);
        }
        return null;
    }

    /**
     * @param accountId id of the account used with the device
     * @param deviceId  id of the device to revoke
     * @param password  password of the account
     */
    public Single<Integer> revokeDevice(final String accountId, final String password, final String deviceId) {
        return mDeviceRevocationSubject
                .filter(r -> r.accountId.equals(accountId) && r.deviceId.equals(deviceId))
                .firstOrError()
                .map(r -> r.code)
                .doOnSubscribe(l -> mExecutor.execute(() -> JamiService.revokeDevice(accountId, password, deviceId)))
                .subscribeOn(Schedulers.io());
    }

    /**
     * @param accountId id of the account used with the device
     * @param newName   new device name
     */
    public void renameDevice(final String accountId, final String newName) {
        final Account account = getAccount(accountId);
        mExecutor.execute(() -> {
            Log.i(TAG, "renameDevice() thread running... " + newName);
            StringMap details = JamiService.getAccountDetails(accountId);
            details.put(ConfigKey.ACCOUNT_DEVICE_NAME.key(), newName);
            JamiService.setAccountDetails(accountId, details);
            account.setDetail(ConfigKey.ACCOUNT_DEVICE_NAME, newName);
            account.setDevices(JamiService.getKnownRingDevices(accountId).toNative());
        });
    }

    public Completable exportToFile(String accountId, String absolutePath, String password) {
        return Completable.fromAction(() -> {
            if (!JamiService.exportToFile(accountId, absolutePath, password))
                throw new IllegalArgumentException("Can't export archive");
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * @param accountId   id of the account
     * @param oldPassword old account password
     */
    public Completable setAccountPassword(final String accountId, final String oldPassword, final String newPassword) {
        return Completable.fromAction(() -> {
            if (!JamiService.changeAccountPassword(accountId, oldPassword, newPassword))
                throw new IllegalArgumentException("Can't change password");
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * Sets the active codecs list of the account in the Daemon
     */
    public void setActiveCodecList(final String accountId, final List<Long> codecs) {
        mExecutor.execute(() -> {
            UintVect list = new UintVect();
            list.reserve(codecs.size());
            list.addAll(codecs);
            JamiService.setActiveCodecList(accountId, list);
            accountSubject.onNext(getAccount(accountId));
        });
    }

    /**
     * @return The account's codecs list from the Daemon
     */
    public Single<List<Codec>> getCodecList(final String accountId) {
        return Single.fromCallable(() -> {
            List<Codec> results = new ArrayList<>();
            UintVect payloads = JamiService.getCodecList();
            UintVect activePayloads = JamiService.getActiveCodecList(accountId);
            for (int i = 0; i < payloads.size(); ++i) {
                StringMap details = JamiService.getCodecDetails(accountId, payloads.get(i));
                if (details.size() > 1) {
                    results.add(new Codec(payloads.get(i), details.toNative(), activePayloads.contains(payloads.get(i))));
                } else {
                    Log.i(TAG, "Error loading codec " + i);
                }
            }
            return results;
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "validateCertificatePath() running...");
                return JamiService.validateCertificatePath(accountID, certificatePath, privateKeyPath, privateKeyPass, "").toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running validateCertificatePath()", e);
        }
        return null;
    }

    public Map<String, String> validateCertificate(final String accountId, final String certificate) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "validateCertificate() running...");
                return JamiService.validateCertificate(accountId, certificate).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running validateCertificate()", e);
        }
        return null;
    }

    public Map<String, String> getCertificateDetailsPath(final String certificatePath) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCertificateDetailsPath() running...");
                return JamiService.getCertificateDetails(certificatePath).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCertificateDetailsPath()", e);
        }
        return null;
    }

    public Map<String, String> getCertificateDetails(final String certificateRaw) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCertificateDetails() running...");
                return JamiService.getCertificateDetails(certificateRaw).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCertificateDetails()", e);
        }
        return null;
    }

    /**
     * @return the supported TLS methods from the Daemon
     */
    public List<String> getTlsSupportedMethods() {
        Log.i(TAG, "getTlsSupportedMethods()");
        return SwigNativeConverter.toJava(JamiService.getSupportedTlsMethod());
    }

    /**
     * @return the account's credentials from the Daemon
     */
    public List<Map<String, String>> getCredentials(final String accountId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCredentials() running...");
                return JamiService.getCredentials(accountId).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCredentials()", e);
        }
        return null;
    }

    /**
     * Sets the account's credentials in the Daemon
     */
    public void setCredentials(final String accountId, final List<Map<String, String>> credentials) {
        Log.i(TAG, "setCredentials() " + accountId);
        mExecutor.execute(() -> JamiService.setCredentials(accountId, SwigNativeConverter.toSwig(credentials)));
    }

    /**
     * Sets the registration state to true for all the accounts in the Daemon
     */
    public void registerAllAccounts() {
        Log.i(TAG, "registerAllAccounts()");
        mExecutor.execute(this::registerAllAccounts);
    }

    /**
     * Registers a new name on the blockchain for the account
     */
    public void registerName(final Account account, final String password, final String name) {

        if (account.registeringUsername) {
            Log.w(TAG, "Already trying to register username");
            return;
        }

        account.registeringUsername = true;
        registerName(account.getAccountID(), password == null ? "" : password, name);
    }

    /**
     * Register a new name on the blockchain for the account Id
     */
    public void registerName(final String account, final String password, final String name) {
        Log.i(TAG, "registerName()");
        mExecutor.execute(() -> JamiService.registerName(account, password, name));
    }

    /* contact requests */

    /**
     * @return all trust requests from the daemon for the account Id
     */
    public List<Map<String, String>> getTrustRequests(final String accountId) {
        try {
            return mExecutor.submit(() -> JamiService.getTrustRequests(accountId).toNative()).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getTrustRequests()", e);
        }
        return null;
    }

    /**
     * Accepts a pending trust request
     */
    public void acceptTrustRequest(final String accountId, final Uri from) {
        Log.i(TAG, "acceptRequest() " + accountId + " " + from);

        Account account = getAccount(accountId);
        if (account != null) {
            TrustRequest request = account.getRequest(from);
            account.removeRequest(from);

            if (request != null) {
                VCard vCard = request.getVCard();
                if (vCard != null) {
                    VCardUtils.savePeerProfileToDisk(vCard, accountId, from.getRawRingId() + ".vcf", mDeviceRuntimeService.provideFilesDir());
                }
                if (!StringUtils.isEmpty(request.getConversationId())
                        && !request.getUri().isSwarm()
                        && request.getUri().getRawRingId().equals(request.getConversationId())) {
                    Contact contact = account.getContactFromCache(request.getUri());
                    if (contact != null)  {
                        account.newSwarm(request.getConversationId(), Conversation.Mode.Syncing);
                        contact.setConversationUri(new Uri(Uri.SWARM_SCHEME, request.getConversationId()));
                    }
                }
            }
            //handleTrustRequest(accountId, from, null, ContactType.INVITATION_ACCEPTED);
        }
        mExecutor.execute(() -> JamiService.acceptTrustRequest(accountId, from.getRawRingId()));
    }

    /**
     * Handles adding contacts and is the initial point of conversation creation
     *
     * @param conversation    the user's account
     * @param contactUri the contacts raw string uri
     */
    private void handleTrustRequest(Conversation conversation, Uri contactUri, TrustRequest request, ContactType type) {
        ContactEvent event = new ContactEvent();
        switch (type) {
            case ADDED:
                break;
            case INVITATION_RECEIVED:
                event.setStatus(Interaction.InteractionStatus.UNKNOWN);
                event.setAuthor(contactUri.getRawRingId());
                event.setTimestamp(request.getTimestamp());
                break;
            case INVITATION_ACCEPTED:
                event.setStatus(Interaction.InteractionStatus.SUCCESS);
                event.setAuthor(contactUri.getRawRingId());
                break;
            case INVITATION_DISCARDED:
                mHistoryService.clearHistory(contactUri.getRawRingId(), conversation.getAccountId(), true).subscribe();
                return;
            default:
                return;
        }
        mHistoryService.insertInteraction(conversation.getAccountId(), conversation, event).subscribe();
    }

    private enum ContactType {
        ADDED, INVITATION_RECEIVED, INVITATION_ACCEPTED, INVITATION_DISCARDED
    }

    /**
     * Refuses and blocks a pending trust request
     */
    public boolean discardTrustRequest(final String accountId, final Uri contactUri) {
        Account account = getAccount(accountId);
        boolean removed = false;
        if (account != null) {
            removed = account.removeRequest(contactUri);
            mHistoryService.clearHistory(contactUri.getRawRingId(), accountId, true).subscribe();
        }
        mExecutor.execute(() -> JamiService.discardTrustRequest(accountId, contactUri.getRawRingId()));
        return removed;
    }

    /**
     * Sends a new trust request
     */
    public void sendTrustRequest(Conversation conversation, final Uri to, final Blob message) {
        Log.i(TAG, "sendTrustRequest() " + conversation.getAccountId() + " " + to);
        handleTrustRequest(conversation, to, null, ContactType.ADDED);
        mExecutor.execute(() -> JamiService.sendTrustRequest(conversation.getAccountId(), to.getRawRingId(), message == null ? new Blob() : message));
    }

    /**
     * Add a new contact for the account Id on the Daemon
     */
    public void addContact(final String accountId, final String uri) {
        Log.i(TAG, "addContact() " + accountId + " " + uri);
        //handleTrustRequest(accountId, Uri.fromString(uri), null, ContactType.ADDED);
        mExecutor.execute(() -> JamiService.addContact(accountId, uri));
    }

    /**
     * Remove an existing contact for the account Id on the Daemon
     */
    public void removeContact(final String accountId, final String uri, final boolean ban) {
        Log.i(TAG, "removeContact() " + accountId + " " + uri + " ban:" + ban);
        mExecutor.execute(() -> JamiService.removeContact(accountId, uri, ban));
    }

    /**
     * @return the contacts list from the daemon
     */
    public List<Map<String, String>> getContacts(final String accountId) {
        try {
            return mExecutor.submit(() -> JamiService.getContacts(accountId).toNative()).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getContacts()", e);
        }
        return null;
    }

    /**
     * Looks up for the availability of the name on the blockchain
     */
    public void lookupName(final String account, final String nameserver, final String name) {
        Log.i(TAG, "lookupName() " + account + " " + nameserver + " " + name);
        mExecutor.execute(() -> JamiService.lookupName(account, nameserver, name));
    }

    public Single<RegisteredName> findRegistrationByName(final String account, final String nameserver, final String name) {
        if (StringUtils.isEmpty(name)) {
            return Single.just(new RegisteredName());
        }
        return getRegisteredNames()
                .filter(r -> account.equals(r.accountId) && name.equals(r.name))
                .firstOrError()
                .doOnSubscribe(s -> mExecutor.execute(() -> JamiService.lookupName(account, nameserver, name)))
                .subscribeOn(Schedulers.from(mExecutor));
    }

    public Single<UserSearchResult> searchUser(final String account, final String query) {
        if (StringUtils.isEmpty(query)) {
            return Single.just(new UserSearchResult(account, query));
        }
        String encodedUrl;
        try {
            encodedUrl = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Single.error(e);
        }
        return getSearchResults()
                .filter(r -> account.equals(r.accountId) && encodedUrl.equals(r.query))
                .firstOrError()
                .doOnSubscribe(s -> mExecutor.execute(() -> JamiService.searchUser(account, encodedUrl)))
                .subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * Reverse looks up the address in the blockchain to find the name
     */
    public void lookupAddress(final String account, final String nameserver, final String address) {
        mExecutor.execute(() -> JamiService.lookupAddress(account, nameserver, address));
    }

    public void pushNotificationReceived(final String from, final Map<String, String> data) {
        // Log.i(TAG, "pushNotificationReceived()");
        mExecutor.execute(() -> JamiService.pushNotificationReceived(from, StringMap.toSwig(data)));
    }

    public void setPushNotificationToken(final String pushNotificationToken) {
        //Log.i(TAG, "setPushNotificationToken()");
        mExecutor.execute(() -> JamiService.setPushNotificationToken(pushNotificationToken));
    }

    void volumeChanged(String device, int value) {
        Log.w(TAG, "volumeChanged " + device + " " + value);
    }

    void accountsChanged() {
        // Accounts have changed in Daemon, we have to update our local cache
        refreshAccountsCacheFromDaemon();
    }

    void stunStatusFailure(String accountId) {
        Log.d(TAG, "stun status failure: " + accountId);
    }

    void registrationStateChanged(String accountId, String newState, int code, String detailString) {
        //Log.d(TAG, "registrationStateChanged: " + accountId + ", " + newState + ", " + code + ", " + detailString);

        Account account = getAccount(accountId);
        if (account == null) {
            return;
        }
        String oldState = account.getRegistrationState();
        if (oldState.contentEquals(AccountConfig.STATE_INITIALIZING) &&
                !newState.contentEquals(AccountConfig.STATE_INITIALIZING)) {
            account.setDetails(JamiService.getAccountDetails(account.getAccountID()).toNative());
            account.setCredentials(JamiService.getCredentials(account.getAccountID()).toNative());
            account.setDevices(JamiService.getKnownRingDevices(account.getAccountID()).toNative());
            account.setVolatileDetails(JamiService.getVolatileAccountDetails(account.getAccountID()).toNative());
        } else {
            account.setRegistrationState(newState, code);
        }

        if (!oldState.equals(newState)) {
            accountSubject.onNext(account);
        }
    }

    void accountDetailsChanged(String accountId, Map<String, String> details) {
        Account account = getAccount(accountId);
        if (account == null) {
            return;
        }
        Log.d(TAG, "accountDetailsChanged: " + accountId + " " + details.size());
        account.setDetails(details);
        accountSubject.onNext(account);
    }

    void volatileAccountDetailsChanged(String accountId, Map<String, String> details) {
        Account account = getAccount(accountId);
        if (account == null) {
            return;
        }
        //Log.d(TAG, "volatileAccountDetailsChanged: " + accountId + " " + details.size());
        account.setVolatileDetails(details);
        accountSubject.onNext(account);
    }

    public void accountProfileReceived(String accountId, String name, String photo) {
        Account account = getAccount(accountId);
        if (account == null)
            return;
        mVCardService.saveVCardProfile(accountId, account.getUri(), name, photo)
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> account.resetProfile(), e -> Log.e(TAG, "Error saving profile", e));
    }

    void profileReceived(String accountId, String peerId, String vcardPath) {
        Account account = getAccount(accountId);
        if (account == null)
            return;
        Log.w(TAG, "profileReceived: " + accountId + ", " + peerId + ", " + vcardPath);
        Contact contact = account.getContactFromCache(peerId);
        mVCardService.peerProfileReceived(accountId, peerId, new File(vcardPath))
                .subscribe(profile -> contact.setProfile(profile.first, profile.second), e -> Log.e(TAG, "Error saving contact profile", e));
    }

    void incomingAccountMessage(String accountId, String messageId, String callId, String from, Map<String, String> messages) {
        Log.d(TAG, "incomingAccountMessage: " + accountId + " " + messages.size());
        Message message = new Message();
        message.accountId = accountId;
        message.messageId = messageId;
        message.callId = callId;
        message.author = from;
        message.messages = messages;
        incomingMessageSubject.onNext(message);
    }

    void accountMessageStatusChanged(String accountId, String conversationId, String messageId, String peer, int status) {
        InteractionStatus newStatus = InteractionStatus.fromIntTextMessage(status);
        Log.d(TAG, "accountMessageStatusChanged: " + accountId + ", " + conversationId + ", " + messageId + ", " + peer + ", " + newStatus);
        if (StringUtils.isEmpty(conversationId)) {
            mHistoryService
                    .accountMessageStatusChanged(accountId, messageId, peer, newStatus)
                    .subscribe(messageSubject::onNext, e -> Log.e(TAG, "Error updating message: " + e.getLocalizedMessage()));
        } else {
            Interaction msg = new Interaction(accountId);
            msg.setStatus(newStatus);
            msg.setSwarmInfo(conversationId, messageId, null);
            messageSubject.onNext(msg);
        }
    }

    public void composingStatusChanged(String accountId, String conversationId, String contactUri, int status) {
        Log.d(TAG, "composingStatusChanged: " + accountId + ", " + contactUri + ", " + conversationId + ", " + status);
        getAccountSingle(accountId)
                .subscribe(account -> account.composingStatusChanged(conversationId, Uri.fromId(contactUri), Account.ComposingStatus.fromInt(status)));
    }

    void errorAlert(int alert) {
        Log.d(TAG, "errorAlert : " + alert);
    }

    void knownDevicesChanged(String accountId, Map<String, String> devices) {
        Account accountChanged = getAccount(accountId);
        if (accountChanged != null) {
            accountChanged.setDevices(devices);
            accountSubject.onNext(accountChanged);
        }
    }

    void exportOnRingEnded(String accountId, int code, String pin) {
        Log.d(TAG, "exportOnRingEnded: " + accountId + ", " + code + ", " + pin);
        ExportOnRingResult result = new ExportOnRingResult();
        result.accountId = accountId;
        result.code = code;
        result.pin = pin;
        mExportSubject.onNext(result);
    }

    void nameRegistrationEnded(String accountId, int state, String name) {
        Log.d(TAG, "nameRegistrationEnded: " + accountId + ", " + state + ", " + name);

        Account acc = getAccount(accountId);
        if (acc == null) {
            Log.w(TAG, "Can't find account for name registration callback");
            return;
        }

        acc.registeringUsername = false;
        acc.setVolatileDetails(JamiService.getVolatileAccountDetails(acc.getAccountID()).toNative());
        if (state == 0) {
            acc.setDetail(ConfigKey.ACCOUNT_REGISTERED_NAME, name);
        }

        accountSubject.onNext(acc);
    }

    void migrationEnded(String accountId, String state) {
        Log.d(TAG, "migrationEnded: " + accountId + ", " + state);
        MigrationResult result = new MigrationResult();
        result.accountId = accountId;
        result.state = state;
        mMigrationSubject.onNext(result);
    }

    void deviceRevocationEnded(String accountId, String device, int state) {
        Log.d(TAG, "deviceRevocationEnded: " + accountId + ", " + device + ", " + state);
        DeviceRevocationResult result = new DeviceRevocationResult();
        result.accountId = accountId;
        result.deviceId = device;
        result.code = state;
        if (state == 0) {
            Account account = getAccount(accountId);
            if (account != null) {
                Map<String, String> devices = account.getDevices();
                devices.remove(device);
                account.setDevices(devices);
                accountSubject.onNext(account);
            }
        }
        mDeviceRevocationSubject.onNext(result);
    }

    void incomingTrustRequest(String accountId, String conversationId, String from, String message, long received) {
        Log.d(TAG, "incomingTrustRequest: " + accountId + ", " + conversationId + ", " + from + ", " + received);

        Account account = getAccount(accountId);
        if (account != null) {
            Uri fromUri = Uri.fromString(from);
            TrustRequest request = account.getRequest(fromUri);
            if (request == null)
                request = new TrustRequest(accountId, fromUri, received * 1000L, message, conversationId);
            else
                request.setVCard(Ezvcard.parse(message).first());

            VCard vcard = request.getVCard();
            if (vcard != null) {
                Contact contact = account.getContactFromCache(fromUri);
                if (!contact.detailsLoaded) {
                    // VCardUtils.savePeerProfileToDisk(vcard, accountId, from + ".vcf", mDeviceRuntimeService.provideFilesDir());
                    mVCardService.loadVCardProfile(vcard)
                            .subscribeOn(Schedulers.computation())
                            .subscribe(profile -> contact.setProfile(profile.first, profile.second));
                }
            }
            account.addRequest(request);
            // handleTrustRequest(account, Uri.fromString(from), request, ContactType.INVITATION_RECEIVED);
            if (account.isEnabled())
                lookupAddress(accountId, "", from);
            incomingRequestsSubject.onNext(request);
        }
    }

    void contactAdded(String accountId, String uri, boolean confirmed) {
        Account account = getAccount(accountId);
        if (account != null) {
            account.addContact(uri, confirmed);
            if (account.isEnabled())
                lookupAddress(accountId, "", uri);
        }
    }

    void contactRemoved(String accountId, String uri, boolean banned) {
        Account account = getAccount(accountId);
        Log.d(TAG, "Contact removed: " + uri + " User is banned: " + banned);
        if (account != null) {
            mHistoryService.clearHistory(uri, accountId, true).subscribe();
            account.removeContact(uri, banned);
        }
    }

    void registeredNameFound(String accountId, int state, String address, String name) {
        try {
            //Log.d(TAG, "registeredNameFound: " + accountId + ", " + state + ", " + name + ", " + address);
            if (!StringUtils.isEmpty(address)) {
                Account account = getAccount(accountId);
                if (account != null) {
                    account.registeredNameFound(state, address, name);
                }
            }

            RegisteredName r = new RegisteredName();
            r.accountId = accountId;
            r.address = address;
            r.name = name;
            r.state = state;
            registeredNameSubject.onNext(r);
        } catch (Exception e) {
            Log.w(TAG, "registeredNameFound exception", e);
        }
    }

    public void userSearchEnded(String accountId, int state, String query, ArrayList<Map<String, String>> results) {
        Account account = getAccount(accountId);
        UserSearchResult r = new UserSearchResult(accountId, query);
        r.state = state;
        r.results = new ArrayList<>(results.size());
        for (Map<String, String> m : results) {
            String uri = m.get("id");
            String username = m.get("username");
            String firstName = m.get("firstName");
            String lastName = m.get("lastName");
            String picture_b64 = m.get("profilePicture");
            Contact contact = account.getContactFromCache(uri);
            if (contact != null) {
                contact.setUsername(username);
                contact.setProfile(firstName + " " + lastName, mVCardService.base64ToBitmap(picture_b64));
                r.results.add(contact);
            }
        }
        searchResultSubject.onNext(r);
    }

    private Interaction addMessage(Account account, Conversation conversation, Map<String, String> message)  {
        /* for (Map.Entry<String, String> e : message.entrySet()) {
            Log.w(TAG, e.getKey() + " -> " + e.getValue());
        } */
        String id = message.get("id");
        String type = message.get("type");
        String author = message.get("author");
        String parent = message.get("linearizedParent");
        List<String> parents = StringUtils.isEmpty(parent) ? Collections.emptyList() : Collections.singletonList(parent);
        Uri authorUri = Uri.fromId(author);

        long timestamp = Long.parseLong(message.get("timestamp")) * 1000;
        Contact contact = conversation.findContact(authorUri);
        if (contact == null) {
            contact = account.getContactFromCache(authorUri);
        }
        Interaction interaction;
        switch (type) {
            case "member":
                contact.setAddedDate(new Date(timestamp));
                interaction = new ContactEvent(contact);
                break;
            case "text/plain":
                interaction = new TextMessage(author, account.getAccountID(), timestamp, conversation, message.get("body"), !contact.isUser());
                break;
            case "application/data-transfer+json": {
                try {
                    String fileName = message.get("displayName");
                    String fileId = message.get("fileId");
                    //interaction = account.getDataTransfer(fileId);
                    //if (interaction == null) {
                        String[] paths = new String[1];
                        long[] progressA = new long[1];
                        long[] totalA = new long[1];
                        JamiService.fileTransferInfo(account.getAccountID(), conversation.getUri().getRawRingId(), fileId, paths, totalA, progressA);
                        if (totalA[0] == 0) {
                            totalA[0] = Long.parseLong(message.get("totalSize"));
                        }
                        File path = new File(paths[0]);
                        interaction = new DataTransfer(fileId, account.getAccountID(), author, fileName, contact.isUser(), timestamp, totalA[0], progressA[0]);
                        ((DataTransfer)interaction).setDaemonPath(path);
                        boolean isComplete = path.exists() && progressA[0] == totalA[0];
                        Log.w(TAG, "add DataTransfer at " + paths[0] + " with progress " + progressA[0] + "/" + totalA[0]);
                        interaction.setStatus(isComplete ? InteractionStatus.TRANSFER_FINISHED : InteractionStatus.FILE_AVAILABLE);
                    //}
                } catch (Exception e) {
                    interaction = new Interaction(conversation, Interaction.InteractionType.INVALID);
                }
                break;
            }
            case "application/call-history+json":
                interaction = new Call(null, account.getAccountID(), authorUri.getRawUriString(), contact.isUser() ? Call.Direction.OUTGOING : Call.Direction.INCOMING, timestamp);
                ((Call) interaction).setDuration(Long.parseLong(message.get("duration")));
                break;
            case "merge":
            default:
                interaction = new Interaction(conversation, Interaction.InteractionType.INVALID);
                break;
        }
        interaction.setContact(contact);
        interaction.setSwarmInfo(conversation.getUri().getRawRingId(), id, parents);
        interaction.setConversation(conversation);
        if (conversation.addSwarmElement(interaction)) {
            if (conversation.isVisible())
                mHistoryService.setMessageRead(account.getAccountID(), conversation.getUri(), interaction.getMessageId());
        }
        return interaction;
    }

    public void conversationLoaded(String accountId, String conversationId, List<Map<String, String>> messages) {
        try {
            // Log.w(TAG, "ConversationCallback: conversationLoaded " + accountId + "/" + conversationId + " " + messages.size());
            Account account = getAccount(accountId);
            if (account == null) {
                Log.w(TAG, "conversationLoaded: can't find account");
                return;
            }
            Conversation conversation = account.getSwarm(conversationId);
            synchronized (conversation) {
                for (Map<String, String> message : messages) {
                    addMessage(account, conversation, message);
                }
                conversation.stopLoading();
            }
            account.conversationChanged();
        } catch (Exception e) {
            Log.e(TAG, "Exception loading message", e);
        }
    }

    private enum ConversationMemberEvent {
        Add, Join, Remove, Ban
    }

    public void conversationMemberEvent(String accountId, String conversationId, String peerUri, int event) {
        Log.w(TAG, "ConversationCallback: conversationMemberEvent " + accountId + "/" + conversationId);
        Account account = getAccount(accountId);
        if (account == null) {
            Log.w(TAG, "conversationMemberEvent: can't find account");
            return;
        }
        Conversation conversation = account.getSwarm(conversationId);
        Uri uri = Uri.fromId(peerUri);
        switch (ConversationMemberEvent.values()[event])  {
            case Add:
            case Join: {
                Contact contact = conversation.findContact(uri);
                if (contact == null) {
                    conversation.addContact(account.getContactFromCache(uri));
                }
                break;
            }
            case Remove:
            case Ban: {
                Contact contact = conversation.findContact(uri);
                if (contact != null) {
                    conversation.removeContact(contact);
                }
                break;
            }
        }
    }

    public void conversationReady(String accountId, String conversationId) {
        Log.w(TAG, "ConversationCallback: conversationReady " + accountId + "/" + conversationId);
        Account account = getAccount(accountId);
        if (account == null) {
            Log.w(TAG, "conversationReady: can't find account");
            return;
        }
        StringMap info = JamiService.conversationInfos(accountId, conversationId);
        /*for (Map.Entry<String, String> i : info.entrySet()) {
            Log.w(TAG, "conversation info: " + i.getKey() + " " + i.getValue());
        }*/
        int modeInt = Integer.parseInt(info.get("mode"));
        Conversation.Mode mode = Conversation.Mode.values()[modeInt];
        Conversation conversation = account.newSwarm(conversationId, mode);

        for (Map<String, String> member : JamiService.getConversationMembers(accountId, conversationId)) {
            Uri uri = Uri.fromId(member.get("uri"));
            Contact contact = conversation.findContact(uri);
            if (contact == null) {
                contact = account.getContactFromCache(uri);
                conversation.addContact(contact);
            }
        }
        //if (conversation.getLastElementLoaded() == null)
        //    conversation.setLastElementLoaded(Completable.defer(() -> loadMore(conversation, 2).ignoreElement()).cache());
        account.conversationStarted(conversation);
        loadMore(conversation, 2);
    }

    public void conversationRemoved(String accountId, String conversationId) {
        Account account = getAccount(accountId);
        if (account == null) {
            Log.w(TAG, "conversationRemoved: can't find account");
            return;
        }
        account.removeSwarm(conversationId);
    }

    public void conversationRequestReceived(String accountId, String conversationId, Map<String, String> metadata) {
        Log.w(TAG, "ConversationCallback: conversationRequestReceived " + accountId + "/" + conversationId + " " + metadata.size());
        Account account = getAccount(accountId);
        if (account == null) {
            Log.w(TAG, "conversationRequestReceived: can't find account");
            return;
        }
        Uri contactUri = Uri.fromId(metadata.get("from"));
        account.addRequest(new TrustRequest(account.getAccountID(), contactUri, conversationId));
    }

    public void messageReceived(String accountId, String conversationId, Map<String, String> message) {
        Log.w(TAG, "ConversationCallback: messageReceived " + accountId + "/" + conversationId + " " + message.size());
        Account account = getAccount(accountId);
        Conversation conversation = account.getSwarm(conversationId);
        synchronized (conversation) {
            Interaction interaction = addMessage(account, conversation, message);
            account.conversationUpdated(conversation);
            boolean isIncoming = !interaction.getContact().isUser();
            if (isIncoming) {
                incomingSwarmMessageSubject.onNext(interaction);
                if (interaction instanceof DataTransfer)
                    dataTransferSubject.onNext((DataTransfer)interaction);
            }
        }
    }

    public Single<DataTransfer> sendFile(final File file, final DataTransfer dataTransfer) {
        return Single.fromCallable(() -> {
            mStartingTransfer = dataTransfer;

            DataTransferInfo dataTransferInfo = new DataTransferInfo();
            dataTransferInfo.setAccountId(dataTransfer.getAccount());

            String conversationId = dataTransfer.getConversationId();
            if (!StringUtils.isEmpty(conversationId))
                dataTransferInfo.setConversationId(conversationId);
            else
                dataTransferInfo.setPeer(dataTransfer.getConversation().getParticipant());

            dataTransferInfo.setPath(file.getAbsolutePath());
            dataTransferInfo.setDisplayName(dataTransfer.getDisplayName());

            Log.i(TAG, "sendFile() id=" + dataTransfer.getId() + " accountId=" + dataTransferInfo.getAccountId() + ", peer=" + dataTransferInfo.getPeer() + ", filePath=" + dataTransferInfo.getPath());
            long[] id = new long[1];
            DataTransferError err = getDataTransferError(JamiService.sendFileLegacy(dataTransferInfo, id));
            if (err != DataTransferError.SUCCESS) {
                throw new IOException(err.name());
            } else {
                Log.e(TAG, "sendFile: got ID " + id[0]);
                dataTransfer.setDaemonId(id[0]);
            }
            return dataTransfer;
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    public void sendFile(Conversation conversation, final File file) {
        mExecutor.execute(() -> JamiService.sendFile(conversation.getAccountId(), conversation.getUri().getRawRingId(), file.getAbsolutePath(), file.getName(), ""));
    }

    public List<net.jami.daemon.Message> getLastMessages(String accountId, long baseTime) {
        try {
            return mExecutor.submit(() -> SwigNativeConverter.toJava(JamiService.getLastMessages(accountId, baseTime))).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void acceptFileTransfer(final String accountId, final Uri conversationUri, String messageId, String fileId) {
        Account account = getAccount(accountId);
        if (account != null) {
            Conversation conversation = account.getByUri(conversationUri);
            acceptFileTransfer(conversation, fileId, conversation.isSwarm() ? (DataTransfer)conversation.getMessage(messageId) : account.getDataTransfer(fileId));
        }
    }

    public void acceptFileTransfer(Conversation conversation, String fileId, DataTransfer transfer) {
        if (conversation.isSwarm()) {
            String conversationId = conversation.getUri().getRawRingId();
            File newPath = mDeviceRuntimeService.getNewConversationPath(conversation.getAccountId(), conversationId, transfer.getDisplayName());
            Log.i(TAG, "downloadFile() id=" + conversation.getAccountId() + ", path=" + conversationId + " " + fileId + " to -> " + newPath.getAbsolutePath());
            JamiService.downloadFile(conversation.getAccountId(), conversationId, transfer.getMessageId(), fileId, newPath.getAbsolutePath());
        } else {
            if (transfer == null) {
                return;
            }
            File path = mDeviceRuntimeService.getTemporaryPath(conversation.getUri().getRawRingId(), transfer.getStoragePath());
            Log.i(TAG, "acceptFileTransfer() id=" + fileId + ", path=" + path.getAbsolutePath());
            JamiService.acceptFileTransfer(conversation.getAccountId(), fileId, path.getAbsolutePath());
        }
    }

    public void cancelDataTransfer(final String accountId, final String conversationId, final String messageId, final String fileId) {
        Log.i(TAG, "cancelDataTransfer() id=" + fileId);
        mExecutor.execute(() -> JamiService.cancelDataTransfer(accountId, conversationId, fileId));
    }

    private class DataTransferRefreshTask implements Runnable {
        private final Account mAccount;
        private final Conversation mConversation;
        private final DataTransfer mToUpdate;
        public ScheduledFuture<?> scheduledTask;

        DataTransferRefreshTask(Account account, Conversation conversation, DataTransfer t) {
            mAccount = account;
            mConversation = conversation;
            mToUpdate = t;
        }

        @Override
        public void run() {
            synchronized (mToUpdate) {
                if (mToUpdate.getStatus() == Interaction.InteractionStatus.TRANSFER_ONGOING) {
                    dataTransferEvent(mAccount, mConversation, mToUpdate.getMessageId(), mToUpdate.getFileId(), 5);
                } else {
                    scheduledTask.cancel(false);
                    scheduledTask = null;
                }
            }
        }
    }

    void dataTransferEvent(String accountId, String conversationId, String interactionId, final String fileId, int eventCode) {
        Account account = getAccount(accountId);
        if (account != null) {
            Conversation conversation = StringUtils.isEmpty(conversationId) ? null : account.getSwarm(conversationId);
            dataTransferEvent(account, conversation, interactionId, fileId, eventCode);
        }
    }
    void dataTransferEvent(Account account, Conversation conversation, final String interactionId, final String fileId, int eventCode) {
        Interaction.InteractionStatus transferStatus = getDataTransferEventCode(eventCode);
        Log.d(TAG, "Data Transfer " + interactionId + " " + fileId + " " + transferStatus);

        String from;
        long total, progress;
        String displayName;
        DataTransfer transfer = account.getDataTransfer(fileId);
        boolean outgoing = false;
        if (conversation == null) {
            DataTransferInfo info = new DataTransferInfo();
            DataTransferError err = getDataTransferError(JamiService.dataTransferInfo(account.getAccountID(), fileId, info));
            if (err != DataTransferError.SUCCESS) {
                Log.d(TAG, "Data Transfer error getting details " + err);
                return;
            }
            from = info.getPeer();
            total = info.getTotalSize();
            progress = info.getBytesProgress();
            conversation = account.getByUri(from);
            outgoing = info.getFlags() == 0;
            displayName = info.getDisplayName();
        } else {
            String[] paths = new String[1];
            long[] progressA = new long[1];
            long[] totalA = new long[1];
            JamiService.fileTransferInfo(account.getAccountID(), conversation.getUri().getRawRingId(), fileId, paths, totalA, progressA);
            progress = progressA[0];
            total = totalA[0];
            if (transfer == null && !StringUtils.isEmpty(interactionId)) {
                transfer = (DataTransfer) conversation.getMessage(interactionId);
            }
            if (transfer == null)
                return;
            transfer.setConversation(conversation);
            transfer.setDaemonPath(new File(paths[0]));
            from = transfer.getAuthor();
            displayName = transfer.getDisplayName();
        }

        if (transfer == null) {
            if (outgoing && mStartingTransfer != null) {
                Log.d(TAG, "Data Transfer mStartingTransfer");
                transfer = mStartingTransfer;
                mStartingTransfer = null;
            } else {
                transfer = new DataTransfer(conversation, from, account.getAccountID(), displayName,
                        outgoing, total,
                        progress, fileId);
                if (conversation.isSwarm()) {
                    transfer.setSwarmInfo(conversation.getUri().getRawRingId(), interactionId, null);
                } else {
                    mHistoryService.insertInteraction(account.getAccountID(), conversation, transfer).blockingAwait();
                }
            }
            account.putDataTransfer(fileId, transfer);
        } else synchronized (transfer) {
            InteractionStatus oldState = transfer.getStatus();
            if (oldState != transferStatus) {
                if (transferStatus == Interaction.InteractionStatus.TRANSFER_ONGOING) {
                    DataTransferRefreshTask task = new DataTransferRefreshTask(account, conversation, transfer);
                    task.scheduledTask = mExecutor.scheduleAtFixedRate(task,
                            DATA_TRANSFER_REFRESH_PERIOD,
                            DATA_TRANSFER_REFRESH_PERIOD, TimeUnit.MILLISECONDS);
                } else if (transferStatus.isError()) {
                    if (!transfer.isOutgoing()) {
                        File tmpPath = mDeviceRuntimeService.getTemporaryPath(conversation.getUri().getRawRingId(), transfer.getStoragePath());
                        tmpPath.delete();
                    }
                } else if (transferStatus == (Interaction.InteractionStatus.TRANSFER_FINISHED)) {
                    if (!conversation.isSwarm() && !transfer.isOutgoing()) {
                        File tmpPath = mDeviceRuntimeService.getTemporaryPath(conversation.getUri().getRawRingId(), transfer.getStoragePath());
                        File path = mDeviceRuntimeService.getConversationPath(conversation.getUri().getRawRingId(), transfer.getStoragePath());
                        FileUtils.moveFile(tmpPath, path);
                    }
                }
            }
            transfer.setStatus(transferStatus);
            transfer.setBytesProgress(progress);
            if (!conversation.isSwarm()) {
                mHistoryService.updateInteraction(transfer, account.getAccountID()).subscribe();
            }
        }

        Log.d(TAG, "Data Transfer dataTransferSubject.onNext");
        dataTransferSubject.onNext(transfer);
    }

    private static Interaction.InteractionStatus getDataTransferEventCode(int eventCode) {
        Interaction.InteractionStatus dataTransferEventCode = Interaction.InteractionStatus.INVALID;
        try {
            dataTransferEventCode = InteractionStatus.fromIntFile(eventCode);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Log.e(TAG, "getEventCode: invalid data transfer status from daemon");
        }
        return dataTransferEventCode;
    }

    private static DataTransferError getDataTransferError(Long errorCode) {
        if (errorCode == null) {
            Log.e(TAG, "getDataTransferError: invalid error code");
        } else {
            try {
                return DataTransferError.values()[errorCode.intValue()];
            } catch (ArrayIndexOutOfBoundsException ignored) {
                Log.e(TAG, "getDataTransferError: invalid data transfer error from daemon");
            }
        }
        return DataTransferError.UNKNOWN;
    }

    public Subject<DataTransfer> getDataTransfers() {
        return dataTransferSubject;
    }

    public Observable<DataTransfer> observeDataTransfer(DataTransfer transfer) {
        return dataTransferSubject
                .filter(t -> t == transfer)
                .startWithItem(transfer);
    }

    public void setProxyEnabled(boolean enabled) {
        mExecutor.execute(() -> {
            synchronized (mAccountList) {
                for (Account acc : mAccountList) {
                    if (acc.isJami() && (acc.isDhtProxyEnabled() != enabled)) {
                        Log.d(TAG, (enabled ? "Enabling" : "Disabling") + " proxy for account " + acc.getAccountID());
                        acc.setDhtProxyEnabled(enabled);
                        StringMap details = JamiService.getAccountDetails(acc.getAccountID());
                        details.put(ConfigKey.PROXY_ENABLED.key(), enabled ? "true" : "false");
                        JamiService.setAccountDetails(acc.getAccountID(), details);
                    }
                }
            }
        });
    }
}
