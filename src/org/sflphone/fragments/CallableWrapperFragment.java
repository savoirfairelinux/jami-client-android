package org.sflphone.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import org.sflphone.interfaces.CallInterface;
import org.sflphone.service.CallManagerCallBack;

/**
 * Created by lisional on 10/02/14.
 */
public abstract class CallableWrapperFragment extends Fragment implements CallInterface {


    private CallReceiver mReceiver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReceiver = new CallReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        intentFilter.addAction(CallManagerCallBack.INCOMING_TEXT);
        intentFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
        intentFilter.addAction(CallManagerCallBack.CONF_CREATED);
        intentFilter.addAction(CallManagerCallBack.CONF_REMOVED);
        intentFilter.addAction(CallManagerCallBack.CONF_CHANGED);
        intentFilter.addAction(CallManagerCallBack.RECORD_STATE_CHANGED);
        intentFilter.addAction(CallManagerCallBack.ZRTP_OFF);
        intentFilter.addAction(CallManagerCallBack.ZRTP_ON);
        intentFilter.addAction(CallManagerCallBack.DISPLAY_SAS);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }


    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void callStateChanged(String callID, String state) {

    }

    @Override
    public void incomingText(String ID, String from, String msg) {

    }

    @Override
    public void confCreated(String id) {

    }

    @Override
    public void confRemoved(String id) {

    }

    @Override
    public void confChanged(String id, String state) {

    }

    @Override
    public void recordingChanged(String callID, String filename) {

    }

    @Override
    public void secureZrtpOn(String id) {

    }

    @Override
    public void secureZrtpOff(String id) {

    }

    @Override
    public void displaySAS(String callID, String SAS, boolean verified) {

    }

    public class CallReceiver extends BroadcastReceiver {

        private final String TAG = CallReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(CallManagerCallBack.INCOMING_TEXT)) {
                incomingText(intent.getStringExtra("CallID"), intent.getStringExtra("From"), intent.getStringExtra("Msg"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CALL_STATE_CHANGED)) {
                callStateChanged(intent.getStringExtra("CallID"), intent.getStringExtra("State"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CREATED)) {
                confCreated(intent.getStringExtra("confID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_REMOVED)) {
                confRemoved(intent.getStringExtra("confID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CHANGED)) {
                confChanged(intent.getStringExtra("confID"), intent.getStringExtra("state"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.RECORD_STATE_CHANGED)) {
                recordingChanged(intent.getStringExtra("callID"), intent.getStringExtra("file"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_OFF)) {
                secureZrtpOff(intent.getStringExtra("callID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_ON)) {
                secureZrtpOn(intent.getStringExtra("callID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.DISPLAY_SAS)) {
                displaySAS(intent.getStringExtra("callID"), intent.getStringExtra("SAS"), intent.getBooleanExtra("verified", false));
            } else {
                Log.e(TAG, "Unknown action: " + intent.getAction());
            }

        }

    }


}