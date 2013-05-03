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
import android.graphics.Region;

import com.savoirfairelinux.sflphone.R;

public class Bubble extends Shape2d {

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

    private RectF region;

    private RectF bounds;

    public RectF getBounds() {
        return bounds;
    }

    public Path getPath() {
        return path;
    }

    private Bubble(Context c, float X, float Y, float rad, int resID) {
        posX = X;
        posY = Y;

        // Initialize the bitmap object by loading an image from the resources folder
        if (resID != -1) {
            internalBMP = BitmapFactory.decodeResource(c.getResources(), resID);
        } else {
            internalBMP = BitmapFactory.decodeResource(c.getResources(), R.drawable.ic_contact_picture);
        }
        internalBMP = Bitmap.createScaledBitmap(internalBMP, (int) rad, (int) rad, false);
        externalBMP = Bitmap.createBitmap(internalBMP.getWidth(), internalBMP.getHeight(), Bitmap.Config.ARGB_8888);

        radius = externalBMP.getWidth() / 2;
        path = new Path();

        path.addCircle(radius, radius, radius, Path.Direction.CW);

        bounds = new RectF(posX - radius, posY - radius, posX + radius, posY + radius);

        mPaintPath = new Paint();
        mPaintPath.setARGB(255, 200, 200, 200);
        mPaintPath.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintPath.setAntiAlias(true);

        mBitmapPaint = new Paint();
        mBitmapPaint.setXfermode(new PorterDuffXfermode(Mode.SRC));

        internalCanvas = new Canvas(externalBMP);
        internalCanvas.clipPath(path, Region.Op.INTERSECT);
        internalCanvas.drawPath(path, mPaintPath);

        RectF tmp = new RectF(0, 0, externalBMP.getWidth(), externalBMP.getHeight());
        internalCanvas.drawBitmap(internalBMP, null, tmp, mBitmapPaint);

    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float X) {
        if (X + radius <= region.right && X - radius >= region.left)
            this.posX = X;

        path.reset();
        path.addCircle(posX, posY, radius, Path.Direction.CW);
        bounds.left = posX - radius;
        bounds.right = posX + radius;
    }

    public void setPosY(float Y) {
        if (Y + radius <= region.bottom && Y - radius >= region.top)
            this.posY = Y;

        path.reset();
        path.addCircle(posX, posY, radius, Path.Direction.CW);
        bounds.top = posY - radius;
        bounds.bottom = posY + radius;
    }

    public float getPosY() {
        return posY;
    }

    public float getRadius() {
        return radius;
    }

    @Override
    public String toString() {
        String str = "Bounds :" + getLeft() + " " + getRight() + " " + getTop() + " " + getBottom() + " " + getHeight() + " " + getWidth() + " "
                + posX + " " + posY;
        return str;
    }

    /**
     * A more readable way to create balls than using a 5 param constructor of all numbers.
     */
    public static class Builder {
        private Context mContext;
        private float mX = -1;
        private float mY = -1;
        private float mRadiusPixels = -1;
        private int resID = -1;

        public void setResID(int resID) {
            this.resID = resID;
        }

        public Builder(Context c) {
            mContext = c;
        }

        public Bubble create() {

            if (mX < 0) {
                throw new IllegalStateException("X must be set");
            }
            if (mY < 0) {
                throw new IllegalStateException("Y must be stet");
            }
            if (mRadiusPixels <= 0) {
                throw new IllegalStateException("radius must be set");
            }
            return new Bubble(mContext, mX, mY, mRadiusPixels, resID);
        }

        public Builder setX(float x) {
            mX = x;
            return this;
        }

        public Builder setY(float y) {
            mY = y;
            return this;
        }

        public Builder setRadiusPixels(float pixels) {
            mRadiusPixels = pixels;
            return this;
        }
    }

    @Override
    public float getLeft() {
        return posX - radius;
    }

    @Override
    public float getRight() {
        return posX + radius;
    }

    @Override
    public float getTop() {
        return posY - radius;
    }

    @Override
    public float getBottom() {
        return posY + radius;
    }

    public void setRegion(int w, int h) {
        region = new RectF(0, 0, w, h);

    }

}
