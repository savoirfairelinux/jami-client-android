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
	private float density = 1.f;
	public PointF speed = new PointF(0, 0);
	public PointF last_speed = new PointF();
	public PointF attractor = null;

	public boolean dragged = false;
	public long last_drag;


	public void setAttractor(PointF attractor) {
		this.attractor = attractor;
	}

	public Bubble(float x, float y, float size, Bitmap photo) {
		internalBMP = Bitmap.createScaledBitmap(photo, (int) size, (int) size, false);
		int w = internalBMP.getWidth(), h = internalBMP.getHeight();

		pos.set(x, y);
		radius = w / 2;
		bounds = new RectF(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius);
		attractor = new PointF(x, y);

		Path path = new Path();
		path.addCircle(radius, radius, radius, Path.Direction.CW);

		Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint.setStyle(Paint.Style.FILL);
		Bitmap circle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas circle_drawer = new Canvas(circle);
		circle_drawer.drawOval(new RectF(0, 0, w, h), circlePaint);

		externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(externalBMP);

		circlePaint.setFilterBitmap(false);
		canvas.drawBitmap(internalBMP, 0, 0, circlePaint);

		circlePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		canvas.drawBitmap(circle, 0, 0, circlePaint);
	}

	public Bubble(float x, float y, float rad, Context c, int resID) {
		// Initialize the bitmap object by loading an image from the resources folder
		this(x, y, rad, BitmapFactory.decodeResource(c.getResources(), resID==-1 ? resID : R.drawable.ic_contact_picture));
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
		float rad = scale*radius*density;
		bounds.set(pos.x-rad, pos.y-rad, pos.x+rad, pos.y+rad);
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

	public void setDensity(float density)
	{
		this.density = density;
	}
}
