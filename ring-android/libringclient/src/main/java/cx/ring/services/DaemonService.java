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

import cx.ring.daemon.Blob;
import cx.ring.daemon.Callback;
import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.daemon.VideoCallback;
import cx.ring.model.Codec;
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

    public DaemonService() {
        try {
            loadNativeLibrary();
        } catch (Exception e) {
            Log.e(TAG, "Problem with the current Pj stack...", e);
        }
    }

    public void startDaemon(final Callback callManagerCallback,
                            final ConfigurationCallback configurationManagerCallback,
                            final VideoCallback videoManagerCallBack) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Starting daemon ...");
                if (!mDaemonStarted) {
                    startRingServicePolling();
                    Ringservice.init(configurationManagerCallback, callManagerCallback, videoManagerCallBack);
                    mDaemonStarted = true;
                    Log.i(TAG, "DaemonService started");
                }
            }
        });
    }

    private void startRingServicePolling() {
        mScheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Ringservice.pollEvents();
            }
        }, 0, POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void stopDaemon() {
        mScheduledExecutor.shutdown();

        if (mDaemonStarted) {
            mExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "stopping daemon ...");
                    Ringservice.fini();
                    mDaemonStarted = false;
                    Log.i(TAG, "DaemonService stopped");
                }
            });
        }
    }

    public abstract void loadNativeLibrary() throws Exception;

    public abstract File provideFilesDir();

    public abstract String provideDefaultVCardName();

    private <T> T getFutureResult(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            Log.e(TAG, "Error while getting Future value", e);
        }

        return null;
    }

    public String placeCall(final String account, final String number, final boolean video) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "placeCall() thread running... " + number + " video: " + video);
                String callId = Ringservice.placeCall(account, number);
                if (!video) {
                    Ringservice.muteLocalMedia(callId, "MEDIA_TYPE_VIDEO", true);
                }
                return callId;
            }
        });

        return getFutureResult(result);
    }

    public void refuse(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "refuse() thread running...");
                Ringservice.refuse(callID);
                Ringservice.hangUp(callID);
            }
        });
    }

    public void accept(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "accept() thread running...");
                Ringservice.accept(callID);
            }
        });
    }

    public void hangUp(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "hangUp() thread running...");
                Ringservice.hangUp(callID);
            }
        });
    }

    public void hold(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "hold() thread running...");
                Ringservice.hold(callID);
            }
        });
    }

    public void unhold(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "unhold() thread running...");
                Ringservice.unhold(callID);
            }
        });
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

    public boolean isStarted() {
        return mDaemonStarted;
    }

    public Map<String, String> getCallDetails(final String callID) {

        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getCallDetails() thread running...");
                return Ringservice.getCallDetails(callID).toNative();
            }
        });

        return getFutureResult(result);
    }

    public void muteRingTone(boolean mute) {
        Log.d(TAG, (mute ? "Muting." : "Unmuting.") + " ringtone.");
        Ringservice.muteRingtone(mute);
    }

    public void setAudioPlugin(final String audioPlugin) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setAudioPlugin() thread running...");
                Ringservice.setAudioPlugin(audioPlugin);
            }
        });
    }

    public String getCurrentAudioOutputPlugin() {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "getCurrentAudioOutputPlugin() thread running...");
                return Ringservice.getCurrentAudioOutputPlugin();
            }
        });

        return getFutureResult(result);
    }

    public List<String> getAccountList() {

        Future<List<String>> result = mExecutor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                Log.i(TAG, "getAccountList() thread running...");
                return new ArrayList<>(Ringservice.getAccountList());
            }
        });

        return getFutureResult(result);
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

        return getFutureResult(result);
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

        return getFutureResult(result);
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

        return getFutureResult(result);
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

        return getFutureResult(result);
    }

    public Map<String, String> getKnownRingDevices(final String accountId) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getKnownRingDevices() thread running...");
                return Ringservice.getKnownRingDevices(accountId).toNative();
            }
        });

        return getFutureResult(result);
    }

    /*************************
     * Transfer related API
     *************************/

    public void transfer(final String callID, final String to) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "transfer() thread running...");
                if (Ringservice.transfer(callID, to)) {
                    // todo conference is not a priority in Android client for now
                   /* Bundle bundle = new Bundle();
                    bundle.putString("CallID", callID);
                    bundle.putString("State", "HUNGUP");
                    Intent intent = new Intent(CallManagerCallBack.CALL_STATE_CHANGED);
                    intent.putExtra("com.savoirfairelinux.sflphone.service.newstate", bundle);
                    sendBroadcast(intent);*/
                } else {
                    Log.i(TAG, "NOT OK");
                }
            }
        });
    }

    public void attendedTransfer(final String transferID, final String targetID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "attendedTransfer() thread running...");
                if (Ringservice.attendedTransfer(transferID, targetID)) {
                    Log.i(TAG, "OK");
                } else {
                    Log.i(TAG, "NOT OK");
                }
            }
        });
    }

    /*************************
     * Conference related API
     *************************/

    public void removeConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "createConference() thread running...");
                Ringservice.removeConference(confID);
            }
        });
    }

    public void joinParticipant(final String selCallID, final String dragCallID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "joinParticipant() thread running...");
                Ringservice.joinParticipant(selCallID, dragCallID);
                // Generate a CONF_CREATED callback
            }
        });
    }

    public void addParticipant(final String callID, final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "addParticipant() thread running...");
                Ringservice.addParticipant(callID, confID);
            }
        });
    }

    public void addMainParticipant(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "addMainParticipant() thread running...");
                Ringservice.addMainParticipant(confID);
            }
        });

    }

    public void detachParticipant(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "detachParticipant() thread running... " + callID);
                Ringservice.detachParticipant(callID);
            }
        });
    }

    public void joinConference(final String selConfID, final String dragConfID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "joinConference() thread running...");
                Ringservice.joinConference(selConfID, dragConfID);
            }
        });
    }

    public void hangUpConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "hangUpConference() thread running...");
                Ringservice.hangUpConference(confID);
            }
        });
    }

    public void holdConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "holdConference() thread running...");
                Ringservice.holdConference(confID);
            }
        });
    }

    public void unholdConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "unholdConference() thread running...");
                Ringservice.unholdConference(confID);
            }
        });
    }

    public boolean isConferenceParticipant(final String callID) {
        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.i(TAG, "isConferenceParticipant() thread running...");
                return Ringservice.isConferenceParticipant(callID);
            }
        });

        return getFutureResult(result);
    }

    public Map<String, ArrayList<String>> getConferenceList() {

        Future<Map<String, ArrayList<String>>> result = mExecutor.submit(new Callable<Map<String, ArrayList<String>>>() {
            @Override
            public Map<String, ArrayList<String>> call() throws Exception {
                Log.i(TAG, "DRingService.getConferenceList() thread running...");
                StringVect callIds = Ringservice.getCallList();
                HashMap<String, ArrayList<String>> confs = new HashMap<>(callIds.size());
                for (int i = 0; i < callIds.size(); i++) {
                    String callId = callIds.get(i);
                    String confId = Ringservice.getConferenceId(callId);
                    if (confId == null || confId.isEmpty()) {
                        confId = callId;
                    }
                    ArrayList<String> calls = confs.get(confId);
                    if (calls == null) {
                        calls = new ArrayList<>();
                        confs.put(confId, calls);
                    }
                    calls.add(callId);
                }
                return confs;
            }
        });

        return getFutureResult(result);
    }

    public List<String> getParticipantList(final String confID) {

        Future<List<String>> result = mExecutor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                Log.i(TAG, "getParticipantList() thread running...");
                return new ArrayList<>(Ringservice.getParticipantList(confID));
            }
        });

        return getFutureResult(result);
    }

    public String getConferenceId(String callID) {
        return Ringservice.getConferenceId(callID);
    }

    public String getConferenceDetails(final String callID) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "getConferenceDetails() thread running...");
                return Ringservice.getConferenceDetails(callID).get("CONF_STATE");
            }
        });

        return getFutureResult(result);
    }

    public String getRecordPath() {
        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "getRecordPath() thread running...");
                return Ringservice.getRecordPath();
            }
        });

        return getFutureResult(result);
    }

    public boolean toggleRecordingCall(final String id) {

        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.i(TAG, "toggleRecordingCall() thread running...");
                return Ringservice.toggleRecording(id);
            }
        });

        return getFutureResult(result);
    }

    public boolean startRecordedFilePlayback(final String filepath) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setRecordingCall() thread running...");
                Ringservice.startRecordedFilePlayback(filepath);
            }
        });
        return false;
    }

    public void stopRecordedFilePlayback(final String filepath) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "stopRecordedFilePlayback() thread running...");
                Ringservice.stopRecordedFilePlayback(filepath);
            }
        });
    }

    public void setRecordPath(final String path) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setRecordPath() " + path + " thread running...");
                Ringservice.setRecordPath(path);
            }
        });
    }

    public void sendTextMessage(final String callID, final String msg) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DsendTextMessage() thread running...");
                StringMap messages = new StringMap();
                messages.setRaw("text/plain", Blob.fromString(msg));
                Ringservice.sendTextMessage(callID, messages, "", false);
            }
        });
    }

    public long sendAccountTextMessage(final String accountID, final String to, final String msg) {
        Future<Long> result = mExecutor.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                Log.i(TAG, "sendAccountTextMessage() thread running... " + accountID + " " + to + " " + msg);
                StringMap msgs = new StringMap();
                msgs.setRaw("text/plain", Blob.fromString(msg));
                return Ringservice.sendAccountTextMessage(accountID, to, msgs);
            }
        });

        return getFutureResult(result);
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

        return getFutureResult(result);
    }

    public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "validateCertificatePath() thread running...");
                return Ringservice.validateCertificatePath(accountID, certificatePath, privateKeyPath, "", "").toNative();
            }
        });

        return getFutureResult(result);
    }

    public Map<String, String> validateCertificate(final String accountID, final String certificate) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "validateCertificate() thread running...");
                return Ringservice.validateCertificate(accountID, certificate).toNative();
            }
        });

        return getFutureResult(result);
    }

    public Map<String, String> getCertificateDetailsPath(final String certificatePath) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getCertificateDetailsPath() thread running...");
                return Ringservice.getCertificateDetails(certificatePath).toNative();
            }
        });

        return getFutureResult(result);
    }

    public Map<String, String> getCertificateDetails(final String certificateRaw) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getCertificateDetails() thread running...");
                return Ringservice.getCertificateDetails(certificateRaw).toNative();
            }
        });

        return getFutureResult(result);
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

    public void playDtmf(final String key) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "playDtmf() thread running...");
                Ringservice.playDTMF(key);
            }
        });
    }

    public Map<String, String> getConference(final String id) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getCredentials() thread running...");
                return Ringservice.getConferenceDetails(id).toNative();
            }
        });

        return getFutureResult(result);
    }

    public void setMuted(final boolean mute) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "setMuted() thread running...");
                Ringservice.muteCapture(mute);
            }
        });
    }

    public boolean isCaptureMuted() {
        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.i(TAG, "isCaptureMuted() thread running...");
                return Ringservice.isCaptureMuted();
            }
        });

        return getFutureResult(result);
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

        return getFutureResult(result);
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

        return getFutureResult(result);
    }

    public int restoreAccounts(final String archivePath, final String password) {
        Future<Integer> result = mExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return Ringservice.importAccounts(archivePath, password);
            }
        });

        return getFutureResult(result);
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
}
