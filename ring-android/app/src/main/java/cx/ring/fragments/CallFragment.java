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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.ComponentName;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.rodolfonavalon.shaperipplelibrary.model.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.call.CallPresenter;
import cx.ring.call.CallView;
import cx.ring.client.CallActivity;
import cx.ring.client.ContactDetailsActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.ConversationSelectionActivity;
import cx.ring.client.HomeActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.databinding.FragCallBinding;
import cx.ring.databinding.ItemConferenceParticipantBinding;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.SipCall;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.service.DRingService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.NotificationService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.utils.Log;
import cx.ring.utils.MediaButtonsHelper;
import cx.ring.views.AvatarDrawable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class CallFragment extends BaseSupportFragment<CallPresenter> implements CallView, MediaButtonsHelper.MediaButtonsHelperCallback {

    public static final String TAG = CallFragment.class.getSimpleName();

    public static final String ACTION_PLACE_CALL = "PLACE_CALL";
    public static final String ACTION_GET_CALL = "GET_CALL";

    public static final String KEY_ACTION = "action";
    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_AUDIO_ONLY = "AUDIO_ONLY";

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 6;
    private static final int REQUEST_PERMISSION_INCOMING = 1003;
    private static final int REQUEST_PERMISSION_OUTGOING = 1004;

    private FragCallBinding binding;
    private OrientationEventListener mOrientationListener;

    private MenuItem dialPadBtn = null;
    private boolean restartVideo = false;
    private boolean restartPreview = false;
    private PowerManager.WakeLock mScreenWakeLock = null;
    private int mCurrentOrientation = 0;

    private int mVideoWidth = -1;
    private int mVideoHeight = -1;
    private int mPreviewWidth = 720, mPreviewHeight = 1280;
    private int mPreviewSurfaceWidth = 0, mPreviewSurfaceHeight = 0;

    private boolean mBackstackLost = false;

    private ConfParticipantAdapter confAdapter = null;
    private boolean mConferenceMode = false;

    static class ParticipantView extends RecyclerView.ViewHolder {
        final ItemConferenceParticipantBinding binding;
        Disposable disposable = null;
        ParticipantView(@NonNull ItemConferenceParticipantBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }

    public static class ConfParticipantAdapter extends RecyclerView.Adapter<ParticipantView> {
        private List<SipCall> calls = null;
        public interface ConfParticipantSelected {
            void onParticipantSelected(View view, SipCall contact);
        }
        private final ConfParticipantSelected onSelectedCallback;

        ConfParticipantAdapter(@NonNull ConfParticipantSelected cb) {
            onSelectedCallback = cb;
        }

        @NonNull
        @Override
        public ParticipantView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ParticipantView(ItemConferenceParticipantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ParticipantView holder, int position) {
            final SipCall call = calls.get(position);
            final CallContact contact = call.getContact();
            final Context context = holder.itemView.getContext();
            SipCall.CallStatus status = call.getCallStatus();
            if (status == SipCall.CallStatus.CURRENT)  {
                holder.binding.displayName.setText(contact.getDisplayName());
                holder.binding.photo.setAlpha(1f);
            } else {
                holder.binding.displayName.setText(String.format("%s\n%s", contact.getDisplayName(), context.getText(callStateToHumanState(status))));
                holder.binding.photo.setAlpha(.5f);
            }
            if (holder.disposable != null)
                holder.disposable.dispose();
            holder.disposable = AvatarFactory.getAvatar(context, contact)
                    .subscribe(holder.binding.photo::setImageDrawable);
            holder.itemView.setOnClickListener(view -> onSelectedCallback.onParticipantSelected(view, call));
        }

        @Override
        public int getItemCount() {
            return calls == null ? 0 : calls.size();
        }

        void updateFromCalls(@NonNull final List<SipCall> contacts) {
            final List<SipCall> oldCalls = calls;
            calls = contacts;
            if (oldCalls != null) {
                DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return oldCalls.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return contacts.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return oldCalls.get(oldItemPosition) == contacts.get(newItemPosition);
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return false;
                    }
                }).dispatchUpdatesTo(this);
            } else {
                notifyDataSetChanged();
            }
        }
    }

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

    public static int callStateToHumanState(final SipCall.CallStatus state) {
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
        super.initPresenter(presenter);
        Bundle args = getArguments();
        if (args != null) {
            String action = args.getString(KEY_ACTION);
            if (action != null) {
                if (action.equals(ACTION_PLACE_CALL)) {
                    prepareCall(false);
                }
                else if (action.equals(ACTION_GET_CALL) || action.equals(CallActivity.ACTION_CALL_ACCEPT)) {
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
            }
            ArrayList<RemoteAction> actions = new ArrayList<>(1);
            actions.add(new RemoteAction(
                    Icon.createWithResource(context, R.drawable.baseline_call_end_24),
                    getString(R.string.action_call_hangup),
                    getString(R.string.action_call_hangup),
                    PendingIntent.getService(context, new Random().nextInt(),
                            new Intent(DRingService.ACTION_CALL_END)
                                    .setClass(context, DRingService.class)
                                    .putExtra(NotificationService.KEY_CALL_ID, callId), PendingIntent.FLAG_ONE_SHOT)));
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
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
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
        if (binding.videoSurface.getVisibility() == View.VISIBLE) {
            restartVideo = true;
        }
        if (binding.previewContainer.getVisibility() == View.VISIBLE) {
            restartPreview = true;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        injectFragment(((JamiApplication) requireActivity().getApplication()).getRingInjectionComponent());
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
        view.setOnSystemUiVisibilityChangeListener(visibility -> {
            boolean ui = (visibility & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0;
            presenter.uiVisibilityChanged(ui);
        });
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

    /**
     * Releases current wakelock and acquires a new proximity wakelock if current call is audio only.
     * @param isAudioOnly true if it is an audio call
     */
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
                    presenter.addConferenceParticipant(path.getAccountId(), path.getContactId());
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu m, @NonNull MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.ac_call, m);
        dialPadBtn = m.findItem(R.id.menuitem_dialpad);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        presenter.prepareOptionMenu();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
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
        binding.contactBubbleLayout.getHandler().post(() -> binding.contactBubbleLayout.setVisibility(display ? View.VISIBLE : View.GONE));
    }

    @Override
    public void displayVideoSurface(final boolean displayVideoSurface, final boolean displayPreviewContainer) {
        binding.videoSurface.setVisibility(displayVideoSurface ? View.VISIBLE : View.GONE);
        binding.previewContainer.setVisibility(displayPreviewContainer ? View.VISIBLE : View.GONE);
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
        binding.callControlGroup.setVisibility(display ? View.VISIBLE : View.GONE);
        binding.callHangupBtn.setVisibility(display ? View.VISIBLE : View.GONE);
        binding.confControlGroup.setVisibility((mConferenceMode && display) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayDialPadKeyboard() {
        KeyboardVisibilityManager.showKeyboard(getActivity(), binding.dialpadEditText, InputMethodManager.SHOW_FORCED);
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
        if (binding.callStatusTxt != null) {
            if (duration == 0)
                binding.callStatusTxt.setText(null);
            else
                binding.callStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
        }
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void updateContactBubble(@NonNull final List<SipCall> contacts) {
        Log.w(TAG, "updateContactBubble " + contacts.size());

        mConferenceMode = contacts.size() > 1;
        String username = mConferenceMode ? "Conference with " + contacts.size() + " people" : contacts.get(0).getContact().getRingUsername();
        String displayName = mConferenceMode ? null : contacts.get(0).getContact().getDisplayName();

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

        binding.contactBubble.setImageDrawable(new AvatarDrawable(getActivity(), contacts.get(0).getContact()));

        if (!mConferenceMode) {
            binding.confControlGroup.setVisibility(View.GONE);
        } else {
            binding.confControlGroup.setVisibility(View.VISIBLE);
            if (confAdapter  == null) {
                confAdapter = new ConfParticipantAdapter((view, call) -> {
                    Context context = requireContext();
                    PopupMenu popup = new PopupMenu(context, view);
                    popup.inflate(R.menu.conference_participant_actions);
                    popup.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.conv_contact_details:
                                presenter.openParticipantContact(call);
                                break;
                            case R.id.conv_contact_hangup:
                                presenter.hangupParticipant(call);
                                break;
                            default:
                                return false;
                        }
                        return true;
                    });
                    MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popup.getMenu(), view);
                    menuHelper.setForceShowIcon(true);
                    menuHelper.show();
                });
            }
            confAdapter.updateFromCalls(contacts);
            if (binding.confControlGroup.getAdapter() == null)
                binding.confControlGroup.setAdapter(confAdapter);
        }
    }

    @Override
    public void updateCallStatus(final SipCall.CallStatus callStatus) {
        binding.callStatusTxt.setText(callStateToHumanState(callStatus));
    }

    @Override
    public void initMenu(boolean isSpeakerOn, boolean displayFlip, boolean canDial, boolean onGoingCall) {
        if (binding.callCameraFlipBtn != null) {
            binding.callCameraFlipBtn.setVisibility(displayFlip ? View.VISIBLE : View.GONE);
        }
        if (dialPadBtn != null) {
            dialPadBtn.setVisible(canDial);
        }
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
        Context context = requireContext();
        if (DeviceUtils.isTablet(context)) {
            startActivity(new Intent(context, HomeActivity.class)
                    .setAction(DRingService.ACTION_CONV_ACCEPT)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, conversationId));
        } else {
            startActivityForResult(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, conversationId), context, ConversationActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT), HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    @Override
    public void goToAddContact(CallContact callContact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(callContact),
                ConversationFragment.REQ_ADD_CONTACT);
    }

    @Override
    public void goToContact(String accountId, CallContact contact) {
        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.withAppendedPath(android.net.Uri.withAppendedPath(ContentUriHandler.CONTACT_CONTENT_URI, accountId), contact.getPrimaryNumber()))
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

            if ((!audioGranted || !videoGranted) && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            if (!audioGranted && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                presenter.initOutGoing(getArguments().getString(KEY_ACCOUNT_ID),
                        getArguments().getString(ConversationFragment.KEY_CONTACT_RING_ID),
                        getArguments().getBoolean(KEY_AUDIO_ONLY));
            }
        }
    }

    @Override
    public void finish() {
        Activity activity = getActivity();
        if (activity != null) {
            if (mBackstackLost) {
                activity.finishAndRemoveTask();
                startActivity(
                        Intent.makeMainActivity(
                                new ComponentName(activity, HomeActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                activity.finish();
            }
        }
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