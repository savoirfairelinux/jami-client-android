package org.sflphone.views.parallaxscrollview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class AnotherView extends LinearLayout{

	private ScrollCallbacks mCallbacks;

    public AnotherView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mCallbacks != null)
        {
            mCallbacks.onScrollChanged(l, t, oldl, oldt);
        }
    }

    @Override
    public int computeVerticalScrollRange()
    {
        return super.computeVerticalScrollRange();
    }

    public void setCallbacks(ScrollCallbacks listener)
    {
        mCallbacks = listener;
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);
    }

    static interface ScrollCallbacks
    {
        public void onScrollChanged(int l, int t, int oldl, int oldt);
    }
	
}
