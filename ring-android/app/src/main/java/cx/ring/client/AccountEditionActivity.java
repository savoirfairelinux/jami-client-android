/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import cx.ring.R;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.AudioManagementFragment;
import cx.ring.fragments.NestedSettingsFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import cx.ring.fragments.GeneralAccountFragment;

public class AccountEditionActivity extends AppCompatActivity implements LocalService.Callbacks, GeneralAccountFragment.Callbacks, AudioManagementFragment.Callbacks,
        AdvancedAccountFragment.Callbacks, SecurityAccountFragment.Callbacks, NestedSettingsFragment.Callbacks {
    private static final String TAG = AccountEditionActivity.class.getSimpleName();

    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "accounts");

    private boolean mBound = false;
    private LocalService service;

    private Account acc_selected = null;

    private NestedSettingsFragment toDisplay;

    private Observer mAccountObserver = new Observer() {

        @Override
        public void update(Observable observable, Object data) {
            processAccount();
        }
    };

    PreferencesPagerAdapter mPreferencesPagerAdapter;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            service = binder.getService();
            mBound = true;

            setContentView(R.layout.activity_account_settings);

            final ActionBar actionBar = getSupportActionBar();

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);

            String account_id = getIntent().getData().getLastPathSegment();
            Log.i(TAG, "Service connected " + className.getClassName() + " " + getIntent().getData().toString());

            acc_selected = service.getAccount(account_id);
            acc_selected.addObserver(mAccountObserver);
            actionBar.setTitle(acc_selected.getAlias());

            ArrayList<Pair<String, Fragment>> fragments = new ArrayList<>();
            if (acc_selected.isIP2IP()) {
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_audio_tab), new AudioManagementFragment()));
            } else {
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_basic_tab), new GeneralAccountFragment()));
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_audio_tab), new AudioManagementFragment()));
                if(acc_selected.isSip())
                {
                    fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_advanced_tab), new AdvancedAccountFragment()));
                    fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_security_tab), new SecurityAccountFragment()));
                }
            }

            final ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
            mPreferencesPagerAdapter = new PreferencesPagerAdapter(AccountEditionActivity.this, getFragmentManager(), fragments);
            mViewPager.setAdapter(mPreferencesPagerAdapter);
            mViewPager.setOffscreenPageLimit(3);
            com.astuetz.PagerSlidingTabStrip mSlidingTabLayout = (com.astuetz.PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
            mSlidingTabLayout.setViewPager(mViewPager);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            acc_selected.deleteObserver(mAccountObserver);
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!mBound) {
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (acc_selected.isIP2IP()) {
            return true;
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_edition, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (toDisplay != null) {
            getFragmentManager().beginTransaction().setCustomAnimations(R.animator.slidein_up, R.animator.slideout_down).remove(toDisplay).commit();
            ((SecurityAccountFragment) mPreferencesPagerAdapter.getItem(3)).updateSummaries();
            toDisplay = null;
            return;
        }

        if (acc_selected.isIP2IP()) {
            super.onBackPressed();
            return;
        }

        super.onBackPressed();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
            if (toDisplay != null) {
                getFragmentManager().beginTransaction().setCustomAnimations(R.animator.slidein_up, R.animator.slideout_down).remove(toDisplay)
                        .commit();
                ((SecurityAccountFragment) mPreferencesPagerAdapter.getItem(3)).updateSummaries();
                toDisplay = null;
            } else
                finish();
            return true;
        case R.id.menuitem_delete:
            AlertDialog dialog = createDeleteDialog();
            dialog.show();
            break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processAccount() {
        try {
            service.getRemoteService().setCredentials(acc_selected.getAccountID(), acc_selected.getCredentialsHashMapList());
            Map<String, String> details = acc_selected.getDetails();
            service.getRemoteService().setAccountDetails(acc_selected.getAccountID(), details);
            Log.w(TAG, "service.setAccountDetails " + details.get("Account.hostname"));
            getSupportActionBar().setTitle(acc_selected.getAlias());;

        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private AlertDialog createDeleteDialog() {
        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage("Do you really want to delete this account").setTitle("Delete Account")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Bundle bundle = new Bundle();
                        bundle.putString("AccountID", acc_selected.getAccountID());

                        try {
                            service.getRemoteService().removeAccount(acc_selected.getAccountID());
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

    @Override
    public IDRingService getRemoteService() {
        return service.getRemoteService();
    }

    @Override
    public LocalService getService() {
        return service;
    }

    public class PreferencesPagerAdapter extends FragmentPagerAdapter {

        Context mContext;
        ArrayList<Pair<String, Fragment>> fragments;

        public PreferencesPagerAdapter(Context c, FragmentManager fm, ArrayList<Pair<String, Fragment>> items) {
            super(fm);
            mContext = c;
            fragments = items;
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position).second;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragments.get(position).first;
        }
    }

    @Override
    public Account getAccount() {
        return acc_selected;
    }

    @Override
    public void displayCredentialsScreen() {
        toDisplay = new NestedSettingsFragment();
        Bundle b = new Bundle();
        b.putInt("MODE", 0);
        toDisplay.setArguments(b);
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator.slidein_up, R.animator.slideout_down)
                .replace(R.id.hidden_container, toDisplay).commit();
    }

    @Override
    public void displaySRTPScreen() {
        toDisplay = new NestedSettingsFragment();
        Bundle b = new Bundle();
        b.putInt("MODE", 1);
        toDisplay.setArguments(b);
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator.slidein_up, R.animator.slideout_down)
                .replace(R.id.hidden_container, toDisplay).commit();
    }

    @Override
    public void displayTLSScreen() {
        toDisplay = new NestedSettingsFragment();
        Bundle b = new Bundle();
        b.putInt("MODE", 2);
        toDisplay.setArguments(b);
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator.slidein_up, R.animator.slideout_down)
                .replace(R.id.hidden_container, toDisplay).commit();
    }

}
