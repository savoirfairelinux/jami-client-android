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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.account.AccountDetail;
import com.savoirfairelinux.sflphone.account.AccountDetailAdvanced;
import com.savoirfairelinux.sflphone.account.AccountDetailBasic;
import com.savoirfairelinux.sflphone.account.AccountDetailSrtp;
import com.savoirfairelinux.sflphone.account.AccountDetailTls;

public class AccountPreferenceActivity extends PreferenceActivity {
    private static final String TAG = "AccoutPreferenceActivity";

    public static final String KEY_MODE = "mode";

    public interface mode {
        static final int CREATION_MODE = 0;
        static final int EDITION_MODE = 1;
    }

    public interface result {
        static final int ACCOUNT_CREATED = Activity.RESULT_FIRST_USER + 0;
        static final int ACCOUNT_MODIFIED = Activity.RESULT_FIRST_USER + 1;
        static final int ACCOUNT_DELETED = Activity.RESULT_FIRST_USER + 2;
    }

    private AccountDetailBasic basicDetails = null;
    private AccountDetailAdvanced advancedDetails = null;
    private AccountDetailSrtp srtpDetails = null;
    private AccountDetailTls tlsDetails = null;
    private PreferenceManager mPreferenceManager;
    private String mAccountID;
    private ArrayList<String> requiredFields = null;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.account_creation_preferences);
        mPreferenceManager = getPreferenceManager();

        Bundle b = getIntent().getExtras();

        switch (b.getInt(KEY_MODE)) {
        case mode.CREATION_MODE:
            Log.i(TAG, "CREATION");
            initCreation();
            break;
        case mode.EDITION_MODE:
            Log.i(TAG, "ESDITION");
            initEdition();
            break;
        }

        requiredFields = new ArrayList<String>();
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS);
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME);
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME);
        requiredFields.add(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD);

    }

    private void initCreation() {
        basicDetails = new AccountDetailBasic();
        advancedDetails = new AccountDetailAdvanced();
        srtpDetails = new AccountDetailSrtp();
        tlsDetails = new AccountDetailTls();

        addPreferenceListener(basicDetails);
        addPreferenceListener(advancedDetails);
        addPreferenceListener(srtpDetails);
        addPreferenceListener(tlsDetails);

    }
    
    private void initEdition() {

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        Bundle b = getIntent().getExtras();

        switch (b.getInt(KEY_MODE)) {
        case mode.CREATION_MODE:
            Log.i(TAG, "CREATION");
            inflater.inflate(R.menu.account_creation, menu);
            break;
        case mode.EDITION_MODE:
            Log.i(TAG, "onCreateOptionsMenu: " + mAccountID);

            if (mAccountID.equals("IP2IP"))
                return true;

            inflater.inflate(R.menu.account_edition, menu);
            break;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        Bundle b = getIntent().getExtras();
        switch (b.getInt(KEY_MODE)) {
        case mode.CREATION_MODE:
            Log.i(TAG, "CREATION");
            AlertDialog dialog = createCancelDialog();
            dialog.show();
            break;
        case mode.EDITION_MODE:
            finish();
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case R.id.menuitem_delete:
            AlertDialog dialog = createDeleteDialog();
            dialog.show();
            break;
        case R.id.menuitem_create:
            processAccount(result.ACCOUNT_CREATED);
            break;
        case R.id.menuitem_edit:
            processAccount(result.ACCOUNT_MODIFIED);
            break;

        }

        return true;
    }

    private void processAccount(int resultCode) {
        AlertDialog dialog;
        ArrayList<String> missingValue = new ArrayList<String>();
        if (validateAccountCreation(missingValue)) {

            Bundle bundle = new Bundle();
            bundle.putString("AccountID", mAccountID);
            HashMap<String, String> accountDetails = new HashMap<String, String>();

            updateAccountDetails(accountDetails, basicDetails);
            updateAccountDetails(accountDetails, advancedDetails);
            updateAccountDetails(accountDetails, srtpDetails);
            updateAccountDetails(accountDetails, tlsDetails);

            bundle.putSerializable(AccountDetail.TAG, accountDetails);
            Intent resultIntent = new Intent();
            resultIntent.putExtras(bundle);

            setResult(resultCode, resultIntent);
            finish();
        } else {
            dialog = createCouldNotValidateDialog(missingValue);
            dialog.show();
        }

    }

    public boolean validateAccountCreation(ArrayList<String> missingValue) {
        boolean valid = true;

        for (String s : requiredFields) {
            EditTextPreference pref = (EditTextPreference) mPreferenceManager.findPreference(s);
            Log.i(TAG, "Looking for " + s);
            if (pref.getText().isEmpty()) {
                Log.i(TAG, "    INVALIDATED " + s + " " + pref.getText() + ";");
                valid = false;
                missingValue.add(pref.getTitle().toString());
            }
        }

        return valid;
    }

   
    private void updateAccountDetails(HashMap<String, String> accountDetails, AccountDetail det) {
        for (AccountDetail.PreferenceEntry p : det.getDetailValues()) {
            Preference pref = mPreferenceManager.findPreference(p.mKey);
            if (pref != null) {
                if (p.isTwoState) {
                    CheckBoxPreference boxPref = (CheckBoxPreference) pref;
                    accountDetails.put(p.mKey, boxPref.isChecked() ? "true" : "false");
                } else {
                    EditTextPreference textPref = (EditTextPreference) pref;
                    accountDetails.put(p.mKey, textPref.getText());
                }
            }
        }
    }

    Preference.OnPreferenceChangeListener changeBasicPreferenceListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            basicDetails.setDetailString(preference.getOrder(), ((CharSequence) newValue).toString());
            return true;
        }
    };
    
    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = mPreferenceManager.findPreference(p.mKey);
            if (pref != null) {
                if (!p.isTwoState) {
                    ((EditTextPreference) pref).setText(p.mValue);
                    pref.setSummary(p.mValue);
                }
            }
        }
    }

    private void addPreferenceListener(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = mPreferenceManager.findPreference(p.mKey);
            if (pref != null) {
                if (!p.isTwoState) {
                    pref.setOnPreferenceChangeListener(changeBasicPreferenceListener);
                }
            }
        }
    }

    
    
    
    
    /******************************************
     * 
     * AlertDialogs
     * 
     ******************************************/
    
    private AlertDialog createCouldNotValidateDialog(ArrayList<String> missingValue) {
        String message = "The following parameters are missing:";

        for (String s : missingValue)
            message += "\n    - " + s;

        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage(message).setTitle("Missing Parameters").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* Nothing to be done */
            }
        });

        AlertDialog alertDialog = builder.create();
        return alertDialog;
    }
    
    private AlertDialog createCancelDialog() {
        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage("All parameters will be lost").setTitle("Account Creation").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Activity activity = ((Dialog) dialog).getOwnerActivity();
                activity.finish();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* Terminate with no action */
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOwnerActivity(ownerActivity);

        return alertDialog;
    }
    
    private AlertDialog createDeleteDialog() {
        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage("Do you realy want to delete this account").setTitle("Delete Account")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Bundle bundle = new Bundle();
                        bundle.putString("AccountID", mAccountID);

                        Intent resultIntent = new Intent();
                        resultIntent.putExtras(bundle);

                        Activity activity = ((Dialog) dialog).getOwnerActivity();
                        activity.setResult(result.ACCOUNT_DELETED, resultIntent);
                        activity.finish();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Terminate with no action */
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOwnerActivity(ownerActivity);

        return alertDialog;
    }

}
