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

package com.savoirfairelinux.sflphone.client.receiver;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;

import com.savoirfairelinux.sflphone.account.AccountManagementUI;
import com.savoirfairelinux.sflphone.service.ConfigurationManagerCallback;
import com.savoirfairelinux.sflphone.service.ISipService;

public class AccountListReceiver extends BroadcastReceiver 
{
    private static final long serialVersionUID = -9178386308804218835L;
    static final String TAG = "AccountList";
    private String currentAccountID = "";
    private ArrayList<String> mList = new ArrayList<String>();
    private ArrayList<AccountManagementUI> mUserInterfaceList = new ArrayList<AccountManagementUI>();
    private static ISipService mService = null;
    // private HashMap<String, AccountPreferenceScreen> mAccountList = new HashMap<String, AccountPreferenceScreen>();

    public static final String DEFAULT_ACCOUNT_ID = "IP2IP";

    public AccountListReceiver() {
    }

    public void setSipService(ISipService service) {
        mService = service;
        mList = getAccountListFromService();
    }
        

    public void addManagementUI(AccountManagementUI ui) {
        mUserInterfaceList.add(ui);
    }

    public void accountSelected(String accountID, AccountManagementUI userInterface) {
        if(!mUserInterfaceList.isEmpty()) {
            for(AccountManagementUI ui : mUserInterfaceList) {
                 ui.setSelectedAccount(accountID);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String signalName = intent.getStringExtra(ConfigurationManagerCallback.SIGNAL_NAME);
        Log.d(TAG, "Signal received: " + signalName);
        
        if(signalName.equals(ConfigurationManagerCallback.ACCOUNTS_CHANGED)) {
            processAccountsChangedSignal(intent);
        } else if(signalName.equals(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED)) {
            processAccountStateChanged(intent);
        }
    }

    private ArrayList<String> getAccountListFromService() {
        ArrayList<String> list = null;

        try {
            list = (ArrayList<String>)mService.getAccountList();
        }
        catch (RemoteException e) {
            Log.e(TAG, "Remote exception", e);
        }

        list.remove(DEFAULT_ACCOUNT_ID);

        return list;
    }


    private void processAccountsChangedSignal(Intent intent) {
        ArrayList<String> newList = getAccountListFromService();

        Log.i(TAG, "Process AccountsChanged signal in AccountList");

        newList.remove(DEFAULT_ACCOUNT_ID);

        if(!mUserInterfaceList.isEmpty()) {

            if(newList.size() > mList.size()) {
                for(AccountManagementUI ui : mUserInterfaceList) {
                    ui.accountAdded(newList);
                }
            }
        }

        mList = newList;
    }

    private void processAccountStateChanged(Intent intent) {
        if(!mUserInterfaceList.isEmpty()) {
        }
    }
}
