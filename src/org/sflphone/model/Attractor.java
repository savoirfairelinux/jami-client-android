package org.sflphone.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;

public class Attractor {

	public interface Callback {

		/**
		 * Called when a bubble is on the "active" zone of the attractor.
		 * 
		 * @param b The bubble that is on the attractor.
		 * @return true if the bubble should be removed from the model, false otherwise.
		 */
		public boolean onBubbleSucked(Bubble b);
	}

	final PointF pos;
	final float radius;

	final Callback callback;
	private final Bitmap img;

	private final RectF bounds = new RectF();

	public Attractor(PointF pos, float size, Callback callback, Bitmap img) {
		this.pos = pos;
		this.radius = size/2;
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
