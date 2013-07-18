/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
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
package com.savoirfairelinux.sflphone.client;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.SectionsPagerAdapter;
import com.savoirfairelinux.sflphone.fragments.ContactListFragment;
import com.savoirfairelinux.sflphone.fragments.DialingFragment;
import com.savoirfairelinux.sflphone.fragments.HistoryFragment;
import com.savoirfairelinux.sflphone.fragments.HomeFragment;
import com.savoirfairelinux.sflphone.fragments.MenuFragment;
import com.savoirfairelinux.sflphone.interfaces.CallInterface;
import com.savoirfairelinux.sflphone.loaders.LoaderConstants;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.receivers.CallReceiver;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.views.CustomSlidingDrawer;

public class SFLPhoneHomeActivity extends Activity implements DialingFragment.Callbacks, ContactListFragment.Callbacks, HomeFragment.Callbacks,
        HistoryFragment.Callbacks, CallInterface, MenuFragment.Callbacks {

    SectionsPagerAdapter mSectionsPagerAdapter = null;
    static final String TAG = "SFLPhoneHomeActivity";

    /**
     * Fragments used
     */
    private ContactListFragment mContactsFragment = null;
    // private DialingFragment mDialingFragment = null;
    // private CallElementListFragment mCallElementList = null;
    // private HistoryFragment mHistorySectionFragment = null;
    private MenuFragment fMenu;

    private boolean mBound = false;
    private ISipService service;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    public static final int REQUEST_CODE_PREFERENCES = 1;
    private static final int REQUEST_CODE_CALL = 2;

    RelativeLayout mSliderButton;
    CustomSlidingDrawer mDrawer;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    CallReceiver callReceiver;

    private TabHost mTabHost;

    // public SFLPhoneHome extends Activity implements ActionBar.TabListener, OnClickListener

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            try {
                getFragmentManager().putFragment(bundle, mSectionsPagerAdapter.getClassName(i), mSectionsPagerAdapter.getItem(i));
            } catch (IllegalStateException e) {
                Log.e(TAG, "fragment=" + mSectionsPagerAdapter.getItem(i));
            }
        }

        getFragmentManager().putFragment(bundle, "ContactsListFragment", mContactsFragment);
        Log.w(TAG, "onSaveInstanceState()");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        callReceiver = new CallReceiver(this);

        // String libraryPath = getApplicationInfo().dataDir + "/lib";
        // Log.i(TAG, libraryPath);

        setContentView(R.layout.activity_sflphone_home);

        // Bind to LocalService
        if (!mBound) {
            Log.i(TAG, "onStart: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        if (mContactsFragment == null) {
            mContactsFragment = new ContactListFragment();
            Log.w(TAG, "Recreated mContactListFragment=" + mContactsFragment);
            getFragmentManager().beginTransaction().replace(R.id.contacts_frame, mContactsFragment).commit();
        }

        mDrawer = (CustomSlidingDrawer) findViewById(R.id.custom_sliding_drawer);

        mContactsFragment.setHandleView((RelativeLayout) findViewById(R.id.slider_button));

        mDrawer.setmTrackHandle(findViewById(R.id.handle_title));

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer(0.7f));

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
        mDrawerLayout, /* DrawerLayout object */
        R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
        R.string.drawer_open, /* "open drawer" description for accessibility */
        R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabHost.setCurrentTab(position);
            }
        });

    }

    private void initialiseTabHost(Bundle args) {

        mTabHost.setup();
        TabInfo tabInfo = null;
        SFLPhoneHomeActivity
                .AddTab(this,
                        this.mTabHost,
                        this.mTabHost.newTabSpec("Tab1").setIndicator(mSectionsPagerAdapter.getPageTitle(0),
                                getResources().getDrawable(mSectionsPagerAdapter.getIconOf(0))), (tabInfo = new TabInfo("Tab1",
                                DialingFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        SFLPhoneHomeActivity.AddTab(
                this,
                this.mTabHost,
                this.mTabHost.newTabSpec("Tab2").setIndicator(mSectionsPagerAdapter.getPageTitle(1),
                        getResources().getDrawable(mSectionsPagerAdapter.getIconOf(1))), (tabInfo = new TabInfo("Tab2", HomeFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        SFLPhoneHomeActivity
                .AddTab(this,
                        this.mTabHost,
                        this.mTabHost.newTabSpec("Tab3").setIndicator(mSectionsPagerAdapter.getPageTitle(2),
                                getResources().getDrawable(mSectionsPagerAdapter.getIconOf(2))), (tabInfo = new TabInfo("Tab3",
                                HistoryFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);

        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

            @Override
            public void onTabChanged(String tabId) {
                int pos = mTabHost.getCurrentTab();
                mViewPager.setCurrentItem(pos);

            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
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
        super.onStart();
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        intentFilter.addAction(CallManagerCallBack.INCOMING_TEXT);
        intentFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
        registerReceiver(callReceiver, intentFilter);

    }

    private boolean isClosing = false;
    private Timer t = new Timer();

    @Override
    public void onBackPressed() {

        if (getActionBar().getCustomView() != null) {
            getActionBar().setDisplayShowCustomEnabled(false);
            getActionBar().setCustomView(null);
            // Display all the contacts again
            getLoaderManager().restartLoader(LoaderConstants.CONTACT_LOADER, null, mContactsFragment);
            return;
        }

        if (mDrawer.isOpened()) {
            mDrawer.animateClose();
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

    /* activity no more in foreground */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(callReceiver);
        try {
            service.createNotification();

        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

    }

    /* activity is no longer visible */
    @Override
    protected void onStop() {
        super.onStop();

    }

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        /* stop the service, if no other bound user, no need to check if it is running */
        if (mBound) {
            Log.i(TAG, "onDestroy: Unbinding service...");

            unbindService(mConnection);
            mBound = false;

        }

        super.onDestroy();
    }

    public void launchCallActivity(SipCall infos) {
        Log.i(TAG, "Launch Call Activity");
        Bundle bundle = new Bundle();
        Conference tmp = new Conference("-1");

        tmp.getParticipants().add(infos);

        bundle.putParcelable("conference", tmp);
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtra("resuming", false);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE_CALL);
        overridePendingTransition(R.anim.slide_down, R.anim.slide_up);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);

            try {

                fMenu = new MenuFragment();
                getFragmentManager().beginTransaction().replace(R.id.left_drawer, fMenu).commit();
                mSectionsPagerAdapter = new SectionsPagerAdapter(SFLPhoneHomeActivity.this, getFragmentManager());
                initialiseTabHost(null);
                mViewPager.setOffscreenPageLimit(2);
                mViewPager.setAdapter(mSectionsPagerAdapter);
                mTabHost.setCurrentTab(1);
                service.destroyNotification();
                // mAdapter = new AccountSelectionAdapter(SFLPhoneHomeActivity.this, service, new ArrayList<Account>());
                // spinnerAccounts.setAdapter(mAdapter);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
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
            Log.w(TAG, "In Activity");
            fMenu.updateAllAccounts();
            break;
        case REQUEST_CODE_CALL:
            Log.w(TAG, "Result out of CallActivity");
            if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getItem(2) != null)
                getLoaderManager().restartLoader(LoaderConstants.HISTORY_LOADER, null, (HistoryFragment) mSectionsPagerAdapter.getItem(2));
            break;
        }

    }

    @Override
    public ISipService getService() {
        return service;
    }

    /**
     * Interface implemented to handle incoming events
     */
    @Override
    public void incomingCall(Intent call) {
        Toast.makeText(this, "New Call incoming", Toast.LENGTH_LONG).show();
        SipCall infos = call.getParcelableExtra("newcall");

        // mCallElementList.addCall(infos);

        launchCallActivity(infos);

    }

    @Override
    public void callStateChanged(Intent callState) {
        Bundle b = callState.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
        String cID = b.getString("CallID");
        String state = b.getString("State");
        Log.i(TAG, "callStateChanged" + cID + "    " + state);
        // mCallElementList.updateCall(cID, state);

    }

    @Override
    public void incomingText(Intent msg) {
        Bundle b = msg.getBundleExtra("com.savoirfairelinux.sflphone.service.newtext");
        b.getString("CallID");
        String from = b.getString("From");
        String mess = b.getString("Msg");
        Toast.makeText(getApplicationContext(), "text from " + from + " : " + mess, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onContactSelected(final CallContact c) {

        Thread launcher = new Thread(new Runnable() {

            final String[] CONTACTS_PHONES_PROJECTION = new String[] { Phone.NUMBER, Phone.TYPE };
            final String[] CONTACTS_SIP_PROJECTION = new String[] { SipAddress.SIP_ADDRESS, SipAddress.TYPE };

            @Override
            public void run() {
                SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();
                try {
                    callBuilder.startCallCreation().setAccountID(service.getAccountList().get(1).toString())
                            .setCallType(SipCall.state.CALL_TYPE_OUTGOING);
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
                    callBuilder.setContact(c);
                    launchCallActivity(callBuilder.build());
                } catch (RemoteException e1) {
                    Log.e(TAG, e1.toString());
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }

            }
        });
        launcher.start();
        mDrawer.close();

    }

    @Override
    public void onCallDialed(String to) {

        if (fMenu.getSelectedAccount() == null) {
            Toast.makeText(this, "No Account Selected", Toast.LENGTH_SHORT).show();
            return;
        }

        SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();
        callBuilder.startCallCreation().setAccountID(fMenu.getSelectedAccount().getAccountID()).setCallType(SipCall.state.CALL_TYPE_OUTGOING);
        callBuilder.setContact(CallContact.ContactBuilder.buildUnknownContact(to));

        try {
            launchCallActivity(callBuilder.build());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

    }

    @Override
    public void onContactDragged() {

        mDrawer.close();
        mTabHost.setCurrentTab(1);

    }

    private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, SFLPhoneHomeActivity.TabInfo>();

    /**
     * A simple factory that returns dummy views to the Tabhost
     * 
     * @author mwho
     */
    class TabFactory implements TabContentFactory {

        private final Context mContext;

        /**
         * @param context
         */
        public TabFactory(Context context) {
            mContext = context;
        }

        /**
         * (non-Javadoc)
         * 
         * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
         */
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }

    }

    /**
     * 
     * @author mwho Maintains extrinsic info of a tab's construct
     */
    private class TabInfo {
        private String tag;

        TabInfo(String tag, Class<?> clazz, Bundle args) {
            this.tag = tag;
        }

    }

    private static void AddTab(SFLPhoneHomeActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(activity.new TabFactory(activity));
        tabHost.addTab(tabSpec);
    }

    @Override
    public void openDrawer() {
        mDrawer.animateOpen();
    }

    @Override
    public void confCreated(Intent intent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void confRemoved(Intent intent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void confChanged(Intent intent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void recordingChanged(Intent intent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void resumeCallActivity() {
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtra("resuming", true);
        startActivityForResult(intent, REQUEST_CODE_CALL);
    }

}
