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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ConfigKey;
import cx.ring.model.SecureSipCall;
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
import cx.ring.utils.BlockchainInputHandler;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;

public class CallPresenter extends RootPresenter<CallView> implements Observer<ServiceEvent> {

    protected AccountService mAccountService;
    protected ConversationFacade mConversationFacade;
    protected NotificationService mNotificationService;
    protected DeviceRuntimeService mDeviceRuntimeService;
    protected HardwareService mHardwareService;
    protected CallService mCallService;
    protected ContactService mContactService;
    protected HistoryService mHistoryService;

    private Conference mConference;
    private boolean mOnGoingCall;
    private boolean mHasVideo;

    private int videoWidth = -1;
    private int videoHeight = -1;
    private int previewWidth = -1;
    private int previewHeight = -1;

    private BlockchainInputHandler mBlockchainInputHandler;

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

        mHardwareService.removeVideoSurface(mConference.getId());
        mHardwareService.removePreviewVideoSurface();
        mHardwareService.stopCapture();
    }

    @Override
    public void bindView(CallView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mCallService.addObserver(this);
        mHardwareService.addObserver(this);
    }

    public void init(String accountId, Uri number, boolean hasVideo) {
        SipCall call = new SipCall(null, accountId, number, SipCall.Direction.OUTGOING);
        call.muteVideo(!hasVideo);
        mHasVideo = hasVideo;

        CallContact contact = call.getContact();
        if (contact == null) {
            contact = mContactService.findContactByNumber(call.getNumberUri().getRawUriString());
        }

        if (number == null || number.isEmpty()) {
            number = contact.getPhones().get(0).getNumber();
        }
        String callId = mCallService.placeCall(call.getAccount(), number.getUriString(), !call.isVideoMuted());
        if (callId == null || callId.isEmpty()) {
            return;
        }
        call.setCallID(callId);
        Account account = mAccountService.getAccount(call.getAccount());
        if (account.isRing()
                || account.getDetailBoolean(ConfigKey.SRTP_ENABLE)
                || account.getDetailBoolean(ConfigKey.TLS_ENABLE)) {
            SecureSipCall secureCall = new SecureSipCall(call, account.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
            mConference = new Conference(secureCall);
        } else {
            mConference = new Conference(call);
        }
        mConference.getParticipants().get(0).setContact(contact);
        confUpdate();
        getUsername();
        getContactDetails();
    }

    public void init(String confId) {
        mConference = mConversationFacade.getConference(confId);
        mHasVideo = true;
        confUpdate();
        getUsername();
        getContactDetails();
    }

    public void prepareOptionMenu() {
        boolean isSpeakerOn = mHardwareService.isSpeakerPhoneOn();
        SipCall call = (mConference != null && !mConference.getParticipants().isEmpty()) ? mConference.getParticipants().get(0) : null;
        boolean hasContact = call != null && null != call.getContact() && call.getContact().isUnknown();
        boolean canDial = mOnGoingCall && mConference != null && !mConference.isIncoming();
        getView().initMenu(isSpeakerOn, hasContact, mHasVideo, canDial);
    }

    public void chatClick() {
        SipCall call = mConference.getParticipants().get(0);
        if (call == null
                || call.getContact() == null
                || call.getContact().getIds() == null
                || call.getContact().getIds().isEmpty()) {
            return;
        }
        getView().goToConversation(call.getContact().getIds().get(0));
    }

    public void addContact() {
        SipCall call = mConference.getParticipants().get(0);
        if (call == null || call.getContact() == null) {
            return;
        }
        getView().goToAddContact(call.getContact());
    }

    public void speakerClick() {
        mHardwareService.switchSpeakerPhone();
    }

    public void switchVideoInputClick() {
        //TODO change boolean
        mHardwareService.switchInput(mConference.getId(), true);
        getView().switchCameraIcon(true);
    }

    public void screenRotationClick() {
        getView().changeScreenRotation();
    }

    public void dialpadClick() {
        getView().displayDialPadKeyboard();
    }

    public void acceptCall() {
        if (mConference == null || mConference.getParticipants().size() == 0) {
            return;
        }
        mCallService.accept(mConference.getParticipants().get(0).getCallId());
    }

    public void hangupCall() {
        mCallService.hangUp(mConference.getParticipants().get(0).getCallId());
        mNotificationService.cancelCallNotification(mConference.getParticipants().get(0).getCallId().hashCode());
        getView().finish();
    }

    public void refuseCall() {
        mCallService.refuse(mConference.getParticipants().get(0).getCallId());
        mNotificationService.cancelCallNotification(mConference.getParticipants().get(0).getCallId().hashCode());
        getView().finish();
    }

    public void videoSurfaceCreated(Object holder) {
        mHardwareService.addVideoSurface(mConference.getId(), holder);
        getView().displayContactBubble(false);
        getView().blockScreenRotation();
    }

    public void previewVideoSurfaceCreated(Object holder) {
        mHardwareService.addPreviewVideoSurface(holder);
        mHardwareService.startCapture(null);
    }

    public void videoSurfaceDestroyed() {
        mHardwareService.removeVideoSurface(mConference.getId());
    }

    public void previewVideoSurfaceDestroyed() {
        mHardwareService.removePreviewVideoSurface();
        mHardwareService.stopCapture();
    }

    public void displayChanged() {
        //TODO change boolean
        mHardwareService.switchInput(mConference.getId(), true);
    }

    public void layoutChanged() {
        getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
    }

    public void uiVisibilityChanged(boolean displayed) {
        getView().displayHangupButton(mOnGoingCall && displayed);
    }

    private void confUpdate() {
        if (mConference == null || mConference.getParticipants().isEmpty()) {
            getView().finish();
        } else {
            if (mConference.isOnGoing()) {
                mOnGoingCall = true;
                getView().initNormalStateDisplay(mHasVideo);
                //TODO securityDisplay
                getView().initContactDisplay(mConference.getParticipants().get(0));
                if (mHasVideo) {
                    mHardwareService.setPreviewSettings();
                    getView().displayVideoSurface(true);
                }
                executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(timeRunnable, 0, 1, TimeUnit.SECONDS);
            } else if (mConference.isRinging()) {
                mOnGoingCall = false;
                getView().updateCallStatus(mConference.getParticipants().get(0).getCallState());

                if (mConference.isIncoming()) {
                    if (mAccountService.getAccount(mConference.getParticipants().get(0).getAccount()).isAutoanswerEnabled()) {
                        mCallService.accept(mConference.getParticipants().get(0).getCallId());
                    } else {
                        getView().initIncomingCallDisplay();
                        getView().initContactDisplay(mConference.getParticipants().get(0));
                    }
                } else {
                    getView().initOutGoingCallDisplay();
                    getView().initContactDisplay(mConference.getParticipants().get(0));
                }
            } else {
                mNotificationService.cancelCallNotification(mConference.getParticipants().get(0).getCallId().hashCode());
                executor.shutdown();
                getView().finish();
            }
        }
    }

    private void updateTime() {
        if (mConference != null && !mConference.getParticipants().isEmpty()) {
            long duration = System.currentTimeMillis() - mConference.getParticipants().get(0).getTimestampStart();
            duration = duration / 1000;
            if (mConference.isOnGoing()) {
                getView().updateTime(duration);
            }
        }
    }

    private void getUsername() {
        if (mConference == null || mConference.getParticipants().isEmpty()) {
            return;
        }

        if (mBlockchainInputHandler == null || !mBlockchainInputHandler.isAlive()) {
            mBlockchainInputHandler = new BlockchainInputHandler(new WeakReference<>(mAccountService));
        }

        String[] split = mConference.getParticipants().get(0).getNumber().split(":");
        if (split.length > 0) {
            mBlockchainInputHandler.enqueueNextLookup(split[1]);
        }
    }

    private void getContactDetails() {
        CallContact callContact = mConference.getParticipants().get(0).getContact();
        Tuple<String, byte[]> tuple = mContactService.loadContactData(callContact);
        getView().updateContactBubbleWithVCard(tuple.first, tuple.second);
    }

    private void parseCall(String callId, int callState, HashMap<String, String> callDetails) {
        if (callState == SipCall.State.INCOMING ||
                callState == SipCall.State.OVER) {
            mHistoryService.updateVCard();
        }

        if (!mConference.getParticipants().get(0).getCallId().equals(callId)) {
            return;
        }

        SipCall call = mConference.getCallById(callId);
        int oldState = call.getCallState();

        if (callState != oldState) {
            if ((call.isRinging() || callState == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
                call.setTimestampStart(System.currentTimeMillis());
            }
            if (callState == SipCall.State.RINGING) {
                mAccountService.sendProfile(callId, call.getAccount());
            }
            call.setCallState(callState);
        }

        call.setDetails(callDetails);

        if (callState == SipCall.State.HUNGUP
                || callState == SipCall.State.BUSY
                || callState == SipCall.State.FAILURE
                || callState == SipCall.State.OVER) {
            if (callState == SipCall.State.HUNGUP) {
                call.setTimestampEnd(System.currentTimeMillis());
            }

            mHistoryService.insertNewEntry(mConference);
            mConference.removeParticipant(call);
        } else if (callState != SipCall.State.INACTIVE) {
            mNotificationService.showCallNotification(mConference);
        }
        if (callState == SipCall.State.FAILURE || callState == SipCall.State.BUSY || callState == SipCall.State.HUNGUP) {
            hangupCall();
        }

        mDeviceRuntimeService.updateAudioState(mConference);
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
                    HashMap<String, String> callDetails = (HashMap<String, String>) event.getEventInput(ServiceEvent.EventInput.DETAILS, HashMap.class);
                    parseCall(callId, newState, callDetails);
                    confUpdate();
                    break;
            }
        } else if (observable instanceof AccountService) {
            switch (event.getEventType()) {
                case REGISTERED_NAME_FOUND:
                    final String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                    getView().updateContactBuble(name);
                    break;
            }
        } else if (observable instanceof HardwareService) {
            switch (event.getEventType()) {
                case VIDEO_EVENT:
                    boolean videoStart = event.getEventInput(ServiceEvent.EventInput.VIDEO_START, Boolean.class, false);
                    boolean camera = event.getEventInput(ServiceEvent.EventInput.VIDEO_CAMERA, Boolean.class, false);
                    String callId = event.getEventInput(ServiceEvent.EventInput.VIDEO_CALL, String.class);

                    Log.d("CallPresenter", "VIDEO_EVENT: " + videoStart + ", cameraId: " + camera + ", callId: " + callId);

                    if (videoStart) {
                        getView().displayVideoSurface(true);
                    } else if (camera) {
                        previewWidth = event.getEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, Integer.class, 0);
                        previewHeight = event.getEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, Integer.class, 0);
                    } else if (mConference != null && callId != null && mConference.getId().equals(callId)) {
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
