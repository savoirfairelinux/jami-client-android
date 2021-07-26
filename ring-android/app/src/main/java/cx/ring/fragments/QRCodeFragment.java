/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 * Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import cx.ring.R;
import cx.ring.databinding.FragQrcodeBinding;
import cx.ring.share.ScanFragment;
import cx.ring.share.ShareFragment;
import cx.ring.utils.DeviceUtils;

public class QRCodeFragment extends BottomSheetDialogFragment {

    public static final String TAG = QRCodeFragment.class.getSimpleName();
    public static final String ARG_START_PAGE_INDEX = "start_page";

    public static final int INDEX_CODE = 0;
    public static final int INDEX_SCAN = 1;

    public static QRCodeFragment newInstance(int startPage) {
        QRCodeFragment fragment = new QRCodeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_START_PAGE_INDEX, startPage);
        fragment.setArguments(args);
        return fragment;
    }

    private FragQrcodeBinding mBinding = null;
    private int mStartPageIndex;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Bundle args = getArguments();
        mStartPageIndex = args.getInt(ARG_START_PAGE_INDEX, 0);

        mBinding = FragQrcodeBinding.inflate(inflater, container, false);
        mBinding.viewPager.setAdapter(new SectionsPagerAdapter(getContext(), getChildFragmentManager()));
        mBinding.tabs.setupWithViewPager(mBinding.viewPager);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mStartPageIndex != 0) {
            mBinding.tabs.getTabAt(mStartPageIndex).select();
        }
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogINterface -> {
            if (DeviceUtils.isTablet(requireContext())) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
            }
        });
        return dialog;
    }

    static class SectionsPagerAdapter extends FragmentPagerAdapter {
        @StringRes
        private final int[] TAB_TITLES = new int[]{R.string.tab_code, R.string.tab_scan};
        private final Context mContext;

        SectionsPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ShareFragment();
                case 1:
                    return new ScanFragment();
                default:
                    return null;
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return mContext.getResources().getString(TAB_TITLES[position]);
        }

        @Override
        public int getCount() {
            return TAB_TITLES.length;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        addGlobalLayoutListener(getView());
    }

    private void addGlobalLayoutListener(final View view) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                setPeekHeight(v.getMeasuredHeight());
                v.removeOnLayoutChangeListener(this);
            }
        });
    }

    public void setPeekHeight(int peekHeight) {
        BottomSheetBehavior<?> behavior = getBottomSheetBehaviour();
        if (behavior == null) {
            return;
        }

        behavior.setPeekHeight(peekHeight);
    }

    private BottomSheetBehavior<?> getBottomSheetBehaviour() {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) ((View) getView().getParent()).getLayoutParams();
        CoordinatorLayout.Behavior<?> behavior = layoutParams.getBehavior();
        if (behavior instanceof BottomSheetBehavior) {
            return (BottomSheetBehavior<?>) behavior;
        }

        return null;
    }

}