/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.StringUtils;

public class CallPresenter extends RootPresenter<CallView> implements Observer<ServiceEvent> {

    public final static String TAG = CallPresenter.class.getSimpleName();

    protected AccountService mAccountService;
    protected NotificationService mNotificationService;
    protected HardwareService mHardwareService;
    protected CallService mCallService;
    protected ContactService mContactService;
    protected HistoryService mHistoryService;

    private SipCall mSipCall;
    private boolean mOnGoingCall = false;
    private boolean mAudioOnly = true;

    private int videoWidth = -1;
    private int videoHeight = -1;
    private int previewWidth = -1;
    private int previewHeight = -1;

    private ScheduledExecutorService executor;
    private Runnable timeRunnable = new Runnable() {
        public void run() {
            updateTime();
        }
    };

    @Inject
    public CallPresenter(AccountService accountService,
                         NotificationService notificationService,
                         HardwareService hardwareService,
                         CallService callService,
                         ContactService contactService,
                         HistoryService historyService) {
        this.mAccountService = accountService;
        this.mNotificationService = notificationService;
        this.mHardwareService = hardwareService;
        this.mCallService = callService;
        this.mContactService = contactService;
        this.mHistoryService = historyService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mCallService.removeObserver(this);
        mHardwareService.removeObserver(this);

        if (!mAudioOnly) {
            mHardwareService.stopCapture();
        }
    }

    @Override
    public void bindView(CallView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mCallService.addObserver(this);
        mHardwareService.addObserver(this);
    }

    public void initOutGoing(String accountId, String contactRingId, boolean audioOnly) {
        if (mHardwareService.getCameraCount() == 0) {
            audioOnly = true;
        }

        mSipCall = mCallService.placeCall(accountId, StringUtils.toNumber(contactRingId), audioOnly);
        if (mSipCall == null) {
            Log.w(TAG, "initOutGoing: null Call");
            finish();
            return;
        }

        mAudioOnly = mSipCall.isAudioOnly();

        getView().updateMenu();

        confUpdate();
        getContactDetails();
        getView().blockScreenRotation();
    }

    public void initIncoming(String confId) {
        mSipCall = mCallService.getCurrentCallForId(confId);
        if (mSipCall == null) {
            Log.w(TAG, "initIncoming: null Call");
            finish();
            return;
        }
        mAudioOnly = mSipCall.isAudioOnly();
        confUpdate();
        getContactDetails();
        getView().blockScreenRotation();
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
        getView().goToConversation(mAccountService.getCurrentAccount().getAccountID(), mSipCall.getContact().getIds().get(0));
    }

    public void addContact() {
        if (mSipCall == null || mSipCall.getContact() == null) {
            return;
        }
        getView().goToAddContact(mSipCall.getContact());
    }

    public void speakerClick() {
        mHardwareService.switchSpeakerPhone();
    }

    public void switchVideoInputClick() {
        mHardwareService.switchInput(mSipCall.getCallId());
        getView().switchCameraIcon(mHardwareService.isPreviewFromFrontCamera());
    }

    public void screenRotationClick() {
        getView().changeScreenRotation();
    }

    public void configurationChanged() {
        mHardwareService.restartCamera(mSipCall.getCallId());
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
        if (mSipCall != null) {
            mCallService.refuse(mSipCall.getCallId());
            mNotificationService.cancelCallNotification(mSipCall.getCallId().hashCode());
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
        mHardwareService.addPreviewVideoSurface(holder);
        mHardwareService.startCapture(null);
    }

    public void videoSurfaceDestroyed() {
        if (mSipCall == null) {
            return;
        }
        mHardwareService.removeVideoSurface(mSipCall.getCallId());
    }

    public void previewVideoSurfaceDestroyed() {
        mHardwareService.removePreviewVideoSurface();
        mHardwareService.stopCapture();
    }

    public void displayChanged() {
        mHardwareService.switchInput(mSipCall.getCallId());
    }

    public void layoutChanged() {
        getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
    }

    public void uiVisibilityChanged(boolean displayed) {
        getView().displayHangupButton(mOnGoingCall && displayed);
    }

    private void finish() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        mSipCall = null;
        getView().finish();
    }

