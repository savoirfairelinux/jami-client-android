/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
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
package cx.ring.account;

import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import cx.ring.databinding.FragAccJamiCreateBinding;
import net.jami.mvp.AccountCreationModel;

import cx.ring.views.WizardViewPager;

public class JamiAccountCreationFragment extends Fragment {

    private static final int NUM_PAGES = 3;

    private FragAccJamiCreateBinding mBinding;
    private Fragment mCurrentFragment;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (mCurrentFragment instanceof ProfileCreationFragment) {
                ProfileCreationFragment fragment = (ProfileCreationFragment) mCurrentFragment;
                ((AccountWizardActivity) getActivity()).profileCreated(fragment.getModel(), false);
                return;
            }
            mBinding.pager.setCurrentItem(mBinding.pager.getCurrentItem() - 1);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccJamiCreateBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);

        ScreenSlidePagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getChildFragmentManager());
        mBinding.pager.setAdapter(pagerAdapter);
        mBinding.pager.disableScroll(true);
        mBinding.pager.setOffscreenPageLimit(1);
        mBinding.indicator.setupWithViewPager(mBinding.pager, true);

        mBinding.pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentFragment = pagerAdapter.getRegisteredFragment(position);
                boolean enable = mCurrentFragment instanceof JamiAccountPasswordFragment || mCurrentFragment instanceof ProfileCreationFragment;
                onBackPressedCallback.setEnabled(enable);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        LinearLayout tabStrip = ((LinearLayout) mBinding.indicator.getChildAt(0));
        for(int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    public void scrollPagerFragment(AccountCreationModel accountCreationModel) {
        if (accountCreationModel == null) {
            mBinding.pager.setCurrentItem(mBinding.pager.getCurrentItem() - 1);
            return;
        }
        mBinding.pager.setCurrentItem(mBinding.pager.getCurrentItem() + 1);
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof JamiAccountPasswordFragment) {
                ((JamiAccountPasswordFragment) fragment).setUsername(accountCreationModel.getUsername());
            }
        }
    }

    private static class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        SparseArray<Fragment> mRegisteredFragments = new SparseArray<>();
        private int mCurrentPosition = -1;

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
            switch (position) {
                case 0:
                    fragment = JamiAccountUsernameFragment.newInstance(ringAccountViewModel);
                    break;
                case 1:
                    fragment = JamiAccountPasswordFragment.newInstance(ringAccountViewModel);
                    break;
                case 2:
                    fragment = ProfileCreationFragment.newInstance(ringAccountViewModel);
                    break;
            }

            return fragment;
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            super.setPrimaryItem(container, position, object);

            if (position != mCurrentPosition && container instanceof WizardViewPager) {
                Fragment fragment = (Fragment) object;
                WizardViewPager pager = (WizardViewPager) container;

                if (fragment.getView() != null) {
                    mCurrentPosition = position;
                    pager.measureCurrentView(fragment.getView());
                }
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return super.instantiateItem(container, position);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            mRegisteredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        public Fragment getRegisteredFragment(int position) {
            return mRegisteredFragments.get(position);
        }
    }

}
