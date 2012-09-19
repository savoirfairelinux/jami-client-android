/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@gmail.com>
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
package com.savoirfairelinux.sflphone.client;

import android.util.Log;
import java.util.ArrayList;

public class SipCall
{
    final static String TAG = "SipCall";
    static ArrayList<SipCall> CallList = new ArrayList<SipCall>();
    CallContact mContact;

    public SipCall()
    {
        mContact = null;
        CallList.add(this);
    }

    public SipCall(CallContact contact)
    {
        mContact = contact;
        CallList.add(this);
    }

    protected void finalize() throws Throwable
    {
       CallList.remove(this);
    }

    public static SipCall getCallInstance(CallContact contact)
    {
        Log.i(TAG, "Get call instance for " + contact.getDisplayName()); 
        if(CallList.isEmpty())
            return new SipCall(contact);
       
        for(SipCall sipcall : CallList) {
            Log.i(TAG, "Searching...");

            if(sipcall.mContact.getDisplayName().equals(contact.getDisplayName())) {
                return sipcall;
            }
        }

        return new SipCall(contact);
    }

    public static int getNbCalls()
    {
        return CallList.size();
    }

    public void answer()
    {

    }

    public void hangup()
    {

    }
}
