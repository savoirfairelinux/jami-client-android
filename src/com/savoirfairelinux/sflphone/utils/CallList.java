/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

package com.savoirfairelinux.sflphone.utils;

import com.savoirfairelinux.sflphone.client.SipCall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

public class CallList extends BroadcastReceiver
{
    static final String TAG = "CallList";
    ArrayList<SipCall> mList = new ArrayList<SipCall>();

    private enum Signals {
        NEW_CALL_CREATED,
        INCOMING_CALL,
        INCOMING_MESSAGE,
        CALL_STATE_CHANGED
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String signalName = intent.getStringExtra("signal-name");
        Log.d(TAG, "Signal received: " + signalName);

        Signals signalReceived = Signals.valueOf(signalName.toUpperCase());
        switch(signalReceived) {
            case NEW_CALL_CREATED:
                break;
            case INCOMING_CALL:
                break;
            case INCOMING_MESSAGE:
                break;
            case CALL_STATE_CHANGED:
                break;
        }
    }
}
