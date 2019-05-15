/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Rational;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

import com.rodolfonavalon.shaperipplelibrary.model.Circle;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.call.CallPresenter;
import cx.ring.call.CallView;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragCallBinding;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.SipCall;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.service.DRingService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.NotificationService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.utils.Log;
import cx.ring.utils.MediaButtonsHelper;
import cx.ring.views.AvatarDrawable;
import io.reactivex.disposables.CompositeDisposable;

public class CallFragment extends BaseSupportFragment<CallPresenter> implements CallView, MediaButtonsHelper.MediaButtonsHelperCallback {

    public static final String TAG = CallFragment.class.getSimpleName();

    public static final String ACTION_PLACE_CALL = "PLACE_CALL";
    public static final String ACTION_GET_CALL = "GET_CALL";

    public static final String KEY_ACTION = "action";
    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_AUDIO_ONLY = "AUDIO_ONLY";

    private static final int REQUEST_PERMISSION_AUDIO_INCOMING = 1003;
    private static final int REQUEST_PERMISSION_VIDEO_INCOMING = 1004;
    private static final int REQUEST_PERMISSION_AUDIO_OUTGOING = 1005;
    private static final int REQUEST_PERMISSION_VIDEO_OUTGOING = 1006;
    private FragCallBinding binding;
    private OrientationEventListener mOrientationListener;

    private MenuItem dialPadBtn = null;
    private boolean restartVideo = false;
    private PowerManager.WakeLock mScreenWakeLock;
    private int mCurrentOrientation = 0;

