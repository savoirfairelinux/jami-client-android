package org.sflphone.loaders;

import java.util.ArrayList;
import java.util.HashMap;

import org.sflphone.model.Account;
import org.sflphone.service.ISipService;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
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
            for (String id : accountIDs) {

                if (id.contentEquals(ACCOUNT_IP2IP)) {
                    details = (HashMap<String, String>) service.getAccountDetails(id);
                    IP2IP = new Account(ACCOUNT_IP2IP, details);
                    continue;
                }
                details = (HashMap<String, String>) service.getAccountDetails(id);
                Account tmp = new Account(id, details);

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
