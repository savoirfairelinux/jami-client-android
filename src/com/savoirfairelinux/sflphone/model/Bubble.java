package com.savoirfairelinux.sflphone.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.savoirfairelinux.sflphone.R;

public class Bubble
{
	// Create a paint for the fill
	private Paint mPaintPath, mBitmapPaint;

	public Paint getmPaintPath() {
		return mPaintPath;
	}

	public Paint getmBitmapPaint() {
		return mBitmapPaint;
	}

	private float radius;

	// A Bitmap object that is going to be passed to the BitmapShader
	private Bitmap internalBMP, externalBMP;

	public Bitmap getExternalBMP() {
		return externalBMP;
	}

	Canvas internalCanvas;

	// Two floats to store the touch position
	private float posX;
	private float posY;
	Path path;

	private RectF bounds;

	public RectF getBounds() {
		return bounds;
	}

	public Path getPath() {
		return path;
	}

	public Bubble(Context c, float X, float Y, float rad, int resID) {
		posX = X;
		posY = Y;

		// Initialize the bitmap object by loading an image from the resources folder
		if (resID != -1) {
			internalBMP = BitmapFactory.decodeResource(c.getResources(), resID);
		} else {
			internalBMP = BitmapFactory.decodeResource(c.getResources(), R.drawable.ic_contact_picture);
		}
		internalBMP = Bitmap.createScaledBitmap(internalBMP, (int) rad, (int) rad, false);
		int w = internalBMP.getWidth(), h = internalBMP.getHeight();

		externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		radius = externalBMP.getWidth() / 2;
		path = new Path();

		path.addCircle(radius, radius, radius, Path.Direction.CW);

		bounds = new RectF(posX - radius, posY - radius, posX + radius, posY + radius);

		mPaintPath = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintPath.setStyle(Paint.Style.FILL);
		mPaintPath.setAntiAlias(true);
		Bitmap circle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas circle_drawer = new Canvas(circle);
		circle_drawer.drawOval(new RectF(0, 0, w, h), mPaintPath);

		mPaintPath.setFilterBitmap(false);
		
		internalCanvas = new Canvas(externalBMP);
		internalCanvas.drawBitmap(internalBMP, 0, 0, mPaintPath);
		mPaintPath.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		internalCanvas.drawBitmap(circle, 0, 0, mPaintPath);
	}

	public float getPosX() {
		return posX;
	}

	public void setPosX(float x) {
		posX = x;
		bounds.left = posX - radius;
		bounds.right = posX + radius;
	}

	public void setPosY(float y) {
		posY = y;
		bounds.top = posY - radius;
		bounds.bottom = posY + radius;
	}

	public float getPosY() {
		return posY;
	}

	public float getRadius() {
		return radius;
	}
}
