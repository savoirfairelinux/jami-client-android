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

package org.sflphone.loaders;

import java.util.ArrayList;
import java.util.HashMap;

import org.sflphone.model.Account;
import org.sflphone.service.ISipService;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class AccountsLoader extends AsyncTaskLoader<Bundle> {

    private static final String TAG = AccountsLoader.class.getSimpleName();
    public static final String ACCOUNTS = "accounts";
    public static final String ACCOUNT_IP2IP = "IP2IP";
    ISipService service;

    public AccountsLoader(Context context, ISipService ref) {
        super(context);
        service = ref;
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    @Override
    public Bundle loadInBackground() {

        ArrayList<Account> accounts = new ArrayList<Account>();
        Account IP2IP = null;

        try {
            ArrayList<String> accountIDs = (ArrayList<String>) service.getAccountList();
            HashMap<String, String> details;
            ArrayList<HashMap<String, String>> credentials;
            for (String id : accountIDs) {

                if (id.contentEquals(ACCOUNT_IP2IP)) {
                    details = (HashMap<String, String>) service.getAccountDetails(id);
                    IP2IP = new Account(ACCOUNT_IP2IP, details, new ArrayList<HashMap<String, String>>()); // Empty credentials
                    continue;
                }
                details = (HashMap<String, String>) service.getAccountDetails(id);
                credentials = (ArrayList<HashMap<String, String>>) service.getCredentials(id);
                Account tmp = new Account(id, details, credentials);

                accounts.add(tmp);

                Log.i(TAG, "account:" + tmp.getAlias() + " " + tmp.isEnabled());

            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        } catch (NullPointerException e1) {
            Log.e(TAG, e1.toString());
        }

        Bundle result = new Bundle();
        result.putParcelableArrayList(ACCOUNTS, accounts);
        result.putParcelable(ACCOUNT_IP2IP, IP2IP);
        return result;
    }

}
