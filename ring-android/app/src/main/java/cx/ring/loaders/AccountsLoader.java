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

package cx.ring.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;

public class AccountsLoader extends AsyncTaskLoader<Bundle> {

    private static final String TAG = AccountsLoader.class.getSimpleName();
    public static final String ACCOUNTS = "accounts";
    public static final String ACCOUNT_IP2IP = "IP2IP";
    IDRingService service;
    Bundle mData;

    public AccountsLoader(Context context, IDRingService ref) {
        super(context);
        service = ref;
    }

    /****************************************************/
    /** (1) A task that performs the asynchronous load **/
    /****************************************************/

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    @Override
    public Bundle loadInBackground() {

        ArrayList<Account> accounts = new ArrayList<>();
        Account IP2IP = null;

        try {
            ArrayList<String> accountIDs = (ArrayList<String>) service.getAccountList();
            Map<String, String> details;
            ArrayList<Map<String, String>> credentials;
            Map<String, String> state;
            for (String id : accountIDs) {

                details = (Map<String, String>) service.getAccountDetails(id);
                state = (Map<String, String>) service.getVolatileAccountDetails(id);
                if (id.contentEquals(ACCOUNT_IP2IP)) {
                    IP2IP = new Account(ACCOUNT_IP2IP, details, new ArrayList<Map<String, String>>(), state); // Empty credentials
                    //accounts.add(IP2IP);
                    continue;
                }

                credentials = (ArrayList<Map<String, String>>) service.getCredentials(id);
                /*for (Map.Entry<String, String> entry : state.entrySet()) {
                    Log.i(TAG, "state:" + entry.getKey() + " -> " + entry.getValue());
                }*/
                Account tmp = new Account(id, details, credentials, state);

                accounts.add(tmp);

               // Log.i(TAG, "account:" + tmp.getAlias() + " " + tmp.isEnabled());

            }
        } catch (RemoteException | NullPointerException e) {
            Log.e(TAG, e.toString());
        }

        Bundle result = new Bundle();
        result.putParcelableArrayList(ACCOUNTS, accounts);
        result.putParcelable(ACCOUNT_IP2IP, IP2IP);
        return result;
    }
}
