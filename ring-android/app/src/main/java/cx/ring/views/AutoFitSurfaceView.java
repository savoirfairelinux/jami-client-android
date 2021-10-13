package cx.ring.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.core.view.ViewCompat;

import java.util.Collections;
import java.util.List;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitSurfaceView extends SurfaceView {

    private int mRatioWidth = 720;
    private int mRatioHeight = 1280;
    private final int mSize;
    private final List<Rect> mBounds = Collections.singletonList(new Rect());

    public AutoFitSurfaceView(Context context) {
        this(context, null);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mSize = (Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, context.getResources().getDisplayMetrics())));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mBounds.get(0).set(left, top, right, bottom);
        ViewCompat.setSystemGestureExclusionRects(this, mBounds);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        if (mRatioWidth != width || mRatioHeight != height) {
            mRatioWidth = width;
            mRatioHeight = height;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = Math.min(MeasureSpec.getSize(widthMeasureSpec), mSize);
        int height = Math.min(MeasureSpec.getSize(heightMeasureSpec), mSize);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }
}
