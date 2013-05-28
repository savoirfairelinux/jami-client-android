/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
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

package com.savoirfairelinux.sflphone.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.AccountSelectionAdapter;
import com.savoirfairelinux.sflphone.loaders.AccountsLoader;
import com.savoirfairelinux.sflphone.model.Account;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.views.ClearableEditText;

public class DialingFragment extends Fragment implements LoaderCallbacks<ArrayList<Account>> {

    private static final String TAG = HistoryFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";
    private boolean isReady;
    private ISipService service;

    ClearableEditText textField;
    // private AccountSelectionSpinner mAccountSelectionSpinner;

    AccountSelectionAdapter mAdapter;
    private Callbacks mCallbacks = sDummyCallbacks;
    private Spinner spinnerAccounts;

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallDialed(String account, String to) {
        }

        @Override
        public ISipService getService() {
            // TODO Auto-generated method stub
            return null;
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
        void onCallDialed(String account, String to);

        public ISipService getService();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // mAdapter = new HistoryAdapter(getActivity(),new ArrayList<HashMap<String, String>>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_dialing, parent, false);
        
        spinnerAccounts = (Spinner) inflatedView.findViewById(R.id.account_selection);

        spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                // public void onClick(DialogInterface dialog, int which) {

                Log.i(TAG, "Selected Account: " + mAdapter.getItem(pos));
                if (null != view) {
                    ((RadioButton) view.findViewById(R.id.account_checked)).toggle();
                }
                mAdapter.setSelectedAccount(pos);
                // accountSelectedNotifyAccountList(mAdapter.getItem(pos));
                // setSelection(cursor.getPosition(),true);

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        textField = (ClearableEditText) inflatedView.findViewById(R.id.textField);
        ((ImageButton) inflatedView.findViewById(R.id.buttonCall)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Account account = mAdapter.getSelectedAccount();
                String to = textField.getText().toString();
                mCallbacks.onCallDialed(account.getAccountID(), to);
            }
        });

        ((Button) inflatedView.findViewById(R.id.alphabetic_keyboard)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textField.setInputType(EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                InputMethodManager lManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                lManager.showSoftInput(textField.getEdit_text(), 0);
            }
        });

        ((Button) inflatedView.findViewById(R.id.numeric_keyboard)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textField.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                InputMethodManager lManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                lManager.showSoftInput(textField.getEdit_text(), 0);
            }
        });

        isReady = true;
        if (mCallbacks.getService() != null) {

            onServiceSipBinded(mCallbacks.getService());
        }
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    public Account getSelectedAccount() {
        return mAdapter.getSelectedAccount();
    }

    /**
     * Called by activity to pass a reference to sipservice to Fragment.
     * 
     * @param isip
     */
    public void onServiceSipBinded(ISipService isip) {

        if (isReady) {
            service = isip;

            mAdapter = new AccountSelectionAdapter(getActivity(), service, new ArrayList<Account>());
            spinnerAccounts.setAdapter(mAdapter);
            getActivity().getLoaderManager().initLoader(555, null, this);
        }

    }

    @Override
    public Loader<ArrayList<Account>> onCreateLoader(int id, Bundle args) {
        AccountsLoader l = new AccountsLoader(getActivity(), service);
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Account>> loader, ArrayList<Account> results) {
        mAdapter.removeAll();
        mAdapter.addAll(results);

    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Account>> arg0) {
        // TODO Auto-generated method stub

    }

}
