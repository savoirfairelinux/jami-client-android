/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import net.jami.daemon.Blob;
import net.jami.daemon.JamiService;
import net.jami.daemon.StringMap;
import net.jami.daemon.StringVect;
import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Conference;
import net.jami.model.Conversation;
import net.jami.model.Call;
import net.jami.model.Uri;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import ezvcard.VCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class CallService {
    private final static String TAG = CallService.class.getSimpleName();
    public final static String MIME_TEXT_PLAIN = "text/plain";
    public static final String MIME_GEOLOCATION = "application/geo";
    public static final String MEDIA_TYPE_AUDIO = "MEDIA_TYPE_AUDIO";
    public static final String MEDIA_TYPE_VIDEO = "MEDIA_TYPE_VIDEO";

    private final ScheduledExecutorService mExecutor;
    private final ContactService mContactService;
    private final AccountService mAccountService;

    public CallService(ScheduledExecutorService executor, ContactService contactService, AccountService accountService) {
        mExecutor = executor;
        mContactService = contactService;
        mAccountService = accountService;
    }

    private final Map<String, Call> currentCalls = new HashMap<>();
    private final Map<String, Conference> currentConferences = new HashMap<>();

    private final PublishSubject<Call> callSubject = PublishSubject.create();
    private final PublishSubject<Conference> conferenceSubject = PublishSubject.create();

    // private final Set<String> currentConnections = new HashSet<>();
    // private final BehaviorSubject<Integer> connectionSubject = BehaviorSubject.createDefault(0);

    public Observable<Conference> getConfsUpdates() {
        return conferenceSubject;
    }

    private Observable<Conference> getConfCallUpdates(final Conference conf) {
        Log.w(TAG, "getConfCallUpdates " + conf.getConfId());

        return conferenceSubject
                .filter(c -> c == conf)
                .startWithItem(conf)
                .map(Conference::getParticipants)
                .switchMap(list -> Observable.fromIterable(list)
                        .flatMap(call -> callSubject.filter(c -> c == call)))
                .map(call -> conf)
                .startWithItem(conf);
    }

    public Observable<Conference> getConfUpdates(final String confId) {
        Call call = getCurrentCallForId(confId);
        return call == null ? Observable.error(new IllegalArgumentException()) : getConfUpdates(call);
        /*Conference call = currentConferences.get(confId);
        return call == null ? Observable.error(new IllegalArgumentException()) : conferenceSubject
                .filter(c -> c.getId().equals(confId));//getConfUpdates(call);*/
    }

    /*public Observable<Boolean> getConnectionUpdates() {
        return connectionSubject
                .map(i -> i > 0)
                .distinctUntilChanged();
    }*/

    private void updateConnectionCount() {
        //connectionSubject.onNext(currentConnections.size() - 2*currentCalls.size());
    }

    public void setIsComposing(String accountId, String uri, boolean isComposing) {
        mExecutor.execute(() -> JamiService.setIsComposing(accountId, uri, isComposing));
    }

    public void onConferenceInfoUpdated(String confId, List<Map<String, String>> info) {
        Log.w(TAG, "onConferenceInfoUpdated " + confId + " " + info);
        Conference conference = getConference(confId);
        boolean isModerator = false;
        if (conference != null) {
            List<Conference.ParticipantInfo> newInfo = new ArrayList<>(info.size());
            if (conference.isConference()) {
                for (Map<String, String> i : info) {
                    Call call = conference.findCallByContact(Uri.fromString(i.get("uri")));
                    if (call != null) {
                        Conference.ParticipantInfo confInfo = new Conference.ParticipantInfo(call, call.getContact(), i);
                        if (confInfo.isEmpty()) {
                            Log.w(TAG, "onConferenceInfoUpdated: ignoring empty entry " + i);
                            continue;
                        }
                        if (confInfo.contact.isUser() && confInfo.isModerator) {
                            isModerator = true;
                        }
                        newInfo.add(confInfo);
                    } else {
                        Log.w(TAG, "onConferenceInfoUpdated " + confId + " can't find call for " + i);
                        // TODO
                    }
                }
            } else {
                Account account = mAccountService.getAccount(conference.getCall().getAccount());
                for (Map<String, String> i : info) {
                    Conference.ParticipantInfo confInfo = new Conference.ParticipantInfo(null, account.getContactFromCache(Uri.fromString(i.get("uri"))), i);
                    if (confInfo.isEmpty()) {
                        Log.w(TAG, "onConferenceInfoUpdated: ignoring empty entry " + i);
                        continue;
                    }
                    if (confInfo.contact.isUser() && confInfo.isModerator) {
                        isModerator = true;
                    }
                    newInfo.add(confInfo);
                }
            }
            conference.setIsModerator(isModerator);
            conference.setInfo(newInfo);
        } else {
            Log.w(TAG, "onConferenceInfoUpdated can't find conference" + confId);
        }
    }

    public void setConfMaximizedParticipant(String confId, Uri uri) {
        mExecutor.execute(() -> {
            JamiService.setActiveParticipant(confId, uri == null ? "" : uri.getRawRingId());
            JamiService.setConferenceLayout(confId, 1);
        });
    }

    public void setConfGridLayout(String confId) {
        mExecutor.execute(() -> JamiService.setConferenceLayout(confId, 0));
    }

    public void remoteRecordingChanged(String callId, Uri peerNumber, boolean state) {
        Log.w(TAG, "remoteRecordingChanged " + callId + " " + peerNumber + " " + state);
        Conference conference = getConference(callId);
        Call call;
        if (conference == null) {
            call = getCurrentCallForId(callId);
            if (call != null) {
                conference = getConference(call);
            }
        } else {
            call = conference.getFirstCall();
        }
        Account account = call == null ? null : mAccountService.getAccount(call.getAccount());
        Contact contact = account == null ? null : account.getContactFromCache(peerNumber);
        if (conference != null && contact != null) {
            conference.setParticipantRecording(contact, state);
        }
    }

    private static class ConferenceEntity {
        Conference conference;
        ConferenceEntity(Conference conf) {
            conference = conf;
        }
    }

    public Observable<Conference> getConfUpdates(final Call call) {
        return getConfUpdates(getConference(call));
    }
    private Observable<Conference> getConfUpdates(final Conference conference) {
        Log.w(TAG, "getConfUpdates " + conference.getId());

        ConferenceEntity conferenceEntity = new ConferenceEntity(conference);
        return conferenceSubject
                .startWithItem(conference)
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

    public Observable<Call> getCallsUpdates() {
        return callSubject;
    }
    private Observable<Call> getCallUpdates(final Call call) {
        return callSubject.filter(c -> c == call)
                .startWithItem(call)
                .takeWhile(c -> c.getCallStatus() != Call.CallStatus.OVER);
    }
    /*public Observable<SipCall> getCallUpdates(final String callId) {
        SipCall call = getCurrentCallForId(callId);
        return call == null ? Observable.error(new IllegalArgumentException()) : getCallUpdates(call);
    }*/

    public Observable<Call> placeCallObservable(final String accountId, final Uri conversationUri, final Uri number, final boolean audioOnly) {
        return placeCall(accountId, conversationUri, number, audioOnly)
                .flatMapObservable(this::getCallUpdates);
    }

    public Single<Call> placeCall(final String account, final Uri conversationUri, final Uri number, final boolean audioOnly) {
        return Single.fromCallable(() -> {
            Log.i(TAG, "placeCall() thread running... " + number + " audioOnly: " + audioOnly);

            HashMap<String, String> volatileDetails = new HashMap<>();
            volatileDetails.put(Call.KEY_AUDIO_ONLY, String.valueOf(audioOnly));

            String callId = JamiService.placeCall(account, number.getUri(), StringMap.toSwig(volatileDetails));
            if (callId == null || callId.isEmpty())
                return null;
            if (audioOnly) {
                JamiService.muteLocalMedia(callId, "MEDIA_TYPE_VIDEO", true);
            }
            Call call = addCall(account, callId, number, Call.Direction.OUTGOING);
            if (conversationUri != null && conversationUri.isSwarm())
                call.setSwarmInfo(conversationUri.getRawRingId());
            call.muteVideo(audioOnly);
            updateConnectionCount();
            return call;
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    public void refuse(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "refuse() running... " + callId);
            JamiService.refuse(callId);
            JamiService.hangUp(callId);
        });
    }

    public void accept(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "accept() running... " + callId);
            JamiService.muteCapture(false);
            JamiService.accept(callId);
        });
    }

    public void hangUp(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "hangUp() running... " + callId);
            JamiService.hangUp(callId);
        });
    }

    public void muteParticipant(String confId, String peerId, boolean mute) {
        mExecutor.execute(() -> {
            Log.i(TAG, "mute participant... " + peerId);
            JamiService.muteParticipant(confId, peerId, mute);
        });
    }

    public void hangupParticipant(String confId, String peerId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "hangup participant... " + peerId);
            JamiService.hangupParticipant(confId, peerId);
        });
    }

    public void hold(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "hold() running... " + callId);
            JamiService.hold(callId);
        });
    }

    public void unhold(final String callId) {
        mExecutor.execute(() -> {
            Log.i(TAG, "unhold() running... " + callId);
            JamiService.unhold(callId);
        });
    }

    public Map<String, String> getCallDetails(final String callId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCallDetails() running... " + callId);
                return JamiService.getCallDetails(callId).toNative();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCallDetails()", e);
        }
        return null;
    }

    public void muteRingTone(boolean mute) {
        Log.d(TAG, (mute ? "Muting." : "Unmuting.") + " ringtone.");
        JamiService.muteRingtone(mute);
    }

    public void restartAudioLayer() {
        mExecutor.execute(() -> {
            Log.i(TAG, "restartAudioLayer() running...");
            JamiService.setAudioPlugin(JamiService.getCurrentAudioOutputPlugin());
        });
    }

    public void setAudioPlugin(final String audioPlugin) {
        mExecutor.execute(() -> {
            Log.i(TAG, "setAudioPlugin() running...");
            JamiService.setAudioPlugin(audioPlugin);
        });
    }

    public String getCurrentAudioOutputPlugin() {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getCurrentAudioOutputPlugin() running...");
                return JamiService.getCurrentAudioOutputPlugin();
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getCallDetails()", e);
        }
        return null;
    }

    public void playDtmf(final String key) {
        mExecutor.execute(() -> {
            Log.i(TAG, "playDTMF() running...");
            JamiService.playDTMF(key);
        });
    }

    public void setMuted(final boolean mute) {
        mExecutor.execute(() -> {
            Log.i(TAG, "muteCapture() running...");
            JamiService.muteCapture(mute);
        });
    }

    public void setLocalMediaMuted(final String callId, String mediaType, final boolean mute) {
        mExecutor.execute(() -> {
            Log.i(TAG, "muteCapture() running...");
            JamiService.muteLocalMedia(callId, mediaType, mute);
        });
    }

    public boolean isCaptureMuted() {
        return JamiService.isCaptureMuted();
    }

    public void transfer(final String callId, final String to) {
        mExecutor.execute(() -> {
            Log.i(TAG, "transfer() thread running...");
            if (JamiService.transfer(callId, to)) {
                Log.i(TAG, "OK");
            } else {
                Log.i(TAG, "NOT OK");
            }
        });
    }

    public void attendedTransfer(final String transferId, final String targetID) {
        mExecutor.execute(() -> {
            Log.i(TAG, "attendedTransfer() thread running...");
            if (JamiService.attendedTransfer(transferId, targetID)) {
                Log.i(TAG, "OK");
            } else {
                Log.i(TAG, "NOT OK");
            }
        });
    }

    public String getRecordPath() {
        try {
            return mExecutor.submit(JamiService::getRecordPath).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running isCaptureMuted()", e);
        }
        return null;
    }

    public boolean toggleRecordingCall(final String id) {
        mExecutor.execute(() -> JamiService.toggleRecording(id));
        return false;
    }

    public boolean startRecordedFilePlayback(final String filepath) {
        mExecutor.execute(() -> JamiService.startRecordedFilePlayback(filepath));
        return false;
    }

    public void stopRecordedFilePlayback() {
        mExecutor.execute(JamiService::stopRecordedFilePlayback);
    }

    public void setRecordPath(final String path) {
        mExecutor.execute(() -> JamiService.setRecordPath(path));
    }

    public void sendTextMessage(final String callId, final String msg) {
        mExecutor.execute(() -> {
            Log.i(TAG, "sendTextMessage() thread running...");
            StringMap messages = new StringMap();
            messages.setRaw("text/plain", Blob.fromString(msg));
            JamiService.sendTextMessage(callId, messages, "", false);
        });
    }

    public Single<Long> sendAccountTextMessage(final String accountId, final String to, final String msg) {
        return Single.fromCallable(() -> {
            Log.i(TAG, "sendAccountTextMessage() running... " + accountId + " " + to + " " + msg);
            StringMap msgs = new StringMap();
            msgs.setRaw("text/plain", Blob.fromString(msg));
            return JamiService.sendAccountTextMessage(accountId, to, msgs);
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    public Completable cancelMessage(final String accountId, final long messageID) {
        return Completable.fromAction(() -> {
            Log.i(TAG, "CancelMessage() running...   Account ID:  " + accountId + " " + "Message ID " + " " + messageID);
            JamiService.cancelMessage(accountId, messageID);
        }).subscribeOn(Schedulers.from(mExecutor));
    }

    private Call getCurrentCallForId(String callId) {
        return currentCalls.get(callId);
    }

    /*public Call getCurrentCallForContactId(String contactId) {
        for (Call call : currentCalls.values()) {
            if (contactId.contains(call.getContact().getPrimaryNumber())) {
                return call;
            }
        }
        return null;
    }*/

    public void removeCallForId(String callId) {
        synchronized (currentCalls) {
            currentCalls.remove(callId);
            currentConferences.remove(callId);
        }
    }

    private Call addCall(String accountId, String callId, Uri from, Call.Direction direction) {
        synchronized (currentCalls) {
            Call call = currentCalls.get(callId);
            if (call == null) {
                Account account = mAccountService.getAccount(accountId);
                Contact contact = mContactService.findContact(account, from);
                Uri conversationUri = contact.getConversationUri().blockingFirst();
                Conversation conversation = conversationUri.equals(from) ? account.getByUri(from) : account.getSwarm(conversationUri.getRawRingId());
                call = new Call(callId, from.getUri(), accountId, conversation, contact, direction);
                currentCalls.put(callId, call);
            } else {
                Log.w(TAG, "Call already existed ! " + callId + " " + from);
            }
            return call;
        }
    }

    private Conference addConference(Call call) {
        String confId = call.getConfId();
        if (confId == null) {
            confId = call.getDaemonIdString();
        }
        Conference conference = currentConferences.get(confId);
        if (conference == null) {
            conference = new Conference(call);
            currentConferences.put(confId, conference);
            conferenceSubject.onNext(conference);
        }
        return conference;
    }

    private Call parseCallState(String callId, String newState) {
        Call.CallStatus callState = Call.CallStatus.fromString(newState);
        Call call = currentCalls.get(callId);
        if (call != null) {
            call.setCallState(callState);
            call.setDetails(JamiService.getCallDetails(callId).toNative());
        } else if (callState !=  Call.CallStatus.OVER && callState !=  Call.CallStatus.FAILURE) {
            Map<String, String> callDetails = JamiService.getCallDetails(callId);
            call = new Call(callId, callDetails);
            if (StringUtils.isEmpty(call.getContactNumber())) {
                Log.w(TAG, "No number");
                return null;
            }

            call.setCallState(callState);
            Account account = mAccountService.getAccount(call.getAccount());

            Contact contact = mContactService.findContact(account, Uri.fromString(call.getContactNumber()));
            String registeredName = callDetails.get(Call.KEY_REGISTERED_NAME);
            if (registeredName != null && !registeredName.isEmpty()) {
                contact.setUsername(registeredName);
            }

            Conversation conversation = account.getByUri(contact.getConversationUri().blockingFirst());
            call.setContact(contact);
            call.setConversation(conversation);
            Log.w(TAG, "parseCallState " + contact + " " + contact.getConversationUri().blockingFirst() + " " + conversation + " " + conversation.getParticipant());

            currentCalls.put(callId, call);
            updateConnectionCount();
        }
        return call;
    }

    public void connectionUpdate(String id, int state) {
        // Log.d(TAG, "connectionUpdate: " + id + " " + state);
        /*switch(state) {
            case 0:
                currentConnections.add(id);
                break;
            case 1:
            case 2:
                currentConnections.remove(id);
                break;
        }
        updateConnectionCount();*/
    }

    void callStateChanged(String callId, String newState, int detailCode) {
        Log.d(TAG, "call state changed: " + callId + ", " + newState + ", " + detailCode);
        try {
            synchronized (currentCalls) {
                Call call = parseCallState(callId, newState);
                if (call != null) {
                    callSubject.onNext(call);
                    if (call.getCallStatus() == Call.CallStatus.OVER) {
                        currentCalls.remove(call.getDaemonIdString());
                        currentConferences.remove(call.getDaemonIdString());
                        updateConnectionCount();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception during state change: ", e);
        }
    }

    void incomingCall(String accountId, String callId, String from) {
        Log.d(TAG, "incoming call: " + accountId + ", " + callId + ", " + from);

        Call call = addCall(accountId, callId, Uri.fromStringWithName(from).first, Call.Direction.INCOMING);
        callSubject.onNext(call);
        updateConnectionCount();
    }

    public void incomingMessage(String callId, String from, Map<String, String> messages) {
        Call call = currentCalls.get(callId);
        if (call == null || messages == null) {
            Log.w(TAG, "incomingMessage: unknown call or no message: " + callId + " " + from);
            return;
        }
        VCard vcard = call.appendToVCard(messages);
        if (vcard != null) {
            mContactService.saveVCardContactData(call.getContact(), call.getAccount(), vcard);
        }
        if (messages.containsKey(MIME_TEXT_PLAIN)) {
            mAccountService.incomingAccountMessage(call.getAccount(), null, callId, from, messages);
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
        mExecutor.execute(() -> JamiService.removeConference(confId));
    }

    public Single<Boolean> joinParticipant(final String selCallId, final String dragCallId) {
        return Single.fromCallable(() -> JamiService.joinParticipant(selCallId, dragCallId))
                .subscribeOn(Schedulers.from(mExecutor));
    }

    public void addParticipant(final String callId, final String confId) {
        mExecutor.execute(() -> JamiService.addParticipant(callId, confId));
    }

    public void addMainParticipant(final String confId) {
        mExecutor.execute(() -> JamiService.addMainParticipant(confId));
    }

    public void detachParticipant(final String callId) {
        mExecutor.execute(() -> JamiService.detachParticipant(callId));
    }

    public void joinConference(final String selConfId, final String dragConfId) {
        mExecutor.execute(() -> JamiService.joinConference(selConfId, dragConfId));
    }

    public void hangUpConference(final String confId) {
        mExecutor.execute(() -> JamiService.hangUpConference(confId));
    }

    public void holdConference(final String confId) {
        mExecutor.execute(() -> JamiService.holdConference(confId));
    }

    public void unholdConference(final String confId) {
        mExecutor.execute(() -> JamiService.unholdConference(confId));
    }

    public boolean isConferenceParticipant(final String callId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "isConferenceParticipant() running...");
                return JamiService.isConferenceParticipant(callId);
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
                StringVect callIds = JamiService.getCallList();
                HashMap<String, ArrayList<String>> confs = new HashMap<>(callIds.size());
                for (int i = 0; i < callIds.size(); i++) {
                    String callId = callIds.get(i);
                    String confId = JamiService.getConferenceId(callId);
                    Map<String, String> callDetails = JamiService.getCallDetails(callId).toNative();

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
                return new ArrayList<>(JamiService.getParticipantList(confId));
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error running getParticipantList()", e);
        }
        return null;
    }

    public Conference getConference(Call call) {
        return addConference(call);
    }

    public String getConferenceId(String callId) {
        return JamiService.getConferenceId(callId);
    }

    public String getConferenceState(final String callId) {
        try {
            return mExecutor.submit(() -> {
                Log.i(TAG, "getConferenceDetails() thread running...");
                return JamiService.getConferenceDetails(callId).get("CONF_STATE");
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
                return JamiService.getConferenceDetails(id).toNative();
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
        StringVect participants = JamiService.getParticipantList(confId);
        StringMap map = JamiService.getConferenceDetails(confId);
        conf.setState(map.get("STATE"));
        for (String callId : participants) {
            Call call = getCurrentCallForId(callId);
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
            for (Call call : conf.getParticipants()) {
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
            conf.setState(state);
            Set<String> participants = new HashSet<>(JamiService.getParticipantList(confId));
            // Add new participants
            for (String callId : participants) {
                if (!conf.contains(callId)) {
                    Call call = getCurrentCallForId(callId);
                    if (call != null) {
                        Log.d(TAG, "conference changed: adding participant " + callId + " " + call.getContact().getDisplayName());
                        call.setConfId(confId);
                        conf.addParticipant(call);
                    }
                    currentConferences.remove(callId);
                }
            }

            // Remove participants
            List<Call> calls = conf.getParticipants();
            Iterator<Call> i = calls.iterator();
            boolean removed = false;
            while (i.hasNext()) {
                Call call = i.next();
                if (!participants.contains(call.getDaemonIdString())) {
                    Log.d(TAG, "conference changed: removing participant " + call.getDaemonIdString() + " " + call.getContact().getDisplayName());
                    call.setConfId(null);
                    i.remove();
                    removed = true;
                }
            }

            conferenceSubject.onNext(conf);

            if (removed && conf.getParticipants().size() == 1 && conf.getConfId() != null) {
                Call call = conf.getCall();
                call.setConfId(null);
                addConference(call);
            }
        } catch (Exception e) {
            Log.w(TAG, "exception in conferenceChanged", e);
        }
    }
}
