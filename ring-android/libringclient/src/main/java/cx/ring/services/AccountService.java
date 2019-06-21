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
package cx.ring.services;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.daemon.DataTransferInfo;
import cx.ring.daemon.Message;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.CallContact;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.model.DataTransfer;
import cx.ring.model.DataTransferError;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.TextMessage;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Log;
import cx.ring.utils.SwigNativeConverter;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * This service handles the accounts (Ring and SIP)
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

    @Inject
    @Named("DaemonExecutor")
    ScheduledExecutorService mExecutor;

    @Inject
    HistoryService mHistoryService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    VCardService mVCardService;

    private Account mCurrentAccount;
    private List<Account> mAccountList = new ArrayList<>();
    private boolean mHasSipAccount;
    private boolean mHasRingAccount;
    private AtomicBoolean mAccountsLoaded = new AtomicBoolean(false);

    private final HashMap<Long, DataTransfer> mDataTransfers = new HashMap<>();
    private DataTransfer mStartingTransfer = null;
    private Timer mTransferRefreshTimer = null;

    private final BehaviorSubject<List<Account>> accountsSubject = BehaviorSubject.create();
    private final Subject<Account> accountSubject = PublishSubject.create();
    private final Observable<Account> currentAccountSubject = accountsSubject
            .filter(l -> !l.isEmpty())
            .map(l -> l.get(0))
            .distinctUntilChanged();

    private final Subject<TextMessage> incomingMessageSubject = PublishSubject.create();
    private final Subject<TextMessage> messageSubject = PublishSubject.create();
    private final Subject<DataTransfer> dataTransferSubject = PublishSubject.create();
    private final Subject<TrustRequest> incomingRequestsSubject = PublishSubject.create();

    public void refreshAccounts() {
        accountsSubject.onNext(mAccountList);
    }

    public class RegisteredName {
        public String accountId;
        public String name;
        public String address;
        public int state;
    }

    private final Subject<RegisteredName> registeredNameSubject = PublishSubject.create();

    private class ExportOnRingResult {
        String accountId;
        int code;
        String pin;
    }

    private class DeviceRevocationResult {
        String accountId;
        String deviceId;
        int code;
    }

    private class MigrationResult {
        String accountId;
        String state;
    }

    private final Subject<ExportOnRingResult> mExportSubject = PublishSubject.create();
    private final Subject<DeviceRevocationResult> mDeviceRevocationSubject = PublishSubject.create();
    private final Subject<MigrationResult> mMigrationSubject = PublishSubject.create();

    public Observable<RegisteredName> getRegisteredNames() {
        return registeredNameSubject;
    }

    public Observable<TextMessage> getIncomingMessages() {
        return incomingMessageSubject;
    }

    public Observable<TextMessage> getMessageStateChanges() {
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

    public boolean isLoaded() {
        return mAccountsLoaded.get();
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

    private void refreshAccountsCacheFromDaemon() {
        Log.w(TAG, "refreshAccountsCacheFromDaemon");
        mAccountsLoaded.set(false);
        boolean hasSip = false, hasJami = false;
        List<Account> curList = mAccountList;
        List<String> accountIds = new ArrayList<>(Ringservice.getAccountList());
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

        mAccountList = newAccounts;

        mHistoryService.migrateDatabase(accountIds);
        mVCardService.migrateProfiles(accountIds);

        for (String accountId : accountIds) {
            Account account = getAccount(accountId);
            Map<String, String> details = Ringservice.getAccountDetails(accountId).toNative();
            List<Map<String, String>> credentials = Ringservice.getCredentials(accountId).toNative();
            Map<String, String> volatileAccountDetails = Ringservice.getVolatileAccountDetails(accountId).toNative();
            if (account == null) {
                account = new Account(accountId, details, credentials, volatileAccountDetails);
                newAccounts.add(account);
            } else {
                account.setDetails(details);
                account.setCredentials(credentials);
                account.setVolatileDetails(volatileAccountDetails);
            }


            if (account.isSip()) {
                hasSip = true;
            } else if (account.isRing()) {
                hasJami = true;
                boolean enabled = account.isEnabled();

                account.setDevices(Ringservice.getKnownRingDevices(accountId).toNative());


                account.setContacts(Ringservice.getContacts(accountId).toNative());
                List<Map<String, String>> requests = Ringservice.getTrustRequests(accountId).toNative();
                for (Map<String, String> requestInfo : requests) {
                    TrustRequest request = new TrustRequest(accountId, requestInfo);
                    account.addRequest(request);
                    CallContact contact = account.getContactFromCache(request.getContactId());
                    if (!contact.detailsLoaded) {
                        final VCard vcard = request.getVCard();
                        contact.setVCard(vcard);
                        mVCardService.loadVCardProfile(vcard)
                                .subscribeOn(Schedulers.computation())
                                .subscribe(profile -> contact.setProfile(profile.first, profile.second));
                    }
                    // If name is in cache this can be synchronous
                    if (enabled)
                        Ringservice.lookupAddress(accountId, "", request.getContactId());
                }
                if (enabled) {
                    for (CallContact contact : account.getContacts().values()) {
                        if (!contact.isUsernameLoaded())
                            Ringservice.lookupAddress(accountId, "", contact.getPrimaryUri().getRawRingId());
                    }
                }
            }

            mVCardService.migrateContact(account.getContacts(), account.getAccountID());
        }
        mVCardService.deleteLegacyProfiles();
        mHasSipAccount = hasSip;
        mHasRingAccount = hasJami;
        if (!newAccounts.isEmpty()) {
            Account newAccount = newAccounts.get(0);
            if (mCurrentAccount != newAccount) {
                mCurrentAccount = newAccount;
            }
        }
        accountsSubject.onNext(newAccounts);
        mAccountsLoaded.set(true);
    }

    private Account getAccountByName(final String name) {
        for (Account acc : mAccountList) {
            if (acc.getAlias().equals(name))
                return acc;
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
            String accountId = Ringservice.addAccount(StringMap.toSwig(map));
            if (accountId == null) {
                throw new RuntimeException("Can't create account.");
            }

            Map<String, String> accountDetails = Ringservice.getAccountDetails(accountId).toNative();
            List<Map<String, String>> accountCredentials = Ringservice.getCredentials(accountId).toNative();
            Map<String, String> accountVolatileDetails = Ringservice.getVolatileAccountDetails(accountId).toNative();
            Map<String, String> accountDevices = Ringservice.getKnownRingDevices(accountId).toNative();

            Account account = getAccount(accountId);
            if (account == null) {
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
                        .startWith(account))
                .subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * @return the current Account from the local cache
     */
    public Account getCurrentAccount() {
        return mCurrentAccount;
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
        final List<Account> accounts = getAccounts();
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
        for (Account account : mAccountList) {
            String accountID = account.getAccountID();
            if (accountID.equals(accountId)) {
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
                        String accountID = account.getAccountID();
                        if (accountID.equals(accountId)) {
                            return account;
                        }
                    }
                    Log.d(TAG, "getAccountSingle() can't find account " + accountId);
                    throw new IllegalArgumentException();
                });
    }

    /**
     * @return Accounts list from the local cache
     */
    public List<Account> getAccounts() {
        return mAccountList;
    }

    public Observable<List<Account>> getObservableAccountList() {
        return accountsSubject;
    }

    private Single<List<Account>> loadAccountProfiles(List<Account> accounts) {
        if (accounts.isEmpty())
            return Single.just(accounts);
        List<Single<Account>> loadedAccounts = new ArrayList<>(accounts.size());
        for (Account account : accounts)
            loadedAccounts.add(loadAccountProfile(account));
        return Single.concatEager(loadedAccounts).toList(accounts.size());
    }

    public Observable<List<Account>> getProfileAccountList() {
        return accountsSubject.concatMapSingle(this::loadAccountProfiles);
    }

    private Single<Account> loadAccountProfile(Account account) {
        if (account.getProfile() == null)
            return VCardUtils.loadLocalProfileFromDisk(mDeviceRuntimeService.provideFilesDir(), account.getAccountID())
                    .subscribeOn(Schedulers.io())
                    .map(vCard -> {
                        account.setProfile(vCard);
                        return account;
                    })
                    .onErrorReturn(e -> account);
        else
            return Single.just(account);
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

    public Observable<Account> getCurrentAccountSubject() {
        return currentAccountSubject;
    }

    public void subscribeBuddy(final String accountID, final String uri, final boolean flag) {
        mExecutor.execute(() -> Ringservice.subscribeBuddy(accountID, uri, flag));
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

                    Log.d(TAG, "sendProfile, vcard " + stringVCard);

                    while (i <= nbTotal) {
                        HashMap<String, String> chunk = new HashMap<>();
                        Log.d(TAG, "length vcard " + stringVCard.length() + " id " + key + " part " + i + " nbTotal " + nbTotal);
                        String keyHashMap = VCardUtils.MIME_RING_PROFILE_VCARD + "; id=" + key + ",part=" + i + ",of=" + nbTotal;
                        String message = stringVCard.substring(0, Math.min(VCARD_CHUNK_SIZE, stringVCard.length()));
                        chunk.put(keyHashMap, message);
                        Ringservice.sendTextMessage(callId, StringMap.toSwig(chunk), "Me", false);
                        if (stringVCard.length() > VCARD_CHUNK_SIZE) {
                            stringVCard = stringVCard.substring(VCARD_CHUNK_SIZE);
                        }
                        i++;
                    }
                });
    }

    /**
     * @return Account Ids list from Daemon
     */
    public List<String> getAccountList() {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getAccountList() running...");
                return new ArrayList<>(Ringservice.getAccountList());
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getAccountList()", e);
        }
        return new ArrayList<>();
    }

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
            Ringservice.setAccountsOrder(order.toString());
        });
    }

    /**
     * @return the account details from the Daemon
     */
    public Map<String, String> getAccountDetails(final String accountId) {
        try {
            return mExecutor.submit(() -> Ringservice.getAccountDetails(accountId).toNative()).get();
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
        mExecutor.execute(() -> Ringservice.setAccountDetails(accountId, StringMap.toSwig(map)));
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
                    mExecutor.execute(() -> Ringservice.setAccountDetails(accountId, StringMap.toSwig(details)));
                })
                .subscribeOn(Schedulers.from(mExecutor));
    }

    public void setAccountEnabled(final String accountId, final boolean active) {
        mExecutor.execute(() -> Ringservice.sendRegister(accountId, active));
    }

    /**
     * Sets the activation state of the account in the Daemon
     */
    public void setAccountActive(final String accountId, final boolean active) {
        mExecutor.execute(() -> Ringservice.setAccountActive(accountId, active));
    }

    /**
     * Sets the activation state of all the accounts in the Daemon
     */
    public void setAccountsActive(final boolean active) {
        mExecutor.execute(() -> {
            Log.i(TAG, "setAccountsActive() running... " + active);
            StringVect list = Ringservice.getAccountList();
            for (int i = 0, n = list.size(); i < n; i++) {
                String accountId = list.get(i);
                Account a = getAccount(accountId);
                // If the proxy is enabled we can considered the account
                // as always active
                if (a.isDhtProxyEnabled()) {
                    Ringservice.setAccountActive(accountId, true);
                } else {
                    Ringservice.setAccountActive(accountId, active);
                }
            }
        });
    }

    /**
     * Sets the video activation state of all the accounts in the local cache
     */
    public void setAccountsVideoEnabled(boolean isEnabled) {
        for (Account account : mAccountList) {
            account.setDetail(ConfigKey.VIDEO_ENABLED, isEnabled);
        }
    }

    /**
     * @return the account volatile details from the Daemon
     */
    public Map<String, String> getVolatileAccountDetails(final String accountId) {
        try {
            return mExecutor.submit(() -> Ringservice.getVolatileAccountDetails(accountId).toNative()).get();
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
        return Single.fromCallable(() -> Ringservice.getAccountTemplate(accountType).toNative())
                .subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * Removes the account in the Daemon as well as local history
     */
    public void removeAccount(final String accountId) {
        Log.i(TAG, "removeAccount() " + accountId);
        mExecutor.execute(() -> Ringservice.removeAccount(accountId));
        mHistoryService.deleteAccountHistory(accountId);
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
                    mExecutor.execute(() -> Ringservice.exportOnRing(accountId, password));
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * @return the list of the account's devices from the Daemon
     */
    public Map<String, String> getKnownRingDevices(final String accountId) {
        Log.i(TAG, "getKnownRingDevices() " + accountId);
        try {
            return mExecutor.submit(() -> Ringservice.getKnownRingDevices(accountId).toNative()).get();
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
                .doOnSubscribe(l -> mExecutor.execute(() -> Ringservice.revokeDevice(accountId, password, deviceId)))
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
            StringMap details = Ringservice.getAccountDetails(accountId);
            details.set(ConfigKey.ACCOUNT_DEVICE_NAME.key(), newName);
            Ringservice.setAccountDetails(accountId, details);
            account.setDetail(ConfigKey.ACCOUNT_DEVICE_NAME, newName);
            account.setDevices(Ringservice.getKnownRingDevices(accountId).toNative());
        });
    }

    public Completable exportToFile(String accountId, String absolutePath, String password) {
        return Completable.fromAction(() -> {
            if (!Ringservice.exportToFile(accountId, absolutePath, password))
                throw new IllegalArgumentException("Can't export archive");
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * @param accountId   id of the account
     * @param oldPassword old account password
     */
    public Completable setAccountPassword(final String accountId, final String oldPassword, final String newPassword) {
        return Completable.fromAction(() -> {
            if (!Ringservice.changeAccountPassword(accountId, oldPassword, newPassword))
                throw new IllegalArgumentException("Can't change password");
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * Sets the active codecs list of the account in the Daemon
     */
    public void setActiveCodecList(final String accountId, final List<Long> codecs) {
        mExecutor.execute(() -> {
            UintVect list = new UintVect(codecs.size());
            for (Long codec : codecs) {
                list.add(codec);
            }
            Ringservice.setActiveCodecList(accountId, list);
            accountSubject.onNext(getAccount(accountId));
        });
    }

    /**
     * @return The account's codecs list from the Daemon
     */
    public Single<List<Codec>> getCodecList(final String accountId) {
        return Single.fromCallable(() -> {
            List<Codec> results = new ArrayList<>();

            UintVect activePayloads = Ringservice.getActiveCodecList(accountId);
            for (int i = 0; i < activePayloads.size(); ++i) {
                Log.i(TAG, "getCodecDetails(" + accountId + ", " + activePayloads.get(i) + ")");
                StringMap codecsDetails = Ringservice.getCodecDetails(accountId, activePayloads.get(i));
                results.add(new Codec(activePayloads.get(i), codecsDetails.toNative(), true));
            }
            UintVect payloads = Ringservice.getCodecList();

            cl:
            for (int i = 0; i < payloads.size(); ++i) {
                for (Codec co : results) {
                    if (co.getPayload() == payloads.get(i)) {
                        continue cl;
                    }
                }
                StringMap details = Ringservice.getCodecDetails(accountId, payloads.get(i));
                if (details.size() > 1) {
                    results.add(new Codec(payloads.get(i), details.toNative(), false));
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
                return Ringservice.validateCertificatePath(accountID, certificatePath, privateKeyPath, privateKeyPass, "").toNative();
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
                return Ringservice.validateCertificate(accountId, certificate).toNative();
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
                return Ringservice.getCertificateDetails(certificatePath).toNative();
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
                return Ringservice.getCertificateDetails(certificateRaw).toNative();
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
        return SwigNativeConverter.toJava(Ringservice.getSupportedTlsMethod());
    }

    /**
     * @return the account's credentials from the Daemon
     */
    public List<Map<String, String>> getCredentials(final String accountId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCredentials() running...");
                return Ringservice.getCredentials(accountId).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCredentials()", e);
        }
        return null;
    }

    /**
     * Sets the account's credentials in the Daemon
     */
    public void setCredentials(final String accountId, final List credentials) {
        Log.i(TAG, "setCredentials() " + accountId);
        mExecutor.execute(() -> Ringservice.setCredentials(accountId, SwigNativeConverter.toSwig(credentials)));
    }

    /**
     * Sets the registration state to true for all the accounts in the Daemon
     */
    public void registerAllAccounts() {
        Log.i(TAG, "registerAllAccounts()");
        mExecutor.execute(this::registerAllAccounts);
    }

    /**
     * Backs  up all the accounts into to an archive in the path
     */
    public int backupAccounts(final List<String> accountIds, final String toDir, final String password) {
        try {
            return mExecutor.submit(() -> {
                StringVect ids = new StringVect();
                ids.addAll(accountIds);
                return Ringservice.exportAccounts(ids, toDir, password);
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running backupAccounts()", e);
        }
        return 1;
    }

    /**
     * Restores the saved accounts from a path
     */
    public int restoreAccounts(final String archivePath, final String password) {
        try {
            return mExecutor.submit(() -> Ringservice.importAccounts(archivePath, password)).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running restoreAccounts()", e);
        }
        return 1;
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
        registerName(account.getAccountID(), password, name);
    }

    /**
     * Register a new name on the blockchain for the account Id
     */
    public void registerName(final String account, final String password, final String name) {
        Log.i(TAG, "registerName()");
        mExecutor.execute(() -> Ringservice.registerName(account, password, name));
    }

    /* contact requests */

    /**
     * @return all trust requests from the daemon for the account Id
     */
    public List<Map<String, String>> getTrustRequests(final String accountId) {
        try {
            return mExecutor.submit(() -> Ringservice.getTrustRequests(accountId).toNative()).get();
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
            if (request != null) {
                VCard vCard = request.getVCard();
                if (vCard != null) {
                    VCardUtils.savePeerProfileToDisk(vCard, accountId,from.getRawRingId() + ".vcf", mDeviceRuntimeService.provideFilesDir());
                }
            }
            account.removeRequest(from);
        }
        mExecutor.execute(() -> Ringservice.acceptTrustRequest(accountId, from.getRawRingId()));
    }

    /**
     * Refuses and blocks a pending trust request
     */
    public boolean discardTrustRequest(final String accountId, final Uri contact) {
        Account account = getAccount(accountId);
        boolean removed = false;
        if (account != null) {
            removed = account.removeRequest(contact);
        }
        mExecutor.execute(() -> Ringservice.discardTrustRequest(accountId, contact.getRawRingId()));
        return removed;
    }

    /**
     * Sends a new trust request
     */
    public void sendTrustRequest(final String accountId, final String to, final Blob message) {
        Log.i(TAG, "sendTrustRequest() " + accountId + " " + to);
        mExecutor.execute(() -> Ringservice.sendTrustRequest(accountId, to, message));
    }


    /**
     * Add a new contact for the account Id on the Daemon
     */
    public void addContact(final String accountId, final String uri) {
        Log.i(TAG, "addContact() " + accountId + " " + uri);
        mExecutor.execute(() -> Ringservice.addContact(accountId, uri));
    }

    /**
     * Remove an existing contact for the account Id on the Daemon
     */
    public void removeContact(final String accountId, final String uri, final boolean ban) {
        Log.i(TAG, "removeContact() " + accountId + " " + uri + " ban:" + ban);
        mExecutor.execute(() -> Ringservice.removeContact(accountId, uri, ban));
    }

    /**
     * @return the contacts list from the daemon
     */
    public List<Map<String, String>> getContacts(final String accountId) {
        try {
            return mExecutor.submit(() -> Ringservice.getContacts(accountId).toNative()).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getContacts()", e);
        }
        return null;
    }

    /**
     * Looks up for the availibility of the name on the blockchain
     */
    public void lookupName(final String account, final String nameserver, final String name) {
        Log.i(TAG, "lookupName() " + account + " " + nameserver + " " + name);
        mExecutor.execute(() -> Ringservice.lookupName(account, nameserver, name));
    }

    public Single<RegisteredName> findRegistrationByName(final String account, final String nameserver, final String name) {
        if (name == null || name.isEmpty()) {
            return Single.just(new RegisteredName());
        }
        return getRegisteredNames()
                .filter(r -> account.equals(r.accountId) && name.equals(r.name))
                .firstOrError()
                .doOnSubscribe(s -> {
                    mExecutor.execute(() -> Ringservice.lookupName(account, nameserver, name));
                })
                .subscribeOn(Schedulers.from(mExecutor));
    }

    /**
     * Reverse looks up the address in the blockchain to find the name
     */
    public void lookupAddress(final String account, final String nameserver, final String address) {
        mExecutor.execute(() -> Ringservice.lookupAddress(account, nameserver, address));
    }

    public void pushNotificationReceived(final String from, final Map<String, String> data) {
        // Log.i(TAG, "pushNotificationReceived()");
        mExecutor.execute(() -> Ringservice.pushNotificationReceived(from, StringMap.toSwig(data)));
    }

    public void setPushNotificationToken(final String pushNotificationToken) {
        Log.i(TAG, "setPushNotificationToken()");
        mExecutor.execute(() -> Ringservice.setPushNotificationToken(pushNotificationToken));
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
        Log.d(TAG, "registrationStateChanged: " + accountId + ", " + newState + ", " + code + ", " + detailString);

        Account account = getAccount(accountId);
        if (account == null) {
            return;
        }
        String oldState = account.getRegistrationState();
        if (oldState.contentEquals(AccountConfig.STATE_INITIALIZING) &&
                !newState.contentEquals(AccountConfig.STATE_INITIALIZING)) {
            account.setDetails(Ringservice.getAccountDetails(account.getAccountID()).toNative());
            account.setCredentials(Ringservice.getCredentials(account.getAccountID()).toNative());
            account.setDevices(Ringservice.getKnownRingDevices(account.getAccountID()).toNative());
            account.setVolatileDetails(Ringservice.getVolatileAccountDetails(account.getAccountID()).toNative());
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
        Log.d(TAG, "volatileAccountDetailsChanged: " + accountId + " " + details.size());
        account.setVolatileDetails(details);
        accountSubject.onNext(account);
    }

    void incomingAccountMessage(String accountId, String callId, String from, Map<String, String> messages) {
        Log.d(TAG, "incomingAccountMessage: " + accountId + " " + messages.size());
        String message = messages.get(CallService.MIME_TEXT_PLAIN);
        if (message != null) {
            mHistoryService
                    .incomingMessage(accountId, callId, from, message)
                    .subscribe(incomingMessageSubject::onNext);
        }
    }

    void accountMessageStatusChanged(String accountId, long messageId, String to, int status) {
        Log.d(TAG, "accountMessageStatusChanged: " + accountId + ", " + messageId + ", " + to + ", " + status);
        mHistoryService
                .accountMessageStatusChanged(accountId, messageId, to, status)
                .subscribe(messageSubject::onNext, e -> Log.e(TAG, "Error updating message", e));
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
        acc.setVolatileDetails(Ringservice.getVolatileAccountDetails(acc.getAccountID()).toNative());
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

    void incomingTrustRequest(String accountId, String from, String message, long received) {
        Log.d(TAG, "incomingTrustRequest: " + accountId + ", " + from + ", " + received);

        Account account = getAccount(accountId);
        if (account != null) {
            TrustRequest request = new TrustRequest(accountId, from, received * 1000L, message);
            VCard vcard = request.getVCard();
            if (vcard != null) {
                CallContact contact = account.getContactFromCache(request.getContactId());
                if (!contact.detailsLoaded) {
                    VCardUtils.savePeerProfileToDisk(vcard, accountId, from + ".vcf", mDeviceRuntimeService.provideFilesDir());
                    mVCardService.loadVCardProfile(vcard)
                            .subscribeOn(Schedulers.computation())
                            .subscribe(profile -> contact.setProfile(profile.first, profile.second));
                }
            }
            account.addRequest(request);
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
        if (account != null)
            account.removeContact(uri, banned);
    }

    void registeredNameFound(String accountId, int state, String address, String name) {
        // Log.d(TAG, "registeredNameFound: " + accountId + ", " + state + ", " + name + ", " + address);

        Account account = getAccount(accountId);
        if (account != null) {
            account.registeredNameFound(state, address, name);
        }

        RegisteredName r = new RegisteredName();
        r.accountId = accountId;
        r.address = address;
        r.name = name;
        r.state = state;
        registeredNameSubject.onNext(r);
    }

    public DataTransferError sendFile(final DataTransfer dataTransfer, File file) {
        mStartingTransfer = dataTransfer;

        DataTransferInfo dataTransferInfo = new DataTransferInfo();
        dataTransferInfo.setAccountId(dataTransfer.getAccountId());
        dataTransferInfo.setPeer(dataTransfer.getPeerId());
        dataTransferInfo.setPath(file.getPath());
        dataTransferInfo.setDisplayName(dataTransfer.getDisplayName());

        Log.i(TAG, "sendFile() id=" + dataTransfer.getId() + " accountId=" + dataTransferInfo.getAccountId() + ", peer=" + dataTransferInfo.getPeer() + ", filePath=" + dataTransferInfo.getPath());
        try {
            return getDataTransferError(mExecutor.submit(() -> Ringservice.sendFile(dataTransferInfo, 0)).get());
        } catch (Exception ignored) {
        }
        return DataTransferError.UNKNOWN;
    }

    public List<Message> getLastMessages(String accountId, long baseTime) {
        try {
            return mExecutor.submit(() -> SwigNativeConverter.toJava(Ringservice.getLastMessages(accountId, baseTime))).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void acceptFileTransfer(long id) {
        acceptFileTransfer(getDataTransfer(id));
    }

    public void acceptFileTransfer(DataTransfer transfer) {
        if (transfer == null)
            return;
        File path = mDeviceRuntimeService.getTemporaryPath(transfer.getPeerId(), transfer.getStoragePath());
        acceptFileTransfer(transfer.getDataTransferId(), path.getAbsolutePath(), 0);
    }

    private void acceptFileTransfer(final Long dataTransferId, final String filePath, final long offset) {
        Log.i(TAG, "acceptFileTransfer() id=" + dataTransferId + ", path=" + filePath + ", offset=" + offset);
        mExecutor.execute(() -> Ringservice.acceptFileTransfer(dataTransferId, filePath, offset));
    }

    public void cancelDataTransfer(final Long dataTransferId) {
        Log.i(TAG, "cancelDataTransfer() id=" + dataTransferId);
        mExecutor.execute(() -> Ringservice.cancelDataTransfer(dataTransferId));
    }

    private class DataTransferRefreshTask extends TimerTask {
        private final DataTransfer mToUpdate;

        DataTransferRefreshTask(DataTransfer t) {
            mToUpdate = t;
        }

        @Override
        public void run() {
            DataTransferEventCode eventCode;
            synchronized (mToUpdate) {
                eventCode = mToUpdate.getEventCode();
                if (eventCode == DataTransferEventCode.ONGOING) {
                    dataTransferEvent(mToUpdate.getDataTransferId(), eventCode.ordinal());
                } else {
                    cancel();
                }
            }
        }
    }

    void dataTransferEvent(final long transferId, int eventCode) {
        DataTransferEventCode dataEvent = getDataTransferEventCode(eventCode);
        DataTransferInfo info = new DataTransferInfo();
        if (getDataTransferError(Ringservice.dataTransferInfo(transferId, info)) != DataTransferError.SUCCESS)
            return;

        boolean outgoing = info.getFlags() == 0;
        DataTransfer transfer = mDataTransfers.get(transferId);
        if (transfer == null) {
            if (outgoing && mStartingTransfer != null) {
                transfer = mStartingTransfer;
                mStartingTransfer = null;
                transfer.setDataTransferId(transferId);
            } else {
                transfer = new DataTransfer(transferId, info.getDisplayName(),
                        outgoing, info.getTotalSize(),
                        info.getBytesProgress(), info.getPeer(), info.getAccountId());
                mHistoryService.insertDataTransfer(transfer).blockingAwait();
            }
            mDataTransfers.put(transferId, transfer);
        } else synchronized (transfer) {
            DataTransferEventCode oldState = transfer.getEventCode();
            if (oldState != dataEvent) {
                if (dataEvent == DataTransferEventCode.ONGOING) {
                    if (mTransferRefreshTimer == null)
                        mTransferRefreshTimer = new Timer();
                    mTransferRefreshTimer.scheduleAtFixedRate(
                            new DataTransferRefreshTask(transfer),
                            DATA_TRANSFER_REFRESH_PERIOD,
                            DATA_TRANSFER_REFRESH_PERIOD);
                } else if (dataEvent.isError()) {
                    if (!transfer.isOutgoing()) {
                        File tmpPath = mDeviceRuntimeService.getTemporaryPath(transfer.getPeerId(), transfer.getStoragePath());
                        tmpPath.delete();
                    }
                } else if (dataEvent == DataTransferEventCode.FINISHED) {
                    if (!transfer.isOutgoing()) {
                        File tmpPath = mDeviceRuntimeService.getTemporaryPath(transfer.getPeerId(), transfer.getStoragePath());
                        File path = mDeviceRuntimeService.getConversationPath(transfer.getPeerId(), transfer.getStoragePath());
                        FileUtils.moveFile(tmpPath, path);
                    }
                }
            }
            transfer.setEventCode(dataEvent);
            transfer.setBytesProgress(info.getBytesProgress());
            mHistoryService.updateDataTransfer(transfer).subscribe();
        }

        dataTransferSubject.onNext(transfer);
    }

    private static DataTransferEventCode getDataTransferEventCode(int eventCode) {
        DataTransferEventCode dataTransferEventCode = DataTransferEventCode.INVALID;
        try {
            dataTransferEventCode = DataTransferEventCode.values()[eventCode];
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Log.e(TAG, "getEventCode: invalid data transfer status from daemon");
        }
        return dataTransferEventCode;
    }

    private static DataTransferError getDataTransferError(Long errorCode) {
        DataTransferError dataTransferError = DataTransferError.UNKNOWN;
        if (errorCode == null) {
            Log.e(TAG, "getDataTransferError: invalid error code");
        } else {
            try {
                dataTransferError = DataTransferError.values()[errorCode.intValue()];
            } catch (ArrayIndexOutOfBoundsException ignored) {
                Log.e(TAG, "getDataTransferError: invalid data transfer error from daemon");
            }
        }
        return dataTransferError;
    }

    public DataTransfer getDataTransfer(long id) {
        return mDataTransfers.get(id);
    }

    public Subject<DataTransfer> getDataTransfers() {
        return dataTransferSubject;
    }

    public Observable<DataTransfer> observeDataTransfer(DataTransfer transfer) {
        return dataTransferSubject
                .filter(t -> t == transfer)
                .startWith(transfer);
    }

    public void setProxyEnabled(boolean enabled) {
        mExecutor.execute(() -> {
            for (Account acc : mAccountList) {
                if (acc.isRing() && (acc.isDhtProxyEnabled() != enabled)) {
                    Log.d(TAG, (enabled ? "Enabling" : "Disabling") + " proxy for account " + acc.getAccountID());
                    acc.setDhtProxyEnabled(enabled);
                    StringMap details = Ringservice.getAccountDetails(acc.getAccountID());
                    details.set(ConfigKey.PROXY_ENABLED.key(), enabled ? "true" : "false");
                    Ringservice.setAccountDetails(acc.getAccountID(), details);
                }
            }
        });
    }
}
