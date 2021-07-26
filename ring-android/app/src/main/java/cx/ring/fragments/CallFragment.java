/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Rational;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.percentlayout.widget.PercentFrameLayout;

import com.rodolfonavalon.shaperipplelibrary.model.Circle;

import net.jami.call.CallPresenter;
import net.jami.call.CallView;
import net.jami.daemon.JamiService;
import net.jami.model.Call;
import net.jami.model.Conference;
import net.jami.model.Contact;
import net.jami.model.Uri;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;
import net.jami.services.NotificationService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.adapters.ConfParticipantAdapter;
import cx.ring.application.JamiApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.ContactDetailsActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.ConversationSelectionActivity;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragCallBinding;
import cx.ring.databinding.ItemParticipantLabelBinding;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.plugins.RecyclerPicker.RecyclerPicker;
import cx.ring.plugins.RecyclerPicker.RecyclerPickerLayoutManager;
import cx.ring.service.DRingService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.MediaButtonsHelper;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class CallFragment extends BaseSupportFragment<CallPresenter, CallView> implements CallView, MediaButtonsHelper.MediaButtonsHelperCallback, RecyclerPickerLayoutManager.ItemSelectedListener {

    public static final String TAG = CallFragment.class.getSimpleName();

    public static final String ACTION_PLACE_CALL = "PLACE_CALL";
    public static final String ACTION_GET_CALL = "GET_CALL";

    public static final String KEY_ACTION = "action";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_AUDIO_ONLY = "AUDIO_ONLY";

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 6;
    private static final int REQUEST_PERMISSION_INCOMING = 1003;
    private static final int REQUEST_PERMISSION_OUTGOING = 1004;
    private static final int REQUEST_CODE_SCREEN_SHARE = 7;

    private FragCallBinding binding;
    private OrientationEventListener mOrientationListener;

    private MenuItem dialPadBtn = null;
    private MenuItem pluginsMenuBtn = null;
    private boolean restartVideo = false;
    private boolean restartPreview = false;
    private PowerManager.WakeLock mScreenWakeLock = null;
    private int mCurrentOrientation = 0;

    private int mVideoWidth = -1;
    private int mVideoHeight = -1;
    private int mPreviewWidth = 720, mPreviewHeight = 1280;
    private int mPreviewSurfaceWidth = 0, mPreviewSurfaceHeight = 0;

    private MediaProjectionManager mProjectionManager;

    private boolean mBackstackLost = false;

    private ConfParticipantAdapter confAdapter = null;
    private boolean mConferenceMode = false;
    private boolean choosePluginMode = false;
    public boolean isChoosePluginMode() {
        return choosePluginMode;
    }
    private boolean pluginsModeFirst = true;
    private List<String> callMediaHandlers;
    private int previousPluginPosition = -1;
    private RecyclerPicker rp;
    private final ValueAnimator animation = new ValueAnimator();

    private PointF previewDrag = null;
    private final ValueAnimator previewSnapAnimation = new ValueAnimator();
    private final int[] previewMargins = new int[4];
    private float previewHiddenState = 0;
    private enum PreviewPosition {LEFT, RIGHT}
    private PreviewPosition previewPosition = PreviewPosition.RIGHT;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public static CallFragment newInstance(@NonNull String action, @Nullable ConversationPath path, @Nullable String contactId, boolean audioOnly) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        if (path != null)
            path.toBundle(bundle);
        bundle.putString(Intent.EXTRA_PHONE_NUMBER, contactId);
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

    public static int callStateToHumanState(final Call.CallStatus state) {
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
                return R.string.call_human_state_none;
        }
    }

    @Override
    protected void initPresenter(CallPresenter presenter) {
        Bundle args = getArguments();
        if (args != null) {
            String action = args.getString(KEY_ACTION);
            if (action != null) {
                if (action.equals(ACTION_PLACE_CALL)) {
                    prepareCall(false);
                } else if (action.equals(ACTION_GET_CALL) || action.equals(CallActivity.ACTION_CALL_ACCEPT)) {
                    presenter.initIncomingCall(getArguments().getString(KEY_CONF_ID), action.equals(ACTION_GET_CALL));
                }
            }
        }
    }

    public void onUserLeave() {
        presenter.requestPipMode();
    }

    @Override
    public void enterPipMode(String callId) {
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
            } else {
                return;
            }
            ArrayList<RemoteAction> actions = new ArrayList<>(1);
            actions.add(new RemoteAction(
                    Icon.createWithResource(context, R.drawable.baseline_call_end_24),
                    getString(R.string.action_call_hangup),
                    getString(R.string.action_call_hangup),
                    PendingIntent.getService(context, new Random().nextInt(),
                            new Intent(DRingService.ACTION_CALL_END)
                                    .setClass(context, JamiService.class)
                                    .putExtra(NotificationService.KEY_CALL_ID, callId), PendingIntent.FLAG_ONE_SHOT)));
            paramBuilder.setActions(actions);
            try {
                requireActivity().enterPictureInPictureMode(paramBuilder.build());
            } catch (Exception e) {
                Log.w(TAG, "Can't enter  PIP mode", e);
            }
        } else if (DeviceUtils.isTv(context)) {
            requireActivity().enterPictureInPictureMode();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (restartVideo && restartPreview) {
            displayVideoSurface(true, !presenter.isPipMode());
            restartVideo = false;
            restartPreview = false;
        } else if (restartVideo) {
            displayVideoSurface(true, false);
            restartVideo = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        previewSnapAnimation.cancel();
        if (binding.videoSurface.getVisibility() == View.VISIBLE) {
            restartVideo = true;
        }
        if (!choosePluginMode) {
            if (binding.previewContainer.getVisibility() == View.VISIBLE) {
                restartPreview = true;
            }
        }else {
            if (binding.pluginPreviewContainer.getVisibility() == View.VISIBLE) {
                restartPreview = true;
                presenter.stopPlugin();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.frag_call, container, false);
        binding.setPresenter(this);
        rp = new RecyclerPicker(binding.recyclerPicker,
                R.layout.item_picker,
                LinearLayout.HORIZONTAL, this);
        rp.setFirstLastElementsWidths(112, 112);
        return binding.getRoot();
    }

    private final TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
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
            configurePreview(width, 1);
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

    /**
     * @param hiddenState 0.f if fully shown, 1.f if fully hidden.
     */
    private void setPreviewDragHiddenState(float hiddenState) {
        binding.previewSurface.setAlpha(1.f - (3 * hiddenState / 4));
        binding.pluginPreviewSurface.setAlpha(1.f - (3 * hiddenState / 4));
        binding.previewHandle.setAlpha(hiddenState);
        binding.pluginPreviewHandle.setAlpha(hiddenState);
    }

    @SuppressLint({"ClickableViewAccessibility", "RtlHardcoded", "WakelockTimeout"})
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);
        mCurrentOrientation = getResources().getConfiguration().orientation;
        float dpRatio = requireActivity().getResources().getDisplayMetrics().density;

        animation.setDuration(150);
        animation.addUpdateListener(valueAnimator -> {
            if (binding == null)
                return;
            int upBy = (int) valueAnimator.getAnimatedValue();
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.previewContainer.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, (int) (upBy * dpRatio));
            binding.previewContainer.setLayoutParams(layoutParams);
        });

        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (activity instanceof AppCompatActivity) {
                AppCompatActivity ac_activity = (AppCompatActivity) activity;
                ActionBar ab = ac_activity.getSupportActionBar();
                if (ab != null) {
                    ab.setHomeAsUpIndicator(R.drawable.baseline_chat_24);
                    ab.setDisplayHomeAsUpEnabled(true);
                }
            }
        }

        mProjectionManager = (MediaProjectionManager) requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        PowerManager powerManager = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "ring:callLock");
            mScreenWakeLock.setReferenceCounted(false);
            if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
                mScreenWakeLock.acquire();
            }
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

        binding.pluginPreviewSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
        binding.pluginPreviewSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                presenter.pluginSurfaceCreated(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                presenter.pluginSurfaceDestroyed();
            }
        });

        view.setOnSystemUiVisibilityChangeListener(visibility -> {
            boolean ui = (visibility & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0;
            presenter.uiVisibilityChanged(ui);
        });
        boolean ui = (view.getSystemUiVisibility() & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0;
        presenter.uiVisibilityChanged(ui);

        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                resetVideoSize(mVideoWidth, mVideoHeight));

        WindowManager windowManager = (WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
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
        }

        binding.shapeRipple.setRippleShape(new Circle());
        binding.callSpeakerBtn.setChecked(presenter.isSpeakerphoneOn());
        binding.callMicBtn.setChecked(presenter.isMicrophoneMuted());
        binding.pluginPreviewSurface.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                configureTransform(mPreviewSurfaceWidth, mPreviewSurfaceHeight));
        binding.previewSurface.setSurfaceTextureListener(listener);
        binding.previewSurface.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                configureTransform(mPreviewSurfaceWidth, mPreviewSurfaceHeight));

        previewSnapAnimation.setDuration(250);
        previewSnapAnimation.setFloatValues(0.f, 1.f);
        previewSnapAnimation.setInterpolator(new DecelerateInterpolator());
        previewSnapAnimation.addUpdateListener(animation -> {
            float animatedFraction = animation == null ? 1 : animation.getAnimatedFraction();
            configurePreview(mPreviewSurfaceWidth, animatedFraction);
        });

        binding.previewContainer.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            RelativeLayout parent = (RelativeLayout) v.getParent();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();

            if (action == MotionEvent.ACTION_DOWN) {
                previewSnapAnimation.cancel();
                previewDrag = new PointF(event.getX(), event.getY());
                v.setElevation(v.getContext().getResources().getDimension(R.dimen.call_preview_elevation_dragged));
                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.setMargins((int) v.getX(), (int) v.getY(), parent.getWidth() - ((int) v.getX() + v.getWidth()), parent.getHeight() - ((int) v.getY() + v.getHeight()));
                v.setLayoutParams(params);
                return true;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (previewDrag != null) {
                    int currentXPosition = params.leftMargin + (int) (event.getX() - previewDrag.x);
                    int currentYPosition = params.topMargin + (int) (event.getY() - previewDrag.y);
                    params.setMargins(
                            currentXPosition,
                            currentYPosition,
                            -((currentXPosition + v.getWidth()) - (int) event.getX()),
                            -((currentYPosition + v.getHeight()) - (int) event.getY()));
                    v.setLayoutParams(params);

                    float outPosition = binding.previewContainer.getWidth() * 0.85f;
                    float drapOut = 0.f;
                    if (currentXPosition < 0) {
                        drapOut = Math.min(1.f, -currentXPosition / outPosition);
                    } else if (currentXPosition + v.getWidth() > parent.getWidth()) {
                        drapOut = Math.min(1.f, (currentXPosition + v.getWidth() - parent.getWidth()) / outPosition);
                    }
                    setPreviewDragHiddenState(drapOut);
                    return true;
                }
                return false;
            } else if (action == MotionEvent.ACTION_UP) {
                if (previewDrag != null) {
                    int currentXPosition = params.leftMargin + (int) (event.getX() - previewDrag.x);

                    previewSnapAnimation.cancel();
                    previewDrag = null;
                    v.setElevation(v.getContext().getResources().getDimension(R.dimen.call_preview_elevation));
                    int ml = 0, mr = 0, mt = 0, mb = 0;

                    FrameLayout.LayoutParams hp = (FrameLayout.LayoutParams) binding.previewHandle.getLayoutParams();
                    if (params.leftMargin + (v.getWidth() / 2) > parent.getWidth() / 2) {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        mr = (int) (parent.getWidth() - v.getWidth() - v.getX());
                        previewPosition = PreviewPosition.RIGHT;
                        hp.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                    } else {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        ml = (int) v.getX();
                        previewPosition = PreviewPosition.LEFT;
                        hp.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
                    }
                    binding.previewHandle.setLayoutParams(hp);

                    if (params.topMargin + (v.getHeight() / 2) > parent.getHeight() / 2) {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        mb = (int) (parent.getHeight() - v.getHeight() - v.getY());
                    } else {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        mt = (int) v.getY();
                    }
                    previewMargins[0] = ml;
                    previewMargins[1] = mt;
                    previewMargins[2] = mr;
                    previewMargins[3] = mb;
                    params.setMargins(ml, mt, mr, mb);
                    v.setLayoutParams(params);

                    float outPosition = binding.previewContainer.getWidth() * 0.85f;
                    previewHiddenState = currentXPosition < 0
                            ? Math.min(1.f, -currentXPosition / outPosition)
                            : ((currentXPosition + v.getWidth() > parent.getWidth())
                                ? Math.min(1.f, (currentXPosition + v.getWidth() - parent.getWidth()) / outPosition)
                                : 0.f);
                    setPreviewDragHiddenState(previewHiddenState);

                    previewSnapAnimation.start();
                    return true;
                }
                return false;
            } else {
                return false;
            }
        });

        binding.pluginPreviewContainer.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            RelativeLayout parent = (RelativeLayout) v.getParent();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();

            if (action == MotionEvent.ACTION_DOWN) {
                previewSnapAnimation.cancel();
                previewDrag = new PointF(event.getX(), event.getY());
                v.setElevation(v.getContext().getResources().getDimension(R.dimen.call_preview_elevation_dragged));
                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.setMargins((int) v.getX(), (int) v.getY(), parent.getWidth() - ((int) v.getX() + v.getWidth()), parent.getHeight() - ((int) v.getY() + v.getHeight()));
                v.setLayoutParams(params);
                return true;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (previewDrag != null) {
                    int currentXPosition = params.leftMargin + (int) (event.getX() - previewDrag.x);
                    int currentYPosition = params.topMargin + (int) (event.getY() - previewDrag.y);
                    params.setMargins(
                            currentXPosition,
                            currentYPosition,
                            -((currentXPosition + v.getWidth()) - (int) event.getX()),
                            -((currentYPosition + v.getHeight()) - (int) event.getY()));
                    v.setLayoutParams(params);

                    float outPosition = binding.pluginPreviewContainer.getWidth() * 0.85f;
                    float drapOut = 0.f;
                    if (currentXPosition < 0) {
                        drapOut = Math.min(1.f, -currentXPosition / outPosition);
                    } else if (currentXPosition + v.getWidth() > parent.getWidth()) {
                        drapOut = Math.min(1.f, (currentXPosition + v.getWidth() - parent.getWidth()) / outPosition);
                    }
                    setPreviewDragHiddenState(drapOut);
                    return true;
                }
                return false;
            } else if (action == MotionEvent.ACTION_UP) {
                if (previewDrag != null) {
                    int currentXPosition = params.leftMargin + (int) (event.getX() - previewDrag.x);

                    previewSnapAnimation.cancel();
                    previewDrag = null;
                    v.setElevation(v.getContext().getResources().getDimension(R.dimen.call_preview_elevation));
                    int ml = 0, mr = 0, mt = 0, mb = 0;

                    FrameLayout.LayoutParams hp = (FrameLayout.LayoutParams) binding.pluginPreviewHandle.getLayoutParams();
                    if (params.leftMargin + (v.getWidth() / 2) > parent.getWidth() / 2) {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        mr = (int) (parent.getWidth() - v.getWidth() - v.getX());
                        previewPosition = PreviewPosition.RIGHT;
                        hp.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                    } else {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        ml = (int) v.getX();
                        previewPosition = PreviewPosition.LEFT;
                        hp.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
                    }
                    binding.pluginPreviewHandle.setLayoutParams(hp);

                    if (params.topMargin + (v.getHeight() / 2) > parent.getHeight() / 2) {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        mb = (int) (parent.getHeight() - v.getHeight() - v.getY());
                    } else {
                        params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        mt = (int) v.getY();
                    }
                    previewMargins[0] = ml;
                    previewMargins[1] = mt;
                    previewMargins[2] = mr;
                    previewMargins[3] = mb;
                    params.setMargins(ml, mt, mr, mb);
                    v.setLayoutParams(params);

                    float outPosition = binding.pluginPreviewContainer.getWidth() * 0.85f;
                    previewHiddenState = currentXPosition < 0
                            ? Math.min(1.f, -currentXPosition / outPosition)
                            : ((currentXPosition + v.getWidth() > parent.getWidth())
                            ? Math.min(1.f, (currentXPosition + v.getWidth() - parent.getWidth()) / outPosition)
                            : 0.f);
                    setPreviewDragHiddenState(previewHiddenState);

                    previewSnapAnimation.start();
                    return true;
                }
                return false;
            } else {
                return false;
            }
        });

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

    private void configurePreview(int width, float animatedFraction) {
        Context context = getContext();
        if (context == null || binding == null)
            return;
        float margin = context.getResources().getDimension(R.dimen.call_preview_margin);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.previewContainer.getLayoutParams();
        float r = 1.f - animatedFraction;
        float hideMargin = 0.f;
        float targetHiddenState = 0.f;
        if (previewHiddenState > 0.f) {
            targetHiddenState = 1.f;
            float v = width * 0.85f * animatedFraction;
            hideMargin = previewPosition == PreviewPosition.RIGHT ? v : -v;
        }
        setPreviewDragHiddenState(previewHiddenState * r + targetHiddenState * animatedFraction);

        float f = margin * animatedFraction;
        params.setMargins(
                (int) (previewMargins[0] * r + f + hideMargin),
                (int) (previewMargins[1] * r + f),
                (int) (previewMargins[2] * r + f - hideMargin),
                (int) (previewMargins[3] * r + f));
        binding.previewContainer.setLayoutParams(params);
        binding.pluginPreviewContainer.setLayoutParams(params);
    }

    /**
     * Releases current wakelock and acquires a new proximity wakelock if current call is audio only.
     *
     * @param isAudioOnly true if it is an audio call
     */
    @SuppressLint("WakelockTimeout")
    @Override
    public void handleCallWakelock(boolean isAudioOnly) {
        if (isAudioOnly) {
            if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
                mScreenWakeLock.release();
            }
            PowerManager powerManager = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                mScreenWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "ring:callLock");
                mScreenWakeLock.setReferenceCounted(false);

                if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
                    mScreenWakeLock.acquire();
                }
            }
        }
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
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSION_INCOMING && requestCode != REQUEST_PERMISSION_OUTGOING)
            return;
        for (int i = 0, n = permissions.length; i < n; i++) {
            boolean audioGranted = mDeviceRuntimeService.hasAudioPermission();
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    presenter.cameraPermissionChanged(granted);
                    if (audioGranted) {
                        initializeCall(requestCode == REQUEST_PERMISSION_INCOMING);
                    }
                    break;
                case Manifest.permission.RECORD_AUDIO:
                    presenter.audioPermissionChanged(granted);
                    initializeCall(requestCode == REQUEST_PERMISSION_INCOMING);
                    break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ConversationPath path = ConversationPath.fromUri(data.getData());
                if (path != null) {
                    presenter.addConferenceParticipant(path.getAccountId(), path.getConversationUri());
                }
            }
        } else if (requestCode == REQUEST_CODE_SCREEN_SHARE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    startScreenShare(mProjectionManager.getMediaProjection(resultCode, data));
                } catch (Exception e) {
                    Log.w(TAG, "Error starting screen sharing", e);
                }
            } else {
                binding.callScreenshareBtn.setChecked(false);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu m, @NonNull MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.ac_call, m);
        dialPadBtn = m.findItem(R.id.menuitem_dialpad);
        pluginsMenuBtn = m.findItem(R.id.menuitem_video_plugins);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        presenter.prepareOptionMenu();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            presenter.chatClick();
        } else if (itemId == R.id.menuitem_dialpad) {
            presenter.dialpadClick();
        } else if (itemId == R.id.menuitem_video_plugins) {
            displayVideoPluginsCarousel();
        }
        return true;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity == null ? null : activity.getSupportActionBar();
        if (actionBar != null) {
            if (isInPictureInPictureMode) {
                actionBar.hide();
            } else {
                mBackstackLost = true;
                actionBar.show();
            }
        }
        presenter.pipModeChanged(isInPictureInPictureMode);
    }

    @Override
    public void displayContactBubble(final boolean display) {
        if (binding != null)
            binding.contactBubbleLayout.getHandler().post(() -> {
                if (binding != null) binding.contactBubbleLayout.setVisibility(display ? View.VISIBLE : View.GONE);
            });
    }

    @Override
    public void displayVideoSurface(final boolean displayVideoSurface, final boolean displayPreviewContainer) {
        binding.videoSurface.setVisibility(displayVideoSurface ? View.VISIBLE : View.GONE);
        if (choosePluginMode) {
            binding.pluginPreviewSurface.setVisibility(displayPreviewContainer ? View.VISIBLE : View.GONE);
            binding.pluginPreviewContainer.setVisibility(displayPreviewContainer ? View.VISIBLE : View.GONE);
            binding.previewContainer.setVisibility(View.GONE);
        } else {
            binding.pluginPreviewSurface.setVisibility(View.GONE);
            binding.pluginPreviewContainer.setVisibility(View.GONE);
            binding.previewContainer.setVisibility(displayPreviewContainer ? View.VISIBLE : View.GONE);
        }
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
        Log.w(TAG, "displayHangupButton " + display);
        display &= !choosePluginMode;
        binding.callControlGroup.setVisibility(display ? View.VISIBLE : View.GONE);
        binding.callHangupBtn.setVisibility(display ? View.VISIBLE : View.GONE);
        binding.confControlGroup.setVisibility((mConferenceMode && display) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayDialPadKeyboard() {
        binding.dialpadEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) binding.dialpadEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    @Override
    public void switchCameraIcon(boolean isFront) {
        binding.callCameraFlipBtn.setImageResource(isFront ? R.drawable.baseline_camera_front_24 : R.drawable.baseline_camera_rear_24);
    }

    @Override
    public void updateAudioState(HardwareService.AudioState state) {
        binding.callSpeakerBtn.setChecked(state.getOutputType() == HardwareService.AudioOutput.SPEAKERS);
    }

    @Override
    public void updateMenu() {
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void updateTime(final long duration) {
        if (binding != null) {
            if (duration <= 0)
                binding.callStatusTxt.setText(null);
            else
                binding.callStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
        }
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void updateContactBubble(@NonNull final List<Call> contacts) {
        Log.w(TAG, "updateContactBubble " + contacts.size());

        String username = contacts.size() > 1 ? "Conference with " + contacts.size() + " people" : contacts.get(0).getContact().getDisplayName();
        String displayName = contacts.size() > 1 ? null : contacts.get(0).getContact().getDisplayName();

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

        binding.contactBubble.setImageDrawable(
                new AvatarDrawable.Builder()
                        .withContact(contacts.get(0).getContact())
                        .withCircleCrop(true)
                        .withPresence(false)
                        .build(getActivity())
        );

    }

    @SuppressLint("RestrictedApi")
    @Override
    public void updateConfInfo(List<Conference.ParticipantInfo> participantInfo) {
        Log.w(TAG, "updateConfInfo " + participantInfo);

        mConferenceMode = participantInfo.size() > 1;

        binding.participantLabelContainer.removeAllViews();
        if (!participantInfo.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(binding.participantLabelContainer.getContext());
            for (Conference.ParticipantInfo i : participantInfo) {
                String displayName = i.contact.getDisplayName();
                if (!TextUtils.isEmpty(displayName)) {
                    ItemParticipantLabelBinding label = ItemParticipantLabelBinding.inflate(inflater);
                    PercentFrameLayout.LayoutParams params = new PercentFrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.getPercentLayoutInfo().leftMarginPercent = i.x / (float) mVideoWidth;
                    params.getPercentLayoutInfo().topMarginPercent = i.y / (float) mVideoHeight;
                    params.getPercentLayoutInfo().rightMarginPercent = 1.f - (i.x + i.w) / (float) mVideoWidth;
                    //params.getPercentLayoutInfo().rightMarginPercent = (i.x + i.w) / (float) mVideoWidth;
                    label.participantName.setText(displayName);
                    label.moderator.setVisibility(i.isModerator ? View.VISIBLE : View.GONE);
                    label.mute.setVisibility(i.audioMuted ? View.VISIBLE : View.GONE);
                    binding.participantLabelContainer.addView(label.getRoot(), params);
                }
            }
        }
        binding.participantLabelContainer.setVisibility(participantInfo.isEmpty() ? View.GONE : View.VISIBLE);

        if (participantInfo.isEmpty() || participantInfo.size() < 2) {
            binding.confControlGroup.setVisibility(View.GONE);
        } else {
            binding.confControlGroup.setVisibility(View.VISIBLE);
            if (confAdapter == null) {
                confAdapter = new ConfParticipantAdapter((view, info) -> {
                    if (presenter == null)
                        return;
                    boolean maximized = presenter.isMaximized(info);
                    PopupMenu popup = new PopupMenu(view.getContext(), view);
                    popup.inflate(R.menu.conference_participant_actions);
                    popup.setOnMenuItemClickListener(item -> {
                        if (presenter == null)
                            return false;
                        int itemId = item.getItemId();
                        if (itemId == R.id.conv_contact_details) {
                            presenter.openParticipantContact(info);
                        } else if (itemId == R.id.conv_contact_hangup) {
                            presenter.hangupParticipant(info);
                        } else if (itemId == R.id.conv_mute) {
                            //call.muteAudio(!info.audioMuted);
                            presenter.muteParticipant(info, !info.audioMuted);
                        } else if (itemId == R.id.conv_contact_maximize) {
                            presenter.maximizeParticipant(info);
                        } else {
                            return false;
                        }
                        return true;
                    });
                    MenuBuilder menu = (MenuBuilder) popup.getMenu();
                    MenuItem maxItem = menu.findItem(R.id.conv_contact_maximize);
                    MenuItem muteItem = menu.findItem(R.id.conv_mute);
                    if (maximized) {
                        maxItem.setTitle(R.string.action_call_minimize);
                        maxItem.setIcon(R.drawable.baseline_close_fullscreen_24);
                    } else {
                        maxItem.setTitle(R.string.action_call_maximize);
                        maxItem.setIcon(R.drawable.baseline_open_in_full_24);
                    }
                    if (!info.audioMuted) {
                        muteItem.setTitle(R.string.action_call_mute);
                        muteItem.setIcon(R.drawable.baseline_mic_off_24);
                    } else {
                        muteItem.setTitle(R.string.action_call_unmute);
                        muteItem.setIcon(R.drawable.baseline_mic_24);
                    }
                    MenuPopupHelper menuHelper = new MenuPopupHelper(view.getContext(), menu, view);
                    menuHelper.setGravity(Gravity.END);
                    menuHelper.setForceShowIcon(true);
                    menuHelper.show();
                });
            }
            confAdapter.updateFromCalls(participantInfo);
            if (binding.confControlGroup.getAdapter() == null)
                binding.confControlGroup.setAdapter(confAdapter);
        }
    }

    @Override
    public void updateParticipantRecording(Set<Contact> contacts) {
        if (contacts.size() == 0) {
            binding.recordLayout.setVisibility(View.INVISIBLE);
            binding.recordIndicator.clearAnimation();
            return;
        }
        StringBuilder names = new StringBuilder();
        Iterator<Contact> contact =  contacts.iterator();
        for (int i = 0; i < contacts.size(); i++) {
            names.append(" ").append(contact.next().getDisplayName());
            if (i != contacts.size() - 1) {
                names.append(",");
            }
        }
        binding.recordLayout.setVisibility(View.VISIBLE);
        binding.recordIndicator.setAnimation(getBlinkingAnimation());
        binding.recordName.setText(getString(R.string.remote_recording, names));
    }

    @Override
    public void updateCallStatus(final Call.CallStatus callStatus) {
        binding.callStatusTxt.setText(callStateToHumanState(callStatus));
    }

    @Override
    public void initMenu(boolean isSpeakerOn, boolean displayFlip, boolean canDial,
                         boolean showPluginBtn, boolean onGoingCall) {
        if (binding != null) {
            binding.callCameraFlipBtn.setVisibility(displayFlip ? View.VISIBLE : View.GONE);
        }
        if (dialPadBtn != null) {
            dialPadBtn.setVisible(canDial);
        }

        if (pluginsMenuBtn != null) {
            pluginsMenuBtn.setVisible(showPluginBtn);
        }
        updateMenu();
    }

    @Override
    public void initNormalStateDisplay(final boolean audioOnly, boolean isMuted) {
        Log.w(TAG, "initNormalStateDisplay");
        binding.shapeRipple.stopRipple();

        binding.callAcceptBtn.setVisibility(View.GONE);
        binding.callRefuseBtn.setVisibility(View.GONE);
        binding.callControlGroup.setVisibility(View.VISIBLE);
        binding.callHangupBtn.setVisibility(View.VISIBLE);

        binding.contactBubbleLayout.setVisibility(audioOnly ? View.VISIBLE : View.GONE);
        binding.callMicBtn.setChecked(isMuted);

        requireActivity().invalidateOptionsMenu();
        CallActivity callActivity = (CallActivity) getActivity();
        if (callActivity != null) {
            callActivity.showSystemUI();
        }
    }

    @Override
    public void initIncomingCallDisplay() {
        Log.w(TAG, "initIncomingCallDisplay");

        binding.callAcceptBtn.setVisibility(View.VISIBLE);
        binding.callRefuseBtn.setVisibility(View.VISIBLE);
        binding.callControlGroup.setVisibility(View.GONE);
        binding.callHangupBtn.setVisibility(View.GONE);

        binding.contactBubbleLayout.setVisibility(View.VISIBLE);
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void initOutGoingCallDisplay() {
        Log.w(TAG, "initOutGoingCallDisplay");

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
    public void resetPluginPreviewVideoSize(int previewWidth, int previewHeight, int rot) {
        if (previewWidth == -1 && previewHeight == -1)
            return;
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        boolean flip = (rot % 180) != 0;
        binding.pluginPreviewSurface.setAspectRatio(flip ? mPreviewHeight : mPreviewWidth, flip ? mPreviewWidth : mPreviewHeight);
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
        if (null == binding || null == activity) {
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
        if (!choosePluginMode) {
//            binding.pluginPreviewSurface.setTransform(matrix);
//        }
//        else {
            binding.previewSurface.setTransform(matrix);
        }
    }

    @Override
    public void goToConversation(String accountId, Uri conversationId) {
        Context context = requireContext();
        if (DeviceUtils.isTablet(context)) {
            startActivity(new Intent(DRingService.ACTION_CONV_ACCEPT, ConversationPath.toUri(accountId, conversationId), context, HomeActivity.class));
        } else {
            startActivityForResult(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, conversationId), context, ConversationActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT), HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    @Override
    public void goToAddContact(Contact contact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(contact),
                ConversationFragment.REQ_ADD_CONTACT);
    }

    @Override
    public void goToContact(String accountId, Contact contact) {
        startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, contact.getUri()))
                .setClass(requireContext(), ContactDetailsActivity.class));
    }

    /**
     * Checks if permissions are accepted for camera and microphone. Takes into account whether call is incoming and outgoing, and requests permissions if not available.
     * Initializes the call if permissions are accepted.
     *
     * @param isIncoming true if call is incoming, false for outgoing
     * @see #initializeCall(boolean) initializeCall
     */
    @Override
    public void prepareCall(boolean isIncoming) {
        boolean audioGranted = mDeviceRuntimeService.hasAudioPermission();
        boolean audioOnly;
        int permissionType;

        if (isIncoming) {
            audioOnly = presenter.isAudioOnly();
            permissionType = REQUEST_PERMISSION_INCOMING;
        } else {
            Bundle args = getArguments();
            audioOnly = args != null && args.getBoolean(KEY_AUDIO_ONLY);
            permissionType = REQUEST_PERMISSION_OUTGOING;
        }
        if (!audioOnly) {
            boolean videoGranted = mDeviceRuntimeService.hasVideoPermission();

            if ((!audioGranted || !videoGranted) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ArrayList<String> perms = new ArrayList<>();
                if (!videoGranted) {
                    perms.add(Manifest.permission.CAMERA);
                }
                if (!audioGranted) {
                    perms.add(Manifest.permission.RECORD_AUDIO);
                }
                requestPermissions(perms.toArray(new String[perms.size()]), permissionType);
            } else if (audioGranted && videoGranted) {
                initializeCall(isIncoming);
            }
        } else {
            if (!audioGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, permissionType);
            } else if (audioGranted) {
                initializeCall(isIncoming);
            }
        }
    }

    /**
     * Starts a call. Takes into account whether call is incoming or outgoing.
     *
     * @param isIncoming true if call is incoming, false for outgoing
     */
    private void initializeCall(boolean isIncoming) {
        if (isIncoming) {
            presenter.acceptCall();
        } else {
            Bundle args;
            args = getArguments();
            if (args != null) {
                ConversationPath conversation = ConversationPath.fromBundle(args);
                presenter.initOutGoing(conversation.getAccountId(),
                        conversation.getConversationUri(),
                        args.getString(Intent.EXTRA_PHONE_NUMBER),
                        args.getBoolean(KEY_AUDIO_ONLY));
            }
        }
    }

    @Override
    public void finish() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finishAndRemoveTask();
            if (mBackstackLost) {
                startActivity(Intent.makeMainActivity(new ComponentName(activity, HomeActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
    }

    public void speakerClicked() {
        presenter.speakerClick(binding.callSpeakerBtn.isChecked());
    }

    private void startScreenShare(MediaProjection mediaProjection) {
        if (presenter.startScreenShare(mediaProjection)) {
            if(choosePluginMode) {
                binding.pluginPreviewSurface.setVisibility(View.GONE);
            } else {
                binding.previewSurface.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(requireContext(), "Can't start screen sharing", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopShareScreen() {
        binding.previewSurface.setVisibility(View.VISIBLE);
        presenter.stopScreenShare();
    }

    public void shareScreenClicked(boolean checked) {
        if (!checked) {
            stopShareScreen();
        } else {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_SHARE);
        }
    }

    public void micClicked() {
        presenter.muteMicrophoneToggled(binding.callMicBtn.isChecked());
        binding.callMicBtn.setImageResource(binding.callMicBtn.isChecked()? R.drawable.baseline_mic_off_24 : R.drawable.baseline_mic_24);
    }

    public void hangUpClicked() {
        presenter.hangupCall();
    }

    public void refuseClicked() {
        presenter.refuseCall();
    }

    public void acceptClicked() {
        prepareCall(true);
    }

    public void cameraFlip() {
        presenter.switchVideoInputClick();
    }

    public void addParticipant() {
        presenter.startAddParticipant();
    }

    @Override
    public void startAddParticipant(String conferenceId) {
        startActivityForResult(
                new Intent(Intent.ACTION_PICK)
                        .setClass(requireActivity(), ConversationSelectionActivity.class)
                        .putExtra(KEY_CONF_ID, conferenceId),
                CallFragment.REQUEST_CODE_ADD_PARTICIPANT);
    }

    @Override
    public void toggleCallMediaHandler(String id, String callId, boolean toggle) {
        JamiService.toggleCallMediaHandler(id, callId, toggle);
    }

    public Map<String, String> getCallMediaHandlerDetails(String id) {
        return JamiService.getCallMediaHandlerDetails(id).toNative();
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

    public boolean displayPluginsButton() {
        return JamiService.getPluginsEnabled() && JamiService.getCallMediaHandlers().size() > 0;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Reset the padding of the RecyclerPicker on each
        rp.setFirstLastElementsWidths(112, 112);
        binding.recyclerPicker.setVisibility(View.GONE);
        if (choosePluginMode) {
            displayHangupButton(false);
            binding.recyclerPicker.setVisibility(View.VISIBLE);
            movePreview(true);
            if (previousPluginPosition != -1) {
                rp.scrollToPosition(previousPluginPosition);
            }
        } else {
            movePreview(false);
        }
    }

    public void toggleVideoPluginsCarousel(boolean toggle) {
        if (choosePluginMode) {
            if (toggle) {
                binding.recyclerPicker.setVisibility(View.VISIBLE);
                movePreview(true);
            } else {
                binding.recyclerPicker.setVisibility(View.INVISIBLE);
                movePreview(false);
            }
        }
    }

    public void movePreview(boolean up) {
        // Move the preview container (cardview) by a certain margin
        if(up) {
            animation.setIntValues(12, 128);
        } else {
            animation.setIntValues(128, 12);
        }
        animation.start();
    }

    /**
     * Function that is called to show/hide the plugins recycler viewer and update UI
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    public void displayVideoPluginsCarousel() {
        choosePluginMode = !choosePluginMode;

        Context context = requireActivity();

        // Create callMediaHandlers and videoPluginsItems in a lazy manner
        if (pluginsModeFirst) {
            // Init
            callMediaHandlers = JamiService.getCallMediaHandlers();
            List<Drawable> videoPluginsItems = new ArrayList<>(callMediaHandlers.size() + 1);

            videoPluginsItems.add(context.getDrawable(R.drawable.baseline_cancel_24));
            // Search for plugin call media handlers icons
            // If a call media handler doesn't have an icon use a standard android icon
            for (String callMediaHandler : callMediaHandlers) {
                Map<String, String> details = getCallMediaHandlerDetails(callMediaHandler);
                String drawablePath = details.get("iconPath");
                if (drawablePath != null && drawablePath.endsWith("svg"))
                    drawablePath = drawablePath.replace(".svg", ".png");
                Drawable handlerIcon = Drawable.createFromPath(drawablePath);
                if (handlerIcon == null) {
                    handlerIcon = context.getDrawable(R.drawable.ic_jami);
                }
                videoPluginsItems.add(handlerIcon);
            }

            rp.updateData(videoPluginsItems);

            pluginsModeFirst = false;
        }

        if (choosePluginMode) {
            // hide hang up button and other call buttons
            displayHangupButton(false);
            // Display the plugins recyclerpicker
            binding.recyclerPicker.setVisibility(View.VISIBLE);
            movePreview(true);

            // Start loading the first or previous plugin if one was active
            if(callMediaHandlers.size() > 0) {
                // If no previous plugin was active, take the first, else previous
                int position;
                if (previousPluginPosition < 1) {
                    rp.scrollToPosition(1);
                    position = 1;
                    previousPluginPosition = 1;
                } else {
                    position = previousPluginPosition;
                }
                String callMediaId = callMediaHandlers.get(position-1);
                presenter.startPlugin(callMediaId);
            }

        } else {
            if (previousPluginPosition > 0) {
                String callMediaId = callMediaHandlers.
                        get(previousPluginPosition-1);

                presenter.toggleCallMediaHandler(callMediaId, false);
                rp.scrollToPosition(previousPluginPosition);
            }
            presenter.stopPlugin();
            binding.recyclerPicker.setVisibility(View.GONE);
            movePreview(false);
            displayHangupButton(true);
        }

        //change preview image
        displayVideoSurface(true,true);
    }

    /**
     * Called whenever a plugin drawable in the recycler picker is clicked or scrolled to
     */
    @Override
    public void onItemSelected(int position) {
        Log.i(TAG, "selected position: " + position);
        /* If there was a different plugin before, unload it
         * If previousPluginPosition = -1 or 0, there was no plugin
         */
        if (previousPluginPosition > 0) {
            String callMediaId = callMediaHandlers.get(previousPluginPosition-1);
            presenter.toggleCallMediaHandler(callMediaId, false);
        }

        if (position > 0) {
            previousPluginPosition = position;
            String callMediaId = callMediaHandlers.get(position-1);
            presenter.toggleCallMediaHandler(callMediaId, true);
        }
    }


    /**
     * Called whenever a plugin drawable in the recycler picker is clicked
     */
    @Override
    public void onItemClicked(int position) {
        Log.i(TAG, "selected position: " + position);
        if (position == 0) {
            /* If there was a different plugin before, unload it
             * If previousPluginPosition = -1 or 0, there was no plugin
             */
            if (previousPluginPosition > 0) {
                String callMediaId = callMediaHandlers.get(previousPluginPosition-1);
                presenter.toggleCallMediaHandler(callMediaId, false);
                rp.scrollToPosition(previousPluginPosition);
            }

            CallActivity callActivity = (CallActivity) getActivity();
            if (callActivity != null) {
                callActivity.showSystemUI();
            }

            toggleVideoPluginsCarousel(false);
            displayVideoPluginsCarousel();
        }
    }

    private Animation getBlinkingAnimation() {
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(400);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        return animation;
    }

}