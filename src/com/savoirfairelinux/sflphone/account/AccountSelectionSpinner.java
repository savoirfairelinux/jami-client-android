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
package com.savoirfairelinux.sflphone.account;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class AccountSelectionSpinner extends Spinner implements AccountManagementUI {
    private static final String TAG = "AccountSelectionButton";
    private Context mContext;
    private ArrayList<String> mList = new ArrayList<String>();
    private AccountListReceiver mAccountList = null;
    ArrayAdapter mListAdapter;

    public AccountSelectionSpinner(Context context) {
        super(context);
        mContext = context;

    }

    public AccountSelectionSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;


    }

    public AccountSelectionSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mListAdapter = new ArrayAdapter(mContext, android.R.layout.simple_expandable_list_item_1, mList.toArray());

        setOnItemSelectedListener(onClick);
        setAdapter(mListAdapter);
    }

    private AdapterView.OnItemSelectedListener onClick = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
            // public void onClick(DialogInterface dialog, int which) {

            Log.i(TAG, "Selected Account: " + ((TextView) view).getText());
            // mButton.setText(((TextView)view).getText());
            accountSelectedNotifyAccountList(((TextView) view).getText().toString());
            // setSelection(cursor.getPosition(),true);

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub

        }

    };


    public void setAccountList(AccountListReceiver accountList) {
        Log.i(TAG,"setAccountList");
        mAccountList = accountList;
        
    }

    public void accountSelectedNotifyAccountList(String accountID) {
        Log.i(TAG, "->accountSelectedNotifyAccountList");
        if (mAccountList != null) {
            mAccountList.accountSelected(accountID, this);
        }
    }

    public void setSelectedAccount(String accountID) {
        Log.i(TAG,"Account Selected");
        // setText(accountID);
    }

    public void accountAdded(ArrayList<String> newList) {
        mListAdapter = new ArrayAdapter(mContext, android.R.layout.simple_expandable_list_item_1, newList.toArray());

        setOnItemSelectedListener(onClick);
        setAdapter(mListAdapter);
        // Log.i(TAG, "Account added");
        // mList = newList;
        //
        // if(newList.size() == 1) {
        // setText(newList.get(0));
        // }
    }

    public void accountRemoved() {
        Log.i(TAG,"Account Removed");
    }

    public void accountUpdated() {
        Log.i(TAG,"Account Updated");
    }
}
