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

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.SWIGTYPE_p_time_t;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
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

    private static final String TAG = AccountService.class.getName();

    private static final int VCARD_CHUNK_SIZE = 1000;

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    @Named("ApplicationExecutor")
    ExecutorService mApplicationExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private Account mCurrentAccount;
    private List<Account> mAccountList;
    private ConfigurationCallbackHandler mCallbackHandler;
    private boolean mHasSipAccount;
    private boolean mHasRingAccount;

    public AccountService() {
        mCallbackHandler = new ConfigurationCallbackHandler();
        mAccountList = new ArrayList<>();
    }

    public ConfigurationCallbackHandler getCallbackHandler() {
        return mCallbackHandler;
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

        mApplicationExecutor.submit(new Runnable() {
            @Override
            public void run() {
                refreshAccountsCacheFromDaemon();

                if (!mAccountList.isEmpty()) {
                    setCurrentAccount(mAccountList.get(0));
                }

                setAccountsActive(isConnected);
                Ringservice.connectivityChanged();
            }
        });
    }

    private void refreshAccountsCacheFromDaemon() {
        mAccountList.clear();
        List<String> accountIds = getAccountList();
        for (String accountId : accountIds) {
            Map<String, String> details = getAccountDetails(accountId);
            List<Map<String, String>> credentials = getCredentials(accountId);
            Map<String, String> volatileAccountDetails = getVolatileAccountDetails(accountId);
            Account account = new Account(accountId, details, credentials, volatileAccountDetails);
            account.setDevices(getKnownRingDevices(accountId));

            if (account.isSip()) {
                mHasSipAccount = true;
            }

            if (account.isRing()) {
                mHasRingAccount = true;
            }

            mAccountList.add(account);
        }
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
        List<String> orderedAccountIdList = new ArrayList<>();
        String selectedID = mCurrentAccount.getAccountID();
        orderedAccountIdList.add(selectedID);
        for (Account account : getAccounts()) {
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
            if (account.getAccountID().equals(accountId)) {
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
                final String ringProfileVCardMime = "x-ring/ring.profile.vcard";

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
                    String keyHashMap = ringProfileVCardMime + "; id=" + key + ",part=" + i + ",of=" + nbTotal;
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

        ArrayList<Account> newlist = new ArrayList<>(mAccountList.size());
        String order = "";
        for (String accountId : accountOrder) {
            Account account = getAccount(accountId);
            if (account != null) {
                newlist.add(account);
            }
            order += accountId + File.separator;
        }

        final String orderForDaemon = order;

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
     * @param uri
     * @return an Account from the local cache that corresponds to the uri
     */
    public Account guessAccount(Uri uri) {
        if (uri.isRingId()) {
            for (Account account : mAccountList) {
                if (account.isRing()) {
                    return account;
                }
            }
            // ring ids must be called with ring accounts
            return null;
        }
        for (Account account : mAccountList) {
            if (account.isSip() && account.getHost().equals(uri.getHost())) {
                return account;
            }
        }
        if (uri.isSingleIp()) {
            for (Account account : mAccountList) {
                if (account.isIP2IP()) {
                    return account;
                }
            }
        }
        return mAccountList.get(0);
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
     * @param deviceId id of the device to revoke
     * @param password password of the account
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
     * @param newName new device name
     */
    public void renameDevice(final String accountId, final String newName) {
        final Account acc = getAccount(accountId);
        acc.setDevices(FutureUtils.executeDaemonThreadCallable(
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
        acc.setDetail(ConfigKey.ACCOUNT_DEVICE_NAME, newName);
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
                        Log.i(TAG, "lookupAddress() thread running...");
                        Ringservice.lookupAddress(account, nameserver, address);
                        return true;
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
    public StringMap getTrustRequests(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<StringMap>() {
                    @Override
                    public StringMap call() throws Exception {
                        Log.i(TAG, "getTrustRequests() thread running...");
                        return Ringservice.getTrustRequests(accountId);
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

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "acceptTrustRequest() thread running...");
                        return Ringservice.acceptTrustRequest(accountId, from);
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
    public Boolean discardTrustRequest(final String accountId, final String from) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "discardTrustRequest() thread running...");
                        return Ringservice.discardTrustRequest(accountId, from);
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

    class ConfigurationCallbackHandler extends ConfigurationCallback {

        @Override
        public void volumeChanged(String device, int value) {
            Log.d(TAG, "volume changed");
            super.volumeChanged(device, value);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VOLUME_CHANGED);
            event.addEventInput(ServiceEvent.EventInput.DEVICE, device);
            event.addEventInput(ServiceEvent.EventInput.VALUE, value);
            notifyObservers(event);
        }

        @Override
        public void accountsChanged() {
            super.accountsChanged();
            Log.d(TAG, "accounts changed");
            String currentAccountId = "";

            if (mCurrentAccount != null) {
                currentAccountId = mCurrentAccount.getAccountID();
            }

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

        @Override
        public void stunStatusFailure(String accountId) {
            Log.d(TAG, "stun status failure: " + accountId);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.STUN_STATUS_FAILURE);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            notifyObservers(event);
        }

        @Override
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

        @Override
        public void incomingAccountMessage(String accountId, String from, StringMap messages) {

            String msg = null;
            final String textPlainMime = "text/plain";
            if (null != messages && messages.has_key(textPlainMime)) {
                msg = messages.getRaw(textPlainMime).toJavaString();
            }
            if (msg == null) {
                return;
            }

            Log.d(TAG, "incomingAccountMessage: " + accountId + ", " + from + ", " + msg);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.INCOMING_ACCOUNT_MESSAGE);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.FROM, from);
            event.addEventInput(ServiceEvent.EventInput.MESSAGES, msg);
            notifyObservers(event);
        }

        @Override
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

        @Override
        public void errorAlert(int alert) {
            Log.d(TAG, "errorAlert : " + alert);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.ERROR_ALERT);
            event.addEventInput(ServiceEvent.EventInput.ALERT, alert);
            notifyObservers(event);
        }

        @Override
        public void getHardwareAudioFormat(IntVect ret) {
            Log.d(TAG, "getHardwareAudioFormat: " + ret.toString());

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.GET_HARDWARE_AUDIO_FORMAT);
            event.addEventInput(ServiceEvent.EventInput.FORMATS, ret);
            notifyObservers(event);
        }

        @Override
        public void getAppDataPath(String name, StringVect ret) {
            Log.d(TAG, "getAppDataPath: " + name + ", " + ret);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.GET_APP_DATA_PATH);
            event.addEventInput(ServiceEvent.EventInput.NAME, name);
            event.addEventInput(ServiceEvent.EventInput.PATHS, ret);
            notifyObservers(event);
        }

        @Override
        public void knownDevicesChanged(String accountId, StringMap devices) {
            Log.d(TAG, "knownDevicesChanged: " + accountId + ", " + devices);

            Account accountChanged = getAccount(accountId);
            accountChanged.setDevices(devices.toNative());

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.KNOWN_DEVICES_CHANGED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.DEVICES, devices);
            notifyObservers(event);
        }

        @Override
        public void exportOnRingEnded(String accountId, int code, String pin) {
            Log.d(TAG, "exportOnRingEnded: " + accountId + ", " + code + ", " + pin);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.EXPORT_ON_RING_ENDED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.CODE, code);
            event.addEventInput(ServiceEvent.EventInput.PIN, pin);
            notifyObservers(event);
        }

        @Override
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

        @Override
        public void registeredNameFound(String accountId, int state, String address, String name) {
            Log.d(TAG, "registeredNameFound: " + accountId + ", " + state + ", " + name + ", " + address);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.REGISTERED_NAME_FOUND);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.STATE, state);
            event.addEventInput(ServiceEvent.EventInput.ADDRESS, address);
            event.addEventInput(ServiceEvent.EventInput.NAME, name);
            notifyObservers(event);
        }

        @Override
        public void migrationEnded(String accountId, String state) {
            Log.d(TAG, "migrationEnded: " + accountId + ", " + state);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.MIGRATION_ENDED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.STATE, state);
            notifyObservers(event);
        }

        @Override
        public void deviceRevocationEnded(String accountId, String device, int state) {
            Log.d(TAG, "deviceRevocationEnded: " + accountId + ", " + device + ", " + state);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.DEVICE_REVOCATION_ENDED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.DEVICE, device);
            event.addEventInput(ServiceEvent.EventInput.STATE, state);
            notifyObservers(event);
        }

        @Override
        public void incomingTrustRequest(String accountId, String from, Blob message, SWIGTYPE_p_time_t received) {
            Log.d(TAG, "incomingTrustRequest: " + accountId + ", " + from + ", " + message + ", " + received);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.INCOMING_TRUST_REQUEST);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.FROM, from);
            event.addEventInput(ServiceEvent.EventInput.MESSAGE, message);
            event.addEventInput(ServiceEvent.EventInput.TIME, received);
            notifyObservers(event);
        }
    }
}