/*
 *  Copyright (C) 2004-2015 Savoir-faire Linux Inc.
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
import android.app.FragmentManager;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.CompoundButton.OnCheckedChangeListener;
import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.interfaces.CallInterface;

import java.util.ArrayList;
import java.util.Locale;

import cx.ring.model.BubbleContact;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;
import cx.ring.model.account.Account;
import cx.ring.service.LocalService;

public class CallFragment extends CallableWrapperFragment implements CallInterface {

    static private final String TAG = CallFragment.class.getSimpleName();

    public static final int REQUEST_TRANSFER = 10;

    // Screen wake lock for incoming call
    private WakeLock mScreenWakeLock;
    private ImageView contactBubbleView;
    private TextView contactBubbleTxt;
    private View acceptButton;
    private View refuseButton;
    private View hangupButton;

    private View securityIndicator;

    private final int BTN_MSG_IDX = 0;
    private final int BTN_HOLD_IDX = 1;
    private final int BTN_TRANSFER_IDX = 2;
    private final int BTN_HUNGUP_IDX = 3;

    ViewSwitcher mSecuritySwitch;
    private TextView mCallStatusTxt;
    private ToggleButton mToggleSpeakers;

    public Callbacks mCallbacks = sDummyCallbacks;
    boolean accepted = false;

    TransferDFragment editName;

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

        //mCallbacks.onFragmentCreated();
    }

    /**
     * The Activity calling this fragment has to implement this interface
     */
    public interface Callbacks extends LocalService.Callbacks {
        //void onFragmentCreated();
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

        mCallbacks.getSupportActionBar().setTitle(contact.getDisplayName());
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
            mCallbacks.getSupportActionBar().setTitle(contact.getDisplayName());
        }
    }

    private void initOutGoingCallDisplay() {
        Log.i(TAG, "Start outgoing display");

        final SipCall call = getConference().getParticipants().get(0);
        CallContact contact = call.getContact();
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

        mCallbacks.getSupportActionBar().setTitle(contact.getDisplayName());
    }

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
