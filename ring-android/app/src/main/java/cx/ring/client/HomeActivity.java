/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import cx.ring.R;
import cx.ring.fragments.AboutFragment;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.CallListFragment;
import cx.ring.fragments.ContactListFragment;
import cx.ring.fragments.DialingFragment;
import cx.ring.fragments.MenuFragment;
import cx.ring.fragments.SettingsFragment;
import cx.ring.model.CallContact;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

import android.app.Fragment;
import android.app.FragmentManager;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class HomeActivity extends AppCompatActivity implements LocalService.Callbacks, DialingFragment.Callbacks, NavigationView.OnNavigationItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback, ContactListFragment.Callbacks {

    static final String TAG = HomeActivity.class.getSimpleName();

    public static final int REQUEST_CODE_PREFERENCES = 1;
    public static final int REQUEST_CODE_CALL = 3;
    public static final int REQUEST_CODE_CONVERSATION = 4;

    private LocalService service;
    private boolean mBound = false;
    private boolean mNoAccountOpened = false;

    private NavigationView fMenu;
    private MenuFragment fMenuHead = null;
    private DrawerLayout mNavigationDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private float mToolbarSize;
    private FloatingActionButton actionButton;
    protected Fragment fContent;

    private static void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e(TAG, "Uncaught Exception detected in thread ", e);
                    //e.printStackTrace();
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Could not set the Default Uncaught Exception Handler");
        }
    }

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setDefaultUncaughtExceptionHandler();

        mToolbarSize = getResources().getDimension(R.dimen.abc_action_bar_default_height_material);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        actionButton = (FloatingActionButton) findViewById(R.id.action_button);

        fMenu = (NavigationView) findViewById(R.id.left_drawer);
        fMenu.setNavigationItemSelectedListener(this);
        mNavigationDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mNavigationDrawer, /* DrawerLayout object */
                //  R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };

        mNavigationDrawer.setDrawerListener(mDrawerToggle);

        // Bind to LocalService

        String[] toRequest = LocalService.checkRequiredPermissions(this);
        if (toRequest.length > 0) {
            ActivityCompat.requestPermissions(this, toRequest, LocalService.PERMISSIONS_REQUEST);
        } else if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction());
            switch (intent.getAction()) {
                case LocalService.ACTION_ACCOUNT_UPDATE:
                    if (!mNoAccountOpened && service.getAccounts().isEmpty()) {
                        mNoAccountOpened = true;
                        startActivityForResult(new Intent().setClass(HomeActivity.this, AccountWizard.class), AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        if (!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("installed", false)) {
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("installed", true).commit();
            copyAssetFolder(getAssets(), "ringtones", getFilesDir().getAbsolutePath() + "/ringtones");
        }
        super.onStart();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.w(TAG, "onRequestPermissionsResult");

        switch (requestCode) {
            case LocalService.PERMISSIONS_REQUEST: {
                if (grantResults.length == 0) {
                    return;
                }

                for (int i=0, n=permissions.length; i<n; i++) {
                    switch (permissions[i]) {
                        case Manifest.permission.RECORD_AUDIO:
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                Log.e(TAG, "Missing required permission RECORD_AUDIO");
                                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                        .setTitle(R.string.start_error_title)
                                        .setMessage(R.string.start_error_mic_required)
                                        .setIcon(R.drawable.ic_mic_black_48dp)
                                        .setCancelable(false)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                finish();
                                            }
                                        });
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            finish();
                                        }
                                    });
                                }
                                builder.show();
                                return;
                            }
                            break;
                        case Manifest.permission.READ_CONTACTS:
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                            sharedPref.edit().putBoolean(getString(R.string.pref_systemContacts_key), grantResults[i] == PackageManager.PERMISSION_GRANTED).apply();
                            break;
                    }
                }

                if (!mBound) {
                    Intent intent = new Intent(this, LocalService.class);
                    startService(intent);
                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                }

                break;
            }
        }
    }

    public void setToolbarState(boolean double_h, int title_res) {
        ViewGroup.LayoutParams params = toolbar.getLayoutParams();
        if (double_h) {
            params.height = (int) (mToolbarSize * 2);
            actionButton.setVisibility(View.VISIBLE);
        } else {
            params.height = (int) mToolbarSize;
            actionButton.setVisibility(View.GONE);
        }
        toolbar.setLayoutParams(params);
        toolbar.setMinimumHeight((int) mToolbarSize);
        toolbar.setTitle(title_res);
    }

    public FloatingActionButton getActionButton() {
        return actionButton;
    }

    private static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            Log.i(TAG, "Creating :" + toPath);
            boolean res = true;
            for (String file : files)
                if (file.contains("")) {
                    Log.i(TAG, "Copying file :" + fromAssetPath + "/" + file + " to " + toPath + "/" + file);
                    res &= copyAsset(assetManager, fromAssetPath + "/" + file, toPath + "/" + file);
                } else {
                    Log.i(TAG, "Copying folder :" + fromAssetPath + "/" + file + " to " + toPath + "/" + file);
                    res &= copyAssetFolder(assetManager, fromAssetPath + "/" + file, toPath + "/" + file);
                }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /* user gets back to the activity, e.g. through task manager */
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /* activity gets back to the foreground and user input */
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {

        if (mNavigationDrawer.isDrawerVisible(Gravity.LEFT)) {
            mNavigationDrawer.closeDrawer(Gravity.LEFT);
            return;
        }

        /*if (mContactDrawer.isExpanded() || mContactDrawer.isAnchored()) {
            mContactDrawer.collapsePane();
            return;
        }*/

        if (getFragmentManager().getBackStackEntryCount() > 1) {
            popCustomBackStack();
            fMenu.getMenu().findItem(R.id.menuitem_home).setChecked(true);
            return;
        }

        super.onBackPressed();
    }

    private void popCustomBackStack() {
        FragmentManager fm = getFragmentManager();
        FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(0);
        fContent = fm.findFragmentByTag(entry.getName());
        for (int i = 0; i < fm.getBackStackEntryCount() - 1; ++i) {
            fm.popBackStack();
        }
    }

    /* activity no more in foreground */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unregisterReceiver(receiver);
            unbindService(mConnection);
            mBound = false;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            Log.i(TAG, "onServiceConnected " + className.getClassName());
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            service = binder.getService();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
            intentFilter.addAction(LocalService.ACTION_ACCOUNT_UPDATE);
            registerReceiver(receiver, intentFilter);

            fContent = new CallListFragment();
            if (fMenuHead != null)
                fMenu.removeHeaderView(fMenuHead.getView());
            fMenu.inflateHeaderView(R.layout.menuheader);
            fMenuHead = (MenuFragment) getFragmentManager().findFragmentById(R.id.accountselector);

            getFragmentManager().beginTransaction().replace(R.id.main_frame, fContent, "Home").addToBackStack("Home").commitAllowingStateLoss();
            mBound = true;
            Log.i(TAG, "Service connected service=" + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "onServiceConnected " + className.getClassName());
            if (fMenuHead != null) {
                fMenu.removeHeaderView(fMenuHead.getView());
                fMenuHead = null;
            }

            mBound = false;
            Log.i(TAG, "Service disconnected service=" + service);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected " + item.getItemId());

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_PREFERENCES:
            case AccountsManagementFragment.ACCOUNT_EDIT_REQUEST:
                if (fMenuHead != null)
                    fMenuHead.updateAllAccounts();
                break;
            case REQUEST_CODE_CALL:
                if (resultCode == CallActivity.RESULT_FAILURE) {
                    Log.w(TAG, "Call Failed");
                }
                break;
        }

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
    public void onCallDialed(String to) {
        Intent intent = new Intent()
                .setClass(this, CallActivity.class)
                .setAction(Intent.ACTION_CALL)
                .setData(Uri.parse(to));
        startActivityForResult(intent, REQUEST_CODE_CALL);
    }

    private AlertDialog createNotRegisteredDialog() {
        final Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);

        builder.setMessage(getResources().getString(R.string.cannot_pass_sipcall))
                .setTitle(getResources().getString(R.string.cannot_pass_sipcall_title))
                .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOwnerActivity(ownerActivity);

        return alertDialog;
    }

    private AlertDialog createAccountDialog() {
        final Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);

        builder.setMessage(getResources().getString(R.string.create_new_account_dialog))
                .setTitle(getResources().getString(R.string.create_new_account_dialog_title))
                .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent in = new Intent();
                        in.setClass(ownerActivity, AccountWizard.class);
                        ownerActivity.startActivityForResult(in, HomeActivity.REQUEST_CODE_PREFERENCES);
                    }
                }).setNegativeButton(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOwnerActivity(ownerActivity);

        return alertDialog;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem pos) {
        pos.setChecked(true);
        mNavigationDrawer.closeDrawers();

        switch (pos.getItemId()) {
            case R.id.menuitem_home:

                if (fContent instanceof CallListFragment)
                    break;

                if (getFragmentManager().getBackStackEntryCount() == 1)
                    break;

                popCustomBackStack();

                break;
            case  R.id.menuitem_accounts:
                if (fContent instanceof AccountsManagementFragment)
                    break;
                fContent = new AccountsManagementFragment();
                getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.main_frame, fContent, "Accounts").addToBackStack("Accounts").commit();
                break;
            case R.id.menuitem_about:
                if (fContent instanceof AboutFragment)
                    break;
                fContent = new AboutFragment();
                getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.main_frame, fContent, "About").addToBackStack("About").commit();
                break;
            case R.id.menuitem_prefs:
                if (fContent instanceof SettingsFragment)
                    break;
                fContent = new SettingsFragment();
                getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.main_frame, fContent, "Prefs").addToBackStack("Prefs").commit();
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onCallContact(final CallContact c) {
        Log.w(TAG, "onCallContact " + c.toString() + " " + c.getId() + " " + c.getKey());

        if (c.getPhones().size() > 1) {
            final CharSequence numbers[] = new CharSequence[c.getPhones().size()];
            int i = 0;
            for (CallContact.Phone p : c.getPhones())
                numbers[i++] = p.getNumber().getRawUriString();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.choose_number);
            builder.setItems(numbers, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(CallActivity.ACTION_CALL)
                            .setClass(HomeActivity.this, CallActivity.class)
                            .setData(Uri.parse(selected.toString()));
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(CallActivity.ACTION_CALL)
                    .setClass(this, CallActivity.class)
                    .setData(Uri.parse(c.getPhones().get(0).getNumber().getRawUriString()));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        }
    }

    @Override
    public void onTextContact(final CallContact c) {
        if (c.getPhones().size() > 1) {
            final CharSequence numbers[] = new CharSequence[c.getPhones().size()];
            int i = 0;
            for (CallContact.Phone p : c.getPhones())
                numbers[i++] = p.getNumber().getRawUriString();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.choose_number);
            builder.setItems(numbers, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(Intent.ACTION_VIEW)
                            .setClass(HomeActivity.this, ConversationActivity.class)
                            .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)))
                            .putExtra("number", selected);
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setClass(this, ConversationActivity.class)
                    .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

}
