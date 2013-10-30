package org.sflphone.adapters;

import java.util.ArrayList;
import java.util.Locale;

import org.sflphone.R;
import org.sflphone.fragments.DialingFragment;
import org.sflphone.fragments.HistoryFragment;
import org.sflphone.fragments.HomeFragment;
import org.sflphone.views.PagerSlidingTabStrip;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;


public class SectionsPagerAdapter extends FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

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
        return fragments.size();
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
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public int getPageIconResId(int position) {
        switch (position) {
        case 0:
            return R.drawable.ic_action_dial_pad_light;
        case 1:
            return R.drawable.ic_action_call;
        case 2:
            return R.drawable.ic_action_time;
        default:
            Log.e(TAG, "getPageTitle: unknown tab position " + position);
            break;
        }
        return 0;
    }
}