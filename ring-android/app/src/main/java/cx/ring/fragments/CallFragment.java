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
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.interfaces.CallInterface;

import java.util.Locale;

import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;
import cx.ring.service.DRingService;
import cx.ring.service.LocalService;

public class CallFragment extends CallableWrapperFragment implements CallInterface {

    static private final String TAG = CallFragment.class.getSimpleName();

    public static final int REQUEST_TRANSFER = 10;

    // Screen wake lock for incoming call
    private WakeLock mScreenWakeLock;
    private ImageView contactBubbleView;
    private TextView contactBubbleTxt;
    private TextView contactBubbleNumTxt;
    private View acceptButton;
    private View refuseButton;
    private View hangupButton;
    private View securityIndicator;
    private MenuItem speakerPhoneBtn = null;
    private MenuItem addContactBtn = null;

    ViewSwitcher mSecuritySwitch;
    private TextView mCallStatusTxt;

    public Callbacks mCallbacks = sDummyCallbacks;

    private AudioManager audioManager;

    TransferDFragment editName;

    @Override
    public void onCreate(Bundle savedBundle) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedBundle);

        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        setHasOptionsMenu(true);
        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "cx.ring.onIncomingCall");
        mScreenWakeLock.setReferenceCounted(false);

        Log.d(TAG, "Acquire wake up lock");
        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
        }
    }

    /**
     * The Activity calling this fragment has to implement this interface
     */
    public interface Callbacks extends LocalService.Callbacks {
        void startTimer();
        void terminateCall();
        Conference getDisplayedConference();
        void updateDisplayedConference(Conference c);
        ActionBar getSupportActionBar();
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static class DummyCallbacks extends LocalService.DummyCallbacks implements Callbacks {
        @Override
        public void terminateCall() {}
        @Override
        public Conference getDisplayedConference() {
            return null;
        }
        @Override
        public void updateDisplayedConference(Conference c) { }
        @Override
        public ActionBar getSupportActionBar() { return null; }
        @Override
        public void startTimer() { }
    }
    private static final Callbacks sDummyCallbacks = new DummyCallbacks();

    @Override
    public void onAttach(Activity activity) {
        Log.i(TAG, "onAttach");
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    public void refreshState() {
        Conference conf = getConference();
        if (conf == null)  {
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
    }

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.ac_call, m);
        speakerPhoneBtn = m.findItem(R.id.menuitem_speaker);
        addContactBtn = m.findItem(R.id.menuitem_addcontact);
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
            addContactBtn.setVisible(getConference().getParticipants().get(0).getContact().isUnknown());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menuitem_chat:
                Intent intent = new Intent()
                        .setClass(getActivity(), ConversationActivity.class)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, getConference().getParticipants().get(0).getContact().getIds().get(0)));
                intent.putExtra("resuming", true);
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
                break;
            case R.id.menuitem_addcontact:
                startActivityForResult(getConference().getParticipants().get(0).getContact().getAddNumberIntent(), ConversationActivity.REQ_ADD_CONTACT);
                break;
            case R.id.menuitem_speaker:
                audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
                getActivity().invalidateOptionsMenu();
                break;
        }
        return true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        Log.w(TAG, "onResume()");
        super.onResume();
        //initializeWiFiListener();
        refreshState();

        Conference c = getConference();
        if (c != null) {
            c.mVisible = true;
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
            notificationManager.cancel(c.notificationId);
        }
    }

    @Override
    public void onPause() {
        Log.w(TAG, "onPause()");
        super.onPause();
        //getActivity().unregisterReceiver(wifiReceiver);
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
        Conference c = getConference();
        if (c != null) {
            c.mVisible = false;
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

        String newState = c.getState();
        if (c.isOnGoing()) {
            initNormalStateDisplay();
        } else if (c.isRinging()) {
            mCallStatusTxt.setText(newState);

            if (c.isIncoming()) {
                initIncomingCallDisplay();
            } else
                initOutGoingCallDisplay();
        } else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
            notificationManager.cancel(c.notificationId);
            mCallStatusTxt.setText(newState);
            mCallbacks.terminateCall();
        }
    }

    @Override
    public void secureZrtpOn(Conference updated, String id) {
        Log.i(TAG, "secureZrtpOn");
        mCallbacks.updateDisplayedConference(updated);
        updateSecurityDisplay();
    }

    @Override
    public void secureZrtpOff(Conference updated, String id) {
        Log.i(TAG, "secureZrtpOff");
        mCallbacks.updateDisplayedConference(updated);
        updateSecurityDisplay();
    }

    @Override
    public void displaySAS(Conference updated, final String securedCallID) {
        Log.i(TAG, "displaySAS");
        mCallbacks.updateDisplayedConference(updated);
        updateSecurityDisplay();
    }

    @Override
    public void zrtpNegotiationFailed(Conference c, String securedCallID) {
        mCallbacks.updateDisplayedConference(c);
        updateSecurityDisplay();
    }

    @Override
    public void zrtpNotSupported(Conference c, String securedCallID) {
        mCallbacks.updateDisplayedConference(c);
        updateSecurityDisplay();
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
                    initNormalStateDisplay();
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

        contactBubbleView = (ImageView) rootView.findViewById(R.id.contact_bubble);
        contactBubbleTxt = (TextView) rootView.findViewById(R.id.contact_bubble_txt);
        contactBubbleNumTxt = (TextView) rootView.findViewById(R.id.contact_bubble_num_txt);
        acceptButton  = rootView.findViewById(R.id.call_accept_btn);
        refuseButton  = rootView.findViewById(R.id.call_refuse_btn);
        hangupButton  = rootView.findViewById(R.id.call_hangup_btn);
        mCallStatusTxt = (TextView) rootView.findViewById(R.id.call_status_txt);
        mSecuritySwitch = (ViewSwitcher) rootView.findViewById(R.id.security_switcher);
        securityIndicator = rootView.findViewById(R.id.security_indicator);

        SurfaceView preview = (SurfaceView)rootView.findViewById(R.id.video_preview_surface);
        DRingService.mVideoPreviewSurface = preview;

        preview = (SurfaceView)rootView.findViewById(R.id.camera_preview_surface);
        preview.setZOrderOnTop(true);
        DRingService.mCameraPreviewSurface = preview;

        return rootView;
    }

    public Conference getConference() {
        return mCallbacks.getDisplayedConference();
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
        new ContactPictureTask(getActivity(), contactBubbleView, contact).run();
        mCallbacks.getSupportActionBar().setTitle(name);
    }

    private void initNormalStateDisplay() {
        Log.i(TAG, "Start normal display");
        mCallbacks.startTimer();
        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.GONE);

        final SipCall call = getConference().getParticipants().get(0);
        final String call_id = call.getCallId();
        initContactDisplay(call);

        hangupButton.setVisibility(View.VISIBLE);
        hangupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mCallbacks.getRemoteService().hangUp(call_id);
                    mCallbacks.terminateCall();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        updateSecurityDisplay();
    }

    private void updateSecurityDisplay() {
        //First we check if all participants use a security layer.
        boolean secure_call = !getConference().getParticipants().isEmpty();
        for (SipCall c : getConference().getParticipants())
            secure_call &= c instanceof SecureSipCall && ((SecureSipCall)c).isSecure();

        securityIndicator.setVisibility(secure_call ? View.VISIBLE : View.GONE);
        if (!secure_call)
            return;

        Log.i(TAG, "Enable security display");
        if (getConference().hasMultipleParticipants()) {
            //TODO What layout should we put?
        } else {
            final SecureSipCall secured = (SecureSipCall) getConference().getParticipants().get(0);
            switch (secured.displayModule()) {
                case SecureSipCall.DISPLAY_GREEN_LOCK:
                    Log.i(TAG, "DISPLAY_GREEN_LOCK");
                    showLock(R.drawable.green_lock);
                    break;
                case SecureSipCall.DISPLAY_RED_LOCK:
                    Log.i(TAG, "DISPLAY_RED_LOCK");
                    showLock(R.drawable.red_lock);
                    break;
                case SecureSipCall.DISPLAY_CONFIRM_SAS:
                    final Button sas = (Button) mSecuritySwitch.findViewById(R.id.confirm_sas);
                    sas.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                mCallbacks.getRemoteService().confirmSAS(secured.getCallId());
                                showLock(R.drawable.green_lock);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    mSecuritySwitch.setDisplayedChild(0);
                    mSecuritySwitch.setVisibility(View.VISIBLE);
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

    protected Bitmap getContactPhoto(CallContact contact, int size) {
        if (contact.getPhotoId() > 0) {
            return ContactPictureTask.loadContactPhoto(getActivity().getContentResolver(), contact.getId());
        } else {
            return ContactPictureTask.decodeSampledBitmapFromResource(getResources(), R.drawable.ic_contact_picture, size, size);
        }
    }

    private void initIncomingCallDisplay() {
        Log.i(TAG, "Start incoming display");
        if (mCallbacks.getService().getAccount(getConference().getParticipants().get(0).getAccount()).isAutoanswerEnabled()) {
            try {
                mCallbacks.getRemoteService().accept(getConference().getParticipants().get(0).getCallId());
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            final SipCall call = getConference().getParticipants().get(0);
            initContactDisplay(call);
            acceptButton.setVisibility(View.VISIBLE);
            acceptButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    acceptButton.setOnClickListener(null);
                    refuseButton.setOnClickListener(null);
                    try {
                        mCallbacks.getRemoteService().accept(call.getCallId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
            refuseButton.setVisibility(View.VISIBLE);
            refuseButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    acceptButton.setOnClickListener(null);
                    refuseButton.setOnClickListener(null);
                    try {
                        mCallbacks.getRemoteService().refuse(call.getCallId());
                        mCallbacks.terminateCall();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
            hangupButton.setVisibility(View.GONE);
        }
    }

    private void initOutGoingCallDisplay() {
        Log.i(TAG, "Start outgoing display");

        final SipCall call = getConference().getParticipants().get(0);
        initContactDisplay(call);

        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.GONE);

        hangupButton.setVisibility(View.VISIBLE);
        hangupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mCallbacks.getRemoteService().hangUp(call.getCallId());
                    mCallbacks.terminateCall();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /*
    public void makeTransfer(BubbleContact contact) {
        FragmentManager fm = getFragmentManager();
        editName = TransferDFragment.newInstance();
        Bundle b = new Bundle();
        try {
            b.putParcelableArrayList("calls", (ArrayList<Conference>) mCallbacks.getRemoteService().getConcurrentCalls());
            b.putParcelable("call_selected", contact.associated_call);
            editName.setArguments(b);
            editName.setTargetFragment(this, REQUEST_TRANSFER);
            editName.show(fm, "");
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

    }*/

    public void updateTime() {
        if (getConference() != null && !getConference().getParticipants().isEmpty()) {
            long duration = System.currentTimeMillis() - getConference().getParticipants().get(0).getTimestampStart();
            duration = duration / 1000;
            if (getConference().isOnGoing())
                mCallStatusTxt.setText(String.format("%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
        }

    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        try {

            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_UP:
                    break;
                default:
                    String toSend = Character.toString(event.getDisplayLabel());
                    toSend = toSend.toUpperCase(Locale.getDefault());
                    Log.d(TAG, "toSend " + toSend);
                    mCallbacks.getRemoteService().playDtmf(toSend);
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
