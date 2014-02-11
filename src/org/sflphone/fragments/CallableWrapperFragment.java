package org.sflphone.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        super.onResume();IntentFilter intentFilter = new IntentFilter();
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
    public void callStateChanged(Intent callState) {

    }

    @Override
    public void incomingText(Intent msg) {

    }

    @Override
    public void confCreated(Intent intent) {

    }

    @Override
    public void confRemoved(Intent intent) {

    }

    @Override
    public void confChanged(Intent intent) {

    }

    @Override
    public void recordingChanged(Intent intent) {

    }

    @Override
    public void secureZrtpOn(Intent intent) {

    }

    @Override
    public void secureZrtpOff(Intent intent) {

    }

    @Override
    public void displaySAS(Intent intent) {

    }

    public class CallReceiver extends BroadcastReceiver {

        private final String TAG = CallReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(CallManagerCallBack.INCOMING_TEXT)) {
                incomingText(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CALL_STATE_CHANGED)) {
                callStateChanged(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CREATED)) {
                confCreated(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_REMOVED)) {
                confRemoved(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CHANGED)) {
                confChanged(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.RECORD_STATE_CHANGED)) {
                recordingChanged(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_OFF)) {
                secureZrtpOff(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_ON)) {
                secureZrtpOn(intent);
            } else if (intent.getAction().contentEquals(CallManagerCallBack.DISPLAY_SAS)) {
                displaySAS(intent);
            } else {
                Log.e(TAG, "Unknown action: " + intent.getAction());
            }

        }

    }


}