package org.sflphone.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import org.sflphone.interfaces.AccountsInterface;
import org.sflphone.service.ConfigurationManagerCallback;

/**
 * Created by lisional on 11/02/14.
 */
public class AccountWrapperFragment extends Fragment implements AccountsInterface {


    private AccountsReceiver mReceiver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReceiver = new AccountsReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void accountsChanged() {

    }

    @Override
    public void accountStateChanged(String accoundID, String state, int code) {

    }


    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    public class AccountsReceiver extends BroadcastReceiver {

        private final String TAG = AccountsReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED)) {
                Log.i(TAG, "Received" + intent.getAction());
                accountStateChanged(intent.getStringExtra("Account"), intent.getStringExtra("state"), intent.getIntExtra("code", 0));
            } else if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNTS_CHANGED)) {
                Log.i(TAG, "Received" + intent.getAction());
                accountsChanged();

            }

        }
    }


}