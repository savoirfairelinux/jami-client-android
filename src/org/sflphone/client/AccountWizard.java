/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package org.sflphone.client;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import org.sflphone.R;
import org.sflphone.fragments.AccountCreationFragment;
import org.sflphone.fragments.AccountCreationFragment.Callbacks;
import org.sflphone.service.ISipService;
import org.sflphone.service.SipService;

import java.util.ArrayList;
import java.util.Locale;

public class AccountWizard extends Activity implements Callbacks {
    static final String TAG = "AccountWizard";
    private boolean mBound = false;
    private ISipService service;
    ViewPager mViewPager;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wizard);
        mViewPager = (ViewPager) findViewById(R.id.pager);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(AccountWizard.this, getFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);

        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, SipService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

    }

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return true;
        }
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        Context mContext;
        ArrayList<Fragment> fragments;

        public SectionsPagerAdapter(Context c, FragmentManager fm) {
            super(fm);
            mContext = c;
            fragments = new ArrayList<Fragment>();
            fragments.add(new AccountCreationFragment());

        }

        @Override
        public Fragment getItem(int i) {

            return fragments.get(i);
        }

        public String getClassName(int i) {
            String name;

            switch (i) {
            case 0:
                name = AccountCreationFragment.class.getName();
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
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return mContext.getString(R.string.title_section0).toUpperCase(Locale.getDefault());
            default:
                Log.e(TAG, "getPageTitle: unknown tab position " + position);
                break;
            }
            return null;
        }
    }

    @Override
    public ISipService getService() {
        return service;
    }

}
