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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;

public class SFLPhoneHome extends Activity implements ActionBar.TabListener, OnClickListener
{
	SectionsPagerAdapter mSectionsPagerAdapter;
	static final String TAG = "SFLPhoneHome";
	private ButtonSectionFragment buttonFragment;
	ImageButton buttonCall, buttonHangup;
	Handler callbackHandler;
	static ManagerImpl managerImpl;
	/* default callID */
	static String callID = "007";
	static boolean callOnGoing = false;
	private String incomingCallID = "";

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
	final private int[] icon_res_id = {R.drawable.ic_tab_call, R.drawable.ic_tab_history, R.drawable.ic_tab_play_selected};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sflphone_home);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

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
		ManagerImpl.setAppPath(getAppPath());
		managerImpl = new ManagerImpl(callbackHandler);
		managerImpl.setActivity(this);
		Log.i(TAG, "managerImpl created with callbackHandler " + callbackHandler);

		buttonCall = (ImageButton) findViewById(R.id.buttonCall);
		buttonHangup = (ImageButton) findViewById(R.id.buttonHangUp);
//		buttonIncomingCall = (ImageButton) findViewById(R.id.buttonIncomingCall);
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
//		ManagerImpl.initN("");
	}

	public void setIncomingCallID(String id) {
		incomingCallID = id;
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
				fragment = new CallElementList();
				break;
			case 1:
				fragment = new DummySectionFragment();
				break;
			case 2:
				buttonFragment = new ButtonSectionFragment();
				Log.i(TAG, "getItem: fragment is " + buttonFragment);
				fragment = buttonFragment;
				managerImpl.setButtonFragment(buttonFragment);
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
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase();
			case 1:
				return getString(R.string.title_section2).toUpperCase();
			case 2:
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
		buttonCall = buttonFragment.getCallButton();
		buttonHangup = buttonFragment.getHangUpButton();
		
    	switch (view.getId()) {
    	case R.id.buttonCall:
    		TextView textView = (TextView) findViewById(R.id.editAccountID);
    		String accountID = textView.getText().toString();
    		EditText editText;
    		Random random = new Random();

    		if (incomingCallID != "") {
    			buttonCall.clearAnimation();
    			ManagerImpl.answerCall(incomingCallID);
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

    				/* new random callID */
    				callID = Integer.toString(random.nextInt());

    				Log.d(TAG, "ManagerImpl.placeCall(" + accountID + ", " + callID + ", " + to + ");");
    				ManagerImpl.placeCall(accountID, callID, to);
    				callOnGoing = true;
    				buttonCall.setEnabled(false);
    				buttonHangup.setEnabled(true);
    			}
    		}
        	break;
    	case R.id.buttonHangUp:
    		if (incomingCallID != "") {
    			buttonCall.clearAnimation();
    			ManagerImpl.refuseCall(incomingCallID);
    			incomingCallID="";
				buttonCall.setEnabled(true);
				buttonHangup.setEnabled(true);
    		} else {
    			if (callOnGoing == true) {
    				Log.d(TAG, "ManagerImpl.hangUp(" + callID + ");");
    				ManagerImpl.hangUp(callID);
    				callOnGoing = false;
    				buttonCall.setEnabled(true);
    				buttonHangup.setEnabled(false);
    			}
    		}

			buttonCall.setImageResource(R.drawable.ic_call);
    		break;
    	case R.id.buttonInit:
    		ManagerImpl.initN("");
    		break;
    	case R.id.buttonCallVoid:
    		ManagerImpl.callVoid();
        	break;
    	case R.id.buttonGetNewData:
    		Data d = ManagerImpl.getNewData(42, "foo");
    		if (d != null)
    			buttonFragment.getNewDataText().setText("getNewData(42, \"foo\") == Data(" + d.i + ", \"" + d.s + "\")");
    		break;
    	case R.id.buttonGetDataString:
    		Data daita = new Data(43, "bar");
    		String s = ManagerImpl.getDataString(daita);
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
