/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
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

import java.util.ArrayList;

import org.sflphone.R;
import org.sflphone.adapters.AccountSelectionAdapter;
import org.sflphone.adapters.ContactPictureTask;
import org.sflphone.adapters.MenuAdapter;
import org.sflphone.client.ActivityHolder;
import org.sflphone.client.HomeActivity;
import org.sflphone.client.SettingsActivity;
import org.sflphone.interfaces.AccountsInterface;
import org.sflphone.loaders.AccountsLoader;
import org.sflphone.loaders.LoaderConstants;
import org.sflphone.model.Account;
import org.sflphone.model.CallContact;
import org.sflphone.receivers.AccountsReceiver;
import org.sflphone.service.ConfigurationManagerCallback;
import org.sflphone.service.ISipService;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract.Profile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public class MenuFragment extends Fragment implements LoaderCallbacks<Bundle>, AccountsInterface {

    private static final String TAG = MenuFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";

    MenuAdapter mAdapter;
    String[] mProjection = new String[] { Profile._ID, Profile.DISPLAY_NAME_PRIMARY, Profile.LOOKUP_KEY, Profile.PHOTO_URI };
    AccountSelectionAdapter mAccountAdapter;
    private Spinner spinnerAccounts;
    AccountsReceiver accountReceiver;
    private Callbacks mCallbacks = sDummyCallbacks;

    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            return null;
        }
    };

    public interface Callbacks {

        public ISipService getService();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        try {
            mCallbacks.getService().registerAllAccounts();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        getLoaderManager().initLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new MenuAdapter(getActivity());
        accountReceiver = new AccountsReceiver(this);

        String[] categories = getResources().getStringArray(R.array.menu_categories);
        // ArrayAdapter<String> paramAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
        // R.array.menu_items_param));
        // ArrayAdapter<String> helpAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
        // R.array.menu_items_help));

        // Add Sections
        // mAdapter.addSection(categories[0], paramAdapter);
        // mAdapter.addSection(categories[1], helpAdapter);

    }

    public void onResume() {
        super.onResume();

        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter2.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        getActivity().registerReceiver(accountReceiver, intentFilter2);

    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(accountReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_menu, parent, false);

        ArrayAdapter<String> paramAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
                R.array.menu_items_param));
        ((ListView) inflatedView.findViewById(R.id.listView)).setAdapter(paramAdapter);
        ((ListView) inflatedView.findViewById(R.id.listView)).setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {

                Intent in = new Intent();
                switch (pos) {
                case 0:
                    in.setClass(getActivity(), SettingsActivity.class);
                    getActivity().startActivityForResult(in, HomeActivity.REQUEST_CODE_PREFERENCES);
                    break;
                case 1:
                    in.putExtra("ActivityHolder.args", ActivityHolder.args.FRAG_ABOUT);
                    in.setClass(getActivity(), ActivityHolder.class);
                    getActivity().startActivity(in);
                    break;
                }
            }
        });

        spinnerAccounts = (Spinner) inflatedView.findViewById(R.id.account_selection);
        mAccountAdapter = new AccountSelectionAdapter(getActivity(), new ArrayList<Account>());
        spinnerAccounts.setAdapter(mAccountAdapter);
        spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                if (null != view) {
                    ((RadioButton) view.findViewById(R.id.account_checked)).toggle();
                }
                mAccountAdapter.setSelectedAccount(pos);
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

    @Override
    public Loader<Bundle> onCreateLoader(int id, Bundle args) {
        AccountsLoader l = new AccountsLoader(getActivity(), mCallbacks.getService());
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle bun) {
        mAccountAdapter.removeAll();
        ArrayList<Account> accounts = bun.getParcelableArrayList(AccountsLoader.ACCOUNTS);
        mAccountAdapter.addAll(accounts);
    }

    @Override
    public void onLoaderReset(Loader<Bundle> arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Called by activity to pass a reference to sipservice to Fragment.
     * 
     * @param isip
     */
    public void onServiceSipBinded(ISipService isip) {

    }

    public void updateAllAccounts() {
        if (getActivity() != null)
            getActivity().getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
    }

    public void updateAccount(Intent accountState) {
        if (mAccountAdapter != null)
            mAccountAdapter.updateAccount(accountState);
    }

    @Override
    public void accountsChanged() {
        updateAllAccounts();

    }

    @Override
    public void accountStateChanged(Intent accountState) {
        updateAccount(accountState);

    }

}
