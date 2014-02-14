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

import org.sflphone.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

public class BubbleContact extends Bubble {

    public SipCall associated_call;
    Bitmap buttonMsg, buttonUnhold, buttonHold, buttonTransfer, buttonHangUp;

    public interface drawerPosition {
        int UNDEFINED = -1;
        int TOP = 0;
        int RIGHT = 1;
        int BOTTOM = 2;
        int LEFT = 3;
    }

    public BubbleContact(Context context, SipCall call, float x, float y, float size) {
        super(context, call.getmContact(), x, y, size);
        associated_call = call;

        buttonMsg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_chat);
        buttonHold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_pause_over_video);
        buttonUnhold = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_play_over_video);
        buttonTransfer = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_forward);
        buttonHangUp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_end_call);

        setDrawer(new ActionDrawer(0, 0, drawerPosition.UNDEFINED));

    }

    @Override
    public void expand(int width, int height) {

        expanded = true;
        generateBitmap();
        if (pos.x < width / 3) {

            // Left
            act = new ActionDrawer(width * 2 / 3, (int) (getRadius() * 1.5f), drawerPosition.LEFT);
            act.adjustBounds(pos.x, pos.y);
            act.generateBitmap(actions.NOTHING);

        } else if (pos.x > 2 * width / 3) {
            // Right
            act = new ActionDrawer(width * 2 / 3, (int) (getRadius() * 1.5f), drawerPosition.RIGHT);
            act.adjustBounds(pos.x, pos.y);
            act.generateBitmap(actions.NOTHING);

        } else {
            // Middle of the screen
            if (pos.y < height / 3) {
                // Middle Top

                act = new ActionDrawer((int) (getRadius() * 1.5f), height / 2, drawerPosition.TOP);
                act.adjustBounds(pos.x, pos.y);
                act.generateBitmap(actions.NOTHING);

            } else if (pos.y > 2 * height / 3) {
                // Middle Bottom

                act = new ActionDrawer((int) (getRadius() * 1.5f), height / 2, drawerPosition.BOTTOM);
                act.adjustBounds(pos.x, pos.y);
                act.generateBitmap(actions.NOTHING);

            }
        }

    }

    protected class ActionDrawer extends Bubble.ActionDrawer {

        int direction;
        RectF boundsHoldButton, boundsMsgButton, boundsTransferButton, boundsHangUpButton;
        @SuppressWarnings("unused")
        private String TAG = ActionDrawer.class.getSimpleName();

        int wHang, hHang;
        int wHold, hHold;
        int wMsg, hMsg;
        int wTrans, hTrans;
        private RectF boundsTransferIcon,boundsMsgIcon, boundsHangIcon, boundsHoldIcon;
        
        private int LINE_PADDING = 25;


        Paint pButtons = new Paint();

        public ActionDrawer(int w, int h, int dir) {
            super(w, h);
            direction = dir;

            wHang = buttonHangUp.getWidth();
            hHang = buttonHangUp.getHeight();

            wMsg = buttonMsg.getWidth();
            hMsg = buttonMsg.getHeight();

            wHold = buttonHold.getWidth();
            hHold = buttonHold.getHeight();

            wTrans = buttonTransfer.getWidth();
            hTrans = buttonTransfer.getHeight();
        }

        @Override
        public int getAction(float x, float y) {

            float relativeX = x - getDrawerBounds().left;
            float relativeY = y - getDrawerBounds().top;

            int result = actions.NOTHING;

            if (!getDrawerBounds().contains(x, y) && !getBounds().contains(x, y)) {
                return actions.OUT_OF_BOUNDS;
            }

            if (boundsHoldButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Holding");
                result = actions.HOLD;
            }

            if (boundsMsgButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Msg");
                result = actions.MESSAGE;
            }

            if (boundsHangUpButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "hangUp");
                result = actions.HANGUP;
            }

            if (boundsTransferButton.contains(relativeX, relativeY)) {
                Log.i("Bubble", "Transfer");
                result = actions.TRANSFER;
            }

            return result;

        }

        public void generateBitmap(int action) {

            img = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
            Canvas c = new Canvas(img);
            c.drawRect(new RectF(0, 0, mWidth, mHeight), mBackgroundPaint);
            float rHeight, rWidth;

            switch (direction) {
            case drawerPosition.TOP:
                rHeight = bounds.height() - getRadius();
                boundsHoldButton = new RectF(0, getRadius(), mWidth, getRadius() + rHeight / 4);
                boundsMsgButton = new RectF(0, getRadius() + rHeight / 4, mWidth, getRadius() + 2 * rHeight / 4);
                boundsTransferButton = new RectF(0, getRadius() + 2 * rHeight / 4, mWidth, getRadius() + 3 * rHeight / 4);
                boundsHangUpButton = new RectF(0, getRadius() + 3 * rHeight / 4, mWidth, getRadius() + rHeight);

                calculateIconBounds();
                draw(c, action);
                
                c.drawLine(LINE_PADDING, boundsHoldButton.bottom, mWidth - LINE_PADDING, boundsHoldButton.bottom, mLines);
                c.drawLine(LINE_PADDING, boundsMsgButton.bottom, mWidth - LINE_PADDING, boundsMsgButton.bottom, mLines);
                c.drawLine(LINE_PADDING, boundsTransferButton.bottom, mWidth - LINE_PADDING, boundsTransferButton.bottom, mLines);
                
                break;
            case drawerPosition.BOTTOM:
                rHeight = bounds.height() - getRadius();
                boundsHangUpButton = new RectF(0, 0, mWidth, rHeight / 4);
                boundsTransferButton = new RectF(0, rHeight / 4, mWidth, 2 * rHeight / 4);
                boundsMsgButton = new RectF(0, 2 * rHeight / 4, mWidth, 3 * rHeight / 4);
                boundsHoldButton = new RectF(0, 3 * rHeight / 4, mWidth, rHeight);

                calculateIconBounds();
                draw(c, action);
                
                c.drawLine(LINE_PADDING, boundsHangUpButton.bottom, mWidth - LINE_PADDING, boundsHangUpButton.bottom, mLines);
                c.drawLine(LINE_PADDING, boundsTransferButton.bottom, mWidth - LINE_PADDING, boundsTransferButton.bottom, mLines);
                c.drawLine(LINE_PADDING, boundsMsgButton.bottom, mWidth - LINE_PADDING,boundsMsgButton.bottom, mLines);
                
                break;
            case drawerPosition.RIGHT:
                rWidth = bounds.width() - getRadius();
                boundsHoldButton = new RectF(0, 0, rWidth / 4, mHeight);
                boundsMsgButton = new RectF(rWidth / 4, 0, 2 * rWidth / 4, mHeight);
                boundsTransferButton = new RectF(2 * rWidth / 4, 0, 3 * rWidth / 4, mHeight);
                boundsHangUpButton = new RectF(3 * rWidth / 4, 0, rWidth, mHeight);

                calculateIconBounds();
                draw(c, action);
                
                c.drawLine(boundsHoldButton.right, LINE_PADDING, boundsHoldButton.right, mHeight - LINE_PADDING, mLines);
                c.drawLine(boundsMsgButton.right, LINE_PADDING, boundsMsgButton.right, mHeight - LINE_PADDING, mLines);
                c.drawLine(boundsTransferButton.right, LINE_PADDING, boundsTransferButton.right, mHeight - LINE_PADDING, mLines);

                break;
            case drawerPosition.LEFT:

                rWidth = bounds.width() - getRadius();
                boundsHangUpButton = new RectF(getRadius(), 0, getRadius() + rWidth / 4, mHeight);
                boundsTransferButton = new RectF(getRadius() + rWidth / 4, 0, getRadius() + 2 * rWidth / 4, mHeight);
                boundsMsgButton = new RectF(getRadius() + 2 * rWidth / 4, 0, getRadius() + 3 * rWidth / 4, mHeight);
                boundsHoldButton = new RectF(getRadius() + 3 * rWidth / 4, 0, getRadius() + rWidth, mHeight);

                calculateIconBounds();
                draw(c, action);
                break;
            }

        }



        private void draw(Canvas c, int action) {
            if (action == actions.HANGUP) {
                c.drawRect(boundsHangUpButton, mSelector);
            }
            c.drawBitmap(buttonHangUp, null, boundsHangIcon, pButtons);

            if (action == actions.HOLD) {
                c.drawRect(boundsHoldButton, mSelector);
            }
            if (associated_call.isOnHold()) {
                c.drawBitmap(buttonUnhold, null, boundsHoldIcon, pButtons);
            } else {
                c.drawBitmap(buttonHold, null, boundsHoldIcon, pButtons);
            }
            if (action == actions.MESSAGE) {
                c.drawRect(boundsMsgButton, mSelector);
            }
            c.drawBitmap(buttonMsg, null, boundsMsgIcon, pButtons);

            if (action == actions.TRANSFER) {
                c.drawRect(boundsTransferButton, mSelector);
            }
            c.drawBitmap(buttonTransfer, null, boundsTransferIcon, pButtons);
        }

        private void calculateIconBounds() {
            boundsHoldIcon = new RectF((int) boundsHoldButton.centerX() - wHold / 2, (int) boundsHoldButton.centerY() - hHold / 2,
                    (int) boundsHoldButton.centerX() + wHold / 2, (int) boundsHoldButton.centerY() + hHold / 2);
            boundsHangIcon = new RectF((int) boundsHangUpButton.centerX() - wHang / 2, (int) boundsHangUpButton.centerY() - hHang / 2,
                    (int) boundsHangUpButton.centerX() + wHang / 2, (int) boundsHangUpButton.centerY() + hHang / 2);
            boundsMsgIcon = new RectF((int) boundsMsgButton.centerX() - wMsg / 2, (int) boundsMsgButton.centerY() - hMsg / 2,
                    (int) boundsMsgButton.centerX() + wMsg / 2, (int) boundsMsgButton.centerY() + hMsg / 2);
            boundsTransferIcon = new RectF((int) boundsTransferButton.centerX() - wTrans / 2, (int) boundsTransferButton.centerY() - hTrans / 2,
                    (int) boundsTransferButton.centerX() + wTrans / 2, (int) boundsTransferButton.centerY() + hTrans / 2);
        }

        public void adjustBounds(float x, float y) {
            switch (direction) {
            case drawerPosition.TOP:
                setBounds(x - getRadius(), y, x + getRadius(), y + getHeight());
                break;
            case drawerPosition.BOTTOM:
                setBounds(x - getRadius(), y - getHeight(), x + getRadius(), y);
                break;
            case drawerPosition.RIGHT:
                setBounds(x - getWidth(), y - getRadius(), x, y + +getRadius());
                break;
            case drawerPosition.LEFT:
                setBounds(x, y - getRadius(), x + getWidth(), y + getRadius());
                break;
            }

        }

        @Override
        public void setBounds(float left, float top, float right, float bottom) {
            int margin = (int) (0.5f * getRadius()) / 2;
            switch (direction) {
            case drawerPosition.TOP:
            case drawerPosition.BOTTOM:
                super.setBounds(left + margin, top, right - margin, bottom);
                break;
            case drawerPosition.RIGHT:
            case drawerPosition.LEFT:
                super.setBounds(left, top + margin, right, bottom - margin);
                break;
            }

        }
    }

    public Bitmap getDrawerBitmap() {
        return act.getBitmap();
    }

    public RectF getDrawerBounds() {
        return act.getDrawerBounds();
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
            act.adjustBounds(pos.x, pos.y);
        }
    }

    @Override
    public int getRadius() {
        if (expanded)
            return (int) (radius * density);
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

    public SipCall getCall() {
        return associated_call;
    }

    public void setCall(SipCall call) {
        associated_call = call;
        if (expanded) {
            act.generateBitmap(actions.NOTHING);
        }

    }

    @Override
    public String getName() {
        return associated_call.getmContact().getmDisplayName();
    }

    @Override
    public boolean callIDEquals(String call) {
        return associated_call.getCallId().contentEquals(call);
    }

    @Override
    public String getCallID() {
        return associated_call.getCallId();
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
