/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.fragments;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rodolfonavalon.shaperipplelibrary.ShapeRipple;
import com.rodolfonavalon.shaperipplelibrary.model.Circle;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.call.CallPresenter;
import cx.ring.call.CallView;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.SipCall;
import cx.ring.mvp.BaseFragment;
import cx.ring.service.DRingService;
import cx.ring.services.HardwareServiceImpl;
import cx.ring.services.NotificationService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.utils.Log;
import cx.ring.utils.MediaButtonsHelper;
import cx.ring.views.AvatarDrawable;
import cx.ring.views.CheckableImageButton;
import io.reactivex.disposables.CompositeDisposable;

public class CallFragment extends BaseFragment<CallPresenter> implements CallView, MediaButtonsHelper.MediaButtonsHelperCallback {

    public static final String TAG = CallFragment.class.getSimpleName();

    public static final String ACTION_PLACE_CALL = "PLACE_CALL";
    public static final String ACTION_GET_CALL = "GET_CALL";

    public static final String KEY_ACTION = "action";
    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_AUDIO_ONLY = "AUDIO_ONLY";

    @BindView(R.id.contact_bubble_layout)
    protected ViewGroup contactBubbleLayout;

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

    @BindView(R.id.call_speaker_btn)
    protected CheckableImageButton speakerButton;

    @BindView(R.id.call_mic_btn)
    protected CheckableImageButton micButton;

    @BindView(R.id.call_camera_flip_btn)
    protected ImageButton flipCameraBtn = null;

    @BindView(R.id.call_status_txt)
    protected TextView mCallStatusTxt;

    @BindView(R.id.dialpad_edit_text)
    protected EditText mNumeralDialEditText;

    @BindView(R.id.shape_ripple)
    protected ShapeRipple shapeRipple = null;

    @BindView(R.id.video_preview_surface)
    protected SurfaceView mVideoSurface = null;

    @BindView(R.id.camera_preview_surface)
    protected SurfaceView mVideoPreview = null;

    @BindView(R.id.call_control_group)
    protected ViewGroup controlLayout;

