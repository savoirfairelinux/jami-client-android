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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.model.CallContact;
import cx.ring.model.DaemonEvent;
import cx.ring.model.Uri;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;

public class CallService extends Observable {

    private final static String TAG = CallService.class.getName();

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private Map<String, CallContact> mContacts;
    private CallbackHandler mCallbackHandler;

    public CallService() {
        mCallbackHandler = new CallbackHandler();
        mContacts = new HashMap<>();
    }

    public CallbackHandler getCallbackHandler() {
        return mCallbackHandler;
    }

    public void addContact(CallContact contact) {
        if (contact == null
                || contact.getPhones().isEmpty()
                || contact.getPhones().get(0) == null
                || contact.getPhones().get(0).getNumber() == null) {
            return;
        }
        mContacts.put(contact.getPhones().get(0).getNumber().toString(), contact);
    }

    public CallContact getContact(Uri uri) {
        return mContacts.get(uri.toString());
    }

    public String placeCall(final String account, final String number, final boolean video) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Log.i(TAG, "placeCall() thread running... " + number + " video: " + video);
                        String callId = Ringservice.placeCall(account, number);
                        if (!video) {
                            Ringservice.muteLocalMedia(callId, "MEDIA_TYPE_VIDEO", true);
                        }
                        return callId;
                    }
                }
        );
    }

    public void refuse(final String callID) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "refuse() thread running...");
                        Ringservice.refuse(callID);
                        Ringservice.hangUp(callID);
                        return true;
                    }
                }
        );
    }

    public void accept(final String callID) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "accept() thread running...");
                        Ringservice.accept(callID);
                        return true;
                    }
                }
        );
    }

    public void hangUp(final String callID) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "hangUp() thread running...");
                        Ringservice.hangUp(callID);
                        return true;
                    }
                }
        );
    }

    public void hold(final String callID) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "hold() thread running...");
                        Ringservice.hold(callID);
                        return true;
                    }
                }
        );
    }

    public void unhold(final String callID) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "unhold() thread running...");
                        Ringservice.unhold(callID);
                        return true;
                    }
                }
        );
    }

    public Map<String, String> getCallDetails(final String callID) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getCallDetails() thread running...");
                        return Ringservice.getCallDetails(callID).toNative();
                    }
                }
        );
    }

    public void muteRingTone(boolean mute) {
        Log.d(TAG, (mute ? "Muting." : "Unmuting.") + " ringtone.");
        Ringservice.muteRingtone(mute);
    }

    public void setAudioPlugin(final String audioPlugin) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setAudioPlugin() thread running...");
                        Ringservice.setAudioPlugin(audioPlugin);
                        return true;
                    }
                }
        );
    }

    public String getCurrentAudioOutputPlugin() {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Log.i(TAG, "getCurrentAudioOutputPlugin() thread running...");
                        return Ringservice.getCurrentAudioOutputPlugin();
                    }
                }
        );
    }

    public void playDtmf(final String key) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "playDtmf() thread running...");
                        Ringservice.playDTMF(key);
                        return true;
                    }
                }
        );
    }

    public void setMuted(final boolean mute) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setMuted() thread running...");
                        Ringservice.muteCapture(mute);
                        return true;
                    }
                }
        );
    }

    @SuppressWarnings("ConstantConditions")
    public boolean isCaptureMuted() {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "isCaptureMuted() thread running...");
                        return Ringservice.isCaptureMuted();
                    }
                }
        );
    }

    public void transfer(final String callID, final String to) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "transfer() thread running...");
                        if (Ringservice.transfer(callID, to)) {
                            Log.i(TAG, "OK");
                        } else {
                            Log.i(TAG, "NOT OK");
                        }
                        return true;
                    }
                }
        );
    }

    public void attendedTransfer(final String transferID, final String targetID) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "attendedTransfer() thread running...");
                        if (Ringservice.attendedTransfer(transferID, targetID)) {
                            Log.i(TAG, "OK");
                        } else {
                            Log.i(TAG, "NOT OK");
                        }
                        return true;
                    }
                }
        );
    }

    public String getRecordPath() {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Log.i(TAG, "getRecordPath() thread running...");
                        return Ringservice.getRecordPath();
                    }
                }
        );
    }

    @SuppressWarnings("ConstantConditions")
    public boolean toggleRecordingCall(final String id) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "toggleRecordingCall() thread running...");
                        return Ringservice.toggleRecording(id);
                    }
                }
        );
    }

    public boolean startRecordedFilePlayback(final String filepath) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setRecordingCall() thread running...");
                        Ringservice.startRecordedFilePlayback(filepath);
                        return true;
                    }
                }
        );
        return false;
    }

    public void stopRecordedFilePlayback(final String filepath) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "stopRecordedFilePlayback() thread running...");
                        Ringservice.stopRecordedFilePlayback(filepath);
                        return true;
                    }
                }
        );
    }

    public void setRecordPath(final String path) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "setRecordPath() " + path + " thread running...");
                        Ringservice.setRecordPath(path);
                        return true;
                    }
                }
        );
    }

    public void sendTextMessage(final String callID, final String msg) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "DsendTextMessage() thread running...");
                        StringMap messages = new StringMap();
                        messages.setRaw("text/plain", Blob.fromString(msg));
                        Ringservice.sendTextMessage(callID, messages, "", false);
                        return true;
                    }
                }
        );
    }

    @SuppressWarnings("ConstantConditions")
    public long sendAccountTextMessage(final String accountID, final String to, final String msg) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        Log.i(TAG, "sendAccountTextMessage() thread running... " + accountID + " " + to + " " + msg);
                        StringMap msgs = new StringMap();
                        msgs.setRaw("text/plain", Blob.fromString(msg));
                        return Ringservice.sendAccountTextMessage(accountID, to, msgs);
                    }
                }
        );
    }

    class CallbackHandler {

        void callStateChanged(String callId, String newState, int detailCode) {
            Log.d(TAG, "call state changed: " + callId + ", " + newState + ", " + detailCode);

            // it is thread safe: executed in the same daemon thread than the callback
            Map<String, String> callDetails = Ringservice.getCallDetails(callId).toNative();

            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.CALL_STATE_CHANGED);
            event.addEventInput(DaemonEvent.EventInput.CALL_ID, callId);
            event.addEventInput(DaemonEvent.EventInput.STATE, newState);
            event.addEventInput(DaemonEvent.EventInput.DETAILS, callDetails);
            event.addEventInput(DaemonEvent.EventInput.DETAIL_CODE, detailCode);
            notifyObservers(event);
        }

        void incomingCall(String accountId, String callId, String from) {
            Log.d(TAG, "incoming call: " + accountId + ", " + callId + ", " + from);
            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.INCOMING_CALL);
            event.addEventInput(DaemonEvent.EventInput.CALL_ID, callId);
            event.addEventInput(DaemonEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(DaemonEvent.EventInput.FROM, from);
            notifyObservers(event);
        }

        void incomingMessage(String callId, String from, StringMap messages) {
            Log.d(TAG, "incoming message: " + callId + ", " + from);
            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.INCOMING_MESSAGE);
            event.addEventInput(DaemonEvent.EventInput.CALL_ID, callId);
            event.addEventInput(DaemonEvent.EventInput.FROM, from);
            event.addEventInput(DaemonEvent.EventInput.MESSAGES, messages);
            notifyObservers(event);
        }

        void recordPlaybackFilepath(String id, String filename) {
            Log.d(TAG, "record playback filepath: " + id + ", " + filename);
            // todo needs more explainations on that
        }

        void onRtcpReportReceived(String callId, IntegerMap stats) {
            Log.i(TAG, "on RTCP report received: " + callId);
            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.CONFERENCE_CHANGED);
            event.addEventInput(DaemonEvent.EventInput.CALL_ID, callId);
            event.addEventInput(DaemonEvent.EventInput.STATS, stats);
            notifyObservers(event);
        }
    }
}