/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
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

package com.savoirfairelinux.sflphone.fragments;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Attractor;
import com.savoirfairelinux.sflphone.model.Bubble;
import com.savoirfairelinux.sflphone.model.BubbleModel;
import com.savoirfairelinux.sflphone.model.BubblesView;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class CallFragment extends Fragment implements Callback, SensorEventListener {

    static final String TAG = "CallFragment";

    float BUBBLE_SIZE = 75;
    static final float ATTRACTOR_SIZE = 40;

    public static final int REQUEST_TRANSFER = 10;

    private Conference conf;

    private TextView callStatusTxt;
    private BubblesView view;
    private BubbleModel model;

    private Callbacks mCallbacks = sDummyCallbacks;

    private SipCall myself;

    boolean accepted = false;
    private Bitmap call_icon;

    private SensorManager mSensorManager;

    private Sensor mSensor;

    @Override
    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        Bundle b = getArguments();
        conf = new Conference((Conference) b.getParcelable("conference"));
        model = new BubbleModel(getResources().getDisplayMetrics().density);
        BUBBLE_SIZE = getResources().getDimension(R.dimen.bubble_size);
        Log.e(TAG, "BUBBLE_SIZE " + BUBBLE_SIZE);
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onSendMessage(SipCall call, String msg) {
        }

        @Override
        public void callContact(SipCall call) {
        }

        @Override
        public void onCallAccepted(SipCall call) {
        }

        @Override
        public void onCallRejected(SipCall call) {
        }

        @Override
        public void onCallEnded(SipCall call) {
        }

        @Override
        public void onCallSuspended(SipCall call) {
        }

        @Override
        public void onCallResumed(SipCall call) {
        }

        @Override
        public void onCalltransfered(SipCall call, String to) {
        }

        @Override
        public void onRecordCall(SipCall call) {
        }

        @Override
        public ISipService getService() {
            return null;
        }

        @Override
        public void replaceCurrentCallDisplayed() {
        }

        @Override
        public void startTimer() {
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {

        public ISipService getService();

        public void callContact(SipCall call);

        public void onCallAccepted(SipCall call);

        public void onCallRejected(SipCall call);

        public void onCallEnded(SipCall call);

        public void onCallSuspended(SipCall call);

        public void onCallResumed(SipCall call);

        public void onCalltransfered(SipCall call, String to);

        public void onRecordCall(SipCall call);

        public void onSendMessage(SipCall call, String msg);

        public void replaceCurrentCallDisplayed();

        public void startTimer();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        // rootView.requestDisallowInterceptTouchEvent(true);

        mCallbacks = (Callbacks) activity;
        myself = SipCall.SipCallBuilder.buildMyselfCall(activity.getContentResolver(), "Me");

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SipCall transfer = null;
        if (requestCode == REQUEST_TRANSFER) {
            switch (resultCode) {
            case TransferDFragment.RESULT_TRANSFER_CONF:
                Conference c = data.getParcelableExtra("target");
                transfer = data.getParcelableExtra("transfer");
                try {

                    mCallbacks.getService().attendedTransfer(transfer.getCallId(), c.getParticipants().get(0).getCallId());

                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // Toast.makeText(getActivity(), "Transfer complete", Toast.LENGTH_LONG).show();
                break;

            case TransferDFragment.RESULT_TRANSFER_NUMBER:
                String to = data.getStringExtra("to_number");
                transfer = data.getParcelableExtra("transfer");
                try {
                    // Toast.makeText(getActivity(), "Transferring " + transfer.getContact().getmDisplayName() + " to " + to,
                    // Toast.LENGTH_SHORT).show();
                    mCallbacks.getService().transfer(transfer.getCallId(), to);
                    mCallbacks.getService().hangUp(transfer.getCallId());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            case Activity.RESULT_CANCELED:
            default:
                model.clear();
                initNormalStateDisplay();
                break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

        view = (BubblesView) rootView.findViewById(R.id.main_view);
        view.setFragment(this);
        view.setModel(model);
        view.getHolder().addCallback(this);

        callStatusTxt = (TextView) rootView.findViewById(R.id.call_status_txt);
        call_icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_call);

        ((ImageButton) rootView.findViewById(R.id.dialpad_btn)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                InputMethodManager lManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                lManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });

        return rootView;
    }

    private void initNormalStateDisplay() {
        Log.i(TAG, "Start normal display");

        mCallbacks.startTimer();

        getBubbleFor(myself, model.width / 2, model.height / 2);

        int angle_part = 360 / conf.getParticipants().size();
        double dX = 0;
        double dY = 0;
        int radiusCalls = (int) (model.width / 2 - BUBBLE_SIZE);
        for (int i = 0; i < conf.getParticipants().size(); ++i) {

            if (conf.getParticipants().get(i) == null) {
                Log.i(TAG, i + " null ");
                continue;
            }
            dX = Math.cos(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            dY = Math.sin(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            getBubbleFor(conf.getParticipants().get(i), (int) (model.width / 2 + dX), (int) (model.height / 2 + dY));
        }

        model.clearAttractors();
    }

    private void initIncomingCallDisplay() {
        Log.i(TAG, "Start incoming display");

        mCallbacks.startTimer();

        int radiusCalls = (int) (model.width / 2 - BUBBLE_SIZE);
        getBubbleFor(myself, model.width / 2, model.height / 2 + radiusCalls);
        getBubbleFor(conf.getParticipants().get(0), model.width / 2, model.height / 2 - radiusCalls);

        model.clearAttractors();
        model.addAttractor(new Attractor(new PointF(model.width / 2, model.height / 2), ATTRACTOR_SIZE, new Attractor.Callback() {
            @Override
            public boolean onBubbleSucked(Bubble b) {

                if (!accepted) {
                    mCallbacks.onCallAccepted(conf.getParticipants().get(0));
                    accepted = true;
                }
                return false;
            }
        }, call_icon));
    }

    private void initOutGoingCallDisplay() {
        Log.i(TAG, "Start outgoing display");

        mCallbacks.startTimer();

        getBubbleFor(myself, model.width / 2, model.height / 2);

        // TODO off-thread image loading
        int angle_part = 360 / conf.getParticipants().size();
        double dX = 0;
        double dY = 0;
        int radiusCalls = (int) (model.width / 2 - BUBBLE_SIZE);
        for (int i = 0; i < conf.getParticipants().size(); ++i) {
            dX = Math.cos(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            dY = Math.sin(Math.toRadians(angle_part * i - 90)) * radiusCalls;
            getBubbleFor(conf.getParticipants().get(i), (int) (model.width / 2 + dX), (int) (model.height / 2 + dY));
        }

        model.clearAttractors();
    }

    /**
     * Retrieves or create a bubble for a given contact. If the bubble exists, it is moved to the new location.
     * 
     * @param call
     *            The call associated to a contact
     * @param x
     *            Initial or new x position.
     * @param y
     *            Initial or new y position.
     * @return Bubble corresponding to the contact.
     */
    private Bubble getBubbleFor(SipCall call, float x, float y) {
        Bubble contact_bubble = model.getBubble(call);
        if (contact_bubble != null) {
            contact_bubble.attractor.set(x, y);
            return contact_bubble;
        }

        contact_bubble = new Bubble(getActivity(), call, x, y, BUBBLE_SIZE);

        model.addBubble(contact_bubble);
        return contact_bubble;
    }

    /**
     * Should be called when a bubble is removed from the model
     */
    void bubbleRemoved(Bubble b) {
        if (b.associated_call == null) {
            return;
        }
    }

    public void changeCallState(String callID, String newState) {
        Log.w(TAG, "Call :" + callID + " " + newState);
        if (newState.contentEquals("FAILURE")) {
            try {
                mCallbacks.getService().hangUp(callID);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < conf.getParticipants().size(); ++i) {
            if (callID.equals(conf.getParticipants().get(i).getCallId())) {
                if (newState.contentEquals("HUNGUP")) {
                    model.removeBubble(conf.getParticipants().get(i));
                    conf.getParticipants().remove(i);
                } else {
                    conf.getParticipants().get(i).setCallState(newState);
                }
            }
        }

        if (conf.isOnGoing())
            initNormalStateDisplay();

        if (conf.getParticipants().size() == 0) {
            mCallbacks.replaceCurrentCallDisplayed();
        }
    }

    public boolean draggingBubble() {
        return view == null ? false : view.isDraggingBubble();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (conf.getParticipants().size() == 1) {
            if (conf.getParticipants().get(0).isIncoming() && conf.getParticipants().get(0).isRinging()) {
                initIncomingCallDisplay();
            } else {
                if (conf.getParticipants().get(0).isRinging()) {
                    initOutGoingCallDisplay();
                }
                try {
                    if (conf.getParticipants().get(0).isOutGoing()
                            && mCallbacks.getService().getCall(conf.getParticipants().get(0).getCallId()) == null) {
                        mCallbacks.getService().placeCall(conf.getParticipants().get(0));
                        initOutGoingCallDisplay();
                    } else if (conf.getParticipants().get(0).isOutGoing() && conf.getParticipants().get(0).isRinging()) {
                        initOutGoingCallDisplay();
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            if (conf.getParticipants().get(0).isOngoing()) {
                initNormalStateDisplay();
            }
        } else if (conf.getParticipants().size() > 1) {
            initNormalStateDisplay();
        }
    }

    public void makeTransfer(Bubble contact) {
        FragmentManager fm = getFragmentManager();
        TransferDFragment editName = new TransferDFragment();

        Bundle b = new Bundle();
        b.putParcelableArrayList("calls", new ArrayList<Conference>());
        b.putParcelable("call_selected", contact.associated_call);
        editName.setArguments(b);
        editName.setTargetFragment(this, REQUEST_TRANSFER);
        editName.show(fm, "");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // check that soft input is hidden
        InputMethodManager lManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        lManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public BubblesView getBubbleView() {
        return view;
    }

    public void updateTime() {
        long duration = System.currentTimeMillis() / 1000 - this.conf.getParticipants().get(0).getTimestamp_start();
        callStatusTxt.setText(String.format("%d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] == 0) {
            WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
            params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            params.screenBrightness = 0.004f;
            getActivity().getWindow().setAttributes(params);

        } else {
            WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            params.screenBrightness = 1.0f;
            getActivity().getWindow().setAttributes(params);
        }
    }

    public Conference getConference() {
        return conf;
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        try {
            String toSend = Character.toString(event.getDisplayLabel());
            toSend.toUpperCase(Locale.getDefault());
            Log.d(TAG, "toSend " + toSend);
            mCallbacks.getService().playDtmf(toSend);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
