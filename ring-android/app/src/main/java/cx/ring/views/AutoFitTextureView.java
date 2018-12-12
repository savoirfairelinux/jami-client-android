package cx.ring.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureView {

    private int mRatioWidth = 720;
    private int mRatioHeight = 1280;
    private final int mSize;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 192, context.getResources().getDisplayMetrics()));
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
        Log.w("AutoFitTextureView", "setAspectRatio " + width + "x" + height);
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.w("AutoFitTextureView", "onMeasure " + widthMeasureSpec + " " + heightMeasureSpec);
        //int width = MeasureSpec.getSize(widthMeasureSpec);
        //int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = Math.min(MeasureSpec.getSize(widthMeasureSpec), mSize);
        int height = Math.min(MeasureSpec.getSize(heightMeasureSpec), mSize);
        //MeasureSpec.getMode(widthMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            /*int maxDim = Math.min(Math.max(width, height), mSize);
            if (width > height) {
                setMeasuredDimension(maxDim, maxDim * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(maxDim * mRatioWidth / mRatioHeight, maxDim);
            }*/
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }
    /*@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.w("AutoFitTextureView", "onMeasure " + width + "x" + height);
        //width = Math.min(width, mSize);
        //height = Math.min(height, mSize);
        if (0 != mRatioWidth && 0 != mRatioHeight) {
            int wrh = width * mRatioHeight;
            int hrw = height * mRatioWidth;
            //if (wrh == hrw) {
            //    setMeasuredDimension(width, height);
            //    return;
            //}
            float w, h;
            if (wrh < hrw) {
                w = width;
                h = wrh / (float)mRatioWidth;
            } else {
                w = hrw / (float)mRatioHeight;
                h = height;
            }
            //width = Math.round(w);
            //height = Math.round(h);
            Log.w("AutoFitTextureView", "setMeasuredDimension " + width + "x" + height + " ratio: " + (width > height ? height/(float)width : width/(float)height));
            setMeasuredDimension((int)w, (int)h);
        } else {
            setMeasuredDimension(0, 0);
        }
    }*/
}
