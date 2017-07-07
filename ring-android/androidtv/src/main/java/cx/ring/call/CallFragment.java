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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skyfishjy.library.RippleBackground;

import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.call.CallPresenter;
import cx.ring.call.CallView;
import cx.ring.client.HomeActivity;
import cx.ring.model.CallContact;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.mvp.BaseFragment;
import cx.ring.service.DRingService;
import cx.ring.utils.CircleTransform;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.KeyboardVisibilityManager;

public class CallFragment extends BaseFragment<CallPresenter> implements CallView {

    public static final String TAG = CallFragment.class.getSimpleName();

    public static final String ACTION_PLACE_CALL = "PLACE_CALL";
    public static final String ACTION_GET_CALL = "GET_CALL";

    public static final String KEY_ACTION = "action";
    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_HAS_VIDEO = "hasVideo";

    @Inject
    protected CallPresenter callPresenter;

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

    @BindView(R.id.dialpad_edit_text)
    protected EditText mNumeralDialEditText;

    @BindView(R.id.ripple_animation)
    protected RippleBackground mPulseAnimation;

    @BindView(R.id.video_preview_surface)
    protected SurfaceView mVideoSurface = null;

    @BindView(R.id.camera_preview_surface)
    protected SurfaceView mVideoPreview = null;

    private MenuItem speakerPhoneBtn = null;
    private MenuItem addContactBtn = null;
    private MenuItem flipCameraBtn = null;
    private MenuItem dialPadBtn = null;
    private MenuItem changeScreenOrientationBtn = null;

    // Screen wake lock for incoming call
    private PowerManager.WakeLock mScreenWakeLock;
    private DisplayManager.DisplayListener displayListener;

