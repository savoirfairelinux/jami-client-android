package com.savoirfairelinux.sflphone.adapters;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.fragments.CallElementListFragment;
import com.savoirfairelinux.sflphone.fragments.CallFragment;
import com.savoirfairelinux.sflphone.fragments.DialingFragment;
import com.savoirfairelinux.sflphone.fragments.HistoryFragment;

public class CallPagerAdapter extends FragmentStatePagerAdapter {
    
    private static final String TAG = SectionsPagerAdapter.class.getSimpleName();
    Context mContext;
    

    
    HashMap<String,Fragment> calls;

    public CallPagerAdapter(Context c, FragmentManager fm) {
        super(fm);
        mContext = c;
        calls = new HashMap<String,Fragment>();
    }

    @Override
    public Fragment getItem(int i) {
        
        for(int j = 0 ; j <= i ; ++j){
            calls.entrySet().iterator().next();
        }
        
        
        return calls.entrySet().iterator().next().getValue();
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
        return calls.size();
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

    public void addCall(String mCallID, CallFragment newCall) {
        Log.w(TAG, "Put "+mCallID);
        calls.put(mCallID,newCall);
        notifyDataSetChanged();
    }

    public Fragment getCall(String callID) {
        Log.w(TAG, "Get "+callID);
        return calls.get(callID);
        
    }

    public void remove(String callID) {
        calls.remove(callID);
        notifyDataSetChanged();
        
    }
}