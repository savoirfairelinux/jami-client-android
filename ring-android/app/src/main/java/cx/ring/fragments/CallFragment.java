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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.*;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import cx.ring.R;
import cx.ring.interfaces.CallInterface;
import cx.ring.service.ISipService;

import java.util.ArrayList;
import java.util.Locale;

import cx.ring.model.Attractor;
import cx.ring.model.Bubble;
import cx.ring.model.BubbleContact;
import cx.ring.model.BubbleModel;
import cx.ring.model.BubbleUser;
import cx.ring.model.BubblesView;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;

public class CallFragment extends CallableWrapperFragment implements CallInterface, Callback {

    static final String TAG = "CallFragment";



    private float bubbleSize = 75; // dip
    private float attractorSize = 40;
    public static final int REQUEST_TRANSFER = 10;

    // Screen wake lock for incoming call
    private WakeLock mScreenWakeLock;

    private BubblesView mBubbleView;
    private BubbleModel mBubbleModel;

    private Bitmap buttonCall;
    private Bitmap buttonMsg;
    private Bitmap buttonHold;
    private Bitmap buttonUnhold;
    private Bitmap buttonTransfer;
    private Bitmap buttonHangUp;

    private final int BTN_MSG_IDX = 0;
    private final int BTN_HOLD_IDX = 1;
    private final int BTN_TRANSFER_IDX = 2;
    private final int BTN_HUNGUP_IDX = 3;

    private BubbleModel.ActionGroup userActions;
    private BubbleModel.ActionGroup callActions;

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
        super.onCreate(savedBundle);