    private int mVideoWidth = -1;
    private int mVideoHeight = -1;
    private int mPreviewWidth = 720, mPreviewHeight = 1280;
    private int mPreviewSurfaceWidth = 0, mPreviewSurfaceHeight = 0;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

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
        Bundle args = getArguments();
        if (args != null) {
            String action = args.getString(KEY_ACTION);
            if (action != null) {
                if (action.equals(ACTION_PLACE_CALL)) {
                    if (isPermissionAccepted(false)) {
                        initializeCall(false);
                    }
                } else if (action.equals(ACTION_GET_CALL)) {
                    presenter.initIncoming(getArguments().getString(KEY_CONF_ID));
                }
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
        Context context = requireContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams.Builder paramBuilder = new PictureInPictureParams.Builder();
            if (binding.videoSurface.getVisibility() == View.VISIBLE) {
                int[] l = new int[2];
                binding.videoSurface.getLocationInWindow(l);
                int x = l[0];
                int y = l[1];
                int w = binding.videoSurface.getWidth();
                int h = binding.videoSurface.getHeight();
                Rect videoBounds = new Rect(x, y, x + w, y + h);
                paramBuilder.setAspectRatio(new Rational(w, h));
                paramBuilder.setSourceRectHint(videoBounds);
            }
            ArrayList<RemoteAction> actions = new ArrayList<>(1);
            actions.add(new RemoteAction(
                    Icon.createWithResource(context, R.drawable.baseline_call_end_24),
                    getString(R.string.action_call_hangup),
                    getString(R.string.action_call_hangup),
                    PendingIntent.getService(context, new Random().nextInt(),
                            new Intent(DRingService.ACTION_CALL_END)
                                    .setClass(context, DRingService.class)
                                    .putExtra(NotificationService.KEY_CALL_ID, sipCall.getCallId()), PendingIntent.FLAG_ONE_SHOT)));
            paramBuilder.setActions(actions);
            requireActivity().enterPictureInPictureMode(paramBuilder.build());
        } else if (DeviceUtils.isTv(context)) {
            requireActivity().enterPictureInPictureMode();
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
        if (binding.videoSurface.getVisibility() == View.VISIBLE) {
            restartVideo = true;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        injectFragment(((RingApplication) requireActivity().getApplication()).getRingInjectionComponent());
        binding = DataBindingUtil.inflate(inflater, R.layout.frag_call, container, false);
        binding.setPresenter(this);
        return binding.getRoot();
    }

    private TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mPreviewSurfaceWidth = width;
            mPreviewSurfaceHeight = height;
            presenter.previewVideoSurfaceCreated(binding.previewSurface);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            mPreviewSurfaceWidth = width;
            mPreviewSurfaceHeight = height;
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            presenter.previewVideoSurfaceDestroyed();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);
        mCurrentOrientation = getResources().getConfiguration().orientation;
        PowerManager powerManager = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "ring:callLock");
        mScreenWakeLock.setReferenceCounted(false);

        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
        }

        binding.videoSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
        binding.videoSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
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
        view.setOnSystemUiVisibilityChangeListener(visibility -> {
            boolean ui = (visibility & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0;
            presenter.uiVisibilityChanged(ui);
        });
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                resetVideoSize(mVideoWidth, mVideoHeight));

        WindowManager windowManager = (WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE);
        mOrientationListener = new OrientationEventListener(getContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rot = windowManager.getDefaultDisplay().getRotation();
                if (mCurrentOrientation != rot) {
                    mCurrentOrientation = rot;
                    presenter.configurationChanged(rot);
                }
            }
        };
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }

        binding.shapeRipple.setRippleShape(new Circle());
        binding.callSpeakerBtn.setChecked(presenter.isSpeakerphoneOn());
        binding.callMicBtn.setChecked(presenter.isMicrophoneMuted());
        binding.previewSurface.setSurfaceTextureListener(listener);
        binding.previewSurface.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                configureTransform(mPreviewSurfaceWidth, mPreviewSurfaceHeight));

        binding.dialpadEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.sendDtmf(s.subSequence(start, start + count));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mOrientationListener != null) {
            mOrientationListener.disable();
            mOrientationListener = null;
        }
        mCompositeDisposable.clear();
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted;
        for (int i = 0, n = permissions.length; i < n; i++) {
            boolean videoGranted = mDeviceRuntimeService.hasVideoPermission();
            boolean audioGranted = mDeviceRuntimeService.hasAudioPermission();

            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    switch (requestCode) {
                        case REQUEST_PERMISSION_VIDEO_INCOMING:
                            granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            presenter.cameraPermissionChanged(granted);
                            if (granted && audioGranted) {
                                initializeCall(true);
                            }
                            break;
                        case REQUEST_PERMISSION_VIDEO_OUTGOING:
                            granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            presenter.cameraPermissionChanged(granted);
                            if (granted && audioGranted) {
                                initializeCall(false);
                            } else {
                                presenter.refuseCall();
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case Manifest.permission.RECORD_AUDIO:
                    switch (requestCode) {
                        case REQUEST_PERMISSION_VIDEO_INCOMING:
                            granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            presenter.audioPermissionChanged(granted);
                            if (granted && videoGranted) {
                                initializeCall(true);
                            }
                            break;
                        case REQUEST_PERMISSION_AUDIO_INCOMING:
                            granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            presenter.audioPermissionChanged(granted);
                            if (granted) {
                                initializeCall(true);
                            }
                            break;
                        case REQUEST_PERMISSION_VIDEO_OUTGOING:
                            granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            presenter.audioPermissionChanged(granted);
                            if (granted && videoGranted) {
                                initializeCall(false);
                            } else if (!granted) {
                                presenter.refuseCall();

                            }
                            break;
                        case REQUEST_PERMISSION_AUDIO_OUTGOING:
                            granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            presenter.audioPermissionChanged(granted);
                            if (granted) {
                                initializeCall(false);
                            } else {
                                presenter.refuseCall();
                            }
                            break;
                        default:
                            break;
                    }
                default:
                    break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.ac_call, m);
        dialPadBtn = m.findItem(R.id.menuitem_dialpad);
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
        }
        return true;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity == null ? null : activity.getSupportActionBar();
        if (actionBar != null) {
            if (isInPictureInPictureMode)
                actionBar.hide();
            else
                actionBar.show();
        }
        presenter.pipModeChanged(isInPictureInPictureMode);
    }

    @Override
    public void displayContactBubble(final boolean display) {
        binding.contactBubbleLayout.getHandler().post(() -> binding.contactBubbleLayout.setVisibility(display ? View.VISIBLE : View.GONE));
    }

    @Override
    public void displayVideoSurface(final boolean display) {
        binding.videoSurface.setVisibility(display ? View.VISIBLE : View.GONE);
        binding.previewContainer.setVisibility(display ? View.VISIBLE : View.GONE);
        updateMenu();
    }

    @Override
    public void displayPreviewSurface(final boolean display) {
        if (display) {
            binding.videoSurface.setZOrderOnTop(false);
            binding.videoSurface.setZOrderMediaOverlay(false);
        } else {
            binding.videoSurface.setZOrderMediaOverlay(true);
            binding.videoSurface.setZOrderOnTop(true);
        }
    }

    @Override
    public void displayHangupButton(boolean display) {
        binding.callControlGroup.setVisibility(display ? View.VISIBLE : View.GONE);
        binding.callHangupBtn.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayDialPadKeyboard() {
        KeyboardVisibilityManager.showKeyboard(getActivity(), binding.dialpadEditText, InputMethodManager.SHOW_FORCED);
    }

    @Override
    public void switchCameraIcon(boolean isFront) {
        binding.callCameraFlipBtn.setImageResource(isFront ? R.drawable.ic_camera_front_white : R.drawable.ic_camera_rear_white);
    }

    @Override
    public void updateMenu() {
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void updateTime(final long duration) {
        if (binding.callStatusTxt != null)
            binding.callStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
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
            binding.contactBubbleNumTxt.setVisibility(View.VISIBLE);
            binding.contactBubbleTxt.setText(displayName);
            binding.contactBubbleNumTxt.setText(username);
        } else {
            binding.contactBubbleNumTxt.setVisibility(View.GONE);
            binding.contactBubbleTxt.setText(username);
        }

        binding.contactBubble.setImageDrawable(new AvatarDrawable(getActivity(), contact));
    }

    @Override
    public void updateCallStatus(final SipCall.State callState) {
        switch (callState) {
            case NONE:
                binding.callStatusTxt.setText("");
                break;
            default:
                binding.callStatusTxt.setText(callStateToHumanState(callState));
                break;
        }
    }

    @Override
    public void initMenu(boolean isSpeakerOn, boolean hasContact, boolean displayFlip, boolean canDial, boolean onGoingCall) {
        if (binding.callCameraFlipBtn != null) {
            binding.callCameraFlipBtn.setVisibility(displayFlip ? View.VISIBLE : View.GONE);
        }
        if (dialPadBtn != null) {
            dialPadBtn.setVisible(canDial);
        }
    }

    @Override
    public void initNormalStateDisplay(final boolean audioOnly, boolean isSpeakerphoneOn, boolean isMuted) {
        binding.shapeRipple.stopRipple();

        binding.callAcceptBtn.setVisibility(View.GONE);
        binding.callRefuseBtn.setVisibility(View.GONE);
        binding.callControlGroup.setVisibility(View.VISIBLE);
        binding.callHangupBtn.setVisibility(View.VISIBLE);

        binding.contactBubbleLayout.setVisibility(audioOnly ? View.VISIBLE : View.GONE);
        binding.callSpeakerBtn.setChecked(isSpeakerphoneOn);
        binding.callMicBtn.setChecked(isMuted);

        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void initIncomingCallDisplay() {
        binding.callAcceptBtn.setVisibility(View.VISIBLE);
        binding.callRefuseBtn.setVisibility(View.VISIBLE);
        binding.callControlGroup.setVisibility(View.GONE);
        binding.callHangupBtn.setVisibility(View.GONE);

        binding.contactBubbleLayout.setVisibility(View.VISIBLE);
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void initOutGoingCallDisplay() {
        binding.callAcceptBtn.setVisibility(View.GONE);
        binding.callRefuseBtn.setVisibility(View.VISIBLE);
        binding.callControlGroup.setVisibility(View.GONE);
        binding.callHangupBtn.setVisibility(View.GONE);

        binding.contactBubbleLayout.setVisibility(View.VISIBLE);
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void resetPreviewVideoSize(int previewWidth, int previewHeight, int rot) {
        if (previewWidth == -1 && previewHeight == -1)
            return;
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        boolean flip = (rot % 180) != 0;
        binding.previewSurface.setAspectRatio(flip ? mPreviewHeight : mPreviewWidth, flip ? mPreviewWidth : mPreviewHeight);
    }

    @Override
    public void resetVideoSize(int videoWidth, int videoHeight) {
        ViewGroup rootView = (ViewGroup) getView();
        if (rootView == null)
            return;
        double videoRatio = videoWidth / (double) videoHeight;
        double screenRatio = rootView.getWidth() / (double) rootView.getHeight();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.videoSurface.getLayoutParams();
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
            binding.videoSurface.setLayoutParams(params);
        }
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == binding.previewSurface || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        boolean rot = Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation;
        // Log.w(TAG, "configureTransform " + viewWidth + "x" + viewHeight + " rot=" + rot + " mPreviewWidth=" + mPreviewWidth + " mPreviewHeight=" + mPreviewHeight);
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (rot) {
            RectF bufferRect = new RectF(0, 0, mPreviewHeight, mPreviewWidth);
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewHeight,
                    (float) viewWidth / mPreviewWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        binding.previewSurface.setTransform(matrix);
    }

    @Override
    public void goToConversation(String accountId, String conversationId) {
        Intent intent = new Intent();
        if (DeviceUtils.isTablet(requireActivity())) {
            intent.setClass(requireActivity(), HomeActivity.class)
                    .setAction(DRingService.ACTION_CONV_ACCEPT)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, conversationId);
            startActivity(intent);
        } else {
            intent.setClass(requireActivity(), ConversationActivity.class)
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

    /**
     * Checks if permissions are accepted for camera and microphone. Takes into account whether call is incoming and outgoing, and requests permissions if not available.
     *
     * @param isIncoming true if call is incoming, false for outgoing
     * @return true if permissions are already accepted
     */
    public boolean isPermissionAccepted(boolean isIncoming) {
        Bundle args = getArguments();
        boolean audioGranted = mDeviceRuntimeService.hasAudioPermission();
        boolean audioOnly;
        int REQUEST_PERMISSION_AUDIO, REQUEST_PERMISSION_VIDEO;

        if (isIncoming) {
            audioOnly = presenter.isAudioOnly();
            REQUEST_PERMISSION_AUDIO = REQUEST_PERMISSION_AUDIO_INCOMING;
            REQUEST_PERMISSION_VIDEO = REQUEST_PERMISSION_VIDEO_INCOMING;

        } else {
            audioOnly = args.getBoolean(KEY_AUDIO_ONLY);
            REQUEST_PERMISSION_AUDIO = REQUEST_PERMISSION_AUDIO_OUTGOING;
            REQUEST_PERMISSION_VIDEO = REQUEST_PERMISSION_VIDEO_OUTGOING;
        }
        if (!audioOnly) {
            boolean videoGranted = mDeviceRuntimeService.hasVideoPermission();

            if (!audioGranted || !videoGranted) {
                ArrayList<String> perms = new ArrayList<>();
                if (!audioGranted)
                    perms.add(Manifest.permission.RECORD_AUDIO);
                if (!videoGranted) {
                    perms.add(Manifest.permission.CAMERA);
                }
                requestPermissions(perms.toArray(new String[perms.size()]), REQUEST_PERMISSION_VIDEO);
            } else {
                return true;
            }
        } else {
            if (!audioGranted) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_PERMISSION_AUDIO);
            } else {
                return true;
            }
        }
        return false;

    }

    /**
     * Starts a call. Takes into account whether call is incoming or outgoing.
     *
     * @param isIncoming true if call is incoming, false for outgoing
     */
    public void initializeCall(boolean isIncoming) {
        if (isIncoming) {
            presenter.acceptCall();
        } else {
            Bundle args;
            args = getArguments();
            if (args != null) {
                presenter.initOutGoing(getArguments().getString(KEY_ACCOUNT_ID),
                        getArguments().getString(ConversationFragment.KEY_CONTACT_RING_ID),
                        getArguments().getBoolean(KEY_AUDIO_ONLY));
            }
        }
    }

    @Override
    public void finish() {
        Activity activity = getActivity();
        if (activity != null)
            activity.finish();
    }

    public void speakerClicked() {
        presenter.speakerClick(binding.callSpeakerBtn.isChecked());
    }

    public void micClicked() {
        presenter.muteMicrophoneToggled(binding.callMicBtn.isChecked());
    }

    public void hangUpClicked() {
        presenter.hangupCall();
    }

    public void refuseClicked() {
        presenter.refuseCall();
    }

    public void acceptClicked() {
        if (isPermissionAccepted(true))
            presenter.acceptCall();
    }

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