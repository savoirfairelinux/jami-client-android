/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
 *          AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.BlockListFragment;
import cx.ring.databinding.FragAccountSettingsBinding;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.DeviceUtils;

public class AccountEditionFragment extends BaseSupportFragment<AccountEditionPresenter> implements
        BackHandlerInterface,
        AccountEditionView,
        ViewTreeObserver.OnScrollChangedListener  {
    private static final String TAG = AccountEditionFragment.class.getSimpleName();

    public static final String ACCOUNT_ID_KEY = AccountEditionFragment.class.getCanonicalName() + "accountid";
    static final String ACCOUNT_HAS_PASSWORD_KEY = AccountEditionFragment.class.getCanonicalName() + "hasPassword";
    public static final String ACCOUNT_ID = TAG + "accountID";

    private static final int SCROLL_DIRECTION_UP = -1;

    private FragAccountSettingsBinding mBinding;

    private boolean mIsVisible;

    private String mAccountId;
    private boolean mAccountIsJami;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccountSettingsBinding.inflate(inflater, container, false);
        // dependency injection
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        mAccountId = getArguments().getString(ACCOUNT_ID);

        HomeActivity activity = (HomeActivity) getActivity();
        if (activity != null && DeviceUtils.isTablet(activity)) {
            activity.setTabletTitle(R.string.navigation_item_account);
        }

        mBinding.fragmentContainer.getViewTreeObserver().addOnScrollChangedListener(this);

        presenter.init(mAccountId);
    }

    @Override
    public void displaySummary(String accountId) {
        toggleView(accountId, true);
        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(JamiAccountSummaryFragment.TAG);
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        if (existingFragment == null) {
            JamiAccountSummaryFragment fragment = new JamiAccountSummaryFragment();
            fragment.setArguments(args);
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment, JamiAccountSummaryFragment.TAG)
                    .commit();
        } else {
            if (!existingFragment.isStateSaved())
                existingFragment.setArguments(args);
            ((JamiAccountSummaryFragment) existingFragment).setAccount(accountId);
        }
    }

    @Override
    public void displaySIPView(String accountId) {
        toggleView(accountId, false);
    }

    @Override
    public void initViewPager(String accountId, boolean isJami) {
        mBinding.pager.setOffscreenPageLimit(4);
        mBinding.slidingTabs.setupWithViewPager(mBinding.pager);
        mBinding.pager.setAdapter(new PreferencesPagerAdapter(getChildFragmentManager(), getActivity(), accountId, isJami));
        BlockListFragment existingFragment = (BlockListFragment) getChildFragmentManager().findFragmentByTag(BlockListFragment.TAG);
        if (existingFragment != null) {
            Bundle args = new Bundle();
            args.putString(ACCOUNT_ID_KEY, accountId);
            if (!existingFragment.isStateSaved())
                existingFragment.setArguments(args);
            existingFragment.setAccount(accountId);
        }
    }

    @Override
    public void goToBlackList(String accountId) {
        BlockListFragment blockListFragment = new BlockListFragment();
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        blockListFragment.setArguments(args);
        getChildFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(BlockListFragment.TAG)
                .replace(R.id.fragment_container, blockListFragment, BlockListFragment.TAG)
                .commit();
        mBinding.slidingTabs.setVisibility(View.GONE);
        mBinding.pager.setVisibility(View.GONE);
        mBinding.fragmentContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        setBackListenerEnabled(false);
    }

    public boolean onBackPressed() {
        if (mBinding == null)
            return false;
        mIsVisible = false;

        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).setToolbarOutlineState(true);
        if (mBinding.fragmentContainer.getVisibility() != View.VISIBLE) {
            toggleView(mAccountId, mAccountIsJami);
            return true;
        }
        JamiAccountSummaryFragment summaryFragment = (JamiAccountSummaryFragment) getChildFragmentManager().findFragmentByTag(JamiAccountSummaryFragment.TAG);
        if (summaryFragment != null && summaryFragment.onBackPressed()) {
            return true;
        }
        return getChildFragmentManager().popBackStackImmediate();
    }

    private void toggleView(String accountId, boolean isJami) {
        mAccountId = accountId;
        mAccountIsJami = isJami;
        mBinding.slidingTabs.setVisibility(isJami? View.GONE : View.VISIBLE);
        mBinding.pager.setVisibility(isJami? View.GONE : View.VISIBLE);
        mBinding.fragmentContainer.setVisibility(isJami? View.VISIBLE : View.GONE);
        setBackListenerEnabled(isJami);
    }

    @Override
    public void exit() {
        Activity activity = getActivity();
        if (activity != null)
            activity.onBackPressed();
    }

    private void setBackListenerEnabled(boolean enable) {
        Activity activity = getActivity();
        if (activity instanceof HomeActivity)
            ((HomeActivity) activity).setAccountFragmentOnBackPressedListener(enable ? this : null);
    }

    private static class PreferencesPagerAdapter extends FragmentStatePagerAdapter {
        private final Context mContext;
        private final String accountId;
        private final boolean isJamiAccount;

        PreferencesPagerAdapter(FragmentManager fm, Context mContext, String accountId, boolean isJamiAccount) {
            super(fm);
            this.mContext = mContext;
            this.accountId = accountId;
            this.isJamiAccount = isJamiAccount;
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
            return isJamiAccount ? 3 : 4;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return isJamiAccount ? getJamiPanel(position) : getSIPPanel(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int resId = isJamiAccount ? getRingPanelTitle(position) : getSIPPanelTitle(position);
            return mContext.getString(resId);
        }

        @NonNull
        private Fragment getJamiPanel(int position) {
            switch (position) {
                case 0:
                    return fragmentWithBundle(new GeneralAccountFragment());
                case 1:
                    return fragmentWithBundle(new MediaPreferenceFragment());
                case 2:
                    return fragmentWithBundle(new AdvancedAccountFragment());
                default:
                    throw new IllegalArgumentException();
            }
        }

        @NonNull
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
                    throw new IllegalArgumentException();
            }
        }

        private Fragment fragmentWithBundle(Fragment result) {
            Bundle args = new Bundle();
            args.putString(ACCOUNT_ID_KEY, accountId);
            result.setArguments(args);
            return result;
        }
    }

    @Override
    public void onScrollChanged() {
        setupElevation();
    }

    private void setupElevation() {
        if (mBinding == null || !mIsVisible) {
            return;
        }
        Activity activity = getActivity();
        if (!(activity instanceof HomeActivity))
            return;
        LinearLayout ll = (LinearLayout) mBinding.pager.getChildAt(mBinding.pager.getCurrentItem());
        if (ll == null) return;
        RecyclerView rv = (RecyclerView)((FrameLayout) ll.getChildAt(0)).getChildAt(0);
        if (rv == null) return;
        HomeActivity homeActivity = (HomeActivity) activity;
        if (rv.canScrollVertically(SCROLL_DIRECTION_UP)) {
            mBinding.slidingTabs.setElevation(mBinding.slidingTabs.getResources().getDimension(R.dimen.toolbar_elevation));
            homeActivity.setToolbarElevation(true);
            homeActivity.setToolbarOutlineState(false);
        } else {
            mBinding.slidingTabs.setElevation(0);
            homeActivity.setToolbarElevation(false);
            homeActivity.setToolbarOutlineState(true);
        }
    }
}
