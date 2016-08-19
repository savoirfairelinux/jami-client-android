/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.skyfishjy.library.RippleBackground;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.interfaces.CallInterface;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;
import cx.ring.service.CallManagerCallBack;
import cx.ring.service.DRingService;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.utils.KeyboardVisibilityManager;

public class CallFragment extends Fragment implements CallInterface {

    static final private String TAG = CallFragment.class.getSimpleName();

    public static final int REQUEST_TRANSFER = 10;

    //~ Regular expression to match DTMF supported characters : 0 to 9, A, B, C, D, * and #
    public static final String DTMF_SUPPORTED_CHARS_REGEX = "^[a-dA-D0-9#*]*$";

    // Screen wake lock for incoming call
    private WakeLock mScreenWakeLock;

    @BindView(R.id.contact_bubble_layout)
    View contactBubbleLayout;

    @BindView(R.id.contact_bubble)
    ImageView contactBubbleView;

    @BindView(R.id.contact_bubble_txt)
    TextView contactBubbleTxt;

    @BindView(R.id.contact_bubble_num_txt)
    TextView contactBubbleNumTxt;

    @BindView(R.id.call_accept_btn)
    View acceptButton;

    @BindView(R.id.call_refuse_btn)
    View refuseButton;

    @BindView(R.id.call_hangup_btn)
    View hangupButton;

    @BindView(R.id.call_status_txt)
    TextView mCallStatusTxt;

    @BindView(R.id.security_indicator)
    View securityIndicator;

    @BindView(R.id.security_switcher)
    ViewSwitcher mSecuritySwitch;

    @BindView(R.id.dialpad_edit_text)
    EditText mNumeralDialEditText;

    @BindView(R.id.ripple_animation)
    RippleBackground mPulseAnimation;

    @BindView(R.id.video_preview_surface)
    SurfaceView mVideoSurface = null;

    private MenuItem speakerPhoneBtn = null;
    private MenuItem addContactBtn = null;
    private MenuItem flipCameraBtn = null;
    private MenuItem dialPadBtn = null;

    @BindView(R.id.camera_preview_surface)
    SurfaceView videoPreview = null;

    public ConversationCallbacks mCallbacks = sDummyCallbacks;

    private AudioManager audioManager;
    private boolean haveVideo = false;
    private int videoWidth = -1, videoHeight = -1;
    private int previewWidth = -1, previewHeight = -1;

    private boolean lastVideoSource = true;
    private Conference mCachedConference = null;

    private boolean ongoingCall = false;

    private DisplayManager.DisplayListener displayListener;

