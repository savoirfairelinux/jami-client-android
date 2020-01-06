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
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.BlackListFragment;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.DeviceUtils;

public class AccountEditionFragment extends BaseSupportFragment<AccountEditionPresenter> implements
        BackHandlerInterface,
        AccountEditionView {

    public static final String ACCOUNT_ID_KEY = AccountEditionFragment.class.getCanonicalName() + "accountid";
    public static final String ACCOUNT_HAS_PASSWORD_KEY = AccountEditionFragment.class.getCanonicalName() + "hasPassword";

    public static final String TAG = AccountEditionFragment.class.getSimpleName();
    public static final String ACCOUNT_ID = TAG + "accountID";

    @BindView(R.id.pager)
    protected ViewPager mViewPager;

    @BindView(R.id.sliding_tabs)
    protected TabLayout mSlidingTabLayout;

    @BindView(R.id.fragment_container)
    protected FrameLayout frameLayout;

    private MenuItem mItemAdvanced;
    private MenuItem mItemBlacklist;

    private String mAccountId;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        mAccountId = getArguments().getString(ACCOUNT_ID);

        presenter.init(mAccountId);
        presenter.onAccountChanged();

        if (DeviceUtils.isTablet(requireContext())) {
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
    }

    @Override
    public void displaySummary(String accountId) {
        toggleView(accountId);
        RingAccountSummaryFragment ringAccountSummaryFragment = new RingAccountSummaryFragment();
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        ringAccountSummaryFragment.setArguments(args);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ringAccountSummaryFragment, RingAccountSummaryFragment.TAG)
                .commit();
    }

    @Override
    public void displaySIPView(String accountId) {
        toggleView(accountId);
    }

    @Override
    public void initViewPager(String accountId, boolean isRing) {
        mViewPager.setOffscreenPageLimit(4);
        mSlidingTabLayout.setupWithViewPager(mViewPager);
        mViewPager.setAdapter(new PreferencesPagerAdapter(getActivity().getSupportFragmentManager(), getActivity(), accountId, isRing));
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
        mSlidingTabLayout.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
        frameLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.account_edition, menu);
        mItemAdvanced = menu.findItem(R.id.menuitem_advanced);
        mItemBlacklist = menu.findItem(R.id.menuitem_blacklist);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
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
        presenter.unbindView();
    }

    public boolean onBackPressed() {
        if (frameLayout.getVisibility() == View.VISIBLE) {
            return false;
        }

        toggleView(mAccountId);
        return true;
    }

    private void toggleView(String accountId) {
        mAccountId = accountId;
        boolean isRing = presenter.getAccount(mAccountId).isRing();

        mSlidingTabLayout.setVisibility(isRing? View.GONE : View.VISIBLE);
        mViewPager.setVisibility(isRing? View.GONE : View.VISIBLE);
        frameLayout.setVisibility(isRing? View.VISIBLE : View.GONE);
        presenter.prepareOptionsMenu();
        if (isRing) {
            ((HomeActivity) getActivity()).setAccountFragmentOnBackPressedListener(this);
        } else {
            ((HomeActivity) getActivity()).setAccountFragmentOnBackPressedListener(null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.menuitem_delete:
                AlertDialog deleteDialog = createDeleteDialog();
                deleteDialog.show();
                break;
            case R.id.menuitem_advanced:
                mSlidingTabLayout.setVisibility(View.VISIBLE);
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
        getActivity().onBackPressed();
    }

    @Override
    public void displayAccountName(final String name) {
//        ((HomeActivity) getActivity()).setToolbarState(null, name);
    }

    @Override
    public int getLayout() {
        return R.layout.frag_account_settings;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    private static class PreferencesPagerAdapter extends FragmentStatePagerAdapter {
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

        @NonNull
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
