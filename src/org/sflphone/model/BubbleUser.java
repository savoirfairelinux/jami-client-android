package org.sflphone.model;

import org.sflphone.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

public class BubbleUser extends Bubble {

    public Conference associated_call;

    float expanded_radius;

    private boolean mMuted;

    public BubbleUser(Context context, CallContact m, Conference conf, float x, float y, float size) {
        super(context, m, x, y, size);
        isUser = true;
        associated_call = conf;
        mMuted = false;
        expanded_radius = (float) (size * 1.5);

        setDrawer(new ActionDrawer(0, 0));
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
        act.generateBitmap(actions.NOTHING);

    }

    @Override
    public int getRadius() {
        if (expanded)
            return (int) (radius * density);
        return (int) (radius * scale * density);
    }

    public int getExpandedRadius() {
        return (int) (expanded_radius * density);
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

        RectF boundsHoldButton, boundsMicButton, boundsRecButton, boundsHangUpButton;

        Bitmap buttonMic, buttonMicMuted, buttonHold, buttonUnhold, buttonRec, buttonHangUp;
        int wHang, hHang;
        int wHold, hHold;
        int wMic, hMic;
        int wRec, hRec;

        float delta;

        private RectF globals;

        public ActionDrawer(int w, int h) {
            super(w, h);

            delta = (float) (w / 2 * Math.cos(Math.toRadians(45d)));
            globals = new RectF(0, 0, mWidth, mHeight);

            buttonMic = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_mic);
            buttonMicMuted = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_mic_muted);
            buttonHold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_pause_over_video);
            buttonUnhold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_play_over_video);
            buttonRec = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.recordpressed);
            buttonHangUp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_end_call);

            wHang = buttonHangUp.getWidth();
            hHang = buttonHangUp.getHeight();

            wHold = buttonHold.getWidth();
            hHold = buttonHold.getHeight();

            wMic = buttonMic.getWidth();
            hMic = buttonMic.getHeight();

            wRec = buttonRec.getWidth();
            hRec = buttonRec.getHeight();
        }

        @Override
        public void generateBitmap(int action) {
            img = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
            Canvas c = new Canvas(img);
            c.drawOval(new RectF(0, 0, mWidth, mHeight), mBackgroundPaint);

            drawHangUp(c, action == actions.HANGUP);
            drawHold(c, action == actions.HOLD);
            drawMute(c, action == actions.MUTE);
            drawRec(c, action == actions.RECORD);

            c.drawLine(mWidth / 2 - delta, mHeight / 2 - delta, mWidth / 2 + delta, mHeight / 2 + delta, mLines);
            c.drawLine(mWidth / 2 - delta, mHeight / 2 + delta, mWidth / 2 + delta, mHeight / 2 - delta, mLines);

        }

        private void drawHangUp(Canvas c, boolean selected) {
            boundsHangUpButton = new RectF(mWidth / 2 - getRadius(), 0, mWidth / 2 + getRadius(), mHeight / 2 - getRadius());

            RectF boundsHangUpIcon = new RectF((int) boundsHangUpButton.centerX() - wHang / 2, (int) boundsHangUpButton.centerY() - hHang / 2,
                    (int) boundsHangUpButton.centerX() + wHang / 2, (int) boundsHangUpButton.centerY() + hHang / 2);

            if (selected) {
                c.drawArc(globals, 225, 90, true, mSelector);
            }

            c.drawBitmap(buttonHangUp, null, boundsHangUpIcon, mButtonPaint);
        }

        private void drawHold(Canvas c, boolean selected) {
            boundsHoldButton = new RectF(0, mHeight / 2 - getRadius(), mWidth / 2 - getRadius(), mHeight / 2 + getRadius());
            RectF boundsHoldIcon = new RectF((int) boundsHoldButton.centerX() - wHold / 2, (int) boundsHoldButton.centerY() - hHold / 2,
                    (int) boundsHoldButton.centerX() + wHold / 2, (int) boundsHoldButton.centerY() + hHold / 2);
            if (selected) {
                c.drawArc(globals, 135, 90, true, mSelector);
            }

            if (associated_call.isOnHold()) {
                c.drawBitmap(buttonUnhold, null, boundsHoldIcon, mButtonPaint);
            } else {
                c.drawBitmap(buttonHold, null, boundsHoldIcon, mButtonPaint);
            }
        }

        private void drawMute(Canvas c, boolean selected) {
            boundsMicButton = new RectF(mWidth / 2 + getRadius(), mHeight / 2 - getRadius(), mWidth, mHeight / 2 + getRadius());
            RectF boundsMuteIcon = new RectF((int) boundsMicButton.centerX() - wMic / 2, (int) boundsMicButton.centerY() - hMic / 2,
                    (int) boundsMicButton.centerX() + wMic / 2, (int) boundsMicButton.centerY() + hMic / 2);
            if (selected || mMuted) {
                c.drawArc(globals, -45, 90, true, mSelector);
            }

            c.drawBitmap(buttonMic, null, boundsMuteIcon, mButtonPaint);
        }

        private void drawRec(Canvas c, boolean selected) {
            boundsRecButton = new RectF(mWidth / 2 - getRadius(), mHeight / 2 + getRadius(), mWidth / 2 + getRadius(), mHeight);
            RectF boundsRecIcon = new RectF((int) boundsRecButton.centerX() - wRec / 2, (int) boundsRecButton.centerY() - hRec / 2,
                    (int) boundsRecButton.centerX() + wRec / 2, (int) boundsRecButton.centerY() + hMic / 2);
            if (selected || associated_call.isRecording()) {
                c.drawArc(globals, 45, 90, true, mSelector);
            }

            c.drawBitmap(buttonRec, null, boundsRecIcon, mButtonPaint);
        }

        @Override
        public int getAction(float x, float y) {

            float relativeX = x - getDrawerBounds().left;
            float relativeY = y - getDrawerBounds().top;

            if (!getDrawerBounds().contains(x, y) && !getBounds().contains(x, y)) {
                return actions.OUT_OF_BOUNDS;
            }

            if (boundsHoldButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Holding");
                return actions.HOLD;
            }

            if (boundsRecButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Record");
                return actions.RECORD;
            }

            if (boundsMicButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Muting");
                return actions.MUTE;
            }

            if (boundsHangUpButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "hangup");
                return actions.HANGUP;
            }

            return actions.NOTHING;

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
            act.generateBitmap(actions.NOTHING);
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
    public boolean isConference() {
        return associated_call.hasMultipleParticipants();
    }

    @Override
    public boolean onDown(MotionEvent event) {
        if (expanded) {
            act.generateBitmap(act.getAction(event.getX(), event.getY()));
            return false;
        }

        if (intersects(event.getX(), event.getY())) {
            dragged = true;
            last_drag = System.nanoTime();
            setPos(event.getX(), event.getY());
            target_scale = .8f;
            return true;
        }
        return false;
    }

    public boolean getMute() {
        return mMuted;
    }

    public void toggleMute() {
        mMuted = !mMuted;
        act.generateBitmap(Bubble.actions.NOTHING);

    }

    public void setMute(boolean captureMuted) {
        mMuted = captureMuted;
        
    }
}
