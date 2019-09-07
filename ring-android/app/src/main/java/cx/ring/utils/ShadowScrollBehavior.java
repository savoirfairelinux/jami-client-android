package cx.ring.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.transition.TransitionManager;

import com.google.android.material.appbar.AppBarLayout;

public class ShadowScrollBehavior extends AppBarLayout.ScrollingViewBehavior
        implements View.OnLayoutChangeListener {

    int totalDy = 0;
    boolean isElevated;
    View child;

    public ShadowScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child,
                                   View dependency) {
        parent.addOnLayoutChangeListener(this);
        this.child = child;
        return super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                       @NonNull View child, @NonNull View directTargetChild,
                                       @NonNull View target, int axes, int type) {
        // Ensure we react to vertical scrolling
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, child, directTargetChild,
                        target, axes, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                  @NonNull View child, @NonNull View target,
                                  int dx, int dy, @NonNull int[] consumed, int type) {
        totalDy += dy;
        if (totalDy <= 0) {
            if (isElevated) {
                ViewGroup parent = (ViewGroup) child.getParent();
                if (parent != null) {
                    TransitionManager.beginDelayedTransition(parent);
                    ViewCompat.setElevation(child, 0);
                }
            }
            totalDy = 0;
            isElevated = false;
        } else {
            if (!isElevated) {
                ViewGroup parent = (ViewGroup) child.getParent();
                if (parent != null) {
                    TransitionManager.beginDelayedTransition(parent);
                    ViewCompat.setElevation(child, dp2px(child.getContext(), 4));
                }
            }
            if (totalDy > target.getBottom())
                totalDy = target.getBottom();
            isElevated = true;
        }
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
    }


    private float dp2px(Context context, int dp) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }


    @Override
    public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        totalDy = 0;
        isElevated = false;
        ViewCompat.setElevation(child, 0);
    }
}
