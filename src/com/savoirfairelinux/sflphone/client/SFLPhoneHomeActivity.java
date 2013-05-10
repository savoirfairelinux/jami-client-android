/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@gmail.com>
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

import java.io.ObjectInputStream.GetField;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.fragments.CallElementListFragment;
import com.savoirfairelinux.sflphone.fragments.ContactListFragment;
import com.savoirfairelinux.sflphone.fragments.HistoryFragment;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipClient;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;

public class SFLPhoneHomeActivity extends Activity implements ActionBar.TabListener, CallElementListFragment.Callbacks, HistoryFragment.Callbacks {
    SectionsPagerAdapter mSectionsPagerAdapter = null;
    static final String TAG = "SFLPhoneHome";
    private static final int REQUEST_CODE_PREFERENCES = 1;
    ImageButton buttonCall, buttonHangup;
    private ContactListFragment mContactListFragment = null;
    private CallElementListFragment mCallElementList = null;
    private HistoryFragment mHistorySectionFragment = null;
    private boolean mBound = false;
    private ISipService service;

    private static final int ACTION_BAR_TAB_CONTACT = 0;
    private static final int ACTION_BAR_TAB_CALL = 1;
    private static final int ACTION_BAR_TAB_HISTORY = 2;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    final private int[] icon_res_id = { R.drawable.ic_tab_call, R.drawable.ic_tab_call, R.drawable.ic_tab_history };

    // public SFLPhoneHome extends Activity implements ActionBar.TabListener, OnClickListener

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            try {
                /* putFragment (Bundle bundle, String key, Fragment fragment) */
                getFragmentManager().putFragment(bundle, mSectionsPagerAdapter.getClassName(i), mSectionsPagerAdapter.getFragment(i));
            } catch (IllegalStateException e) {
                Log.e(TAG, "fragment=" + mSectionsPagerAdapter.getFragment(i));
            }
        }
        Log.w(TAG, "onSaveInstanceState()");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind to LocalService
        if (!mBound) {
            Log.i(TAG, "onStart: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        setContentView(R.layout.activity_sflphone_home);

        if (mSectionsPagerAdapter == null) {
            mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        }

        /* getFragment(Bundle, String) */
        if (savedInstanceState != null) {
            Log.w(TAG, "Activity restarted, recreating PagerAdapter...");
            /* getFragment (Bundle bundle, String key) */
            mContactListFragment = (ContactListFragment) getFragmentManager().getFragment(savedInstanceState,
                    mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_CONTACT));
            mCallElementList = (CallElementListFragment) getFragmentManager().getFragment(savedInstanceState,
                    mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_CALL));
            mHistorySectionFragment = (HistoryFragment) getFragmentManager().getFragment(savedInstanceState,
                    mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_HISTORY));
        }

        if (mContactListFragment == null) {
            mContactListFragment = new ContactListFragment();
            Log.w(TAG, "Recreated mContactListFragment=" + mContactListFragment);
        }
        if (mCallElementList == null) {
            mCallElementList = new CallElementListFragment();
            Log.w(TAG, "Recreated mCallElementList=" + mCallElementList);
        }
        if (mHistorySectionFragment == null) {
            mHistorySectionFragment = new HistoryFragment();
            Log.w(TAG, "Recreated mHistorySectionFragment=" + mHistorySectionFragment);
        }

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // final ActionBar actionBar = getActionBar();

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding tab.
        // We can also use ActionBar.Tab#select() to do this if we have a reference to the
        // Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(actionBar.newTab().setIcon(icon_res_id[i]).setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }

        actionBar.setSelectedNavigationItem(ACTION_BAR_TAB_CALL);

        // buttonHangup.setOnClickListener(new OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // processingHangUpAction();
        //
        // }
        // });

        // IntentFilter callFilter = new IntentFilter(CallManagerCallBack.NEW_CALL_CREATED);
        // callFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        // callFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
        // LocalBroadcastManager.getInstance(this).registerReceiver(mCallList, callFilter);
        //
        // mAccountList = mApplication.getAccountList();
        // Log.w(TAG, "mAccountList=" + mAccountList + ", mCallElementList=" + mCallElementList);

