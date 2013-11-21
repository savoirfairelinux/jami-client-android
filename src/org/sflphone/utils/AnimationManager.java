package org.sflphone.utils;

import android.animation.ObjectAnimator;
import android.view.View;

public class AnimationManager {

    public static ObjectAnimator slideOutDownAnimator(View toAnim, float height) {
//        ObjectAnimator outAnim = ObjectAnimator.ofFloat(toAnim, "y", 0, height);
        ObjectAnimator outAnim = new ObjectAnimator();
        outAnim.setDuration(500);
        outAnim.setPropertyName("y");
        outAnim.setFloatValues(0, height);
        return null;
    }

}
