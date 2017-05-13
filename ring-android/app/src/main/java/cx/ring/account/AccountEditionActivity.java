/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.account;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.contactrequests.BlackListFragment;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.Account;

public class AccountEditionActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener, AccountEditionView {

    @Inject
    AccountEditionPresenter mEditionPresenter;

    public static final String ACCOUNTID_KEY = AccountEditionActivity.class.getCanonicalName() + "accountid";

    private static final String TAG = AccountEditionActivity.class.getSimpleName();

    private Fragment mCurrentlyDisplayed;

    @BindView(R.id.pager)
    ViewPager mViewPager = null;

    @BindView(R.id.sliding_tabs)
    TabLayout mSlidingTabLayout = null;

    private MenuItem mItemAdvanced;
    private MenuItem mItemBlacklist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_settings);

        ButterKnife.bind(this);

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
        mEditionPresenter.bindView(this);
        String accountId = getIntent().getData().getLastPathSegment();
        mEditionPresenter.init(accountId);

        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setAdapter(new PreferencesPagerAdapter(getFragmentManager(), AccountEditionActivity.this, mEditionPresenter.getAccount()));
        mViewPager.addOnPageChangeListener(this);

        mSlidingTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onDestroy() {
        mViewPager.removeOnPageChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        PreferencesPagerAdapter adapter = (PreferencesPagerAdapter) mViewPager.getAdapter();
        mCurrentlyDisplayed = adapter.getItem(position);
    }

    @Override
    public void onPageSelected(int position) {
        if (mCurrentlyDisplayed instanceof RingAccountSummaryFragment) {
            RingAccountSummaryFragment deviceAccountFragment = (RingAccountSummaryFragment) mCurrentlyDisplayed;
            if (deviceAccountFragment.isDisplayingWizard()) {
                deviceAccountFragment.hideWizard();
            }
        }
        PreferencesPagerAdapter adapter = (PreferencesPagerAdapter) mViewPager.getAdapter();
        mCurrentlyDisplayed = adapter.getItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void displaySummary(Account selected) {
        mSlidingTabLayout.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
        mCurrentlyDisplayed = new RingAccountSummaryFragment();
        Bundle args = new Bundle();
        args.putString(ACCOUNTID_KEY, selected.getAccountID());
        mCurrentlyDisplayed.setArguments(args);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mCurrentlyDisplayed, RingAccountSummaryFragment.TAG)
                .commit();
    }

    @Override
    public void showAdvancedOption(boolean show) {
        if (mItemAdvanced != null) {
            mItemAdvanced.setVisible(show);
        }
    }

    @Override
    public void showBlacklistOption(boolean show) {
        if (mItemBlacklist != null) {
            mItemBlacklist.setVisible(show);
        }
    }

    private boolean isAdvancedSettings() {
        return mSlidingTabLayout.getVisibility() == View.VISIBLE;
    }

    private boolean isBlackList() {
        Fragment fragment = getFragmentManager().findFragmentByTag(BlackListFragment.TAG);
        return fragment != null && fragment.isVisible();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_edition, menu);
        mItemAdvanced = menu.findItem(R.id.menuitem_advanced);
        mItemBlacklist = menu.findItem(R.id.menuitem_blacklist);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mEditionPresenter.prepareOptionsMenu();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEditionPresenter.bindView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mEditionPresenter.unbindView();
    }

    @Override
    public void onBackPressed() {
        if ((isAdvancedSettings() && mEditionPresenter.getAccount().isRing()) || isBlackList()) {
            displaySummary(mEditionPresenter.getAccount());
        } else if (!(mCurrentlyDisplayed instanceof BackHandlerInterface) || !((BackHandlerInterface) mCurrentlyDisplayed).onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menuitem_delete:
                AlertDialog deleteDialog = createDeleteDialog();
                deleteDialog.show();
                break;
            case R.id.menuitem_advanced:
                mSlidingTabLayout.setVisibility(View.VISIBLE);
                mViewPager.setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().remove(mCurrentlyDisplayed).commit();
                break;
            case R.id.menuitem_blacklist:
                mSlidingTabLayout.setVisibility(View.GONE);
                mViewPager.setVisibility(View.GONE);
                mCurrentlyDisplayed = new BlackListFragment();
                Bundle args = new Bundle();
                args.putString(ACCOUNTID_KEY, mEditionPresenter.getAccount().getAccountID());
                mCurrentlyDisplayed.setArguments(args);
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.fragment_container, mCurrentlyDisplayed, BlackListFragment.TAG)
                        .commit();
            default:
                break;
        }
        return true;
    }

    @NonNull
    private AlertDialog createDeleteDialog() {
        Activity ownerActivity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        builder.setMessage(R.string.account_delete_dialog_message).setTitle(R.string.account_delete_dialog_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mEditionPresenter.removeAccount();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog alertDialog = builder.create();
        alertDialog.setOwnerActivity(ownerActivity);
        return alertDialog;
    }

    @Override
    public void exit() {
        finish();
    }

    @Override
    public void displayAccountName(final String name) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
                setSupportActionBar(toolbar);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setTitle(name);
                }
            }
        });

    }

    private static class PreferencesPagerAdapter extends FragmentPagerAdapter {
        Account mAccount;
        private Context ctx;

        PreferencesPagerAdapter(FragmentManager fm, Context c, Account account) {
            super(fm);
            ctx = c;
            mAccount = account;
        }

        @Override
        public int getCount() {
            return mAccount.isRing() ? 3 : 4;
        }

        @Override
        public Fragment getItem(int position) {
            return mAccount.isRing() ? getRingPanel(position) : getSIPPanel(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int resId = mAccount.isRing() ? getRingPanelTitle(position) : getSIPPanelTitle(position);
            return ctx.getString(resId);
        }

        @Nullable
        private Fragment getRingPanel(int position) {
            switch (position) {
                case 0:
                    return fragmentWithBundle(new GeneralAccountFragment());
                case 1:
                    return fragmentWithBundle(new MediaPreferenceFragment());
                case 2:
                    return fragmentWithBundle(new AdvancedAccountFragment());
                default:
                    return null;
            }
        }

        @Nullable
        private Fragment getSIPPanel(int position) {
            switch (position) {
                case 0:
                    return fragmentWithBundle(new GeneralAccountFragment());
                case 1:
                    return fragmentWithBundle(new MediaPreferenceFragment());
                case 2:
                    return fragmentWithBundle(new AdvancedAccountFragment());
                case 3:
                    return fragmentWithBundle(new SecurityAccountFragment());
                default:
                    return null;
            }
        }

        private Fragment fragmentWithBundle(Fragment result) {
            Bundle args = new Bundle();
            args.putString(ACCOUNTID_KEY, mAccount.getAccountID());
            result.setArguments(args);
            return result;
        }

        @StringRes
        private static int getRingPanelTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.account_preferences_basic_tab;
                case 1:
                    return R.string.account_preferences_media_tab;
                case 2:
                    return R.string.account_preferences_advanced_tab;
                default:
                    return -1;
            }
        }

        @StringRes
        private static int getSIPPanelTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.account_preferences_basic_tab;
                case 1:
                    return R.string.account_preferences_media_tab;
                case 2:
                    return R.string.account_preferences_advanced_tab;
                case 3:
                    return R.string.account_preferences_security_tab;
                default:
                    return -1;
            }
        }
    }
}
