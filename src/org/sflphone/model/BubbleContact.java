package org.sflphone.model;

import org.sflphone.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class BubbleContact extends Bubble {

    public SipCall associated_call;
    Bitmap buttonMsg, buttonHold, buttonTransfer, buttonHangUp;

    public BubbleContact(Context context, SipCall call, float x, float y, float size) {
        super(context, call.getContact(), x, y, size);
        associated_call = call;
        isUser = false;
        setDrawer(new ActionDrawer(0, 0, false, false));
        buttonMsg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_chat);
        buttonHold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_pause_over_video);
        buttonTransfer = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_forward);
        buttonHangUp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_end_call);

    }

    @Override
    public void expand(int width, int height) {

        expanded = true;
        generateBitmap();
        if (pos.x < width / 3) {

            // Open on the right
            act = new ActionDrawer(width * 2 / 3, getRadius() * 2, false, false);
            act.setBounds(pos.x, pos.y - getRadius(), pos.x + act.getWidth(), pos.y + getRadius());
            act.generateBitmap();


        } else if (pos.x > 2 * width / 3) {
            // Open on the left
            act = new ActionDrawer(width * 2 / 3, getRadius() * 2, true, false);
            act.setBounds(pos.x - act.getWidth(), pos.y - getRadius(), pos.x, pos.y + +getRadius());
            act.generateBitmap();

        } else {
            // Middle of the screen
            if (pos.y < height / 3) {
                // Middle Top

                act = new ActionDrawer((int) (getRadius() * 1.5f), height / 2, false, true);
                int margin = (int) (0.5f * getRadius()) / 2;
                act.setBounds(pos.x - getRadius() + margin, pos.y, pos.x + getRadius() - margin, pos.y + act.getHeight());
                act.generateBitmap();

            } else if (pos.y > 2 * height / 3) {
                // Middle Bottom

                act = new ActionDrawer(getRadius() * 2, height / 2, false, true);
                act.setBounds(pos.x - getRadius(), pos.y - act.getHeight(), pos.x + getRadius(), pos.y);
                act.generateBitmap();

                
            }
        }

    }

    protected class ActionDrawer extends Bubble.ActionDrawer{

        boolean isLeft, isTop;
        RectF boundsHoldButton, boundsMsgButton, boundsTransferButton, boundsHangUpButton;

        public ActionDrawer(int w, int h, boolean left, boolean top) {
            super(w, h);
            isLeft = left;
            isTop = top;
        }
        
        @Override
        public int getAction(float x, float y) {

            float relativeX = x - getBounds().left;
            float relativeY = y - getBounds().top;

            if (boundsHoldButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Holding");
                return actions.HOLD;
            }

            if (boundsMsgButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Msg");
                return actions.MESSAGE;
            }

            if (boundsHangUpButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "hangUp");
                return actions.HANGUP;
            }

            if (boundsTransferButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Transfer");
                return actions.TRANSFER;
            }
            return 0;

        }

        public void generateBitmap() {

            img = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
            Paint paint = new Paint();
            paint.setColor(mContext.getResources().getColor(R.color.sfl_action_blue));
            Canvas c = new Canvas(img);
            c.drawRect(new RectF(0, 0, mWidth, mHeight), paint);

            Paint pButtons = new Paint();
            if (isTop) {
                float rHeight = bounds.height() - getRadius();
                boundsHoldButton = new RectF(0, getRadius(), mWidth, getRadius() + rHeight / 4);
                boundsMsgButton = new RectF(0, getRadius() + rHeight / 4, mWidth, getRadius() + 2 * rHeight / 4);
                boundsTransferButton = new RectF(0, getRadius() + 2 * rHeight / 4, mWidth, getRadius() + 3 * rHeight / 4);
                boundsHangUpButton = new RectF(0, getRadius() + 3 * rHeight / 4, mWidth, getRadius() + rHeight);

                int wHang = buttonHangUp.getWidth();
                int hHang = buttonHangUp.getHeight();
                c.drawBitmap(buttonHangUp, null, new RectF(
                        (int) boundsHangUpButton.centerX() - wHang / 2,
                        (int) boundsHangUpButton.centerY() - hHang / 2, 
                        (int) boundsHangUpButton.centerX() + wHang / 2,
                        (int) boundsHangUpButton.centerY() + hHang / 2), 
                        pButtons);
                
                int wHold = buttonHold.getWidth();
                int hHold = buttonHold.getHeight();
                c.drawBitmap(buttonHold, null, new RectF(
                        (int) boundsHoldButton.centerX() - wHold / 2,
                        (int) boundsHoldButton.centerY() - hHold / 2, 
                        (int) boundsHoldButton.centerX() + wHold / 2,
                        (int) boundsHoldButton.centerY() + hHold / 2)
                        , pButtons);
                
                int wMsg = buttonMsg.getWidth();
                int hMsg = buttonMsg.getHeight();
                
                c.drawBitmap(buttonMsg, null, new RectF(
                        (int) boundsMsgButton.centerX() - wMsg / 2,
                        (int) boundsMsgButton.centerY() - hMsg / 2, 
                        (int) boundsMsgButton.centerX() + wMsg / 2,
                        (int) boundsMsgButton.centerY() + hMsg / 2),
                        pButtons);
                
                int wTrans = buttonTransfer.getWidth();
                int hTrans = buttonTransfer.getHeight();

                c.drawBitmap(buttonTransfer, null, new RectF(
                        (int) boundsTransferButton.centerX() - wTrans / 2,
                        (int) boundsTransferButton.centerY() - hTrans / 2, 
                        (int) boundsTransferButton.centerX() + wTrans / 2,
                        (int) boundsTransferButton.centerY() + hTrans / 2),
                        pButtons);
            }

        }

    }

    public Bitmap getDrawerBitmap() {
        return act.getBitmap();
    }

    public RectF getDrawerBounds() {
        return act.getBounds();
    }

    @Override
    public void set(float x, float y, float s) {
        scale = s;
        pos.x = x;
        pos.y = y;
        if (!expanded) {
            bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
        } else {
            bounds.set(pos.x - getRadius(), pos.y - getRadius(), pos.x + getRadius(), pos.y + getRadius());
            act.setBounds(pos.x - getRadius(), pos.y, pos.x + getRadius(), pos.y + act.getHeight());
        }
    }

    @Override
    public int getRadius() {
        return (int) (radius * scale * density);
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
        return associated_call;
    }

}
