/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
 */

package cx.ring.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.OperationCanceledException;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;

public class AccountsLoader extends AsyncTaskLoader<ArrayList<Account>> {

    private static final String TAG = ContactsLoader.class.getSimpleName();
    IDRingService mService;

    public AccountsLoader(Context context, IDRingService service) {
        super(context);
        Log.d(TAG, "AccountsLoader constructor");
        mService = service;
    }

    private boolean checkCancel() {
        if (isLoadInBackgroundCanceled()) {
            Log.d(TAG, "AccountsLoader cancelled");
            throw new OperationCanceledException();
        }
        if (isAbandoned()) {
            Log.d(TAG, "AccountsLoader abandoned");
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<Account> loadInBackground() {
        Log.d(TAG, "AccountsLoader loadInBackground");
        ArrayList<Account> accounts = new ArrayList<>();
        if (checkCancel() || mService == null)
            return null;
        try {
            ArrayList<String> accountIDs = (ArrayList<String>) mService.getAccountList();
            Map<String, String> details;
            ArrayList<Map<String, String>> credentials;
            Map<String, String> state;
            for (String id : accountIDs) {
                if (checkCancel())
                    return null;
                details = (Map<String, String>) mService.getAccountDetails(id);
                state = (Map<String, String>) mService.getVolatileAccountDetails(id);
                credentials = (ArrayList<Map<String, String>>) mService.getCredentials(id);
                Account tmp = new Account(id, details, credentials, state);
                accounts.add(tmp);
            }
        } catch (RemoteException | NullPointerException e) {
            Log.e(TAG, e.toString());
        }
        if (checkCancel())
            return null;
        return accounts;
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
