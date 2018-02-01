/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
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
package cx.ring.services;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.CallContact;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TrustRequest;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.SwigNativeConverter;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

/**
 * This service handles the accounts (Ring and SIP)
 * - Load and manage the accounts stored in the daemon
 * - Keep a local cache of the accounts
 * - handle the callbacks that are send by the daemon
 * <p>
 * Events are broadcasted by the daemon's callbacks:
 * - ACCOUNTS_CHANGED
 * - ACCOUNT_ADDED
 * - VOLUME_CHANGED
 * - STUN_STATUS_FAILURE
 * - REGISTRATION_STATE_CHANGED
 * - INCOMING_ACCOUNT_MESSAGE
 * - ACCOUNT_MESSAGE_STATUS_CHANGED
 * - ERROR_ALERT
 * - GET_APP_DATA_PATH
 * - EXPORT_ON_RING_ENDED
 * - NAME_REGISTRATION_ENDED
 * - REGISTERED_NAME_FOUND
 * - MIGRATION_ENDED
 * - INCOMING_TRUST_REQUEST
 */
public class AccountService extends Observable {

    private static final String TAG = AccountService.class.getSimpleName();

    private static final int VCARD_CHUNK_SIZE = 1000;

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    @Named("ApplicationExecutor")
    ExecutorService mApplicationExecutor;

    @Inject
    HistoryService mHistoryService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private Account mCurrentAccount;
    private List<Account> mAccountList = new ArrayList<>();
    private boolean mHasSipAccount;
    private boolean mHasRingAccount;
    private AtomicBoolean mAccountsLoaded = new AtomicBoolean(false);

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

