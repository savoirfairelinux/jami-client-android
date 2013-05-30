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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.SectionsPagerAdapter;
import com.savoirfairelinux.sflphone.client.receiver.CallReceiver;
import com.savoirfairelinux.sflphone.fragments.CallElementListFragment;
import com.savoirfairelinux.sflphone.fragments.ContactListFragment;
import com.savoirfairelinux.sflphone.fragments.DialingFragment;
import com.savoirfairelinux.sflphone.fragments.HistoryFragment;
import com.savoirfairelinux.sflphone.fragments.MenuFragment;
import com.savoirfairelinux.sflphone.interfaces.CallInterface;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.views.CustomSlidingDrawer;

public class SFLPhoneHomeActivity extends Activity implements ActionBar.TabListener, DialingFragment.Callbacks, ContactListFragment.Callbacks,
CallElementListFragment.Callbacks, HistoryFragment.Callbacks, CallInterface {

	SectionsPagerAdapter mSectionsPagerAdapter = null;
	static final String TAG = "SFLPhoneHomeActivity";

	private ContactListFragment mContactsFragment = null;
	private DialingFragment mDialingFragment = null;
	private CallElementListFragment mCallElementList = null;
	private HistoryFragment mHistorySectionFragment = null;

	Fragment fMenu;

	private boolean mBound = false;
	private ISipService service;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private static final int REQUEST_CODE_PREFERENCES = 1;
	private static final int REQUEST_CODE_CALL = 2;

	private static final int ACTION_BAR_TAB_DIALING = 0;
	private static final int ACTION_BAR_TAB_CALL = 1;
	private static final int ACTION_BAR_TAB_HISTORY = 2;

	RelativeLayout mSliderButton;
	CustomSlidingDrawer mDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	CallReceiver receiver;

	final private int[] icon_res_id = { R.drawable.ic_tab_call, R.drawable.ic_tab_call, R.drawable.ic_tab_history };

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

		receiver = new CallReceiver(this);

		String libraryPath = getApplicationInfo().dataDir + "/lib";
		Log.i(TAG, libraryPath);

		// Bind to LocalService
		if (!mBound) {
			Log.i(TAG, "onStart: Binding service...");
			Intent intent = new Intent(this, SipService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}

		setContentView(R.layout.activity_sflphone_home);

		if (mSectionsPagerAdapter == null) {
			mSectionsPagerAdapter = new SectionsPagerAdapter(this, getFragmentManager());
		}

		/* getFragment(Bundle, String) */
		if (savedInstanceState != null) {
			Log.w(TAG, "Activity restarted, recreating PagerAdapter...");
			/* getFragment (Bundle bundle, String key) */
			mDialingFragment = (DialingFragment) getFragmentManager().getFragment(savedInstanceState,
					mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_DIALING));
			mCallElementList = (CallElementListFragment) getFragmentManager().getFragment(savedInstanceState,
					mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_CALL));
			mHistorySectionFragment = (HistoryFragment) getFragmentManager().getFragment(savedInstanceState,
					mSectionsPagerAdapter.getClassName(ACTION_BAR_TAB_HISTORY));
		}

		if (mDialingFragment == null) {
			mDialingFragment = new DialingFragment();
			Log.w(TAG, "Recreated mDialingFragment=" + mDialingFragment);
		}

		if (mContactsFragment == null) {
			mContactsFragment = new ContactListFragment();
			Log.w(TAG, "Recreated mContactListFragment=" + mContactsFragment);
			getFragmentManager().beginTransaction().replace(R.id.contacts_frame, mContactsFragment).commit();
		}
		if (mCallElementList == null) {
			mCallElementList = new CallElementListFragment();
			Log.w(TAG, "Recreated mCallElementList=" + mCallElementList);
		}
		if (mHistorySectionFragment == null) {
			mHistorySectionFragment = new HistoryFragment();
			Log.w(TAG, "Recreated mHistorySectionFragment=" + mHistorySectionFragment);
		}

		mDrawer = (CustomSlidingDrawer) findViewById(R.id.custom_sliding_drawer);

		mContactsFragment.setHandleView((RelativeLayout) findViewById(R.id.slider_button));

		mDrawer.setmTrackHandle(findViewById(R.id.handle_title));

		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		mTitle = mDrawerTitle = getTitle();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		// mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// set a custom shadow that overlays the main content when the drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener
		// mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mPlanetTitles));
		// mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

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

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {

			actionBar.addTab(actionBar.newTab().setIcon(icon_res_id[i]).setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}

		actionBar.setSelectedNavigationItem(ACTION_BAR_TAB_CALL);

		fMenu = new MenuFragment();
		getFragmentManager().beginTransaction().replace(R.id.left_drawer, fMenu).commit();
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
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(CallManagerCallBack.INCOMING_CALL);
		intentFilter.addAction(CallManagerCallBack.INCOMING_TEXT);
		intentFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
		registerReceiver(receiver, intentFilter);
		super.onResume();
	}

	/* activity no more in foreground */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
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

			mBound = true;
			mCallElementList.onServiceSipBinded(service);
			mHistorySectionFragment.onServiceSipBinded(service);
			mDialingFragment.onServiceSipBinded(service);
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
			mCallElementList.onServiceSipBinded(service);
			break;
		case REQUEST_CODE_CALL:

			break;
		}

	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// getMenuInflater().inflate(R.menu.activity_sflphone_home, menu);
	// return true;
	// }

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

	@Override
	public void onCallSelected(SipCall c) {
		// launchCallActivity(c.mCallInfo);

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

		mCallElementList.addCall(infos);

		launchCallActivity(infos, CallManagerCallBack.INCOMING_CALL);

	}

	@Override
	public void callStateChanged(Intent callState) {
		Bundle b = callState.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
		String cID = b.getString("CallID");
		String state = b.getString("State");
		Log.i(TAG, "callStateChanged" + cID + "    " + state);
		mCallElementList.updateCall(cID, state);

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
			callBuilder.startCallCreation().setAccountID(service.getAccountList().get(0).toString()).setCallType(SipCall.state.CALL_TYPE_OUTGOING);
		} catch (RemoteException e1) {
			Log.e(TAG,e1.toString());
		}
		callBuilder.addContact(c);

		try {
			launchCallActivity(callBuilder.build(), "call");
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

}
