package com.savoirfairelinux.sflphone.client;

import android.view.View;
import android.support.v4.view.ViewPager;

public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
    private final float scalingStart;

    public ZoomOutPageTransformer(float scalingStart) {
        super();
        this.scalingStart = 1 - scalingStart;
    }

    @Override
    public void transformPage(View page, float position) {
        // page.setRotationY(position * -30);
        final float normalizedposition = Math.abs(Math.abs(position) - 1);
        page.setAlpha(normalizedposition);
    }
}