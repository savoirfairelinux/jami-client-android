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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;

public class SFLPhoneHome extends Activity implements ActionBar.TabListener
{
	SectionsPagerAdapter mSectionsPagerAdapter;
	private static final String TAG = "SFLPhoneHome";

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
				fragment = new ButtonSectionFragment();
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

	public static class ButtonSectionFragment extends Fragment implements OnClickListener
	{ 
		public ButtonSectionFragment()
		{
			setRetainInstance(true);
		}

		public static final String ARG_SECTION_NUMBER = "section_number";
		public static final OnClickListener myListener = new OnClickListener() {
			@Override
		    public void onClick(View view)
		    {
		    	switch (view.getId()) {
		    	case R.id.buttonCall:
		        	ManagerImpl.outgoingCallJ("");
		        	break;
		    	case R.id.buttonInit:
		    		ManagerImpl.initN("");
		    		break;
		    	case R.id.buttonTest1:
		    		Log.i(TAG, "buttonTest1");
		    		break;
		        default:
		    		Log.w(TAG, "unknown button " + view.getId());
		        	break;
		    	}
	    	}
		};

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
		{
			View view;
			Button buttonInit, buttonCall, buttonTest1;
			
			Log.i(TAG, "onCreateView" );
			view = inflater.inflate(R.layout.test_layout, parent, false);
			
			buttonInit = (Button) view.findViewById(R.id.buttonInit);
			if (buttonInit == null)
	        	Log.e(TAG, "buttonInit is " + buttonInit);
			buttonInit.setOnClickListener(myListener);
			
	        buttonCall = (Button) view.findViewById(R.id.buttonCall);
			if (buttonCall == null)
	        	Log.e(TAG, "buttonCall is " + buttonCall);
			buttonCall.setOnClickListener(myListener);

			buttonTest1 = (Button) view.findViewById(R.id.buttonTest1);
			if (buttonTest1 == null)
	        	Log.e(TAG, "buttonTest1 is " + buttonTest1);
			buttonTest1.setOnClickListener(myListener);
			
//			buttonInit.setGravity(Gravity.CENTER);
//			buttonInit.setText("init");
//			buttonInit.setOnClickListener(this);
			//TextView textView = new TextView(getActivity());
			//textView.setGravity(Gravity.CENTER);
			//Bundle args = getArguments();
			//textView.setText(Integer.toString(args.getInt(ARG_SECTION_NUMBER)));
			//textView.setText("java sucks");
			if (parent == null)
				Log.e(TAG, "parent is " + parent);
			if (R.layout.test_layout == 0)
				Log.e(TAG, "buttonInit = " + R.layout.test_layout);
			try {
				inflater.inflate(R.layout.test_layout, parent, false);
			} catch (InflateException e) {
				Log.e(TAG, "Error inflating test_layout ", e);
				return null;
			}
			return view;
		}
		
		@Override
	    public void onClick(View view)
	    {
    		Log.d(TAG, "onClick ");
    	}
    }

	public String getAppPath() {
		PackageManager m = getPackageManager();
		String s = getPackageName();
		Log.d(TAG, "Application path: " + s);
		try {
			PackageInfo p = m.getPackageInfo(s, 0);
			s = p.applicationInfo.dataDir;
		} catch (NameNotFoundException e) {
			Log.w(TAG, "Error Package name not found ", e);
		}
		return s;
	}
}
