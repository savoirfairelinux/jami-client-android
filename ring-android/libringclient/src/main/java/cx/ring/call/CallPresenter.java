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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.SipCall;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;

public class CallPresenter extends RootPresenter<CallView> {

    public final static String TAG = CallPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private ContactService mContactService;
    private NotificationService mNotificationService;
    private HardwareService mHardwareService;
    private CallService mCallService;
    private DeviceRuntimeService mDeviceRuntimeService;

    private SipCall mSipCall;
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

    private Disposable timeUpdateTask = null;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public CallPresenter(AccountService accountService,
                         ContactService contactService,
                         NotificationService notificationService,
                         HardwareService hardwareService,
                         CallService callService, DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mContactService = contactService;
        mNotificationService = notificationService;
        mHardwareService = hardwareService;
        mCallService = callService;
        mDeviceRuntimeService = deviceRuntimeService;
    }

    public void cameraPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.isVideoAvailable()) {
            mHardwareService.initVideo();
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
        mCompositeDisposable.add(mAccountService.getRegisteredNames()
                .observeOn(mUiScheduler)
                .subscribe(r -> {
                    if (mSipCall != null && mSipCall.getContact() != null) {
                        getView().updateContactBubble(mSipCall.getContact());
                    }
                }));
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
                .placeCallObservable(accountId, StringUtils.toNumber(contactRingId), audioOnly)
                .observeOn(mUiScheduler)
                .subscribe(call -> {
                    contactUpdate(call);
                    confUpdate(call);
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

        Observable<SipCall> callObservable = mCallService.getCallUpdates(confId).observeOn(mUiScheduler).share();

        // Handles the case where the call has been accepted, emits a single so as to only check for permissions and start the call once
        mCompositeDisposable.add(callObservable.firstOrError().subscribe(call -> {
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
        mCompositeDisposable.add(callObservable.subscribe(call -> {
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
        boolean hasContact = mSipCall != null && null != mSipCall.getContact() && mSipCall.getContact().isUnknown();
        boolean canDial = mOnGoingCall && mSipCall != null && !mSipCall.isIncoming();
        boolean hasMultipleCamera = mHardwareService.getCameraCount() > 1 && mOnGoingCall && !mAudioOnly;
        getView().initMenu(isSpeakerOn, hasContact, hasMultipleCamera, canDial, mOnGoingCall);
    }

    public void chatClick() {
        if (mSipCall == null
                || mSipCall.getContact() == null
                || mSipCall.getContact().getIds() == null
                || mSipCall.getContact().getIds().isEmpty()) {
            return;
        }
        getView().goToConversation(mSipCall.getAccount(), mSipCall.getContact().getIds().get(0));
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
        if(mSipCall == null)
            return;

        mHardwareService.switchInput(mSipCall.getCallId(), false);
        getView().switchCameraIcon(mHardwareService.isPreviewFromFrontCamera());
    }

    public void configurationChanged(int rotation) {
        mHardwareService.setDeviceOrientation(rotation);
    }

    public void dialpadClick() {
        getView().displayDialPadKeyboard();
    }

    public void acceptCall() {
        if (mSipCall == null) {
            return;
        }
        mCallService.accept(mSipCall.getCallId());
    }

    public void hangupCall() {
        if (mSipCall != null) {
            mCallService.hangUp(mSipCall.getCallId());
        }
        finish();
    }

    public void refuseCall() {
        final SipCall call = mSipCall;
        if (call != null) {
            mCallService.refuse(call.getCallId());
        }
        finish();
    }

    public void videoSurfaceCreated(Object holder) {
        if (mSipCall == null) {
            return;
        }
        mHardwareService.addVideoSurface(mSipCall.getCallId(), holder);
        getView().displayContactBubble(false);
    }

    public void previewVideoSurfaceCreated(Object holder) {
        mHardwareService.addPreviewVideoSurface(holder, mSipCall);
        //mHardwareService.startCapture(null);
    }

    public void videoSurfaceDestroyed() {
        if (mSipCall == null) {
            return;
        }
        mHardwareService.removeVideoSurface(mSipCall.getCallId());
    }

    public void previewVideoSurfaceDestroyed() {
        mHardwareService.removePreviewVideoSurface();
        mHardwareService.endCapture();
    }

    public void displayChanged() {
        mHardwareService.switchInput(mSipCall.getCallId(), false);
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
        mSipCall = null;
        CallView view = getView();
        if (view != null)
            view.finish();
    }

    private void contactUpdate(final SipCall call) {
        if (mSipCall != call) {
            mSipCall = call;
            mCompositeDisposable.add(mContactService.observeContact(call.getAccount(), call.getContact())
                    .observeOn(mUiScheduler)
                    .subscribe(c -> getView().updateContactBubble(c), e -> Log.e(TAG, e.getMessage())));
        }
    }

    private void confUpdate(SipCall call) {
        mAudioOnly = call.isAudioOnly();
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
                view.displayVideoSurface(true, mDeviceRuntimeService.hasVideoPermission());
                if (permissionChanged) {
                    mHardwareService.switchInput(mSipCall.getCallId(), permissionChanged);
                    permissionChanged = false;
                }
            }
            if (timeUpdateTask != null)
                timeUpdateTask.dispose();
            timeUpdateTask = mUiScheduler.schedulePeriodicallyDirect(this::updateTime, 0, 1, TimeUnit.SECONDS);
        } else if (call.isRinging()) {
            view.handleCallWakelock(mAudioOnly);
            if (call.isIncoming()) {
                if (mAccountService.getAccount(call.getAccount()).isAutoanswerEnabled()) {
                    mCallService.accept(call.getCallId());
                    // only display the incoming call screen if the notification is a full screen intent
                } else if (incomingIsFullIntent) {
                    view.initIncomingCallDisplay();
                }
            } else {
                mOnGoingCall = false;
                view.updateCallStatus(call.getCallState());
                view.initOutGoingCallDisplay();
            }
        } else {
            finish();
        }
    }

    private void updateTime() {
        CallView view = getView();
        if (view != null && mSipCall != null) {
            long duration = System.currentTimeMillis() - mSipCall.getTimestampStart();
            duration = duration / 1000;
            if (mSipCall.isOnGoing()) {
                view.updateTime(duration);
            }
        }
    }

    private void onVideoEvent(HardwareService.VideoEvent event) {
        Log.d(TAG, "VIDEO_EVENT: " + event.start + " " + event.callId + " " + event.w + "x" + event.h);

        if (event.start) {
            getView().displayVideoSurface(true, !isPipMode() && mDeviceRuntimeService.hasVideoPermission());
        } else if (mSipCall != null && mSipCall.getCallId().equals(event.callId)) {
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
        if (mSipCall.isRinging() && mSipCall.isIncoming()) {
            acceptCall();
        } else {
            hangupCall();
        }
    }

    public void negativeButtonClicked() {
        if (mSipCall.isRinging() && mSipCall.isIncoming()) {
            refuseCall();
        } else {
            hangupCall();
        }
    }

    public void toggleButtonClicked() {
        if (!(mSipCall.isRinging() && mSipCall.isIncoming())) {
            hangupCall();
        }
    }

    public boolean isAudioOnly() {
        return mAudioOnly;
    }

    public void requestPipMode() {
        if (mSipCall != null && mSipCall.isOnGoing() && !mSipCall.isAudioOnly()) {
            getView().enterPipMode(mSipCall);
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
}
