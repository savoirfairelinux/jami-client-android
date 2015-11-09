/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.interfaces.CallInterface;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import cx.ring.model.BubbleContact;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;
import cx.ring.model.account.Account;
import cx.ring.service.LocalService;

public class CallFragment extends CallableWrapperFragment implements CallInterface {

    static final String TAG = "CallFragment";

    private float bubbleSize = 75; // dip
    private float attractorSize = 40;
    public static final int REQUEST_TRANSFER = 10;

    // Screen wake lock for incoming call
    private WakeLock mScreenWakeLock;
    private ImageView contactBubbleView;
    private TextView contactBubbleTxt;
    private View acceptButton;
    private View refuseButton;
    private View hangupButton;

    private View securityIndicator;

    /*
    private BubblesView mBubbleView;
    private BubbleModel mBubbleModel;

    private Bitmap buttonCall;
    private Bitmap buttonMsg;
    private Bitmap buttonHold;
    private Bitmap buttonUnhold;
    private Bitmap buttonTransfer;
    private Bitmap buttonHangUp;
*/
    private final int BTN_MSG_IDX = 0;
    private final int BTN_HOLD_IDX = 1;
    private final int BTN_TRANSFER_IDX = 2;
    private final int BTN_HUNGUP_IDX = 3;
/*
    private BubbleModel.ActionGroup userActions;
    private BubbleModel.ActionGroup callActions;
*/
    ViewSwitcher mSecuritySwitch;
    private TextView mCallStatusTxt;
    private ToggleButton mToggleSpeakers;

    public Callbacks mCallbacks = sDummyCallbacks;
    boolean accepted = false;

    TransferDFragment editName;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            WifiInfo info = wifiManager.getConnectionInfo();
            Log.i(TAG, "Level of wifi " + info.getRssi());
        }
    };

    @Override
    public void onCreate(Bundle savedBundle) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedBundle);

        Resources r = getResources();
