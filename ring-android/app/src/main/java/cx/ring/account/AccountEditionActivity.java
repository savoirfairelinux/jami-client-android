/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.contactrequests.BlackListFragment;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.mvp.BaseSupportFragment;

public class AccountEditionActivity extends BaseSupportFragment<AccountEditionPresenter> implements AccountEditionView {

    public static final String ACCOUNT_ID_KEY = AccountEditionActivity.class.getCanonicalName() + "accountid";
    public static final String ACCOUNT_HAS_PASSWORD_KEY = AccountEditionActivity.class.getCanonicalName() + "hasPassword";

    public static final String TAG = AccountEditionActivity.class.getSimpleName();
    public static final String ACCOUNT_ID = TAG + "accountID";

//    @Inject
//    protected AccountEditionPresenter mEditionPresenter;

    @BindView(R.id.pager)
    protected ViewPager mViewPager;

//    @BindView(R.id.sliding_tabs)
//    protected TabLayout mSlidingTabLayout;

    @BindView(R.id.fragment_container)
    protected FrameLayout frameLayout;

    private MenuItem mItemAdvanced;
    private MenuItem mItemBlacklist;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        String accountId = getArguments().getString(ACCOUNT_ID);

        presenter.init(accountId);
    }

    //    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.activity_account_settings);
//
//        ButterKnife.bind(this);
//
//        // dependency injection
//        JamiApplication.getInstance().getRingInjectionComponent().inject(this);
//        mEditionPresenter.bindView(this);
//        String accountId = getIntent().getData().getLastPathSegment();
//        mEditionPresenter.init(accountId);
//    }

    @Override
    public void displaySummary(String accountId) {
//        mSlidingTabLayout.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
        RingAccountSummaryFragment ringAccountSummaryFragment = new RingAccountSummaryFragment();
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        ringAccountSummaryFragment.setArguments(args);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ringAccountSummaryFragment, RingAccountSummaryFragment.TAG)
                .commit();
    }

    @Override
    public void initViewPager(String accountId, boolean isRing) {
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setAdapter(new PreferencesPagerAdapter(getActivity().getSupportFragmentManager(), getActivity(), accountId, isRing));

//        mSlidingTabLayout.setupWithViewPager(mViewPager);
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

    @Override
    public void goToBlackList(String accountId) {
        BlackListFragment blackListFragment = new BlackListFragment();
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        blackListFragment.setArguments(args);
        getActivity().getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(BlackListFragment.TAG)
                .replace(R.id.fragment_container, blackListFragment, BlackListFragment.TAG)
                .commit();
//        mSlidingTabLayout.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
        frameLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_edition, menu);
        mItemAdvanced = menu.findItem(R.id.menuitem_advanced);
        mItemBlacklist = menu.findItem(R.id.menuitem_blacklist);
//        return true;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        presenter.prepareOptionsMenu();
//        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unbindView();
    }

//    @Override
//    public void onBackPressed() {
//        if (frameLayout.getVisibility() == View.VISIBLE) {
//            super.onBackPressed();
//        } else {
//            frameLayout.setVisibility(View.VISIBLE);
//            mViewPager.setVisibility(View.GONE);
//            mSlidingTabLayout.setVisibility(View.GONE);
//        }
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//                onBackPressed();
                return true;
            case R.id.menuitem_delete:
                AlertDialog deleteDialog = createDeleteDialog();
                deleteDialog.show();
                break;
            case R.id.menuitem_advanced:
//                mSlidingTabLayout.setVisibility(View.VISIBLE);
                mViewPager.setVisibility(View.VISIBLE);
                frameLayout.setVisibility(View.GONE);
                break;
            case R.id.menuitem_blacklist:
                presenter.goToBlackList();
            default:
                break;
        }
        return true;
    }

    @NonNull
    private AlertDialog createDeleteDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(getActivity())
                .setMessage(R.string.account_delete_dialog_message)
                .setTitle(R.string.account_delete_dialog_title)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> presenter.removeAccount())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.setOwnerActivity(getActivity());
        return alertDialog;
    }

    @Override
    public void goToWizardActivity() {
        Intent intent = new Intent(getActivity(), AccountWizardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void exit() {
//        finish();
    }

    @Override
    public void displayAccountName(final String name) {
//        Toolbar toolbar = findViewById(R.id.main_toolbar);
//        setSupportActionBar(toolbar);
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setTitle(name);
//        }
    }

    @Override
    public int getLayout() {
        return R.layout.activity_account_settings;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    private static class PreferencesPagerAdapter extends FragmentPagerAdapter {
        private Context mContext;
        private String accountId;
        private boolean isRing;

        public PreferencesPagerAdapter(FragmentManager fm, Context mContext, String accountId, boolean isRing) {
            super(fm);
            this.mContext = mContext;
            this.accountId = accountId;
            this.isRing = isRing;
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

        @Override
        public int getCount() {
            return isRing ? 3 : 4;
        }

        @Override
        public Fragment getItem(int position) {
            return isRing ? getRingPanel(position) : getSIPPanel(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int resId = isRing ? getRingPanelTitle(position) : getSIPPanelTitle(position);
            return mContext.getString(resId);
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
                    return GeneralAccountFragment.newInstance(accountId);
                case 1:
                    return MediaPreferenceFragment.newInstance(accountId);
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
            args.putString(ACCOUNT_ID_KEY, accountId);
            result.setArguments(args);
            return result;
        }
    }
}
