/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.Call;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cx.ring.client.CallActivity;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.CallContact;
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

    static final String TAG = CallActivity.class.getSimpleName();

    protected AccountService mAccountService;
    protected CallService mCallService;
    protected NotificationService mNotificationService;
    protected HardwareService mHardwareService;
    protected DeviceRuntimeService mDeviceRuntimeService;
    protected ContactService mContactService;

    private SipCall mSipCall;
    private boolean mOnGoingCall = false;
    private boolean mHasVideo = false;

    private BlockchainInputHandler mBlockchainInputHandler;

    private ScheduledExecutorService executor;
    private Runnable timeRunnable = new Runnable() {
        public void run() {
            updateTime();
        }
    };

    @Inject
    public CallPresenter(AccountService accountService,
                         NotificationService notificationService,
                         DeviceRuntimeService deviceRuntimeService,
                         HardwareService hardwareService,
                         CallService callService,
                         ContactService contactService) {
        this.mAccountService = accountService;
        this.mNotificationService = notificationService;
        this.mDeviceRuntimeService = deviceRuntimeService;
        this.mHardwareService = hardwareService;
        this.mCallService = callService;
        this.mContactService = contactService;
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

    @Override
    public void afterInjection() {

    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }
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

    public void initIncoming(String confId) {
        mSipCall = mCallService.getCurrentCallForId(confId);
        //FIXME sipCall is null for unknowm reason atm
        if (mSipCall == null) {
//            finish();
            return;
        }
        CallContact contact = mSipCall.getContact();
        if (contact == null) {
            contact = mContactService.findContactByNumber(mSipCall.getNumberUri().getRawUriString());
        }
        mSipCall.setContact(contact);
        //TODO find a way to get camera on emulator or on tv
        mHasVideo = false;
        confUpdate();
        getUsername();
//        getContactDetails();
//        getView().blockScreenRotation();

        mDeviceRuntimeService.updateAudioState(mSipCall.isRinging() && mSipCall.isIncoming());
    }

    public void initOutGoing(String accountId, Uri number, boolean hasVideo) {
        CallContact contact = mContactService.findContactByNumber(number.getRawUriString());

        Log.d(TAG, "initOutGoing: " + contact + " " + accountId + " " + number);

        String callId = mCallService.placeCall(accountId, number.getUriString(), hasVideo);
        if (callId == null || callId.isEmpty()) {
//            finish();
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
//        getView().blockScreenRotation();
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
                Log.d(TAG, "mSipCall : " + mSipCall + " <> " + mSipCall.getCallState());

                getView().updateCallStatus(mSipCall.getCallState());
                getView().initOutGoingCallDisplay();
                getView().initContactDisplay(mSipCall);
            }
        } else {
//            finish();
            return;
        }
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

    private void getUsername() {
        if (mSipCall == null) {
            return;
        }

        if (mBlockchainInputHandler == null || !mBlockchainInputHandler.isAlive()) {
            mBlockchainInputHandler = new BlockchainInputHandler(new WeakReference<>(mAccountService));
        }

        String[] split = mSipCall.getNumber().split(":");
        if (split.length > 0) {
            mBlockchainInputHandler.enqueueNextLookup(split[1]);
        }
    }

    private void getContactDetails() {
        CallContact callContact = mSipCall.getContact();
        Tuple<String, byte[]> tuple = mContactService.loadContactData(callContact);
        getView().updateContactBubbleWithVCard(tuple.first, tuple.second);
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
}
