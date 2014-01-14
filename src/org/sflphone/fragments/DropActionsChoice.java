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

package org.sflphone.fragments;

import org.sflphone.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class DropActionsChoice extends DialogFragment {

    ListAdapter mAdapter;
    private Bundle args;
    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

    /**
     * Create a new instance of CallActionsDFragment
     */
    public static DropActionsChoice newInstance() {
        DropActionsChoice f = new DropActionsChoice();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pick a style based on the num.
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ListView rootView = new ListView(getActivity());

        args = getArguments();
        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.drop_actions));

        // ListView list = (ListView) rootView.findViewById(R.id.concurrent_calls);
        rootView.setAdapter(mAdapter);
        rootView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                Intent in = new Intent();

                in.putExtra("transfer", args.getParcelable("call_initial"));
                in.putExtra("target", args.getParcelable("call_targeted"));

                switch (pos) {
                case 0: // Transfer
                    getTargetFragment().onActivityResult(REQUEST_TRANSFER, 0, in);
                    break;
                case 1: // Conference
                    getTargetFragment().onActivityResult(REQUEST_CONF, 0, in);
                    break;
                }
                dismiss();

            }
        });

        final AlertDialog a = new AlertDialog.Builder(getActivity()).setView(rootView).setTitle("Choose Action")
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dismiss();
                    }
                }).create();

        return a;
    }

}
