/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.DeviceAccountFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.Account;
import cx.ring.model.DaemonEvent;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.services.AccountService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class AccountEditionActivity extends AppCompatActivity implements AccountCallbacks, Observer<DaemonEvent> {

    @Inject
    AccountService mAccountService;

    public static final AccountCallbacks DUMMY_CALLBACKS = new AccountCallbacks() {
        @Override
        public IDRingService getRemoteService() {
            return null;
        }

        @Override
        public LocalService getService() {
            return null;
        }

        @Override
        public Account getAccount() {
            return null;
        }

        @Override
        public void addOnAccountChanged(AccountChangedListener list) {
            // Dummy
        }

        @Override
        public void removeOnAccountChanged(AccountChangedListener list) {
            // Dummy
        }

        @Override
        public void saveAccount() {
            // Dummy
        }
    };

    private static final String TAG = AccountEditionActivity.class.getSimpleName();
    private final ArrayList<AccountChangedListener> listeners = new ArrayList<>();
    private boolean mBound = false;
    private LocalService mService;
    private Account mAccSelected = null;

    private Fragment mCurrentlyDisplayed;
    private ViewPager mViewPager = null;
    private PagerSlidingTabStrip mSlidingTabLayout = null;

   /* private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(LocalService.ACTION_ACCOUNT_UPDATE)) {
                for (AccountChangedListener l : listeners) {
                    l.accountChanged(mAccSelected);
                }
            }
        }
    };*/

    @Override
    public void update(Observable o, DaemonEvent arg) {
        if (arg == null) {
            return;
        }

        switch (arg.getEventType()) {
            case ACCOUNTS_CHANGED:
                for (AccountChangedListener l : listeners) {
                    l.accountChanged(mAccSelected);
                }
                break;
            default:
                Log.d (TAG, "Event "+arg.getEventType()+" is not handled here");
                break;
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            mService = binder.getService();
            mBound = true;

            String accountId = getIntent().getData().getLastPathSegment();
            Log.i(TAG, "Service connected " + className.getClassName() + " " + getIntent().getData().toString());

            mAccSelected = mAccountService.getAccount(accountId);
            if (mAccSelected == null) {
                finish();
            }

            //registerReceiver(mReceiver, new IntentFilter(LocalService.ACTION_ACCOUNT_UPDATE));

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(mAccSelected.getAlias());
            }

            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setOffscreenPageLimit(4);
            mViewPager.setAdapter(new PreferencesPagerAdapter(getFragmentManager(), AccountEditionActivity.this, mAccSelected.isRing()));

            final PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
            mSlidingTabLayout.setViewPager(mViewPager);

            for (AccountChangedListener listener : listeners) {
                listener.accountChanged(mAccSelected);
            }

            mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    PreferencesPagerAdapter adapter = (PreferencesPagerAdapter) mViewPager.getAdapter();
                    mCurrentlyDisplayed = adapter.getItem(position);
                }

                @Override
                public void onPageSelected(int position) {
                    if (mCurrentlyDisplayed instanceof DeviceAccountFragment) {
                        DeviceAccountFragment deviceAccountFragment = (DeviceAccountFragment) mCurrentlyDisplayed;
                        if (deviceAccountFragment.isDisplayingWizard()) {
                            deviceAccountFragment.hideWizard();
                        }
                    }
                    PreferencesPagerAdapter adapter = (PreferencesPagerAdapter) mViewPager.getAdapter();
                    mCurrentlyDisplayed = adapter.getItem(position);
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    //~ Empty.
                }
            });

            if (mAccSelected.isRing()) {
                mSlidingTabLayout.setVisibility(View.GONE);
                mViewPager.setVisibility(View.GONE);
                mCurrentlyDisplayed = new DeviceAccountFragment();
                getFragmentManager().beginTransaction().add(R.id.fragment_container, mCurrentlyDisplayed).commit();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Called in case of service crashing or getting killed
            //unregisterReceiver(mReceiver);
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        mAccountService.addObserver(this);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);

        if (!mBound) {
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void finishAdvanced() {
        mSlidingTabLayout.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
    }

    private boolean isAdvancedSettings() {
        return mSlidingTabLayout.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_edition, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(mReceiver);
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (isAdvancedSettings()) {
            finishAdvanced();
        } else if (!(mCurrentlyDisplayed instanceof BackHandlerInterface) || !((BackHandlerInterface) mCurrentlyDisplayed).onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menuitem_delete:
                AlertDialog deleteDialog = createDeleteDialog();
                deleteDialog.show();
                break;
            case R.id.menuitem_advanced:
                mSlidingTabLayout.setVisibility(View.VISIBLE);
                mViewPager.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
        return true;
    }

    @NonNull
    private AlertDialog createDeleteDialog() {
        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage(R.string.account_delete_dialog_message).setTitle(R.string.account_delete_dialog_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Bundle bundle = new Bundle();
                        bundle.putString("AccountID", mAccSelected.getAccountID());

                        try {
                            mService.getRemoteService().removeAccount(mAccSelected.getAccountID());
                        } catch (RemoteException e) {
                            Log.e(TAG, "Error while removing account", e);
                        }
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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
    public void saveAccount() {
        if (mAccSelected == null || mService == null) {
            return;
        }

        final Account account = mAccSelected;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(account.getAlias());
        }

        final IDRingService remote = getRemoteService();
        if (remote == null) {
            Log.w(TAG, "Error updating account, remote service is null");
            return;
        }

        mService.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "updating account");
                    remote.setCredentials(account.getAccountID(), account.getCredentialsHashMapList());
                    remote.setAccountDetails(account.getAccountID(), account.getDetails());
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception updating account", e);
                }
            }
        });
    }

    @Override
    public IDRingService getRemoteService() {
        return mService.getRemoteService();
    }

    @Override
    public LocalService getService() {
        return mService;
    }

    @Override
    public Account getAccount() {
        return mAccSelected;
    }

    @Override
    public void addOnAccountChanged(AccountChangedListener list) {
        listeners.add(list);
    }

    @Override
    public void removeOnAccountChanged(AccountChangedListener list) {
        listeners.remove(list);
    }

    private static class PreferencesPagerAdapter extends FragmentPagerAdapter {
        boolean isRing = false;
        private Context ctx;

        PreferencesPagerAdapter(FragmentManager fm, Context c, boolean ring) {
            super(fm);
            ctx = c;
            isRing = ring;
        }

        @Override
        public int getCount() {
            return isRing ? 3 : 4;
        }

        @Override
        public Fragment getItem(int position) {
            return isRing ? getRingPanel(position) : getSIPPanel(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int resId = isRing ? getRingPanelTitle(position) : getSIPPanelTitle(position);
            return ctx.getString(resId);
        }

        @Nullable
        private static Fragment getRingPanel(int position) {
            switch (position) {
                case 0:
                    return new GeneralAccountFragment();
                case 1:
                    return new MediaPreferenceFragment();
                case 2:
                    return new AdvancedAccountFragment();
                default:
                    return null;
            }
        }

        @Nullable
        private static Fragment getSIPPanel(int position) {
            switch (position) {
                case 0:
                    return new GeneralAccountFragment();
                case 1:
                    return new MediaPreferenceFragment();
                case 2:
                    return new AdvancedAccountFragment();
                case 3:
                    return new SecurityAccountFragment();
                default:
                    return null;
            }
        }

        @StringRes
        private static int getRingPanelTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.account_preferences_basic_tab;
                case 1:
                    return R.string.account_preferences_media_tab;
                case 2:
                    return R.string.account_preferences_advanced_tab;
                default:
                    return -1;
            }
        }

        @StringRes
        private static int getSIPPanelTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.account_preferences_basic_tab;
                case 1:
                    return R.string.account_preferences_media_tab;
                case 2:
                    return R.string.account_preferences_advanced_tab;
                case 3:
                    return R.string.account_preferences_security_tab;
                default:
                    return -1;
            }
        }
    }
}
