package org.sflphone.model;

import org.sflphone.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

public class BubbleUser extends Bubble {

    public Conference associated_call;
    Bitmap buttonMic, buttonMicMuted, buttonHold, buttonUnhold, buttonRecord, buttonHangUp;
    float expanded_radius;

    public BubbleUser(Context context, CallContact m, Conference conf, float x, float y, float size) {
        super(context, m, x, y, size);
        isUser = true;
        associated_call = conf;

        expanded_radius = (float) (size * 1.5);

        buttonMic = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_mic);
        buttonMicMuted = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_mic_muted);
        buttonHold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_pause_over_video);
        buttonUnhold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_play_over_video);
        // buttonRecord = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_);
        buttonHangUp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_end_call);

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

        RectF boundsHoldButton, boundsMicButton, boundsRecordButton, boundsHangUpButton;
        int wHang, hHang;
        int wHold, hHold;
        int wMic, hMic;
        Paint mBackgroundPaint;
        Paint mSelector;
        Paint mButtonPaint;

        public ActionDrawer(int w, int h) {
            super(w, h);
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mContext.getResources().getColor(R.color.sfl_blue_9));
            mBackgroundPaint.setDither(true);

            mSelector = new Paint();
            mSelector.setStyle(Style.FILL);
            mSelector.setColor(mContext.getResources().getColor(R.color.sfl_blue_9));

            mButtonPaint = new Paint();

            wHang = buttonHangUp.getWidth();
            hHang = buttonHangUp.getHeight();

            wHold = buttonHold.getWidth();
            hHold = buttonHold.getHeight();

            wMic = buttonMic.getWidth();
            hMic = buttonMic.getHeight();
        }

        @Override
        public void generateBitmap(int action) {
            img = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
            Canvas c = new Canvas(img);
            c.drawOval(new RectF(0, 0, mWidth, mHeight), mBackgroundPaint);

            drawHangUp(c, action == actions.HANGUP);
            drawHold(c, action == actions.HOLD);
            drawMute(c, action == actions.MUTE);

            // boundsRecordButton = new RectF(mWidth / 2 - getRadius(), mHeight / 2 + getRadius(), mWidth / 2 + getRadius(), mHeight);
            // c.drawBitmap(buttonRecord, null, boundsRecordButton, test4);

            // float startAngle = ;
            // float sweepAngle = 60;
            //
            // float startX = mHeight / 2;
            // float startY = mWidth / 2;
            //
            // float angle = (float) ((startAngle + sweepAngle / 2) * Math.PI / 180);
            // float stopX = (float) (startX + getRadius() * Math.cos(angle));
            // float stopY = (float) (startY + getRadius() * Math.sin(angle));
            //
            // Toast.makeText(mContext, "startX:" + startX + " startY:" + startY, Toast.LENGTH_SHORT).show();
            // Toast.makeText(mContext, "stopX:" + stopX + " stopY:" + stopY, Toast.LENGTH_SHORT).show();
            //
            // Paint mPaint = new Paint();
            // mPaint.setColor(Color.RED);
            // c.drawLine(startX, startY, stopX, stopY, mPaint);
        }

        private void drawHangUp(Canvas c, boolean selected) {
            boundsHangUpButton = new RectF(mWidth / 2 - getRadius(), 0, mWidth / 2 + getRadius(), mHeight / 2 - getRadius());

            RectF boundsHangUpIcon = new RectF((int) boundsHangUpButton.centerX() - wHang / 2, (int) boundsHangUpButton.centerY() - hHang / 2,
                    (int) boundsHangUpButton.centerX() + wHang / 2, (int) boundsHangUpButton.centerY() + hHang / 2);

            if (selected) {
                c.drawCircle(boundsHangUpButton.centerX(), boundsHangUpButton.centerY(), boundsHangUpButton.width() / 2, mSelector);
            }

            c.drawBitmap(buttonHangUp, null, boundsHangUpIcon, mButtonPaint);
        }

        private void drawHold(Canvas c, boolean selected) {
            boundsHoldButton = new RectF(0, mHeight / 2 - getRadius(), mWidth / 2 - getRadius(), mHeight / 2 + getRadius());
            RectF boundsHoldIcon = new RectF((int) boundsHoldButton.centerX() - wHold / 2, (int) boundsHoldButton.centerY() - hHold / 2,
                    (int) boundsHoldButton.centerX() + wHold / 2, (int) boundsHoldButton.centerY() + hHold / 2);
            if (selected) {
                c.drawCircle(boundsHoldButton.centerX(), boundsHoldButton.centerY(), boundsHoldButton.width() / 2, mSelector);
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
            if (selected) {
                c.drawCircle(boundsMicButton.centerX(), boundsMicButton.centerY(), boundsMicButton.width() / 2, mSelector);
            }

            c.drawBitmap(buttonMic, null, boundsMuteIcon, mButtonPaint);
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
}
