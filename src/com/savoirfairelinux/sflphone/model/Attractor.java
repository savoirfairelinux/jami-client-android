package com.savoirfairelinux.sflphone.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;

public class Attractor {

	public interface Callback {
		public void onBubbleSucked(Bubble b);
	}

	final PointF pos;
	final float radius;

	final Callback callback;
	private final Bitmap img;

	private final RectF bounds = new RectF();

	public Attractor(PointF pos, float radius, Callback callback, Bitmap img) {
		this.pos = pos;
		this.radius = radius;
		this.callback = callback;
		this.img = img;
	}

	public Attractor(PointF pos, float radius, Callback callback, Context c, int resId) {
		this(pos, radius, callback, BitmapFactory.decodeResource(c.getResources(), resId));
	}

	public void setDensity(float density)
	{
		bounds.set(pos.x - radius*density, pos.y - radius*density, pos.x + radius*density, pos.y + radius*density);
	}

	public RectF getBounds() {
		return bounds;
	}

	public Bitmap getBitmap() {
		return img;
	}

}
