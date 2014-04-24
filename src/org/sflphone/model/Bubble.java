/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.model;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import org.sflphone.R;
import org.sflphone.adapters.ContactPictureTask;

public abstract class Bubble {
    // A Bitmap object that is going to be passed to the BitmapShader
    protected Bitmap externalBMP;

    protected PointF pos = new PointF();
    protected RectF bounds;
    public float target_scale = 1.f;
    protected float radius;
    protected float scale = 1.f;
    public PointF speed = new PointF(0, 0);
    //public PointF last_speed = new PointF();
    public final PointF attractionPoint;
    public Attractor attractor = null;

    public boolean isUser;

    public boolean dragged = false;

    public boolean markedToDie = false;
    public long last_drag;
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

    /*public void setAttractor(PointF attractor) {
        this.attractorPoint = attractor;
    }
*/
    public Bubble(Context context, CallContact contact, float x, float y, float size) {
        mContext = context;
        pos.set(x, y);
        radius = size / 2; // 10 is the white stroke
        saved_photo = getContactPhoto(context, contact, (int) size);
        generateBitmap();
        attractionPoint = new PointF(x, y);
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

        Paint mLines = new Paint();
        mLines.setStyle(Style.STROKE);
        mLines.setStrokeWidth(8);
        mLines.setColor(Color.WHITE);

        mLines.setDither(true);
        mLines.setAntiAlias(true);
        internalCanvas.drawCircle(w / 2, h / 2, w / 2 - 4, mLines);

        bounds = new RectF(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
    }

    protected Bitmap getContactPhoto(Context context, CallContact contact, int size) {
        if (contact.getPhoto_id() > 0) {
            return ContactPictureTask.loadContactPhoto(context.getContentResolver(), contact.getId());
        } else {
            return ContactPictureTask.decodeSampledBitmapFromResource(context.getResources(), R.drawable.ic_contact_picture, size, size);
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
        bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
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
        return (int) (radius * scale);
    }

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

    public abstract String getName();

    public abstract boolean callIDEquals(String call);
    
    public abstract String getCallID();

    public boolean isConference() {
        return false;
    }
}