        Resources r = getResources();

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
                //if (b.isUser) {
                try {
                    if (b.isConference())
                        mCallbacks.getService().hangUpConference(b.getCallID());
                    else
                        mCallbacks.getService().hangUp(b.getCallID());

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
                /*}
                return false;*/
            }
        });

        setHasOptionsMenu(true);
        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "org.sflphone.onIncomingCall");
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
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public void onFragmentCreated() {

        }

        @Override
        public ISipService getService() {
            return null;
        }

        @Override
        public void terminateCall() {
        }

        @Override
        public Conference getDisplayedConference() {
            return null;
        }

        @Override
        public void updateDisplayedConference(Conference c) {
        }

        @Override
        public void startTimer() {
        }

        @Override
        public void slideChatScreen() {
        }

    };

    /**
     * The Activity calling this fragment has to implement this interface
     */
    public interface Callbacks {

        public void onFragmentCreated();

        public ISipService getService();

        public void startTimer();

        public void slideChatScreen();

        public void terminateCall();

        public Conference getDisplayedConference();

        public void updateDisplayedConference(Conference c);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        // rootView.requestDisallowInterceptTouchEvent(true);

        mCallbacks = (Callbacks) activity;
        // myself = SipCall.SipCallBuilder.buildMyselfCall(activity.getContentResolver(), "Me");

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
                mCallbacks.slideChatScreen();
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
        mCallbacks.updateDisplayedConference(updated);
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

                        mCallbacks.getService().attendedTransfer(transfer.getCallId(), c.getParticipants().get(0).getCallId());

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;

                case TransferDFragment.RESULT_TRANSFER_NUMBER:
                    String to = data.getStringExtra("to_number");
                    transfer = data.getParcelableExtra("transfer");
                    try {
                        mCallbacks.getService().transfer(transfer.getCallId(), to);
                        mCallbacks.getService().hangUp(transfer.getCallId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case Activity.RESULT_CANCELED:
                default:
                    synchronized (mBubbleModel) {
                        mBubbleModel.clear();
                    }
                    initNormalStateDisplay();
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

        mBubbleView = (BubblesView) rootView.findViewById(R.id.main_view);
        //mBubbleView.setFragment(this);
        mBubbleView.setModel(mBubbleModel);
        mBubbleView.getHolder().addCallback(this);

        mCallStatusTxt = (TextView) rootView.findViewById(R.id.call_status_txt);

        mSecuritySwitch = (ViewSwitcher) rootView.findViewById(R.id.security_switcher);
        mToggleSpeakers = (ToggleButton) rootView.findViewById(R.id.speaker_toggle);

        mToggleSpeakers.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    mCallbacks.getService().toggleSpeakerPhone(isChecked);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });

        synchronized (mBubbleModel) {
            mBubbleModel.setSize(mBubbleView.getWidth(), mBubbleView.getHeight() - mToggleSpeakers.getHeight(), bubbleSize);
        }

        rootView.findViewById(R.id.dialpad_btn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                InputMethodManager lManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                lManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });

        return rootView;
    }

    public Conference getConference() {
        return mCallbacks.getDisplayedConference();
    }

    private void initNormalStateDisplay() {
        Log.i(TAG, "Start normal display");
        synchronized (mBubbleModel) {
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
                float dX = FloatMath.cos(angle_part * i + angle_shift) * radiusCalls;
                float dY = FloatMath.sin(angle_part * i + angle_shift) * radiusCalls;
                getBubbleFor(partee, (int) (c.x + dX), (int) (c.y + dY));
            }
        }
        mBubbleModel.curState = BubbleModel.State.Incall;
        updateSecurityDisplay();
    }

    private void updateSecurityDisplay() {

        //First we check if at least one participant use a security layer.
        if (!getConference().useSecureLayer())
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
                                mCallbacks.getService().confirmSAS(secured.getCallId());
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

    private void initIncomingCallDisplay() {
        Log.i(TAG, "Start incoming display");
        if (getConference().getParticipants().get(0).getAccount().isAutoanswerEnabled()) {
            try {
                mCallbacks.getService().accept(getConference().getParticipants().get(0).getCallId());
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            getBubbleFor(getConference().getParticipants().get(0), mBubbleModel.getWidth() / 2, 2 * mBubbleModel.getHeight() / 3);
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
            mBubbleModel.curState = BubbleModel.State.Incoming;
        }
    }

    private void initOutGoingCallDisplay() {
        Log.i(TAG, "Start outgoing display");
        synchronized (mBubbleModel) {
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
        mBubbleModel.curState = BubbleModel.State.Outgoing;
    }

    /**
     * Retrieves or create a bubble for a given contact. If the bubble exists, it is moved to the new location.
     *
     * @param call The call associated to a contact
     * @param x    Initial or new x position.
     * @param y    Initial or new y position.
     * @return Bubble corresponding to the contact.
     */
    private Bubble getBubbleFor(SipCall call, float x, float y) {
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
/*
        try {
            ((BubbleUser) contact_bubble).setMute(mCallbacks.getService().isCaptureMuted());
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e1) {
            e1.printStackTrace();
        }*/
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
        if (getConference().getParticipants().size() == 1) {
            if (getConference().getParticipants().get(0).isIncoming() && getConference().getParticipants().get(0).isRinging()) {
                initIncomingCallDisplay();
            } else if (getConference().getParticipants().get(0).isRinging()) {
                initOutGoingCallDisplay();
            } else if (getConference().getParticipants().get(0).isOngoing()) {
                initNormalStateDisplay();
            }
        } else if (getConference().getParticipants().size() > 1) {
            initNormalStateDisplay();
        }
    }

    public void makeTransfer(BubbleContact contact) {
        FragmentManager fm = getFragmentManager();
        editName = TransferDFragment.newInstance();
        Bundle b = new Bundle();
        try {
            b.putParcelableArrayList("calls", (ArrayList<Conference>) mCallbacks.getService().getConcurrentCalls());
            b.putParcelable("call_selected", contact.associated_call);
            editName.setArguments(b);
            editName.setTargetFragment(this, REQUEST_TRANSFER);
            editName.show(fm, "");
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

    }

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

    public void updateTime() {
        if (getConference() != null) {
            long duration = System.currentTimeMillis() - getConference().getParticipants().get(0).getTimestampStart_();
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
                    mCallbacks.getService().playDtmf(toSend);
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
