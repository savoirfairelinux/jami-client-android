package org.sflphone.adapters;

import java.util.ArrayList;
import java.util.Locale;

import org.sflphone.fragments.DialingFragment;
import org.sflphone.fragments.HistoryFragment;
import org.sflphone.fragments.HomeFragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.RemoteException;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;

import org.sflphone.R;

public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
    
    private static final String TAG = SectionsPagerAdapter.class.getSimpleName();
    Context mContext;
    ArrayList<Fragment> fragments;

    public SectionsPagerAdapter(Context c, FragmentManager fm) {
        super(fm);
        mContext = c;
        fragments = new ArrayList<Fragment>();
        fragments.add(new DialingFragment());
        fragments.add(new HomeFragment());
        fragments.add(new HistoryFragment());
    }

    @Override
    public Fragment getItem(int i) {

        return fragments.get(i);
    }

    public String getClassName(int i) {
        String name;

        switch (i) {
        case 0:
            name = DialingFragment.class.getName();
            break;
        case 1:
            name = HomeFragment.class.getName();
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
    
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
        case 0:
            return mContext.getString(R.string.title_section0).toUpperCase(Locale.getDefault());
        case 1:
            return mContext.getString(R.string.title_section1).toUpperCase(Locale.getDefault());
        case 2:
            return mContext.getString(R.string.title_section2).toUpperCase(Locale.getDefault());
        default:
            Log.e(TAG, "getPageTitle: unknown tab position " + position);
            break;
        }
        return null;
    }

    public void updateHome() {
        try {
            ((HomeFragment) fragments.get(1)).updateLists();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e1){
            e1.printStackTrace();
        }
    }
}