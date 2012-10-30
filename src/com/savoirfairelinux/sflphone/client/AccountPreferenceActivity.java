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
import android.content.ServiceConnection;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.EditTextPreference;
import android.preference.CheckBoxPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.utils.AccountDetail;
import com.savoirfairelinux.sflphone.utils.AccountDetailsHandler;
import com.savoirfairelinux.sflphone.utils.AccountDetailBasic;
import com.savoirfairelinux.sflphone.utils.AccountDetailAdvanced;
import com.savoirfairelinux.sflphone.utils.AccountDetailSrtp;
import com.savoirfairelinux.sflphone.utils.AccountDetailTls;

import java.util.HashMap;
import java.util.ArrayList;

public class AccountPreferenceActivity extends PreferenceActivity
{
    private static final String TAG = "AccoutPreferenceActivity";
    public static final int ACCOUNT_MODIFIED = Activity.RESULT_FIRST_USER + 0;
    public static final int ACCOUNT_NOT_MODIFIED = Activity.RESULT_FIRST_USER + 1;
    public static final int ACCOUNT_DELETED = Activity.RESULT_FIRST_USER + 2;

    private AccountDetailBasic basicDetails = null;
    private AccountDetailAdvanced advancedDetails = null;
    private AccountDetailSrtp srtpDetails = null;
    private AccountDetailTls tlsDetails = null;
    private PreferenceManager mPreferenceManager;
    private HashMap<String, String> mPreferenceMap;
    private MenuItem deleteAccountAction = null;
    private String mAccountID;
           
    Preference.OnPreferenceChangeListener changeBasicPreferenceListener = 
                                            new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(getString(R.string.account_current_value_label)+(CharSequence)newValue);
            
            // String preferenceKey = basicDetailKeys.get(preference.getOrder()).mKey; 
            // accountPreference.preferenceMap.put(preferenceKey, ((CharSequence)newValue).toString());
            basicDetails.setDetailString(preference.getOrder(), ((CharSequence)newValue).toString());
            // if(preferenceKey == AccountDetailBasic.CONFIG_ACCOUNT_ALIAS)
            //     accountPreference.mScreen.setTitle(((CharSequence)newValue.toString()));
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeNewAccountTwoStateListener = 
                                            new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return true;
        }
    };

    private void init() {

        Bundle b = getIntent().getExtras();
        mAccountID = b.getString("AccountID");
        ArrayList<String> basicPreferenceList = b.getStringArrayList(AccountDetailBasic.BUNDLE_TAG);
        ArrayList<String> advancedPreferenceList = b.getStringArrayList(AccountDetailAdvanced.BUNDLE_TAG);
        ArrayList<String> srtpPreferenceList = b.getStringArrayList(AccountDetailSrtp.BUNDLE_TAG);
        ArrayList<String> tlsPreferenceList = b.getStringArrayList(AccountDetailTls.BUNDLE_TAG);

        basicDetails = new AccountDetailBasic(basicPreferenceList); 
        advancedDetails = new AccountDetailAdvanced(advancedPreferenceList);
        srtpDetails = new AccountDetailSrtp(srtpPreferenceList);
        tlsDetails = new AccountDetailTls(tlsPreferenceList);

        setPreferenceDetails(basicDetails);
        setPreferenceDetails(advancedDetails);
        setPreferenceDetails(srtpDetails);
        setPreferenceDetails(tlsDetails);

        addPreferenceListener(basicDetails);
        addPreferenceListener(advancedDetails);
        addPreferenceListener(srtpDetails);
        addPreferenceListener(tlsDetails);
    }

    private void setPreferenceDetails(AccountDetail details) {
        for(AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = mPreferenceManager.findPreference(p.mKey);
            if(pref != null) {
                if(!p.isTwoState) {
                    ((EditTextPreference)pref).setText(p.mValue);
                    pref.setSummary(getString(R.string.account_current_value_label) + p.mValue);
                }
            }
        }
    }

    private void addPreferenceListener(AccountDetail details) {
        for(AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = mPreferenceManager.findPreference(p.mKey);
            if(pref != null) {
                if(!p.isTwoState) {
                    pref.setOnPreferenceChangeListener(changeBasicPreferenceListener);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.account_creation_preferences);
        mPreferenceManager = getPreferenceManager();

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        deleteAccountAction = menu.add("Delete Account");
        deleteAccountAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
       super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", mAccountID);
        bundle.putStringArrayList(AccountDetailBasic.BUNDLE_TAG, basicDetails.getValuesOnly()); 
        bundle.putStringArrayList(AccountDetailAdvanced.BUNDLE_TAG, advancedDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailSrtp.BUNDLE_TAG, srtpDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailTls.BUNDLE_TAG, tlsDetails.getValuesOnly());

        Intent resultIntent = new Intent();
        resultIntent.putExtras(bundle);

        setResult(ACCOUNT_MODIFIED, resultIntent);
        finish(); 
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog dialog = createAlertDialog();
        dialog.show();

        return true;
    }

    private AlertDialog createAlertDialog()
    {
        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage("Do you realy want to delete this account").setTitle("Delete Account")
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int whichButton) {
                       Bundle bundle = new Bundle();
                       bundle.putString("AccountID", mAccountID);

                       Intent resultIntent = new Intent();
                       resultIntent.putExtras(bundle);

                       Activity activity = ((Dialog)dialog).getOwnerActivity();
                       activity.setResult(ACCOUNT_DELETED, resultIntent);
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

    private void updateAccountDetails(HashMap<String, String> accountDetails, AccountDetail det) {
        for(AccountDetail.PreferenceEntry p : det.getDetailValues()) {
            Preference pref = mPreferenceManager.findPreference(p.mKey);
            if(pref != null) {
                if(p.isTwoState) {
                    CheckBoxPreference boxPref = (CheckBoxPreference) pref;
                    accountDetails.put(p.mKey, boxPref.isChecked() ? "true" : "false");
                }
                else {
                    EditTextPreference textPref = (EditTextPreference)pref;
                    accountDetails.put(p.mKey, textPref.getText());
                }
            }
        }
    }

    private void updateAccountDetails() {
        HashMap<String, String> accountDetails = new HashMap<String, String>();

        updateAccountDetails(accountDetails, basicDetails);
        updateAccountDetails(accountDetails, advancedDetails);
        updateAccountDetails(accountDetails, srtpDetails);
        updateAccountDetails(accountDetails, tlsDetails);
    }
}

