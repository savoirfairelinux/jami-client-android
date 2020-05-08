/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.BlackListFragment;
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

    private FragAccountSettingsBinding binding;

    private boolean mIsVisible;

    private MenuItem mItemAdvanced;
    private MenuItem mItemBlacklist;

    private String mAccountId;
    private boolean mAccountIsJami;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccountSettingsBinding.inflate(inflater, container, false);
        // dependency injection
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        mAccountId = getArguments().getString(ACCOUNT_ID);

        if (DeviceUtils.isTablet(getContext()) && getActivity() != null) {
            Toolbar toolbar = getActivity().findViewById(R.id.main_toolbar);
            TextView title = toolbar.findViewById(R.id.contact_title);
            ImageView logo = toolbar.findViewById(R.id.contact_image);

            logo.setVisibility(View.GONE);
            title.setText(R.string.navigation_item_account);
            title.setTextSize(19);
            title.setTypeface(null, Typeface.BOLD);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) title.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_TOP);
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            title.setLayoutParams(params);
        }

        binding.fragmentContainer.getViewTreeObserver().addOnScrollChangedListener(this);

        presenter.init(mAccountId);
    }

    @Override
    public void displaySummary(String accountId) {
        toggleView(accountId, true);
        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(JamiAccountSummaryFragment.TAG);
        if (existingFragment == null) {
            JamiAccountSummaryFragment fragment = new JamiAccountSummaryFragment();
            Bundle args = new Bundle();
            args.putString(ACCOUNT_ID_KEY, accountId);
            fragment.setArguments(args);
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment, JamiAccountSummaryFragment.TAG)
                    .commit();
        } else {
            ((JamiAccountSummaryFragment) existingFragment).setAccount(accountId);
        }
    }

    @Override
    public void displaySIPView(String accountId) {
        toggleView(accountId, false);
    }

    @Override
    public void initViewPager(String accountId, boolean isJami) {
        binding.pager.setOffscreenPageLimit(4);
        binding.slidingTabs.setupWithViewPager(binding.pager);
        binding.pager.setAdapter(new PreferencesPagerAdapter(getChildFragmentManager(), getActivity(), accountId, isJami));
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
        requireFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(BlackListFragment.TAG)
                .replace(R.id.fragment_container, blackListFragment, BlackListFragment.TAG)
                .commit();
        binding.slidingTabs.setVisibility(View.GONE);
        binding.pager.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.account_edition, menu);
        mItemAdvanced = menu.findItem(R.id.menuitem_advanced);
        mItemBlacklist = menu.findItem(R.id.menuitem_blacklist);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        presenter.prepareOptionsMenu();
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
        mIsVisible = false;
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).setToolbarOutlineState(true);
        if (binding.fragmentContainer.getVisibility() != View.VISIBLE) {
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
        binding.slidingTabs.setVisibility(isJami? View.GONE : View.VISIBLE);
        binding.pager.setVisibility(isJami? View.GONE : View.VISIBLE);
        binding.fragmentContainer.setVisibility(isJami? View.VISIBLE : View.GONE);
        presenter.prepareOptionsMenu(isJami);
        setBackListenerEnabled(isJami);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getActivity() != null)
                    getActivity().onBackPressed();
                return true;
            case R.id.menuitem_delete:
                AlertDialog deleteDialog = createDeleteDialog();
                deleteDialog.show();
                break;
            case R.id.menuitem_advanced:
                binding.slidingTabs.setVisibility(View.VISIBLE);
                binding.pager.setVisibility(View.VISIBLE);
                binding.fragmentContainer.setVisibility(View.GONE);
                JamiAccountSummaryFragment fragment = (JamiAccountSummaryFragment) getChildFragmentManager().findFragmentByTag(JamiAccountSummaryFragment.TAG);
                if (fragment != null)
                    fragment.setFragmentVisibility(false);
                mIsVisible = true;
                setupElevation();
                break;
            case R.id.menuitem_blacklist:
                presenter.goToBlackList();
                if (getActivity() instanceof HomeActivity)
                    ((HomeActivity) getActivity()).setToolbarElevation(false);
            default:
                break;
        }
        return true;
    }

    @NonNull
    private AlertDialog createDeleteDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.account_delete_dialog_message)
                .setTitle(R.string.account_delete_dialog_title)
                .setPositiveButton(R.string.menu_delete, (dialog, whichButton) -> presenter.removeAccount())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        Activity activity = getActivity();
        if (activity != null)
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
        Activity activity = getActivity();
        if (activity != null)
            activity.onBackPressed();
    }

    private void setBackListenerEnabled(boolean enable) {
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).setAccountFragmentOnBackPressedListener(enable ? this : null);
    }

    private static class PreferencesPagerAdapter extends FragmentStatePagerAdapter {
        private Context mContext;
        private String accountId;
        private boolean isJamiAccount;

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
        if (binding == null || !mIsVisible) {
            return;
        }
        Activity activity = getActivity();
        if (!(activity instanceof HomeActivity))
            return;
        LinearLayout ll = (LinearLayout) binding.pager.getChildAt(binding.pager.getCurrentItem());
        if (ll == null) return;
        RecyclerView rv = (RecyclerView)((FrameLayout) ll.getChildAt(0)).getChildAt(0);
        if (rv == null) return;
        HomeActivity homeActivity = (HomeActivity) activity;
        if (rv.canScrollVertically(SCROLL_DIRECTION_UP)) {
            binding.slidingTabs.setElevation(binding.slidingTabs.getResources().getDimension(R.dimen.toolbar_elevation));
            homeActivity.setToolbarElevation(true);
            homeActivity.setToolbarOutlineState(false);
        } else {
            binding.slidingTabs.setElevation(0);
            homeActivity.setToolbarElevation(false);
            homeActivity.setToolbarOutlineState(true);
        }
    }
}
