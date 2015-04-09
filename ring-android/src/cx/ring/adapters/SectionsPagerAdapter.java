/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.adapters;

import java.util.ArrayList;
import java.util.Locale;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import cx.ring.R;
import cx.ring.fragments.CallListFragment;
import cx.ring.fragments.DialingFragment;
import cx.ring.fragments.HistoryFragment;
import cx.ring.views.PagerSlidingTabStrip;

import android.content.Context;
import android.util.Log;

public class SectionsPagerAdapter extends android.support.v4.app.FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

    private static final String TAG = SectionsPagerAdapter.class.getSimpleName();
    Context mContext;
    ArrayList<Fragment> fragments;

    public SectionsPagerAdapter(Context c, FragmentManager fm) {
        super(fm);
        mContext = c;
        fragments = new ArrayList<Fragment>();
        fragments.add(new DialingFragment());
        fragments.add(new CallListFragment());
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
            name = CallListFragment.class.getName();
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