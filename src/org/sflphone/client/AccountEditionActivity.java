/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

package org.sflphone.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.account.AccountDetailBasic;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.account.AccountDetailTls;
import org.sflphone.fragments.AudioManagementFragment;
import org.sflphone.fragments.EditionFragment;
import org.sflphone.service.ISipService;
import org.sflphone.service.SipService;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class AccountEditionActivity extends Activity implements TabListener, EditionFragment.Callbacks, AudioManagementFragment.Callbacks {
    private static final String TAG = "AccoutPreferenceActivity";

    public static final String KEY_MODE = "mode";
    private boolean mBound = false;
    private ISipService service;
    private String mAccountID;
    HashMap<String, String> basicPreferenceList;
    HashMap<String, String> advancedPreferenceList;
    HashMap<String, String> srtpPreferenceList;
    HashMap<String, String> tlsPreferenceList;

    PreferencesPagerAdapter mPreferencesPagerAdapter;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            mBound = true;
            mPreferencesPagerAdapter = new PreferencesPagerAdapter(AccountEditionActivity.this, getFragmentManager());
            mViewPager.setAdapter(mPreferencesPagerAdapter);
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            for (int i = 0; i < mPreferencesPagerAdapter.getCount(); i++) {
                getActionBar().addTab(
                        getActionBar().newTab().setText(mPreferencesPagerAdapter.getPageTitle(i)).setTabListener(AccountEditionActivity.this));

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    private ViewPager mViewPager;

    public interface result {
        static final int ACCOUNT_MODIFIED = Activity.RESULT_FIRST_USER + 1;
        static final int ACCOUNT_DELETED = Activity.RESULT_FIRST_USER + 2;
    }

    // private ArrayList<String> requiredFields = null;
    // EditionFragment mEditionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wizard);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        // mEditionFragment = new EditionFragment();
        // mEditionFragment.setArguments(getIntent().getExtras());
        // getFragmentManager().beginTransaction().replace(R.id.frag_container, mEditionFragment).commit();
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getActionBar().setSelectedNavigationItem(position);
            }
        });

        basicPreferenceList = (HashMap<String, String>) getIntent().getExtras().getSerializable(AccountDetailBasic.BUNDLE_TAG);
        advancedPreferenceList = (HashMap<String, String>) getIntent().getExtras().getSerializable(AccountDetailAdvanced.BUNDLE_TAG);
        srtpPreferenceList = (HashMap<String, String>) getIntent().getExtras().getSerializable(AccountDetailSrtp.BUNDLE_TAG);
        tlsPreferenceList = (HashMap<String, String>) getIntent().getExtras().getSerializable(AccountDetailTls.BUNDLE_TAG);

        mAccountID = getIntent().getExtras().getString("AccountID");
        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_edition, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (mPreferencesPagerAdapter.getItem(0) != null && ((EditionFragment) mPreferencesPagerAdapter.getItem(0)).isDifferent()) {
            AlertDialog dialog = createCancelDialog();
            dialog.show();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onDestroy() {

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        // stopService(new Intent(this, SipService.class));
        // serviceIsOn = false;
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.menuitem_delete:
            AlertDialog dialog = createDeleteDialog();
            dialog.show();
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
        if (((EditionFragment) mPreferencesPagerAdapter.getItem(0)).validateAccountCreation(missingValue)) {

            HashMap<String, String> accountDetails = new HashMap<String, String>();

            updateAccountDetails(accountDetails, ((EditionFragment) mPreferencesPagerAdapter.getItem(0)).getBasicDetails());
            updateAccountDetails(accountDetails, ((EditionFragment) mPreferencesPagerAdapter.getItem(0)).getAdvancedDetails());
            updateAccountDetails(accountDetails, ((EditionFragment) mPreferencesPagerAdapter.getItem(0)).getSrtpDetails());
            updateAccountDetails(accountDetails, ((EditionFragment) mPreferencesPagerAdapter.getItem(0)).getTlsDetails());

            accountDetails.put("Account.type", "SIP");
            try {
                service.setAccountDetails(mAccountID, accountDetails);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            finish();
        } else {
            dialog = createCouldNotValidateDialog(missingValue);
            dialog.show();
        }

    }

    private void updateAccountDetails(HashMap<String, String> accountDetails, AccountDetail det) {
        for (AccountDetail.PreferenceEntry p : det.getDetailValues()) {
            Log.i(TAG, "updateAccountDetails: pref " + p.mKey + " value " + det.getDetailString(p.mKey));
            accountDetails.put(p.mKey, det.getDetailString(p.mKey));
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
        builder.setMessage(message).setTitle("Missing Parameters").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
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
        builder.setMessage("Modifications will be lost").setTitle("Account Edition")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Activity activity = ((Dialog) dialog).getOwnerActivity();
                        activity.finish();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
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
        builder.setMessage("Do you really want to delete this account").setTitle("Delete Account")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Bundle bundle = new Bundle();
                        bundle.putString("AccountID", mAccountID);

                        try {
                            service.removeAccount(mAccountID);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        finish();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Terminate with no action */
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOwnerActivity(ownerActivity);

        return alertDialog;
    }

    public class PreferencesPagerAdapter extends FragmentStatePagerAdapter {

        Context mContext;
        ArrayList<Fragment> fragments;

        public PreferencesPagerAdapter(Context c, FragmentManager fm) {
            super(fm);
            mContext = c;
            fragments = new ArrayList<Fragment>();
            fragments.add(new EditionFragment());
            fragments.add(new AudioManagementFragment());
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return getString(R.string.account_preferences_basic).toUpperCase(Locale.getDefault());
            case 1:
                return getString(R.string.account_preferences_audio).toUpperCase(Locale.getDefault());
            default:
                Log.e(TAG, "getPreferencePageTitle: unknown tab position " + position);
                break;
            }
            return null;
        }
    }

    @Override
    public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
        // TODO Stub de la méthode généré automatiquement

    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());

    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // TODO Stub de la méthode généré automatiquement

    }

    @Override
    public HashMap<String, String> getBasicDetails() {
        return basicPreferenceList;
    }

    @Override
    public HashMap<String, String> getAdvancedDetails() {
        return advancedPreferenceList;
    }

    @Override
    public HashMap<String, String> getSRTPDetails() {
        return srtpPreferenceList;
    }

    @Override
    public HashMap<String, String> getTLSDetails() {
        return tlsPreferenceList;
    }

    @Override
    public ISipService getService() {
       return service;
    }
    
    @Override
    public String getAccountID(){
        return mAccountID;
    }

}
