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
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import cx.ring.R;
import cx.ring.adapters.AccountSelectionAdapter;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.loaders.AccountsLoader;
import cx.ring.loaders.LoaderConstants;
import cx.ring.model.account.Account;
import cx.ring.model.CallContact;
import cx.ring.service.ISipService;

import java.util.ArrayList;

public class MenuFragment extends AccountWrapperFragment implements LoaderManager.LoaderCallbacks<Bundle> {

    @SuppressWarnings("unused")
    private static final String TAG = MenuFragment.class.getSimpleName();

    AccountSelectionAdapter mAccountAdapter;
    private Spinner spinnerAccounts;
    private Callbacks mCallbacks = sDummyCallbacks;

    private ListView sections;

    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            return null;
        }

        @Override
        public void onSectionSelected(int pos) {

        }
    };

    public Account retrieveAccountById(String accountID) {
        Account toReturn;
        toReturn = mAccountAdapter.getAccount(accountID);

        if(toReturn == null || !toReturn.isRegistered())
            return getSelectedAccount();

        return toReturn;
    }

    public interface Callbacks {

        public ISipService getService();

        public void onSectionSelected(int pos);

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
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume() {
        super.onResume();

        Log.i(TAG, "Resuming");
        getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
    }



    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_menu, parent, false);

        ArrayAdapter<String> paramAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
                R.array.menu_items_param));
        sections = (ListView) inflatedView.findViewById(R.id.listView);
        sections.setAdapter(paramAdapter);
        backToHome();
        sections.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View selected, int pos, long arg3) {
                mCallbacks.onSectionSelected(pos);
            }
        });

        spinnerAccounts = (Spinner) inflatedView.findViewById(R.id.account_selection);
        mAccountAdapter = new AccountSelectionAdapter(getActivity(), new ArrayList<Account>());
        spinnerAccounts.setAdapter(mAccountAdapter);
        spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                mAccountAdapter.setSelectedAccount(pos);
                view.findViewById(R.id.account_selected).setVisibility(View.GONE);
                try {
                    mCallbacks.getService().setAccountOrder(mAccountAdapter.getAccountOrder());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                mAccountAdapter.setSelectedAccount(-1);
            }
        });

        CallContact user = CallContact.ContactBuilder.buildUserContact(getActivity().getContentResolver());
        new ContactPictureTask(getActivity(), (ImageView) inflatedView.findViewById(R.id.user_photo), user).run();

        ((TextView) inflatedView.findViewById(R.id.user_name)).setText(user.getmDisplayName());

        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public Account getSelectedAccount() {
        return mAccountAdapter.getSelectedAccount();
    }

    public void updateAllAccounts() {
        if (getActivity() != null)
            getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
    }

    @Override
    public void accountsChanged() {
        updateAllAccounts();

    }

    @Override
    public void accountStateChanged(String accoundID, String state, int code) {
        if (mAccountAdapter != null)
            mAccountAdapter.updateAccount(accoundID, state, code);
    }

    @Override
    public AsyncTaskLoader<Bundle> onCreateLoader(int arg0, Bundle arg1) {
        AccountsLoader l = new AccountsLoader(getActivity(), mCallbacks.getService());
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle data) {
        mAccountAdapter.removeAll();
        ArrayList<Account> accounts = data.getParcelableArrayList(AccountsLoader.ACCOUNTS);
        mAccountAdapter.addAll(accounts);
    }

    @Override
    public void onLoaderReset(Loader<Bundle> loader) {

    }

    public void backToHome() {
        sections.setItemChecked(0, true);
    }

}
