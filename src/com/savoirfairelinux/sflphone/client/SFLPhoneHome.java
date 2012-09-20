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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.SipService;

public class SFLPhoneHome extends Activity implements ActionBar.TabListener, OnClickListener
{
    SectionsPagerAdapter mSectionsPagerAdapter;
    static final String TAG = "SFLPhoneHome";
    private ButtonSectionFragment buttonFragment;
    Handler callbackHandler;
    private Manager manager;
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

        // FIXME
        callbackHandler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle b = msg.getData();
                TextView callVoidText;

                Log.i(TAG, "handleMessage");

                callVoidText = buttonFragment.getcallVoidText();
                if (callVoidText == null)
                    Log.e(TAG, "SFLPhoneHome: callVoidText is " + callVoidText);
                    callVoidText.setText(b.getString("callback_string"));

                    Log.i(TAG, "handleMessage: " + b.getString("callback_string"));
                }
        };

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
    }

    // FIXME
    static {
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("expat");
        System.loadLibrary("yaml");
        System.loadLibrary("ccgnu2");
        System.loadLibrary("crypto");
        System.loadLibrary("ssl");
        System.loadLibrary("ccrtp1");
        System.loadLibrary("dbus");
        System.loadLibrary("dbus-c++-1");
        System.loadLibrary("samplerate");
        System.loadLibrary("codec_ulaw");
        System.loadLibrary("codec_alaw");
        System.loadLibrary("speexresampler");
        System.loadLibrary("sflphone");
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        Log.i(TAG, "starting SipService");
        startSipService();
    }

    @Override
    protected void onPause() {
        /* stop the service, no need to check if it is running */
        stopService(new Intent(this, SipService.class));
        serviceIsOn = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startSipService() {
        Thread thread = new Thread("StartSFLphoneService") {
            public void run() {
                Intent sipServiceIntent = new Intent(SFLPhoneHome.this, SipService.class);
                //sipServiceIntent.putExtra(ServiceConstants.EXTRA_OUTGOING_ACTIVITY, new ComponentName(SFLPhoneHome.this, SFLPhoneHome.class));
                startService(sipServiceIntent);
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

        // FIXME
        callbackHandler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle b = msg.getData();
                TextView callVoidText;

                Log.i(TAG, "handleMessage");

                callVoidText = buttonFragment.getcallVoidText();
                if (callVoidText == null)
                    Log.e(TAG, "SFLPhoneHome: callVoidText is " + callVoidText);
                callVoidText.setText(b.getString("callback_string"));

                Log.i(TAG, "handleMessage: " + b.getString("callback_string"));
            }
        };

        manager = new Manager(callbackHandler);
        Log.i(TAG, "ManagerImpl::instance() = " + Manager.managerImpl);
        Manager.setActivity(this);
        /* set static AppPath before calling manager.init */
        Manager.managerImpl.setPath(getAppPath());
        Log.i(TAG, "manager created with callbackHandler " + callbackHandler);
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

	public String getAppPath() {
		PackageManager pkgMng = getPackageManager();
		String pkgName = getPackageName();

		try {
			PackageInfo pkgInfo = pkgMng.getPackageInfo(pkgName, 0);
			pkgName = pkgInfo.applicationInfo.dataDir;
		} catch (NameNotFoundException e) {
			Log.w(TAG, "Error Package name not found ", e);
		}

		Log.d(TAG, "Application path: " + pkgName);
		return pkgName;
	}

	@Override
    public void onClick(View view)
    {
        buttonService = (Button) findViewById(R.id.buttonService);
        
    	switch (view.getId()) {
    	case R.id.buttonCall:
    		TextView textView = (TextView) findViewById(R.id.editAccountID);
    		String accountID = textView.getText().toString();
    		EditText editText;
    		Random random = new Random();

    		if (incomingCallID != "") {
    			buttonCall.clearAnimation();
//    			manager.managerImpl.answerCall(incomingCallID);
				manager.callmanagerJNI.accept(incomingCallID);
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

    				Log.d(TAG, "manager.managerImpl.placeCall(" + accountID + ", " + callID + ", " + to + ");");
//    				manager.managerImpl.outgoingCall(accountID, callID, to);
    				manager.callmanagerJNI.placeCall(accountID, callID, to);
    				callOnGoing = true;
    				buttonCall.setEnabled(false);
    				buttonHangup.setEnabled(true);
    			}
    		}
        	break;
    	case R.id.buttonHangUp:
    		if (incomingCallID != "") {
    			buttonCall.clearAnimation();
//    			manager.managerImpl.refuseCall(incomingCallID);
				manager.callmanagerJNI.refuse(incomingCallID);
    			incomingCallID="";
				buttonCall.setEnabled(true);
				buttonHangup.setEnabled(true);
    		} else {
    			if (callOnGoing == true) {
    				Log.d(TAG, "manager.managerImpl.hangUp(" + callID + ");");
//    				manager.managerImpl.hangupCall(callID);
    				manager.callmanagerJNI.hangUp(callID);
    				callOnGoing = false;
    				buttonCall.setEnabled(true);
    				buttonHangup.setEnabled(false);
    			}
    		}

			buttonCall.setImageResource(R.drawable.ic_call);
    		break;
    	case R.id.buttonInit:
    		Manager.managerImpl.setPath("");
    		Manager.managerImpl.init("");
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
    	case R.id.buttonCallVoid:
    		Manager.callVoid();
        	break;
    	case R.id.buttonGetNewData:
    		Data d = Manager.getNewData(42, "foo");
    		if (d != null)
    			buttonFragment.getNewDataText().setText("getNewData(42, \"foo\") == Data(" + d.i + ", \"" + d.s + "\")");
    		break;
    	case R.id.buttonGetDataString:
    		Data daita = new Data(43, "bar");
    		String s = Manager.getDataString(daita);
    		if (s != "") {
    			buttonFragment.getDataStringText().setText("getDataString(Data(43, \"bar\")) == \"" + s + "\"");
    		}
        	break;
        default:
    		Log.w(TAG, "unknown button " + view.getId());
        	break;
    	}
	}
}
