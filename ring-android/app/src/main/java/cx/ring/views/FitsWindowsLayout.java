package cx.ring.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class FitsWindowsLayout extends FrameLayout {

    private Rect windowInsets = new Rect();
    private Rect tempInsets = new Rect();

    public FitsWindowsLayout(Context context) {
        super(context);
    }

    public FitsWindowsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FitsWindowsLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        windowInsets.set(insets);
        super.fitSystemWindows(insets);
        return false;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        tempInsets.set(windowInsets);
        super.fitSystemWindows(tempInsets);
    }
}