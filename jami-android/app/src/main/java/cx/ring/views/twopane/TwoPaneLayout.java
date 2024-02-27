/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cx.ring.views.twopane;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.Openable;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowInfoTracker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * TwoPaneLayout provides a horizontal, multi-pane layout for use at the top level
 * of a UI. A left (or start) pane is treated as a content list or browser, subordinate to a
 * primary detail view for displaying content.
 *
 * <p>Child views overlap if their combined width exceeds the available width
 * in the TwoPaneLayout. Each of child views is expand out to fill the available width in
 * the TwoPaneLayout.</p>
 *
 * <p>Thanks to this behavior, TwoPaneLayout may be suitable for creating layouts
 * that can smoothly adapt across many different screen sizes, expanding out fully on larger
 * screens and collapsing on smaller screens.</p>
 *
 * <p>TwoPaneLayout is distinct from a navigation drawer as described in the design
 * guide and should not be used in the same scenarios. TwoPaneLayout should be thought
 * of only as a way to allow a two-pane layout normally used on larger screens to adapt to smaller
 * screens in a natural way. The interaction patterns expressed by TwoPaneLayout imply
 * a physicality and direct information hierarchy between panes that does not necessarily exist
 * in a scenario where a navigation drawer should be used instead.</p>
 *
 * <p>Appropriate uses of TwoPaneLayout include pairings of panes such as a contact list and
 * subordinate interactions with those contacts, or an email thread list with the content pane
 * displaying the contents of the selected thread. Inappropriate uses of SlidingPaneLayout include
 * switching between disparate functions of your app, such as jumping from a social stream view
 * to a view of your personal profile - cases such as this should use the navigation drawer
 * pattern instead. ({@link androidx.drawerlayout.widget.DrawerLayout DrawerLayout} implements
 * this pattern.)</p>
 *
 * <p>Like {@link android.widget.LinearLayout LinearLayout}, TwoPaneLayout supports
 * the use of the layout parameter <code>layout_weight</code> on child views to determine
 * how to divide leftover space after measurement is complete. It is only relevant for width.
 * When views do not overlap weight behaves as it does in a LinearLayout.</p>
 */
public class TwoPaneLayout extends ViewGroup implements Openable {
    private static final String TAG = "TwoPaneLayout";

    /** Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage. */
    private static final String ACCESSIBILITY_CLASS_NAME =
            "cx.ring.views.twopane.TwoPaneLayout";

    /**
     * True if a panel can slide with the current measurements
     */
    private boolean mCanSlide;

    /**
     * The child view that can slide, if any.
     */
    private View mSlideableView;

    /**
     * How far the panel is offset from its usual position.
     * range [0, 1] where 0 = open, 1 = closed.
     */
    private boolean isOpened = false;

    private final List<PanelListener> mPanelListeners = new CopyOnWriteArrayList<>();

    /**
     * Stores whether or not the pane was open the last time it was slideable.
     * If open/close operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mPreservedOpenState;
    private boolean mFirstLayout = true;

    private final Rect mTmpRect = new Rect();

    FoldingFeature mFoldingFeature;

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelListener {
        /**
         * Called when a detail view becomes slid completely open.
         *
         * @param panel The detail view that was slid to an open position
         */
        void onPanelOpened(@NonNull View panel);