        mApplicationExecutor.submit(new Runnable() {
            @Override
            public void run() {
                refreshAccountsCacheFromDaemon();

                if (!mAccountList.isEmpty()) {
                    setCurrentAccount(mAccountList.get(0));
                } else {
                    setChanged();
                    ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.ACCOUNTS_CHANGED);
                    notifyObservers(event);
                }

                setAccountsActive(isConnected);
                Ringservice.connectivityChanged();
            }
        });
    }

    private void refreshAccountsCacheFromDaemon() {
        mAccountsLoaded.set(false);
        mAccountList = new ArrayList<>();
        List<String> accountIds = getAccountList();
        for (String accountId : accountIds) {
            Map<String, String> details = getAccountDetails(accountId);
            List<Map<String, String>> credentials = getCredentials(accountId);
            Map<String, String> volatileAccountDetails = getVolatileAccountDetails(accountId);
            Account account = new Account(accountId, details, credentials, volatileAccountDetails);
            mAccountList.add(account);

            if (account.isSip()) {
                mHasSipAccount = true;
            } else if (account.isRing()) {
                mHasRingAccount = true;

                account.setDevices(getKnownRingDevices(accountId));
                account.setContacts(getContacts(accountId));
                List<Map<String, String>> requests = getTrustRequests(accountId);
                for (Map<String, String> requestInfo : requests) {
                    TrustRequest request = new TrustRequest(accountId, requestInfo);
                    account.addRequest(request);
                    // If name is in cache this can be synchronous
                    lookupAddress(accountId, "", request.getContactId());
                }
                for (CallContact contact : account.getContacts().values()) {
                    lookupAddress(accountId, "", contact.getPhones().get(0).getNumber().getRawRingId());
                }
            }
        }
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
    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    public Account addAccount(final Map map) {

        String accountId = FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Log.i(TAG, "addAccount() thread running...");
                        return Ringservice.addAccount(StringMap.toSwig(map));
                    }
                }
        );

        if (accountId == null) {
            return null;
        }

        Map<String, String> accountDetails = getAccountDetails(accountId);
        Map<String, String> accountVolatileDetails = getVolatileAccountDetails(accountId);
        List<Map<String, String>> accountCredentials = getCredentials(accountId);
        Map<String, String> accountDevices = getKnownRingDevices(accountId);

        Account account = getAccount(accountId);

        if (account == null) {
            account = new Account(accountId, accountDetails, accountCredentials, accountVolatileDetails);
            account.setDevices(accountDevices);
            if (account.isSip()) {
                account.setRegistrationState(AccountConfig.STATE_READY, -1);
            }
        }

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.ACCOUNT_ADDED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.STATE, account.getRegistrationState());
        notifyObservers(event);

        setCurrentAccount(account);

        return account;
    }

    /**
     * @return the current Account from the local cache
     */
    public Account getCurrentAccount() {
        return mCurrentAccount;
    }

    /**
     * Sets the current Account in the local cache (also sends a ACCOUNTS_CHANGED event)
     *
     * @param currentAccount
     */
    public void setCurrentAccount(Account currentAccount) {
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
     * @param accountId
     * @return the Account from the local cache that matches the accountId
     */
    public Account getAccount(String accountId) {
        for (Account account : mAccountList) {
            String accountID = account.getAccountID();
            if (accountID != null && accountID.equals(accountId)) {
                return account;
            }
        }
        return null;
    }

    /**
     * @return Accounts list from the local cache
     */
    public List<Account> getAccounts() {
        return mAccountList;
    }

    /**
     * put VCard on the DHT
     *
     * @param callId
     * @param accountId
     */
    public void sendProfile(final String callId, final String accountId) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                VCard vcard = VCardUtils.loadLocalProfileFromDisk(
                        mDeviceRuntimeService.provideFilesDir(),
                        accountId);
                String stringVCard = VCardUtils.vcardToString(vcard);

                int nbTotal = stringVCard.length() / VCARD_CHUNK_SIZE + (stringVCard.length() % VCARD_CHUNK_SIZE != 0 ? 1 : 0);
                int i = 1;
                Random r = new Random(System.currentTimeMillis());
                int key = r.nextInt();

                Log.d(TAG, "sendProfile, vcard " + stringVCard);

                while (i <= nbTotal) {
                    HashMap<String, String> chunk = new HashMap<>();
                    Log.d(TAG, "length vcard " + stringVCard.length() + " id " + key + " part " + i + " nbTotal " + nbTotal);
                    String keyHashMap = VCardUtils.MIME_RING_PROFILE_VCARD + "; id=" + key + ",part=" + i + ",of=" + nbTotal;
                    String message = stringVCard.substring(0, Math.min(VCARD_CHUNK_SIZE, stringVCard.length()));
                    chunk.put(keyHashMap, message);
                    if (stringVCard.length() > VCARD_CHUNK_SIZE) {
                        stringVCard = stringVCard.substring(VCARD_CHUNK_SIZE);
                    }
                    i++;
                    Ringservice.sendTextMessage(callId, StringMap.toSwig(chunk), "Me", false);
                }
            }
        });
    }

    /**
     * @return Account Ids list from Daemon
     */
    public List<String> getAccountList() {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<List<String>>() {
                    @Override
                    public List<String> call() throws Exception {
                        Log.i(TAG, "getAccountList() thread running...");
                        return new ArrayList<>(Ringservice.getAccountList());
                    }
                }
        );
    }

    /**
     * Sets the order of the accounts in the Daemon
     *
     * @param accountOrder The ordered list of account ids
     */
    public void setAccountOrder(final List<String> accountOrder) {
        StringBuilder order = new StringBuilder();
        for (String accountId : accountOrder) {
            order.append(accountId);
            order.append(File.separator);
        }
        final String orderForDaemon = order.toString();

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setAccountsOrder() " + orderForDaemon + " thread running...");
                        Ringservice.setAccountsOrder(orderForDaemon);
                        return true;
                    }
                }
        );
    }

    /**
     * @param accountId
     * @return the account details from the Daemon
     */
    public Map<String, String> getAccountDetails(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getAccountDetails() thread running...");
                        return Ringservice.getAccountDetails(accountId).toNative();
                    }
                }
        );
    }

    /**
     * Sets the account details in the Daemon
     *
     * @param accountId
     * @param map
     */
    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    public void setAccountDetails(final String accountId, final Map map) {
        Log.i(TAG, "setAccountDetails() " + map.get("Account.hostname"));
        final StringMap swigmap = StringMap.toSwig(map);

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Ringservice.setAccountDetails(accountId, swigmap);
                        Log.i(TAG, "setAccountDetails() thread running... " + swigmap.get("Account.hostname"));
                        return true;
                    }
                }
        );

    }

    /**
     * Sets the activation state of the account in the Daemon
     *
     * @param accountId
     * @param active
     */
    public void setAccountActive(final String accountId, final boolean active) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setAccountActive() thread running... " + accountId + " -> " + active);
                        Ringservice.setAccountActive(accountId, active);
                        return true;
                    }
                }
        );
    }

    /**
     * Sets the activation state of all the accounts in the Daemon
     *
     * @param active
     */
    public void setAccountsActive(final boolean active) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setAccountsActive() thread running... " + active);
                        StringVect list = Ringservice.getAccountList();
                        for (int i = 0, n = list.size(); i < n; i++) {
                            Ringservice.setAccountActive(list.get(i), active);
                        }
                        return true;
                    }
                }
        );
    }

    /**
     * Sets the video activation state of all the accounts in the local cache
     *
     * @param isEnabled
     */
    public void setAccountsVideoEnabled(boolean isEnabled) {
        for (Account account : mAccountList) {
            account.setDetail(ConfigKey.VIDEO_ENABLED, isEnabled);
        }

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.ACCOUNTS_CHANGED);
        notifyObservers(event);
    }

    /**
     * @param accountId
     * @return the account volatile details from the Daemon
     */
    public Map<String, String> getVolatileAccountDetails(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getVolatileAccountDetails() thread running...");
                        return Ringservice.getVolatileAccountDetails(accountId).toNative();
                    }
                }
        );
    }

    /**
     * @param accountType
     * @return the default template (account details) for a type of account
     */
    public Map<String, String> getAccountTemplate(final String accountType) {
        Log.i(TAG, "getAccountTemplate() " + accountType);
        return Ringservice.getAccountTemplate(accountType).toNative();
    }

    /**
     * Removes the account in the Daemon
     *
     * @param accountId
     */
    public void removeAccount(final String accountId) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "removeAccount() thread running...");
                        Ringservice.removeAccount(accountId);
                        return true;
                    }
                }
        );
    }

    /**
     * Exports the account on the DHT (used for multidevices feature)
     *
     * @param accountId
     * @param password
     * @return the generated pin
     */
    public String exportOnRing(final String accountId, final String password) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Log.i(TAG, "exportOnRing() thread running...");
                        return Ringservice.exportOnRing(accountId, password);
                    }
                }
        );
    }

    /**
     * @param accountId
     * @return the list of the account's devices from the Daemon
     */
    public Map<String, String> getKnownRingDevices(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getKnownRingDevices() thread running...");
                        return Ringservice.getKnownRingDevices(accountId).toNative();
                    }
                }
        );
    }

    /**
     * @param accountId id of the account used with the device
     * @param deviceId  id of the device to revoke
     * @param password  password of the account
     */
    public void revokeDevice(final String accountId, final String password, final String deviceId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "revokeDevice() thread running...");
                        return Ringservice.revokeDevice(accountId, password, deviceId);
                    }
                }
        );
    }

    /**
     * @param accountId id of the account used with the device
     * @param newName   new device name
     */
    public void renameDevice(final String accountId, final String newName) {
        final Account account = getAccount(accountId);
        account.setDevices(FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "renameDevice() thread running... " + newName);
                        StringMap details = Ringservice.getAccountDetails(accountId);
                        details.set(ConfigKey.ACCOUNT_DEVICE_NAME.key(), newName);
                        Ringservice.setAccountDetails(accountId, details);
                        return Ringservice.getKnownRingDevices(accountId).toNative();
                    }
                }
        ));
        account.setDetail(ConfigKey.ACCOUNT_DEVICE_NAME, newName);
    }

    /**
     * Sets the active codecs list of the account in the Daemon
     *
     * @param codecs
     * @param accountId
     */
    public void setActiveCodecList(final List codecs, final String accountId) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setActiveCodecList() thread running...");
                        UintVect list = new UintVect(codecs.size());
                        for (Object codec : codecs) {
                            list.add((Long) codec);
                        }
                        Ringservice.setActiveCodecList(accountId, list);

                        return true;
                    }
                }
        );
    }

    /**
     * @param accountId
     * @return The account's codecs list from the Daemon
     */
    public List<Codec> getCodecList(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<List<Codec>>() {
                    @Override
                    public List<Codec> call() throws Exception {
                        Log.i(TAG, "getCodecList() thread running...");
                        ArrayList<Codec> results = new ArrayList<>();

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
                    }
                }
        );
    }

    /**
     * @param accountID
     * @param certificatePath
     * @param privateKeyPath
     * @param privateKeyPass
     * @return
     */
    public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "validateCertificatePath() thread running...");
                        return Ringservice.validateCertificatePath(accountID, certificatePath, privateKeyPath, "", "").toNative();
                    }
                }
        );
    }

    /**
     * @param accountId
     * @param certificate
     * @return
     */
    public Map<String, String> validateCertificate(final String accountId, final String certificate) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "validateCertificate() thread running...");
                        return Ringservice.validateCertificate(accountId, certificate).toNative();
                    }
                }
        );
    }

    /**
     * @param certificatePath
     * @return
     */
    public Map<String, String> getCertificateDetailsPath(final String certificatePath) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getCertificateDetailsPath() thread running...");
                        return Ringservice.getCertificateDetails(certificatePath).toNative();
                    }
                }
        );
    }

    /**
     * @param certificateRaw
     * @return
     */
    public Map<String, String> getCertificateDetails(final String certificateRaw) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getCertificateDetails() thread running...");
                        return Ringservice.getCertificateDetails(certificateRaw).toNative();
                    }
                }
        );
    }

    /**
     * @return the supported TLS methods from the Daemon
     */
    public List<String> getTlsSupportedMethods() {
        Log.i(TAG, "getTlsSupportedMethods()");
        return SwigNativeConverter.convertSwigToNative(Ringservice.getSupportedTlsMethod());
    }

    /**
     * @param accountId
     * @return the account's credentials from the Daemon
     */
    public List<Map<String, String>> getCredentials(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<List<Map<String, String>>>() {
                    @Override
                    public List<Map<String, String>> call() throws Exception {
                        Log.i(TAG, "getCredentials() thread running...");
                        return Ringservice.getCredentials(accountId).toNative();
                    }
                }
        );
    }

    /**
     * Sets the account's credentials in the Daemon
     *
     * @param accountId
     * @param creds
     */
    public void setCredentials(final String accountId, final List creds) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setCredentials() thread running...");
                        Ringservice.setCredentials(accountId, SwigNativeConverter.convertFromNativeToSwig(creds));
                        return true;
                    }
                }
        );
    }

    /**
     * Sets the registration state to true for all the accounts in the Daemon
     */
    public void registerAllAccounts() {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "registerAllAccounts() thread running...");
                        Ringservice.registerAllAccounts();
                        return true;
                    }
                }
        );
    }

    /**
     * Backs  up all the accounts into to an archive in the path
     *
     * @param accountIds
     * @param toDir
     * @param password
     * @return
     */
    public int backupAccounts(final List accountIds, final String toDir, final String password) {

        //noinspection ConstantConditions
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        StringVect ids = new StringVect();
                        for (Object s : accountIds) {
                            ids.add((String) s);
                        }
                        return Ringservice.exportAccounts(ids, toDir, password);
                    }
                }
        );
    }

    /**
     * Restores the saved accounts from a path
     *
     * @param archivePath
     * @param password
     * @return
     */
    public int restoreAccounts(final String archivePath, final String password) {

        //noinspection ConstantConditions
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return Ringservice.importAccounts(archivePath, password);
                    }
                }
        );
    }

    /**
     * Registers a new name on the blockchain for the account
     *
     * @param account
     * @param password
     * @param name
     */
    public void registerName(final Account account, final String password, final String name) {

        if (account.registeringUsername) {
            Log.w(TAG, "Already trying to register username");
            return;
        }

        registerName(account.getAccountID(), password, name);
    }

    /**
     * Register a new name on the blockchain for the account Id
     *
     * @param account
     * @param password
     * @param name
     */
    public void registerName(final String account, final String password, final String name) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "registerName() thread running...");
                        Ringservice.registerName(account, password, name);
                        return true;
                    }
                }
        );
    }

    /* contact requests */

    /**
     * @param accountId
     * @return all trust requests from the daemon for the account Id
     */
    public List<Map<String, String>> getTrustRequests(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<List<Map<String, String>>>() {
                    @Override
                    public List<Map<String, String>> call() throws Exception {
                        Log.i(TAG, "getTrustRequests() thread running...");
                        return Ringservice.getTrustRequests(accountId).toNative();
                    }
                }
        );
    }

    /**
     * Accepts a pending trust request
     *
     * @param accountId
     * @param from
     * @return
     */
    public Boolean acceptTrustRequest(final String accountId, final String from) {
        Account account = getAccount(accountId);
        account.removeRequest(from);
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "acceptTrustRequest() thread running...");
                        boolean ok = Ringservice.acceptTrustRequest(accountId, from);
                        if (ok) {
                            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.INCOMING_TRUST_REQUEST);
                            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
                            notifyObservers(event);
                        }
                        return ok;
                    }
                }
        );
    }

    /**
     * Refuses and blocks a pending trust request
     *
     * @param accountId
     * @param from
     * @return
     */
    public void discardTrustRequest(final String accountId, final String from) {
        Account account = getAccount(accountId);
        account.removeRequest(from);
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Log.i(TAG, "discardTrustRequest() " + accountId + " " + from);
                        boolean ok = Ringservice.discardTrustRequest(accountId, from);
                        if (ok) {
                            setChanged();
                            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.INCOMING_TRUST_REQUEST);
                            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
                            notifyObservers(event);
                        }
                        return null;
                    }
                }
        );
    }

    /**
     * Sends a new trust request
     *
     * @param accountId
     * @param to
     * @param message
     */
    public void sendTrustRequest(final String accountId, final String to, final Blob message) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "sendTrustRequest() thread running...");
                        Ringservice.sendTrustRequest(accountId, to, message);
                        return true;
                    }
                }
        );
    }


    /**
     * Add a new contact for the account Id on the Daemon
     *
     * @param accountId
     * @param uri
     */
    public void addContact(final String accountId, final String uri) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "addContact() thread running...");
                        Ringservice.addContact(accountId, uri);
                        return true;
                    }
                }
        );
    }

    /**
     * Remove an existing contact for the account Id on the Daemon
     *
     * @param accountId
     * @param uri
     */
    public void removeContact(final String accountId, final String uri, final boolean ban) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "removeContact() thread running...");
                        Ringservice.removeContact(accountId, uri, ban);
                        return true;
                    }
                }
        );
    }

    /**
     * @param accountId
     * @return the contacts list from the daemon
     */
    public List<Map<String, String>> getContacts(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<List<Map<String, String>>>() {
                    @Override
                    public List<Map<String, String>> call() throws Exception {
                        Log.i(TAG, "getContacts() thread running...");
                        return Ringservice.getContacts(accountId).toNative();
                    }
                }
        );
    }

    /**
     * Looks up for the availibility of the name on the blockchain
     *
     * @param account
     * @param nameserver
     * @param name
     */
    public void lookupName(final String account, final String nameserver, final String name) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "lookupName() thread running...");
                        Ringservice.lookupName(account, nameserver, name);
                        return true;
                    }
                }
        );
    }

    /**
     * Reverse looks up the address in the blockchain to find the name
     *
     * @param account
     * @param nameserver
     * @param address
     */
    public void lookupAddress(final String account, final String nameserver, final String address) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "lookupAddress() " + address);
                        Ringservice.lookupAddress(account, nameserver, address);
                        return true;
                    }
                }
        );
    }

    public void pushNotificationReceived(final String from, final Map<String, String> data) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "pushNotificationReceived() " + from + " " + data.size());
                        Ringservice.pushNotificationReceived(from, StringMap.toSwig(data));
                        return true;
                    }
                }
        );
    }

    public void setPushNotificationToken(final String pushNotificationToken) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Log.i(TAG, "setPushNotificationToken() " + pushNotificationToken);
                        Ringservice.setPushNotificationToken(pushNotificationToken);
                        return null;
                    }
                }
        );
    }

    public void volumeChanged(String device, int value) {
        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VOLUME_CHANGED);
        event.addEventInput(ServiceEvent.EventInput.DEVICE, device);
        event.addEventInput(ServiceEvent.EventInput.VALUE, value);
        notifyObservers(event);
    }

    public void accountsChanged() {
        String currentAccountId = mCurrentAccount == null ? "" : mCurrentAccount.getAccountID();

        // Accounts have changed in Daemon, we have to update our local cache
        refreshAccountsCacheFromDaemon();

        // if there was a current account we restore it according to the new list
        Account currentAccount = getAccount(currentAccountId);
        if (currentAccount != null) {
            mCurrentAccount = currentAccount;
        } else if (!mAccountList.isEmpty()) {
            // no current account, by default it will be the first one
            mCurrentAccount = mAccountList.get(0);
        } else {
            mCurrentAccount = null;
        }

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.ACCOUNTS_CHANGED);
        notifyObservers(event);
    }

    public void stunStatusFailure(String accountId) {
        Log.d(TAG, "stun status failure: " + accountId);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.STUN_STATUS_FAILURE);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        notifyObservers(event);
    }

    public void registrationStateChanged(String accountId, String newState, int code, String detailString) {
        Log.d(TAG, "stun status registrationStateChanged: " + accountId + ", " + newState + ", " + code + ", " + detailString);

        Account account = getAccount(accountId);
        if (account == null) {
            return;
        }
        String oldState = account.getRegistrationState();
        if (oldState.contentEquals(AccountConfig.STATE_INITIALIZING) &&
                !newState.contentEquals(AccountConfig.STATE_INITIALIZING)) {
            account.setDetails(getAccountDetails(account.getAccountID()));
            account.setCredentials(getCredentials(account.getAccountID()));
            account.setDevices(getKnownRingDevices(account.getAccountID()));
            account.setVolatileDetails(getVolatileAccountDetails(account.getAccountID()));
        } else {
            account.setRegistrationState(newState, code);
        }

        if (!oldState.equals(newState)) {
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.REGISTRATION_STATE_CHANGED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.STATE, newState);
            event.addEventInput(ServiceEvent.EventInput.DETAIL_CODE, code);
            event.addEventInput(ServiceEvent.EventInput.DETAIL_STRING, detailString);
            notifyObservers(event);
        }
    }

    public void volatileAccountDetailsChanged(String accountId, StringMap details) {
        Account account = getAccount(accountId);
        if (account == null) {
            return;
        }
        Log.d(TAG, "volatileAccountDetailsChanged: " + accountId + " " + details.size());
        account.setVolatileDetails(details.toNative());
    }

    public void accountMessageStatusChanged(String accountId, long messageId, String to, int status) {
        Log.d(TAG, "accountMessageStatusChanged: " + accountId + ", " + messageId + ", " + to + ", " + status);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.ACCOUNT_MESSAGE_STATUS_CHANGED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.MESSAGE_ID, messageId);
        event.addEventInput(ServiceEvent.EventInput.TO, to);
        event.addEventInput(ServiceEvent.EventInput.STATE, status);
        notifyObservers(event);
    }

    public void errorAlert(int alert) {
        Log.d(TAG, "errorAlert : " + alert);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.ERROR_ALERT);
        event.addEventInput(ServiceEvent.EventInput.ALERT, alert);
        notifyObservers(event);
    }

    public void knownDevicesChanged(String accountId, StringMap devices) {
        Log.d(TAG, "knownDevicesChanged: " + accountId + ", " + devices);

        Account accountChanged = getAccount(accountId);
        if (accountChanged != null) {
            accountChanged.setDevices(devices.toNative());
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.KNOWN_DEVICES_CHANGED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.DEVICES, devices);
            notifyObservers(event);
        }
    }

    public void exportOnRingEnded(String accountId, int code, String pin) {
        Log.d(TAG, "exportOnRingEnded: " + accountId + ", " + code + ", " + pin);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.EXPORT_ON_RING_ENDED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.CODE, code);
        event.addEventInput(ServiceEvent.EventInput.PIN, pin);
        notifyObservers(event);
    }

    public void nameRegistrationEnded(String accountId, int state, String name) {
        Log.d(TAG, "nameRegistrationEnded: " + accountId + ", " + state + ", " + name);

        Account acc = getAccount(accountId);
        if (acc == null) {
            Log.w(TAG, "Can't find account for name registration callback");
            return;
        }

        acc.registeringUsername = false;
        acc.setVolatileDetails(getVolatileAccountDetails(acc.getAccountID()));
        acc.setDetail(ConfigKey.ACCOUNT_REGISTERED_NAME, name);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.NAME_REGISTRATION_ENDED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.STATE, state);
        event.addEventInput(ServiceEvent.EventInput.NAME, name);
        notifyObservers(event);
    }

    public void migrationEnded(String accountId, String state) {
        Log.d(TAG, "migrationEnded: " + accountId + ", " + state);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.MIGRATION_ENDED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.STATE, state);
        notifyObservers(event);
    }

    public void deviceRevocationEnded(String accountId, String device, int state) {
        Log.d(TAG, "deviceRevocationEnded: " + accountId + ", " + device + ", " + state);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.DEVICE_REVOCATION_ENDED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.DEVICE, device);
        event.addEventInput(ServiceEvent.EventInput.STATE, state);
        notifyObservers(event);
    }

    public void incomingTrustRequest(String accountId, String from, Blob message, long received) {
        Log.d(TAG, "incomingTrustRequest: " + accountId + ", " + from + ", " + message + ", " + received);

        Account account = getAccount(accountId);
        if (account != null) {
            TrustRequest request = new TrustRequest(accountId, from, received, message.toJavaString());
            account.addRequest(request);
            lookupAddress(accountId, "", from);
        }
    }


    public void contactAdded(String accountId, String uri, boolean confirmed) {
        Log.d(TAG, "contactAdded: " + accountId + ", " + uri + ", " + confirmed);

        Account account = getAccount(accountId);
        if (account == null) {
            Log.d(TAG, "contactAdded: unknown account" + accountId);
            return;
        }
        account.addContact(uri, confirmed);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONTACT_ADDED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.CONFIRMED, confirmed);
        notifyObservers(event);
    }

    public void contactRemoved(String accountId, String uri, boolean banned) {
        Log.d(TAG, "contactRemoved: " + accountId + ", " + uri + ", " + banned);

        Account account = getAccount(accountId);
        if (account == null) {
            Log.d(TAG, "contactRemoved: unknown account" + accountId);
            return;
        }
        account.removeContact(uri, banned);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONTACT_REMOVED);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.BANNED, banned);
        notifyObservers(event);
    }

    public void registeredNameFound(String accountId, int state, String address, String name) {
        Log.d(TAG, "registeredNameFound: " + accountId + ", " + state + ", " + name + ", " + address);

        Account account = getAccount(accountId);
        if (account != null) {
            if (state == 0) {
                CallContact contact = account.getContact(address);
                if (contact != null) {
                    contact.setUsername(name);
                }
            }
            TrustRequest request = account.getRequest(address);
            if (request != null) {
                Log.d(TAG, "registeredNameFound: updating TrustRequest " + name);
                boolean resolved = request.isNameResolved();
                request.setUsername(name);
                if (!resolved) {
                    Log.d(TAG, "registeredNameFound: TrustRequest resolved " + name);
                    setChanged();
                    ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.INCOMING_TRUST_REQUEST);
                    event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
                    event.addEventInput(ServiceEvent.EventInput.FROM, request.getContactId());
                    notifyObservers(event);
                }
            }
        }

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.REGISTERED_NAME_FOUND);
        event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
        event.addEventInput(ServiceEvent.EventInput.STATE, state);
        event.addEventInput(ServiceEvent.EventInput.ADDRESS, address);
        event.addEventInput(ServiceEvent.EventInput.NAME, name);
        notifyObservers(event);
    }
}