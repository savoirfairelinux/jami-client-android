/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@gmail.com>
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.*;
import org.sflphone.R;
import org.sflphone.fragments.AboutFragment;
import org.sflphone.fragments.AccountsManagementFragment;
import org.sflphone.fragments.CallListFragment;
import org.sflphone.fragments.ContactListFragment;
import org.sflphone.fragments.DialingFragment;
import org.sflphone.fragments.HistoryFragment;
import org.sflphone.fragments.HomeFragment;
import org.sflphone.fragments.MenuFragment;
import org.sflphone.history.HistoryEntry;
import org.sflphone.model.account.Account;
import org.sflphone.model.CallContact;
import org.sflphone.model.Conference;
import org.sflphone.model.SipCall;
import org.sflphone.service.ISipService;
import org.sflphone.service.SipService;
import org.sflphone.views.SlidingUpPanelLayout;
import org.sflphone.views.SlidingUpPanelLayout.PanelSlideListener;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class HomeActivity extends FragmentActivity implements DialingFragment.Callbacks, AccountsManagementFragment.Callbacks,
        ContactListFragment.Callbacks, CallListFragment.Callbacks, HistoryFragment.Callbacks, MenuFragment.Callbacks {

    static final String TAG = HomeActivity.class.getSimpleName();

    private ContactListFragment mContactsFragment = null;
    private MenuFragment fMenu;

    private boolean mBound = false;
    private ISipService service;

    public static final int REQUEST_CODE_PREFERENCES = 1;
    public static final int REQUEST_CODE_CALL = 3;

    SlidingUpPanelLayout mContactDrawer;
    private DrawerLayout mNavigationDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private boolean isClosing = false;
    private Timer t = new Timer();

    protected Fragment fContent;

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Bind to LocalService
        if (!mBound) {
            Log.i(TAG, "onStart: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        mContactsFragment = new ContactListFragment();
        getFragmentManager().beginTransaction().replace(R.id.contacts_frame, mContactsFragment).commit();

        mContactDrawer = (SlidingUpPanelLayout) findViewById(R.id.contact_panel);
        // mContactDrawer.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
        mContactDrawer.setAnchorPoint(0.3f);
        mContactDrawer.setDragView(findViewById(R.id.contacts_frame));
        mContactDrawer.setEnableDragViewTouchEvents(true);
        mContactDrawer.setPanelSlideListener(new PanelSlideListener() {

            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (slideOffset < 0.2) {
                    if (getActionBar().isShowing()) {
                        getActionBar().hide();
                    }
                } else {
                    if (!getActionBar().isShowing()) {
                        getActionBar().show();
                    }
                }
            }

            @Override
            public void onPanelExpanded(View panel) {

            }

            @Override
            public void onPanelCollapsed(View panel) {

            }

            @Override
            public void onPanelAnchored(View panel) {

            }
        });

        mNavigationDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set a custom shadow that overlays the main content when the drawer opens
        mNavigationDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mNavigationDrawer, /* DrawerLayout object */
                R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mNavigationDrawer.setDrawerListener(mDrawerToggle);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
        if (mContactDrawer.isExpanded()) {
            getActionBar().hide();
        }
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

    private static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            Log.i(TAG, "Creating :" + toPath);
            boolean res = true;
            for (String file : files)
                if (file.contains(".")) {
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

        if (mContactDrawer.isExpanded() || mContactDrawer.isAnchored()) {
            mContactDrawer.collapsePane();
            return;
        }

        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            popCustomBackStack();
            fMenu.backToHome();
            return;
        }

        if (isClosing) {
            super.onBackPressed();
            t.cancel();
            finish();
        } else {
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    isClosing = false;
                }
            }, 3000);
            Toast.makeText(this, getResources().getString(R.string.close_msg), Toast.LENGTH_SHORT).show();
            isClosing = true;
        }
    }

    private void popCustomBackStack() {
        FragmentManager fm = getSupportFragmentManager();
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

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: destroying service...");
        Intent sipServiceIntent = new Intent(this, SipService.class);
        stopService(sipServiceIntent);
    }

    public void launchCallActivity(SipCall infos) {
        Conference tmp = new Conference(Conference.DEFAULT_ID);

        tmp.getParticipants().add(infos);
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtra("conference", tmp);
        intent.putExtra("resuming", false);
        startActivityForResult(intent, REQUEST_CODE_CALL);

        // overridePendingTransition(R.anim.slide_down, R.anim.slide_up);
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            fMenu = new MenuFragment();
            fContent = new HomeFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.left_drawer, fMenu).replace(R.id.main_frame, fContent, "Home").addToBackStack("Home").commit();
            mBound = true;
            Log.d(TAG, "Service connected service=" + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            mBound = false;
            Log.d(TAG, "Service disconnected service=" + service);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected " + item.getItemId());

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_PREFERENCES:
            case AccountsManagementFragment.ACCOUNT_EDIT_REQUEST:
                if (fMenu != null)
                    fMenu.updateAllAccounts();
                break;
            case REQUEST_CODE_CALL:
                if (resultCode == CallActivity.RESULT_FAILURE) {
                    Log.w(TAG, "Call Failed");
                }
                break;
        }

    }

    @Override
    public ISipService getService() {
        return service;
    }

    @Override
    public void onTextContact(final CallContact c) {
        // TODO
    }

    @Override
    public void onEditContact(final CallContact c) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(c.getId()));
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onCallContact(final CallContact c) {

        if (fMenu.getSelectedAccount() == null) {
            createAccountDialog().show();
            return;
        }

        if (!fMenu.getSelectedAccount().isRegistered()) {
            createNotRegisteredDialog().show();
            return;
        }

        getActionBar().show();
        Thread launcher = new Thread(new Runnable() {

            final String[] CONTACTS_PHONES_PROJECTION = new String[]{Phone.NUMBER, Phone.TYPE};
            final String[] CONTACTS_SIP_PROJECTION = new String[]{SipAddress.SIP_ADDRESS, SipAddress.TYPE};

            @Override
            public void run() {

                Bundle args = new Bundle();
                args.putString(SipCall.ID, Integer.toString(Math.abs(new Random().nextInt())));
                args.putParcelable(SipCall.ACCOUNT, fMenu.getSelectedAccount());
                args.putInt(SipCall.STATE, SipCall.state.CALL_STATE_NONE);
                args.putInt(SipCall.TYPE, SipCall.direction.CALL_TYPE_OUTGOING);

                Cursor cPhones = getContentResolver().query(Phone.CONTENT_URI, CONTACTS_PHONES_PROJECTION, Phone.CONTACT_ID + " =" + c.getId(),
                        null, null);

                while (cPhones.moveToNext()) {
                    c.addPhoneNumber(cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)), cPhones.getInt(cPhones.getColumnIndex(Phone.TYPE)));
                }
                cPhones.close();

                Cursor cSip = getContentResolver().query(Phone.CONTENT_URI, CONTACTS_SIP_PROJECTION, Phone.CONTACT_ID + "=" + c.getId(), null,
                        null);

                while (cSip.moveToNext()) {
                    c.addSipNumber(cSip.getString(cSip.getColumnIndex(SipAddress.SIP_ADDRESS)), cSip.getInt(cSip.getColumnIndex(SipAddress.TYPE)));
                }
                cSip.close();

                args.putParcelable(SipCall.CONTACT, c);

                launchCallActivity(new SipCall(args));
            }
        });
        launcher.start();
        mContactDrawer.collapsePane();

    }

    @Override
    public void onCallHistory(HistoryEntry to) {

        Account usedAccount = fMenu.retrieveAccountById(to.getAccountID());

        if (usedAccount == null) {
            createAccountDialog().show();
            return;
        }

        if (usedAccount.isRegistered()) {
            Bundle args = new Bundle();
            args.putString(SipCall.ID, Integer.toString(Math.abs(new Random().nextInt())));
            args.putParcelable(SipCall.ACCOUNT, usedAccount);
            args.putInt(SipCall.STATE, SipCall.state.CALL_STATE_NONE);
            args.putInt(SipCall.TYPE, SipCall.direction.CALL_TYPE_OUTGOING);
            args.putParcelable(SipCall.CONTACT, to.getContact());

            try {
                launchCallActivity(new SipCall(args));
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        } else {
            createNotRegisteredDialog().show();
        }
    }

    @Override
    public void onCallDialed(String to) {
        Account usedAccount = fMenu.getSelectedAccount();

        if (usedAccount == null) {
            createAccountDialog().show();
            return;
        }

        if (fMenu.getSelectedAccount().isRegistered()) {
            Bundle args = new Bundle();
            args.putString(SipCall.ID, Integer.toString(Math.abs(new Random().nextInt())));
            args.putParcelable(SipCall.ACCOUNT, usedAccount);
            args.putInt(SipCall.STATE, SipCall.state.CALL_STATE_NONE);
            args.putInt(SipCall.TYPE, SipCall.direction.CALL_TYPE_OUTGOING);
            args.putParcelable(SipCall.CONTACT, CallContact.ContactBuilder.buildUnknownContact(to));

            try {
                launchCallActivity(new SipCall(args));
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        } else {
            createNotRegisteredDialog().show();
        }
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
    public void onContactDragged() {
        mContactDrawer.collapsePane();
    }

    @Override
    public void toggleDrawer() {
        if (mContactDrawer.isAnchored())
            mContactDrawer.expandPane();
        else if (!mContactDrawer.isExpanded())
            mContactDrawer.expandPane(0.3f);
        else
            mContactDrawer.collapsePane();
    }

    @Override
    public void toggleForSearchDrawer() {
        if (mContactDrawer.isExpanded()) {
            mContactDrawer.collapsePane();
        } else
            mContactDrawer.expandPane();
    }

    @Override
    public void setDragView(RelativeLayout relativeLayout) {
        mContactDrawer.setDragView(relativeLayout);
    }

    @Override
    public void onSectionSelected(int pos) {

        mNavigationDrawer.closeDrawers();

        switch (pos) {
            case 0:

                if (fContent instanceof HomeFragment)
                    break;

                if (getSupportFragmentManager().getBackStackEntryCount() == 1)
                    break;

                popCustomBackStack();

                break;
            case 1:
                if (fContent instanceof AccountsManagementFragment)
                    break;
                fContent = new AccountsManagementFragment();
                getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.main_frame, fContent, "Accounts").addToBackStack("Accounts").commit();
                break;
            case 2:
                if (fContent instanceof AboutFragment)
                    break;
                fContent = new AboutFragment();
                getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.main_frame, fContent, "About").addToBackStack("About").commit();
                break;
        }


    }

}
