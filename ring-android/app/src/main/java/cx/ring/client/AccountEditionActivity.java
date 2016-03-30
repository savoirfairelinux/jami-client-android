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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import cx.ring.R;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import cx.ring.fragments.GeneralAccountFragment;

public class AccountEditionActivity extends AppCompatActivity implements LocalService.Callbacks, GeneralAccountFragment.Callbacks, MediaPreferenceFragment.Callbacks,
        AdvancedAccountFragment.Callbacks, SecurityAccountFragment.Callbacks {
    private static final String TAG = AccountEditionActivity.class.getSimpleName();

    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "accounts");

    private boolean mBound = false;
    private LocalService service;

    private Account acc_selected = null;

    private Observer mAccountObserver = new Observer() {

        @Override
        public void update(Observable observable, Object data) {
            processAccount();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            service = binder.getService();
            mBound = true;

            String account_id = getIntent().getData().getLastPathSegment();
            Log.i(TAG, "Service connected " + className.getClassName() + " " + getIntent().getData().toString());

            acc_selected = service.getAccount(account_id);
            acc_selected.addObserver(mAccountObserver);
            getSupportActionBar().setTitle(acc_selected.getAlias());

            ArrayList<Pair<String, Fragment>> fragments = new ArrayList<>();
            if (acc_selected.isIP2IP()) {
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_media_tab), new MediaPreferenceFragment()));
            } else {
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_basic_tab), new GeneralAccountFragment()));
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_media_tab), new MediaPreferenceFragment()));
                fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_advanced_tab), new AdvancedAccountFragment()));
                if (acc_selected.isSip()) {
                    fragments.add(new Pair<String, Fragment>(getString(R.string.account_preferences_security_tab), new SecurityAccountFragment()));
                }
            }

            final ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.setAdapter(new PreferencesPagerAdapter(getFragmentManager(), fragments));

            PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
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

        setContentView(R.layout.activity_account_settings);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setElevation(0);

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
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog dialog;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menuitem_delete:
                dialog = createDeleteDialog();
                dialog.show();
                break;
            case R.id.menuitem_export:
                showExportDialog();
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processAccount() {
        final Account acc = acc_selected;
        final IDRingService remote = getRemoteService();
        getSupportActionBar().setTitle(acc.getAlias());
        ;
        service.getThreadPool().submit(new Runnable() {
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

    static boolean checkPassword(TextView pwd, TextView confirm) {
        boolean error = false;
        if (pwd.getText().length() < 6) {
            pwd.setError("minimum 6 characters");
            error = true;
        } else {
            pwd.setError(null);
        }
        if (confirm != null) {
            if (!pwd.getText().toString().equals(confirm.getText().toString())) {
                confirm.setError("passwords do not match");
                error = true;
            } else {
                confirm.setError(null);
            }
        }
        return error;
    }

    private AlertDialog showExportDialog() {
        Activity ownerActivity = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.dialog_account_export, null);
        final TextView pwd = (TextView) v.findViewById(R.id.newpwd_txt);
        final TextView pwd_confirm = (TextView) v.findViewById(R.id.newpwd_confirm_txt);
        builder.setMessage("Save account settings and credentials, password encrypted.").setTitle("Export Account")
                .setPositiveButton("Export", null).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                        /* Terminate with no action */
            }
        }).setView(v);


        final AlertDialog alertDialog = builder.create();
        pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_NEXT)
                    return checkPassword(v, null);
                return false;
            }
        });
        pwd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkPassword((TextView) v, null);
                }
            }
        });
        pwd_confirm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (!checkPassword(pwd, v)) {
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
                        return true;
                    }
                }
                return false;
            }
        });


        alertDialog.setOwnerActivity(ownerActivity);
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPassword(pwd, pwd_confirm)) {
                    doExportAccount(pwd.getText().toString());
                    alertDialog.dismiss();
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

        /*File[] paths = ContextCompat.getExternalFilesDirs(this, env);
        for (File path : paths)
            Log.w(TAG, path.getAbsolutePath());*/

        File path = Environment.getExternalStoragePublicDirectory(env);
        if (!path.mkdirs() && !path.isDirectory())
            Log.e(TAG, "Directory " + path.getAbsolutePath() + " not created");

        return new File(path, "ring_export");
    }

    void doExportAccount(final String password) {
        ArrayList<String> ids = new ArrayList<>(1);
        ids.add(acc_selected.getAccountID());
        File path = getExportStorageDir();
        try {
            getRemoteService().exportAccounts(ids, path.getAbsolutePath(), password);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Saved to " + path, Toast.LENGTH_LONG).show();
    }

    @Override
    public IDRingService getRemoteService() {
        return service.getRemoteService();
    }

    @Override
    public LocalService getService() {
        return service;
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
        return acc_selected;
    }


}
