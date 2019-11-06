/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.call;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class CallPresenter extends RootPresenter<CallView> {

    public final static String TAG = CallPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private ContactService mContactService;
    private HardwareService mHardwareService;
    private CallService mCallService;
    private DeviceRuntimeService mDeviceRuntimeService;
    private ConversationFacade mConversationFacade;

    private Conference mConference;
    private final List<SipCall> mPendingCalls = new ArrayList<>();
    private final Subject<List<SipCall>> mPendingSubject = BehaviorSubject.createDefault(mPendingCalls);

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
            mHardwareService.initVideo().blockingAwait();
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

    public void initOutGoing(String accountId, String contactRingId, boolean audioOnly) {
        if (accountId == null || contactRingId == null) {
            Log.e(TAG, "initOutGoing: null account or contact");
            hangupCall();
            return;
        }
        if (mHardwareService.getCameraCount() == 0) {
            audioOnly = true;
        }
        //getView().blockScreenRotation();

        mCompositeDisposable.add(mCallService
                .placeCall(accountId, StringUtils.toNumber(contactRingId), audioOnly)
                //.map(mCallService::getConference)
                .flatMapObservable(call -> mCallService.getConfUpdates(call))
                .observeOn(mUiScheduler)
                .subscribe(conference -> {
                    contactUpdate(conference);
                    confUpdate(conference);
                }, e -> {
                    hangupCall();
                    Log.e(TAG, "Error with initOutgoing: " + e.getMessage());
                }));
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
    }

    public void prepareOptionMenu() {
        boolean isSpeakerOn = mHardwareService.isSpeakerPhoneOn();
        //boolean hasContact = mSipCall != null && null != mSipCall.getContact() && mSipCall.getContact().isUnknown();
        boolean canDial = mOnGoingCall && mConference != null && !mConference.isIncoming();
        boolean hasMultipleCamera = mHardwareService.getCameraCount() > 1 && mOnGoingCall && !mAudioOnly;
        getView().initMenu(isSpeakerOn, hasMultipleCamera, canDial, mOnGoingCall);
    }

    public void chatClick() {
        if (mConference == null || mConference.getParticipants().isEmpty()) {
            return;
        }
        SipCall firstCall = mConference.getParticipants().get(0);
        if (firstCall == null
                || firstCall.getContact() == null
                || firstCall.getContact().getIds() == null
                || firstCall.getContact().getIds().isEmpty()) {
            return;
        }
        getView().goToConversation(firstCall.getAccount(), firstCall.getContact().getIds().get(0));
    }

    public void speakerClick(boolean checked) {
        mHardwareService.toggleSpeakerphone(checked);
    }

    public void muteMicrophoneToggled(boolean checked) {
        mCallService.setMuted(checked);
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

    /*public void addParticipantClick() {
        if(mSipCall == null)
            return;

        getView().launchAddParti
    }*/

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
        for (SipCall call : mPendingCalls) {
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
            Observable<List<SipCall>> callsObservable = mPendingSubject
                    .map(pendingList -> {
                        Log.w(TAG, "mPendingSubject onNext " + pendingList.size() + " " + conference.getParticipants().size());
                        if (pendingList.isEmpty())
                            return conference.getParticipants();
                        List<SipCall> newList = new ArrayList<>(conference.getParticipants().size() + pendingList.size());
                        newList.addAll(conference.getParticipants());
                        newList.addAll(pendingList);
                        return newList;
                    });

            // Updates of individual contacts
            Observable<List<Observable<SipCall>>> contactsObservable = callsObservable
                    .flatMapSingle(calls -> Observable
                            .fromIterable(calls)
                            .map(call -> mContactService.observeContact(call.getAccount(), call.getContact())
                                    .map(contact -> call))
                            .toList(calls.size()));

            // Combined updates of contacts as participant list updates
            Observable<List<SipCall>> contactUpdates = contactsObservable
                    .switchMap(list -> Observable
                            .combineLatest(list, objects -> {
                                Log.w(TAG, "flatMapObservable " + objects.length);
                                ArrayList<SipCall> calls = new ArrayList<>(objects.length);
                                for (Object call : objects)
                                    calls.add((SipCall)call);
                                return (List<SipCall>)calls;
                            }))
                    .filter(list -> !list.isEmpty());

            contactDisposable = contactUpdates
                    .observeOn(mUiScheduler)
                    .subscribe(cs -> getView().updateContactBubble(cs), e -> Log.e(TAG, "Error updating contact data", e));
            mCompositeDisposable.add(contactDisposable);
        }
    }

    private void confUpdate(Conference call) {
        Log.w(TAG, "confUpdate " + call.getId());

        mConference = call;
        mAudioOnly = !call.hasVideo();
        CallView view = getView();
        if (view == null)
            return;
        view.updateMenu();
        if (call.isOnGoing()) {
            Log.w(TAG, "confUpdate call.isOnGoing");

            mOnGoingCall = true;
            view.initNormalStateDisplay(mAudioOnly, isMicrophoneMuted());
            view.updateMenu();
            if (!mAudioOnly) {
                mHardwareService.setPreviewSettings();
                videoSurfaceUpdateId(call.getId());
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
            Log.w(TAG, "confUpdate call.isRinging");
            SipCall scall = call.getCall();

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
        if (!(mConference.isRinging() && mConference.isIncoming())) {
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

    public boolean isSpeakerphoneOn() {
        return mHardwareService.isSpeakerPhoneOn();
    }

    public void sendDtmf(CharSequence s) {
        mCallService.playDtmf(s.toString());
    }

    public void addConferenceParticipant(String accountId, String contactId) {
        String destCallId = mConference.getId();

        mCompositeDisposable.add(mConversationFacade.startConversation(accountId, new Uri(contactId))
                .map(Conversation::getCurrentCalls)
                .subscribe(calls -> {
                    if (calls.isEmpty()) {
                        final Observer<SipCall>  pendingObserver = new Observer<SipCall>() {
                            private SipCall call = null;
                            @Override
                            public void onSubscribe(Disposable d) {}

                            @Override
                            public void onNext(SipCall sipCall) {
                                Log.w(TAG, "placeCallObservable onNext " + sipCall.getCallStatus());
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
                                Log.w(TAG, "placeCallObservable onComplete " + call);
                                if (call != null) {
                                    mPendingCalls.remove(call);
                                    mPendingSubject.onNext(mPendingCalls);
                                    call = null;
                                }
                            }
                        };

                        // Place new call, join to conference when answered
                        Maybe<SipCall> newCall = mCallService.placeCallObservable(accountId, contactId, mAudioOnly)
                                .doOnEach(pendingObserver)
                                .filter(SipCall::isOnGoing)
                                .firstElement()
                                .delay(1, TimeUnit.SECONDS)
                                .doOnEvent((v, e) -> pendingObserver.onComplete());
                        if (mConference.getParticipants().size() > 1) {
                            mCompositeDisposable.add(newCall.subscribe(call -> mCallService.joinConference(destCallId, call.getDaemonIdString())));
                        } else {
                            mCompositeDisposable.add(newCall.subscribe(call -> mCallService.joinParticipant(destCallId, call.getDaemonIdString())));
                        }

                    } else {
                        // Selected contact already in call or conference, join it to current conference
                        Conference call = calls.get(0);
                        if (call != mConference) {
                            if (mConference.getParticipants().size() > 1) {
                                mCallService.joinConference(destCallId, call.getId());
                            } else {
                                mCallService.joinParticipant(destCallId, call.getId());
                            }
                        }
                    }
                }));
    }

    public void startAddParticipant() {
        getView().startAddParticipant(mConference.getId());
    }

    public void hangupParticipant(SipCall call) {
        mCallService.hangUp(call.getDaemonIdString());
    }

    public void openParticipantContact(SipCall call) {
        getView().goToContact(call.getAccount(), call.getContact());
    }
}
