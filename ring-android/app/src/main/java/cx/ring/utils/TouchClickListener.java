package cx.ring.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class TouchClickListener implements GestureDetector.OnGestureListener, View.OnTouchListener {
    private final View.OnClickListener onClickListener;
    private final GestureDetector mGestureDetector;
    private View view;

    public TouchClickListener(Context c, View.OnClickListener l) {
        onClickListener = l;
        mGestureDetector = new GestureDetector(c, this);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        onClickListener.onClick(view);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        view = v;
        mGestureDetector.onTouchEvent(event);
        view = null;
        return true;
    }
}
