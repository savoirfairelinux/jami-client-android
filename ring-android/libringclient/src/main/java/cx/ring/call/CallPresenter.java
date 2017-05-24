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

import cx.ring.facades.ConversationFacade;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;

public class CallPresenter extends RootPresenter<CallView> implements Observer<ServiceEvent> {

    public final static String TAG = CallPresenter.class.getSimpleName();

    protected AccountService mAccountService;
    protected ConversationFacade mConversationFacade;
    protected NotificationService mNotificationService;
    protected DeviceRuntimeService mDeviceRuntimeService;
    protected HardwareService mHardwareService;
    protected CallService mCallService;
    protected ContactService mContactService;
    protected HistoryService mHistoryService;

    private SipCall mSipCall;
    private boolean mOnGoingCall = false;
    private boolean mHasVideo = false;

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
                         ConversationFacade conversationFacade,
                         NotificationService notificationService,
                         DeviceRuntimeService deviceRuntimeService,
                         HardwareService hardwareService,
                         CallService callService,
                         ContactService contactService,
                         HistoryService mHistoryService) {
        this.mAccountService = accountService;
        this.mConversationFacade = conversationFacade;
        this.mNotificationService = notificationService;
        this.mDeviceRuntimeService = deviceRuntimeService;
        this.mHardwareService = hardwareService;
        this.mCallService = callService;
        this.mContactService = contactService;
        this.mHistoryService = mHistoryService;
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mCallService.removeObserver(this);
        mHardwareService.removeObserver(this);

        if (mHasVideo) {
            mHardwareService.stopCapture();
        }

        mDeviceRuntimeService.closeAudioState();
    }

    @Override
    public void bindView(CallView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mCallService.addObserver(this);
        mHardwareService.addObserver(this);
    }

    public void initOutGoing(String accountId, Uri number, boolean hasVideo) {
        CallContact contact = mContactService.findContactByNumber(number.getRawUriString());

        Log.d(TAG, "initOutGoing: " + (contact != null));

        String callId = mCallService.placeCall(accountId, number.getUriString(), hasVideo);
        if (callId == null || callId.isEmpty()) {
            finish();
            return;
        }

        mSipCall = mCallService.getCurrentCallForId(callId);
        mSipCall.muteVideo(!hasVideo);
        mSipCall.setCallID(callId);
        mSipCall.setContact(contact);
        mHasVideo = hasVideo;
        confUpdate();
        getUsername();
        getContactDetails();
        getView().blockScreenRotation();
    }

    public void initIncoming(String confId) {
        mSipCall = mCallService.getCurrentCallForId(confId);
        //FIXME sipCall is null for unknowm reason atm
        if (mSipCall == null) {
            finish();
            return;
        }
        CallContact contact = mSipCall.getContact();
        if (contact == null) {
            contact = mContactService.findContactByNumber(mSipCall.getNumberUri().getRawUriString());
        }
        mSipCall.setContact(contact);
        mHasVideo = true;
        confUpdate();
        getUsername();
        getContactDetails();
        getView().blockScreenRotation();

        mDeviceRuntimeService.updateAudioState(mSipCall.isRinging() && mSipCall.isIncoming());
    }

    public void prepareOptionMenu() {
        boolean isSpeakerOn = mHardwareService.isSpeakerPhoneOn();
        boolean hasContact = mSipCall != null && null != mSipCall.getContact() && mSipCall.getContact().isUnknown();
        boolean canDial = mOnGoingCall && mSipCall != null && !mSipCall.isIncoming();
        boolean hasMultipleCamera = mHardwareService.getCameraCount() > 1 && mOnGoingCall && mHasVideo;
        getView().initMenu(isSpeakerOn, hasContact, hasMultipleCamera, canDial, mOnGoingCall);
    }

    public void chatClick() {
        if (mSipCall == null
                || mSipCall.getContact() == null
                || mSipCall.getContact().getIds() == null
                || mSipCall.getContact().getIds().isEmpty()) {
            return;
        }
        getView().goToConversation(mSipCall.getContact().getIds().get(0));
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
        if (mSipCall != null) {
            mNotificationService.cancelCallNotification(mSipCall.getCallId().hashCode());
        }
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
        if (mSipCall.isOnGoing()) {
            mOnGoingCall = true;
            getView().initNormalStateDisplay(mHasVideo);
            getView().initContactDisplay(mSipCall);
            if (mHasVideo) {
                mHardwareService.setPreviewSettings();
                getView().displayVideoSurface(true);
            }
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(timeRunnable, 0, 1, TimeUnit.SECONDS);
        } else if (mSipCall.isRinging()) {
            if (mSipCall.isIncoming()) {
                if (mAccountService.getAccount(mSipCall.getAccount()).isAutoanswerEnabled()) {
                    mCallService.accept(mSipCall.getCallId());
                } else {
                    getView().initIncomingCallDisplay();
                    getView().initContactDisplay(mSipCall);
                }
            } else {
                mOnGoingCall = false;
                getView().updateCallStatus(mSipCall.getCallState());
                getView().initOutGoingCallDisplay();
                getView().initContactDisplay(mSipCall);
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

    private void getUsername() {
        if (mSipCall == null) {
            return;
        }

        String[] split = mSipCall.getNumber().split(":");
        if (split.length > 1) {
            mAccountService.lookupAddress("", "", split[1]);
        }
    }

    private void getContactDetails() {
        CallContact callContact = mSipCall.getContact();
        Tuple<String, byte[]> tuple = mContactService.loadContactData(callContact);
        getView().updateContactBubbleWithVCard(tuple.first, tuple.second);
    }

    private void parseCall(String callId, int callState) {
        if (mSipCall == null || !mSipCall.getCallId().equals(callId)) {
            return;
        }

        if (callState == SipCall.State.SEARCHING ||
                callState == SipCall.State.OVER) {
            mHistoryService.updateVCard();
        }

        if (callState == SipCall.State.HUNGUP
                || callState == SipCall.State.BUSY
                || callState == SipCall.State.FAILURE
                || callState == SipCall.State.OVER) {
            finish();
        } else if (callState != SipCall.State.INACTIVE) {
            mNotificationService.showCallNotification(new Conference(mSipCall));
        }

        mDeviceRuntimeService.updateAudioState(mSipCall.isRinging() && mSipCall.isIncoming());
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        if (observable instanceof CallService) {
            switch (event.getEventType()) {
                case CALL_STATE_CHANGED:
                    String callId = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);
                    int newState = SipCall.stateFromString(event.getEventInput(ServiceEvent.EventInput.STATE, String.class));

                    Log.d(TAG, "CALL_STATE_CHANGED: " + callId + " " + newState);

                    parseCall(callId, newState);
                    confUpdate();
                    break;
            }
        } else if (observable instanceof AccountService) {
            switch (event.getEventType()) {
                case REGISTERED_NAME_FOUND:
                    String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                    String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);

                    if (mSipCall.getNumber().contains(address)) {
                        getView().updateContactBubble(name);
                    }
                    break;
            }
        } else if (observable instanceof HardwareService) {
            switch (event.getEventType()) {
                case VIDEO_EVENT:
                    boolean videoStart = event.getEventInput(ServiceEvent.EventInput.VIDEO_START, Boolean.class, false);
                    boolean camera = event.getEventInput(ServiceEvent.EventInput.VIDEO_CAMERA, Boolean.class, false);
                    String callId = event.getEventInput(ServiceEvent.EventInput.VIDEO_CALL, String.class);

                    Log.d(TAG, "VIDEO_EVENT: " + videoStart + " " + camera + " " + callId);

                    if (videoStart) {
                        getView().displayVideoSurface(true);
                    } else if (camera) {
                        previewWidth = event.getEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, Integer.class, 0);
                        previewHeight = event.getEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, Integer.class, 0);
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
}
