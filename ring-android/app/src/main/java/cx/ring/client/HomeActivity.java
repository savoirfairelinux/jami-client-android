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
package cx.ring.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import cx.ring.R;
import cx.ring.fragments.AboutFragment;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.CallListFragment;
import cx.ring.fragments.ContactListFragment;
import cx.ring.fragments.DialingFragment;
import cx.ring.fragments.HistoryFragment;
import cx.ring.fragments.HomeFragment;
import cx.ring.fragments.MenuFragment;
import cx.ring.history.HistoryEntry;
import cx.ring.model.account.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;
import cx.ring.service.ISipService;
import cx.ring.service.SipService;
import cx.ring.views.SlidingUpPanelLayout;
import cx.ring.views.SlidingUpPanelLayout.PanelSlideListener;

import android.app.Fragment;
import android.app.FragmentManager;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity implements DialingFragment.Callbacks, AccountsManagementFragment.Callbacks,
        ContactListFragment.Callbacks, CallListFragment.Callbacks, HistoryFragment.Callbacks, NavigationView.OnNavigationItemSelectedListener, MenuFragment.Callbacks {

    static final String TAG = HomeActivity.class.getSimpleName();

    private ContactListFragment mContactsFragment = null;
    private NavigationView fMenu;
    private MenuFragment fMenuHead = null;

    private boolean mBound = false;
    private ISipService service;

    public static final int REQUEST_CODE_PREFERENCES = 1;
    public static final int REQUEST_CODE_CALL = 3;
    public static final int REQUEST_CODE_CONVERSATION = 4;

    SlidingUpPanelLayout mContactDrawer;
    private DrawerLayout mNavigationDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private FloatingActionButton actionButton;

    private boolean isClosing = false;
    private Timer t = new Timer();

    protected Fragment fContent;

    public static final Pattern RING_ID_REGEX = Pattern.compile("^\\s+(?:ring(?:[\\s\\:]+))?(\\p{XDigit}{40})\\s+$", Pattern.CASE_INSENSITIVE);

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

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        actionButton = (FloatingActionButton) findViewById(R.id.action_button);

        fMenu = (NavigationView) findViewById(R.id.left_drawer);
        fMenu.setNavigationItemSelectedListener(this);

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
                    if (getSupportActionBar().isShowing()) {
                        getSupportActionBar().hide();
                    }
                } else {
                    if (!getSupportActionBar().isShowing()) {
                        getSupportActionBar().show();
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

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
        if (mContactDrawer.isExpanded()) {
            getSupportActionBar().hide();
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

    public void setToolbarState(boolean double_h, int title_res) {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            int abSz = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
            ViewGroup.LayoutParams params = toolbar.getLayoutParams();//toolbar.setContentInsetsRelative();

            //TypedArray a = obtainStyledAttributes(attrs, R.styleable.Toolbar_titleMarginBottom);

            //toolbar.get
            if (double_h) {
                params.height = abSz*2;
                actionButton.setVisibility(View.VISIBLE);
            }
            else {
                params.height = abSz;
                actionButton.setVisibility(View.GONE);
            }
            toolbar.setLayoutParams(params);
            toolbar.setMinimumHeight(abSz);
        }
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

        if (mContactDrawer.isExpanded() || mContactDrawer.isAnchored()) {
            mContactDrawer.collapsePane();
            return;
        }

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

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Log.i(TAG, "onDestroy: destroying service...");
        //Intent sipServiceIntent = new Intent(this, SipService.class);
        //stopService(sipServiceIntent);
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
            fContent = new HomeFragment();
            if (fMenuHead != null)
                fMenu.removeHeaderView(fMenuHead.getView());
            fMenu.inflateHeaderView(R.layout.menuheader);
            fMenuHead = (MenuFragment) getFragmentManager().findFragmentById(R.id.accountselector);

            getFragmentManager().beginTransaction().replace(R.id.main_frame, fContent, "Home").addToBackStack("Home").commit();
            mBound = true;
            Log.d(TAG, "Service connected service=" + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (fMenuHead != null) {
                fMenu.removeHeaderView(fMenuHead.getView());
                fMenuHead = null;
            }

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

        if (fMenuHead.getSelectedAccount() == null) {
            createAccountDialog().show();
            return;
        }

        if (!fMenuHead.getSelectedAccount().isRegistered()) {
            createNotRegisteredDialog().show();
            return;
        }

        getSupportActionBar().show();
        Thread launcher = new Thread(new Runnable() {

            final String[] CONTACTS_PHONES_PROJECTION = new String[]{Phone.NUMBER, Phone.TYPE};
            final String[] CONTACTS_SIP_PROJECTION = new String[]{SipAddress.SIP_ADDRESS, SipAddress.TYPE};

            @Override
            public void run() {

                Bundle args = new Bundle();
                //args.putString(SipCall.ID, Integer.toString(Math.abs(new Random().nextInt())));
                args.putParcelable(SipCall.ACCOUNT, fMenuHead.getSelectedAccount());
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

        Account usedAccount = fMenuHead.retrieveAccountById(to.getAccountID());

        if (usedAccount == null) {
            createAccountDialog().show();
            return;
        }

        if (usedAccount.isRegistered()) {
            Bundle args = new Bundle();
            //args.putString(SipCall.ID, Integer.toString(Math.abs(new Random().nextInt())));
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
        Account usedAccount = fMenuHead.getSelectedAccount();

        if (usedAccount == null) {
            createAccountDialog().show();
            return;
        }

        if (usedAccount.isRegistered() || usedAccount.isIP2IP()) {
            Bundle args = new Bundle();

            Matcher m = RING_ID_REGEX.matcher(to);
            if (m.matches() && m.groupCount() > 0) {
                to = "ring:"+m.group(1);
            }
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
    public boolean onNavigationItemSelected(MenuItem pos) {
        pos.setChecked(true);
        mNavigationDrawer.closeDrawers();

        switch (pos.getItemId()) {
            case R.id.menuitem_home:

                if (fContent instanceof HomeFragment)
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
            default:
                return false;
        }

        return true;
    }

}
