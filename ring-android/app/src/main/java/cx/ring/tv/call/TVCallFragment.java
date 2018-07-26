/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
package cx.ring.tv.call;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.rodolfonavalon.shaperipplelibrary.ShapeRipple;
import com.rodolfonavalon.shaperipplelibrary.model.Circle;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.call.CallPresenter;
import cx.ring.call.CallView;
import cx.ring.contacts.AvatarFactory;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.SipCall;
import cx.ring.mvp.BaseFragment;
import cx.ring.services.HardwareServiceImpl;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class TVCallFragment extends BaseFragment<CallPresenter> implements CallView {

    public static final String TAG = TVCallFragment.class.getSimpleName();

    public static final String ACTION_PLACE_CALL = "PLACE_CALL";
    public static final String ACTION_GET_CALL = "GET_CALL";

    public static final String KEY_ACTION = "action";
    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_CONTACT_RING_ID = "CONTACT_RING_ID";
    public static final String KEY_AUDIO_ONLY = "AUDIO_ONLY";

    @BindView(R.id.contact_bubble_layout)
    protected View contactBubbleLayout;

    @BindView(R.id.contact_bubble)
    protected ImageView contactBubbleView;

    @BindView(R.id.contact_bubble_txt)
    protected TextView contactBubbleTxt;

    @BindView(R.id.contact_bubble_num_txt)
    protected TextView contactBubbleNumTxt;

    @BindView(R.id.call_accept_btn)
    protected View acceptButton;

    @BindView(R.id.call_refuse_btn)
    protected View refuseButton;

    @BindView(R.id.call_hangup_btn)
    protected View hangupButton;

    @BindView(R.id.call_status_txt)
    protected TextView mCallStatusTxt;

    @BindView(R.id.shape_ripple)
    protected ShapeRipple shapeRipple = null;

    @BindView(R.id.video_preview_surface)
    protected SurfaceView mVideoSurface = null;

    @BindView(R.id.camera_preview_surface)
    protected SurfaceView mVideoPreview = null;

    // Screen wake lock for incoming call
    private PowerManager.WakeLock mScreenWakeLock;
    private Runnable runnable;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public static TVCallFragment newInstance(@NonNull String action, @Nullable String accountID, @Nullable String contactRingId, boolean audioOnly) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_ACCOUNT_ID, accountID);
        bundle.putSerializable(KEY_CONTACT_RING_ID, contactRingId);
        bundle.putBoolean(KEY_AUDIO_ONLY, audioOnly);
        TVCallFragment countDownFragment = new TVCallFragment();
        countDownFragment.setArguments(bundle);
        return countDownFragment;
    }

    public static TVCallFragment newInstance(@NonNull String action, @Nullable String confId) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_CONF_ID, confId);
        TVCallFragment countDownFragment = new TVCallFragment();
        countDownFragment.setArguments(bundle);
        return countDownFragment;
    }

    public static int callStateToHumanState(final int state) {
        switch (state) {
            case SipCall.State.SEARCHING:
                return R.string.call_human_state_searching;
            case SipCall.State.CONNECTING:
                return R.string.call_human_state_connecting;
            case SipCall.State.RINGING:
                return R.string.call_human_state_ringing;
            case SipCall.State.CURRENT:
                return R.string.call_human_state_current;
            case SipCall.State.HUNGUP:
                return R.string.call_human_state_hungup;
            case SipCall.State.BUSY:
                return R.string.call_human_state_busy;
            case SipCall.State.FAILURE:
                return R.string.call_human_state_failure;
            case SipCall.State.HOLD:
                return R.string.call_human_state_hold;
            case SipCall.State.UNHOLD:
                return R.string.call_human_state_unhold;
            case SipCall.State.OVER:
                return R.string.call_human_state_over;
            case SipCall.State.NONE:
            default:
                return R.string.call_human_state_none;
        }
    }

    @Override
    protected void initPresenter(CallPresenter presenter) {
        super.initPresenter(presenter);

        String action = getArguments().getString(KEY_ACTION);
        if (action != null) {
            if (action.equals(ACTION_PLACE_CALL)) {
                presenter.initOutGoing(getArguments().getString(KEY_ACCOUNT_ID),
                        getArguments().getString(KEY_CONTACT_RING_ID),
                        getArguments().getBoolean(KEY_AUDIO_ONLY));
            } else if (action.equals(ACTION_GET_CALL)) {
                presenter.initIncoming(getArguments().getString(KEY_CONF_ID));
            }
        }
    }

    @Override
    public int getLayout() {
        return R.layout.tv_frag_call;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "ring:callLock");
        mScreenWakeLock.setReferenceCounted(false);

        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
        }

        mVideoSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
        mVideoSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                presenter.videoSurfaceCreated(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                presenter.videoSurfaceDestroyed();
            }
        });

        mVideoPreview.getHolder().setFormat(PixelFormat.RGBA_8888);
        mVideoPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                presenter.previewVideoSurfaceCreated(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                presenter.previewVideoSurfaceDestroyed();
            }
        });
        mVideoPreview.setZOrderMediaOverlay(true);

        shapeRipple.setRippleShape(new Circle());

        runnable = () -> presenter.uiVisibilityChanged(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCompositeDisposable.clear();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }

        presenter.hangupCall();
    }

    @Override
    public void blockScreenRotation() {

    }

    @Override
    public void displayContactBubble(final boolean display) {
        contactBubbleLayout.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayVideoSurface(final boolean display) {
        getActivity().runOnUiThread(() -> {
            mVideoSurface.setVisibility(display ? View.VISIBLE : View.GONE);
            mVideoPreview.setVisibility(display ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void displayPreviewSurface(boolean display) {
        if (display) {
            mVideoPreview.setZOrderMediaOverlay(true);
            mVideoSurface.setZOrderMediaOverlay(false);
        } else {
            mVideoPreview.setZOrderMediaOverlay(false);
            mVideoSurface.setZOrderMediaOverlay(true);
        }
    }

    @Override
    public void displayHangupButton(boolean display) {
        if (display) {
            hangupButton.setVisibility(View.VISIBLE);
        } else {
            AlphaAnimation fadeOutAnimation = new AlphaAnimation(1, 0);
            fadeOutAnimation.setInterpolator(new AccelerateInterpolator());
            fadeOutAnimation.setStartOffset(1000);
            fadeOutAnimation.setDuration(1000);

            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(fadeOutAnimation);
            hangupButton.setAnimation(animationSet);

            hangupButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void displayDialPadKeyboard() {
    }

    @Override
    public void switchCameraIcon(boolean isFront) {

    }

    @Override
    public void updateMenu() {

    }

    @Override
    public void changeScreenRotation() {

    }

    @Override
    public void updateTime(final long duration) {
        getActivity().runOnUiThread(() -> mCallStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60)));
    }

    @Override
    public void updateContactBubble(@NonNull final CallContact contact) {
        String username = contact.getRingUsername();
        String ringId = contact.getIds().get(0);
        Log.d(TAG, "updateContactBubble: username=" + username + ", ringId=" + ringId + " photo:" + contact.getPhoto());

        String displayName = contact.getDisplayName();
        boolean hasProfileName = displayName != null && !displayName.contentEquals(username);

        if (hasProfileName) {
            contactBubbleNumTxt.setVisibility(View.VISIBLE);
            contactBubbleTxt.setText(displayName);
            contactBubbleNumTxt.setText(username);
        } else {
            contactBubbleNumTxt.setVisibility(View.GONE);
            contactBubbleTxt.setText(username);
        }

        mCompositeDisposable.add(Single.fromCallable(() -> Glide.with(getActivity())
                .load(AvatarFactory.getAvatar(
                        getActivity(),
                        contact.getPhoto(),
                        username,
                        ringId, true))
                .apply(AvatarFactory.getGlideOptions(true, false))
                .submit()
                .get())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> contactBubbleView.setImageDrawable(d)));
    }

    @Override
    public void updateCallStatus(final int callState) {
            switch (callState) {
                case SipCall.State.NONE:
                    mCallStatusTxt.setText("");
                    break;
                default:
                    mCallStatusTxt.setText(callStateToHumanState(callState));
                    break;
            }
    }

    @Override
    public void initMenu(boolean isSpeakerOn, boolean hasContact, boolean displayFlip, boolean canDial, boolean onGoingCall) {

    }

    @Override
    public void initNormalStateDisplay(final boolean audioOnly) {
        shapeRipple.stopRipple();

        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.GONE);
        hangupButton.setVisibility(View.VISIBLE);

        contactBubbleLayout.setVisibility(audioOnly ? View.INVISIBLE : View.VISIBLE);

        getActivity().invalidateOptionsMenu();

        handleVisibilityTimer();
    }

    @Override
    public void initIncomingCallDisplay() {
        acceptButton.setVisibility(View.VISIBLE);
        refuseButton.setVisibility(View.VISIBLE);
        hangupButton.setVisibility(View.GONE);
    }

    @Override
    public void initOutGoingCallDisplay() {
        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.VISIBLE);
        hangupButton.setVisibility(View.GONE);
    }

    @Override
    public void resetVideoSize(final int videoWidth, final int videoHeight, final int previewWidth, final int previewHeight) {
        getActivity().runOnUiThread(() -> {
            ViewGroup rootView = (ViewGroup) getView();
            if (rootView == null)
                return;

            double videoRatio = videoWidth / (double) videoHeight;
            double screenRatio = getView().getWidth() / (double) getView().getHeight();

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoSurface.getLayoutParams();
            int oldW = params.width;
            int oldH = params.height;
            if (videoRatio >= screenRatio) {
                params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                params.height = (int) (videoHeight * (double) rootView.getWidth() / (double) videoWidth);
            } else {
                params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
                params.width = (int) (videoWidth * (double) rootView.getHeight() / (double) videoHeight);
            }

            if (oldW != params.width || oldH != params.height) {
                mVideoSurface.setLayoutParams(params);
            }

            final int mPreviewWidth = HardwareServiceImpl.VIDEO_WIDTH;
            final int mPreviewHeight = HardwareServiceImpl.VIDEO_HEIGHT;

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            RelativeLayout.LayoutParams paramsPreview = (RelativeLayout.LayoutParams) mVideoPreview.getLayoutParams();
            oldW = paramsPreview.width;
            oldH = paramsPreview.height;
            double previewMaxDim = Math.max(mPreviewWidth, mPreviewHeight);
            double previewRatio = metrics.density * 160. / previewMaxDim;
            paramsPreview.width = (int) (mPreviewWidth * previewRatio);
            paramsPreview.height = (int) (mPreviewHeight * previewRatio);
            if (oldW != paramsPreview.width || oldH != paramsPreview.height) {
                mVideoPreview.setLayoutParams(paramsPreview);
            }
        });
    }

    @Override
    public void goToConversation(String accountId, String conversationId) {

    }

    @Override
    public void goToAddContact(CallContact callContact) {

    }

    @Override
    public void finish() {
        getActivity().finish();
    }

    @Override
    public void onUserLeave() {
        presenter.requestPipMode();
    }

    @Override
    public void enterPipMode(SipCall sipCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getActivity().enterPictureInPictureMode();
        }
    }

    @OnClick({R.id.call_hangup_btn})
    public void hangUpClicked() {
        presenter.hangupCall();
    }

    @OnClick(R.id.call_refuse_btn)
    public void refuseClicked() {
        presenter.refuseCall();
    }

    @OnClick(R.id.call_accept_btn)
    public void acceptClicked() {
        presenter.acceptCall();
    }

    public void onKeyDown() {
        handleVisibilityTimer();
    }

    private void handleVisibilityTimer() {
        presenter.uiVisibilityChanged(true);
        View view = getView();
        if (view != null && runnable != null) {
            Handler handler = view.getHandler();
            if (handler != null) {
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 5000);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        View view = getView();
        if (view != null && runnable != null) {
            Handler handler = view.getHandler();
            if (handler != null)
                handler.removeCallbacks(runnable);
        }
    }
}