        /**
         * Called when a detail view becomes slid completely closed.
         *
         * @param panel The detail view that was slid to a closed position
         */
        void onPanelClosed(@NonNull View panel);
    }

    private final FoldingFeatureObserver.OnFoldingFeatureChangeListener mOnFoldingFeatureChangeListener =
            new FoldingFeatureObserver.OnFoldingFeatureChangeListener() {
                @Override
                public void onFoldingFeatureChange(@NonNull FoldingFeature foldingFeature) {
                    mFoldingFeature = foldingFeature;
                    // Start transition animation when folding feature changed
                    Transition changeBounds = new ChangeBounds();
                    changeBounds.setDuration(300L);
                    changeBounds.setInterpolator(PathInterpolatorCompat.create(0.2f, 0, 0, 1));
                    TransitionManager.beginDelayedTransition(TwoPaneLayout.this, changeBounds);
                    requestLayout();
                }
            };

    private FoldingFeatureObserver mFoldingFeatureObserver;

    public TwoPaneLayout(@NonNull Context context) {
        this(context, null);
    }

    public TwoPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs,
                         int defStyle) {
        super(context, attrs, defStyle);
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        WindowInfoTracker repo = WindowInfoTracker.getOrCreate(context);
        Executor mainExecutor = ContextCompat.getMainExecutor(context);
        mFoldingFeatureObserver = new FoldingFeatureObserver(repo, mainExecutor);
        mFoldingFeatureObserver.setOnFoldingFeatureChangeListener(mOnFoldingFeatureChangeListener);
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of
     * panel slide events.
     *
     * @param listener Listener to notify when panel slide events occur.
     * @see #removePanelListener(PanelListener)
     */
    public void addPanelListener(@NonNull PanelListener listener) {
        mPanelListeners.add(listener);
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of
     * panel slide events.
     *
     * @param listener Listener to remove from being notified of panel slide events
     * @see #addPanelListener(PanelListener)
     */
    public void removePanelListener(@NonNull PanelListener listener) {
        mPanelListeners.remove(listener);
    }

    void dispatchOnPanelOpened(@NonNull View panel) {
        for (PanelListener listener : mPanelListeners) {
            listener.onPanelOpened(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelClosed(@NonNull View panel) {
        for (PanelListener listener : mPanelListeners) {
            listener.onPanelClosed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewsVisibility(View panel) {
        boolean visibility = !mCanSlide || !isOpened;
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);

            if (child == panel) {
                // There are still more children above the panel but they won't be affected.
                break;
            } else if (child.getVisibility() == GONE) {
                continue;
            }
            child.setVisibility(visibility ? VISIBLE : INVISIBLE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
        if (mFoldingFeatureObserver != null) {
            Activity activity = getActivityOrNull(getContext());
            if (activity != null) {
                mFoldingFeatureObserver.registerLayoutStateChangeCallback(activity);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
        if (mFoldingFeatureObserver != null) {
            mFoldingFeatureObserver.unregisterLayoutStateChangeCallback();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int layoutHeight = 0;
        int maxLayoutHeight = 0;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                layoutHeight = maxLayoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
                break;
            case MeasureSpec.AT_MOST:
                maxLayoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
                break;
        }

        float weightSum = 0;
        boolean canSlide = false;
        final int widthAvailable = Math.max(widthSize - getPaddingLeft() - getPaddingRight(), 0);
        int widthRemaining = widthAvailable;
        final int childCount = getChildCount();

        if (childCount > 2) {
            Log.e(TAG, "onMeasure: More than two child views are not supported.");
        }

        // We'll find the current one below.
        mSlideableView = null;

        // First pass. Measure based on child LayoutParams width/height.
        // Weight will incur a second pass.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (child.getVisibility() == GONE) {
                continue;
            }

            if (lp.weight > 0) {
                weightSum += lp.weight;

                // If we have no width, weight is the only contributor to the final size.
                // Measure this view on the weight pass only.
                if (lp.width == 0) continue;
            }

            int childWidthSpec;
            final int horizontalMargin = lp.leftMargin + lp.rightMargin;

            int childWidthSize = Math.max(widthAvailable - horizontalMargin, 0);
            // When the parent width spec is UNSPECIFIED, measure each of child to get its
            // desired width.
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(childWidthSize,
                        widthMode == MeasureSpec.UNSPECIFIED ? widthMode : MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(childWidthSize, widthMode);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom(), lp.height);
            child.measure(childWidthSpec, childHeightSpec);
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            if (childHeight > layoutHeight) {
                if (heightMode == MeasureSpec.AT_MOST) {
                    layoutHeight = Math.min(childHeight, maxLayoutHeight);
                } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                    layoutHeight = childHeight;
                }
            }

            widthRemaining -= childWidth;
            // Skip first child (list pane), the list pane is always a non-sliding pane.
            if (i == 0) {
                continue;
            }
            canSlide |= lp.slideable = widthRemaining < 0;
            if (lp.slideable) {
                mSlideableView = child;
            }
        }
        // Second pass. Resolve weight.
        // Child views overlap when the combined width of child views cannot fit into the
        // available width. Each of child views is sized to fill all available space. If there is
        // no overlap, distribute the extra width proportionally to weight.
        if (canSlide || weightSum > 0) {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final boolean skippedFirstPass = lp.width == 0 && lp.weight > 0;
                final int measuredWidth = skippedFirstPass ? 0 : child.getMeasuredWidth();
                int newWidth = measuredWidth;
                int childWidthSpec = 0;
                if (canSlide) {
                    // Child view consumes available space if the combined width cannot fit into
                    // the layout available width.
                    final int horizontalMargin = lp.leftMargin + lp.rightMargin;
                    newWidth = widthAvailable - horizontalMargin;
                    childWidthSpec = MeasureSpec.makeMeasureSpec(
                            newWidth, MeasureSpec.EXACTLY);

                } else if (lp.weight > 0) {
                    // Distribute the extra width proportionally similar to LinearLayout
                    final int widthToDistribute = Math.max(0, widthRemaining);
                    final int addedWidth = (int) (lp.weight * widthToDistribute / weightSum);
                    newWidth = measuredWidth + addedWidth;
                    childWidthSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY);
                }
                final int childHeightSpec = measureChildHeight(child, heightMeasureSpec,
                        getPaddingTop() + getPaddingBottom());
                if (measuredWidth != newWidth) {
                    child.measure(childWidthSpec, childHeightSpec);
                    final int childHeight = child.getMeasuredHeight();
                    if (childHeight > layoutHeight) {
                        if (heightMode == MeasureSpec.AT_MOST) {
                            layoutHeight = Math.min(childHeight, maxLayoutHeight);
                        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                            layoutHeight = childHeight;
                        }
                    }
                }
            }
        }

        // At this point, all child views have been measured. Calculate the device fold position
        // in the view. Update the split position to where the fold when it exists.
        ArrayList<Rect> splitViews = splitViewPositions();

        if (splitViews != null && !canSlide) {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);

                if (child.getVisibility() == GONE) {
                    continue;
                }

                final Rect splitView = splitViews.get(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                // If child view cannot fit in the separating view, expand the child view to fill
                // available space.
                final int horizontalMargin = lp.leftMargin + lp.rightMargin;
                final int childHeightSpec = MeasureSpec.makeMeasureSpec(child.getMeasuredHeight(),
                        MeasureSpec.EXACTLY);
                int childWidthSpec = MeasureSpec.makeMeasureSpec(splitView.width(),
                        MeasureSpec.AT_MOST);
                child.measure(childWidthSpec, childHeightSpec);
                if ((child.getMeasuredWidthAndState() & MEASURED_STATE_TOO_SMALL) != 0 || (
                        getMinimumWidth(child) != 0
                                && splitView.width() < getMinimumWidth(child))) {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(widthAvailable - horizontalMargin,
                            MeasureSpec.EXACTLY);
                    child.measure(childWidthSpec, childHeightSpec);
                    // Skip first child (list pane), the list pane is always a non-sliding pane.
                    if (i == 0) {
                        continue;
                    }
                    canSlide = lp.slideable = true;
                    mSlideableView = child;
                } else {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(splitView.width(),
                            MeasureSpec.EXACTLY);
                    child.measure(childWidthSpec, childHeightSpec);
                }
            }
        }

        final int measuredWidth = widthSize;
        final int measuredHeight = layoutHeight + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(measuredWidth, measuredHeight);
        mCanSlide = canSlide;
    }

    private static int getMinimumWidth(View child) {
        return ViewCompat.getMinimumWidth(child);
    }

    private static int measureChildHeight(@NonNull View child,
            int spec, int padding) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int childHeightSpec;
        final boolean skippedFirstPass = lp.width == 0 && lp.weight > 0;
        if (skippedFirstPass) {
            // This was skipped the first time; figure out a real height spec.
            childHeightSpec = getChildMeasureSpec(spec, padding, lp.height);

        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(
                    child.getMeasuredHeight(), MeasureSpec.EXACTLY);
        }
        return childHeightSpec;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final int width = r - l;
        final int paddingStart = isLayoutRtl ? getPaddingRight() : getPaddingLeft();
        final int paddingEnd = isLayoutRtl ? getPaddingLeft() : getPaddingRight();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();
        int xStart = paddingStart;
        int nextXStart = xStart;

        if (mFirstLayout) {
            isOpened = mCanSlide && mPreservedOpenState;
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final int childWidth = child.getMeasuredWidth();
            int offset = 0;

            if (lp.slideable) {
                final int margin = lp.leftMargin + lp.rightMargin;
                final int range = Math.min(nextXStart, width - paddingEnd) - xStart - margin;
                final int lpMargin = isLayoutRtl ? lp.rightMargin : lp.leftMargin;
                final int pos = (isOpened) ? 0 : range;
                xStart += pos + lpMargin;
            } else {
                xStart = nextXStart;
            }

            final int childRight;
            final int childLeft;
            if (isLayoutRtl) {
                childRight = width - xStart + offset;
                childLeft = childRight - childWidth;
            } else {
                childLeft = xStart - offset;
                childRight = childLeft + childWidth;
            }

            final int childTop = paddingTop;
            final int childBottom = childTop + child.getMeasuredHeight();
            child.layout(childLeft, paddingTop, childRight, childBottom);

            // If a folding feature separates the content, we use its width as the extra
            // offset for the next child, in order to avoid rendering the content under it.
            int nextXOffset = 0;
            if (mFoldingFeature != null
                    && mFoldingFeature.getOrientation() == FoldingFeature.Orientation.VERTICAL
                    && mFoldingFeature.isSeparating()) {
                nextXOffset = mFoldingFeature.getBounds().width();
            }
            nextXStart += child.getWidth() + Math.abs(nextXOffset);
        }

        if (mFirstLayout) {
            updateObscuredViewsVisibility(mSlideableView);
        }

        mFirstLayout = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        if (w != oldw) {
            mFirstLayout = true;
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (!isInTouchMode() && !mCanSlide) {
            mPreservedOpenState = child == mSlideableView;
        }
    }

    /**
     * Close the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now closed/in the process of closing
     */
    public boolean closePane() {
        if (!mCanSlide) {
            mPreservedOpenState = false;
        }
        if (mFirstLayout || slideTo(false)) {
            mPreservedOpenState = false;
            return true;
        }
        return false;
    }


    /**
     * Open the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now open/in the process of opening
     */
    public boolean openPane() {
        if (!mCanSlide) {
            mPreservedOpenState = true;
        }
        if (mFirstLayout || slideTo(true)) {
            mPreservedOpenState = true;
            return true;
        }
        return false;
    }

    /**
     * Open the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    @Override
    public void open() {
        openPane();
    }

    /**
     * Close the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    @Override
    public void close() {
        closePane();
    }

    /**
     * Check if the detail view is completely open. It can be open either because the slider
     * itself is open revealing the detail view, or if all content fits without sliding.
     *
     * @return true if the detail view is completely open
     */
    @Override
    public boolean isOpen() {
        return !mCanSlide || isOpened;
    }

    /**
     * Check if both the list and detail view panes in this layout can fully fit side-by-side. If
     * not, the content pane has the capability to slide back and forth. Note that the lock mode
     * is not taken into account in this method. This method is typically used to determine
     * whether the layout is showing two-pane or single-pane.
     *
     * @return true if both panes cannot fit side-by-side, and detail pane in this layout has
     * the capability to slide back and forth.
     */
    public boolean isSlideable() {
        return mCanSlide;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        boolean result;
        final int save = canvas.save();

        if (mCanSlide && !lp.slideable && mSlideableView != null) {
            // Clip against the slider; no sense drawing what will immediately be covered.
            canvas.getClipBounds(mTmpRect);
            if (isLayoutRtlSupport()) {
                mTmpRect.left = Math.max(mTmpRect.left, mSlideableView.getRight());
            } else {
                mTmpRect.right = Math.min(mTmpRect.right, mSlideableView.getLeft());
            }
            canvas.clipRect(mTmpRect);
        }

        result = super.drawChild(canvas, child, drawingTime);

        canvas.restoreToCount(save);

        return result;
    }

    /**
     * @param opened position to switch to
     */
    boolean slideTo(boolean opened) {
        if (!mCanSlide) {
            // Nothing to do.
            return false;
        }

        View slideableView = mSlideableView;
        isOpened = opened;
        mFirstLayout = true;
        requestLayout();
        invalidate();
        if (opened)
            dispatchOnPanelOpened(slideableView);
        else
            dispatchOnPanelClosed(slideableView);
        mPreservedOpenState = opened;
        return true;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.isOpen = isSlideable() ? isOpen() : mPreservedOpenState;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.isOpen) {
            openPane();
        } else {
            closePane();
        }
        mPreservedOpenState = ss.isOpen;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[]{
            android.R.attr.layout_weight
        };

        /**
         * The weighted proportion of how much of the leftover space
         * this child should consume after measurement.
         */
        public float weight = 0;

        /**
         * True if this pane is the slideable pane in the layout.
         */
        boolean slideable;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(@NonNull android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            this.weight = source.weight;
        }

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            this.weight = a.getFloat(0, 0);
            a.recycle();
        }

    }

    static class SavedState extends AbsSavedState {
        boolean isOpen;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            isOpen = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isOpen ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect mTmpRect = new Rect();

        @Override
        public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
            final AccessibilityNodeInfoCompat superNode = AccessibilityNodeInfoCompat.obtain(info);
            super.onInitializeAccessibilityNodeInfo(host, superNode);
            copyNodeInfoNoChildren(info, superNode);
            superNode.recycle();

            info.setClassName(ACCESSIBILITY_CLASS_NAME);
            info.setSource(host);

            final ViewParent parent = ViewCompat.getParentForAccessibility(host);
            if (parent instanceof View) {
                info.setParent((View) parent);
            }

            // This is a best-approximation of addChildrenForAccessibility()
            // that accounts for filtering.
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    // Force importance to "yes" since we can't read the value.
                    ViewCompat.setImportantForAccessibility(
                            child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    info.addChild(child);
                }
            }
        }

        @Override
        public void onInitializeAccessibilityEvent(@NonNull View host, @NonNull AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(ACCESSIBILITY_CLASS_NAME);
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
                AccessibilityNodeInfoCompat src) {
            final Rect rect = mTmpRect;

            src.getBoundsInScreen(rect);
            dest.setBoundsInScreen(rect);

            dest.setVisibleToUser(src.isVisibleToUser());
            dest.setPackageName(src.getPackageName());
            dest.setClassName(src.getClassName());
            dest.setContentDescription(src.getContentDescription());

            dest.setEnabled(src.isEnabled());
            dest.setClickable(src.isClickable());
            dest.setFocusable(src.isFocusable());
            dest.setFocused(src.isFocused());
            dest.setAccessibilityFocused(src.isAccessibilityFocused());
            dest.setSelected(src.isSelected());
            dest.setLongClickable(src.isLongClickable());

            dest.addAction(src.getActions());

            dest.setMovementGranularities(src.getMovementGranularities());
        }
    }

    boolean isLayoutRtlSupport() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * @return A pair of rects define the position of the split, or {@null} if there is no split
     */
    private ArrayList<Rect> splitViewPositions() {
        if (mFoldingFeature == null || !mFoldingFeature.isSeparating()) {
            return null;
        }

        // Don't support horizontal fold in list-detail view layout
        if (mFoldingFeature.getBounds().left == 0) {
            return null;
        }
        // vertical split
        if (mFoldingFeature.getBounds().top == 0) {
            Rect splitPosition = getFoldBoundsInView(mFoldingFeature, this);
            if (splitPosition == null) {
                return null;
            }
            Rect leftRect = new Rect(getPaddingLeft(), getPaddingTop(),
                    Math.max(getPaddingLeft(), splitPosition.left),
                    getHeight() - getPaddingBottom());
            int rightBound = getWidth() - getPaddingRight();
            Rect rightRect = new Rect(Math.min(rightBound, splitPosition.right),
                    getPaddingTop(), rightBound, getHeight() - getPaddingBottom());
            return new ArrayList<>(Arrays.asList(leftRect, rightRect));
        }
        return null;
    }

    private static Rect getFoldBoundsInView(@NonNull FoldingFeature foldingFeature, View view) {
        int[] viewLocationInWindow = new int[2];
        view.getLocationInWindow(viewLocationInWindow);

        Rect viewRect = new Rect(viewLocationInWindow[0], viewLocationInWindow[1],
                viewLocationInWindow[0] + view.getWidth(),
                viewLocationInWindow[1] + view.getWidth());
        Rect foldRectInView = new Rect(foldingFeature.getBounds());
        // Translate coordinate space of split from window coordinate space to current view
        // position in window
        boolean intersects = foldRectInView.intersect(viewRect);
        // Check if the split is overlapped with the view
        if ((foldRectInView.width() == 0 && foldRectInView.height() == 0) || !intersects) {
            return null;
        }
        foldRectInView.offset(-viewLocationInWindow[0], -viewLocationInWindow[1]);
        return foldRectInView;
    }

    @Nullable
    private static Activity getActivityOrNull(Context context) {
        Context iterator = context;
        while (iterator instanceof ContextWrapper) {
            if (iterator instanceof Activity) {
                return (Activity) iterator;
            }
            iterator = ((ContextWrapper) iterator).getBaseContext();
        }
        return null;
    }
}
