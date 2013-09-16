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

package com.savoirfairelinux.sflphone.account;

import java.util.HashMap;

import android.util.Log;

import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.StringMap;

public class CallDetailsHandler {
    
       private static String TAG = CallDetailsHandler.class.getSimpleName();

    public static HashMap<String, String> convertSwigToNative(StringMap swigmap) {

        HashMap<String, String> entry = new HashMap<String, String>();

        Log.i(TAG, "CALL_TYPE: " + swigmap.get(ServiceConstants.call.CALL_TYPE));
        Log.i(TAG, "PEER_NUMBER: " + swigmap.get(ServiceConstants.call.PEER_NUMBER));
        Log.i(TAG, "DISPLAY_NAME: " + swigmap.get(ServiceConstants.call.DISPLAY_NAME));
        Log.i(TAG, "CALL_STATE: " + swigmap.get(ServiceConstants.call.CALL_STATE));
        Log.i(TAG, "CONF_ID" + swigmap.get(ServiceConstants.call.CONF_ID));
        Log.i(TAG, "TIMESTAMP_START: " + swigmap.get(ServiceConstants.call.TIMESTAMP_START));
        Log.i(TAG, "ACCOUNTID: " + swigmap.get(ServiceConstants.call.ACCOUNTID));
        
        entry.put(ServiceConstants.call.CALL_TYPE, swigmap.get(ServiceConstants.call.CALL_TYPE));
        entry.put(ServiceConstants.call.PEER_NUMBER, swigmap.get(ServiceConstants.call.PEER_NUMBER));
        entry.put(ServiceConstants.call.DISPLAY_NAME, swigmap.get(ServiceConstants.call.DISPLAY_NAME));
        entry.put(ServiceConstants.call.CALL_STATE, swigmap.get(ServiceConstants.call.CALL_STATE));
        entry.put(ServiceConstants.call.CONF_ID, swigmap.get(ServiceConstants.call.CONF_ID));
        entry.put(ServiceConstants.call.TIMESTAMP_START, swigmap.get(ServiceConstants.call.TIMESTAMP_START));
        entry.put(ServiceConstants.call.ACCOUNTID, swigmap.get(ServiceConstants.call.ACCOUNTID));

        return entry;
    }

}