    private MenuItem dialPadBtn = null;
    private MenuItem changeScreenOrientationBtn = null;
    private boolean restartVideo = false;
    private PowerManager.WakeLock mScreenWakeLock;
    private int mCurrentOrientation = Configuration.ORIENTATION_UNDEFINED;

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public static CallFragment newInstance(@NonNull String action, @Nullable String accountID, @Nullable String contactRingId, boolean audioOnly) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_ACCOUNT_ID, accountID);
        bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactRingId);
        bundle.putBoolean(KEY_AUDIO_ONLY, audioOnly);
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

    public static int callStateToHumanState(final SipCall.State state) {
        switch (state) {
            case SEARCHING:
                return R.string.call_human_state_searching;
            case CONNECTING:
                return R.string.call_human_state_connecting;
            case RINGING:
                return R.string.call_human_state_ringing;
            case CURRENT:
                return R.string.call_human_state_current;
            case HUNGUP:
                return R.string.call_human_state_hungup;
            case BUSY:
                return R.string.call_human_state_busy;
            case FAILURE:
                return R.string.call_human_state_failure;
            case HOLD:
                return R.string.call_human_state_hold;
            case UNHOLD:
                return R.string.call_human_state_unhold;
            case OVER:
                return R.string.call_human_state_over;
            case NONE:
            default:
                return 0;
        }
    }

    @Override
    protected void initPresenter(CallPresenter presenter) {
        super.initPresenter(presenter);

        String action = getArguments().getString(KEY_ACTION);
        if (action != null) {
            if (action.equals(ACTION_PLACE_CALL)) {
                presenter.initOutGoing(getArguments().getString(KEY_ACCOUNT_ID),
                        getArguments().getString(ConversationFragment.KEY_CONTACT_RING_ID),
                        getArguments().getBoolean(KEY_AUDIO_ONLY));
            } else if (action.equals(ACTION_GET_CALL)) {
                presenter.initIncoming(getArguments().getString(KEY_CONF_ID));
            }
        }
    }

    public void onUserLeave() {
        presenter.requestPipMode();
    }

    @Override
    public void enterPipMode(SipCall sipCall) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams.Builder paramBuilder = new PictureInPictureParams.Builder();
            if (mVideoSurface.getVisibility() == View.VISIBLE) {
                int[] l = new int[2];
                mVideoSurface.getLocationInWindow(l);
                int x = l[0];
                int y = l[1];
                int w = mVideoSurface.getWidth();
                int h = mVideoSurface.getHeight();
                Rect videoBounds = new Rect(x, y, x + w, y + h);
                paramBuilder.setAspectRatio(new Rational(w, h));
                paramBuilder.setSourceRectHint(videoBounds);
            }
            ArrayList<RemoteAction> actions = new ArrayList<>(1);
            actions.add(new RemoteAction(
                    Icon.createWithResource(getContext(), R.drawable.ic_call_end_white),
                    getString(R.string.action_call_hangup),
                    getString(R.string.action_call_hangup),
                    PendingIntent.getService(getContext(), new Random().nextInt(),
                            new Intent(DRingService.ACTION_CALL_END)
                                    .setClass(getContext(), DRingService.class)
                                    .putExtra(NotificationService.KEY_CALL_ID, sipCall.getCallId()), PendingIntent.FLAG_ONE_SHOT)));
            paramBuilder.setActions(actions);
            getActivity().enterPictureInPictureMode(paramBuilder.build());
        } else if (DeviceUtils.isTv(getContext())) {
            getActivity().enterPictureInPictureMode();
        }
    }

    @Override
    public int getLayout() {
        return R.layout.frag_call;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (restartVideo) {
            displayVideoSurface(true);
            restartVideo = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVideoSurface.getVisibility() == View.VISIBLE) {
            restartVideo = true;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);
        mCurrentOrientation = getResources().getConfiguration().orientation;
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
        view.addOnLayoutChangeListener((parent, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> presenter.layoutChanged());
        view.setOnSystemUiVisibilityChangeListener(visibility -> {
            boolean ui = (visibility & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0;
            presenter.uiVisibilityChanged(ui);
        });

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
        speakerButton.setChecked(presenter.isSpeakerphoneOn());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCompositeDisposable.clear();
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int newOrientation = newConfig.orientation;
        if (newOrientation == mCurrentOrientation) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getActivity().isInPictureInPictureMode()) {
            // avoid restarting the camera when entering PIP mode
            return;
        }
        mCurrentOrientation = newOrientation;
        presenter.configurationChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.ac_call, m);
        dialPadBtn = m.findItem(R.id.menuitem_dialpad);
        changeScreenOrientationBtn = m.findItem(R.id.menuitem_change_screen_orientation);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        presenter.prepareOptionMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.menuitem_chat:
                presenter.chatClick();
                break;
            case R.id.menuitem_dialpad:
                presenter.dialpadClick();
                break;
            case R.id.menuitem_change_screen_orientation:
                presenter.screenRotationClick();
                break;
        }
        return true;
    }


    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (isInPictureInPictureMode)
            ((CallActivity) getActivity()).getSupportActionBar().hide();
        else
            ((CallActivity) getActivity()).getSupportActionBar().show();
        presenter.pipModeChanged(isInPictureInPictureMode);
    }

    @Override
    public void blockScreenRotation() {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    @Override
    public void displayContactBubble(final boolean display) {
        contactBubbleLayout.getHandler().post(() -> contactBubbleLayout.setVisibility(display ? View.VISIBLE : View.GONE));
    }

    @Override
    public void displayVideoSurface(final boolean display) {
        mVideoSurface.setVisibility(display ? View.VISIBLE : View.GONE);
        mVideoPreview.setVisibility(display ? View.VISIBLE : View.GONE);
        updateMenu();
    }

    @Override
    public void displayPreviewSurface(final boolean display) {
        if (display) {
            mVideoSurface.setZOrderOnTop(false);
            mVideoPreview.setZOrderMediaOverlay(true);
            mVideoSurface.setZOrderMediaOverlay(false);
        } else {
            mVideoPreview.setZOrderMediaOverlay(false);
            mVideoSurface.setZOrderMediaOverlay(true);
            mVideoSurface.setZOrderOnTop(true);
        }
    }

    @Override
    public void displayHangupButton(boolean display) {
        controlLayout.setVisibility(display ? View.VISIBLE : View.GONE);
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
        flipCameraBtn.setImageResource(isFront ? R.drawable.ic_camera_front_white : R.drawable.ic_camera_rear_white);
    }

    @Override
    public void updateMenu() {
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void changeScreenRotation() {
        if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void updateTime(final long duration) {
        if (mCallStatusTxt != null)
            mCallStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
    }

    @Override
    public void updateContactBubble(@NonNull final CallContact contact) {
        String username = contact.getRingUsername();
        String displayName = contact.getDisplayName();

        String ringId = contact.getIds().get(0);
        Log.d(TAG, "updateContactBubble: contact=" + contact + " username=" + username + ", ringId=" + ringId);

        boolean hasProfileName = displayName != null && !displayName.contentEquals(username);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar ab = activity.getSupportActionBar();
            if (ab != null) {
                if (hasProfileName) {
                    ab.setTitle(displayName);
                    ab.setSubtitle(username);
                } else {
                    ab.setTitle(username);
                    ab.setSubtitle(null);
                }
                ab.setDisplayShowTitleEnabled(true);
            }
        }

        if (hasProfileName) {
            contactBubbleNumTxt.setVisibility(View.VISIBLE);
            contactBubbleTxt.setText(displayName);
            contactBubbleNumTxt.setText(username);
        } else {
            contactBubbleNumTxt.setVisibility(View.GONE);
            contactBubbleTxt.setText(username);
        }

        contactBubbleView.setImageDrawable(new AvatarDrawable(getActivity(), contact));
    }

    @Override
    public void updateCallStatus(final SipCall.State callState) {
        getActivity().runOnUiThread(() -> {
            switch (callState) {
                case NONE:
                    mCallStatusTxt.setText("");
                    break;
                default:
                    mCallStatusTxt.setText(callStateToHumanState(callState));
                    break;
            }
        });
    }

    @Override
    public void initMenu(boolean isSpeakerOn, boolean hasContact, boolean displayFlip, boolean canDial, boolean onGoingCall) {
        if (flipCameraBtn != null) {
            flipCameraBtn.setVisibility(displayFlip ? View.VISIBLE : View.GONE);
        }
        if (dialPadBtn != null) {
            dialPadBtn.setVisible(canDial);
        }
        if (changeScreenOrientationBtn != null) {
            changeScreenOrientationBtn.setVisible(mVideoSurface.getVisibility() == View.VISIBLE);
        }
    }

    @Override
    public void initNormalStateDisplay(final boolean audioOnly, boolean isSpeakerphoneOn) {
        shapeRipple.stopRipple();

        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.GONE);
        controlLayout.setVisibility(View.VISIBLE);
        hangupButton.setVisibility(View.VISIBLE);

        contactBubbleLayout.setVisibility(audioOnly ? View.VISIBLE : View.GONE);
        speakerButton.setChecked(isSpeakerphoneOn);

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void initIncomingCallDisplay() {
        acceptButton.setVisibility(View.VISIBLE);
        refuseButton.setVisibility(View.VISIBLE);
        controlLayout.setVisibility(View.GONE);
        hangupButton.setVisibility(View.GONE);

        contactBubbleLayout.setVisibility(View.VISIBLE);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void initOutGoingCallDisplay() {
        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.VISIBLE);
        controlLayout.setVisibility(View.GONE);
        hangupButton.setVisibility(View.GONE);

        contactBubbleLayout.setVisibility(View.VISIBLE);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void resetVideoSize(final int videoWidth, final int videoHeight, final int previewWidth, final int previewHeight) {
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

        if (previewWidth == -1 && previewHeight == -1)
            return;
        Log.w(TAG, "resetVideoSize preview: " + previewWidth + "x" + previewHeight);
        ViewGroup.LayoutParams paramsPreview = mVideoPreview.getLayoutParams();
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        oldW = paramsPreview.width;
        oldH = paramsPreview.height;
        double previewMaxDim = Math.max(previewWidth, previewHeight);
        double previewRatio = metrics.density * 160. / previewMaxDim;
        paramsPreview.width = (int) (previewWidth * previewRatio);
        paramsPreview.height = (int) (previewHeight * previewRatio);

        if (oldW != paramsPreview.width || oldH != paramsPreview.height) {
            Log.w(TAG, "mVideoPreview.setLayoutParams: " + paramsPreview.width + "x" + paramsPreview.height + " was: " + oldW + "x"+oldH);
            mVideoPreview.setLayoutParams(paramsPreview);
        }

        /*final int mPreviewWidth;
        final int mPreviewHeight;

        if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mPreviewWidth = HardwareServiceImpl.VIDEO_HEIGHT;
            mPreviewHeight = HardwareServiceImpl.VIDEO_WIDTH;
        } else {
            mPreviewWidth = HardwareServiceImpl.VIDEO_WIDTH;
            mPreviewHeight = HardwareServiceImpl.VIDEO_HEIGHT;
        }

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
        }*/
    }

    @Override
    public void goToConversation(String accountId, String conversationId) {
        Intent intent = new Intent();
        if (DeviceUtils.isTablet(getActivity())) {
            intent.setClass(getActivity(), HomeActivity.class)
                    .setAction(DRingService.ACTION_CONV_ACCEPT)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, conversationId);
            startActivity(intent);
        } else {
            intent.setClass(getActivity(), ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, conversationId);
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    @Override
    public void goToAddContact(CallContact callContact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(callContact),
                ConversationFragment.REQ_ADD_CONTACT);
    }

    @Override
    public void finish() {
        getActivity().finish();
    }

    @OnClick({R.id.call_speaker_btn})
    public void speakerClicked() {
        presenter.speakerClick(speakerButton.isChecked());
    }

    @OnClick({R.id.call_mic_btn})
    public void micClicked() {
        presenter.muteMicrophoneToggled(micButton.isChecked());
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

    @OnClick(R.id.call_camera_flip_btn)
    public void cameraFlip() {
        presenter.switchVideoInputClick();
    }

    @Override
    public void positiveMediaButtonClicked() {
        presenter.positiveButtonClicked();
    }

    @Override
    public void negativeMediaButtonClicked() {
        presenter.negativeButtonClicked();
    }

    @Override
    public void toggleMediaButtonClicked() {
        presenter.toggleButtonClicked();
    }
}