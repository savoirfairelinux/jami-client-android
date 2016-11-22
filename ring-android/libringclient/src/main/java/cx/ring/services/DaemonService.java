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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cx.ring.daemon.Callback;
import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.daemon.VideoCallback;
import cx.ring.model.Codec;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.SwigNativeConverter;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public abstract class DaemonService {

    private static final String TAG = DaemonService.class.getName();

    private static final int VCARD_CHUNK_SIZE = 1000;
    private static final int POLLING_TIMEOUT = 50;

    @Inject
    ExecutorService mExecutor;

    @Inject
    ScheduledExecutorService mScheduledExecutor;

    private boolean mDaemonStarted = false;

    public abstract void loadNativeLibrary();

    public abstract File provideFilesDir();

    public abstract String provideDefaultVCardName();

    public DaemonService() {
    }

    public void startDaemon(final CallService.CallCallbackHandler callManagerCallback,
                            final ConferenceService.ConferenceCallbackHandler confManagerCallback,
                            final ConfigurationCallback configurationManagerCallback,
                            final VideoCallback videoManagerCallBack) {

        if (!mDaemonStarted) {
            Log.i(TAG, "Starting daemon ...");
            Callback callback = new DaemonCallback(callManagerCallback, confManagerCallback);
            Ringservice.init(configurationManagerCallback, callback, videoManagerCallBack);
            startRingServicePolling();
            mDaemonStarted = true;
            Log.i(TAG, "DaemonService started");
        }
    }

    private void startRingServicePolling() {
        mScheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                mExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        Ringservice.pollEvents();
                    }
                });
            }
        }, 0, POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void stopDaemon() {
        mScheduledExecutor.shutdown();

        if (mDaemonStarted) {
            Log.i(TAG, "stopping daemon ...");
            Ringservice.fini();
            mDaemonStarted = false;
            Log.i(TAG, "DaemonService stopped");
        }
    }

    public boolean isStarted() {
        return mDaemonStarted;
    }

    public void sendProfile(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final String ringProfileVCardMime = "x-ring/ring.profile.vcard";

                VCard vcard = VCardUtils.loadLocalProfileFromDisk(provideFilesDir(), provideDefaultVCardName());
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
                    Ringservice.sendTextMessage(callID, StringMap.toSwig(chunk), "Me", false);
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

    public Map<String, String> getAccountDetails(final String accountID) {

        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getAccountDetails() thread running...");
                return Ringservice.getAccountDetails(accountID).toNative();
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

    public List<Codec> getCodecList(final String accountID) {
        Future<List<Codec>> result = mExecutor.submit(new Callable<List<Codec>>() {
            @Override
            public List<Codec> call() throws Exception {
                Log.i(TAG, "getCodecList() thread running...");
                ArrayList<Codec> results = new ArrayList<>();

                UintVect activePayloads = Ringservice.getActiveCodecList(accountID);
                for (int i = 0; i < activePayloads.size(); ++i) {
                    Log.i(TAG, "getCodecDetails(" + accountID + ", " + activePayloads.get(i) + ")");
                    StringMap codecsDetails = Ringservice.getCodecDetails(accountID, activePayloads.get(i));
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
                    StringMap details = Ringservice.getCodecDetails(accountID, payloads.get(i));
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

    public Map<String, String> validateCertificate(final String accountID, final String certificate) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "validateCertificate() thread running...");
                return Ringservice.validateCertificate(accountID, certificate).toNative();
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

    public void setActiveCodecList(final List codecs, final String accountID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setActiveCodecList() thread running...");
                UintVect list = new UintVect(codecs.size());
                for (Object codec : codecs) {
                    list.add((Long) codec);
                }
                Ringservice.setActiveCodecList(accountID, list);
            }
        });
    }

    public List<String> getTlsSupportedMethods() {
        Log.i(TAG, "getTlsSupportedMethods()");
        return SwigNativeConverter.convertSwigToNative(Ringservice.getSupportedTlsMethod());
    }

    public List<Map<String, String>> getCredentials(final String accountID) {

        Future<List<Map<String, String>>> result = mExecutor.submit(new Callable<List<Map<String, String>>>() {
            @Override
            public List<Map<String, String>> call() throws Exception {
                Log.i(TAG, "getCredentials() thread running...");
                return Ringservice.getCredentials(accountID).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public void setCredentials(final String accountID, final List creds) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setCredentials() thread running...");
                Ringservice.setCredentials(accountID, SwigNativeConverter.convertFromNativeToSwig(creds));
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

    public int backupAccounts(final List accountIDs, final String toDir, final String password) {
        Future<Integer> result = mExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                StringVect ids = new StringVect();
                for (Object s : accountIDs) {
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

    public void connectivityChanged() {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Ringservice.connectivityChanged();
            }
        });
    }

    public void switchInput(final String id, final String uri, final StringMap map) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "switchInput() thread running..." + uri);
                Ringservice.applySettings(id, map);
                Ringservice.switchInput(id, uri);
            }
        });
    }

    public void setPreviewSettings(final Map<String, StringMap> cameraMaps) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "applySettings() thread running...");
                for (Map.Entry<String, StringMap> entry : cameraMaps.entrySet()) {
                    Ringservice.applySettings(entry.getKey(), entry.getValue());
                }
            }
        });
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

    public long startVideo(final String inputId, Object surface, int width, int height) {
        long inputWindow = RingserviceJNI.acquireNativeWindow(surface);
        if (inputWindow == 0) {
            return inputWindow;
        }

        RingserviceJNI.setNativeWindowGeometry(inputWindow, width, height);
        RingserviceJNI.registerVideoCallback(inputId, inputWindow);

        return inputWindow;
    }

    public void stopVideo(final String inputId, long inputWindow) {
        RingserviceJNI.unregisterVideoCallback(inputId, inputWindow);
        RingserviceJNI.releaseNativeWindow(inputWindow);
    }

    public void setVideoFrame(byte[] data, int width, int height, int rotation) {
        long frame = RingserviceJNI.obtainFrame(data.length);
        if (frame != 0) {
            RingserviceJNI.setVideoFrame(data, data.length, frame, width, height, rotation);
        }
        RingserviceJNI.releaseFrame(frame);
    }

    public void addVideoDevice(String deviceId) {
        RingserviceJNI.addVideoDevice(deviceId);
    }

    public void setDefaultVideoDevice(String deviceId) {
        RingserviceJNI.setDefaultDevice(deviceId);
    }

    private class DaemonCallback extends Callback {

        private CallService.CallCallbackHandler mCallCallbackHandler;
        private ConferenceService.ConferenceCallbackHandler mConferenceCallbackHandler;

        DaemonCallback(CallService.CallCallbackHandler callCallbackHandler,
                       ConferenceService.ConferenceCallbackHandler conferenceCallbackHandler) {
            mCallCallbackHandler = callCallbackHandler;
            mConferenceCallbackHandler = conferenceCallbackHandler;
        }

        @Override
        public void callStateChanged(String callId, String newState, int detailCode) {
            mCallCallbackHandler.callStateChanged(callId, newState, detailCode);
        }

        @Override
        public void incomingCall(String accountId, String callId, String from) {
            mCallCallbackHandler.incomingCall(accountId, callId, from);
        }

        @Override
        public void incomingMessage(String callId, String from, StringMap messages) {
            mCallCallbackHandler.incomingMessage(callId, from, messages);
        }

        @Override
        public void recordPlaybackFilepath(String id, String filename) {
            mCallCallbackHandler.recordPlaybackFilepath(id, filename);
        }

        @Override
        public void onRtcpReportReceived(String callId, IntegerMap stats) {
            mCallCallbackHandler.onRtcpReportReceived(callId, stats);
        }

        @Override
        public void conferenceCreated(final String confId) {
            mConferenceCallbackHandler.conferenceCreated(confId);
        }

        @Override
        public void conferenceRemoved(String confId) {
            mConferenceCallbackHandler.conferenceRemoved(confId);
        }

        @Override
        public void conferenceChanged(String confId, String state) {
            mConferenceCallbackHandler.conferenceChanged(confId, state);
        }
    }
}