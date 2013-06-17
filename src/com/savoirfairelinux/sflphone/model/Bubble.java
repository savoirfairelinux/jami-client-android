package com.savoirfairelinux.sflphone.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.savoirfairelinux.sflphone.R;

public class Bubble {
    public CallContact contact;
    // A Bitmap object that is going to be passed to the BitmapShader
    private Bitmap internalBMP, externalBMP;

    private PointF pos = new PointF();
    private RectF bounds;
    public float target_scale = 1.f;
    private float radius;
    private float scale = 1.f;
    private float density = 1.f;
    public PointF speed = new PointF(0, 0);
    public PointF last_speed = new PointF();
    public PointF attractor = null;

    public boolean dragged = false;
    public long last_drag;
    public boolean expanded; // determine if we draw the buttons around the bubble
    private Bitmap saved_photo;
    private float expanded_radius = 250;

    public void setAttractor(PointF attractor) {
        this.attractor = attractor;
    }

    public Bubble(float x, float y, float size, Bitmap photo) {
        saved_photo = photo;
        internalBMP = Bitmap.createScaledBitmap(photo, (int) size, (int) size, false);
        int w = internalBMP.getWidth(), h = internalBMP.getHeight();

        pos.set(x, y);
        radius = w / 2;
        bounds = new RectF(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius);
        attractor = new PointF(x, y);

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
        this(x, y, rad, BitmapFactory.decodeResource(c.getResources(), resID == -1 ? resID : R.drawable.ic_contact_picture));
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
        float rad = scale * getRadius() * density;
        bounds.set(pos.x - rad, pos.y - rad, pos.x + rad, pos.y + rad);
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

    public PointF getPos() {
        return pos;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float s) {
        set(pos.x, pos.y, s);
    }

    public float getRadius() {
        return expanded ? expanded_radius : radius;
    }

    /**
     * Point intersection test.
     */
    boolean intersects(float x, float y) {
        float dx = x - pos.x, dy = y - pos.y;
        return dx * dx + dy * dy < getRadius() * getRadius();
    }

    /**
     * Other circle intersection test.
     */
    boolean intersects(float x, float y, float radius) {
        float dx = x - pos.x, dy = y - pos.y;
        float tot_radius = getRadius() + radius;
        return dx * dx + dy * dy < tot_radius * tot_radius;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public void expand(int i) {

        // bounds = new RectF(pos.x - 200, pos.y - 200, pos.x + 200, pos.y + 200);
        // Path path = new Path();
        // path.addCircle(radius, radius, radius, Path.Direction.CW);
        //
        // Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // circlePaint.setStyle(Paint.Style.FILL);
        // Bitmap circle = Bitmap.createBitmap((int) (2 * radius), (int) (2 * radius), Bitmap.Config.ARGB_8888);
        // Canvas circle_drawer = new Canvas(circle);
        // circle_drawer.drawOval(new RectF(800 - radius, 800 - radius, 2 * radius, 2 * radius), circlePaint);
        //
        // externalBMP = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        // Canvas canvas = new Canvas(externalBMP);
        //
        // circlePaint.setFilterBitmap(false);
        // canvas.drawBitmap(internalBMP, 200 - radius, 200 - radius, circlePaint);
        //
        // circlePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        // canvas.drawBitmap(circle, 200 - radius, 200 - radius, circlePaint);

        expanded_radius = i;
        expanded = true;
        internalBMP = Bitmap.createScaledBitmap(saved_photo, (int) (2 * radius), (int) (2 * radius), false);
        int w = internalBMP.getWidth(), h = internalBMP.getHeight();

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);
        Bitmap circle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas circle_drawer = new Canvas(circle);
        circle_drawer.drawOval(new RectF((float) (getRadius() - radius), (float) (getRadius() - radius), w, h), circlePaint);

        externalBMP = Bitmap.createBitmap((int) (getRadius()*2), (int) (getRadius()*2), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(externalBMP);

        circlePaint.setFilterBitmap(false);
        canvas.drawBitmap(internalBMP, (float) (getRadius() - radius), (float) (getRadius() - radius), circlePaint);

        Paint mPaintPath = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintPath.setStyle(Paint.Style.FILL);
        mPaintPath.setColor(Color.BLUE);
//        mPaintPath.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

        canvas.drawOval(new RectF(0, 0, getRadius()*2, getRadius()*2), mPaintPath);

         circlePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
         canvas.drawBitmap(circle, (float) (getRadius() - radius), (float) (getRadius() - radius), circlePaint);

    }

    public void retract() {
        expanded = false;
        internalBMP = Bitmap.createScaledBitmap(saved_photo, (int) (2 * radius), (int) (2 * radius), false);
        int w = internalBMP.getWidth(), h = internalBMP.getHeight();

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

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
}
