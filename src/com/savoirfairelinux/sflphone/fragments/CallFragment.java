/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
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

import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.ContactPictureLoader;
import com.savoirfairelinux.sflphone.model.Attractor;
import com.savoirfairelinux.sflphone.model.Bubble;
import com.savoirfairelinux.sflphone.model.BubbleModel;
import com.savoirfairelinux.sflphone.model.BubblesView;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ISipService;

public class CallFragment extends Fragment {
    static final String TAG = "CallFragment";

    private SipCall mCall;

    private BubblesView view;
    private BubbleModel model;
    private PointF screenCenter;
    private DisplayMetrics metrics;

    private Callbacks mCallbacks = sDummyCallbacks;

    private HashMap<CallContact, Bubble> contacts = new HashMap<CallContact, Bubble>();

    private TextView contact_name_txt;

    @Override
    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        model = new BubbleModel(getResources().getDisplayMetrics().density);
        metrics = getResources().getDisplayMetrics();
        screenCenter = new PointF(metrics.widthPixels / 2, metrics.heightPixels / 3);
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
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onCallRejected(SipCall call) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onCallEnded(SipCall call) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onCallSuspended(SipCall call) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onCallResumed(SipCall call) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onCalltransfered(SipCall call, String to) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onRecordCall(SipCall call) {
            // TODO Auto-generated method stub
            
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {

        public void callContact(SipCall call);

        public void onCallAccepted(SipCall call);

        public void onCallRejected(SipCall call);

        public void onCallEnded(SipCall call);

        public void onCallSuspended(SipCall call);

        public void onCallResumed(SipCall call);

        public void onCalltransfered(SipCall call, String to);

        public void onRecordCall(SipCall call);

        public void onSendMessage(SipCall call, String msg);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

        view = (BubblesView) rootView.findViewById(R.id.main_view);
        view.setModel(model);

        Bundle b = getArguments();

        mCall = b.getParcelable("CallInfo");
        Log.i(TAG, "Starting fragment for call " + mCall.getCallId());

        mCall.printCallInfo();
        String pendingAction = b.getString("action");
        if (pendingAction != null && pendingAction.contentEquals("call")) {
            callContact(mCall);
        } else if (pendingAction.contentEquals(CallManagerCallBack.INCOMING_CALL)) {
            callIncoming();
        }

        return rootView;
    }

    private void callContact(SipCall infos) {
        // TODO off-thread image loading
        Bubble contact_bubble;
        if (infos.getContacts().get(0).getPhoto_id() > 0) {
            Bitmap photo = ContactPictureLoader.loadContactPhoto(getActivity().getContentResolver(), infos.getContacts().get(0).getId());
            contact_bubble = new Bubble(getActivity(), screenCenter.x, screenCenter.y, 150, photo);
        } else {
            contact_bubble = new Bubble(getActivity(), screenCenter.x, screenCenter.y, 150, R.drawable.ic_contact_picture);
        }

        model.attractors.clear();
        model.attractors.add(new Attractor(new PointF(metrics.widthPixels / 2, metrics.heightPixels * .8f), new Attractor.Callback() {
            @Override
            public void onBubbleSucked(Bubble b) {
                Log.w(TAG, "Bubble sucked ! ");
                mCallbacks.onCallEnded(mCall);
            }
        }));

        contact_bubble.contact = infos.getContacts().get(0);
        model.listBubbles.add(contact_bubble);
        contacts.put(infos.getContacts().get(0), contact_bubble);

        mCallbacks.callContact(infos);

    }

    private void callIncoming() {
        model.attractors.clear();
        model.attractors.add(new Attractor(new PointF(3 * metrics.widthPixels / 4, metrics.heightPixels / 4), new Attractor.Callback() {
            @Override
            public void onBubbleSucked(Bubble b) {
                mCallbacks.onCallAccepted(mCall);
            }
        }));
        model.attractors.add(new Attractor(new PointF(metrics.widthPixels / 4, metrics.heightPixels / 4), new Attractor.Callback() {
            @Override
            public void onBubbleSucked(Bubble b) {
                mCallbacks.onCallRejected(mCall);
            }
        }));

    }

    public void changeCallState(int callState) {

        mCall.setCallState(callState);
    }

}
