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

import javax.inject.Inject;

import cx.ring.daemon.Blob;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.model.Codec;
import cx.ring.utils.Log;
import cx.ring.utils.SwigNativeConverter;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public abstract class DaemonService {

    private static final String TAG = DaemonService.class.getName();

    private static final int VCARD_CHUNK_SIZE = 1000;

    @Inject
    ExecutorService mExecutor;

    private boolean mIsPjSipStackStarted = false;

    public DaemonService() {
        loadNativeLibrary();
        mIsPjSipStackStarted = true;
    }

    public abstract void loadNativeLibrary();

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

    //@Override
    public String placeCall(final String account, final String number, final boolean video) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "DRingService.placeCall() thread running... " + number + " video: " + video);
                String callId = Ringservice.placeCall(account, number);
                if (!video) {
                    Ringservice.muteLocalMedia(callId, "MEDIA_TYPE_VIDEO", true);
                }
                return callId;
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public void refuse(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.refuse() thread running...");
                Ringservice.refuse(callID);
                Ringservice.hangUp(callID);
            }
        });
    }

    //@Override
    public void accept(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.accept() thread running...");
                Ringservice.accept(callID);
            }
        });
    }

    //@Override
    public void hangUp(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.hangUp() thread running...");
                Ringservice.hangUp(callID);
            }
        });
    }

    //@Override
    public void hold(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.hold() thread running...");
                Ringservice.hold(callID);
            }
        });
    }

    //@Override
    public void unhold(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.unhold() thread running...");
                Ringservice.unhold(callID);
            }
        });
    }

    //@Override
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

    //@Override
    public boolean isStarted() {
        return mIsPjSipStackStarted;
    }

    //@Override
    public Map<String, String> getCallDetails(final String callID) {

        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.getCallDetails() thread running...");
                return Ringservice.getCallDetails(callID).toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public void setAudioPlugin(final String audioPlugin) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setAudioPlugin() thread running...");
                Ringservice.setAudioPlugin(audioPlugin);
            }
        });
    }

    //@Override
    public String getCurrentAudioOutputPlugin() {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "DRingService.getCurrentAudioOutputPlugin() thread running...");
                return Ringservice.getCurrentAudioOutputPlugin();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public List<String> getAccountList() {

        Future<List<String>> result = mExecutor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                Log.i(TAG, "DRingService.getAccountList() thread running...");
                return new ArrayList<>(Ringservice.getAccountList());
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public void setAccountOrder(final String order) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setAccountsOrder() " + order + " thread running...");
                Ringservice.setAccountsOrder(order);
            }
        });
    }

    //@Override
    public Map<String, String> getAccountDetails(final String accountID) {

        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.getAccountDetails() thread running...");
                return Ringservice.getAccountDetails(accountID).toNative();
            }
        });

        return getFutureResult(result);
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    //@Override
    public void setAccountDetails(final String accountId, final Map map) {
        Log.i(TAG, "DRingService.setAccountDetails() " + map.get("Account.hostname"));
        final StringMap swigmap = StringMap.toSwig(map);

        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Ringservice.setAccountDetails(accountId, swigmap);
                Log.i(TAG, "DRingService.setAccountDetails() thread running... " + swigmap.get("Account.hostname"));
            }

        });
    }

    //@Override
    public void setAccountActive(final String accountId, final boolean active) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setAccountActive() thread running... " + accountId + " -> " + active);
                Ringservice.setAccountActive(accountId, active);
            }
        });
    }

    //@Override
    public void setAccountsActive(final boolean active) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setAccountsActive() thread running... " + active);
                StringVect list = Ringservice.getAccountList();
                for (int i = 0, n = list.size(); i < n; i++)
                    Ringservice.setAccountActive(list.get(i), active);
            }
        });
    }

    //@Override
    public Map<String, String> getVolatileAccountDetails(final String accountId) {

        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.getVolatileAccountDetails() thread running...");
                return Ringservice.getVolatileAccountDetails(accountId).toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public Map<String, String> getAccountTemplate(final String accountType) {
        Log.i(TAG, "DRingService.getAccountTemplate() " + accountType);
        return Ringservice.getAccountTemplate(accountType).toNative();
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    //@Override
    public String addAccount(final Map map) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "DRingService.addAccount() thread running...");
                return Ringservice.addAccount(StringMap.toSwig(map));
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public void removeAccount(final String accountId) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.removeAccount() thread running...");
                Ringservice.removeAccount(accountId);
            }
        });
    }

    //@Override
    public String exportOnRing(final String accountId, final String password) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "DRingService.addRingDevice() thread running...");
                return Ringservice.exportOnRing(accountId, password);
            }
        });

        return getFutureResult(result);
    }

    public Map<String, String> getKnownRingDevices(final String accountId) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.getKnownRingDevices() thread running...");
                return Ringservice.getKnownRingDevices(accountId).toNative();
            }
        });

        return getFutureResult(result);
    }

    /*************************
     * Transfer related API
     *************************/

    //@Override
    public void transfer(final String callID, final String to) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.transfer() thread running...");
                if (Ringservice.transfer(callID, to)) {
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

    //@Override
    public void attendedTransfer(final String transferID, final String targetID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.attendedTransfer() thread running...");
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

    //@Override
    public void removeConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.createConference() thread running...");
                Ringservice.removeConference(confID);
            }
        });
    }

    //@Override
    public void joinParticipant(final String selCallID, final String dragCallID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.joinParticipant() thread running...");
                Ringservice.joinParticipant(selCallID, dragCallID);
                // Generate a CONF_CREATED callback
            }
        });
    }

    //@Override
    public void addParticipant(final String callID, final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.addParticipant() thread running...");
                Ringservice.addParticipant(callID, confID);
            }
        });
    }

    //@Override
    public void addMainParticipant(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.addMainParticipant() thread running...");
                Ringservice.addMainParticipant(confID);
            }
        });

    }

    //@Override
    public void detachParticipant(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.detachParticipant() thread running... " + callID);
                Ringservice.detachParticipant(callID);
            }
        });
    }

    //@Override
    public void joinConference(final String selConfID, final String dragConfID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.joinConference() thread running...");
                Ringservice.joinConference(selConfID, dragConfID);
            }
        });
    }

    //@Override
    public void hangUpConference(final String confID) {
        Log.e(TAG, "HANGING UP CONF");
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.hangUpConference() thread running...");
                Ringservice.hangUpConference(confID);
            }
        });
    }

    //@Override
    public void holdConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.holdConference() thread running...");
                Ringservice.holdConference(confID);
            }
        });
    }

    //@Override
    public void unholdConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.unholdConference() thread running...");
                Ringservice.unholdConference(confID);
            }
        });
    }

    //@Override
    public boolean isConferenceParticipant(final String callID) {
        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.i(TAG, "DRingService.isRecording() thread running...");
                return Ringservice.isConferenceParticipant(callID);
            }
        });

        return getFutureResult(result);
    }

    //@Override
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

    //@Override
    public List<String> getParticipantList(final String confID) {

        Future<List<String>> result = mExecutor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                Log.i(TAG, "DRingService.getParticipantList() thread running...");
                return new ArrayList<>(Ringservice.getParticipantList(confID));
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public String getConferenceId(String callID) {
        Log.e(TAG, "getConferenceId not implemented");
        return Ringservice.getConferenceId(callID);
    }

    //@Override
    public String getConferenceDetails(final String callID) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "DRingService.getConferenceDetails() thread running...");
                return Ringservice.getConferenceDetails(callID).get("CONF_STATE");
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public String getRecordPath() {
        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "DRingService.getRecordPath() thread running...");
                return Ringservice.getRecordPath();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public boolean toggleRecordingCall(final String id) {

        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.i(TAG, "DRingService.toggleRecordingCall() thread running...");
                return Ringservice.toggleRecording(id);
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public boolean startRecordedFilePlayback(final String filepath) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setRecordingCall() thread running...");
                Ringservice.startRecordedFilePlayback(filepath);
            }
        });
        return false;
    }

    //@Override
    public void stopRecordedFilePlayback(final String filepath) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.stopRecordedFilePlayback() thread running...");
                Ringservice.stopRecordedFilePlayback(filepath);
            }
        });
    }

    //@Override
    public void setRecordPath(final String path) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setRecordPath() " + path + " thread running...");
                Ringservice.setRecordPath(path);
            }
        });
    }

    //@Override
    public void sendTextMessage(final String callID, final String msg) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.sendTextMessage() thread running...");
                StringMap messages = new StringMap();
                messages.setRaw("text/plain", Blob.fromString(msg));
                Ringservice.sendTextMessage(callID, messages, "", false);
            }
        });
    }

    //@Override
    public long sendAccountTextMessage(final String accountID, final String to, final String msg) {
        Future<Long> result = mExecutor.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                Log.i(TAG, "DRingService.sendAccountTextMessage() thread running... " + accountID + " " + to + " " + msg);
                StringMap msgs = new StringMap();
                msgs.setRaw("text/plain", Blob.fromString(msg));
                return Ringservice.sendAccountTextMessage(accountID, to, msgs);
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public List<Codec> getCodecList(final String accountID) {
        Future<List<Codec>> result = mExecutor.submit(new Callable<List<Codec>>() {
            @Override
            public List<Codec> call() throws Exception {
                Log.i(TAG, "DRingService.getCodecList() thread running...");
                ArrayList<Codec> results = new ArrayList<>();

                UintVect activePayloads = Ringservice.getActiveCodecList(accountID);
                for (int i = 0; i < activePayloads.size(); ++i) {
                    Log.i(TAG, "DRingService.getCodecDetails(" + accountID + ", " + activePayloads.get(i) + ")");
                    StringMap codecsDetails = Ringservice.getCodecDetails(accountID, activePayloads.get(i));
                    results.add(new Codec(activePayloads.get(i), codecsDetails.toNative(), true));
                }
                UintVect payloads = Ringservice.getCodecList();

                cl:
                for (int i = 0; i < payloads.size(); ++i) {
                    for (Codec co : results)
                        if (co.getPayload() == payloads.get(i))
                            continue cl;
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

    //@Override
    public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.validateCertificatePath() thread running...");
                return Ringservice.validateCertificatePath(accountID, certificatePath, privateKeyPath, "", "").toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public Map<String, String> validateCertificate(final String accountID, final String certificate) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.validateCertificate() thread running...");
                return Ringservice.validateCertificate(accountID, certificate).toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public Map<String, String> getCertificateDetailsPath(final String certificatePath) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.getCertificateDetailsPath() thread running...");
                return Ringservice.getCertificateDetails(certificatePath).toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public Map<String, String> getCertificateDetails(final String certificateRaw) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.getCertificateDetails() thread running...");
                return Ringservice.getCertificateDetails(certificateRaw).toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public void setActiveCodecList(final List codecs, final String accountID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setActiveCodecList() thread running...");
                UintVect list = new UintVect(codecs.size());
                for (Object codec : codecs)
                    list.add((Long) codec);
                Ringservice.setActiveCodecList(accountID, list);
            }
        });
    }

    //@Override
    public void playDtmf(final String key) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.playDtmf() thread running...");
                Ringservice.playDTMF(key);
            }
        });
    }

    //@Override
    public Map<String, String> getConference(final String id) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "DRingService.getCredentials() thread running...");
                return Ringservice.getConferenceDetails(id).toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public void setMuted(final boolean mute) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setMuted() thread running...");
                Ringservice.muteCapture(mute);
            }
        });
    }

    //@Override
    public boolean isCaptureMuted() {
        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.i(TAG, "DRingService.isCaptureMuted() thread running...");
                return Ringservice.isCaptureMuted();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public List<String> getTlsSupportedMethods() {
        Log.i(TAG, "DRingService.getTlsSupportedMethods()");
        return SwigNativeConverter.convertSwigToNative(Ringservice.getSupportedTlsMethod());
    }

    //@Override
    public List<Map<String, String>> getCredentials(final String accountID) {

        Future<List<Map<String, String>>> result = mExecutor.submit(new Callable<List<Map<String, String>>>() {
            @Override
            public List<Map<String, String>> call() throws Exception {
                Log.i(TAG, "DRingService.getCredentials() thread running...");
                return Ringservice.getCredentials(accountID).toNative();
            }
        });

        return getFutureResult(result);
    }

    //@Override
    public void setCredentials(final String accountID, final List creds) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.setCredentials() thread running...");
                Ringservice.setCredentials(accountID, SwigNativeConverter.convertFromNativeToSwig(creds));
            }
        });
    }

    //@Override
    public void registerAllAccounts() {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "DRingService.registerAllAccounts() thread running...");
                Ringservice.registerAllAccounts();
            }
        });
    }
}
