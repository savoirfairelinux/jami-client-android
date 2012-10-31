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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import com.savoirfairelinux.sflphone.utils.AccountSelectionButton;

public class AccountSelectionDialog extends AlertDialog
{
    private static final String TAG = "AccountSelectionDialog";
    Context mContext;
    ListView mListView;
    ArrayAdapter mListAdapter;
    ArrayList<String> mItems;
    AccountSelectionButton mButton;

    public AccountSelectionDialog(Context context, ArrayList<String> items, AccountSelectionButton b)
    {
        super(context);
        mContext = context;
        mItems = items;
        mButton = b;
    } 

    private AdapterView.OnItemClickListener onClick = new AdapterView.OnItemClickListener() 
    {
        // public void onClick(DialogInterface dialog, int which) {
        public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
            Log.i(TAG, "Selected Account: " + ((TextView)view).getText());
            mButton.setText(((TextView)view).getText());
        }
    };

    public void onCreate(Bundle savedInstanceState)
    {
        mListView = new ListView(mContext);

        mListAdapter = new ArrayAdapter(mContext, android.R.layout.simple_expandable_list_item_1, mItems.toArray());

        mListView.setOnItemClickListener(onClick);
        mListView.setAdapter(mListAdapter);
        setContentView(mListView);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Account Selection");

        return builder.create();
    }
}
