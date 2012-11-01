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

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.savoirfairelinux.sflphone.client.AccountSelectionDialog;
import com.savoirfairelinux.sflphone.service.ISipService;

import java.util.ArrayList;

public class AccountSelectionButton extends Button
{
    private static final String TAG = "AccountSelectionButton";
    private ISipService mService;
    private Context mContext;

    public AccountSelectionButton(Context context) {
        super(context);
    }

    public AccountSelectionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccountSelectionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSipService(ISipService service, Context context) {
        mService = service;
        mContext = context;
        final AccountSelectionButton b = this;

        ArrayList<String> list = getAccountList();
        if(list.size() > 1) {
            list.remove("IP2IP");
            setText(list.get(0));
        } else {
            setText("IP2IP");
        }

        setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ArrayList<String> list = getAccountList();
                AccountSelectionDialog accountSelectionDialog = new AccountSelectionDialog(mContext, list, b);
                accountSelectionDialog.show();
            }
        });
    }

    public ArrayList<String> getAccountList() {
        ArrayList<String> list = null;

        try {
            list = (ArrayList<String>)mService.getAccountList();
        }
        catch (RemoteException e) {
            Log.e(TAG, "Remote exception", e);
        }

        return list;
    }
}