/*
        bubbleSize = r.getDimension(R.dimen.bubble_size);
        attractorSize = r.getDimension(R.dimen.bubble_action_size);
        float attractorMargin = r.getDimension(R.dimen.bubble_action_margin);

        buttonCall = BitmapFactory.decodeResource(r, R.drawable.ic_action_call);
        buttonMsg = BitmapFactory.decodeResource(r, R.drawable.ic_action_chat);
        buttonHold = BitmapFactory.decodeResource(r, R.drawable.ic_action_pause_over_video);
        buttonUnhold = BitmapFactory.decodeResource(r, R.drawable.ic_action_play_over_video);
        buttonTransfer = BitmapFactory.decodeResource(r, R.drawable.ic_action_forward);
        buttonHangUp = BitmapFactory.decodeResource(r, R.drawable.ic_action_end_call);

        BubbleModel.ActionGroupCallback cb = new BubbleModel.ActionGroupCallback() {
            @Override
            public boolean onBubbleAction(Bubble b, int action) {
                Log.i(TAG, "onBubbleAction ! "+action);
                switch(action) {
                    case BTN_HUNGUP_IDX:
                        try {
                            if (b.isConference())
                                mCallbacks.getService().hangUpConference(b.getCallID());
                            else
                                mCallbacks.getService().hangUp(b.getCallID());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return true;
                    case BTN_HOLD_IDX:
                        try {
                            if (b.getHoldStatus()) {
                                if (b.isConference())
                                    mCallbacks.getService().unholdConference(b.getCallID());
                                else
                                    mCallbacks.getService().unhold(b.getCallID());
                            } else {
                                if (b.isConference())
                                    mCallbacks.getService().holdConference(b.getCallID());
                                else
                                    mCallbacks.getService().hold(b.getCallID());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return false;
                    case BTN_TRANSFER_IDX:
                        makeTransfer((BubbleContact) b);
                        return false;
                }
                return false;
            }
        };

        userActions = new BubbleModel.ActionGroup(cb, attractorMargin, .4f, .25f);
        userActions.addAction(BTN_HOLD_IDX, buttonHold, getString(R.string.action_call_hold), attractorSize);
        userActions.addAction(BTN_HUNGUP_IDX, buttonHangUp, getString(R.string.action_call_hangup), attractorSize);

        callActions = new BubbleModel.ActionGroup(cb, attractorMargin, .4f, .25f);
        callActions.addAction(BTN_HOLD_IDX, buttonHold, getString(R.string.action_call_hold), attractorSize);
        callActions.addAction(BTN_TRANSFER_IDX, buttonTransfer, getString(R.string.action_call_attended_transfer), attractorSize);
        callActions.addAction(BTN_HUNGUP_IDX, buttonHangUp, getString(R.string.action_call_hangup), attractorSize);

        mBubbleModel = new BubbleModel(r.getDisplayMetrics().density, new BubbleModel.ModelCallback() {
            @Override
            public void bubbleGrabbed(Bubble b) {
                if (mBubbleModel.curState != BubbleModel.State.Incall) {
                    return;
                }
                if (b.isUser) {
                    mBubbleModel.setActions(b, userActions);
                } else {
                    mBubbleModel.setActions(b, callActions);
                }
            }

            @Override
            public boolean bubbleEjected(Bubble b) {
                try {
                    if (b.isConference())
                        mCallbacks.getService().hangUpConference(b.getCallID());
                    else
                        mCallbacks.getService().hangUp(b.getCallID());

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });*/

        setHasOptionsMenu(true);
        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "cx.ring.onIncomingCall");
        mScreenWakeLock.setReferenceCounted(false);

        Log.d(TAG, "Acquire wake up lock");
        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
        }

        mCallbacks.onFragmentCreated();
    }

    private void initializeWiFiListener() {
        String connectivity_context = Context.WIFI_SERVICE;
        wifiManager = (WifiManager) getActivity().getSystemService(connectivity_context);
        getActivity().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
    }

    /**
     * The Activity calling this fragment has to implement this interface
     */
    public interface Callbacks extends LocalService.Callbacks {
        void onFragmentCreated();
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
        public void onFragmentCreated() {}
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

        // rootView.requestDisallowInterceptTouchEvent(true);

        mCallbacks = (Callbacks) activity;
        // myself = SipCall.SipCallBuilder.buildMyselfCall(activity.getContentResolver(), "Me");

    }

    public void refreshState() {
        Conference conf = getConference();
        if (conf == null)  {
            contactBubbleView.setImageBitmap(null);
            contactBubbleTxt.setText("");
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menuitem_chat:
                //mCallbacks.slideChatScreen();
                Intent intent = new Intent()
                        .setClass(getActivity(), ConversationActivity.class)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, getConference().getParticipants().get(0).getContact().getIds().get(0)));
                intent.putExtra("resuming", true);
                //intent.putExtra("contact", ((Conversation) v.getTag()).getContact());
                //intent.putExtra("conversation", (Conversation) v.getTag());
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
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
        super.onResume();
        initializeWiFiListener();
        refreshState();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(wifiReceiver);
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }
    }

    @Override
    public void callStateChanged(Conference updated, String callID, String newState) {
        Conference cur = getConference();
        if (cur.getId().equals(callID) || cur.getCallById(callID) != null) {
            mCallbacks.updateDisplayedConference(updated);
        } else {
            return;
        }

        Log.i(TAG, "Call :" + callID + " " + newState);

        if (getConference().isOnGoing()) {
            initNormalStateDisplay();
        } else if (getConference().isRinging()) {
            mCallStatusTxt.setText(newState);

            if (getConference().isIncoming()) {
                initIncomingCallDisplay();
            } else
                initOutGoingCallDisplay();
        } else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
            notificationManager.cancel(getConference().notificationId);
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
                    /*synchronized (mBubbleModel) {
                        mBubbleModel.clear();
                    }*/
                    initNormalStateDisplay();
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

/*
        mBubbleView = (BubblesView) rootView.findViewById(R.id.main_view);
        //mBubbleView.setFragment(this);
        mBubbleView.setModel(mBubbleModel);
        mBubbleView.getHolder().addCallback(this);
*/
        contactBubbleView = (ImageView) rootView.findViewById(R.id.contact_bubble);
        contactBubbleTxt = (TextView) rootView.findViewById(R.id.contact_bubble_txt);
        acceptButton  = rootView.findViewById(R.id.call_accept_btn);
        refuseButton  = rootView.findViewById(R.id.call_refuse_btn);
        hangupButton  = rootView.findViewById(R.id.call_hangup_btn);

        mCallStatusTxt = (TextView) rootView.findViewById(R.id.call_status_txt);

        mSecuritySwitch = (ViewSwitcher) rootView.findViewById(R.id.security_switcher);
        mToggleSpeakers = (ToggleButton) rootView.findViewById(R.id.speaker_toggle);

        mToggleSpeakers.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    mCallbacks.getRemoteService().toggleSpeakerPhone(isChecked);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
/*
        synchronized (mBubbleModel) {
            mBubbleModel.setSize(mBubbleView.getWidth(), mBubbleView.getHeight() - mToggleSpeakers.getHeight(), bubbleSize);
        }*/
/*
        rootView.findViewById(R.id.dialpad_btn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                InputMethodManager lManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                lManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });*/

        securityIndicator = rootView.findViewById(R.id.security_indicator);

        return rootView;
    }

    public Conference getConference() {
        return mCallbacks.getDisplayedConference();
    }

    private void initNormalStateDisplay() {
        Log.i(TAG, "Start normal display");
        mCallbacks.startTimer();
        acceptButton.setVisibility(View.GONE);
        refuseButton.setVisibility(View.GONE);

        final SipCall call = getConference().getParticipants().get(0);
        CallContact contact = call.getContact();
        //contactBubbleView.setImageBitmap(getContactPhoto(contact, contactBubbleView.getWidth()));
        new ContactPictureTask(getActivity(), contactBubbleView, contact).run();
        contactBubbleTxt.setText(contact.getDisplayName());

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

        NotificationCompat.Builder noti = new NotificationCompat.Builder(getActivity())
                .setContentTitle("Current call with " + contact.getDisplayName())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText("call")
                .setContentIntent(PendingIntent.getActivity(getActivity(), new Random().nextInt(),
                        new Intent(getActivity(), CallActivity.class).putExtra("conference", getConference()), PendingIntent.FLAG_ONE_SHOT))
                .addAction(R.drawable.ic_call_end_white_24dp, "Hangup",
                        PendingIntent.getService(getActivity(), new Random().nextInt(),
                                new Intent(getActivity(), SipService.class)
                                        .setAction(SipService.ACTION_CALL_END)
                                        .putExtra("conf", call.getCallId()),
                                PendingIntent.FLAG_ONE_SHOT));
        Log.w("CallNotification ", "Updating " + getConference().notificationId + " for " + contact.getDisplayName());
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
        notificationManager.notify(getConference().notificationId, noti.build());

        mCallbacks.getSupportActionBar().setTitle(contact.getDisplayName());

        /*synchronized (mBubbleModel) {
            mCallbacks.startTimer();
            mBubbleModel.clearAttractors();
            PointF c = mBubbleModel.getCircleCenter();

            getBubbleForUser(getConference(), (int) c.x, (int) c.y);

            final float angle_part = (float) (2 * Math.PI / getConference().getParticipants().size());
            final float angle_shift = (float) (Math.PI / 2);
            float radiusCalls = mBubbleModel.getCircleSize();
            for (int i = 0; i < getConference().getParticipants().size(); ++i) {
                SipCall partee = getConference().getParticipants().get(i);
                if (partee == null) {
                    continue;
                }
                double dX = Math.cos(angle_part * i + angle_shift) * radiusCalls;
                double dY = Math.sin(angle_part * i + angle_shift) * radiusCalls;
                getBubbleFor(partee, (int) (c.x + dX), (int) (c.y + dY));
            }
        }
        mBubbleModel.curState = BubbleModel.State.Incall;*/
        updateSecurityDisplay();
    }

    private void updateSecurityDisplay() {
        //First we check if at least one participant use a security layer.
        boolean secure_call = false;
        for (SipCall c : getConference().getParticipants()) {
            Account acc = mCallbacks.getService().getAccount(c.getAccount());
            if (acc != null && (acc.isRing() || acc.useSecureLayer())) {
                secure_call = true;
                break;
            }
        }

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
                    Log.i(TAG, "Confirm SAS: " + secured.getSAS());
                    sas.setText("Confirm SAS: " + secured.getSAS());
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
            CallContact contact = call.getContact();
            //contactBubbleView.setImageBitmap(getContactPhoto(contact, contactBubbleView.getWidth()));
            new ContactPictureTask(getActivity(), contactBubbleView, contact).run();
            contactBubbleTxt.setText(contact.getDisplayName());
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

            NotificationCompat.Builder noti = new NotificationCompat.Builder(getActivity())
                    .setContentTitle("Incoming call with " + contact.getDisplayName())
                    .setContentText("incoming call")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(PendingIntent.getActivity(getActivity(), new Random().nextInt(),
                            new Intent(getActivity(), CallActivity.class).putExtra("conference", getConference()), PendingIntent.FLAG_ONE_SHOT))
                    .addAction(R.drawable.ic_action_accept, "Accept",
                            PendingIntent.getService(getActivity(), new Random().nextInt(),
                                    new Intent(getActivity(), SipService.class)
                                            .setAction(SipService.ACTION_CALL_ACCEPT)
                                            .putExtra("conf", call.getCallId()),
                                    PendingIntent.FLAG_ONE_SHOT))
                    .addAction(R.drawable.ic_call_end_white_24dp, "Refuse",
                            PendingIntent.getService(getActivity(), new Random().nextInt(),
                                    new Intent(getActivity(), SipService.class)
                                            .setAction(SipService.ACTION_CALL_REFUSE)
                                            .putExtra("conf", call.getCallId()),
                                    PendingIntent.FLAG_ONE_SHOT));
            Log.w("CallNotification ", "Updating for incoming " + getConference().notificationId);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
            notificationManager.notify(getConference().notificationId, noti.build());

            mCallbacks.getSupportActionBar().setTitle(contact.getDisplayName());


            /*getBubbleFor(getConference().getParticipants().get(0), mBubbleModel.getWidth() / 2, 2 * mBubbleModel.getHeight() / 3);
            synchronized (mBubbleModel) {
                mBubbleModel.clearAttractors();
                mBubbleModel.addAttractor(new Attractor(new PointF(3 * mBubbleModel.getWidth() / 4, 2 * mBubbleModel.getHeight() / 3), attractorSize, new Attractor.Callback() {
                    @Override
                    public boolean onBubbleSucked(Bubble b) {
                        if (!accepted) {
                            try {
                                mCallbacks.getService().accept(b.getCallID());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            accepted = true;
                        }
                        return false;
                    }
                }, buttonCall));
                mBubbleModel.addAttractor(new Attractor(new PointF(mBubbleModel.getWidth() / 4, 2 * mBubbleModel.getHeight() / 3), attractorSize, new Attractor.Callback() {
                    @Override
                    public boolean onBubbleSucked(Bubble b) {
                        if (!accepted) {
                            try {
                                mCallbacks.getService().refuse(b.getCallID());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            accepted = true;
                        }
                        return false;
                    }
                }, buttonHangUp));
            }
            mBubbleModel.curState = BubbleModel.State.Incoming;*/
        }
    }

    private void initOutGoingCallDisplay() {
        Log.i(TAG, "Start outgoing display");

        final SipCall call = getConference().getParticipants().get(0);
        CallContact contact = call.getContact();
        //contactBubbleView.setImageBitmap(getContactPhoto(contact, contactBubbleView.getWidth()));
        new ContactPictureTask(getActivity(), contactBubbleView, contact).run();
        contactBubbleTxt.setText(contact.getDisplayName());

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

        NotificationCompat.Builder noti = new NotificationCompat.Builder(getActivity())
                .setContentTitle("Outgoing call with " + contact.getDisplayName())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText("Outgoing call")
                .setContentIntent(PendingIntent.getActivity(getActivity(), new Random().nextInt(),
                        new Intent(getActivity(), CallActivity.class).putExtra("conference", getConference()), PendingIntent.FLAG_ONE_SHOT))
                .addAction(R.drawable.ic_call_end_white_24dp, "Cancel",
                        PendingIntent.getService(getActivity(), new Random().nextInt(),
                                new Intent(getActivity(), SipService.class)
                                        .setAction(SipService.ACTION_CALL_END)
                                        .putExtra("conf", call.getCallId()),
                                PendingIntent.FLAG_ONE_SHOT));

        Log.w("CallNotification ", "Updating for outgoing " + getConference().notificationId);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
        notificationManager.notify(getConference().notificationId, noti.build());

        mCallbacks.getSupportActionBar().setTitle(contact.getDisplayName());

        /*synchronized (mBubbleModel) {
            PointF c = mBubbleModel.getCircleCenter();
            float radiusCalls = mBubbleModel.getCircleSize();
            getBubbleForUser(getConference(), c.x, c.y);
            int angle_part = 360 / getConference().getParticipants().size();
            for (int i = 0; i < getConference().getParticipants().size(); ++i) {
                double dX = Math.cos(Math.toRadians(angle_part * i + 90)) * radiusCalls;
                double dY = Math.sin(Math.toRadians(angle_part * i + 90)) * radiusCalls;
                getBubbleFor(getConference().getParticipants().get(i), (int) (c.x + dX), (int) (c.y + dY));
            }
            mBubbleModel.clearAttractors();
        }
        mBubbleModel.curState = BubbleModel.State.Outgoing;*/
    }
    /*
        /**
         * Retrieves or create a bubble for a given contact. If the bubble exists, it is moved to the new location.
         *
         * @param call The call associated to a contact
         * @param x    Initial or new x position.
         * @param y    Initial or new y position.
         * @return Bubble corresponding to the contact.
         */
   /*  private Bubble getBubbleFor(SipCall call, float x, float y) {
        Bubble contact_bubble = mBubbleModel.getBubble(call.getCallId());
        if (contact_bubble != null) {
            ((BubbleContact) contact_bubble).setCall(call);
            contact_bubble.attractionPoint.set(x, y);
            return contact_bubble;
        }

        contact_bubble = new BubbleContact(getActivity(), call, x, y, bubbleSize);

        mBubbleModel.addBubble(contact_bubble);
        return contact_bubble;
    }

    private Bubble getBubbleForUser(Conference conf, float x, float y) {
        Bubble contact_bubble = mBubbleModel.getUser();
        if (contact_bubble != null) {
            contact_bubble.attractionPoint.set(x, y);
            ((BubbleUser) contact_bubble).setConference(conf);

            return contact_bubble;
        }

        contact_bubble = new BubbleUser(getActivity(), CallContact.ContactBuilder.buildUserContact(getActivity().getContentResolver()), conf, x, y,
                bubbleSize * 1.3f);
        mBubbleModel.addBubble(contact_bubble);
        return contact_bubble;
    }


    public boolean canOpenIMPanel() {
        return mBubbleModel.curState == BubbleModel.State.Incall && (mBubbleView == null || !mBubbleView.isDraggingBubble());
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        synchronized (mBubbleModel) {
            mBubbleModel.setSize(width, height, bubbleSize);
        }
        Log.w(TAG, "CallFragment surfaceChanged " + getConference().getParticipants().size());
        if (getConference().getParticipants().size() == 1) {
            if (getConference().getParticipants().get(0).isIncoming() && getConference().getParticipants().get(0).isRinging()) {
                Log.w(TAG, "CallFragment surfaceChanged INCOMING" + getConference().getParticipants().get(0).getCallId());
                initIncomingCallDisplay();
            } else if (getConference().getParticipants().get(0).isRinging()) {
                Log.w(TAG, "CallFragment surfaceChanged RINGING" + getConference().getParticipants().get(0).getCallId());
                initOutGoingCallDisplay();
            } else if (getConference().getParticipants().get(0).isOngoing()) {
                initNormalStateDisplay();
            }
        } else if (getConference().getParticipants().size() > 1) {
            initNormalStateDisplay();
        }
        if (getConference().getParticipants().size() == 1) {
            if (getConference().getParticipants().get(0).isIncoming() && getConference().getParticipants().get(0).isRinging()) {
                Log.w(TAG, "CallFragment surfaceChanged INCOMING" + getConference().getParticipants().get(0).getCallId());
                initIncomingCallDisplay();
            } else if (getConference().getParticipants().get(0).isRinging()) {
                Log.w(TAG, "CallFragment surfaceChanged RINGING" + getConference().getParticipants().get(0).getCallId());
                initOutGoingCallDisplay();
            } else if (getConference().getParticipants().get(0).isOngoing()) {
                initNormalStateDisplay();
            }
        } else if (getConference().getParticipants().size() > 1) {
            initNormalStateDisplay();
        }
    }
*/
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

    }
/*
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // check that soft input is hidden
        InputMethodManager lManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        lManager.hideSoftInputFromWindow(mBubbleView.getWindowToken(), 0);
        if (editName != null && editName.isVisible()) {
            editName.dismiss();
        }
    }

    public BubblesView getBubbleView() {
        return mBubbleView;
    }
*/
    public void updateTime() {
        if (getConference() != null) {
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
