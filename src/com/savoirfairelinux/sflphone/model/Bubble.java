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
	public CallContact contact;
	// A Bitmap object that is going to be passed to the BitmapShader
	private Bitmap internalBMP, externalBMP;

	private PointF pos = new PointF();
	private RectF bounds;
	public float target_scale = 1.f;
	private final float radius;
	private float scale = 1.f;
	public PointF speed = new PointF(0, 0);
	public PointF last_speed = new PointF();
	public PointF attractor = null;

	public boolean dragged = false;
	public long last_drag;


	public void setAttractor(PointF attractor) {
		this.attractor = attractor;
	}

	public Bubble(Context c, float x, float y, float rad, Bitmap photo) {
		internalBMP = photo;
		pos.set(x, y);

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

		attractor = new PointF(x, y);
		mPaintPath.setFilterBitmap(false);

		Canvas internalCanvas = new Canvas(externalBMP);
		internalCanvas.drawBitmap(internalBMP, 0, 0, mPaintPath);
		mPaintPath.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		internalCanvas.drawBitmap(circle, 0, 0, mPaintPath);
	}

	public Bubble(Context c, float x, float y, float rad, int resID) {
		// Initialize the bitmap object by loading an image from the resources folder
		/*if (resID != -1)
			internalBMP = BitmapFactory.decodeResource(c.getResources(), resID);
		else
			internalBMP = BitmapFactory.decodeResource(c.getResources(), R.drawable.ic_contact_picture);
		 */
		this(c, x, y, rad, BitmapFactory.decodeResource(c.getResources(), resID==-1 ? resID : R.drawable.ic_contact_picture));
	}



	public Bitmap getBitmap() {
		return externalBMP;
	}

	public RectF getBounds() {
		return bounds;
	}

	public void set(float x, float y, float s) {
		scale = s;
		pos.x = x;
		pos.y = y;
		float rad = scale*radius;
		bounds.left = pos.x - rad;
		bounds.right = pos.x + rad;
		bounds.top = pos.y - rad;
		bounds.bottom = pos.y + rad;
	}

	public float getPosX() {
		return pos.x;
	}

	public float getPosY() {
		return pos.y;
	}

	public void setPos(float x, float y) {
		set(x, y, scale);
	}

	public PointF getPos()
	{
		return pos;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float s) {
		set(pos.x, pos.y, s);
	}

	public float getRadius() {
		return radius;
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