    public static CallFragment newInstance(@NonNull String action, @Nullable String accountID, @Nullable Uri number, boolean hasVideo) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_ACCOUNT_ID, accountID);
        bundle.putSerializable(KEY_NUMBER, number);
        bundle.putBoolean(KEY_HAS_VIDEO, hasVideo);
        CallFragment countDownFragment = new CallFragment();
        countDownFragment.setArguments(bundle);
        return countDownFragment;
    }

    public static CallFragment newInstance(@NonNull String action, @Nullable String confId) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_CONF_ID, confId);
        CallFragment countDownFragment = new CallFragment();
        countDownFragment.setArguments(bundle);
        return countDownFragment;
    }

    @Override
    protected CallPresenter createPresenter() {
        return callPresenter;
    }

    @Override
    protected void initPresenter(CallPresenter presenter) {
        super.initPresenter(presenter);

        String action = getArguments().getString(KEY_ACTION);
        if (action.equals(ACTION_PLACE_CALL)) {
            callPresenter.initOutGoing(getArguments().getString(KEY_ACCOUNT_ID),
                    (Uri) getArguments().getSerializable(KEY_NUMBER),
                    getArguments().getBoolean(KEY_HAS_VIDEO));
        } else if (action.equals(ACTION_GET_CALL)) {
            callPresenter.initIncoming(getArguments().getString(KEY_CONF_ID));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View inflatedView = inflater.inflate(R.layout.frag_call, container, false);

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "cx.ring.onIncomingCall");
        mScreenWakeLock.setReferenceCounted(false);

        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            displayListener = new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callPresenter.displayChanged();
                        }
                    });
                }
            };
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
        inflatedView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View parent, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                callPresenter.layoutChanged();
            }
        });
        inflatedView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                boolean ui = (visibility & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0;
                callPresenter.uiVisibilityChanged(ui);
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

        return inflatedView;
    }

    @Override
    public void onStop() {
        super.onStop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager displayManager = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
            displayManager.unregisterDisplayListener(displayListener);
        }

        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        presenter.configurationChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inf) {

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        callPresenter.prepareOptionMenu();
    }


    @Override
    public void blockScreenRotation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            return;
        }
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void displayContactBubble(final boolean display) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                contactBubbleLayout.setVisibility(display ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void displayVideoSurface(final boolean display) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoSurface.setVisibility(display ? View.VISIBLE : View.GONE);
                mVideoPreview.setVisibility(display ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void displayHangupButton(boolean display) {
        hangupButton.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayDialPadKeyboard() {
        KeyboardVisibilityManager.showKeyboard(getActivity(),
                mNumeralDialEditText,
                InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void switchCameraIcon(boolean isFront) {
        flipCameraBtn.setIcon(isFront ? R.drawable.ic_camera_front_white : R.drawable.ic_camera_rear_white);
    }

    @Override
    public void changeScreenRotation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void updateTime(final long duration) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCallStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
            }
        });
    }

    @Override
    public void updateContactBubble(@NonNull final CallContact contact) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                byte[] photo = contact.getPhoto();
                if (photo != null && photo.length > 0) {
                    Glide.with(getActivity())
                            .load(photo)
                            .transform(new CircleTransform(getActivity()))
                            .error(R.drawable.ic_contact_picture)
                            .into(contactBubbleView);
                } else {
                    Glide.with(getActivity())
                            .load(R.drawable.ic_contact_picture)
                            .into(contactBubbleView);
                }

                String username = contact.getRingUsername();
                String displayName = contact.getDisplayName();
                boolean hasProfileName = displayName != null && !displayName.contentEquals(username);
                boolean firstShow = contactBubbleTxt.getText() != null && contactBubbleTxt.getText().length() > 0;

                if (hasProfileName) {
                    contactBubbleNumTxt.setVisibility(View.VISIBLE);
                    contactBubbleTxt.setText(displayName);
                    contactBubbleNumTxt.setText(username);
                } else {
                    contactBubbleNumTxt.setVisibility(View.GONE);
                    contactBubbleTxt.setText(username);
                }

                if (firstShow) {
                    mPulseAnimation.startRippleAnimation();
                }
            }
        });
    }

    @Override
    public void updateCallStatus(final int callState) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (callState) {
                    case SipCall.State.NONE:
                        mCallStatusTxt.setText("");
                        break;
                    default:
                        mCallStatusTxt.setText(callStateToHumanState(callState));
                        break;
                }
            }
        });
    }

    @Override
    public void initMenu(boolean isSpeakerOn, boolean hasContact, boolean displayFlip, boolean canDial, boolean onGoingCall) {
        if (speakerPhoneBtn != null) {
            speakerPhoneBtn.setVisible(onGoingCall);
            if (speakerPhoneBtn.getIcon() != null) {
                speakerPhoneBtn.getIcon().setAlpha(isSpeakerOn ? 255 : 128);
            }
            speakerPhoneBtn.setChecked(isSpeakerOn);
        }
        if (addContactBtn != null) {
            addContactBtn.setVisible(hasContact);
        }
        if (flipCameraBtn != null) {
            flipCameraBtn.setVisible(displayFlip);
        }
        if (dialPadBtn != null) {
            dialPadBtn.setVisible(canDial);
        }
        if (changeScreenOrientationBtn != null) {
            changeScreenOrientationBtn.setVisible(mVideoSurface.getVisibility() == View.VISIBLE);
        }
    }

    @Override
    public void initNormalStateDisplay(final boolean hasVideo) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                acceptButton.setVisibility(View.GONE);
                refuseButton.setVisibility(View.GONE);
                hangupButton.setVisibility(View.VISIBLE);

                contactBubbleLayout.setVisibility(hasVideo ? View.INVISIBLE : View.VISIBLE);

                getActivity().invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void initIncomingCallDisplay() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                acceptButton.setVisibility(View.VISIBLE);
                refuseButton.setVisibility(View.VISIBLE);
                hangupButton.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void initOutGoingCallDisplay() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                acceptButton.setVisibility(View.GONE);
                refuseButton.setVisibility(View.VISIBLE);
                hangupButton.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void resetVideoSize(final int videoWidth, final int videoHeight, final int previewWidth, final int previewHeight) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

                DisplayMetrics metrics = getResources().getDisplayMetrics();
                RelativeLayout.LayoutParams paramsPreview = (RelativeLayout.LayoutParams) mVideoPreview.getLayoutParams();
                oldW = paramsPreview.width;
                oldH = paramsPreview.height;
                double previewMaxDim = Math.max(previewWidth, previewHeight);
                double previewRatio = metrics.density * 160. / previewMaxDim;
                paramsPreview.width = (int) (previewWidth * previewRatio);
                paramsPreview.height = (int) (previewHeight * previewRatio);
                if (oldW != paramsPreview.width || oldH != paramsPreview.height) {
                    mVideoPreview.setLayoutParams(paramsPreview);
                }
            }
        });
    }

    @Override
    public void goToConversation(String conversationId) {

    }

    @Override
    public void goToAddContact(CallContact callContact) {

    }

    @Override
    public void finish() {
        getActivity().finish();
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

    @OnClick({R.id.call_hangup_btn})
    public void hangUpClicked() {
        callPresenter.hangupCall();
    }

    @OnClick(R.id.call_refuse_btn)
    public void refuseClicked() {
        callPresenter.refuseCall();
    }

    @OnClick(R.id.call_accept_btn)
    public void acceptClicked() {
        callPresenter.acceptCall();
    }
}