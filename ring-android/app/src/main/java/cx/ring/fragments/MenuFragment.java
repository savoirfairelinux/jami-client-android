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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import cx.ring.R;
import cx.ring.adapters.AccountSelectionAdapter;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.AccountWizard;
import cx.ring.model.account.Account;
import cx.ring.model.CallContact;
import cx.ring.service.LocalService;

import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = MenuFragment.class.getSimpleName();

    AccountSelectionAdapter mAccountAdapter;
    private Spinner spinnerAccounts;
    private ImageButton shareBtn;
    private Button newAccountBtn;

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;

    public Account retrieveAccountById(String accountID) {
        Account toReturn;
        toReturn = mAccountAdapter.getAccount(accountID);

        if(toReturn == null || !toReturn.isRegistered())
            return getSelectedAccount();

        return toReturn;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (LocalService.Callbacks) activity;
        updateAllAccounts();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = LocalService.DUMMY_CALLBACKS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(LocalService.ACTION_ACCOUNT_UPDATE)) {
                updateAllAccounts();
            }
        }
    };

    @Override
    public void onResume() {
        Log.i(TAG, "Resuming");
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalService.ACTION_ACCOUNT_UPDATE);
        getActivity().registerReceiver(mReceiver, intentFilter);
        updateAllAccounts();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_menu_header, parent, false);

        newAccountBtn = (Button) inflatedView.findViewById(R.id.addaccount_btn);
        newAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent().setClass(getActivity(), AccountWizard.class);
                startActivityForResult(intent, AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
            }
        });

        shareBtn = (ImageButton) inflatedView.findViewById(R.id.share_btn);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Account acc = mAccountAdapter.getSelectedAccount();
                String share_uri;
                if (acc.isRing()) {
                    share_uri = acc.getBasicDetails().getUsername();
                } else {
                    share_uri = acc.getBasicDetails().getUsername() + "@" + acc.getBasicDetails().getHostname();
                }

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "Contact me using the address ring://" + share_uri + " on the Ring distributed communication platform: http://ring.cx";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Contact me on Ring !");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        });

        spinnerAccounts = (Spinner) inflatedView.findViewById(R.id.account_selection);
        mAccountAdapter = new AccountSelectionAdapter(getActivity(), new ArrayList<Account>());
        spinnerAccounts.setAdapter(mAccountAdapter);
        spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                Log.w(TAG, "onItemSelected -> setSelectedAccount" + pos);
                mAccountAdapter.setSelectedAccount(pos);
                //view.findViewById(R.id.account_selected).setVisibility(View.GONE);
                try {
                    mCallbacks.getRemoteService().setAccountOrder(mAccountAdapter.getAccountOrder());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                Log.w(TAG, "onNothingSelected -1");
                mAccountAdapter.setSelectedAccount(-1);
            }
        });

        CallContact user = CallContact.buildUserContact(getActivity());
        new ContactPictureTask(getActivity(), (ImageView) inflatedView.findViewById(R.id.user_photo), user).run();

        ((TextView) inflatedView.findViewById(R.id.user_name)).setText(user.getDisplayName());

        updateAllAccounts();

        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public Account getSelectedAccount() {
        Log.w(TAG, "getSelectedAccount " + mAccountAdapter.getSelectedAccount().getAccountID());

        return mAccountAdapter.getSelectedAccount();
    }

    public void updateAllAccounts() {
        /*if (getActivity() != null)
            getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);*/
        if (mAccountAdapter != null && mCallbacks.getService() != null) {
            List<Account> accs = mCallbacks.getService().getAccounts();
            if (accs.isEmpty()) {
                newAccountBtn.setVisibility(View.VISIBLE);
                shareBtn.setVisibility(View.GONE);
                spinnerAccounts.setVisibility(View.GONE);
            } else {
                newAccountBtn.setVisibility(View.GONE);
                shareBtn.setVisibility(View.VISIBLE);
                spinnerAccounts.setVisibility(View.VISIBLE);
                mAccountAdapter.replaceAll(accs);
            }
        }
    }

}
