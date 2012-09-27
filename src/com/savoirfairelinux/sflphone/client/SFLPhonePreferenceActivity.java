/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

import java.util.List;

import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.support.v4.view.ViewPager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.service.ISipService;

public class SFLPhonePreferenceActivity extends Activity implements ActionBar.TabListener
{
    static final int NUM_PAGES = 2;
    static final String TAG = "SFLPhonePreferenceActivity";
    PreferencesPagerAdapter mPreferencesPagerAdapter;
    private boolean mBound = false;
    static boolean serviceIsOn = false;
    private ISipService service;
    
    ViewPager mViewPager;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            mBound = true;
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.d(TAG, "Service disconnected");
        } 
    };

    private void startSipService() {
        Thread thread = new Thread("StartSFLphoneService") {
            public void run() {
                Intent sipServiceIntent = new Intent(SFLPhonePreferenceActivity.this, SipService.class);
                startService(sipServiceIntent);
                serviceIsOn = true;
            };
        };
        try {
            thread.start();
        } catch (IllegalThreadStateException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Cannot start SFLPhone SipService!");
            AlertDialog alert = builder.create();
            alert.show();
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(TAG,"onCreate SFLPhonePreferenceActivity");

        setContentView(R.layout.activity_sflphone_preferences);

        mPreferencesPagerAdapter = new PreferencesPagerAdapter(getFragmentManager());

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager = (ViewPager) findViewById(R.id.preferences_pager);
	mViewPager.setAdapter(mPreferencesPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
        {
            @Override
            public void onPageSelected(int position)
            {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for(int i = 0; i < mPreferencesPagerAdapter.getCount(); i++) {
            actionBar.addTab(actionBar.newTab().setText(mPreferencesPagerAdapter.getPageTitle(i)).setTabListener(this));
        }

        
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        if(!mBound) {
            Log.i(TAG, "onStart: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: stopping SipService...");
        stopService(new Intent(this, SipService.class));
        serviceIsOn = false;
        super.onDestroy();
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
    }

    public class PreferencesPagerAdapter extends FragmentStatePagerAdapter {

        public PreferencesPagerAdapter(FragmentManager fm) 
        {
            super(fm);
        }

        @Override
        public int getCount() 
        {
            return NUM_PAGES;
        }

        @Override
        public Fragment getItem(int position) 
        {
            Fragment fragment;

            switch (position) {
            case 0:
                fragment = new AccountManagementFragment(service);
                break;
            case 1:
                fragment = new PrefManagementFragment(service);
                break;
            default:
                Log.i(TAG, "Get new fragment " + position + " is null");
                return null;
            }

            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch(position) {
            case 0:
                return getString(R.string.preference_section1).toUpperCase();
            case 1:
                return getString(R.string.preference_section2).toUpperCase();
            default:
                Log.e(TAG, "getPreferencePageTitle: unknown tab position " + position);
                break;
            }
            return null;
        }
    }

    public static class ArrayListFragment extends ListFragment {
        int mNum;

        static ArrayListFragment newInstance(int num) {
            ArrayListFragment f = new ArrayListFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // setListAdapter(new ArrayAdapter<String>(getActivity(),
            //        android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings));
        }

    }
}
