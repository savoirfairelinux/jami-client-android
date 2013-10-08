package org.sflphone.model;

import org.sflphone.R;
import org.sflphone.adapters.ContactPictureTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

public class Bubble {

    // A Bitmap object that is going to be passed to the BitmapShader
    private Bitmap externalBMP;

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

    ActionDrawer act = new ActionDrawer(0, 0, false, false);

    public void setAttractor(PointF attractor) {
        this.attractor = attractor;
    }

    public Bubble(Context context, SipCall call, float x, float y, float size) {

        saved_photo = getContactPhoto(context, call, (int) size);
        associated_call = call;
        pos.set(x, y);
        radius = size / 2;
        expanded_radius = (float) (size * 1.5);

        int w = saved_photo.getWidth(), h = saved_photo.getHeight();
        if (w > h) {
            w = h;
        } else if (h > w) {
            h = w;
        }

        externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        BitmapShader shader;
        shader = new BitmapShader(saved_photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        Canvas internalCanvas = new Canvas(externalBMP);
        internalCanvas.drawOval(new RectF(0, 0, w, h), paint);

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
        attractor = new PointF(x, y);

    }

    private Bitmap getContactPhoto(Context context, SipCall call, int size) {
        if (call.getContact().getPhoto_id() > 0) {
            return ContactPictureTask.loadContactPhoto(context.getContentResolver(), call.getContact().getId());
        } else {
            return ContactPictureTask.decodeSampledBitmapFromResource(context.getResources(), R.drawable.ic_contact_picture, (int) size, (int) size);
        }
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
        if (!expanded) {
            bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
        } else {
            bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
        }
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

    public int getRadius() {
        if (expanded)
            return (int) (expanded_radius * scale * density);

        return (int) (radius * scale * density);
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

    public void expand(int width, int height) {

        expanded = true;

//        if (associated_call.getContact().isUser()) {
            createCircularExpandedBubble();
//        } else {
//            createRectangularExpandedBubble(width, height);
//        }

    }

    private void createRectangularExpandedBubble(int width, int height) {

        // int w = saved_photo.getWidth(), h = saved_photo.getHeight();
        // if (w > h) {
        // w = h;
        // } else if (h > w) {
        // h = w;
        // }
        // externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        //
        // BitmapShader shader;
        // shader = new BitmapShader(saved_photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        //
        // Paint paint = new Paint();
        // paint.setAntiAlias(true);
        // paint.setShader(shader);
        // Canvas internalCanvas = new Canvas(externalBMP);
        // internalCanvas.drawOval(new RectF(0, 0, w, h), paint);
        //
        // bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

        int w = saved_photo.getWidth(), h = saved_photo.getHeight();
        if (w > h) {
            w = h;
        } else if (h > w) {
            h = w;
        }

        BitmapShader shader;
        shader = new BitmapShader(saved_photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        Paint test = new Paint();
        test.setColor(Color.CYAN);
        test.setStyle(Style.FILL);

        if (pos.x < width / 3) {
            // Open on the right
            bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + 300, pos.y + getRadius());
            externalBMP = Bitmap.createBitmap(w + 300, h, Bitmap.Config.ARGB_8888);
            Canvas internalCanvas = new Canvas(externalBMP);
            internalCanvas.drawRect(new RectF(0, 0, w + 300, h), test);
            internalCanvas.drawOval(new RectF(0, 0, w, h), paint);
        } else if (pos.x > 2 * width / 3) {
            // Open on the left
            bounds = new RectF(pos.x - getRadius() - 300, pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
            externalBMP = Bitmap.createBitmap(w + 300, h, Bitmap.Config.ARGB_8888);
            Canvas internalCanvas = new Canvas(externalBMP);
            internalCanvas.drawRect(new RectF(0, 0, w + 300, h), test);
            internalCanvas.drawOval(new RectF(300, 0, 300 + w, h), paint);
        } else {
            // Middle of the screen
            if (pos.y < height / 3) {

                // Middle Top

                Log.i("Bubble", "Middle Top screen");
                Log.i("Bubble", "Bounds:" + bounds.toShortString());
                Log.i("Bubble", "w:" + w);
                Log.i("Bubble", "h:" + h);

                act = new ActionDrawer(w - 200, 4 * h, false, true);
                act.setBounds(new RectF(pos.x - act.getWidth() / 2, pos.y, pos.x + act.getWidth() / 2, pos.y + act.getHeight()));
                
                

                externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

                Canvas internalCanvas = new Canvas(externalBMP);
                internalCanvas.drawOval(new RectF(0, 0, w, h), paint);
//                internalCanvas.drawRect(new RectF(0, h, w, h + act.getHeight()), test);

                bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

            } else if (pos.y > 2 * height / 3) {
                // Middle Bottom
                bounds = new RectF(pos.x - getRadius(), pos.y - 300 - getRadius(), pos.x + getRadius(), pos.y + getRadius());
                externalBMP = Bitmap.createBitmap(w, h + 300, Bitmap.Config.ARGB_8888);
                Canvas internalCanvas = new Canvas(externalBMP);
                internalCanvas.drawRect(new RectF(0, 0, w, h + 300), test);
                internalCanvas.drawOval(new RectF(0, 300, w, h + 300), paint);
            }
        }

    }

    private void createCircularExpandedBubble() {

        int w = saved_photo.getWidth(), h = saved_photo.getHeight();
        if (w > h) {
            w = h;
        } else if (h > w) {
            h = w;
        }

        externalBMP = Bitmap.createBitmap((int) (w * expanded_radius / radius), (int) (h * expanded_radius / radius), Bitmap.Config.ARGB_8888);
        BitmapShader shader;
        shader = new BitmapShader(saved_photo, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        Canvas internalCanvas = new Canvas(externalBMP);

        Paint paint2 = new Paint();
        paint2.setColor(0xAA000000);

        internalCanvas.drawOval(new RectF(0, 0, (int) (w * expanded_radius / radius), (int) (h * expanded_radius / radius)), paint2);
        internalCanvas.drawOval(new RectF(externalBMP.getWidth() / 2 - w / 2, externalBMP.getHeight() / 2 - h / 2,
                externalBMP.getWidth() / 2 + w / 2, externalBMP.getHeight() / 2 + h / 2), paint);

        bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

    }

    public void retract() {
        expanded = false;

        int w = saved_photo.getWidth(), h = saved_photo.getHeight();
        if (w > h) {
            w = h;
        } else if (h > w) {
            h = w;
        }
        externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        BitmapShader shader;
        shader = new BitmapShader(saved_photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        Canvas internalCanvas = new Canvas(externalBMP);
        internalCanvas.drawOval(new RectF(0, 0, w, h), paint);

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
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
        float relativeX = x - pos.x + bounds.width()/2;
        float relativeY = y - pos.y + bounds.height() / 2;

        // Log.i("Bubble", "relativeX:" + relativeX);
        // Log.i("Bubble", "relativeY:" + relativeY);
        //
        // Log.i("Bubble", "pos.x:" + pos.x);
        // Log.i("Bubble", "pos.y:" + pos.y);
        //
        // Log.i("Bubble", "externalBMP.getWidth():" + externalBMP.getWidth());
        // Log.i("Bubble", "externalBMP.getHeight():" + externalBMP.getHeight());

        // Hold - Left
        if (relativeX < bounds.width()/2 / 3 && relativeY > bounds.height() / 3) {
            Log.i("Bubble", "Holding");
            return 1;
        }

        // Record - Right
        if (relativeX > bounds.width()/2 * 2 / 3 && relativeY > bounds.height() / 3) {
            Log.i("Bubble", "Record");
            return 2;
        }

        // Transfer - Bottom
        if (relativeY > bounds.height() * 2 / 3) {
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

    // Calculate the position of this Bubble depending on its coordinates
    // It will open the actions drawer differently depending on it
    public int getPosition(int width, int height) {

        return 0;
    }

    protected class ActionDrawer {

        int mWidth, mHeight;
        boolean isLeft, isTop;
        RectF bounds;

        public ActionDrawer(int w, int h, boolean left, boolean top) {
            isLeft = left;
            isTop = top;
            mWidth = w;
            mHeight = h;
            bounds = new RectF(0, 0, 0, 0);
        }

        public RectF getBounds() {
            return bounds;
        }

        public void setBounds(RectF bounds) {
            this.bounds = bounds;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

    }
}
