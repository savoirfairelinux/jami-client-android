/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package net.jami.call;

import net.jami.daemon.JamiService;
import net.jami.facades.ConversationFacade;
import net.jami.model.Account;
import net.jami.model.Call;
import net.jami.model.Conference;
import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.ConversationHistory;
import net.jami.model.Uri;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.services.CallService;
import net.jami.services.ContactService;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class CallPresenter extends RootPresenter<CallView> {

    public final static String TAG = CallPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ContactService mContactService;
    private final HardwareService mHardwareService;
    private final CallService mCallService;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final ConversationFacade mConversationFacade;

    private Conference mConference;
    private final List<Call> mPendingCalls = new ArrayList<>();
    private final Subject<List<Call>> mPendingSubject = BehaviorSubject.createDefault(mPendingCalls);

    private boolean mOnGoingCall = false;
    private boolean mAudioOnly = true;
    private boolean permissionChanged = false;
    private boolean pipIsActive = false;
    private boolean incomingIsFullIntent = true;
    private boolean callInitialized = false;

    private int videoWidth = -1;
    private int videoHeight = -1;
    private int previewWidth = -1;
    private int previewHeight = -1;
    private String currentSurfaceId = null;
    private String currentPluginSurfaceId = null;

    private Disposable timeUpdateTask = null;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public CallPresenter(AccountService accountService,
                         ContactService contactService,
                         HardwareService hardwareService,
                         CallService callService,
                         DeviceRuntimeService deviceRuntimeService,
                         ConversationFacade conversationFacade) {
        mAccountService = accountService;
        mContactService = contactService;
        mHardwareService = hardwareService;
        mCallService = callService;
        mDeviceRuntimeService = deviceRuntimeService;
        mConversationFacade = conversationFacade;
    }

    public void cameraPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.isVideoAvailable()) {
            mHardwareService.initVideo()
                    .onErrorComplete()
                    .blockingAwait();
            permissionChanged = true;
        }
    }

    public void audioPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.hasMicrophone()) {
            mCallService.restartAudioLayer();
        }
    }


    @Override
    public void unbindView() {
        if (!mAudioOnly) {
            mHardwareService.endCapture();
        }
        super.unbindView();
    }

    @Override
    public void bindView(CallView view) {
        super.bindView(view);
        /*mCompositeDisposable.add(mAccountService.getRegisteredNames()
                .observeOn(mUiScheduler)
                .subscribe(r -> {
                    if (mSipCall != null && mSipCall.getContact() != null) {
                        getView().updateContactBubble(mSipCall.getContact());
                    }
                }));*/
        mCompositeDisposable.add(mHardwareService.getVideoEvents()
                .observeOn(mUiScheduler)
                .subscribe(this::onVideoEvent));
        mCompositeDisposable.add(mHardwareService.getAudioState()
                .observeOn(mUiScheduler)
                .subscribe(state -> getView().updateAudioState(state)));

        /*mCompositeDisposable.add(mHardwareService
                .getBluetoothEvents()
                .subscribe(event -> {
                    if (!event.connected && mSipCall == null) {
                        hangupCall();
                    }
                }));*/
    }

    public void initOutGoing(String accountId, Uri conversationUri, String contactUri, boolean audioOnly) {
        if (accountId == null || contactUri == null) {
            Log.e(TAG, "initOutGoing: null account or contact");
            hangupCall();
            return;
        }
        if (!mHardwareService.hasCamera()) {
            audioOnly = true;
        }
        //getView().blockScreenRotation();

        Observable<Conference> callObservable = mCallService
                .placeCall(accountId, conversationUri, Uri.fromString(StringUtils.toNumber(contactUri)), audioOnly)
                //.map(mCallService::getConference)
                .flatMapObservable(mCallService::getConfUpdates)
                .share();

        mCompositeDisposable.add(callObservable
                .observeOn(mUiScheduler)
                .subscribe(conference -> {
                    contactUpdate(conference);
                    confUpdate(conference);
                }, e -> {
                    hangupCall();
                    Log.e(TAG, "Error with initOutgoing: " + e.getMessage());
                }));

        showConference(callObservable);
    }

    /**
     * Returns to or starts an incoming call
     *
     * @param confId         the call id
     * @param actionViewOnly true if only returning to call or if using full screen intent
     */
    public void initIncomingCall(String confId, boolean actionViewOnly) {
        //getView().blockScreenRotation();

        // if the call is incoming through a full intent, this allows the incoming call to display
        incomingIsFullIntent = actionViewOnly;

        Observable<Conference> callObservable = mCallService.getConfUpdates(confId)
                .observeOn(mUiScheduler)
                .share();

        // Handles the case where the call has been accepted, emits a single so as to only check for permissions and start the call once
        mCompositeDisposable.add(callObservable
                .firstOrError()
                .subscribe(call -> {
                    if (!actionViewOnly) {
                        contactUpdate(call);
                        confUpdate(call);
                        callInitialized = true;
                        getView().prepareCall(true);
                    }
                }, e -> {
                    hangupCall();
                    Log.e(TAG, "Error with initIncoming, preparing call flow :" , e);
                }));

        // Handles retrieving call updates. Items emitted are only used if call is already in process or if user is returning to a call.
        mCompositeDisposable.add(callObservable
                .subscribe(call -> {
                    if (callInitialized || actionViewOnly) {
                        contactUpdate(call);
                        confUpdate(call);
                    }
                }, e -> {
                    hangupCall();
                    Log.e(TAG, "Error with initIncoming, action view flow: ", e);
                }));

        showConference(callObservable);
    }

    private void showConference(Observable<Conference> conference) {
        conference = conference
                .distinctUntilChanged();
        mCompositeDisposable.add(conference
                .switchMap(Conference::getParticipantInfo)
                .observeOn(mUiScheduler)
                .subscribe(info -> getView().updateConfInfo(info),
                        e -> Log.e(TAG, "Error with initIncoming, action view flow: ", e)));

        mCompositeDisposable.add(conference
                .switchMap(Conference::getParticipantRecording)
                .observeOn(mUiScheduler)
                .subscribe(contacts -> getView().updateParticipantRecording(contacts),
                        e -> Log.e(TAG, "Error with initIncoming, action view flow: ", e)));
    }

    public void prepareOptionMenu() {
        boolean isSpeakerOn = mHardwareService.isSpeakerPhoneOn();
        //boolean hasContact = mSipCall != null && null != mSipCall.getContact() && mSipCall.getContact().isUnknown();
        boolean canDial = mOnGoingCall && mConference != null;
        // get the preferences
        boolean displayPluginsButton = getView().displayPluginsButton();
        boolean showPluginBtn = displayPluginsButton && mOnGoingCall && mConference != null;
        boolean hasMultipleCamera = mHardwareService.getCameraCount() > 1 && mOnGoingCall && !mAudioOnly;
        getView().initMenu(isSpeakerOn, hasMultipleCamera, canDial, showPluginBtn, mOnGoingCall);
    }

    public void chatClick() {
        if (mConference == null || mConference.getParticipants().isEmpty()) {
            return;
        }
        Call firstCall = mConference.getParticipants().get(0);
        if (firstCall == null) {
            return;
        }
        ConversationHistory c = firstCall.getConversation();
        if (c instanceof Conversation) {
            Conversation conversation = ((Conversation) c);
            getView().goToConversation(conversation.getAccountId(), conversation.getUri());
        } else if (firstCall.getContact() != null) {
            getView().goToConversation(firstCall.getAccount(), firstCall.getContact().getConversationUri().blockingFirst());
        }
    }

    public void speakerClick(boolean checked) {
        mHardwareService.toggleSpeakerphone(checked);
    }

    public void muteMicrophoneToggled(boolean checked) {
        mCallService.setLocalMediaMuted(mConference.getId(), CallService.MEDIA_TYPE_AUDIO, checked);
    }


    public boolean isMicrophoneMuted() {
        return mCallService.isCaptureMuted();
    }

    public void switchVideoInputClick() {
        if(mConference == null)
            return;
        mHardwareService.switchInput(mConference.getId(), false);
        getView().switchCameraIcon(mHardwareService.isPreviewFromFrontCamera());
    }

    public void configurationChanged(int rotation) {
        mHardwareService.setDeviceOrientation(rotation);
    }

    public void dialpadClick() {
        getView().displayDialPadKeyboard();
    }

    public void acceptCall() {
        if (mConference == null) {
            return;
        }
        mCallService.accept(mConference.getId());
    }

    public void hangupCall() {
        if (mConference != null) {
            if (mConference.isConference())
                mCallService.hangUpConference(mConference.getId());
            else
                mCallService.hangUp(mConference.getId());
        }
        for (Call call : mPendingCalls) {
            mCallService.hangUp(call.getDaemonIdString());
        }
        finish();
    }

    public void refuseCall() {
        final Conference call = mConference;
        if (call != null) {
            mCallService.refuse(call.getId());
        }
        finish();
    }

    public void videoSurfaceCreated(Object holder) {
        if (mConference == null) {
            return;
        }
        String newId = mConference.getId();
        if (!newId.equals(currentSurfaceId)) {
            mHardwareService.removeVideoSurface(currentSurfaceId);
            currentSurfaceId = newId;
        }
        mHardwareService.addVideoSurface(mConference.getId(), holder);
        getView().displayContactBubble(false);
    }

    public void videoSurfaceUpdateId(String newId) {
        if (!Objects.equals(newId, currentSurfaceId)) {
            mHardwareService.updateVideoSurfaceId(currentSurfaceId, newId);
            currentSurfaceId = newId;
        }
    }

    public void pluginSurfaceCreated(Object holder) {
        if (mConference == null) {
            return;
        }
        String newId = mConference.getPluginId();
        if (!newId.equals(currentPluginSurfaceId)) {
            mHardwareService.removeVideoSurface(currentPluginSurfaceId);
            currentPluginSurfaceId = newId;
        }
        mHardwareService.addVideoSurface(mConference.getPluginId(), holder);
        getView().displayContactBubble(false);
    }

    public void pluginSurfaceUpdateId(String newId) {
        if (!Objects.equals(newId, currentPluginSurfaceId)) {
            mHardwareService.updateVideoSurfaceId(currentPluginSurfaceId, newId);
            currentPluginSurfaceId = newId;
        }
    }

    public void previewVideoSurfaceCreated(Object holder) {
        mHardwareService.addPreviewVideoSurface(holder, mConference);
        //mHardwareService.startCapture(null);
    }

    public void videoSurfaceDestroyed() {
        if (currentSurfaceId != null) {
            mHardwareService.removeVideoSurface(currentSurfaceId);
            currentSurfaceId = null;
        }
    }
    public void pluginSurfaceDestroyed() {
        if (currentPluginSurfaceId != null) {
            mHardwareService.removeVideoSurface(currentPluginSurfaceId);
            currentPluginSurfaceId = null;
        }
    }
    public void previewVideoSurfaceDestroyed() {
        mHardwareService.removePreviewVideoSurface();
        mHardwareService.endCapture();
    }

    public void displayChanged() {
        mHardwareService.switchInput(mConference.getId(), false);
    }

    public void layoutChanged() {
        //getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
    }


    public void uiVisibilityChanged(boolean displayed) {
        Log.w(TAG, "uiVisibilityChanged " + mOnGoingCall + " "  + displayed);
        CallView view = getView();
        if (view != null)
            view.displayHangupButton(mOnGoingCall && displayed);
    }

    private void finish() {
        if (timeUpdateTask != null && !timeUpdateTask.isDisposed()) {
            timeUpdateTask.dispose();
            timeUpdateTask = null;
        }
        mConference = null;
        CallView view = getView();
        if (view != null)
            view.finish();
    }

    private Disposable contactDisposable = null;

    private void contactUpdate(final Conference conference) {
        if (mConference != conference) {
            mConference = conference;
            if (contactDisposable != null && !contactDisposable.isDisposed()) {
                contactDisposable.dispose();
            }
            if (conference.getParticipants().isEmpty())
                return;

            // Updates of participant (and  pending participant) list
            Observable<List<Call>> callsObservable = mPendingSubject
                    .map(pendingList -> {
                        Log.w(TAG, "mPendingSubject onNext " + pendingList.size() + " " + conference.getParticipants().size());
                        if (pendingList.isEmpty())
                            return conference.getParticipants();
                        List<Call> newList = new ArrayList<>(conference.getParticipants().size() + pendingList.size());
                        newList.addAll(conference.getParticipants());
                        newList.addAll(pendingList);
                        return newList;
                    });

            // Updates of individual contacts
            Observable<List<Observable<Call>>> contactsObservable = callsObservable
                    .flatMapSingle(calls -> Observable
                            .fromIterable(calls)
                            .map(call -> mContactService.observeContact(call.getAccount(), call.getContact(), false)
                                    .map(contact -> call))
                            .toList(calls.size()));

            // Combined updates of contacts as participant list updates
            Observable<List<Call>> contactUpdates = contactsObservable
                    .switchMap(list -> Observable
                            .combineLatest(list, objects -> {
                                Log.w(TAG, "flatMapObservable " + objects.length);
                                ArrayList<Call> calls = new ArrayList<>(objects.length);
                                for (Object call : objects)
                                    calls.add((Call)call);
                                return (List<Call>)calls;
                            }))
                    .filter(list -> !list.isEmpty());

            contactDisposable = contactUpdates
                    .observeOn(mUiScheduler)
                    .subscribe(cs -> getView().updateContactBubble(cs), e -> Log.e(TAG, "Error updating contact data", e));
            mCompositeDisposable.add(contactDisposable);
        }
        mPendingSubject.onNext(mPendingCalls);
    }

    private void confUpdate(Conference call) {
        Log.w(TAG, "confUpdate " + call.getId() + " " + call.getState());

        Call.CallStatus status = call.getState();
        if (status == Call.CallStatus.HOLD) {
            if (call.isSimpleCall())
                mCallService.unhold(call.getId());
            else
                JamiService.addMainParticipant(call.getConfId());
        }
        mAudioOnly = !call.hasVideo();
        CallView view = getView();
        if (view == null)
            return;
        view.updateMenu();
        if (call.isOnGoing()) {
            mOnGoingCall = true;
            view.initNormalStateDisplay(mAudioOnly, isMicrophoneMuted());
            view.updateMenu();
            if (!mAudioOnly) {
                mHardwareService.setPreviewSettings();
                mHardwareService.updatePreviewVideoSurface(call);
                videoSurfaceUpdateId(call.getId());
                pluginSurfaceUpdateId(call.getPluginId());
                view.displayVideoSurface(true, mDeviceRuntimeService.hasVideoPermission());
                if (permissionChanged) {
                    mHardwareService.switchInput(mConference.getId(), permissionChanged);
                    permissionChanged = false;
                }
            }
            if (timeUpdateTask != null)
                timeUpdateTask.dispose();
            timeUpdateTask = mUiScheduler.schedulePeriodicallyDirect(this::updateTime, 0, 1, TimeUnit.SECONDS);
        } else if (call.isRinging()) {
            Call scall = call.getCall();

            view.handleCallWakelock(mAudioOnly);
            if (scall.isIncoming()) {
                if (mAccountService.getAccount(scall.getAccount()).isAutoanswerEnabled()) {
                    mCallService.accept(scall.getDaemonIdString());
                    // only display the incoming call screen if the notification is a full screen intent
                } else if (incomingIsFullIntent) {
                    view.initIncomingCallDisplay();
                }
            } else {
                mOnGoingCall = false;
                view.updateCallStatus(scall.getCallStatus());
                view.initOutGoingCallDisplay();
            }
        } else {
            finish();
        }
    }

    public void maximizeParticipant(Conference.ParticipantInfo info) {
        Contact contact = info == null ? null : info.contact;
        if (mConference.getMaximizedParticipant() == contact)
            info = null;
        mConference.setMaximizedParticipant(contact);
        if (info != null) {
            mCallService.setConfMaximizedParticipant(mConference.getConfId(), info.contact.getUri());
        } else {
            mCallService.setConfGridLayout(mConference.getConfId());
        }
    }

    private void updateTime() {
        CallView view = getView();
        if (view != null && mConference != null) {
            if (mConference.isOnGoing()) {
                long start = mConference.getTimestampStart();
                if (start != Long.MAX_VALUE) {
                    view.updateTime((System.currentTimeMillis() - start) / 1000);
                } else {
                    view.updateTime(-1);
                }
            }
        }
    }

    private void onVideoEvent(HardwareService.VideoEvent event) {
        Log.d(TAG, "VIDEO_EVENT: " + event.start + " " + event.callId + " " + event.w + "x" + event.h);

        if (event.start) {
            getView().displayVideoSurface(true, !isPipMode() && mDeviceRuntimeService.hasVideoPermission());
        } else if (mConference != null && mConference.getId().equals(event.callId)) {
            getView().displayVideoSurface(event.started, event.started && !isPipMode() && mDeviceRuntimeService.hasVideoPermission());
            if (event.started) {
                videoWidth = event.w;
                videoHeight = event.h;
                getView().resetVideoSize(videoWidth, videoHeight);
            }
        } else if (event.callId == null) {
            if (event.started) {
                previewWidth = event.w;
                previewHeight = event.h;
                getView().resetPreviewVideoSize(previewWidth, previewHeight, event.rot);
            }
        }
        if (mConference != null && mConference.getPluginId().equals(event.callId)) {
            if (event.started) {
                previewWidth = event.w;
                previewHeight = event.h;
                getView().resetPluginPreviewVideoSize(previewWidth, previewHeight, event.rot);
            }
        }
        /*if (event.started || event.start) {
            getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
        }*/
    }

    public void positiveButtonClicked() {
        if (mConference.isRinging() && mConference.isIncoming()) {
            acceptCall();
        } else {
            hangupCall();
        }
    }

    public void negativeButtonClicked() {
        if (mConference.isRinging() && mConference.isIncoming()) {
            refuseCall();
        } else {
            hangupCall();
        }
    }

    public void toggleButtonClicked() {
        if (mConference != null && !(mConference.isRinging() && mConference.isIncoming())) {
            hangupCall();
        }
    }

    public boolean isAudioOnly() {
        return mAudioOnly;
    }

    public void requestPipMode() {
        if (mConference != null && mConference.isOnGoing() && mConference.hasVideo()) {
            getView().enterPipMode(mConference.getId());
        }
    }

    public boolean isPipMode() {
        return pipIsActive;
    }

    public void pipModeChanged(boolean pip) {
        pipIsActive = pip;
        if (pip) {
            getView().displayHangupButton(false);
            getView().displayPreviewSurface(false);
            getView().displayVideoSurface(true, false);
        } else {
            getView().displayPreviewSurface(true);
            getView().displayVideoSurface(true, mDeviceRuntimeService.hasVideoPermission());
        }
    }

    public void toggleCallMediaHandler(String id, boolean toggle)
    {
        if (mConference != null && mConference.isOnGoing() && mConference.hasVideo()) {
            getView().toggleCallMediaHandler(id, mConference.getId(), toggle);
        }
    }

    public boolean isSpeakerphoneOn() {
        return mHardwareService.isSpeakerPhoneOn();
    }

    public void sendDtmf(CharSequence s) {
        mCallService.playDtmf(s.toString());
    }

    public void addConferenceParticipant(String accountId, Uri uri) {
        mCompositeDisposable.add(mConversationFacade.startConversation(accountId, uri)
                .map(Conversation::getCurrentCalls)
                .subscribe(confs -> {
                    if (confs.isEmpty()) {
                        final Observer<Call> pendingObserver = new Observer<Call>() {
                            private Call call = null;

                            @Override
                            public void onSubscribe(@NonNull Disposable d) {}

                            @Override
                            public void onNext(@NonNull Call sipCall) {
                                if (call == null) {
                                    call = sipCall;
                                    mPendingCalls.add(sipCall);
                                }
                                mPendingSubject.onNext(mPendingCalls);
                            }

                            @Override
                            public void onError(Throwable e) {}

                            @Override
                            public void onComplete() {
                                if (call != null) {
                                    mPendingCalls.remove(call);
                                    mPendingSubject.onNext(mPendingCalls);
                                    call = null;
                                }
                            }
                        };

                        Uri contactUri = uri;
                        if (uri.isSwarm()) {
                            Account account = mAccountService.getAccount(accountId);
                            Conversation conversation = account.getSwarm(uri.getRawRingId());
                            if (conversation != null) {
                                contactUri = conversation.getContact().getUri();
                            }
                        }

                        // Place new call, join to conference when answered
                        Maybe<Call> newCall = mCallService.placeCallObservable(accountId, null, contactUri, mAudioOnly)
                                .doOnEach(pendingObserver)
                                .filter(Call::isOnGoing)
                                .firstElement()
                                .delay(1, TimeUnit.SECONDS)
                                .doOnEvent((v, e) -> pendingObserver.onComplete());
                        mCompositeDisposable.add(newCall.subscribe(call ->  {
                            String id = mConference.getId();
                            if (mConference.isConference()) {
                                mCallService.addParticipant(call.getDaemonIdString(), id);
                            } else {
                                mCallService.joinParticipant(id, call.getDaemonIdString()).subscribe();
                            }
                        }));
                    } else {
                        // Selected contact already in call or conference, join it to current conference
                        Conference selectedConf = confs.get(0);
                        if (selectedConf != mConference) {
                            if (mConference.isConference()) {
                                if (selectedConf.isConference())
                                    mCallService.joinConference(mConference.getId(), selectedConf.getId());
                                else
                                    mCallService.addParticipant(selectedConf.getId(), mConference.getId());
                            } else {
                                if (selectedConf.isConference())
                                    mCallService.addParticipant(mConference.getId(), selectedConf.getId());
                                else
                                    mCallService.joinParticipant(mConference.getId(), selectedConf.getId()).subscribe();
                            }
                        }
                    }
                }));
    }

    public void startAddParticipant() {
        getView().startAddParticipant(mConference.getId());
    }

    public void hangupParticipant(Conference.ParticipantInfo info) {
        if (info.call != null)
            mCallService.hangUp(info.call.getDaemonIdString());
        else
            mCallService.hangupParticipant(mConference.getId(), info.contact.getPrimaryNumber());
    }

    public void muteParticipant(Conference.ParticipantInfo info, boolean mute) {
        mCallService.muteParticipant(mConference.getId(), info.contact.getPrimaryNumber(), mute);
    }

    public void openParticipantContact(Conference.ParticipantInfo info) {
        Call call = info.call == null ? mConference.getFirstCall() : info.call;
        getView().goToContact(call.getAccount(), info.contact);
    }

    public void stopCapture() {
        mHardwareService.stopCapture();
    }

    public boolean startScreenShare(Object mediaProjection) {
        return mHardwareService.startScreenShare(mediaProjection);
    }

    public void stopScreenShare() {
        mHardwareService.stopScreenShare();
    }

    public boolean isMaximized(Conference.ParticipantInfo info) {
        return mConference.getMaximizedParticipant() == info.contact;
    }

    public void startPlugin(String mediaHandlerId) {
        mHardwareService.startMediaHandler(mediaHandlerId);
        if(mConference == null)
            return;
        mHardwareService.switchInput(mConference.getId(), mHardwareService.isPreviewFromFrontCamera());
    }

    public void stopPlugin() {
        mHardwareService.stopMediaHandler();
        if(mConference == null)
            return;
        mHardwareService.switchInput(mConference.getId(), mHardwareService.isPreviewFromFrontCamera());
    }

}
