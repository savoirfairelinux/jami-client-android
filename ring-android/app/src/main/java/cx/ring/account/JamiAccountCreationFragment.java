/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import cx.ring.databinding.FragAccJamiCreateBinding;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;

public class JamiAccountCreationFragment extends BaseSupportFragment {

    private static final int NUM_PAGES = 3;

    private FragAccJamiCreateBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccJamiCreateBinding.inflate(inflater, container, false);
        mBinding.pager.setOffscreenPageLimit(NUM_PAGES);
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

        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getChildFragmentManager());
        mBinding.pager.setAdapter(pagerAdapter);
        mBinding.pager.disableScroll(true);
        mBinding.indicator.setupWithViewPager(mBinding.pager, true);

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

    public void scrollPagerFragment(AccountCreationModel accountCreationModel) {
        mBinding.pager.setCurrentItem(mBinding.pager.getCurrentItem() + 1);
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof JamiAccountPasswordFragment) {
                ((JamiAccountPasswordFragment) fragment).setUsername(accountCreationModel.getUsername());
            }
        }
    }

    private static class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
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
        public int getCount() {
            return NUM_PAGES;
        }
    }

}
