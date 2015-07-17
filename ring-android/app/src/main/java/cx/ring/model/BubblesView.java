/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

package cx.ring.model;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import cx.ring.R;

import java.util.List;

public class BubblesView extends GLSurfaceView implements SurfaceHolder.Callback, OnTouchListener {
    private static final String TAG = BubblesView.class.getSimpleName();

    private BubblesThread thread = null;
    private BubbleModel model;

    private Paint black_name_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint white_name_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint canvas_paint = new Paint();
    private Paint circle_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint action_paint = new Paint();

    static private final Interpolator interpolator = new OvershootInterpolator(2.f);
    static private final Interpolator interpolator_dec = new DecelerateInterpolator();

    private final Bitmap ic_bg;
    private final Bitmap ic_bg_sel;

    private GestureDetector gDetector;

    //private float density;
    private float textDensity;
    private float bubbleActionTextDistMin;
    private float bubbleActionTextDistMax;

    private boolean dragging_bubble = false;

    public BubblesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources r = getResources();
        //density = r.getDisplayMetrics().density;
        textDensity = r.getDisplayMetrics().scaledDensity;
        bubbleActionTextDistMin = r.getDimension(R.dimen.bubble_action_textdistmin);
        bubbleActionTextDistMax = r.getDimension(R.dimen.bubble_action_textdistmax);

        ic_bg = BitmapFactory.decodeResource(r, R.drawable.ic_bg);
        ic_bg_sel = BitmapFactory.decodeResource(r, R.drawable.ic_bg_sel);

        if (isInEditMode()) return;

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        this.setZOrderOnTop(true); // necessary
        holder.setFormat(PixelFormat.TRANSLUCENT);
        // create thread only; it's started in surfaceCreated()
        createThread();

        setOnTouchListener(this);
        setFocusable(true);

        black_name_paint.setTextSize(18 * textDensity);
        black_name_paint.setColor(0xFF303030);
        black_name_paint.setTextAlign(Align.CENTER);

        white_name_paint.setTextSize(18 * textDensity);
        white_name_paint.setColor(0xFFEEEEEE);
        white_name_paint.setTextAlign(Align.CENTER);

        circle_paint.setStyle(Paint.Style.STROKE);
        circle_paint.setColor(r.getColor(R.color.darker_gray));
        circle_paint.setXfermode(null);

