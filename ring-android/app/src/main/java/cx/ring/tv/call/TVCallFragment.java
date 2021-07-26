/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.tv.call;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.percentlayout.widget.PercentFrameLayout;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Rational;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;

import com.rodolfonavalon.shaperipplelibrary.model.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import net.jami.call.CallPresenter;
import net.jami.call.CallView;
import cx.ring.client.ContactDetailsActivity;
import cx.ring.client.ConversationSelectionActivity;
import cx.ring.databinding.ItemParticipantLabelBinding;
import cx.ring.databinding.TvFragCallBinding;
import cx.ring.fragments.CallFragment;
import cx.ring.adapters.ConfParticipantAdapter;
import cx.ring.fragments.ConversationFragment;

import net.jami.daemon.JamiService;
import net.jami.model.Contact;
import net.jami.model.Conference;
import net.jami.model.Call;
import cx.ring.mvp.BaseSupportFragment;

import net.jami.model.Uri;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;
import cx.ring.tv.main.HomeActivity;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TVCallFragment extends BaseSupportFragment<CallPresenter, CallView> implements CallView {

    public static final String TAG = TVCallFragment.class.getSimpleName();

    public static final String KEY_ACTION = "action";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_AUDIO_ONLY = "AUDIO_ONLY";

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 6;
    private static final int REQUEST_PERMISSION_INCOMING = 1003;
    private static final int REQUEST_PERMISSION_OUTGOING = 1004;

    private TvFragCallBinding binding;

    // Screen wake lock for incoming call
    private Runnable runnable;
    private int mPreviewWidth = 720, mPreviewHeight = 1280;
    private int mPreviewWidthRot = 720, mPreviewHeightRot = 1280;
    private PowerManager.WakeLock mScreenWakeLock;

    private boolean mBackstackLost = false;
    private boolean mTextureAvailable = false;
    private ConfParticipantAdapter confAdapter = null;
    private boolean mConferenceMode = false;

    private int mVideoWidth = -1;
    private int mVideoHeight = -1;

    private final AlphaAnimation fadeOutAnimation = new AlphaAnimation(1, 0);

    private MediaSessionCompat mSession;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    public static TVCallFragment newInstance(@NonNull String action, @NonNull String accountId, @NonNull String conversationId, @NonNull String contactUri, boolean audioOnly) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putAll(ConversationPath.toBundle(accountId, conversationId));
        bundle.putString(Intent.EXTRA_PHONE_NUMBER, contactUri);
        bundle.putBoolean(KEY_AUDIO_ONLY, audioOnly);
        TVCallFragment fragment = new TVCallFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static TVCallFragment newInstance(@NonNull String action, @Nullable String confId) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_CONF_ID, confId);
        TVCallFragment countDownFragment = new TVCallFragment();
        countDownFragment.setArguments(bundle);
        return countDownFragment;
    }

    public TVCallFragment() {
        fadeOutAnimation.setInterpolator(new AccelerateInterpolator());
        fadeOutAnimation.setStartOffset(1000);
        fadeOutAnimation.setDuration(1000);
    }

    @Override
    protected void initPresenter(CallPresenter presenter) {
        super.initPresenter(presenter);
        String action = getArguments().getString(KEY_ACTION);
        if (action != null) {
            if (action.equals(Intent.ACTION_CALL)) {
                prepareCall(false);
            } else if (action.equals(Intent.ACTION_VIEW)) {
                presenter.initIncomingCall(getArguments().getString(KEY_CONF_ID), true);
            }
        }
    }

    @Override
    public void handleCallWakelock(boolean isAudioOnly) { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = TvFragCallBinding.inflate(inflater, container, false);
        binding.setPresenter(this);
        return binding.getRoot();
    }

    private final TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
            presenter.previewVideoSurfaceCreated(binding.previewSurface);
            mTextureAvailable = true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            presenter.previewVideoSurfaceDestroyed();
            mTextureAvailable = false;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSession = new MediaSessionCompat(requireContext(), TAG);
        mSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, getString(R.string.pip_title))
                .build());

        PowerManager powerManager = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "ring:callLock");
        mScreenWakeLock.setReferenceCounted(false);

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

        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                resetVideoSize(mVideoWidth, mVideoHeight));

        binding.previewSurface.setSurfaceTextureListener(listener);
        binding.shapeRipple.setRippleShape(new Circle());
        runnable = () -> presenter.uiVisibilityChanged(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mTextureAvailable)
            presenter.previewVideoSurfaceCreated(binding.previewSurface);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
        mScreenWakeLock = null;
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }
        presenter.hangupCall();
        runnable = null;
        binding = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
        View view = getView();
        Runnable r = runnable;
        if (view != null && r != null) {
            Handler handler = view.getHandler();
            if (handler != null)
                handler.removeCallbacks(r);
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
                    presenter.addConferenceParticipant(path.getAccountId(), path.getConversationUri());
                }
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if(!isInPictureInPictureMode) {
            mBackstackLost = true;
        }
        presenter.pipModeChanged(isInPictureInPictureMode);
    }

    @Override
    public void displayContactBubble(final boolean display) {
        binding.contactBubbleLayout.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayVideoSurface(final boolean displayVideoSurface, final boolean displayPreviewContainer) {
        binding.videoSurface.setVisibility(displayVideoSurface ? View.VISIBLE : View.GONE);
        binding.previewContainer.setVisibility(displayPreviewContainer ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayPreviewSurface(boolean display) {
        if (display) {
            binding.videoSurface.setZOrderOnTop(false);
            //mVideoPreview.setZOrderMediaOverlay(true);
            binding.videoSurface.setZOrderMediaOverlay(false);
        } else {
            binding.videoSurface.setZOrderMediaOverlay(true);
            binding.videoSurface.setZOrderOnTop(true);
        }
    }

    @Override
    public void displayHangupButton(boolean display) {
        binding.confControlGroup.setVisibility((mConferenceMode && display) ? View.VISIBLE : View.GONE);

        if (display) {
            binding.callHangupBtn.setVisibility(View.VISIBLE);
            binding.callAddBtn.setVisibility(View.VISIBLE);
        } else {
            binding.callHangupBtn.startAnimation(fadeOutAnimation);
            binding.callAddBtn.startAnimation(fadeOutAnimation);
            binding.callHangupBtn.setVisibility(View.GONE);
            binding.callAddBtn.setVisibility(View.GONE);
        }
        if (mConferenceMode && display) {
            binding.confControlGroup.setVisibility(View.VISIBLE);
        } else {
            binding.confControlGroup.startAnimation(fadeOutAnimation);
        }
    }

    @Override
    public void displayDialPadKeyboard() {
    }

    @Override
    public void switchCameraIcon(boolean isFront) {

    }

    @Override
    public void updateAudioState(HardwareService.AudioState state) {

    }

    @Override
    public void updateMenu() {

    }

    @Override
    public void updateTime(final long duration) {
        if (binding != null)
            binding.callStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
    }

    @Override
    public void updateContactBubble(@NonNull final List<Call> calls) {
        mConferenceMode = calls.size() > 1;
        String username = mConferenceMode ? "Conference with " + calls.size() + " people" : calls.get(0).getContact().getRingUsername();
        String displayName = mConferenceMode ? null : calls.get(0).getContact().getDisplayName();

        Contact contact = calls.get(0).getContact();
        String ringId = contact.getIds().get(0);
        Log.d(TAG, "updateContactBubble: username=" + username + ", ringId=" + ringId + " photo:" + contact.getPhoto());

        if (mSession != null) {
            mSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, displayName)
                    .build());
        }

        boolean hasProfileName = displayName != null && !displayName.contentEquals(username);
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
                        .withContact(contact)
                        .withCircleCrop(true)
                        .build(getActivity())
        );

        /*if (!mConferenceMode) {
            binding.confControlGroup.setVisibility(View.GONE);
        } else {
            binding.confControlGroup.setVisibility(View.VISIBLE);
            if (confAdapter  == null) {
                confAdapter = new ConfParticipantAdapter((view, call) -> {
                    Context context = requireContext();
                    PopupMenu popup = new PopupMenu(context, view);
                    popup.inflate(R.menu.conference_participant_actions);
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.conv_contact_details) {
                            presenter.openParticipantContact(call);
                        } else if (itemId == R.id.conv_contact_hangup) {
                            presenter.hangupParticipant(call);
                        } else {
                            return false;
                        }
                        return true;
                    });
                    MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popup.getMenu(), view);
                    menuHelper.setForceShowIcon(true);
                    menuHelper.show();
                });
            }
            confAdapter.updateFromCalls(calls);
            if (binding.confControlGroup.getAdapter() == null)
                binding.confControlGroup.setAdapter(confAdapter);
        }*/
    }

    @Override
    public void updateCallStatus(final Call.CallStatus callStatus) {
        switch (callStatus) {
            case NONE:
                binding.callStatusTxt.setText("");
                break;
            default:
                binding.callStatusTxt.setText(CallFragment.callStateToHumanState(callStatus));
                break;
        }
    }

    @Override
    public void initMenu(boolean isSpeakerOn, boolean displayFlip, boolean canDial,
                         boolean showPluginBtn, boolean onGoingCall) {

    }

    @Override
    public void initNormalStateDisplay(boolean audioOnly, boolean muted) {
        mSession.setActive(true);

        binding.shapeRipple.stopRipple();

        binding.callAcceptBtn.setVisibility(View.GONE);
        binding.callRefuseBtn.setVisibility(View.GONE);
        binding.callHangupBtn.setVisibility(View.VISIBLE);

        binding.contactBubbleLayout.setVisibility(audioOnly ? View.VISIBLE : View.INVISIBLE);

        getActivity().invalidateOptionsMenu();

        handleVisibilityTimer();
    }

    @Override
    public void initIncomingCallDisplay() {
        mSession.setActive(true);

        binding.callAcceptBtn.setVisibility(View.VISIBLE);
        binding.callAcceptBtn.requestFocus();
        binding.callRefuseBtn.setVisibility(View.VISIBLE);
        binding.callHangupBtn.setVisibility(View.GONE);
    }

    @Override
    public void initOutGoingCallDisplay() {
        binding.callAcceptBtn.setVisibility(View.GONE);
        binding.callRefuseBtn.setVisibility(View.VISIBLE);
        binding.callHangupBtn.setVisibility(View.GONE);
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
    }

    @Override
    public void resetVideoSize(final int videoWidth, final int videoHeight) {
        Log.w(TAG, "resetVideoSize " + videoWidth + "x" + videoHeight);
        ViewGroup rootView = (ViewGroup) getView();
        if (rootView == null)
            return;

        double videoRatio = videoWidth / (double) videoHeight;
        double screenRatio = getView().getWidth() / (double) getView().getHeight();

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
        Log.w(TAG, "configureTransform " + viewWidth + "x" + viewHeight + " rot=" + rot + " mPreviewWidth=" + mPreviewWidth + " mPreviewHeight=" + mPreviewHeight);
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (rot) {
            RectF bufferRect = new RectF(0, 0, mPreviewHeightRot, mPreviewWidthRot);
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewHeightRot,
                    (float) viewWidth / mPreviewWidthRot);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        binding.previewSurface.setTransform(matrix);
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
        Bundle args = getArguments();
        boolean audioGranted = mDeviceRuntimeService.hasAudioPermission();
        boolean audioOnly;
        int permissionType;

        if (isIncoming) {
            audioOnly = presenter.isAudioOnly();
            permissionType = REQUEST_PERMISSION_INCOMING;

        } else {
            audioOnly = args.getBoolean(KEY_AUDIO_ONLY);
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
    public void initializeCall(boolean isIncoming) {
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
    public void goToContact(String accountId, Contact contact) {
        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.withAppendedPath(android.net.Uri.withAppendedPath(ContentUriHandler.CONTACT_CONTENT_URI, accountId), contact.getPrimaryNumber()))
                .setClass(requireContext(), ContactDetailsActivity.class));
    }

    @Override
    public boolean displayPluginsButton() {
        return false;
    }

    @Override
    public void updateConfInfo(List<Conference.ParticipantInfo> info) {
        binding.participantLabelContainer.removeAllViews();
        if (!info.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(binding.participantLabelContainer.getContext());
            for (Conference.ParticipantInfo i : info) {
                String displayName = i.contact.getDisplayName();
                if (!TextUtils.isEmpty(displayName)) {
                    ItemParticipantLabelBinding label = ItemParticipantLabelBinding.inflate(inflater);
                    PercentFrameLayout.LayoutParams params = new PercentFrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.getPercentLayoutInfo().leftMarginPercent = i.x / (float) mVideoWidth;
                    params.getPercentLayoutInfo().topMarginPercent = i.y / (float) mVideoHeight;
                    label.participantName.setText(displayName);
                    binding.participantLabelContainer.addView(label.getRoot(), params);
                }
            }
        }
        binding.participantLabelContainer.setVisibility(info.isEmpty() ? View.GONE : View.VISIBLE);

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
                        int itemId = item.getItemId();
                        if (itemId == R.id.conv_contact_details) {
                            presenter.openParticipantContact(call);
                        } else if (itemId == R.id.conv_contact_hangup) {
                            presenter.hangupParticipant(call);
                        } else {
                            return false;
                        }
                        return true;
                    });
                    MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popup.getMenu(), view);
                    menuHelper.setForceShowIcon(true);
                    menuHelper.show();
                });
            }
            confAdapter.updateFromCalls(info);
            if (binding.confControlGroup.getAdapter() == null)
                binding.confControlGroup.setAdapter(confAdapter);
        }
    }

    @Override
    public void updateParticipantRecording(Set<Contact> contacts) {

    }

    @Override
    public void toggleCallMediaHandler(String id, String callId, boolean toggle) {
        JamiService.toggleCallMediaHandler(id, callId, toggle);
    }

    @Override
    public void goToConversation(String accountId, Uri conversationId) {

    }

    @Override
    public void goToAddContact(Contact contact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(contact),
                ConversationFragment.REQ_ADD_CONTACT);
    }

    @Override
    public void startAddParticipant(String conferenceId) {
        startActivityForResult(
                new Intent(Intent.ACTION_PICK)
                        .setClass(requireActivity(), ConversationSelectionActivity.class)
                        .putExtra(KEY_CONF_ID, conferenceId),
                REQUEST_CODE_ADD_PARTICIPANT);
    }

    public void addParticipant() {
        presenter.startAddParticipant();
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

    @Override
    public void finish() {
        Activity activity = getActivity();
        if (mSession != null) {
            mSession.setActive(false);
        }
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

    @Override
    public void onUserLeave() {
        presenter.requestPipMode();
    }

    @Override
    public void enterPipMode(String callId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
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
            requireActivity().enterPictureInPictureMode(paramBuilder.build());
        } else {
            requireActivity().enterPictureInPictureMode();
        }
    }

    public void onKeyDown() {
        handleVisibilityTimer();
    }

    private void handleVisibilityTimer() {
        presenter.uiVisibilityChanged(true);
        View view = getView();
        Runnable r = runnable;
        if (view != null && r != null) {
            Handler handler = view.getHandler();
            if (handler != null) {
                handler.removeCallbacks(r);
                handler.postDelayed(r, 5000);
            }
        }
    }

}