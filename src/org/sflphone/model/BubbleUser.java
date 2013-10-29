package org.sflphone.model;

import org.sflphone.R;
import org.sflphone.model.Bubble.actions;

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
    Bitmap buttonMic, buttonMicMuted, buttonHold, buttonUnhold, buttonRecord, buttonHangUp;
    float expanded_radius;

    public BubbleUser(Context context, CallContact m, Conference conf, float x, float y, float size) {
        super(context, m, x, y, size);
        isUser = true;
        associated_call = conf;
        setDrawer(new ActionDrawer(0, 0));
        expanded_radius = (float) (size * 1.5);

        buttonMic = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_mic);
        buttonMicMuted = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_mic_muted);
        buttonHold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_pause_over_video);
        buttonUnhold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_play_over_video);
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
        if(expanded)
            return (int) (radius * density);
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

    // @Override
    public Conference getConference() {
        return associated_call;
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
            int wHang, hHang;
            int wHold, hHold;
            int wMic, hMic;

            Paint test4 = new Paint();
            boundsHangUpButton = new RectF(mWidth / 2 - getRadius(), 0, mWidth / 2 + getRadius(), mHeight / 2 - getRadius());
            wHang = buttonHangUp.getWidth();
            hHang = buttonHangUp.getHeight();
            c.drawBitmap(buttonHangUp, null,
                    new RectF((int) boundsHangUpButton.centerX() - wHang / 2, (int) boundsHangUpButton.centerY() - hHang / 2,
                            (int) boundsHangUpButton.centerX() + wHang / 2, (int) boundsHangUpButton.centerY() + hHang / 2), test4);
            // c.drawBitmap(buttonHangUp, null, boundsHangUpButton, test4);

            boundsHoldButton = new RectF(0, mHeight / 2 - getRadius(), mWidth / 2 - getRadius(), mHeight / 2 + getRadius());
            // c.drawBitmap(buttonHold, null, boundsHoldButton, test4);

            wHold = buttonHold.getWidth();
            hHold = buttonHold.getHeight();
            if (associated_call.isOnHold()) {
                c.drawBitmap(buttonUnhold, null, new RectF((int) boundsHoldButton.centerX() - wHold / 2,
                        (int) boundsHoldButton.centerY() - hHold / 2, (int) boundsHoldButton.centerX() + wHold / 2, (int) boundsHoldButton.centerY()
                                + hHold / 2), test4);
            } else {
                c.drawBitmap(buttonHold, null, new RectF((int) boundsHoldButton.centerX() - wHold / 2, (int) boundsHoldButton.centerY() - hHold / 2,
                        (int) boundsHoldButton.centerX() + wHold / 2, (int) boundsHoldButton.centerY() + hHold / 2), test4);
            }

            wMic = buttonMic.getWidth();
            hMic = buttonMic.getHeight();
            boundsMicButton = new RectF(mWidth / 2 + getRadius(), mHeight / 2 - getRadius(), mWidth, mHeight / 2 + getRadius());
            c.drawBitmap(buttonMic, null, new RectF((int) boundsMicButton.centerX() - wMic / 2, (int) boundsMicButton.centerY() - hMic / 2,
                    (int) boundsMicButton.centerX() + wMic / 2, (int) boundsMicButton.centerY() + hMic / 2), test4);
            // c.drawBitmap(buttonMic, null, boundsMicButton, test4);
            // //
            boundsRecordButton = new RectF(mWidth / 2 - getRadius(), mHeight / 2 + getRadius(), mWidth / 2 + getRadius(), mHeight);
            // c.drawBitmap(buttonRecord, null, boundsRecordButton, test4);
        }

        @Override
        public int getAction(float x, float y) {

            float relativeX = x - getDrawerBounds().left;
            float relativeY = y - getDrawerBounds().top;

            if(!getDrawerBounds().contains(x, y) && !getBounds().contains(x, y)){
                return actions.OUT_OF_BOUNDS;
            }

            if (boundsHoldButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Holding");
                return actions.HOLD;
            }

            // if (boundsRecordButton.contains(x, y)) {
            // Log.i("Bubble", "Record");
            // return actions.RECORD;
            // }

            if (boundsMicButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Muting");
                return actions.MUTE;
            }

            if (boundsHangUpButton.contains(relativeX, relativeY)) {
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

    public void setConference(Conference c) {
        associated_call = c;
        if (expanded) {
            act.generateBitmap();
        }
    }

    @Override
    public String getName() {
        return mContext.getResources().getString(R.string.me);
    }

    @Override
    public boolean callIDEquals(String call) {
        return associated_call.getId().contentEquals(call);
    }

    @Override
    public String getCallID() {
        if (associated_call.hasMultipleParticipants())
            return associated_call.getId();
        else
            return associated_call.getParticipants().get(0).getCallId();
    }
    
    @Override
    public boolean isConference(){
        return associated_call.hasMultipleParticipants();
    }

}
