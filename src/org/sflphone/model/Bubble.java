package org.sflphone.model;

import org.sflphone.adapters.ContactPictureTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.Log;

import org.sflphone.R;

public class Bubble {

    // A Bitmap object that is going to be passed to the BitmapShader
    private Bitmap internalBMP, externalBMP;

    public SipCall associated_call;
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

    public boolean markedToDie = false;
    public long last_drag;
    public boolean expanded; // determine if we draw the buttons around the bubble
    private Bitmap saved_photo;
    private float expanded_radius;

    public void setAttractor(PointF attractor) {
        this.attractor = attractor;
    }

    public Bubble(Context context, SipCall call, float x, float y, float size) {

        Bitmap photo = null;
        if (call.getContact().getPhoto_id() > 0) {
            photo = ContactPictureTask.loadContactPhoto(context.getContentResolver(), call.getContact().getId());
        } else {
            photo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
        }

        saved_photo = photo;
        internalBMP = Bitmap.createScaledBitmap(photo, (int) size, (int) size, false);
        internalBMP.setHasAlpha(true);
        associated_call = call;
        pos.set(x, y);
        radius = internalBMP.getWidth() / 2;
        expanded_radius = (float) (size * 1.5);
        bounds = new RectF(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius);
        attractor = new PointF(x, y);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);
        Bitmap circle = Bitmap.createBitmap(internalBMP.getWidth(), internalBMP.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas circle_drawer = new Canvas(circle);
        circle_drawer.drawOval(new RectF(0, 0, internalBMP.getWidth(), internalBMP.getHeight()), circlePaint);

        externalBMP = Bitmap.createBitmap(internalBMP.getWidth(), internalBMP.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(externalBMP);

        circlePaint.setFilterBitmap(false);
        canvas.drawBitmap(internalBMP, 0, 0, circlePaint);

        circlePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        canvas.drawBitmap(circle, 0, 0, circlePaint);
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
        float dx = x - pos.x;
        float dy = y - pos.y;

        return dx * dx + dy * dy < getRadius() * density * getRadius() * density;
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

    public void expand() {

        expanded = true;
        internalBMP = Bitmap.createScaledBitmap(saved_photo, (int) (2 * radius), (int) (2 * radius), false);

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setAntiAlias(true);
        circlePaint.setDither(true);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(Color.RED);

        Bitmap circle = Bitmap.createBitmap(internalBMP.getWidth(), internalBMP.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas circle_drawer = new Canvas(circle);
        circle_drawer.drawOval(new RectF(0, 0, internalBMP.getWidth(), internalBMP.getHeight()), circlePaint);

        Canvas canvas = new Canvas(internalBMP);
        circlePaint.setFilterBitmap(false);
        canvas.drawBitmap(internalBMP, 0, 0, circlePaint);

        circlePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        canvas.drawBitmap(circle, 0, 0, circlePaint);
        circle_drawer.drawOval(new RectF(0, 0, internalBMP.getWidth(), internalBMP.getHeight()), circlePaint);

        externalBMP = Bitmap.createBitmap((int) (getRadius() * 2), (int) (getRadius() * 2), Bitmap.Config.ARGB_8888);
        Canvas canvasf = new Canvas(externalBMP);

        Paint mPaintPath = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintPath.setStyle(Paint.Style.FILL);
        mPaintPath.setColor(0xAA000000);

        Paint fatality = new Paint(Paint.ANTI_ALIAS_FLAG);
        fatality.setAntiAlias(true);
        fatality.setDither(true);
        fatality.setStyle(Paint.Style.FILL);

        canvasf.drawOval(new RectF(0, 0, getRadius() * 2, getRadius() * 2), mPaintPath); // background with buttons

        int[] allpixels = new int[internalBMP.getHeight() * internalBMP.getWidth()];

        internalBMP.getPixels(allpixels, 0, internalBMP.getWidth(), 0, 0, internalBMP.getWidth(), internalBMP.getHeight());
        for (int i = 0; i < internalBMP.getHeight() * internalBMP.getWidth(); i++) {
            // Log.i("Bubble", "allpixels[i]:"+allpixels[i]);
            if (allpixels[i] == Color.BLACK) {
                allpixels[i] = 0xAA000000;
            }
        }
        internalBMP.setPixels(allpixels, 0, internalBMP.getWidth(), 0, 0, internalBMP.getWidth(), internalBMP.getHeight());

        canvasf.drawBitmap(internalBMP, (float) (getRadius() - radius), (float) (getRadius() - radius), fatality);

    }

    public void retract() {
        expanded = false;
        internalBMP = Bitmap.createScaledBitmap(saved_photo, (int) (2 * radius), (int) (2 * radius), false);

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setAntiAlias(true);
        circlePaint.setDither(true);
        circlePaint.setStyle(Paint.Style.FILL);

        Bitmap circle = Bitmap.createBitmap(internalBMP.getWidth(), internalBMP.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas circle_drawer = new Canvas(circle);
        circle_drawer.drawOval(new RectF(0, 0, internalBMP.getWidth(), internalBMP.getHeight()), circlePaint);

        externalBMP = Bitmap.createBitmap(internalBMP.getWidth(), internalBMP.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(externalBMP);

        circlePaint.setFilterBitmap(false);
        canvas.drawBitmap(internalBMP, 0, 0, circlePaint);

        circlePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        canvas.drawBitmap(circle, 0, 0, circlePaint);

    }

    /**
     * Compare bubbles based on call ID
     */
    @Override
    public boolean equals(Object c) {
        if (c instanceof Bubble && ((Bubble) c).associated_call.getCallId().contentEquals(associated_call.getCallId())) {
            return true;
        }
        return false;

    }

    /**
     * When the bubble is expanded we need to check on wich action button the user tap
     * 
     * @param x
     * @param y
     * @return
     */
    public int getAction(float x, float y) {
        float relativeX = x - pos.x + externalBMP.getWidth() / 2;
        float relativeY = y - pos.y + externalBMP.getHeight() / 2;

        // Log.i("Bubble", "relativeX:" + relativeX);
        // Log.i("Bubble", "relativeY:" + relativeY);
        //
        // Log.i("Bubble", "pos.x:" + pos.x);
        // Log.i("Bubble", "pos.y:" + pos.y);
        //
        // Log.i("Bubble", "externalBMP.getWidth():" + externalBMP.getWidth());
        // Log.i("Bubble", "externalBMP.getHeight():" + externalBMP.getHeight());

        // Hold - Left
        if (relativeX < externalBMP.getWidth() / 3 && relativeY > externalBMP.getHeight() / 3) {
            Log.i("Bubble", "Holding");
            return 1;
        }

        // Record - Right
        if (relativeX > externalBMP.getWidth() * 2 / 3 && relativeY > externalBMP.getHeight() / 3) {
            Log.i("Bubble", "Record");
            return 2;
        }

        // Transfer - Bottom
        if (relativeY > externalBMP.getHeight() * 2 / 3) {
            Log.i("Bubble", "Transfer");
            return 3;
        }
        return 0;
    }

    public boolean isOnBorder(float w, float h) {
        return (bounds.left < 0 || bounds.right > w || bounds.top < 0 || bounds.bottom > h);
    }

    /**
     * Always return the normal radius of the bubble
     * 
     * @return
     */
    public float getRetractedRadius() {
        return radius;
    }

    public int getHoldStatus() {
        if (associated_call.isOnHold())
            return R.string.action_call_unhold;
        else
            return R.string.action_call_hold;
    }

    public int getRecordStatus() {
        if (associated_call.isRecording())
            return R.string.action_call_stop_record;
        else
            return R.string.action_call_record;
    }
}
