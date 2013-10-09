package org.sflphone.model;

import org.sflphone.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class BubbleUser extends Bubble {

    public Conference associated_call;
    SipCall myself;
    Bitmap buttonMic, buttonHold, buttonRecord, buttonHangUp;
    float expanded_radius;

    public BubbleUser(Context context, SipCall m, Conference conf, float x, float y, float size) {
        super(context, m.getContact(), x, y, size);
        myself = m;
        isUser = true;
        associated_call = conf;
        setDrawer(new ActionDrawer(0, 0));
        expanded_radius = (float) (size * 1.5);

        buttonMic = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_mic);
        buttonHold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_pause_over_video);
        // buttonRecord = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_);
        buttonHangUp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_end_call);
    }

    @Override
    public void set(float x, float y, float s) {
        scale = s;
        pos.x = x;
        pos.y = y;
        bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
        if (!expanded) {
            bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
        } else {
            bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
            act.setBounds(pos.x - getExpandedRadius(), pos.y - getExpandedRadius(), pos.x + getExpandedRadius(), pos.y + getExpandedRadius());
        }
    }

    @Override
    public void expand(int width, int height) {

        expanded = true;
        generateBitmap();
        setDrawer(new ActionDrawer((int) getExpandedRadius() * 2, (int) getExpandedRadius() * 2));

        act.setBounds(pos.x - getExpandedRadius(), pos.y - getExpandedRadius(), pos.x + getExpandedRadius(), pos.y + getExpandedRadius());
        act.generateBitmap();

    }

    @Override
    public int getRadius() {
        return (int) (radius * scale * density);
    }

    public int getExpandedRadius() {
        return (int) (expanded_radius * scale * density);
    }

    @Override
    public boolean getHoldStatus() {
        if (associated_call.isOnHold())
            return true;
        else
            return false;
    }

    @Override
    public boolean getRecordStatus() {
        if (associated_call.isRecording())
            return true;
        else
            return false;
    }

    @Override
    public SipCall getCall() {
        return myself;
    }

    protected class ActionDrawer extends Bubble.ActionDrawer {

        RectF boundsHoldButton, boundsMicButton, boundsRecordButton, boundsHangUpButton;

        public ActionDrawer(int w, int h) {
            super(w, h);
        }

        @Override
        public void generateBitmap() {
            img = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
            Paint paint = new Paint();
            paint.setColor(mContext.getResources().getColor(R.color.sfl_action_blue));
            paint.setDither(true);
            Canvas c = new Canvas(img);
            c.drawOval(new RectF(0, 0, mWidth, mHeight), paint);
            Log.i("Bubble", "mWidth:" + mWidth);
            Log.i("Bubble", "mHeight:" + mHeight);

            Paint test4 = new Paint();
            boundsHangUpButton = new RectF(mWidth / 2 - getRadius(), 0, mWidth / 2 + getRadius(), mHeight / 2 - getRadius());
            c.drawBitmap(buttonHangUp, null, boundsHangUpButton, test4);

            boundsHoldButton = new RectF(0, mHeight / 2 - getRadius(), mWidth / 2 - getRadius(), mHeight / 2 + getRadius());
            c.drawBitmap(buttonHold, null, boundsHoldButton, test4);

            boundsMicButton = new RectF(mWidth / 2 + getRadius(), mHeight / 2 - getRadius(), mWidth, mHeight / 2 + getRadius());
            c.drawBitmap(buttonMic, null, boundsMicButton, test4);
            // //
            // boundsRecordButton = new RectF(externalBMP.getWidth() / 2 - w / 2, 0, externalBMP.getWidth() / 2 + w / 2, externalBMP.getHeight() / 2 -
            // h / 2);
            // c.drawBitmap(buttonRecord, null, boundsRecordButton, test4);
        }

        @Override
        public int getAction(float x, float y) {

            float relativeX = x - getBounds().left;
            float relativeY = y - getBounds().top;

            Log.i("Bubble", "relativeX:" + relativeX);
            Log.i("Bubble", "relativeY:" + relativeY);

            Log.i("Bubble", "pos.x:" + pos.x);
            Log.i("Bubble", "pos.y:" + pos.y);
            //
            // Log.i("Bubble", getBounds().toShortString());
            //
            Log.i("boundsHoldButton", boundsHoldButton.toShortString());
            Log.i("boundsMicButton", boundsMicButton.toShortString());
            Log.i("boundsHangUpButton", boundsHangUpButton.toShortString());

            if (boundsHoldButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Holding");
                return actions.HOLD;
            }

            // if (boundsRecordButton.contains(x, y)) {
            // Log.i("Bubble", "Record");
            // return actions.RECORD;
            // }

            if (boundsMicButton.contains(x, y)) {
                Log.i("Bubble", "Muting");
                return actions.MUTE;
            }

            if (boundsHangUpButton.contains(x, y)) {
                Log.i("Bubble", "hangup");
                return actions.HANGUP;
            }

            return 0;

        }

    }

    @Override
    public Bitmap getDrawerBitmap() {
        return act.img;
    }

    @Override
    public RectF getDrawerBounds() {
        return act.bounds;
    }

    @Override
    public void setCall(SipCall call) {
    }

    @Override
    public void setConference(Conference c) {
        associated_call = c;
    }

}
