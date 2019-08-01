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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import ezvcard.VCard;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class CallService {

    private final static String TAG = CallService.class.getSimpleName();
    public final static String MIME_TEXT_PLAIN = "text/plain";

    @Inject
    @Named("DaemonExecutor")
    ScheduledExecutorService mExecutor;

    @Inject
    ContactService mContactService;

    @Inject
    HistoryService mHistoryService;

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private Map<String, SipCall> currentCalls = new HashMap<>();
    private final PublishSubject<SipCall> callSubject = PublishSubject.create();

    public Subject<SipCall> getCallSubject() {
        return callSubject;
    }

    public Observable<SipCall> getCallUpdates(final SipCall call) {
        return callSubject.filter(c -> c == call).startWith(call);
    }
    public Observable<SipCall> getCallUpdates(final String callId) {
        SipCall call = getCurrentCallForId(callId);
        return call == null ? Observable.just(new SipCall()) : getCallUpdates(call);
    }

    public Observable<SipCall> placeCallObservable(final String accountId, final String number, final boolean audioOnly) {
        return placeCall(accountId, number, audioOnly)
                .flatMapObservable(this::getCallUpdates);
    }

    public Single<SipCall> placeCall(final String account, final String number, final boolean audioOnly) {
        return Single.fromCallable(() -> {
            Log.i(TAG, "placeCall() thread running... " + number + " audioOnly: " + audioOnly);

            HashMap<String, String> volatileDetails = new HashMap<>();
            volatileDetails.put(SipCall.KEY_AUDIO_ONLY, String.valueOf(audioOnly));

            String callId = Ringservice.placeCall(account, number, StringMap.toSwig(volatileDetails));
            if (callId == null || callId.isEmpty())
                return null;
            if (audioOnly) {
                Ringservice.muteLocalMedia(callId, "MEDIA_TYPE_VIDEO", true);
            }
            SipCall call = addCall(account, callId, number, SipCall.Direction.OUTGOING);
            call.muteVideo(audioOnly);
            return call;
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    public void refuse(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "refuse() running... " + callId);
            Ringservice.refuse(callId);
            Ringservice.hangUp(callId);
        });
    }

    public void accept(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "accept() running... " + callId);
            Ringservice.muteCapture(false);
            Ringservice.accept(callId);
        });
    }

    public void hangUp(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "hangUp() running... " + callId);
            Ringservice.hangUp(callId);
        });
    }

    public void hold(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "hold() running... " + callId);
            Ringservice.hold(callId);
        });
    }

    public void unhold(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "unhold() running... " + callId);
            Ringservice.unhold(callId);
        });
    }

    public Map<String, String> getCallDetails(final String callId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCallDetails() running... " + callId);
                return Ringservice.getCallDetails(callId).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCallDetails()", e);
        }
        return null;
    }

    public void muteRingTone(boolean mute) {
        Log.d(TAG, (mute ? "Muting." : "Unmuting.") + " ringtone.");
        Ringservice.muteRingtone(mute);
    }

    public void restartAudioLayer() {
        mExecutor.execute(() -> {
            Log.i(TAG, "restartAudioLayer() running...");
            Ringservice.setAudioPlugin(Ringservice.getCurrentAudioOutputPlugin());
        });
    }

    public void setAudioPlugin(final String audioPlugin) {
        mExecutor.execute(() -> {
            Log.i(TAG, "setAudioPlugin() running...");
            Ringservice.setAudioPlugin(audioPlugin);
        });
    }

    public String getCurrentAudioOutputPlugin() {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCurrentAudioOutputPlugin() running...");
                return Ringservice.getCurrentAudioOutputPlugin();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCallDetails()", e);
        }
        return null;
    }

    public void playDtmf(final String key) {
        mExecutor.execute(() -> {
            Log.i(TAG, "playDTMF() running...");
            Ringservice.playDTMF(key);
        });
    }

    public void setMuted(final boolean mute) {
        mExecutor.execute(() -> {
            Log.i(TAG, "muteCapture() running...");
            Ringservice.muteCapture(mute);
        });
    }

    public boolean isCaptureMuted() {
        return Ringservice.isCaptureMuted();
    }

    public void transfer(final String callId, final String to) {
        mExecutor.execute(() -> {
            Log.i(TAG, "transfer() thread running...");
            if (Ringservice.transfer(callId, to)) {
                Log.i(TAG, "OK");
            } else {
                Log.i(TAG, "NOT OK");
            }
        });
    }

    public void attendedTransfer(final String transferId, final String targetID) {
        mExecutor.execute(() -> {
            Log.i(TAG, "attendedTransfer() thread running...");
            if (Ringservice.attendedTransfer(transferId, targetID)) {
                Log.i(TAG, "OK");
            } else {
                Log.i(TAG, "NOT OK");
            }
        });
    }

    public String getRecordPath() {
        try {
            return mExecutor.submit(Ringservice::getRecordPath).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running isCaptureMuted()", e);
        }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    public boolean toggleRecordingCall(final String id) {
        mExecutor.execute(() -> Ringservice.toggleRecording(id));
        return false;
    }

    public boolean startRecordedFilePlayback(final String filepath) {
        mExecutor.execute(() -> Ringservice.startRecordedFilePlayback(filepath));
        return false;
    }

    public void stopRecordedFilePlayback() {
        mExecutor.execute(Ringservice::stopRecordedFilePlayback);
    }

    public void setRecordPath(final String path) {
        mExecutor.execute(() -> Ringservice.setRecordPath(path));
    }

    public void sendTextMessage(final String callId, final String msg) {
        mExecutor.execute(() -> {
            Log.i(TAG, "sendTextMessage() thread running...");
            StringMap messages = new StringMap();
            messages.setRaw("text/plain", Blob.fromString(msg));
            Ringservice.sendTextMessage(callId, messages, "", false);
        });
    }

    public Single<Long> sendAccountTextMessage(final String accountId, final String to, final String msg) {
        return Single.fromCallable(() -> {
            Log.i(TAG, "sendAccountTextMessage() running... " + accountId + " " + to + " " + msg);
            StringMap msgs = new StringMap();
            msgs.setRaw("text/plain", Blob.fromString(msg));
            return Ringservice.sendAccountTextMessage(accountId, to, msgs);
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    public Completable cancelMessage(final String accountId, final long messageID) {
        return Completable
                .fromAction(() -> {
                    Log.i(TAG, "CancelMessage() running...   Account ID:  " + accountId + " " + "Message ID " + " " + messageID);
                    Ringservice.cancelMessage(accountId, messageID);
                })
                .subscribeOn(Schedulers.from(mExecutor));
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
        Account account = mAccountService.getAccount(accountId);
        Conversation conversation = account.getByUri(new Uri(from).getUri());
        SipCall call = currentCalls.get(callId);
        if (call == null) {
            CallContact contact = mContactService.findContact(account, new Uri(from));
            call = new SipCall(callId, new Uri(from).getUri(), conversation, contact, direction);
            call.setAccount(accountId);
            currentCalls.put(callId, call);
        } else {
            Log.w(TAG, "Call already existed ! " + callId + " " + from);
        }
        return call;
    }

    private SipCall parseCallState(String callId, String newState) {
        SipCall.CallStatus callState = SipCall.stateFromString(newState);
        SipCall sipCall = currentCalls.get(callId);
        if (sipCall != null) {
            sipCall.setCallState(callState);
            sipCall.setDetails(Ringservice.getCallDetails(callId).toNative());
        } else if (callState !=  SipCall.CallStatus.OVER && callState !=  SipCall.CallStatus.FAILURE) {
            Map<String, String> callDetails = Ringservice.getCallDetails(callId).toNative();
            sipCall = new SipCall(callId, callDetails);
            if (!callDetails.containsKey(SipCall.KEY_PEER_NUMBER)) {
                Log.w(TAG, "No number");
                return null;
            }
            sipCall.setCallState(callState);

            CallContact contact = mContactService.findContact(mAccountService.getAccount(sipCall.getAccount()), new Uri(sipCall.getContactNumber()));
            String registeredName = callDetails.get("REGISTERED_NAME");
            if (registeredName != null && !registeredName.isEmpty()) {
                contact.setUsername(registeredName);
            }
            sipCall.setContact(contact);

            Account account = mAccountService.getAccount(sipCall.getAccount());
            sipCall.setConversation(account.getByUri(contact.getPrimaryUri()));

            currentCalls.put(callId, sipCall);
        }
        return sipCall;
    }

    void callStateChanged(String callId, String newState, int detailCode) {
        Log.d(TAG, "call state changed: " + callId + ", " + newState + ", " + detailCode);
        try {
            SipCall call = parseCallState(callId, newState);
            if (call != null) {
                callSubject.onNext(call);
                if (call.getCallStatus() == SipCall.CallStatus.OVER) {
                    currentCalls.remove(call.getDaemonIdString());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception during state change: ", e);
        }
    }

    void incomingCall(String accountId, String callId, String from) {
        Log.d(TAG, "incoming call: " + accountId + ", " + callId + ", " + from);

        SipCall call = addCall(accountId, callId, from, SipCall.Direction.INCOMING);
        callSubject.onNext(call);
    }

    public void incomingMessage(String callId, String from, Map<String, String> messages) {
        SipCall sipCall = currentCalls.get(callId);
        if (sipCall == null || messages == null) {
            Log.w(TAG, "incomingMessage: unknown call or no message: " + callId + " " + from);
            return;
        }
        VCard vcard = sipCall.appendToVCard(messages);
        if (vcard != null) {
            mContactService.saveVCardContactData(sipCall.getContact(), sipCall.getAccount(), vcard);
        }
        if (messages.containsKey(MIME_TEXT_PLAIN)) {
            mAccountService.incomingAccountMessage(sipCall.getAccount(), callId, from, messages);
        }
    }

    void recordPlaybackFilepath(String id, String filename) {
        Log.d(TAG, "record playback filepath: " + id + ", " + filename);
        // todo needs more explainations on that
    }

    void onRtcpReportReceived(String callId, IntegerMap stats) {
        Log.i(TAG, "on RTCP report received: " + callId);
    }

}