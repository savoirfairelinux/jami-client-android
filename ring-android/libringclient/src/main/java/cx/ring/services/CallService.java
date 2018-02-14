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
import cx.ring.daemon.DataTransferInfo;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.model.CallContact;
import cx.ring.model.DataTransferError;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;

public class CallService extends Observable {

    private final static String TAG = CallService.class.getSimpleName();
    private final static String MIME_TEXT_PLAIN = "text/plain";

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    ContactService mContactService;

    @Inject
    HistoryService mHistoryService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private Map<String, SipCall> currentCalls = new HashMap<>();

    public SipCall placeCall(final String account, final String number, final boolean audioOnly) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<SipCall>() {
                    @Override
                    public SipCall call() throws Exception {
                        Log.i(TAG, "placeCall() thread running... " + number + " audioOnly: " + audioOnly);

                        HashMap<String, String> volatileDetails = new HashMap<>();
                        volatileDetails.put(SipCall.KEY_AUDIO_ONLY, String.valueOf(audioOnly));

                        String callId = Ringservice.placeCall(account, number, StringMap.toSwig(volatileDetails));
                        if (callId == null || callId.isEmpty())
                            return null;
                        if (audioOnly) {
                            Ringservice.muteLocalMedia(callId, "MEDIA_TYPE_VIDEO", true);
                        }
                        CallContact contact = mContactService.findContactByNumber(number);
                        SipCall call = addCall(account, callId, number, SipCall.Direction.OUTGOING);
                        call.muteVideo(audioOnly);
                        call.setContact(contact);
                        return call;
                    }
                }
        );
    }

    public void refuse(final String callId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "refuse() thread running...");
                        Ringservice.refuse(callId);
                        Ringservice.hangUp(callId);
                        return true;
                    }
                }
        );
    }

    public void accept(final String callId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "accept() thread running...");
                        Ringservice.accept(callId);
                        return true;
                    }
                }
        );
    }

    public void hangUp(final String callId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "hangUp() thread running...");
                        Ringservice.hangUp(callId);
                        return true;
                    }
                }
        );
    }

    public void hold(final String callId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "hold() thread running...");
                        Ringservice.hold(callId);
                        return true;
                    }
                }
        );
    }

    public void unhold(final String callId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "unhold() thread running...");
                        Ringservice.unhold(callId);
                        return true;
                    }
                }
        );
    }

    public Map<String, String> getCallDetails(final String callId) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getCallDetails() thread running...");
                        return Ringservice.getCallDetails(callId).toNative();
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

    public void transfer(final String callId, final String to) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "transfer() thread running...");
                        if (Ringservice.transfer(callId, to)) {
                            Log.i(TAG, "OK");
                        } else {
                            Log.i(TAG, "NOT OK");
                        }
                        return true;
                    }
                }
        );
    }

    public void attendedTransfer(final String transferId, final String targetID) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "attendedTransfer() thread running...");
                        if (Ringservice.attendedTransfer(transferId, targetID)) {
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

    public void stopRecordedFilePlayback() {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "stopRecordedFilePlayback() thread running...");
                        Ringservice.stopRecordedFilePlayback();
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

    public void sendTextMessage(final String callId, final String msg) {
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
                        Ringservice.sendTextMessage(callId, messages, "", false);
                        return true;
                    }
                }
        );
    }

    @SuppressWarnings("ConstantConditions")
    public long sendAccountTextMessage(final String accountId, final String to, final String msg) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        Log.i(TAG, "sendAccountTextMessage() thread running... " + accountId + " " + to + " " + msg);
                        StringMap msgs = new StringMap();
                        msgs.setRaw("text/plain", Blob.fromString(msg));
                        return Ringservice.sendAccountTextMessage(accountId, to, msgs);
                    }
                }
        );
    }

    public DataTransferError sendFile(final Long dataTransferId, final DataTransferInfo dataTransferInfo) {
        Long errorCode = FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        Log.i(TAG, "sendFile() thread running... accountId=" + dataTransferInfo.getAccountId() + ", peer=" + dataTransferInfo.getPeer() + ", filePath=" + dataTransferInfo.getPath());
                        return Ringservice.sendFile(dataTransferInfo, dataTransferId);
                    }
                }
        );
        return getDataTransferError(errorCode);
    }

    public DataTransferError acceptFileTransfer(final Long dataTransferId, final String filePath, final long offset) {
        Long errorCode = FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        Log.i(TAG, "acceptFileTransfer() thread running... dataTransferId=" + dataTransferId + ", filePath=" + filePath + ", offset=" + offset);
                        return Ringservice.acceptFileTransfer(dataTransferId, filePath, offset);
                    }
                }
        );
        return getDataTransferError(errorCode);
    }

    public DataTransferError cancelDataTransfer(final Long dataTransferId) {
        Long errorCode = FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        Log.i(TAG, "cancelDataTransfer() thread running... dataTransferId=" + dataTransferId);
                        return Ringservice.cancelDataTransfer(dataTransferId);
                    }
                }
        );
        return getDataTransferError(errorCode);
    }

    public DataTransferWrapper dataTransferInfo(final Long dataTransferId, final DataTransferInfo dataTransferInfo) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<DataTransferWrapper>() {
                    @Override
                    public DataTransferWrapper call() throws Exception {
                        Log.i(TAG, "dataTransferInfo() thread running... dataTransferId=" + dataTransferId);
                        long errorCode = Ringservice.dataTransferInfo(dataTransferId, dataTransferInfo);
                        return new DataTransferWrapper(dataTransferInfo, getDataTransferError(errorCode));
                    }
                }
        );
    }

    public SipCall getCurrentCallForId(String callId) {
        return currentCalls.get(callId);
    }

    public SipCall getCurrentCallForContactId(String contactId) {
        for (SipCall call : currentCalls.values()) {
            if (contactId.contains(call.getContact().getPhones().get(0).getNumber().toString())) {
                return call;
            }
        }
        return null;
    }

    public void removeCallForId(String callId) {
        currentCalls.remove(callId);
    }

    private SipCall addCall(String accountId, String callId, String from, int direction) {
        SipCall call = currentCalls.get(callId);
        if (call == null) {
            call = new SipCall(callId, accountId, new Uri(from), direction);
            currentCalls.put(callId, call);
        } else {
            Log.w(TAG, "Call already existed ! " + callId + " " + from);
        }
        return call;
    }

    private SipCall parseCallState(String callId, String newState) {
        int callState = SipCall.stateFromString(newState);
        SipCall sipCall = currentCalls.get(callId);
        if (sipCall != null) {
            sipCall.setCallState(callState);
            sipCall.setDetails(Ringservice.getCallDetails(callId).toNative());
        } else if (callState != SipCall.State.OVER) {
            Map<String, String> callDetails = Ringservice.getCallDetails(callId).toNative();
            sipCall = new SipCall(callId, callDetails);
            sipCall.setCallState(callState);
            CallContact contact = mContactService.findContact(sipCall.getNumberUri());
            String registeredName = callDetails.get("REGISTERED_NAME");
            if (registeredName != null && !registeredName.isEmpty()) {
                contact.setUsername(registeredName);
            }
            sipCall.setContact(contact);
            currentCalls.put(callId, sipCall);
        }
        return sipCall;
    }

    void callStateChanged(String callId, String newState, int detailCode) {
        Log.d(TAG, "call state changed: " + callId + ", " + newState + ", " + detailCode);
        try {
            SipCall call = parseCallState(callId, newState);
            if (call != null) {
                setChanged();

                ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CALL_STATE_CHANGED);
                event.addEventInput(ServiceEvent.EventInput.CALL, call);
                notifyObservers(event);

                if (call.getCallState() == SipCall.State.OVER) {
                    currentCalls.remove(call.getCallId());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception during state change: ", e);
        }
    }

    void incomingCall(String accountId, String callId, String from) {
        Log.d(TAG, "incoming call: " + accountId + ", " + callId + ", " + from);

        SipCall call = addCall(accountId, callId, from, SipCall.Direction.INCOMING);
        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.INCOMING_CALL);
        event.addEventInput(ServiceEvent.EventInput.CALL, call);
        notifyObservers(event);
    }

    public void incomingMessage(String callId, String from, StringMap messages) {
        SipCall sipCall = currentCalls.get(callId);
        if (sipCall == null || messages == null) {
            Log.w(TAG, "incomingMessage: unknown call or no message: " + callId + " " + from);
            return;
        }
        if (sipCall.appendToVCard(messages)) {
            mContactService.saveVCardContactData(sipCall.getContact());
        }
        if (messages.has_key(MIME_TEXT_PLAIN)) {
            mHistoryService.incomingMessage(sipCall.getAccount(), callId, from, messages);
        }
    }

    void recordPlaybackFilepath(String id, String filename) {
        Log.d(TAG, "record playback filepath: " + id + ", " + filename);
        // todo needs more explainations on that
    }

    void onRtcpReportReceived(String callId, IntegerMap stats) {
        Log.i(TAG, "on RTCP report received: " + callId);
        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONFERENCE_CHANGED);
        event.addEventInput(ServiceEvent.EventInput.CALL_ID, callId);
        event.addEventInput(ServiceEvent.EventInput.STATS, stats);
        notifyObservers(event);
    }

    public void dataTransferEvent(long transferId, int eventCode) {
        DataTransferEventCode dataTransferEventCode = getDataTransferEventCode(eventCode);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.DATA_TRANSFER);
        event.addEventInput(ServiceEvent.EventInput.TRANSFER_ID, transferId);
        event.addEventInput(ServiceEvent.EventInput.TRANSFER_EVENT_CODE, dataTransferEventCode);
        notifyObservers(event);
    }

    private static DataTransferEventCode getDataTransferEventCode(int eventCode) {
        DataTransferEventCode dataTransferEventCode = DataTransferEventCode.INVALID;
        try {
            dataTransferEventCode = DataTransferEventCode.values()[eventCode];
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Log.e(TAG, "getDataTransferEventCode: invalid data transfer status from daemon");
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
}