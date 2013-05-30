package com.savoirfairelinux.sflphone.views;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.savoirfairelinux.sflphone.fragments.CallFragment;

public class CallPaneLayout extends SlidingPaneLayout
{
	public CallFragment curFragment = null;

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
		return super.onInterceptTouchEvent(event);
	}

}
