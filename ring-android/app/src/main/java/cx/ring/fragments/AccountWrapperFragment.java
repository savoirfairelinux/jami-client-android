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

package cx.ring.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import cx.ring.interfaces.AccountsInterface;
import cx.ring.service.ConfigurationManagerCallback;

public abstract class AccountWrapperFragment extends Fragment implements AccountsInterface
{
    static final String TAG = "AccountWrapperFragment";

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
        Log.i(TAG, "accountsChanged");
    }

    @Override
    public void accountStateChanged(String accoundID, String state, int code) {
        Log.i(TAG, "accountStateChanged" + accoundID + " " + state + " " + code);
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
                Log.i(TAG, "Received " + intent.getAction() + " " + intent.getStringExtra("Account") + " " + intent.getStringExtra("state") + " " + intent.getIntExtra("code", 0));
                accountStateChanged(intent.getStringExtra("Account"), intent.getStringExtra("state"), intent.getIntExtra("code", 0));
            } else if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNTS_CHANGED)) {
                Log.i(TAG, "Received" + intent.getAction());
                accountsChanged();
            }

        }
    }


}