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
import android.support.annotation.StringRes;
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
    private ViewPager mViewPager = null;
    private PagerSlidingTabStrip mSlidingTabLayout = null;

    private final Observer mAccountObserver = new Observer() {

        @Override
        public void update(Observable observable, Object data) {
            Log.i(TAG, "Observer: account changed !");
            for (AccountChangedListener l : listeners) {
                l.accountChanged(mAccSelected);
            }
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
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

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);

        if (!mBound) {
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }


    public void editAdvanced() {
        mSlidingTabLayout.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
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
            case R.id.menuitem_backup:
                startBackup();
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
                })
                .setNeutralButton(R.string.account_backup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        startBackup();
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

    private void startBackup() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            showBackupDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showBackupDialog();
                } else {
                    Toast.makeText(this, R.string.permission_write_denied, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private AlertDialog showBackupDialog() {
        Activity ownerActivity = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.dialog_account_backup, null);
        final TextView pwd = (TextView) v.findViewById(R.id.newpwd_txt);
        final TextView pwdConfirm = (TextView) v.findViewById(R.id.newpwd_confirm_txt);
        builder.setMessage(R.string.account_backup_message)
                .setTitle(R.string.account_backup_title)
                .setPositiveButton(R.string.account_backup, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                /* Terminate with no action */
                    }
                }).setView(v);


        final AlertDialog alertDialog = builder.create();
        pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.i(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                return actionId == EditorInfo.IME_ACTION_NEXT && checkPassword(v, null);
            }
        });
        pwd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkPassword((TextView) v, null);
                } else {
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        pwdConfirm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.i(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_DONE && !checkPassword(pwd, v)) {
                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
                    return true;
                }
                return false;
            }
        });


        alertDialog.setOwnerActivity(ownerActivity);
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPassword(pwd, pwdConfirm)) {
                    final String pwd_txt = pwd.getText().toString();
                    alertDialog.dismiss();

                    new BackupAccountTask().execute(pwd_txt);
                }
            }
        });

        return alertDialog;
    }

    public File getBackupStorageDir() {
        // Get the directory for the user's public pictures directory.
        String env = Environment.DIRECTORY_DOWNLOADS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            env = Environment.DIRECTORY_DOCUMENTS;
        }

        File path = Environment.getExternalStoragePublicDirectory(env);

        if (!path.mkdirs() && !path.isDirectory()) {
            Log.w(TAG, "Directory " + path.getAbsolutePath() + " not created, using fallback");
            // Fallback case, use Downloads
            env = Environment.DIRECTORY_DOWNLOADS;
            path = Environment.getExternalStoragePublicDirectory(env);

            if (!path.mkdirs() && !path.isDirectory()) {
                Log.e(TAG, "Fallback on " + path.getAbsolutePath() + " failed!");
            }
        }

        return new File(path, getAccount().getAlias() + ".ring");
    }

    private class BackupAccountTask extends AsyncTask<String, Void, Integer> {
        ProgressDialog backupDialog;
        private String path;

        @Override
        protected void onPreExecute() {
            backupDialog = ProgressDialog.show(AccountEditionActivity.this,
                    getString(R.string.backup_dialog_title),
                    getString(R.string.restore_backup_wait), true);
            backupDialog.setCancelable(false);
        }

        protected Integer doInBackground(String... args) {
            int ret = 1;
            ArrayList<String> ids = new ArrayList<>(1);
            ids.add(mAccSelected.getAccountID());
            File filePath = getBackupStorageDir();
            path = filePath.getAbsolutePath();
            try {
                ret = getRemoteService().backupAccounts(ids, path, args[0]);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while backup account", e);
            }
            return ret;
        }

        protected void onPostExecute(Integer ret) {
            if (backupDialog != null) {
                backupDialog.dismiss();
            }

            Log.d(TAG, "Account backup to " + path + " returned " + ret);
            if (ret == 0) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.account_backup_result, path), Snackbar.LENGTH_LONG).show();
            } else {
                new AlertDialog.Builder(AccountEditionActivity.this).setTitle(R.string.backup_failed_dialog_title)
                        .setMessage(R.string.backup_failed_dialog_msg)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
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

        private static Fragment getRingPanel(int position) {
            Log.i(TAG, "PreferencesPagerAdapter getFragment " + position);
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

        private static Fragment getSIPPanel(int position) {
            Log.i(TAG, "PreferencesPagerAdapter getFragment " + position);
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
