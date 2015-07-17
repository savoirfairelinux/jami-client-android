/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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

package cx.ring.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import cx.ring.R;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.AudioManagementFragment;
import cx.ring.fragments.NestedSettingsFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.model.account.Account;
import cx.ring.service.ISipService;
import cx.ring.service.SipService;
import com.astuetz.PagerSlidingTabStrip;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import cx.ring.fragments.GeneralAccountFragment;

public class AccountEditionActivity extends Activity implements GeneralAccountFragment.Callbacks, AudioManagementFragment.Callbacks,
        AdvancedAccountFragment.Callbacks, SecurityAccountFragment.Callbacks, NestedSettingsFragment.Callbacks {
    private static final String TAG = AccountEditionActivity.class.getSimpleName();

    private boolean mBound = false;
    private ISipService service;
    private Account acc_selected;

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
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            mBound = true;

            ArrayList<Fragment> fragments = new ArrayList<>();
            if (acc_selected.isIP2IP()) {
                fragments.add(new AudioManagementFragment());
            } else {
                fragments.add(new GeneralAccountFragment());
                fragments.add(new AudioManagementFragment());
				if(acc_selected.isSip())
				{
					fragments.add(new AdvancedAccountFragment());
					fragments.add(new SecurityAccountFragment());
				}
            }

            ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);

            mPreferencesPagerAdapter = new PreferencesPagerAdapter(AccountEditionActivity.this, getFragmentManager(), fragments);
            mViewPager.setAdapter(mPreferencesPagerAdapter);
            mViewPager.setOffscreenPageLimit(3);

            final PagerSlidingTabStrip strip = PagerSlidingTabStrip.class.cast(findViewById(R.id.pager_sliding_strip));

            strip.setViewPager(mViewPager);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        acc_selected = getIntent().getExtras().getParcelable("account");

        acc_selected.addObserver(mAccountObserver);

        getActionBar().setTitle(acc_selected.getAlias());;

        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, SipService.class);
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
            service.setCredentials(acc_selected.getAccountID(), acc_selected.getCredentialsHashMapList());
            Map<String, String> details = acc_selected.getDetails();
            service.setAccountDetails(acc_selected.getAccountID(), details);
            Log.w(TAG, "service.setAccountDetails " + details.get("Account.hostname"));
            getActionBar().setTitle(acc_selected.getAlias());;

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
                            service.removeAccount(acc_selected.getAccountID());
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

        public PreferencesPagerAdapter(Context c, FragmentManager fm, ArrayList<Fragment> items) {
            super(fm);
            mContext = c;
            fragments = new ArrayList<Fragment>(items);

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
                if (acc_selected.isIP2IP()) {
                    return getString(R.string.account_preferences_audio_tab).toUpperCase(Locale.getDefault());
                } else {
                    return getString(R.string.account_preferences_basic_tab).toUpperCase(Locale.getDefault());
                }
            case 1:
                return getString(R.string.account_preferences_audio_tab).toUpperCase(Locale.getDefault());
            case 2:
                return getString(R.string.account_preferences_advanced_tab).toUpperCase(Locale.getDefault());
            case 3:
                return getString(R.string.account_preferences_security_tab).toUpperCase(Locale.getDefault());
            default:
                Log.e(TAG, "getPreferencePageTitle: unknown tab position " + position);
                break;
            }
            return null;
        }
    }

    @Override
    public ISipService getService() {
        return service;
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
