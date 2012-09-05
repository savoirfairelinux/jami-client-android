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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.support.v4.view.ViewPager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;

public class SFLPhonePreferenceActivity extends Activity implements ActionBar.TabListener
{
    static final int NUM_PAGES = 2;
    static final String TAG = "SFLPhonePreferenceActivity";
    PreferencesPagerAdapter mPreferencesPagerAdapter;
    ViewPager mViewPager;

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
            return ArrayListFragment.newInstance(position);
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
