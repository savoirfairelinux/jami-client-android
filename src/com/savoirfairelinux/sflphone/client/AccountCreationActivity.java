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

package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.EditTextPreference;
import android.preference.CheckBoxPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.utils.AccountDetail;
import com.savoirfairelinux.sflphone.utils.AccountDetailsHandler;
import com.savoirfairelinux.sflphone.utils.AccountDetailBasic;
import com.savoirfairelinux.sflphone.utils.AccountDetailAdvanced;
import com.savoirfairelinux.sflphone.utils.AccountDetailSrtp;
import com.savoirfairelinux.sflphone.utils.AccountDetailTls;

//import java.util.ArrayList; 
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

public class AccountCreationActivity extends PreferenceActivity
{
    static final String TAG = "SFLPhonePreferenceActivity";
    private ISipService service;
    private boolean mBound = false;
    private PreferenceManager mPreferenceManager = null;
    private AccountDetailBasic basicDetails;
    private AccountDetailAdvanced advancedDetails;
    private AccountDetailSrtp srtpDetails;
    private AccountDetailTls tlsDetails;
    private MenuItem createAccountAction = null;

    Preference.OnPreferenceChangeListener changeNewAccountPreferenceListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(getString(R.string.account_current_value_label) + (CharSequence)newValue);
            AccountCreationActivity activity = (AccountCreationActivity)preference.getContext();
            activity.validateAccountCreation();
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeNewAccountTwoStateListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return true;
        } 
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            mBound = true;
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    public AccountCreationActivity()
    {
        basicDetails = new AccountDetailBasic();
        advancedDetails = new AccountDetailAdvanced();
        srtpDetails = new AccountDetailSrtp();
        tlsDetails = new AccountDetailTls();
    }

    private AlertDialog createAlertDialog()
    {
        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage("All parameters will be lost").setTitle("Account Creation")
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Activity activity = ((Dialog)dialog).getOwnerActivity();
                        activity.finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Terminate with no action */
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOwnerActivity(ownerActivity);

        return alertDialog;
    }

    public boolean validateAccountCreation()
    {
        createAccountAction.setEnabled(true);
        return true; 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.account_creation_preferences);
        mPreferenceManager = getPreferenceManager();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        createAccountAction = menu.add("Create Account");
        createAccountAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        createAccountAction.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.isEnabled()) {
            createNewAccount();
            finish();
        }

        return true;
    }

    private void addPreferenceListener(AccountDetail details) {
        for(AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = mPreferenceManager.findPreference(p.mKey);
            if(pref != null) {
                Log.i(TAG, "FOUND " + p.mKey);
                if(!p.isTwoState) {
                    pref.setOnPreferenceChangeListener(changeNewAccountPreferenceListener);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        addPreferenceListener(basicDetails);
        addPreferenceListener(advancedDetails);
        addPreferenceListener(srtpDetails);
        addPreferenceListener(tlsDetails);
 
        if(!mBound) {
            Log.i(TAG, "onStart: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mBound) {
            Log.i(TAG, "onStop: Unbinding service...");
            unbindService(mConnection);
            mBound = false;
        } 
    }

    @Override
    public void onBackPressed() {

        AlertDialog dialog = createAlertDialog();
        dialog.show();

        // super.onBackPressed();
    }

    private void createNewAccount() {

        HashMap<String, String> accountDetails = new HashMap<String, String>();

        for(String s : basicDetails.getDetailKeys()) {
            EditTextPreference pref = (EditTextPreference) mPreferenceManager.findPreference(s);
            if(pref != null) {
                Log.i(TAG, "FOUND " + s + " " + pref.getText());
                accountDetails.put(s, pref.getText());
            }
        }

        for(String s : advancedDetails.getDetailKeys()) {
            EditTextPreference pref = (EditTextPreference) mPreferenceManager.findPreference(s);
            if(pref != null) {
                Log.i(TAG, "FOUND " + s + " " + pref.getText());
                accountDetails.put(s, pref.getText());
            }
        }

        for(String s : srtpDetails.getDetailKeys()) {
            EditTextPreference pref = (EditTextPreference) mPreferenceManager.findPreference(s);
            if(pref != null) {
                Log.i(TAG, "FOUND " + s + " " + pref.getText());
                accountDetails.put(s, pref.getText());
            }
        }

        for(String s : tlsDetails.getDetailKeys()) {
            EditTextPreference pref = (EditTextPreference) mPreferenceManager.findPreference(s);
            if(pref != null) {
                Log.i(TAG, "FOUND " + s + " " + pref.getText());
                accountDetails.put(s, pref.getText());
            }
        }

        try {
            Log.i(TAG, "ADD ACCOUNT");
            service.addAccount(accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }
}
