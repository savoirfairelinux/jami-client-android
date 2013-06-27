package com.savoirfairelinux.sflphone.loaders;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.savoirfairelinux.sflphone.account.AccountDetailAdvanced;
import com.savoirfairelinux.sflphone.account.AccountDetailBasic;
import com.savoirfairelinux.sflphone.model.Account;
import com.savoirfairelinux.sflphone.service.ISipService;

public class AccountsLoader extends AsyncTaskLoader<ArrayList<Account>> {

    private static final String TAG = AccountsLoader.class.getSimpleName();

    ISipService service;

    public AccountsLoader(Context context, ISipService ref) {
        super(context);
        service = ref;
    }

    @Override
    public ArrayList<Account> loadInBackground() {
        
        
        ArrayList<Account> result = new ArrayList<Account>();
        Account.AccountBuilder builder = Account.AccountBuilder.getInstance();

        ArrayList<String> accountIDs;
        HashMap<String, String> details;
        try {
            accountIDs = (ArrayList<String>) service.getAccountList();
            for (String id : accountIDs) {
                
                if(id.contentEquals("IP2IP")){
                    continue;
                }
                details = (HashMap<String, String>) service.getAccountDetails(id);

                builder.setAccountID(id).setAlias(details.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS))
                        .setHost(details.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME))
                        .setRegisteredState(details.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS));
                
                try {
                    result.add(builder.build());
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }

            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        } catch (NullPointerException e1){
            Log.e(TAG, e1.toString());
        }

        return result;
    }

}
