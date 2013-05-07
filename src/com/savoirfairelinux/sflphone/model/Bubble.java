package com.savoirfairelinux.sflphone.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.savoirfairelinux.sflphone.R;

public class Bubble
{
	// A Bitmap object that is going to be passed to the BitmapShader
	private Bitmap internalBMP, externalBMP;

	private PointF pos = new PointF();
	private RectF bounds;
	private float radius;

	public boolean dragged = false;
	public PointF attractor = null;

	public void setAttractor(PointF attractor) {
        this.attractor = attractor;
    }

    public Bubble(Context c, float X, float Y, float rad, int resID) {
		pos.set(X, Y);

		// Initialize the bitmap object by loading an image from the resources folder
		if (resID != -1)
			internalBMP = BitmapFactory.decodeResource(c.getResources(), resID);
		else
			internalBMP = BitmapFactory.decodeResource(c.getResources(), R.drawable.ic_contact_picture);
		internalBMP = Bitmap.createScaledBitmap(internalBMP, (int) rad, (int) rad, false);
		int w = internalBMP.getWidth(), h = internalBMP.getHeight();

		externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		radius = externalBMP.getWidth() / 2;
		Path path = new Path();

		path.addCircle(radius, radius, radius, Path.Direction.CW);

		bounds = new RectF(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius);

		Paint mPaintPath = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintPath.setStyle(Paint.Style.FILL);
		mPaintPath.setAntiAlias(true);
		Bitmap circle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas circle_drawer = new Canvas(circle);
		circle_drawer.drawOval(new RectF(0, 0, w, h), mPaintPath);

		attractor = new PointF(X, Y);
		mPaintPath.setFilterBitmap(false);

		Canvas internalCanvas = new Canvas(externalBMP);
		internalCanvas.drawBitmap(internalBMP, 0, 0, mPaintPath);
		mPaintPath.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		internalCanvas.drawBitmap(circle, 0, 0, mPaintPath);
	}

	public Bitmap getBitmap() {
		return externalBMP;
	}

	public RectF getBounds() {
		return bounds;
	}

	public float getPosX() {
		return pos.x;
	}

	public float getPosY() {
		return pos.y;
	}

	public void setPos(float x, float y) {
		pos.x = x;
		pos.y = y;
		bounds.left = pos.x - radius;
		bounds.right = pos.x + radius;
		bounds.top = pos.y - radius;
		bounds.bottom = pos.y + radius;
	}

	public float getRadius() {
		return radius;
	}

	public PointF getPos()
	{
		return pos;
	}

	/**
	 * Point intersection test.
	 */
	boolean intersects(float x, float y) {
		float dx = x-pos.x, dy = y-pos.y;
		return dx*dx + dy*dy < radius*radius;
	}

	/**
	 * Other circle intersection test.
	 */
	boolean intersects(float x, float y, float radius) {
		float dx = x-pos.x, dy = y-pos.y;
		float tot_radius = this.radius + radius;
		return dx*dx + dy*dy < tot_radius*tot_radius;
	}
}
