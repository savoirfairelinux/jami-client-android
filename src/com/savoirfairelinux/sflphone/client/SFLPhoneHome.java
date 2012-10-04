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

import java.util.Random;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;

public class SFLPhoneHome extends Activity implements ActionBar.TabListener, OnClickListener
{
    SectionsPagerAdapter mSectionsPagerAdapter;
    static final String TAG = "SFLPhoneHome";
    private ButtonSectionFragment buttonFragment;
    /* default callID */
    static String callID = "007";
    static boolean callOnGoing = false;
    static boolean serviceIsOn = false;
    private String incomingCallID = "";
    private static final int REQUEST_CODE_PREFERENCES = 1;
    ImageButton buttonCall, buttonHangup;
    Button buttonService;
    static Animation animation;
    ContactListFragment mContactListFragment;
    CallElementList mCallElementList;
    private boolean mBound = false;
    private ISipService service;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    final private int[] icon_res_id = {R.drawable.ic_tab_call, R.drawable.ic_tab_call, R.drawable.ic_tab_history, R.drawable.ic_tab_play_selected};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sflphone_home);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // final ActionBar actionBar = getActionBar();

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding tab.
        // We can also use ActionBar.Tab#select() to do this if we have a reference to the
        // Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
        {
            @Override
            public void onPageSelected(int position)
            {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            Log.i(TAG, "adding tab: " + i);
            actionBar.addTab(actionBar.newTab().setIcon(icon_res_id[i]).setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }

        buttonCall = (ImageButton) findViewById(R.id.buttonCall);
        buttonHangup = (ImageButton) findViewById(R.id.buttonHangUp);

        // Change alpha from fully visible to invisible
        animation = new AlphaAnimation(1, 0);
        // duration - half a second
        animation.setDuration(500);
        // do not alter animation rate
        animation.setInterpolator(new LinearInterpolator());
        // Repeat animation infinitely
        animation.setRepeatCount(Animation.INFINITE);
        // Reverse
        animation.setRepeatMode(Animation.REVERSE);

        if (!serviceIsOn) {
            Log.i(TAG, "starting SipService");
            startSipService();
        }
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        // Bind to LocalService
        if (!mBound) {
            Log.i(TAG, "onStart: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
        /* stop the service, if no other bound user, no need to check if it is running */
        if (mBound) {
            Log.i(TAG, "onStop: Unbinding service...");
            unbindService(mConnection);
            mBound = false;
        }
    }

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: stopping SipService...");
        stopService(new Intent(this, SipService.class));
        serviceIsOn = false;
        super.onDestroy();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder binder) {
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
                Intent sipServiceIntent = new Intent(SFLPhoneHome.this, SipService.class);
                //sipServiceIntent.putExtra(ServiceConstants.EXTRA_OUTGOING_ACTIVITY, new ComponentName(SFLPhoneHome.this, SFLPhoneHome.class));
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
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("SFLphone", "onOptionsItemSelected " + item.getItemId());
        if(item.getItemId() != 0) {
            // When the button is clicked, launch an activity through this intent
            Intent launchPreferencesIntent = new Intent().setClass(this, SFLPhonePreferenceActivity.class);

            // Make it a subactivity so we know when it returns
            startActivityForResult(launchPreferencesIntent, REQUEST_CODE_PREFERENCES);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_sflphone_home, menu);
        return true;
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
        //		Log.d(TAG, "onTabReselected");
    }

    public void setIncomingCallID(String accountID, String callID, String from) {
        Log.i(TAG, "incomingCall(" + accountID + ", " + callID + ", " + from + ")");
        incomingCallID = callID;
        buttonCall.startAnimation(animation);
        buttonCall.setImageResource(R.drawable.ic_incomingcall);
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the primary sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter
    {

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int i)
        {
            Fragment fragment;

            switch (i) {
            case 0:
                mContactListFragment = new ContactListFragment();
                fragment = mContactListFragment;
                break;
            case 1:
                mCallElementList = new CallElementList();
                SipCall.setCallElementList(mCallElementList);
                fragment = mCallElementList;
                break;
            case 2:
                fragment = new DummySectionFragment();
                break;
            case 3:
                fragment = new ButtonSectionFragment();
                Log.i(TAG, "getItem: fragment is " + fragment);
                break;
            default:
                Log.e(TAG, "getItem: unknown tab position " + i);
                return null;
            }

            Bundle args = new Bundle();
            args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount()
        {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
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

    /**
     * A dummy fragment representing a section of the app, but that simply
     * displays dummy text.
     */
    public static class DummySectionFragment extends Fragment
    {
        public DummySectionFragment()
        {
            setRetainInstance(true);
        }

        public static final String ARG_SECTION_NUMBER = "section_number";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
        {
            TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            Bundle args = getArguments();
            textView.setText(Integer.toString(args.getInt(ARG_SECTION_NUMBER)));
            textView.setText("java sucks");
            return textView;
        }
    }

    @Override
    public void onClick(View view)
    {
        Log.i("SFLPhoneHome", "onClic from SFLPhoneHome");

        buttonService = (Button) findViewById(R.id.buttonService);

        try {
            switch (view.getId()) {
            case R.id.buttonCall:
                TextView textView = (TextView) findViewById(R.id.editAccountID);
                String accountID = textView.getText().toString();
                EditText editText;
                Random random = new Random();

                if (incomingCallID != "") {
                    buttonCall.clearAnimation();
                    service.accept(incomingCallID);
                    callID = incomingCallID;
                    incomingCallID="";
                    callOnGoing = true;
                    buttonCall.setEnabled(false);
                    buttonHangup.setEnabled(true);
                } else {
                    if (callOnGoing == false) {
                        editText = (EditText) findViewById(R.id.editTo);
                        String to = editText.getText().toString();
                        if (to == null) {
                            Log.e(TAG, "to string is " + to);
                            break;
                        }

                        callID = Integer.toString(random.nextInt());

                        Log.d(TAG, "service.placeCall(" + accountID + ", " + callID + ", " + to + ");");
                        service.placeCall(accountID, callID, to);
                        callOnGoing = true;
                        buttonCall.setEnabled(false);
                        buttonHangup.setEnabled(true);
                    }
                }
                break;
            case R.id.buttonHangUp:
                if (incomingCallID != "") {
                    buttonCall.clearAnimation();
                    Log.d(TAG, "service.refuse(" + incomingCallID + ");");
                    service.refuse(incomingCallID);
                    incomingCallID="";
                    buttonCall.setEnabled(true);
                    buttonHangup.setEnabled(true);
                } else {
                    if (callOnGoing == true) {
                        Log.d(TAG, "service.hangUp(" + callID + ");");
                        service.hangUp(callID);
                        callOnGoing = false;
                        buttonCall.setEnabled(true);
                        buttonHangup.setEnabled(false);
                    }
                }

                buttonCall.setImageResource(R.drawable.ic_call);
                break;
            case R.id.buttonInit:
                Log.i(TAG, "R.id.buttonInit");
                String audioPlugin = service.getCurrentAudioOutputPlugin();
                Log.i(TAG, "Current Audio Plugin: " + audioPlugin);               
                break;
            case R.id.buttonService:
                if (!serviceIsOn) {
                    startService(new Intent(this, SipService.class));
                    serviceIsOn = true;
                    buttonService.setText("disable Service");
                }
                else {
                    stopService(new Intent(this, SipService.class));
                    serviceIsOn = false;
                    buttonService.setText("enable Service");
                }
                break;
            default:
                Log.w(TAG, "unknown button " + view.getId());
                break;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }
}
