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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import cx.ring.interfaces.CallInterface;
import cx.ring.model.Conference;
import cx.ring.service.CallManagerCallBack;

import java.util.HashMap;

public abstract class CallableWrapperFragment extends Fragment implements CallInterface {

    private final CallReceiver mReceiver = new CallReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        intentFilter.addAction(CallManagerCallBack.ZRTP_NEGOTIATION_FAILED);
        intentFilter.addAction(CallManagerCallBack.ZRTP_NOT_SUPPORTED);
        intentFilter.addAction(CallManagerCallBack.RTCP_REPORT_RECEIVED);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void callStateChanged(Conference c, String callID, String state) {

    }

    @Override
    public void incomingText(Conference c, String ID, String from, String msg) {

    }

    @Override
    public void confCreated(Conference c, String id) {

    }

    @Override
    public void confRemoved(Conference c, String id) {

    }

    @Override
    public void confChanged(Conference c, String id, String state) {

    }

    @Override
    public void recordingChanged(Conference c, String callID, String filename) {

    }

    @Override
    public void secureZrtpOn(Conference c, String id) {

    }

    @Override
    public void secureZrtpOff(Conference c, String id) {

    }

    @Override
    public void displaySAS(Conference c, String securedCallID) {

    }

    @Override
    public void zrtpNegotiationFailed(Conference c, String securedCallID) {

    }

    @Override
    public void zrtpNotSupported(Conference c, String securedCallID) {

    }

    @Override
    public void rtcpReportReceived(Conference c, HashMap<String, Integer> stats) {

    }


    public class CallReceiver extends BroadcastReceiver {

        private final String TAG = CallReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(CallManagerCallBack.INCOMING_TEXT)) {
                incomingText((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("CallID"), intent.getStringExtra("From"), intent.getStringExtra("Msg"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CALL_STATE_CHANGED)) {
                callStateChanged((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("CallID"), intent.getStringExtra("State"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CREATED)) {
                confCreated((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("confID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_REMOVED)) {
                confRemoved((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("confID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CHANGED)) {
                confChanged((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("confID"), intent.getStringExtra("state"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.RECORD_STATE_CHANGED)) {
                recordingChanged((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("callID"), intent.getStringExtra("file"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_OFF)) {
                secureZrtpOff((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("callID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_ON)) {
                secureZrtpOn((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("callID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.DISPLAY_SAS)) {
                displaySAS((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("callID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_NEGOTIATION_FAILED)) {
                zrtpNegotiationFailed((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("callID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_NOT_SUPPORTED)) {
                zrtpNotSupported((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("callID"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.RTCP_REPORT_RECEIVED)) {
                rtcpReportReceived(null, null); // FIXME
            } else {
                Log.e(TAG, "Unknown action: " + intent.getAction());
            }

        }

    }
}