package com.savoirfairelinux.sflphone.model; // 41 Post - Created by DimasTheDriver on May/24/2012 . Part of the 'Android - rendering a Path with a Bitmap fill' post. http://www.41post.com/?p=4766

import java.util.ArrayList;

import com.savoirfairelinux.sflphone.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class BubblesView extends View {

    private static final String TAG = BubblesView.class.getSimpleName();

    ArrayList<Bubble> listBubbles;

    Paint paint;

    private int width;

    private int height;

    public BubblesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        listBubbles = new ArrayList<Bubble>();
        Bubble.Builder builder = new Bubble.Builder(getContext());
        builder.setRadiusPixels(200).setX(200).setY(300).setResID(R.drawable.me);
        listBubbles.add(builder.create());

        builder.setRadiusPixels(200).setX(200).setY(700).setResID(R.drawable.callee);
        listBubbles.add(builder.create());
        // This View can receive focus, so it can react to touch events.
        this.setFocusable(true);

        paint = new Paint();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(3);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Store the position of the touch event at 'posX' and 'posY'
        if (event.getAction() == MotionEvent.ACTION_MOVE) {

            for (Bubble b : listBubbles) {
                if (b.isPointWithin(event.getX(), event.getY())) {
                    b.setPosX(event.getX());
                    b.setPosY(event.getY());
                    invalidate();
                    return true;
                }
            }

        }
        return true;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        Log.i(TAG, "New Width " + w);
        Log.i(TAG, "New Heigth " + h);
        for (Bubble b : listBubbles) {
            b.setRegion(w, h);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(Color.GRAY);
        // canvas.drawLine(a.getPosX(), a.getPosY(), b.getPosX(), b.getPosY(), paint);

        for (Bubble b : listBubbles) {
            canvas.drawBitmap(b.getExternalBMP(), null, b.getBounds(), null);
        }

    }

    public void addBubble() {
        Bubble.Builder builder = new Bubble.Builder(getContext());
        builder.setRadiusPixels(200).setX(200).setY(300);
        listBubbles.add(builder.create());
        listBubbles.get(listBubbles.size()-1).setRegion(width, height);
        invalidate();
    }
}
