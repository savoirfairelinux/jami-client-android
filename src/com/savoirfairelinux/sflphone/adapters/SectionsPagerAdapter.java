package com.savoirfairelinux.sflphone.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.fragments.CallElementListFragment;
import com.savoirfairelinux.sflphone.fragments.DialingFragment;
import com.savoirfairelinux.sflphone.fragments.HistoryFragment;

public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
    
    private static final String TAG = SectionsPagerAdapter.class.getSimpleName();
    Context mContext;
    final private int[] icon_res_id = { R.drawable.ic_tab_call, R.drawable.ic_tab_call, R.drawable.ic_tab_history };

    public SectionsPagerAdapter(Context c, FragmentManager fm) {
        super(fm);
        mContext = c;
    }

    @Override
    public Fragment getItem(int i) {
        Fragment fragment;

        switch (i) {
        case 0:
            fragment = new DialingFragment();
            Log.w(TAG, "getItem() DialingFragment=" + fragment);
            break;
        case 1:
            fragment = new CallElementListFragment();
            Log.w(TAG, "getItem() CallElementList=" + fragment);
            break;
        case 2:
            fragment = new HistoryFragment();
            Log.w(TAG, "getItem() HistoryFragment=" + fragment);
            break;
        default:
            Log.e(TAG, "getItem() unknown tab position " + i);
            return null;
        }

        // Log.i(TAG, "getItem() fragment is " + fragment);
        Bundle args = new Bundle();
        args.putInt(HistoryFragment.ARG_SECTION_NUMBER, i + 1);
        fragment.setArguments(args);
        return fragment;
    }

//    public Fragment getFragment(int i) {
//        Fragment fragment;
//
//        switch (i) {
//        case 0:
//            fragment = new DialingFragment();
//            break;
//        case 1:
//            fragment = new CallElementListFragment();
//            break;
//        case 2:
//            fragment = new HistoryFragment();
//            break;
//        default:
//            Log.e(TAG, "getClassName: unknown fragment position " + i);
//            fragment = null;
//        }

        // Log.w(TAG, "getFragment: fragment=" + fragment);
//        return fragment;
//    }

    public String getClassName(int i) {
        String name;

        switch (i) {
        case 0:
            name = DialingFragment.class.getName();
            break;
        case 1:
            name = CallElementListFragment.class.getName();
            break;
        case 2:
            name = HistoryFragment.class.getName();
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
        return 3;
    }
    
    public int getIconOf(int pos){
        return icon_res_id[pos];
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
        case 0:
            return mContext.getString(R.string.title_section0).toUpperCase();
        case 1:
            return mContext.getString(R.string.title_section1).toUpperCase();
        case 2:
            return mContext.getString(R.string.title_section2).toUpperCase();
        default:
            Log.e(TAG, "getPageTitle: unknown tab position " + position);
            break;
        }
        return null;
    }
}