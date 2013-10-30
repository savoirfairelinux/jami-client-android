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
import android.view.MotionEvent;

public abstract class Bubble {

    // A Bitmap object that is going to be passed to the BitmapShader
    protected Bitmap externalBMP;

    protected PointF pos = new PointF();
    protected RectF bounds;
    public float target_scale = 1.f;
    protected float radius;
    protected float scale = 1.f;
    protected float density = 1.f;
    public PointF speed = new PointF(0, 0);
    public PointF last_speed = new PointF();
    public PointF attractor = null;
    ActionDrawer act;

    public boolean isUser;

    public boolean dragged = false;

    public boolean markedToDie = false;
    public long last_drag;
    public boolean expanded; // determine if we draw the buttons around the bubble
    protected Bitmap saved_photo;

    public interface actions {
        int OUT_OF_BOUNDS = -1;
        int NOTHING = 0;
        int HOLD = 1;
        int RECORD = 2;
        int HANGUP = 3;
        int MESSAGE = 4;
        int TRANSFER = 5;
        int MUTE = 6;
    }

    protected Context mContext;

    public void setAttractor(PointF attractor) {
        this.attractor = attractor;
    }

    public Bubble(Context context, CallContact contact, float x, float y, float size) {
        mContext = context;
        pos.set(x, y);
        radius = size / 2; // 10 is the white stroke
        saved_photo = getContactPhoto(context, contact, (int) size);
        generateBitmap();
        attractor = new PointF(x, y);
        isUser = false;
    }

    protected void generateBitmap() {

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
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setShader(shader);
        Canvas internalCanvas = new Canvas(externalBMP);
        internalCanvas.drawCircle(w / 2, h / 2, w / 2, paint);

        Paint whiteStroke = new Paint();
        whiteStroke.setStyle(Style.STROKE);
        whiteStroke.setStrokeWidth(8);
        if (expanded) {
            whiteStroke.setColor(mContext.getResources().getColor(R.color.sfl_action_blue));
        } else {
            whiteStroke.setColor(Color.WHITE);
        }
        whiteStroke.setDither(true);
        whiteStroke.setAntiAlias(true);
        internalCanvas.drawCircle(w / 2, h / 2, w / 2 - 4, whiteStroke);

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());

    }

    protected Bitmap getContactPhoto(Context context, CallContact contact, int size) {
        if (contact.getPhoto_id() > 0) {
            return ContactPictureTask.loadContactPhoto(context.getContentResolver(), contact.getId());
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

    public abstract void set(float x, float y, float s);

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

    public abstract int getRadius();

    /**
     * Point intersection test.
     */
    boolean intersects(float x, float y) {
        float dx = x - pos.x;
        float dy = y - pos.y;

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

    public abstract void expand(int width, int height);

    public void retract() {
        expanded = false;
        generateBitmap();
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

    public abstract boolean getHoldStatus();

    public abstract boolean getRecordStatus();

    public abstract Bitmap getDrawerBitmap();

    public abstract RectF getDrawerBounds();

    protected abstract class ActionDrawer {

        int mWidth, mHeight;
        RectF bounds;
        Bitmap img;

        public ActionDrawer(int w, int h) {

            mWidth = w;
            mHeight = h;
            bounds = new RectF(0, 0, 0, 0);

        }

        /**
         * When the bubble is expanded we need to check on wich action button the user tap
         * 
         * @param x
         * @param y
         * @return
         */
        public abstract int getAction(float x, float y);

        public void setBounds(float f, float y, float g, float h) {
            bounds.set(f, y, g, h);
        }

        public abstract void generateBitmap(int action);

        public RectF getDrawerBounds() {
            return bounds;
        }

        public void setBounds(RectF bounds) {
            this.bounds = bounds;
        }

        public Bitmap getBitmap() {
            return img;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public void adjustBounds(float x, float y) {

        }

    }

    public ActionDrawer getDrawer() {
        return act;
    }

    public void setDrawer(ActionDrawer a) {
        act = a;
    }

    public abstract String getName();

    public abstract boolean callIDEquals(String call);
    
    public abstract String getCallID();

    public boolean isConference() {
        return false;
    }

    public abstract boolean onDown(MotionEvent event);

}
