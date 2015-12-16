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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
import cx.ring.service.LocalService;

import java.util.HashMap;

public abstract class CallableWrapperFragment extends Fragment implements CallInterface
{
    private final CallReceiver mReceiver = new CallReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallManagerCallBack.RECORD_STATE_CHANGED);
        intentFilter.addAction(CallManagerCallBack.ZRTP_OFF);
        intentFilter.addAction(CallManagerCallBack.ZRTP_ON);
        intentFilter.addAction(CallManagerCallBack.DISPLAY_SAS);
        intentFilter.addAction(CallManagerCallBack.ZRTP_NEGOTIATION_FAILED);
        intentFilter.addAction(CallManagerCallBack.ZRTP_NOT_SUPPORTED);
        intentFilter.addAction(CallManagerCallBack.RTCP_REPORT_RECEIVED);

        intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);

        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void confUpdate() {
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
            if(intent.getAction().contentEquals(LocalService.ACTION_CONF_UPDATE)) {
                confUpdate();
            } else if (intent.getAction().contentEquals(CallManagerCallBack.RECORD_STATE_CHANGED)) {
                recordingChanged((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("call"), intent.getStringExtra("file"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_OFF)) {
                secureZrtpOff((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("call"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_ON)) {
                secureZrtpOn((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("call"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.DISPLAY_SAS)) {
                displaySAS((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("call"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_NEGOTIATION_FAILED)) {
                zrtpNegotiationFailed((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("call"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.ZRTP_NOT_SUPPORTED)) {
                zrtpNotSupported((Conference) intent.getParcelableExtra("conference"), intent.getStringExtra("call"));
            } else if (intent.getAction().contentEquals(CallManagerCallBack.RTCP_REPORT_RECEIVED)) {
                rtcpReportReceived(null, null); // FIXME
            } else {
                Log.e(TAG, "Unknown action: " + intent.getAction());
            }

        }

    }
}
