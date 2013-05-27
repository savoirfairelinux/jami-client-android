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
        public ISipService getService() {
            // TODO Auto-generated method stub
            return null;
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
        public ISipService getService();

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

        view = (BubblesView) rootView.findViewById(R.id.main_view);
        view.setModel(model);

        Bundle b = getArguments();

        SipCall.CallInfo info = b.getParcelable("CallInfo");
        Log.i(TAG, "Starting fragment for call " + info.mCallID);
        mCall = new SipCall(info);

        String pendingAction = b.getString("action");
        if (pendingAction != null && pendingAction.contentEquals("call")) {
            callContact(info);
        } else if (pendingAction.equals(CallManagerCallBack.INCOMING_CALL)) {
            callIncoming();
        }

        return rootView;
    }

    private void callContact(SipCall.CallInfo infos) {
        // TODO off-thread image loading
        Bubble contact_bubble;
        if (infos.contact.getPhoto_id() > 0) {
            Bitmap photo = ContactPictureLoader.loadContactPhoto(getActivity().getContentResolver(), infos.contact.getId());
            contact_bubble = new Bubble(getActivity(), screenCenter.x, screenCenter.y, 150, photo);
        } else {
            contact_bubble = new Bubble(getActivity(), screenCenter.x, screenCenter.y, 150, R.drawable.ic_contact_picture);
        }

        model.attractors.clear();
        model.attractors.add(new Attractor(new PointF(metrics.widthPixels / 2, metrics.heightPixels * .8f), new Attractor.Callback() {
            @Override
            public void onBubbleSucked(Bubble b) {
                Log.w(TAG, "Bubble sucked ! ");
                onCallEnded();
            }
        }));

        contact_bubble.contact = infos.contact;
        model.listBubbles.add(contact_bubble);
        contacts.put(infos.contact, contact_bubble);
        
        try {
            mCallbacks.getService().placeCall(infos.mAccountID, infos.mCallID, infos.mPhone);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void callIncoming() {
        model.attractors.clear();
        model.attractors.add(new Attractor(new PointF(3 * metrics.widthPixels / 4, metrics.heightPixels / 4), new Attractor.Callback() {
            @Override
            public void onBubbleSucked(Bubble b) {
                onCallAccepted();
            }
        }));
        model.attractors.add(new Attractor(new PointF(metrics.widthPixels / 4, metrics.heightPixels / 4), new Attractor.Callback() {
            @Override
            public void onBubbleSucked(Bubble b) {
                onCallRejected();
            }
        }));

    }

    

    public void onCallAccepted() {

        mCall.notifyServiceAnswer(mCallbacks.getService());
    }

    public void onCallRejected() {
        if (mCall.notifyServiceHangup(mCallbacks.getService()))
            ;

    }

    public void onCallEnded() {
        if (mCall.notifyServiceHangup(mCallbacks.getService()))
            ;

    }

    public void onCallSuspended() {
        mCall.notifyServiceHold(mCallbacks.getService());
    }

    public void onCallResumed() {
        mCall.notifyServiceUnhold(mCallbacks.getService());
    }

    public void onCalltransfered(String to) {
        mCall.notifyServiceTransfer(mCallbacks.getService(), to);

    }

    public void onRecordCall() {
        mCall.notifyServiceRecord(mCallbacks.getService());

    }

    public void onSendMessage(String msg) {
        mCall.notifyServiceSendMsg(mCallbacks.getService(), msg);

    }

    public void changeCallState(int callState) {
        mCall.setCallState(callState);
    }



}