    private void confUpdate() {
        if (mSipCall == null) {
            return;
        }
        mAudioOnly = mSipCall.isAudioOnly();
        if (mSipCall.isOnGoing()) {
            mOnGoingCall = true;
            getView().initNormalStateDisplay(mAudioOnly);
            getView().updateContactBubble(mSipCall.getContact());
            getView().updateMenu();
            if (!mAudioOnly) {
                mHardwareService.setPreviewSettings();
                getView().displayVideoSurface(true);
            }
            if (executor == null || executor.isShutdown()) {
                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(timeRunnable, 0, 1, TimeUnit.SECONDS);
            }
        } else if (mSipCall.isRinging()) {
            if (mSipCall.isIncoming()) {
                if (mAccountService.getAccount(mSipCall.getAccount()).isAutoanswerEnabled()) {
                    mCallService.accept(mSipCall.getCallId());
                } else {
                    getView().initIncomingCallDisplay();
                    getView().updateContactBubble(mSipCall.getContact());
                }
            } else {
                mOnGoingCall = false;
                getView().updateCallStatus(mSipCall.getCallState());
                getView().initOutGoingCallDisplay();
                getView().updateContactBubble(mSipCall.getContact());
            }
        } else {
            finish();
        }
    }

    private void updateTime() {
        if (mSipCall != null) {
            long duration = System.currentTimeMillis() - mSipCall.getTimestampStart();
            duration = duration / 1000;
            if (mSipCall.isOnGoing()) {
                getView().updateTime(duration);
            }
        }
    }

    private void getContactDetails() {
        CallContact callContact = mSipCall.getContact();
        mContactService.loadContactData(callContact);
        getView().updateContactBubble(callContact);
    }

    private void parseCall(SipCall call, int callState) {
        if (mSipCall == null || mSipCall != call) {
            return;
        }

        if (callState == SipCall.State.HUNGUP
                || callState == SipCall.State.BUSY
                || callState == SipCall.State.FAILURE
                || callState == SipCall.State.OVER) {
            finish();
        } else if (callState != SipCall.State.INACTIVE) {
            mNotificationService.showCallNotification(new Conference(mSipCall));
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        if (observable instanceof CallService) {
            switch (event.getEventType()) {
                case CALL_STATE_CHANGED:
                    SipCall call = event.getEventInput(ServiceEvent.EventInput.CALL, SipCall.class);
                    int state = call.getCallState();

                    Log.d(TAG, "CALL_STATE_CHANGED: " + call.getCallId() + " " + state);

                    parseCall(call, state);
                    confUpdate();
                    break;
            }
        } else if (observable instanceof AccountService) {
            switch (event.getEventType()) {
                case REGISTERED_NAME_FOUND:
                    if (mSipCall != null && mSipCall.getContact() != null) {
                        getView().updateContactBubble(mSipCall.getContact());
                    }
                    break;
            }
        } else if (observable instanceof HardwareService) {
            switch (event.getEventType()) {
                case VIDEO_EVENT:
                    boolean videoStart = event.getEventInput(ServiceEvent.EventInput.VIDEO_START, Boolean.class, false);
                    String callId = event.getEventInput(ServiceEvent.EventInput.VIDEO_CALL, String.class);

                    Log.d(TAG, "VIDEO_EVENT: " + videoStart + " " + callId);
                    previewHeight = event.getEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, Integer.class, 0);
                    previewWidth = event.getEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, Integer.class, 0);

                    if (videoStart) {
                        getView().displayVideoSurface(true);
                    } else if (mSipCall != null && callId != null && mSipCall.getCallId().equals(callId)) {
                        boolean videoStarted = event.getEventInput(ServiceEvent.EventInput.VIDEO_STARTED, Boolean.class, false);
                        getView().displayVideoSurface(videoStarted);
                        if (videoStarted) {
                            videoWidth = event.getEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, Integer.class, 0);
                            videoHeight = event.getEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, Integer.class, 0);
                        }
                    }
                    getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
                    break;
            }
        }
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
}
