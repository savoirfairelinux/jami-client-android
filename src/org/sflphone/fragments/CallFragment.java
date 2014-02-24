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

package org.sflphone.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Log;
import android.view.*;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import org.sflphone.R;
import org.sflphone.interfaces.CallInterface;
import org.sflphone.model.*;
import org.sflphone.service.ISipService;

import java.util.ArrayList;
import java.util.Locale;

public class CallFragment extends CallableWrapperFragment implements CallInterface, Callback {

    static final String TAG = "CallFragment";

    float BUBBLE_SIZE = 75;
    static final float ATTRACTOR_SIZE = 40;
    public static final int REQUEST_TRANSFER = 10;

    // Screen wake lock for incoming call
    private WakeLock mScreenWakeLock;

    private BubblesView mBubbleView;
    private BubbleModel mBubbleModel;

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
        Log.e(TAG, "BUBBLE_SIZE " + BUBBLE_SIZE);

        mBubbleModel = new BubbleModel(getResources().getDisplayMetrics().density);
        BUBBLE_SIZE = getResources().getDimension(R.dimen.bubble_size);

        setHasOptionsMenu(true);
        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "org.sflphone.onIncomingCall");
        mScreenWakeLock.setReferenceCounted(false);

        Log.d(TAG, "Acquire wake up lock");
        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
        }
    }

    private void initializeWiFiListener() {
        Log.i(TAG, "executing initializeWiFiListener");

        String connectivity_context = Context.WIFI_SERVICE;
        wifiManager = (WifiManager) getActivity().getSystemService(connectivity_context);

        if (!wifiManager.isWifiEnabled()) {
            if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING) {
                wifiManager.setWifiEnabled(true);
            }
        }

        getActivity().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {

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
    }

    @Override
    public void secureZrtpOff(Conference updated, String id) {
        Log.i(TAG, "secureZrtpOff");
        mCallbacks.updateDisplayedConference(updated);
    }

    @Override
    public void displaySAS(Conference updated, final String securedCallID) {
        Log.i(TAG, "displaySAS");
        mCallbacks.updateDisplayedConference(updated);
        SecureSipCall display = (SecureSipCall) getConference().getCallById(securedCallID);
        enableZRTP(display);
    }

    @Override
    public void zrtpNegotiationFailed(Conference c, String securedCallID) {
        mCallbacks.updateDisplayedConference(c);
        SecureSipCall display = (SecureSipCall) getConference().getCallById(securedCallID);
        display.setZrtpNotSupported(true);
    }

    @Override
    public void zrtpNotSupported(Conference c, String securedCallID) {
        mCallbacks.updateDisplayedConference(c);
        SecureSipCall display = (SecureSipCall) getConference().getCallById(securedCallID);
        display.setZrtpNotSupported(true);
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
                    mBubbleModel.clear();
                    initNormalStateDisplay();
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

        mBubbleView = (BubblesView) rootView.findViewById(R.id.main_view);
        mBubbleView.setFragment(this);
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

        mCallbacks.startTimer();

        getBubbleForUser(getConference(), mBubbleModel.width / 2, mBubbleModel.height / 2);

        int angle_part = 360 / getConference().getParticipants().size();
        double dX, dY;
        int radiusCalls = (int) (mBubbleModel.width / 2 - BUBBLE_SIZE);
        for (int i = 0; i < getConference().getParticipants().size(); ++i) {

            SipCall partee = getConference().getParticipants().get(i);
            if (partee == null) {
                continue;
            }
            dX = Math.cos(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            dY = Math.sin(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            getBubbleFor(partee, (int) (mBubbleModel.width / 2 + dX), (int) (mBubbleModel.height / 2 + dY));
            if (partee instanceof SecureSipCall)
                enableZRTP((SecureSipCall) partee);
        }
        mBubbleModel.clearAttractors();
    }

    private void enableZRTP(final SecureSipCall secured) {
        Log.i(TAG, "enable ZRTP");
        if (secured.isInitialized()) {
            Log.i(TAG, "Call initialized ");
            if (secured.needSASConfirmation()) {
                Log.i(TAG, "needSASConfirmation");
                final Button sas = (Button) mSecuritySwitch.findViewById(R.id.confirm_sas);
                sas.setText("Confirm SAS: " + secured.getSAS());
                sas.setOnClickListener(new OnClickListener() {
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
                mSecuritySwitch.setVisibility(View.VISIBLE);
            } else if (secured.supportZRTP()) {
                Log.i(TAG, "supportZRTP");
                showLock(R.drawable.green_lock);
            } else {
                showLock(R.drawable.red_lock);
            }
        }
    }

    private void showLock(int resId) {
        ImageView lock = (ImageView) mSecuritySwitch.findViewById(R.id.lock_image);
        lock.setImageDrawable(getResources().getDrawable(resId));
        mSecuritySwitch.showNext();
        mSecuritySwitch.setVisibility(View.VISIBLE);
    }

    private void initIncomingCallDisplay() {
        Log.i(TAG, "Start incoming display");

        int radiusCalls = (int) (mBubbleModel.width / 2 - BUBBLE_SIZE);
        getBubbleForUser(getConference(), mBubbleModel.width / 2, mBubbleModel.height / 2 + radiusCalls);
        getBubbleFor(getConference().getParticipants().get(0), mBubbleModel.width / 2, mBubbleModel.height / 2 - radiusCalls);
        Bitmap call_icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_call);
        mBubbleModel.clearAttractors();
        mBubbleModel.addAttractor(new Attractor(new PointF(mBubbleModel.width / 2, mBubbleModel.height / 2), ATTRACTOR_SIZE, new Attractor.Callback() {
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
        }, call_icon));

        if (getConference().getParticipants().get(0).getAccount().isAutoanswerEnabled()) {
            try {
                mCallbacks.getService().accept(getConference().getParticipants().get(0).getCallId());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void initOutGoingCallDisplay() {
        Log.i(TAG, "Start outgoing display");

        getBubbleForUser(getConference(), mBubbleModel.width / 2, mBubbleModel.height / 2);

        // TODO off-thread image loading
        int angle_part = 360 / getConference().getParticipants().size();
        double dX, dY;
        int radiusCalls = (int) (mBubbleModel.width / 2 - BUBBLE_SIZE);
        for (int i = 0; i < getConference().getParticipants().size(); ++i) {
            dX = Math.cos(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            dY = Math.sin(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            getBubbleFor(getConference().getParticipants().get(i), (int) (mBubbleModel.width / 2 + dX), (int) (mBubbleModel.height / 2 + dY));
        }

        mBubbleModel.clearAttractors();
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
            contact_bubble.attractor.set(x, y);
            return contact_bubble;
        }

        contact_bubble = new BubbleContact(getActivity(), call, x, y, BUBBLE_SIZE);

        mBubbleModel.addBubble(contact_bubble);
        return contact_bubble;
    }

    private Bubble getBubbleForUser(Conference conf, float x, float y) {
        Bubble contact_bubble = mBubbleModel.getUser();
        if (contact_bubble != null) {
            contact_bubble.attractor.set(x, y);
            ((BubbleUser) contact_bubble).setConference(conf);

            return contact_bubble;
        }

        contact_bubble = new BubbleUser(getActivity(), CallContact.ContactBuilder.buildUserContact(getActivity().getContentResolver()), conf, x, y,
                BUBBLE_SIZE * 1.3f);

        try {
            ((BubbleUser) contact_bubble).setMute(mCallbacks.getService().isCaptureMuted());
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e1) {
            e1.printStackTrace();
        }
        mBubbleModel.addBubble(contact_bubble);
        return contact_bubble;
    }

    public boolean draggingBubble() {
        return mBubbleView == null ? false : mBubbleView.isDraggingBubble();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

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
                    toSend.toUpperCase(Locale.getDefault());
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