//        IntentFilter accountFilter = new IntentFilter(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
//        accountFilter.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        // LocalBroadcastManager.getInstance(this).registerReceiver(mAccountList, accountFilter);

        // SipCall.setSFLPhoneHomeContext(this);
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
        Log.i(TAG, "onResume");
        super.onResume();
    }

    /* activity no more in foreground */
    @Override
    protected void onPause() {
        super.onPause();
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

        /* unregister broadcast receiver */
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallList);
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccountList);

        super.onDestroy();
    }

    public void launchCallActivity(SipCall.CallInfo infos) {
        Log.i(TAG, "Launch Call Activity");
        Bundle bundle = new Bundle();
        bundle.putParcelable("CallInfo", infos);
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        private ISipClient callback = new ISipClient.Stub() {

            @Override
            public void incomingCall(Intent call) throws RemoteException {
                Log.i(TAG, "Incoming call transfered from Service");
                SipCall.CallInfo infos = new SipCall.CallInfo(call);

                SipCall c = new SipCall(infos);
                mCallElementList.addCall(c);

                launchCallActivity(infos);
            }

            @Override
            public void callStateChanged(Intent callState) throws RemoteException {
                Bundle b = callState.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
                String cID = b.getString("CallID");
                String state = b.getString("State");
                Log.i(TAG, "callStateChanged" + cID + "    " + state);
                mCallElementList.updateCall(cID, state);

            }

            @Override
            public void incomingText(Intent msg) throws RemoteException {
                Bundle b = msg.getBundleExtra("com.savoirfairelinux.sflphone.service.newtext");
                b.getString("CallID");
                String from = b.getString("From");
                String mess = b.getString("Msg");
                Toast.makeText(getApplicationContext(), "text from "+from+" : " + mess , Toast.LENGTH_LONG).show();
                
            }
        };

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);

            mBound = true;
            mCallElementList.onServiceSipBinded(service);
            mHistorySectionFragment.onServiceSipBinded(service);
            Log.d(TAG, "Service connected service=" + service);
            
            try {
                service.registerClient(callback);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
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
        switch(item.getItemId()){
        case R.id.menu_settings :
            Intent launchPreferencesIntent = new Intent().setClass(this, SFLPhonePreferenceActivity.class);
            startActivityForResult(launchPreferencesIntent, REQUEST_CODE_PREFERENCES);
            break;
        case R.id.menu_custom_draw:
            Intent launchNewInterfaceIntent = new Intent().setClass(this, BubblesViewActivity.class);
            startActivityForResult(launchNewInterfaceIntent, 0);
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_PREFERENCES == requestCode && service != null) {
            // Refresh Spinner with modified accounts
            mCallElementList.onServiceSipBinded(service);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_sflphone_home, menu);
        return true;
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // Log.d(TAG, "onTabReselected");
    }

    public void processingHangUpAction() {
        SipCall call = (SipCall) buttonHangup.getTag();
        if (call != null)
            call.notifyServiceHangup(service);
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to one of the primary sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment;

            switch (i) {
            case 0:
                mContactListFragment = new ContactListFragment();
                fragment = mContactListFragment;
                Log.w(TAG, "getItem() ContactListFragment=" + fragment);
                break;
            case 1:
                mCallElementList = new CallElementListFragment();
                // SipCall.setCallElementList(mCallElementList);
                // mCallElementList.setAccountList(mAccountList);
                fragment = mCallElementList;
                Log.w(TAG, "getItem() CallElementList=" + fragment);
                break;
            case 2:
                fragment = new HistoryFragment();
                Log.w(TAG, "getItem() HistoryFragment=" + fragment);
                break;
            default:
                Log.e(TAG, "getItem() unknown tab position " + i);
                return null;
            }

            // Log.i(TAG, "getItem() fragment is " + fragment);
            Bundle args = new Bundle();
            args.putInt(HistoryFragment.ARG_SECTION_NUMBER, i + 1);
            fragment.setArguments(args);
            return fragment;
        }

        public Fragment getFragment(int i) {
            Fragment fragment;

            switch (i) {
            case 0:
                fragment = mContactListFragment;
                break;
            case 1:
                fragment = mCallElementList;
                break;
            case 2:
                fragment = mHistorySectionFragment;
                break;
            default:
                Log.e(TAG, "getClassName: unknown fragment position " + i);
                fragment = null;
            }

            // Log.w(TAG, "getFragment: fragment=" + fragment);
            return fragment;
        }

        public String getClassName(int i) {
            String name;

            switch (i) {
            case 0:
                name = ContactListFragment.class.getName();
                break;
            case 1:
                name = CallElementListFragment.class.getName();
                break;
            case 2:
                name = HistoryFragment.class.getName();
                break;

            default:
                Log.e(TAG, "getClassName: unknown fragment position " + i);
                return null;
            }

            // Log.w(TAG, "getClassName: name=" + name);
            return name;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return getString(R.string.title_section0).toUpperCase();
            case 1:
                return getString(R.string.title_section1).toUpperCase();
            case 2:
                return getString(R.string.title_section2).toUpperCase();
            case 3:
                return getString(R.string.title_section3).toUpperCase();
            default:
                Log.e(TAG, "getPageTitle: unknown tab position " + position);
                break;
            }
            return null;
        }
    }

    @Override
    public void onCallSelected(SipCall c) {
        launchCallActivity(c.mCallInfo);

    }

    @Override
    public ISipService getService() {
        return service;
    }

}
