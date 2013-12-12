package org.sflphone.fragments;

import org.sflphone.R;
import org.sflphone.adapters.SectionsPagerAdapter;
import org.sflphone.views.PagerSlidingTabStrip;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeFragment extends Fragment {
    static final String TAG = HomeFragment.class.getSimpleName();
    
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    SectionsPagerAdapter mSectionsPagerAdapter = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    
    @Override
    public void onCreate(Bundle savedBundle){
        super.onCreate(savedBundle);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getActivity(), getFragmentManager());
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_home, container, false);
        
        
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer(0.7f));
        
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1);

        final PagerSlidingTabStrip strip = PagerSlidingTabStrip.class.cast(rootView.findViewById(R.id.pts_main));

        strip.setViewPager(mViewPager);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_ALPHA = .6f;

        public ZoomOutPageTransformer(float scalingStart) {
            super();
        }

        @Override
        public void transformPage(View page, float position) {
            final float normalizedposition = Math.abs(Math.abs(position) - 1);
            page.setAlpha(MIN_ALPHA + (1.f - MIN_ALPHA) * normalizedposition);
        }
    }

}