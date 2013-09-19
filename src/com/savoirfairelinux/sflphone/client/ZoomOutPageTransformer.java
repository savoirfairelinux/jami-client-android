package com.savoirfairelinux.sflphone.client;

import android.support.v4.view.ViewPager;
import android.view.View;

public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
    private static final float MIN_ALPHA = .6f;
//    private final float scalingStart;

    public ZoomOutPageTransformer(float scalingStart) {
        super();
//        this.scalingStart = 1 - scalingStart;
    }

    @Override
    public void transformPage(View page, float position) {
        // page.setRotationY(position * -30);
        final float normalizedposition = Math.abs(Math.abs(position) - 1);
        page.setAlpha(MIN_ALPHA + (1.f-MIN_ALPHA)*normalizedposition);
    }
}