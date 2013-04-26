package com.savoirfairelinux.sflphone.client;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class RelativePositioningLayout extends RelativeLayout
{
	public RelativePositioningLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public float getXFraction()
	{
		return getX() / getWidth();
	}

	public void setXFraction(float xFraction)
	{
		final int width = getWidth();
		setX((width > 0) ? (xFraction * width) : -9999);
	}
}