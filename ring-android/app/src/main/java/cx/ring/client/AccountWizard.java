/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.client;

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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.fragments.AccountCreationFragment;
import cx.ring.fragments.AccountMigrationFragment;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

public class AccountWizard extends AppCompatActivity implements LocalService.Callbacks {
    static final String TAG = AccountWizard.class.getName();
    private boolean mBound = false;
    private LocalService service;

    @BindView(R.id.pager)
    ViewPager mViewPager;

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((LocalService.LocalBinder) binder).getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // nothing to be done here
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (getIntent().getData() != null && !TextUtils.isEmpty(getIntent().getData().getLastPathSegment())) {
            String accountId = getIntent().getData().getLastPathSegment();
            SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(AccountWizard.this, getFragmentManager(), accountId);
            mViewPager.setAdapter(mSectionsPagerAdapter);
        } else {
            SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(AccountWizard.this, getFragmentManager());
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }

        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
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
            checkAccountPresence();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Ensures that the user has at least one account when exiting this Activity
     * If not, exit the app
     */
    private void checkAccountPresence() {
        if (mBound && !service.getAccounts().isEmpty()) {
            finish();
        } else {
            service.stopSelf();
            finishAffinity();
        }
    }

    @Override
    public void onBackPressed() {
        checkAccountPresence();
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private final Context mContext;
        private final ArrayList<Fragment> fragments;
        private final String mAccountId;

        public SectionsPagerAdapter(Context c, FragmentManager fm) {
            this(c, fm, null);
        }

        public SectionsPagerAdapter(Context c, FragmentManager fm, String accountId) {
            super(fm);
            mContext = c;
            fragments = new ArrayList<>();
            mAccountId = accountId;

            if (TextUtils.isEmpty(mAccountId)) {
                fragments.add(new AccountCreationFragment());
            } else {
                AccountMigrationFragment fragment = new AccountMigrationFragment();
                // give the installation id to display
                Bundle bundle = new Bundle();
                bundle.putString(AccountMigrationFragment.ACCOUNT_ID, mAccountId);
                fragment.setArguments(bundle);
                fragments.add(fragment);
            }
        }

        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);
        }

        public String getClassName(int i) {
            String name;

            switch (i) {
                case 0:
                    if (TextUtils.isEmpty(mAccountId)) {
                        name = AccountCreationFragment.class.getName();
                    } else {
                        name = AccountMigrationFragment.class.getName();
                    }
                    break;

                default:
                    Log.e(TAG, "getClassName: unknown fragment position " + i);
                    return null;
            }

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
    public IDRingService getRemoteService() {
        return service.getRemoteService();
    }

    @Override
    public LocalService getService() {
        return service;
    }

}