    @Override
    public void onAttach(Activity activity) {
        Log.i(TAG, "onAttach");
        super.onAttach(activity);

        if (!(activity instanceof ConversationCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (ConversationCallbacks) activity;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallManagerCallBack.RECORD_STATE_CHANGED);
        intentFilter.addAction(CallManagerCallBack.RTCP_REPORT_RECEIVED);

        intentFilter.addAction(DRingService.VIDEO_EVENT);

        intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);

        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach");
        getActivity().unregisterReceiver(mReceiver);
        mCallbacks = sDummyCallbacks;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedBundle) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedBundle);

        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        setHasOptionsMenu(true);
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
                            try {
                                mCallbacks.getRemoteService().switchInput(getConference().getId(), lastVideoSource);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            };
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
    }

    /**
     * The Activity calling this fragment has to implement this interface
     */
    public interface ConversationCallbacks extends LocalService.Callbacks {
        void startTimer();

        void terminateCall();

        Conference getDisplayedConference();

        void updateDisplayedConference(Conference c);

        ActionBar getSupportActionBar();
    }

    private static final ConversationCallbacks sDummyCallbacks = new ConversationCallbacks() {
        @Override
        public void startTimer() {
            //Dummy implementation
        }

        @Override
        public void terminateCall() {
            //Dummy implementation
        }

        @Override
        public Conference getDisplayedConference() {
            //Dummy implementation
            return null;
        }

        @Override
        public void updateDisplayedConference(Conference c) {
            //Dummy implementation
        }

        @Override
        public ActionBar getSupportActionBar() {
            //Dummy implementation
            return null;
        }

        @Override
        public IDRingService getRemoteService() {
            //Dummy implementation
            return null;
        }

        @Override
        public LocalService getService() {
            //Dummy implementation
            return null;
        }
    };

    public class CallReceiver extends BroadcastReceiver {
        private final String TAG = CallReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.contentEquals(LocalService.ACTION_CONF_UPDATE)) {
                confUpdate();
            } else if (action.contentEquals(DRingService.VIDEO_EVENT)) {
                if (mVideoSurface == null)
                    return;
                Conference conf = getConference();
                if (intent.hasExtra("start")) {
                    mVideoSurface.setVisibility(View.VISIBLE);
                    videoPreview.setVisibility(View.VISIBLE);
                } else if (intent.hasExtra("camera")) {
                    previewWidth = intent.getIntExtra("width", 0);
                    previewHeight = intent.getIntExtra("height", 0);
                } else if (conf != null && conf.getId().equals(intent.getStringExtra("call"))) {
                    if (mVideoSurface != null) {
                        haveVideo = intent.getBooleanExtra("started", false);
                        if (haveVideo) {
                            mVideoSurface.setVisibility(View.VISIBLE);
                            videoPreview.setVisibility(View.VISIBLE);
                            videoWidth = intent.getIntExtra("width", 0);
                            videoHeight = intent.getIntExtra("height", 0);
                        } else {
                            mVideoSurface.setVisibility(View.GONE);
                            videoPreview.setVisibility(View.GONE);
                        }
                    }
                    refreshState();
                }
                resetVideoSizes();
            } else if (action.contentEquals(CallManagerCallBack.RECORD_STATE_CHANGED)) {
                recordingChanged((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("call"), intent.getStringExtra("file"));
            } else if (action.contentEquals(CallManagerCallBack.RTCP_REPORT_RECEIVED)) {
                rtcpReportReceived(null, null); // FIXME
            } else {
                Log.e(TAG, "Unknown action: " + intent.getAction());
            }
        }
    }

    private final CallReceiver mReceiver = new CallReceiver();

    public void refreshState() {
        Conference conf = getConference();

        if (conf == null) {
            contactBubbleView.setImageBitmap(null);
            contactBubbleTxt.setText("");
            contactBubbleNumTxt.setText("");
            acceptButton.setVisibility(View.GONE);
            refuseButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.GONE);
        } else if (conf.getParticipants().size() == 1) {
            SipCall call = conf.getParticipants().get(0);
            if (call.isIncoming() && call.isRinging()) {
                Log.w(TAG, "CallFragment refreshState INCOMING " + call.getCallId());
                initIncomingCallDisplay();
            } else if (conf.getParticipants().get(0).isRinging()) {
                Log.w(TAG, "CallFragment refreshState RINGING " + call.getCallId());
                initOutGoingCallDisplay();
            } else if (call.isOngoing()) {
                initNormalStateDisplay();
            }
        } else if (conf.getParticipants().size() > 1) {
            initNormalStateDisplay();
        }

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.ac_call, m);
        speakerPhoneBtn = m.findItem(R.id.menuitem_speaker);
        addContactBtn = m.findItem(R.id.menuitem_addcontact);
        flipCameraBtn = m.findItem(R.id.menuitem_camera_flip);
        dialPadBtn = m.findItem(R.id.menuitem_dialpad);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (speakerPhoneBtn != null) {
            boolean speakerPhone = audioManager.isSpeakerphoneOn();
            if (speakerPhoneBtn.getIcon() != null)
                speakerPhoneBtn.getIcon().setAlpha(speakerPhone ? 255 : 128);
            speakerPhoneBtn.setChecked(speakerPhone);
        }
        if (addContactBtn != null) {
            SipCall call = (getConference() != null && !getConference().getParticipants().isEmpty()) ? getFirstParticipant() : null;
            addContactBtn.setVisible(call != null && null != call.getContact() && call.getContact().isUnknown());
        }

        flipCameraBtn.setVisible(haveVideo);

        if (dialPadBtn != null) {
            dialPadBtn.setVisible(ongoingCall && getConference() != null && !getConference().isIncoming());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                mCallbacks.terminateCall();
                break;
            case R.id.menuitem_chat:
                Intent intent = new Intent()
                        .setClass(getActivity(), ConversationActivity.class)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, getFirstParticipant().getContact().getIds().get(0)));
                intent.putExtra("resuming", true);
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
                break;
            case R.id.menuitem_addcontact:
                startActivityForResult(getFirstParticipant().getContact().getAddNumberIntent(), ConversationActivity.REQ_ADD_CONTACT);
                break;
            case R.id.menuitem_speaker:
                audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
                getActivity().invalidateOptionsMenu();
                break;
            case R.id.menuitem_camera_flip:
                lastVideoSource = !lastVideoSource;
                try {
                    mCallbacks.getRemoteService().switchInput(getConference().getId(), lastVideoSource);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                item.setIcon(lastVideoSource ? R.drawable.ic_camera_front_white_24dp : R.drawable.ic_camera_rear_white_24dp);
                break;
            case R.id.menuitem_dialpad:
                KeyboardVisibilityManager.showKeyboard(getActivity(),
                        mNumeralDialEditText,
                        InputMethodManager.SHOW_IMPLICIT);
                break;
        }
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();

        Conference c = getConference();
        Log.w(TAG, "onStop() haveVideo=" + haveVideo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager displayManager = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
            displayManager.unregisterDisplayListener(displayListener);
        }

        DRingService.videoSurfaces.remove(c.getId());
        DRingService.mCameraPreviewSurface.clear();
        try {
            IDRingService service = mCallbacks.getRemoteService();
            if (service != null) {
                service.videoSurfaceRemoved(c.getId());
                service.videoPreviewSurfaceRemoved();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager displayManager = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
            displayManager.registerDisplayListener(displayListener, null);
        }

        Conference c = getConference();
        if (c != null && mVideoSurface != null && c.resumeVideo) {
            Log.i(TAG, "Resuming video");
            haveVideo = true;
            mVideoSurface.setVisibility(View.VISIBLE);
            videoPreview.setVisibility(View.VISIBLE);
            c.resumeVideo = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        Conference c = getConference();

        this.confUpdate();

        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        if (c != null) {
            c.mVisible = true;
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
            notificationManager.cancel(c.notificationId);
            if (c.resumeVideo) {
                Log.w(TAG, "Resuming video");
                haveVideo = true;
                mVideoSurface.setVisibility(View.VISIBLE);
                videoPreview.setVisibility(View.VISIBLE);
                c.resumeVideo = false;
            }
        }

        refreshState();
    }

    @Override
    public void onPause() {
        Log.w(TAG, "onPause() haveVideo=" + haveVideo);
        super.onPause();

        Conference c = getConference();
        if (c != null) {
            c.mVisible = false;
            c.resumeVideo = haveVideo;
            c.showCallNotification(getActivity());
        }
    }

    public void confUpdate() {
        Log.w(TAG, "confUpdate()");

        LocalService service = mCallbacks.getService();
        if (service == null)
            return;

        Conference c = service.getConference(getConference().getId());
        mCallbacks.updateDisplayedConference(c);
        if (c == null || c.getParticipants().isEmpty()) {
            mCallbacks.terminateCall();
            return;
        }

        int newState = c.getHumanState();
        String newStateString = (newState == R.string.call_human_state_none ||
                newState == R.string.conference_human_state_default)
                ? "" :
                getString(newState);
        if (c.isOnGoing()) {
            ongoingCall = true;
            initNormalStateDisplay();
        } else if (c.isRinging()) {
            ongoingCall = false;
            mCallStatusTxt.setText(newStateString);

            if (c.isIncoming()) {
                initIncomingCallDisplay();
            } else
                initOutGoingCallDisplay();
        } else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
            notificationManager.cancel(c.notificationId);
            mCallStatusTxt.setText(newStateString);
            mCallbacks.terminateCall();
        }
    }

    @Override
    public void recordingChanged(Conference c, String callID, String filename) {

    }

    @Override
    public void rtcpReportReceived(Conference c, HashMap<String, Integer> stats) {
        // No implementation yet
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SipCall transfer;
        if (requestCode == REQUEST_TRANSFER) {
            switch (resultCode) {
                case TransferDFragment.RESULT_TRANSFER_CONF:
                    Conference c = data.getParcelableExtra("target");
                    transfer = data.getParcelableExtra("transfer");
                    try {
                        mCallbacks.getRemoteService().attendedTransfer(transfer.getCallId(), c.getParticipants().get(0).getCallId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;

                case TransferDFragment.RESULT_TRANSFER_NUMBER:
                    String to = data.getStringExtra("to_number");
                    transfer = data.getParcelableExtra("transfer");
                    try {
                        mCallbacks.getRemoteService().transfer(transfer.getCallId(), to);
                        mCallbacks.getRemoteService().hangUp(transfer.getCallId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case Activity.RESULT_CANCELED:
                default:
                    confUpdate();
                    break;
            }
        }
    }

    void resetVideoSizes() {
        ViewGroup rootView = (ViewGroup) getView();
        if (rootView == null)
            return;

        double videoRatio = videoWidth / (double) videoHeight;
        double screenRatio = getView().getWidth() / (double) getView().getHeight();

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mVideoSurface.getLayoutParams();
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
            Log.w(TAG, "onLayoutChange " + params.width + " x " + params.height);
            mVideoSurface.setLayoutParams(params);
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        FrameLayout.LayoutParams paramsPreview = (FrameLayout.LayoutParams) videoPreview.getLayoutParams();
        oldW = paramsPreview.width;
        oldH = paramsPreview.height;
        double previewMaxDim = Math.max(previewWidth, previewHeight);
        double previewRatio = metrics.density * 160. / previewMaxDim;
        paramsPreview.width = (int) (previewWidth * previewRatio);
        paramsPreview.height = (int) (previewHeight * previewRatio);
        if (oldW != paramsPreview.width || oldH != paramsPreview.height) {
            Log.i(TAG, "onLayoutChange " + paramsPreview.width + " x " + paramsPreview.height);
            videoPreview.setLayoutParams(paramsPreview);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (videoPreview.getVisibility() == View.VISIBLE) {
            try {
                mCallbacks.getRemoteService().setPreviewSettings();
                mCallbacks.getRemoteService().videoPreviewSurfaceAdded();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.frag_call, container, false);

        ButterKnife.bind(this, rootView);

        mNumeralDialEditText.requestFocus();
        mNumeralDialEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //~ Empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String editTextString = s.toString();
                String lastChar = editTextString.substring(start, start + count);
                if (lastChar.matches(DTMF_SUPPORTED_CHARS_REGEX)) {
                    try {
                        Log.d(TAG, "Sending DTMF: " + lastChar.toUpperCase());
                        mCallbacks.getRemoteService().playDtmf(lastChar.toUpperCase());
                    } catch (RemoteException exc) {
                        exc.printStackTrace();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //~ Empty
            }
        });


        mVideoSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
        mVideoSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                contactBubbleLayout.setVisibility(View.GONE);
                Conference c = getConference();
                DRingService.videoSurfaces.put(c.getId(), new WeakReference<>(holder));
                try {
                    mCallbacks.getRemoteService().videoSurfaceAdded(c.getId());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "video surfaceChanged " + format + ", " + width + " x " + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Conference c = getConference();
                DRingService.videoSurfaces.remove(c.getId());
                try {
                    IDRingService service = mCallbacks.getRemoteService();
                    if (service != null)
                        service.videoSurfaceRemoved(c.getId());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        rootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View parent, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                resetVideoSizes();
            }
        });
        rootView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                boolean ui = (visibility & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0;
                if (ongoingCall) {
                    hangupButton.setVisibility(ui ? View.VISIBLE : View.GONE);
                }
            }
        });

        videoPreview.getHolder().setFormat(PixelFormat.RGBA_8888);
        videoPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                DRingService.mCameraPreviewSurface = new WeakReference<>(holder);
                try {
                    mCallbacks.getRemoteService().videoPreviewSurfaceAdded();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "videoPreview surfaceChanged " + format + ", " + width + " x " + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (videoPreview != null && DRingService.mCameraPreviewSurface.get() == holder) {
                    DRingService.mCameraPreviewSurface.clear();
                }
                try {
                    IDRingService service = mCallbacks.getRemoteService();
                    if (service != null)
                        service.videoPreviewSurfaceRemoved();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        videoPreview.setZOrderMediaOverlay(true);

        return rootView;
    }

    public Conference getConference() {
        Conference c = mCallbacks.getDisplayedConference();
        if (c != null) {
            if (mCachedConference != c)
                mCachedConference = c;
            return c;
        }
        return mCachedConference;
    }

    private void initContactDisplay(final SipCall call) {

        CallContact contact = call.getContact();
        final String name = contact.getDisplayName();
        contactBubbleTxt.setText(name);
        if (call.getNumber().contentEquals(name)) {
            contactBubbleNumTxt.setVisibility(View.GONE);
        } else {
            contactBubbleNumTxt.setVisibility(View.VISIBLE);
            contactBubbleNumTxt.setText(call.getNumber());
        }

        mPulseAnimation.startRippleAnimation();

        new ContactPictureTask(getActivity(), contactBubbleView, contact).run();

        ActionBar ab = mCallbacks.getSupportActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        ab.setTitle(name);
    }

    private void initNormalStateDisplay() {
        Log.i(TAG, "Start normal display");
        mCallbacks.startTimer();
        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.GONE);
        hangupButton.setVisibility(View.VISIBLE);

        final SipCall call = getFirstParticipant();
        initContactDisplay(call);

        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        contactBubbleLayout.setVisibility(haveVideo ? View.GONE : View.VISIBLE);
        updateSecurityDisplay();
    }

    private void updateSecurityDisplay() {
        //First we check if all participants use a security layer.
        boolean secure_call = !getConference().getParticipants().isEmpty();
        for (SipCall c : getConference().getParticipants())
            secure_call &= c instanceof SecureSipCall && ((SecureSipCall) c).isSecure();

        securityIndicator.setVisibility(secure_call ? View.VISIBLE : View.GONE);
        if (!secure_call)
            return;

        Log.i(TAG, "Enable security display");
        if (getConference().hasMultipleParticipants()) {
            //TODO What layout should we put?
        } else {
            final SecureSipCall secured = (SecureSipCall) getFirstParticipant();
            switch (secured.displayModule()) {
                case SecureSipCall.DISPLAY_GREEN_LOCK:
                    Log.i(TAG, "DISPLAY_GREEN_LOCK");
                    showLock(R.drawable.green_lock);
                    break;
                case SecureSipCall.DISPLAY_RED_LOCK:
                    Log.i(TAG, "DISPLAY_RED_LOCK");
                    showLock(R.drawable.red_lock);
                    break;
                case SecureSipCall.DISPLAY_NONE:
                    break;
            }
        }
    }

    private void showLock(int resId) {
        ImageView lock = (ImageView) mSecuritySwitch.findViewById(R.id.lock_image);
        lock.setImageDrawable(getResources().getDrawable(resId));
        mSecuritySwitch.setDisplayedChild(1);
        mSecuritySwitch.setVisibility(View.VISIBLE);
    }

    private void initIncomingCallDisplay() {
        Log.i(TAG, "Start incoming display");
        final SipCall call = getFirstParticipant();
        if (mCallbacks.getService().getAccount(call.getAccount()).isAutoanswerEnabled()) {
            try {
                mCallbacks.getRemoteService().accept(call.getCallId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            initContactDisplay(call);
            acceptButton.setVisibility(View.VISIBLE);
            refuseButton.setVisibility(View.VISIBLE);
            hangupButton.setVisibility(View.GONE);
        }
    }

    private void initOutGoingCallDisplay() {
        Log.i(TAG, "Start outgoing display");

        final SipCall call = getFirstParticipant();
        initContactDisplay(call);

        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.VISIBLE);
        hangupButton.setVisibility(View.GONE);
    }

    public void updateTime() {
        if (getConference() != null && !getConference().getParticipants().isEmpty()) {
            long duration = System.currentTimeMillis() - getFirstParticipant().getTimestampStart();
            duration = duration / 1000;
            if (getConference().isOnGoing())
                mCallStatusTxt.setText(String.format("%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
        }
    }

    @OnClick({R.id.call_hangup_btn, R.id.call_refuse_btn})
    public void hangUpClicked() {
        try {
            final SipCall call = getFirstParticipant();
            if (call == null) {
                return;
            }
            final String callId = call.getCallId();
            mCallbacks.getRemoteService().hangUp(callId);
            mCallbacks.terminateCall();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.call_accept_btn)
    public void acceptClicked() {
        final SipCall call = getFirstParticipant();
        if (call == null) {
            return;
        }
        try {
            mCallbacks.getRemoteService().accept(call.getCallId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper accessor that check nullity or emptiness of components to access first call participant
     * @return the first participant or null
     */
    @Nullable
    private SipCall getFirstParticipant() {
        if (getConference() == null || getConference().getParticipants() == null || getConference().getParticipants().isEmpty()) {
            return null;
        }
        return getConference().getParticipants().get(0);
    }
}
