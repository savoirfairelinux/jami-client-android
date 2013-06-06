/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
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
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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
import com.savoirfairelinux.sflphone.adapters.AccountSelectionAdapter;
import com.savoirfairelinux.sflphone.adapters.SectionsPagerAdapter;
import com.savoirfairelinux.sflphone.fragments.CallElementListFragment;
import com.savoirfairelinux.sflphone.fragments.ContactListFragment;
import com.savoirfairelinux.sflphone.fragments.DialingFragment;
import com.savoirfairelinux.sflphone.fragments.HistoryFragment;
import com.savoirfairelinux.sflphone.fragments.MenuFragment;
import com.savoirfairelinux.sflphone.interfaces.AccountsInterface;
import com.savoirfairelinux.sflphone.interfaces.CallInterface;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.receivers.AccountsReceiver;
import com.savoirfairelinux.sflphone.receivers.CallReceiver;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ConfigurationManagerCallback;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.views.CustomSlidingDrawer;

public class SFLPhoneHomeActivity extends Activity implements DialingFragment.Callbacks, ContactListFragment.Callbacks,
        CallElementListFragment.Callbacks, HistoryFragment.Callbacks, CallInterface, AccountsInterface {

    SectionsPagerAdapter mSectionsPagerAdapter = null;
    static final String TAG = "SFLPhoneHomeActivity";

    /**
     * Fragments used
     */
    private ContactListFragment mContactsFragment = null;
    // private DialingFragment mDialingFragment = null;
    // private CallElementListFragment mCallElementList = null;
    // private HistoryFragment mHistorySectionFragment = null;
    private Fragment fMenu;

    private boolean mBound = false;
    private ISipService service;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    AccountSelectionAdapter mAdapter;
//    private Spinner spinnerAccounts;

    public static final int REQUEST_CODE_PREFERENCES = 1;
    private static final int REQUEST_CODE_CALL = 2;

//    private static final int ACTION_BAR_TAB_DIALING = 0;
//    private static final int ACTION_BAR_TAB_CALL = 1;
//    private static final int ACTION_BAR_TAB_HISTORY = 2;

    RelativeLayout mSliderButton;
    CustomSlidingDrawer mDrawer;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    CallReceiver callReceiver;
    AccountsReceiver accountReceiver;

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
        accountReceiver = new AccountsReceiver(this);
        // String libraryPath = getApplicationInfo().dataDir + "/lib";
        // Log.i(TAG, libraryPath);

        setContentView(R.layout.activity_sflphone_home);

        // Bind to LocalService
        if (!mBound) {
            Log.i(TAG, "onStart: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        /* getFragment(Bundle, String) */
        // if (savedInstanceState != null) {
        // Log.w(TAG, "Activity restarted, recreating PagerAdapter...");
        // /* getFragment (Bundle bundle, String key) */
        // mDialingFragment = (DialingFragment) getFragmentManager().getFragment(savedInstanceState,
        // mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_DIALING));
        // mCallElementList = (CallElementListFragment) getFragmentManager().getFragment(savedInstanceState,
        // mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_CALL));
        // mHistorySectionFragment = (HistoryFragment) getFragmentManager().getFragment(savedInstanceState,
        // mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_HISTORY));
        // }

        // if (mDialingFragment == null) {
        // mDialingFragment = new DialingFragment();
        // Log.w(TAG, "Recreated mDialingFragment=" + mDialingFragment);
        // }
        //
        if (mContactsFragment == null) {
            mContactsFragment = new ContactListFragment();
            Log.w(TAG, "Recreated mContactListFragment=" + mContactsFragment);
            getFragmentManager().beginTransaction().replace(R.id.contacts_frame, mContactsFragment).commit();
        }
        // if (mCallElementList == null) {
        // mCallElementList = new CallElementListFragment();
        // Log.w(TAG, "Recreated mCallElementList=" + mCallElementList);
        // }
        // if (mHistorySectionFragment == null) {
        // mHistorySectionFragment = new HistoryFragment();
        // Log.w(TAG, "Recreated mHistorySectionFragment=" + mHistorySectionFragment);
        // }

        mDrawer = (CustomSlidingDrawer) findViewById(R.id.custom_sliding_drawer);

        mContactsFragment.setHandleView((RelativeLayout) findViewById(R.id.slider_button));

        mDrawer.setmTrackHandle(findViewById(R.id.handle_title));

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        

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

        // getActionBar().setCustomView(R.layout.actionbar);
        // spinnerAccounts = (Spinner) getActionBar().getCustomView().findViewById(R.id.account_selection);
        // spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {
        //
        // @Override
        // public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
        // // public void onClick(DialogInterface dialog, int which) {
        //
        // Log.i(TAG, "Selected Account: " + mAdapter.getItem(pos));
        // if (null != view) {
        // ((RadioButton) view.findViewById(R.id.account_checked)).toggle();
        // }
        // mAdapter.setSelectedAccount(pos);
        // // accountSelectedNotifyAccountList(mAdapter.getItem(pos));
        // // setSelection(cursor.getPosition(),true);
        //
        // }
        //
        // @Override
        // public void onNothingSelected(AdapterView<?> arg0) {
        // // TODO Auto-generated method stub
        //
        // }
        // });
        // ((TextView) getActionBar().getCustomView().findViewById(R.id.activity_title)).setText(getTitle());
        // getActionBar().setDisplayShowCustomEnabled(true);
        
        

        fMenu = new MenuFragment();
        getFragmentManager().beginTransaction().replace(R.id.left_drawer, fMenu).commit();
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
                        getResources().getDrawable(mSectionsPagerAdapter.getIconOf(1))), (tabInfo = new TabInfo("Tab2",
                        CallElementListFragment.class, args)));
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
        
        
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter2.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        registerReceiver(accountReceiver, intentFilter2);
        
    }
    
    private boolean isClosing = false;
    private Timer t = new Timer();
    @Override
    public void onBackPressed() {
        if (mDrawer.isOpened()) {
            mDrawer.close();
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
        unregisterReceiver(accountReceiver);

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
            try {
                service.createNotification();

            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
            unbindService(mConnection);
            mBound = false;

        }

        super.onDestroy();
    }

    public void launchCallActivity(SipCall infos, String action) {
        Log.i(TAG, "Launch Call Activity");
        Bundle bundle = new Bundle();
        bundle.putString("action", action);
        bundle.putParcelable("CallInfo", infos);
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE_CALL);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);

            try {

                mSectionsPagerAdapter = new SectionsPagerAdapter(SFLPhoneHomeActivity.this, getFragmentManager());
                initialiseTabHost(null);
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
            Log.w(TAG, "Result out of PreferenceActivity");
            ((DialingFragment)mSectionsPagerAdapter.getItem(0)).updateAllAccounts();
            break;
        case REQUEST_CODE_CALL:
            Log.w(TAG, "Result out of CallActivity");
            break;
        }

    }

    @Override
    public void onCallSelected(SipCall c) {
        launchCallActivity(c, "display");

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

        launchCallActivity(infos, CallManagerCallBack.INCOMING_CALL);

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
    public void onContactSelected(CallContact c) {

        SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();
        try {
            callBuilder.startCallCreation().setAccountID(service.getAccountList().get(1).toString()).setCallType(SipCall.state.CALL_TYPE_OUTGOING);
            callBuilder.addContact(c);
            launchCallActivity(callBuilder.build(), "call");
        } catch (RemoteException e1) {
            Log.e(TAG, e1.toString());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

    }

    @Override
    public void onCallDialed(String accountID, String to) {

        SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();
        callBuilder.startCallCreation().setAccountID(accountID).setCallType(SipCall.state.CALL_TYPE_OUTGOING);
        callBuilder.addContact(CallContact.ContactBuilder.buildUnknownContact(to));

        try {
            launchCallActivity(callBuilder.build(), "call");
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
        private Class<?> clss;
        private Bundle args;
        private Fragment fragment;

        TabInfo(String tag, Class<?> clazz, Bundle args) {
            this.tag = tag;
            this.clss = clazz;
            this.args = args;
        }

    }

    private static void AddTab(SFLPhoneHomeActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(activity.new TabFactory(activity));
        tabHost.addTab(tabSpec);
    }

    @Override
    public void openDrawer() {
        mDrawer.open();
    }

    @Override
    public void accountsChanged() {
        ((DialingFragment)mSectionsPagerAdapter.getItem(0)).updateAllAccounts();
        
    }

    @Override
    public void accountStateChanged(Intent accountState) {
        ((DialingFragment)mSectionsPagerAdapter.getItem(0)).updateAccount(accountState);
        
    }

}
