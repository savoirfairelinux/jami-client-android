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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import java.io.File;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import cx.ring.R;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.DeviceAccountFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

public class AccountEditionActivity extends AppCompatActivity implements AccountCallbacks {

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
    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "accounts");
    private static final String TAG = AccountEditionActivity.class.getSimpleName();
    private static final int REQUEST_WRITE_STORAGE = 112;
    private final ArrayList<AccountChangedListener> listeners = new ArrayList<>();
    private boolean mBound = false;
    private LocalService mService;
    private Account mAccSelected = null;
    private Fragment mCurrentlyDisplayed;

    private Observer mAccountObserver = new Observer() {

        @Override
        public void update(Observable observable, Object data) {
            if (mAccSelected == null || mService == null) {
                return;
            }

            final Account acc = mAccSelected;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(acc.getAlias());
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
                        remote.setCredentials(acc.getAccountID(), acc.getCredentialsHashMapList());
                        remote.setAccountDetails(acc.getAccountID(), acc.getDetails());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error while setting credentials", e);
                    }
                }
            });
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            mService = binder.getService();
            mBound = true;

            String accountId = getIntent().getData().getLastPathSegment();
            Log.i(TAG, "Service connected " + className.getClassName() + " " + getIntent().getData().toString());

            mAccSelected = mService.getAccount(accountId);
            if (mAccSelected == null) {
                finish();
            }

            mAccSelected.addObserver(mAccountObserver);
            getSupportActionBar().setTitle(mAccSelected.getAlias());

            final ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
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

                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Called in case of service crashing or getting killed
            mAccSelected.deleteObserver(mAccountObserver);
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!mBound) {
            Intent intent = new Intent(this, LocalService.class);
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
    protected void onDestroy() {
        super.onDestroy();

        if (mBound) {
            unbindService(mConnection);
            if (mAccSelected != null) {
                mAccSelected.deleteObserver(mAccountObserver);
            }
            mBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (!(mCurrentlyDisplayed instanceof BackHandlerInterface) || !((BackHandlerInterface) mCurrentlyDisplayed).onBackPressed()) {
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
            default:
                break;
        }

        return true;
    }

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

    boolean checkPassword(@NonNull TextView pwd, TextView confirm) {
        boolean error = false;
        if (pwd.getText().length() < 6) {
            pwd.setError(getString(R.string.error_password_char_count));
            error = true;
        } else {
            pwd.setError(null);
        }
        if (confirm != null) {
            if (!pwd.getText().toString().equals(confirm.getText().toString())) {
                confirm.setError(getString(R.string.error_passwords_not_equals));
                error = true;
            } else {
                confirm.setError(null);
            }
        }
        return error;
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
        Fragment[] ringFragments = {new DeviceAccountFragment(), new GeneralAccountFragment(), new MediaPreferenceFragment(), new AdvancedAccountFragment()};
        Fragment[] sipFragments = {new GeneralAccountFragment(), new MediaPreferenceFragment(), new AdvancedAccountFragment(), new SecurityAccountFragment()};
        private Context ctx;

        PreferencesPagerAdapter(FragmentManager fm, Context c, boolean ring) {
            super(fm);
            ctx = c;
            isRing = ring;
        }

        private static int getRingPanelTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.account_preferences_devices_tab;
                case 1:
                    return R.string.account_preferences_basic_tab;
                case 2:
                    return R.string.account_preferences_media_tab;
                case 3:
                    return R.string.account_preferences_advanced_tab;
                default:
                    return -1;
            }
        }

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

        @Override
        public int getCount() {
            // For now we have 4 panels in each account type (Ring/SIP), but it may change
            return isRing ? ringFragments.length : sipFragments.length;
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

        private Fragment getRingPanel(int position) {
            Log.i(TAG, "PreferencesPagerAdapter getFragment " + position);
            return ringFragments[position];
        }

        private Fragment getSIPPanel(int position) {
            Log.i(TAG, "PreferencesPagerAdapter getFragment " + position);
            return sipFragments[position];
        }
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
                    Log.w(TAG, "updating account");
                    remote.setCredentials(account.getAccountID(), account.getCredentialsHashMapList());
                    remote.setAccountDetails(account.getAccountID(), account.getDetails());
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception updating account", e);
                }
            }
        });
    }

}