        gDetector = new GestureDetector(getContext(), new BubbleGestureListener());
        gDetector.setIsLongpressEnabled(false);
    }

    private void createThread() {
        if (thread != null)
            return;
        thread = new BubblesThread(getHolder(), getContext());
        if (model != null)
            thread.setModel(model);
    }

    public void setModel(BubbleModel model) {
        this.model = model;
        thread.setModel(model);
    }

    /*
     * @Override public void onWindowFocusChanged(boolean hasWindowFocus) { if (!hasWindowFocus) { thread.pause(); } }
     */

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.w(TAG, "surfaceChanged " + width + "-" + height);
        /*if (height < model.getHeight()) // probably showing the keyboard, don't move!
            return;

        thread.setSurfaceSize(width, height);*/
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be used.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        createThread();

        Log.w(TAG, "surfaceCreated");
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        Log.w(TAG, "surfaceDestroyed");
        boolean retry = true;
        thread.setRunning(false);
        thread.setPaused(false);
        while (retry) {
            try {
                Log.w(TAG, "joining...");
                thread.join();
                retry = false;
            } catch (InterruptedException ignored) {
            }
        }
        Log.w(TAG, "done");
        thread = null;
    }

    public boolean isDraggingBubble() {
        return dragging_bubble;
    }

    class BubblesThread extends Thread {
        private boolean running = false;
        public boolean suspendFlag = false;
        private SurfaceHolder surfaceHolder;

        BubbleModel model = null;

        public BubblesThread(SurfaceHolder holder, Context context) {
            surfaceHolder = holder;
        }

        public void setModel(BubbleModel model) {
            this.model = model;
        }

        @Override
        public void run() {
            while (running) {
                Canvas c = null;
                try {

                    if (suspendFlag) {
                        synchronized (this) {
                            while (suspendFlag) {
                                try {
                                    wait();
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        c = surfaceHolder.lockCanvas(null);

                        // for the case the surface is destroyed while already in the loop
                        if (c == null || model == null)
                            continue;

                        synchronized (model) {
                            model.update();
                        }
                        synchronized (surfaceHolder) {
                            // Log.w(TAG, "Thread doDraw");
                            synchronized (model) {
                                doDraw(c);
                            }
                        }
                    }

                } finally {
                    if (c != null)
                        surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }

        public void setPaused(boolean wantToPause) {
            synchronized (this) {
                suspendFlag = wantToPause;
                notify();
            }
        }

        public void setRunning(boolean b) {
            running = b;
        }

        /**
         * got multiple IndexOutOfBoundsException, when switching calls. //FIXME
         *
         * @param canvas
         */
        private void doDraw(Canvas canvas) {
            List<Bubble> bubbles = model.getBubbles();
            List<Attractor> attractors = model.getAttractors();
            BubbleModel.ActionGroup actions = model.getActions();

            long now = System.nanoTime();

            canvas_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPaint(canvas_paint);
            canvas_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

            if (model.curState == BubbleModel.State.Incall || model.curState == BubbleModel.State.Outgoing) {
                PointF center = model.getCircleCenter();
                canvas.drawCircle(center.x, center.y, model.getCircleSize(), circle_paint);
            }

            for (Attractor a : attractors) {
                canvas.drawBitmap(a.getBitmap(), null, a.getBounds(), null);
            }

            Bubble drawLater = (actions == null) ? null : actions.bubble;
            for (Bubble b : bubbles) {
                if (b == drawLater) continue;
                canvas.drawBitmap(b.getBitmap(), null, b.getBounds(), null);
                canvas.drawText(b.getName(), b.getPosX(), b.getPosY() - b.getRetractedRadius() * 1.2f, getNamePaint(b));
            }

            if (actions != null) {
                float t = actions.getVisibility(now);
                if (!actions.enabled && t == .0f) {
                    model.clearActions();
                }
                float showed = interpolator.getInterpolation(t);
                float dark = interpolator_dec.getInterpolation(t);
                float dist_range = bubbleActionTextDistMax - bubbleActionTextDistMin;
                action_paint.setAlpha((int) (255 * t));

                List<Attractor> acts = actions.getActions();
                Bubble b = actions.bubble;

                canvas.drawARGB((int)(dark*128), 0, 0, 0);

                white_name_paint.setTextSize(18 * textDensity);
                boolean suck_bubble = false;
                for (Attractor a : acts) {
                    if (b.attractor == a) {
                        canvas.drawBitmap(ic_bg_sel, null, a.getBounds(showed * 2.f, b.getPos(), showed), action_paint);
                        suck_bubble = true;
                    } else
                        canvas.drawBitmap(ic_bg, null, a.getBounds(showed * 2.f, b.getPos(), showed), action_paint);
                    canvas.drawBitmap(a.getBitmap(), null, a.getBounds(showed, b.getPos(), showed), null);
                    float dist_raw = FloatMath.sqrt((b.pos.x - a.pos.x) * (b.pos.x - a.pos.x) + (b.pos.y - a.pos.y) * (b.pos.y - a.pos.y));
                    float dist_min = a.radius + b.radius + bubbleActionTextDistMin;
                    float dist = Math.max(0, dist_raw - dist_min);
                    if (actions.enabled && dist < dist_range) {
                        white_name_paint.setAlpha(255 - (int) (255 * dist / dist_range));
                        canvas.drawText(a.name, a.getBounds().centerX(), a.getBounds().centerY() - a.radius * 2.2f, white_name_paint);
                    }
                }
                white_name_paint.setAlpha(255);

                canvas.drawBitmap(drawLater.getBitmap(), null, drawLater.getBounds(), (!actions.enabled && suck_bubble)? action_paint : null);
            }
        }
    }

    private Paint getNamePaint(Bubble b) {
        black_name_paint.setTextSize(18/* * b.targetScale */ * textDensity);
        return black_name_paint;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Log.w(TAG, "onTouch " + event.getAction());

        int action = event.getActionMasked();

        if (gDetector.onTouchEvent(event))
            return true;

        if (action == MotionEvent.ACTION_UP) {
            if (thread != null && thread.suspendFlag) {
                Log.i(TAG, "Relaunch drawing thread");
                thread.setPaused(false);
            }
            List<Bubble> bubbles = model.getBubbles();
            for (Bubble b : bubbles) {
                if (b.isGrabbed()) {
                    model.ungrabBubble(b);
                }
            }
            dragging_bubble = false;
        } else if (action != MotionEvent.ACTION_DOWN && !isDraggingBubble() && thread != null && !thread.suspendFlag) {
            Log.i(TAG, "Not dragging thread should be stopped");
            thread.setPaused(true);
        }
        return true;
    }

    public void restartDrawing() {
        if (thread != null && thread.suspendFlag) {
            Log.i(TAG, "Relaunch drawing thread");
            thread.setPaused(false);
        }
    }

    public void stopThread() {
        if (thread != null && thread.suspendFlag) {
            Log.i(TAG, "Stop drawing thread");
            thread.setPaused(true);
        }
    }

    class BubbleGestureListener implements OnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            synchronized (model) {
                List<Bubble> bubbles = model.getBubbles();
                for (Bubble b : bubbles) {
                    if (b.intersects(event.getX(), event.getY())) {
                        model.grabBubble(b);
                        b.setPos(event.getX(), event.getY());
                        dragging_bubble = true;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent event, float distanceX, float distanceY) {
            synchronized (model) {
                List<Bubble> bubbles = model.getBubbles();
                for (Bubble b : bubbles) {
                    if (b.isGrabbed()) {
                        b.drag(event.getX(), event.getY());
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }
    }
}
