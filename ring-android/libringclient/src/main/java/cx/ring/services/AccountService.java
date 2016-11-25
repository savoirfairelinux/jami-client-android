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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.model.Codec;
import cx.ring.model.DaemonEvent;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.SwigNativeConverter;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public class AccountService extends Observable {

    private final static String TAG = AccountService.class.getName();

    private static final int VCARD_CHUNK_SIZE = 1000;

    @Inject
    ExecutorService mExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private ConfigurationCallback mCallbackHandler;

    public AccountService() {
        mCallbackHandler = new ConfigurationCallbackHandler();
    }

    public ConfigurationCallback getCallbackHandler() {
        return mCallbackHandler;
    }

    public void sendProfile(final String callId, final String accountId) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final String ringProfileVCardMime = "x-ring/ring.profile.vcard";

                VCard vcard = VCardUtils.loadLocalProfileFromDisk(
                        mDeviceRuntimeService.provideFilesDir(),
                        accountId,
                        mDeviceRuntimeService.provideDefaultVCardName());
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

    public List<String> getAccountList() {

        Future<List<String>> result = mExecutor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                Log.i(TAG, "getAccountList() thread running...");
                return new ArrayList<>(Ringservice.getAccountList());
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public void setAccountOrder(final String order) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setAccountsOrder() " + order + " thread running...");
                Ringservice.setAccountsOrder(order);
            }
        });
    }

    public Map<String, String> getAccountDetails(final String accountId) {

        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getAccountDetails() thread running...");
                return Ringservice.getAccountDetails(accountId).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    public void setAccountDetails(final String accountId, final Map map) {
        Log.i(TAG, "setAccountDetails() " + map.get("Account.hostname"));
        final StringMap swigmap = StringMap.toSwig(map);

        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Ringservice.setAccountDetails(accountId, swigmap);
                Log.i(TAG, "setAccountDetails() thread running... " + swigmap.get("Account.hostname"));
            }

        });
    }

    public void setAccountActive(final String accountId, final boolean active) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setAccountActive() thread running... " + accountId + " -> " + active);
                Ringservice.setAccountActive(accountId, active);
            }
        });
    }

    public void setAccountsActive(final boolean active) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setAccountsActive() thread running... " + active);
                StringVect list = Ringservice.getAccountList();
                for (int i = 0, n = list.size(); i < n; i++) {
                    Ringservice.setAccountActive(list.get(i), active);
                }
            }
        });
    }

    public Map<String, String> getVolatileAccountDetails(final String accountId) {

        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getVolatileAccountDetails() thread running...");
                return Ringservice.getVolatileAccountDetails(accountId).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, String> getAccountTemplate(final String accountType) {
        Log.i(TAG, "getAccountTemplate() " + accountType);
        return Ringservice.getAccountTemplate(accountType).toNative();
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    public String addAccount(final Map map) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "addAccount() thread running...");
                return Ringservice.addAccount(StringMap.toSwig(map));
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public void removeAccount(final String accountId) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "removeAccount() thread running...");
                Ringservice.removeAccount(accountId);
            }
        });
    }

    public String exportOnRing(final String accountId, final String password) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "addRingDevice() thread running...");
                return Ringservice.exportOnRing(accountId, password);
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, String> getKnownRingDevices(final String accountId) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getKnownRingDevices() thread running...");
                return Ringservice.getKnownRingDevices(accountId).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public void setActiveCodecList(final List codecs, final String accountId) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setActiveCodecList() thread running...");
                UintVect list = new UintVect(codecs.size());
                for (Object codec : codecs) {
                    list.add((Long) codec);
                }
                Ringservice.setActiveCodecList(accountId, list);
            }
        });
    }

    public List<Codec> getCodecList(final String accountId) {
        Future<List<Codec>> result = mExecutor.submit(new Callable<List<Codec>>() {
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
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "validateCertificatePath() thread running...");
                return Ringservice.validateCertificatePath(accountID, certificatePath, privateKeyPath, "", "").toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, String> validateCertificate(final String accountId, final String certificate) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "validateCertificate() thread running...");
                return Ringservice.validateCertificate(accountId, certificate).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, String> getCertificateDetailsPath(final String certificatePath) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getCertificateDetailsPath() thread running...");
                return Ringservice.getCertificateDetails(certificatePath).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, String> getCertificateDetails(final String certificateRaw) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getCertificateDetails() thread running...");
                return Ringservice.getCertificateDetails(certificateRaw).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public List<String> getTlsSupportedMethods() {
        Log.i(TAG, "getTlsSupportedMethods()");
        return SwigNativeConverter.convertSwigToNative(Ringservice.getSupportedTlsMethod());
    }

    public List<Map<String, String>> getCredentials(final String accountId) {

        Future<List<Map<String, String>>> result = mExecutor.submit(new Callable<List<Map<String, String>>>() {
            @Override
            public List<Map<String, String>> call() throws Exception {
                Log.i(TAG, "getCredentials() thread running...");
                return Ringservice.getCredentials(accountId).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public void setCredentials(final String accountId, final List creds) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setCredentials() thread running...");
                Ringservice.setCredentials(accountId, SwigNativeConverter.convertFromNativeToSwig(creds));
            }
        });
    }

    public void registerAllAccounts() {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "registerAllAccounts() thread running...");
                Ringservice.registerAllAccounts();
            }
        });
    }

    public int backupAccounts(final List accountIds, final String toDir, final String password) {
        Future<Integer> result = mExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                StringVect ids = new StringVect();
                for (Object s : accountIds) {
                    ids.add((String) s);
                }
                return Ringservice.exportAccounts(ids, toDir, password);
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public int restoreAccounts(final String archivePath, final String password) {
        Future<Integer> result = mExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return Ringservice.importAccounts(archivePath, password);
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public void lookupName(final String account, final String nameserver, final String name) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "lookupName() thread running...");
                Ringservice.lookupName(account, nameserver, name);
            }
        });
    }

    public void lookupAddress(final String account, final String nameserver, final String address) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "lookupAddress() thread running...");
                Ringservice.lookupAddress(account, nameserver, address);
            }
        });
    }

    public void registerName(final String account, final String password, final String name) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "registerName() thread running...");
                Ringservice.registerName(account, password, name);
            }
        });
    }

    private class ConfigurationCallbackHandler extends ConfigurationCallback {

        @Override
        public void volumeChanged(String device, int value) {
            Log.d(TAG, "volume changed");
            super.volumeChanged(device, value);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.VOLUME_CHANGED);
            event.addEventInput(DaemonEvent.EventInput.DEVICE, device);
            event.addEventInput(DaemonEvent.EventInput.VALUE, value);
            notifyObservers(event);
        }

        @Override
        public void accountsChanged() {
            super.accountsChanged();
            Log.d(TAG, "accounts changed");

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.ACCOUNTS_CHANGED);
            notifyObservers(event);
        }

        @Override
        public void stunStatusFailure(String accountId) {
            Log.d(TAG, "stun status failure: " + accountId);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.STUN_STATUS_FAILURE);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            notifyObservers(event);
        }

        @Override
        public void registrationStateChanged(String accountId, String state, int code, String detailString) {
            Log.d(TAG, "stun status registrationStateChanged: " + accountId + ", " + state + ", " + code + ", " + detailString);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.REGISTRATION_STATE_CHANGED);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.STATE, state);
            event.addEventInput(DaemonEvent.EventInput.DETAIL_CODE, code);
            event.addEventInput(DaemonEvent.EventInput.DETAIL_STRING, detailString);
            notifyObservers(event);
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
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.INCOMING_ACCOUNT_MESSAGE);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.FROM, from);
            event.addEventInput(DaemonEvent.EventInput.MESSAGES, msg);
            notifyObservers(event);
        }

        @Override
        public void accountMessageStatusChanged(String accountId, long messageId, String to, int status) {
            Log.d(TAG, "accountMessageStatusChanged: " + accountId + ", " + messageId + ", " + to + ", " + status);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.ACCOUNT_MESSAGE_STATUS_CHANGED);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.MESSAGE_ID, messageId);
            event.addEventInput(DaemonEvent.EventInput.TO, to);
            event.addEventInput(DaemonEvent.EventInput.STATE, status);
            notifyObservers(event);
        }

        @Override
        public void errorAlert(int alert) {
            Log.d(TAG, "errorAlert : " + alert);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.ERROR_ALERT);
            event.addEventInput(DaemonEvent.EventInput.ALERT, alert);
            notifyObservers(event);
        }

        @Override
        public void getHardwareAudioFormat(IntVect ret) {
            Log.d(TAG, "getHardwareAudioFormat: " + ret.toString());

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.GET_HARDWARE_AUDIO_FORMAT);
            event.addEventInput(DaemonEvent.EventInput.AUDIO_FORMATS, ret);
            notifyObservers(event);
        }

        @Override
        public void getAppDataPath(String name, StringVect ret) {
            Log.d(TAG, "getAppDataPath: " + name + ", " + ret);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.GET_APP_DATA_PATH);
            event.addEventInput(DaemonEvent.EventInput.NAME, name);
            event.addEventInput(DaemonEvent.EventInput.PATHS, ret);
            notifyObservers(event);
        }

        @Override
        public void knownDevicesChanged(String accountId, StringMap devices) {
            Log.d(TAG, "knownDevicesChanged: " + accountId + ", " + devices);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.KNOWN_DEVICES_CHANGED);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.DEVICES, devices);
            notifyObservers(event);
        }

        @Override
        public void exportOnRingEnded(String accountId, int code, String pin) {
            Log.d(TAG, "exportOnRingEnded: " + accountId + ", " + code + ", " + pin);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.EXPORT_ON_RING_ENDED);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.CODE, code);
            event.addEventInput(DaemonEvent.EventInput.PIN, pin);
            notifyObservers(event);
        }

        @Override
        public void nameRegistrationEnded(String accountId, int state, String name) {
            Log.d(TAG, "nameRegistrationEnded: " + accountId + ", " + state + ", " + name);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.NAME_REGISTRATION_ENDED);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.STATE, state);
            event.addEventInput(DaemonEvent.EventInput.NAME, name);
            notifyObservers(event);
        }

        @Override
        public void registeredNameFound(String accountId, int state, String address, String name) {
            Log.d(TAG, "registeredNameFound: " + accountId + ", " + state + ", " + name + ", " + address);

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.REGISTERED_NAME_FOUND);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.STATE, state);
            event.addEventInput(DaemonEvent.EventInput.ADDRESS, address);
            event.addEventInput(DaemonEvent.EventInput.NAME, name);
            notifyObservers(event);
        }
    }
}