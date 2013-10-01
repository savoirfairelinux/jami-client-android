package org.sflphone.views;

import org.sflphone.fragments.CallFragment;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CallPaneLayout extends SlidingPaneLayout
{
	private CallFragment curFragment = null;

	public CallFragment getCurFragment() {
        return curFragment;
    }

    public void setCurFragment(CallFragment curFragment) {
        this.curFragment = curFragment;
    }

    public CallPaneLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CallPaneLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event)
	{
		if(curFragment!=null && curFragment.draggingBubble()) {
			return false;
		}
		
		return false;
//		return super.onInterceptTouchEvent(event);
	}

}
