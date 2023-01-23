package cx.ring.views;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.widget.Checkable;

public class CheckableImageButton extends AppCompatImageButton implements Checkable {
    private boolean mChecked;
    private OnCheckedChangeListener onCheckedChangeListener;

    private static final int[] CHECKED_STATE_SET = {
        android.R.attr.state_checked
    };

    public CheckableImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, CHECKED_STATE_SET);
        boolean checked = a.getBoolean(0, false);
        setChecked(checked);
        a.recycle();
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    public void toggle() {
        setChecked(!mChecked);
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, checked);
            }
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes.
     *
     * @param listener
     *            the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        onCheckedChangeListener = listener;
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    public static interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a button has changed.
         *
         * @param button
         *            The button view whose state has changed.
         * @param isChecked
         *            The new checked state of button.
         */
        void onCheckedChanged(Checkable button, boolean isChecked);
    }

}
