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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import ezvcard.VCard;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

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

    private final Map<String, SipCall> currentCalls = new HashMap<>();
    private final Map<String, Conference> currentConferences = new HashMap<>();

    private final PublishSubject<SipCall> callSubject = PublishSubject.create();
    private final PublishSubject<Conference> conferenceSubject = PublishSubject.create();

    private final Set<String> currentConnections = new HashSet<>();
    private final BehaviorSubject<Integer> connectionSubject = BehaviorSubject.createDefault(0);

    public Observable<Conference> getConfsUpdates() {
        return conferenceSubject;
    }

    private Observable<Conference> getConfCallUpdates(final Conference conf) {
        Log.w(TAG, "getConfCallUpdates " + conf.getConfId());

        return conferenceSubject
                .filter(c -> c == conf)
                .startWith(conf)
                .map(Conference::getParticipants)
                .switchMap(list -> Observable.fromIterable(list)
                        .flatMap(call -> callSubject.filter(c -> c == call)))
                .map(call -> conf)
                .startWith(conf);
    }

    public Observable<Conference> getConfUpdates(final String confId) {
        SipCall call = getCurrentCallForId(confId);
        return call == null ? Observable.error(new IllegalArgumentException()) : getConfUpdates(call);
        /*Conference call = currentConferences.get(confId);
        return call == null ? Observable.error(new IllegalArgumentException()) : conferenceSubject
                .filter(c -> c.getId().equals(confId));//getConfUpdates(call);*/
    }

    public Observable<Boolean> getConnectionUpdates() {
        return connectionSubject
                .map(i -> i > 0)
                .distinctUntilChanged();
    }

    private void updateConnectionCount() {
        connectionSubject.onNext(currentConnections.size() - 2*currentCalls.size());
    }

    private static class ConferenceEntity {
        Conference conference;
        ConferenceEntity(Conference conf) {
            conference = conf;
        }
    }

    public Observable<Conference> getConfUpdates(final SipCall call) {
        return getConfUpdates(getConference(call));
    }
    private Observable<Conference> getConfUpdates(final Conference conference) {
        Log.w(TAG, "getConfUpdates " + conference.getId());

        ConferenceEntity conferenceEntity = new ConferenceEntity(conference);
        return conferenceSubject
                .startWith(conference)
                .filter(conf -> {
                    Log.w(TAG, "getConfUpdates filter " + conf.getConfId() + " " + conf.getParticipants().size() + " (tracked " + conferenceEntity.conference.getConfId() + " " + conferenceEntity.conference.getParticipants().size() + ")");
                    if (conf == conferenceEntity.conference) {
                        return true;
                    }
                    if (conf.contains(conferenceEntity.conference.getId())) {
                        Log.w(TAG, "Switching tracked conference (up) to " + conf.getId());
                        conferenceEntity.conference = conf;
                        return true;
                    }
                    if (conferenceEntity.conference.getParticipants().size() == 1
                            && conf.getParticipants().size() == 1
                            && conferenceEntity.conference.getCall() == conf.getCall()
                            && conf.getCall().getDaemonIdString().equals(conf.getConfId())) {
                        Log.w(TAG, "Switching tracked conference (down) to " + conf.getId());
                        conferenceEntity.conference = conf;
                        return true;
                    }
                    return false;
                })
                .switchMap(this::getConfCallUpdates);
    }

    public Observable<SipCall> getCallsUpdates() {
        return callSubject;
    }
    private Observable<SipCall> getCallUpdates(final SipCall call) {
        return callSubject.filter(c -> c == call)
                .startWith(call)
                .takeWhile(c -> c.getCallStatus() != SipCall.CallStatus.OVER);
    }
    /*public Observable<SipCall> getCallUpdates(final String callId) {
        SipCall call = getCurrentCallForId(callId);
        return call == null ? Observable.error(new IllegalArgumentException()) : getCallUpdates(call);
    }*/

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
            updateConnectionCount();
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

    private SipCall getCurrentCallForId(String callId) {
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
        currentConferences.remove(callId);
    }

    private SipCall addCall(String accountId, String callId, String from, SipCall.Direction direction) {
        SipCall call = currentCalls.get(callId);
        if (call == null) {
            Account account = mAccountService.getAccount(accountId);
            Uri fromUri = new Uri(from);
            Conversation conversation = account.getByUri(fromUri);
            CallContact contact = mContactService.findContact(account, fromUri);
            call = new SipCall(callId, new Uri(from).getUri(), accountId, conversation, contact, direction);
            currentCalls.put(callId, call);
        } else {
            Log.w(TAG, "Call already existed ! " + callId + " " + from);
        }
        return call;
    }

    private Conference addConference(SipCall call) {
        String confId = call.getConfId();
        if (confId == null) {
            confId = call.getDaemonIdString();
        }
        Conference conference = currentConferences.get(confId);
        if (conference == null) {
            conference = new Conference(call);
            currentConferences.put(confId, conference);
            conferenceSubject.onNext(conference);
        } else {
            Log.w(TAG, "Conference already existed ! " + confId);
        }
        return conference;
    }

    private SipCall parseCallState(String callId, String newState) {
        SipCall.CallStatus callState = SipCall.CallStatus.fromString(newState);
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
            updateConnectionCount();
        }
        return sipCall;
    }


    public void connectionUpdate(String id, int state) {
        Log.d(TAG, "connectionUpdate: " + id + " " + state);
        switch(state) {
            case 0:
                currentConnections.add(id);
                break;
            case 1:
            case 2:
                currentConnections.remove(id);
                break;
        }
        updateConnectionCount();
    }

    void callStateChanged(String callId, String newState, int detailCode) {
        Log.d(TAG, "call state changed: " + callId + ", " + newState + ", " + detailCode);
        try {
            SipCall call = parseCallState(callId, newState);
            if (call != null) {
                callSubject.onNext(call);
                if (call.getCallStatus() == SipCall.CallStatus.OVER) {
                    currentCalls.remove(call.getDaemonIdString());
                    currentConferences.remove(call.getDaemonIdString());
                    updateConnectionCount();
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
        updateConnectionCount();
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

    void onRtcpReportReceived(String callId) {
        Log.i(TAG, "on RTCP report received: " + callId);
    }

    public void removeConference(final String confId) {
        mExecutor.execute(() -> Ringservice.removeConference(confId));
    }

    public void joinParticipant(final String selCallId, final String dragCallId) {
        mExecutor.execute(() -> Ringservice.joinParticipant(selCallId, dragCallId));
    }

    public void addParticipant(final String callId, final String confId) {
        mExecutor.execute(() -> Ringservice.addParticipant(callId, confId));
    }

    public void addMainParticipant(final String confId) {
        mExecutor.execute(() -> Ringservice.addMainParticipant(confId));
    }

    public void detachParticipant(final String callId) {
        mExecutor.execute(() -> Ringservice.detachParticipant(callId));
    }

    public void joinConference(final String selConfId, final String dragConfId) {
        mExecutor.execute(() -> Ringservice.joinConference(selConfId, dragConfId));
    }

    public void hangUpConference(final String confId) {
        mExecutor.execute(() -> Ringservice.hangUpConference(confId));
    }

    public void holdConference(final String confId) {
        mExecutor.execute(() -> Ringservice.holdConference(confId));
    }

    public void unholdConference(final String confId) {
        mExecutor.execute(() -> Ringservice.unholdConference(confId));
    }

    public boolean isConferenceParticipant(final String callId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "isConferenceParticipant() running...");
                return Ringservice.isConferenceParticipant(callId);
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running isConferenceParticipant()", e);
        }
        return false;
    }

    public Map<String, ArrayList<String>> getConferenceList() {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getConferenceList() running...");
                StringVect callIds = Ringservice.getCallList();
                HashMap<String, ArrayList<String>> confs = new HashMap<>(callIds.size());
                for (int i = 0; i < callIds.size(); i++) {
                    String callId = callIds.get(i);
                    String confId = Ringservice.getConferenceId(callId);
                    Map<String, String> callDetails = Ringservice.getCallDetails(callId).toNative();

                    //todo remove condition when callDetails does not contains sips ids anymore
                    if (!callDetails.get("PEER_NUMBER").contains("sips")) {
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
                }
                return confs;
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running isConferenceParticipant()", e);
        }
        return null;
    }

    public List<String> getParticipantList(final String confId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getParticipantList() running...");
                return new ArrayList<>(Ringservice.getParticipantList(confId));
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getParticipantList()", e);
        }
        return null;
    }

    public Conference getConference(SipCall call) {
        return addConference(call);
    }

    public String getConferenceId(String callId) {
        return Ringservice.getConferenceId(callId);
    }

    public String getConferenceState(final String callId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getConferenceDetails() thread running...");
                return Ringservice.getConferenceDetails(callId).get("CONF_STATE");
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getParticipantList()", e);
        }
        return null;
    }

    public Conference getConference(final String id) {
        return currentConferences.get(id);
    }

    public Map<String, String> getConferenceDetails(final String id) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCredentials() thread running...");
                return Ringservice.getConferenceDetails(id).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getParticipantList()", e);
        }
        return null;
    }

    void conferenceCreated(final String confId) {
        Log.d(TAG, "conference created: " + confId);

        Conference conf = currentConferences.get(confId);
        if (conf == null) {
            conf = new Conference(confId);
            currentConferences.put(confId, conf);
        }
        StringVect participants = Ringservice.getParticipantList(confId);
        for (String callId : participants) {
            SipCall call = getCurrentCallForId(callId);
            if (call != null) {
                Log.d(TAG, "conference created: adding participant " + callId + " " + call.getContact().getDisplayName());
                call.setConfId(confId);
                conf.addParticipant(call);
            }
            Conference rconf = currentConferences.remove(callId);
            Log.d(TAG, "conference created: removing conference " + callId + " " + rconf + " now " + currentConferences.size());
        }
        conferenceSubject.onNext(conf);
    }

    void conferenceRemoved(String confId) {
        Log.d(TAG, "conference removed: " + confId);

        Conference conf = currentConferences.remove(confId);
        if (conf != null) {
            for (SipCall call : conf.getParticipants()) {
                call.setConfId(null);
            }
            conf.removeParticipants();
            conferenceSubject.onNext(conf);
        }
    }

    void conferenceChanged(String confId, String state) {
        Log.d(TAG, "conference changed: " + confId + ", " + state);
        try {
            Conference conf = currentConferences.get(confId);
            if (conf == null) {
                conf = new Conference(confId);
                currentConferences.put(confId, conf);
            }
            Set<String> participants = new HashSet<>(Ringservice.getParticipantList(confId));
            // Add new participants
            for (String callId : participants) {
                if (!conf.contains(callId)) {
                    SipCall call = getCurrentCallForId(callId);
                    if (call != null) {
                        Log.d(TAG, "conference changed: adding participant " + callId + " " + call.getContact().getDisplayName());
                        call.setConfId(confId);
                        conf.addParticipant(call);
                    }
                    currentConferences.remove(callId);
                }
            }

            // Remove participants
            List<SipCall> calls = conf.getParticipants();
            Iterator<SipCall> i = calls.iterator();
            boolean removed = false;
            while (i.hasNext()) {
                SipCall call = i.next();
                if (!participants.contains(call.getDaemonIdString())) {
                    Log.d(TAG, "conference changed: removing participant " + call.getDaemonIdString() + " " + call.getContact().getDisplayName());
                    call.setConfId(null);
                    i.remove();
                    removed = true;
                }
            }

            conferenceSubject.onNext(conf);

            if (removed && conf.getParticipants().size() == 1 && conf.getConfId() != null) {
                SipCall call = conf.getCall();
                call.setConfId(null);
                addConference(call);
            }
        } catch (Exception e) {
            Log.w(TAG, "exception in conferenceChanged", e);
        }
    }
}
