/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
import cx.ring.services.HardwareService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;

public class CallPresenter extends RootPresenter<CallView> {

    public final static String TAG = CallPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private NotificationService mNotificationService;
    private HardwareService mHardwareService;
    private CallService mCallService;

    private SipCall mSipCall;
    private boolean mOnGoingCall = false;
    private boolean mAudioOnly = true;

    private int videoWidth = -1;
    private int videoHeight = -1;
    private int previewWidth = -1;
    private int previewHeight = -1;

    private Runnable timeRunnable = this::updateTime;
    private Disposable timeUpdateTask = null;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public CallPresenter(AccountService accountService,
                         NotificationService notificationService,
                         HardwareService hardwareService,
                         CallService callService) {
        mAccountService = accountService;
        mNotificationService = notificationService;
        mHardwareService = hardwareService;
        mCallService = callService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        if (!mAudioOnly) {
            mHardwareService.stopCapture();
        }
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
    }

    public void initOutGoing(String accountId, String contactRingId, boolean audioOnly) {
        if (accountId == null || contactRingId == null) {
            Log.e(TAG, "initOutGoing: null account or contact");
            getView().finish();
            return;
        }
        if (mHardwareService.getCameraCount() == 0) {
            audioOnly = true;
        }
        getView().blockScreenRotation();

        mCompositeDisposable.add(mCallService
                .placeCallObservable(accountId, StringUtils.toNumber(contactRingId), audioOnly)
                .observeOn(mUiScheduler)
                .subscribe(call ->  {
                    contactUpdate(call);
                    mAudioOnly = call.isAudioOnly();
                    getView().updateMenu();
                    confUpdate();
                }));
    }

    public void initIncoming(String confId) {
        getView().blockScreenRotation();
        mCompositeDisposable.add(mCallService.getCallUpdates(confId)
                .observeOn(mUiScheduler)
                .subscribe(call -> {
                    contactUpdate(call);
                    mAudioOnly = call.isAudioOnly();
                    getView().updateMenu();
                    confUpdate();
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

    public void speakerClick() {
        mHardwareService.toggleSpeakerphone();
    }

    public void switchVideoInputClick() {
        mHardwareService.switchInput(mSipCall.getCallId());
        getView().switchCameraIcon(mHardwareService.isPreviewFromFrontCamera());
    }

    public void screenRotationClick() {
        getView().changeScreenRotation();
    }

    public void configurationChanged() {
        if (mSipCall != null) {
            mHardwareService.restartCamera(mSipCall.getCallId());
        }
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
            mNotificationService.cancelCallNotification(call.getCallId().hashCode());
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
            mCompositeDisposable.add(call.getContact()
                    .getUpdates()
                    .observeOn(mUiScheduler)
                    .subscribe(c -> getView().updateContactBubble(c)));
        }
    }

    private void confUpdate() {
        if (mSipCall == null) {
            return;
        }
        CallView view = getView();
        mAudioOnly = mSipCall.isAudioOnly();
        if (mSipCall.isOnGoing()) {
            mOnGoingCall = true;
            view.initNormalStateDisplay(mAudioOnly);
            view.updateMenu();
            if (!mAudioOnly) {
                mHardwareService.setPreviewSettings();
                view.displayVideoSurface(true);
            }
            if (timeUpdateTask != null)
                timeUpdateTask.dispose();
            timeUpdateTask = mUiScheduler.schedulePeriodicallyDirect(timeRunnable, 0, 1, TimeUnit.SECONDS);
        } else if (mSipCall.isRinging()) {
            if (mSipCall.isIncoming()) {
                if (mAccountService.getAccount(mSipCall.getAccount()).isAutoanswerEnabled()) {
                    mCallService.accept(mSipCall.getCallId());
                } else {
                    view.initIncomingCallDisplay();
                }
            } else {
                mOnGoingCall = false;
                view.updateCallStatus(mSipCall.getCallState());
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
        Log.d(TAG, "VIDEO_EVENT: " + event.start + " " + event.callId);
        previewHeight = event.w;
        previewWidth = event.h;

        if (event.start) {
            getView().displayVideoSurface(true);
        } else if (mSipCall != null && mSipCall.getCallId().equals(event.callId)) {
            getView().displayVideoSurface(event.started);
            if (event.started) {
                videoWidth = event.w;
                videoHeight = event.h;
            }
        }
        getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
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

    public void requestPipMode() {
        if (mSipCall != null && mSipCall.isOnGoing() && !mSipCall.isAudioOnly()) {
            getView().enterPipMode(mSipCall);
        }
    }

    public void pipModeChanged(boolean pip) {
        if (pip) {
            getView().displayHangupButton(false);
            getView().displayPreviewSurface(false);
        } else {
            getView().displayPreviewSurface(true);
        }
    }
}
