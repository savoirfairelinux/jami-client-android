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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.skyfishjy.library.RippleBackground;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.interfaces.CallInterface;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.service.CallManagerCallBack;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.BlockchainInputHandler;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.Photo;

public class CallFragment extends Fragment implements CallInterface, ContactDetailsTask.DetailsLoadedCallback, Observer<ServiceEvent> {

    static final private String TAG = CallFragment.class.getSimpleName();

    public static final int REQUEST_TRANSFER = 10;

    //~ Regular expression to match DTMF supported characters : 0 to 9, A, B, C, D, * and #
    public static final String DTMF_SUPPORTED_CHARS_REGEX = "^[a-dA-D0-9#*]*$";

    // Screen wake lock for incoming call
    private WakeLock mScreenWakeLock;

    @Inject
    AccountService mAccountService;

    @Inject
    NotificationService mNotificationService;

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
    private MenuItem changeScreenOrientationBtn = null;

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

    private BlockchainInputHandler mBlockchainInputHandler;

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
        intentFilter.addAction(CallManagerCallBack.VCARD_COMPLETED);

        intentFilter.addAction(RingApplication.VIDEO_EVENT);

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

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

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
            } else if (action.contentEquals(RingApplication.VIDEO_EVENT)) {
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
            } else if (action.contentEquals(CallManagerCallBack.VCARD_COMPLETED)) {
                updateContactBubble();
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
        changeScreenOrientationBtn = m.findItem(R.id.menuitem_change_screen_orientation);
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
        if (changeScreenOrientationBtn != null) {
            changeScreenOrientationBtn.setVisible(mVideoSurface.getVisibility() == View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        SipCall firstParticipant = getFirstParticipant();
        switch (item.getItemId()) {
            case android.R.id.home:
                if (firstParticipant != null) {
                    startConversationActivity(firstParticipant.getContact());
                }
                break;
            case R.id.menuitem_chat:
                if (firstParticipant == null
                        || firstParticipant.getContact() == null
                        || firstParticipant.getContact().getIds() == null
                        || firstParticipant.getContact().getIds().isEmpty()) {
                    break;
                }
                startConversationActivity(firstParticipant.getContact());
                break;
            case R.id.menuitem_addcontact:
                if (firstParticipant == null || firstParticipant.getContact() == null) {
                    break;
                }
                startActivityForResult(ActionHelper.getAddNumberIntentForContact(firstParticipant.getContact()),
                        ConversationFragment.REQ_ADD_CONTACT);
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
                item.setIcon(lastVideoSource ? R.drawable.ic_camera_front_white : R.drawable.ic_camera_rear_white);
                break;
            case R.id.menuitem_dialpad:
                KeyboardVisibilityManager.showKeyboard(getActivity(),
                        mNumeralDialEditText,
                        InputMethodManager.SHOW_IMPLICIT);
                break;
            case R.id.menuitem_change_screen_orientation:
                changeScreenOrientation();
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

        RingApplication application = (RingApplication) getActivity().getApplication();

        application.videoSurfaces.remove(c.getId());
        application.mCameraPreviewSurface.clear();
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
        if (c != null && mVideoSurface != null && c.shouldResumeVideo()) {
            Log.i(TAG, "Resuming video");
            haveVideo = true;
            mVideoSurface.setVisibility(View.VISIBLE);
            videoPreview.setVisibility(View.VISIBLE);

            c.setResumeVideo(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        mAccountService.addObserver(this);

        Conference conference = getConference();

        confUpdate();

        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        if (conference != null) {
            conference.setVisible(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
            notificationManager.cancel(conference.getUuid());
            if (conference.shouldResumeVideo()) {
                Log.w(TAG, "Resuming video");
                haveVideo = true;
                mVideoSurface.setVisibility(View.VISIBLE);
                videoPreview.setVisibility(View.VISIBLE);

                conference.setResumeVideo(false);
            }
        }

        refreshState();
    }

    @Override
    public void onPause() {
        Log.w(TAG, "onPause() haveVideo=" + haveVideo);
        super.onPause();

        mAccountService.removeObserver(this);

        Conference conference = getConference();
        if (conference != null) {
            conference.setVisible(false);
            conference.setResumeVideo(haveVideo);
            mNotificationService.showCallNotification(conference);
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

        int newState = getHumanState(c);
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
            notificationManager.cancel(c.getUuid());
            mCallStatusTxt.setText(newStateString);
            mCallbacks.terminateCall();
        }
    }

    public int getHumanState(Conference conference) {
        if (conference.getParticipants().size() == 1) {
            return callStateToHumanState(conference.getParticipants().get(0).getCallState());
        }
        return getConferenceHumanState(conference.getState());
    }

    public static int callStateToHumanState(final int state) {
        switch (state) {
            case SipCall.State.INCOMING:
                return R.string.call_human_state_incoming;
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

    public int getConferenceHumanState(final int state) {
        switch (state) {
            case Conference.state.ACTIVE_ATTACHED:
                return R.string.conference_human_state_active_attached;
            case Conference.state.ACTIVE_DETACHED:
                return R.string.conference_human_state_active_detached;
            case Conference.state.ACTIVE_ATTACHED_REC:
                return R.string.conference_human_state_active_attached_rec;
            case Conference.state.ACTIVE_DETACHED_REC:
                return R.string.conference_human_state_active_detached_rec;
            case Conference.state.HOLD:
                return R.string.conference_human_state_hold;
            case Conference.state.HOLD_REC:
                return R.string.conference_human_state_hold_rec;
            default:
                return R.string.conference_human_state_default;
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
        updatePreview();
    }

    private void updatePreview() {
        if (videoPreview.getVisibility() == View.VISIBLE) {
            try {
                mCallbacks.getRemoteService().setPreviewSettings();
                mCallbacks.getRemoteService().videoPreviewSurfaceAdded();
            } catch (RemoteException e) {
                Log.e(TAG, "service not found ", e);
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
                RingApplication application = (RingApplication) getActivity().getApplication();
                contactBubbleLayout.setVisibility(View.GONE);
                Conference c = getConference();
                application.videoSurfaces.put(c.getId(), new WeakReference<>(holder));
                blockSensorScreenRotation();
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
                RingApplication application = (RingApplication) getActivity().getApplication();
                application.videoSurfaces.remove(c.getId());
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
                RingApplication application = (RingApplication) getActivity().getApplication();
                application.mCameraPreviewSurface = new WeakReference<>(holder);
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
                RingApplication application = (RingApplication) getActivity().getApplication();
                if (videoPreview != null && application.mCameraPreviewSurface.get() == holder) {
                    application.mCameraPreviewSurface.clear();
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
            getUsername(call);
        }

        mPulseAnimation.startRippleAnimation();

        updateContactBubble();
    }

    private void getUsername(SipCall call) {
        Log.d(TAG, "blockchain with " + call.getNumber());

        if (mBlockchainInputHandler == null || !mBlockchainInputHandler.isAlive()) {
            mBlockchainInputHandler = new BlockchainInputHandler(new WeakReference<>(mAccountService));
        }

        String[] split = call.getNumber().split(":");
        if (split.length > 0) {
            mBlockchainInputHandler.enqueueNextLookup(split[1]);
        }
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

        updatePreview();
    }

    private void updateSecurityDisplay() {
        //First we check if all participants use a security layer.
        boolean secureCall = !getConference().getParticipants().isEmpty();
        for (SipCall c : getConference().getParticipants())
            secureCall &= c instanceof SecureSipCall && ((SecureSipCall) c).isSecure();

        securityIndicator.setVisibility(secureCall ? View.VISIBLE : View.GONE);
        if (!secureCall)
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
        lock.setImageDrawable(ResourcesCompat.getDrawable(getResources(), resId, null));
        mSecuritySwitch.setDisplayedChild(1);
        mSecuritySwitch.setVisibility(View.VISIBLE);
    }

    private void initIncomingCallDisplay() {
        Log.i(TAG, "Start incoming display");
        final SipCall call = getFirstParticipant();
        if (mAccountService.getAccount(call.getAccount()).isAutoanswerEnabled()) {
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
                mCallStatusTxt.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
        }
    }

    /**
     * Updates the bubble contact image with the vcard image, the contact image or by default the
     * contact picture drawable.
     */
    private void updateContactBubble() {
        Conference conference = this.getConference();
        Context context = getActivity();
        if (conference == null || context == null) {
            return;
        }

        SipCall participant = getFirstParticipant();
        if (participant == null) {
            return;
        }

        VCard vcard;
        String username = participant.getNumberUri().getUsername();
        Log.d(TAG, "username " + username);
        vcard = VCardUtils.loadPeerProfileFromDisk(context.getFilesDir(), username + ".vcf");
        if (vcard == null) {
            Log.d(TAG, "No vcard.");
            setDefaultPhoto();
            return;
        } else {
            Log.d(TAG, "VCard found: " + vcard);
        }

        if (!vcard.getPhotos().isEmpty()) {
            Photo tmp = vcard.getPhotos().get(0);
            if (tmp.getData() != null) {
                contactBubbleView.setImageBitmap(BitmapUtils.cropImageToCircle(tmp.getData()));
            } else {
                setDefaultPhoto();
            }
        } else {
            setDefaultPhoto();
        }

        if (TextUtils.isEmpty(vcard.getFormattedName().getValue())) {
            return;
        }
        contactBubbleTxt.setText(vcard.getFormattedName().getValue());
        ActionBar ab = mCallbacks.getSupportActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        ab.setTitle(vcard.getFormattedName().getValue());

        if (participant.getNumber().contentEquals(vcard.getFormattedName().getValue())) {
            contactBubbleNumTxt.setVisibility(View.GONE);
        } else {
            contactBubbleNumTxt.setVisibility(View.VISIBLE);
            contactBubbleNumTxt.setText(participant.getNumber());
            getUsername(participant);
        }
    }


    @OnClick({R.id.call_hangup_btn, R.id.call_refuse_btn})
    public void hangUpClicked(View view) {
        try {
            final SipCall call = getFirstParticipant();
            if (call == null) {
                return;
            }
            final String callId = call.getCallId();
            startConversationActivity(call.getContact());
            if (view.getId() == R.id.call_hangup_btn) {
                mCallbacks.getRemoteService().hangUp(callId);
            } else {
                mCallbacks.getRemoteService().refuse(callId);
            }
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

    public void changeScreenOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void blockSensorScreenRotation() {
        changeScreenOrientationBtn.setVisible(true);
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

    /**
     * Helper accessor that check nullity or emptiness of components to access first call participant
     *
     * @return the first participant or null
     */
    @Nullable
    private SipCall getFirstParticipant() {
        if (getConference() == null || getConference().getParticipants() == null || getConference().getParticipants().isEmpty()) {
            return null;
        }
        return getConference().getParticipants().get(0);
    }

    public void onBackPressed() {
        SipCall call = getFirstParticipant();
        if (call != null) {
            startConversationActivity(call.getContact());
        }
    }

    private void startConversationActivity(CallContact contact) {
        if (contact == null || contact.getIds().isEmpty()) {
            return;
        }
        Intent intent = new Intent();
        if (ConversationFragment.isTabletMode(getActivity())) {
            intent.setClass(getActivity(), HomeActivity.class)
                    .setAction(LocalService.ACTION_CONV_ACCEPT)
                    .putExtra("conversationID", contact.getIds().get(0));
            startActivity(intent);
        } else {
            intent.setClass(getActivity(), ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .setData(Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, contact.getIds().get(0)))
                    .putExtra("resuming", true);
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    private void setDefaultPhoto() {
        if (getConference() != null
                && getConference().getParticipants() != null
                && !getConference().getParticipants().isEmpty()) {
            final SipCall call = getConference().getParticipants().get(0);
            final CallContact contact = call.getContact();
            if (contact != null) {
                new ContactDetailsTask(getActivity(), contact, this).run();
            }
        } else {
            contactBubbleView.setImageDrawable(
                    ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null));
        }
    }

    @Override
    public void onDetailsLoaded(Bitmap bmp, String formattedName) {
        if (bmp != null) {
            contactBubbleView.setImageBitmap(bmp);
        }

        if (formattedName != null) {
            contactBubbleTxt.setText(formattedName);
            ActionBar ab = mCallbacks.getSupportActionBar();
            ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
            ab.setTitle(formattedName);
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTERED_NAME_FOUND:
                final String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                RingApplication.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        contactBubbleNumTxt.setText(name);
                    }
                });
                break;
            default:
                Log.d(TAG, "This event type is not handled here " + event.getEventType());
                break;
        }
    }
}
