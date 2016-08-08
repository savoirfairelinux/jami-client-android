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
import android.util.Pair;
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
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

public class AccountEditionActivity extends AppCompatActivity implements LocalService.Callbacks, GeneralAccountFragment.Callbacks, MediaPreferenceFragment.Callbacks,
        AdvancedAccountFragment.Callbacks, SecurityAccountFragment.Callbacks {
    private static final String TAG = AccountEditionActivity.class.getSimpleName();

    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "accounts");
    private static final int REQUEST_WRITE_STORAGE = 112;

    private boolean mBound = false;
    private LocalService mService;

    private Account mAccSelected = null;

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
                        e.printStackTrace();
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

            ArrayList<Pair<String, Fragment>> fragments = new ArrayList<>();
            fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_basic_tab), new GeneralAccountFragment()));
            fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_media_tab), new MediaPreferenceFragment()));
            fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_advanced_tab), new AdvancedAccountFragment()));
            if (mAccSelected.isSip()) {
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_security_tab), new SecurityAccountFragment()));
            }

            final ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.setAdapter(new PreferencesPagerAdapter(getFragmentManager(), fragments));

            PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
            mSlidingTabLayout.setViewPager(mViewPager);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menuitem_delete:
                AlertDialog deleteDialog = createDeleteDialog();
                deleteDialog.show();
                break;
            case R.id.menuitem_export:
                startExport();
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
                            e.printStackTrace();
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
                .setNeutralButton(R.string.account_export, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        startExport();
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

    private void startExport() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            showExportDialog();
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
                    showExportDialog();
                } else {
                    Toast.makeText(this, R.string.permission_write_denied, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private AlertDialog showExportDialog() {
        Activity ownerActivity = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.dialog_account_export, null);
        final TextView pwd = (TextView) v.findViewById(R.id.newpwd_txt);
        final TextView pwdConfirm = (TextView) v.findViewById(R.id.newpwd_confirm_txt);
        builder.setMessage(R.string.account_export_message)
                .setTitle(R.string.account_export_title)
                .setPositiveButton(R.string.account_export, null)
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
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
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

                    new ExportAccountTask().execute(pwd_txt);
                }
            }
        });

        return alertDialog;
    }

    public File getExportStorageDir() {
        // Get the directory for the user's public pictures directory.
        String env = Environment.DIRECTORY_DOWNLOADS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            env = Environment.DIRECTORY_DOCUMENTS;

        File path = Environment.getExternalStoragePublicDirectory(env);

        if (!path.mkdirs() && !path.isDirectory()) {
            Log.w(TAG, "Directory " + path.getAbsolutePath() + " not created, using fallback");
            // Fallback case, use Downloads
            env = Environment.DIRECTORY_DOWNLOADS;
            path = Environment.getExternalStoragePublicDirectory(env);

            if (!path.mkdirs() && !path.isDirectory())
                Log.e(TAG, "Fallback on " + path.getAbsolutePath() + " failed!");
        }

        return new File(path, getAccount().getAlias() + ".ring");
    }

    private class ExportAccountTask extends AsyncTask<String, Void, Integer> {
        ProgressDialog exportDialog;
        private String path;

        @Override
        protected void onPreExecute() {
            exportDialog = ProgressDialog.show(AccountEditionActivity.this,
                    getString(R.string.export_dialog_title),
                    getString(R.string.import_export_wait), true);
            exportDialog.setCancelable(false);
        }

        protected Integer doInBackground(String... args) {
            int ret = 1;
            ArrayList<String> ids = new ArrayList<>(1);
            ids.add(mAccSelected.getAccountID());
            File fpath = getExportStorageDir();
            path = fpath.getAbsolutePath();
            try {
                ret = getRemoteService().exportAccounts(ids, path, args[0]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return ret;
        }

        protected void onPostExecute(Integer ret) {
            if (exportDialog != null) {
                exportDialog.dismiss();
            }

            Log.d(TAG, "Account export to " + path + " returned " + ret);
            if (ret == 0) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.account_export_result, path), Snackbar.LENGTH_LONG).show();
            } else
                new AlertDialog.Builder(AccountEditionActivity.this).setTitle(R.string.export_failed_dialog_title)
                        .setMessage(R.string.export_failed_dialog_msg)
                        .setPositiveButton(android.R.string.ok, null).show();
        }
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
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private static class PreferencesPagerAdapter extends FragmentPagerAdapter {
        private final ArrayList<Pair<String, Fragment>> fragments;

        public PreferencesPagerAdapter(FragmentManager fm, ArrayList<Pair<String, Fragment>> items) {
            super(fm);
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
        return mAccSelected;
    }


}
