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
package cx.ring.views.twopane

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.Openable
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.window.layout.FoldingFeature
import androidx.window.layout.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.layout.WindowInfoTracker
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * TwoPaneLayout provides a horizontal, multi-pane layout for use at the top level
 * of a UI. A left (or start) pane is treated as a content list or browser, subordinate to a
 * primary detail view for displaying content.
 *
 * Each of child views is expand out to fill the available width in
 * the TwoPaneLayout.
 *
 * Thanks to this behavior, TwoPaneLayout may be suitable for creating layouts
 * that can smoothly adapt across many different screen sizes, expanding out fully on larger
 * screens and collapsing on smaller screens.
 *
 * Like [LinearLayout][android.widget.LinearLayout], TwoPaneLayout supports
 * the use of the layout parameter `layout_weight` on child views to determine
 * how to divide leftover space after measurement is complete. It is only relevant for width.
 * When views do not overlap weight behaves as it does in a LinearLayout.
 */
class TwoPaneLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle), Openable {
    /**
     * Check if both the list and detail view panes in this layout can fully fit side-by-side. If
     * not, the content pane has the capability to slide back and forth. Note that the lock mode
     * is not taken into account in this method. This method is typically used to determine
     * whether the layout is showing two-pane or single-pane.
     *
     * @return true if both panes are unable to fit side-by-side, and detail pane in this layout has
     * the capability to slide back and forth.
     */
    /**
     * True if a panel can slide with the current measurements
     */
    var isSlideable = false
        private set

    /**
     * The child view that can slide, if any.
     */
    private var mSlideableView: View? = null

    /**
     * How far the panel is offset from its usual position.
     * range [0, 1] where 0 = open, 1 = closed.
     */
    private var isOpened = false
    private val mPanelListeners: MutableList<PanelListener> = CopyOnWriteArrayList()
    private var isTwoPaneMode = false

    private val tmpRect = Rect()
    private val tmpRect2 = Rect()
    private val foldBoundsCalculator = FoldBoundsCalculator()

    /**
     * Stores whether or not the pane was open the last time it was slideable.
     * If open/close operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private var mPreservedOpenState = false
    private var mFirstLayout = true
    private var foldingFeature: FoldingFeature? = null
        set(value) {
            if (value != field) {
                field = value
                val changeBounds: Transition = ChangeBounds()
                changeBounds.setDuration(300L)
                changeBounds.setInterpolator(PathInterpolatorCompat.create(0.2f, 0f, 0f, 1f))
                TransitionManager.beginDelayedTransition(this@TwoPaneLayout, changeBounds)
                requestLayout()
            }
        }

    /**
     * Listener for monitoring events about sliding panes.
     */
    interface PanelListener {
        /**
         * Called when a detail view becomes slid completely open.
         *
         * @param panel The detail view that was slid to an open position
         */
        fun onPanelOpened(panel: View)

        /**
         * Called when a detail view becomes slid completely closed.
         *
         * @param panel The detail view that was slid to a closed position
         */
        fun onPanelClosed(panel: View)

        /**
         * Called when the two-pane mode changes.
         *
         * @param isTwoPaneMode True if the layout is in two-pane mode, false otherwise.
         */
        fun onTwoPaneModeChanged(isTwoPaneMode: Boolean)
    }

    private val mFoldingFeatureObserver: FoldingFeatureObserver?
    init {
        ViewCompat.setAccessibilityDelegate(this, AccessibilityDelegate())
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        val repo = WindowInfoTracker.getOrCreate(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        mFoldingFeatureObserver = FoldingFeatureObserver(repo, mainExecutor)
        mFoldingFeatureObserver.setOnFoldingFeatureChangeListener(object : FoldingFeatureObserver.OnFoldingFeatureChangeListener {
            override fun onFoldingFeatureChange(foldingFeature: FoldingFeature) {
                this@TwoPaneLayout.foldingFeature = foldingFeature
            }
        })
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of
     * panel slide events.
     *
     * @param listener Listener to notify when panel slide events occur.
     * @see .removePanelListener
     */
    fun addPanelListener(listener: PanelListener) {
        mPanelListeners.add(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of
     * panel slide events.
     *
     * @param listener Listener to remove from being notified of panel slide events
     * @see .addPanelListener
     */
    fun removePanelListener(listener: PanelListener) {
        mPanelListeners.remove(listener)
    }

    fun dispatchOnPanelOpened(panel: View) {
        for (listener in mPanelListeners) {
            listener.onPanelOpened(panel)
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    fun dispatchOnPanelClosed(panel: View) {
        for (listener in mPanelListeners) {
            listener.onPanelClosed(panel)
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    fun dispatchTwoPaneModeChanged() {
        for (listener in mPanelListeners) {
            listener.onTwoPaneModeChanged(isTwoPaneMode)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mFirstLayout = true
        if (mFoldingFeatureObserver != null) {
            val activity = getActivityOrNull(context)
            if (activity != null) {
                mFoldingFeatureObserver.registerLayoutStateChangeCallback(activity)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mFirstLayout = true
        mFoldingFeatureObserver?.unregisterLayoutStateChangeCallback()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var layoutHeight = 0
        var maxLayoutHeight = 0
        when (heightMode) {
            MeasureSpec.EXACTLY -> {
                maxLayoutHeight = heightSize - paddingTop - paddingBottom
                layoutHeight = maxLayoutHeight
            }
            MeasureSpec.AT_MOST -> maxLayoutHeight = heightSize - paddingTop - paddingBottom
        }
        var weightSum = 0f
        var canSlide = false
        val widthAvailable = max(widthSize - getPaddingLeft() - getPaddingRight(), 0)
        var widthRemaining = widthAvailable
        val previousTwoPaneMode = isTwoPaneMode
        val childCount = childCount
        if (childCount > 2) {
            Log.e(TAG, "onMeasure: More than two child views are not supported.")
        }

        // We'll find the current one below.
        mSlideableView = null

        // First pass. Measure based on child LayoutParams width/height.
        // Weight will incur a second pass.
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            if (child.visibility == GONE) {
                continue
            }
            if (lp.weight > 0) {
                weightSum += lp.weight

                // If we have no width, weight is the only contributor to the final size.
                // Measure this view on the weight pass only.
                if (lp.width == 0) continue
            }
            var childWidthSpec: Int
            val horizontalMargin = lp.leftMargin + lp.rightMargin
            val childWidthSize = max(widthAvailable - horizontalMargin, 0)
            // When the parent width spec is UNSPECIFIED, measure each of child to get its
            // desired width.
            childWidthSpec = when (lp.width) {
                ViewGroup.LayoutParams.WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(childWidthSize,
                        if (widthMode == MeasureSpec.UNSPECIFIED) widthMode else MeasureSpec.AT_MOST)
                ViewGroup.LayoutParams.MATCH_PARENT -> MeasureSpec.makeMeasureSpec(childWidthSize, widthMode)
                else -> MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
            }
            val childHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                paddingTop + paddingBottom, lp.height
            )
            child.measure(childWidthSpec, childHeightSpec)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            if (childHeight > layoutHeight) {
                if (heightMode == MeasureSpec.AT_MOST) {
                    layoutHeight = min(childHeight, maxLayoutHeight)
                } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                    layoutHeight = childHeight
                }
            }
            widthRemaining -= childWidth
            // Skip first child (list pane), the list pane is always a non-sliding pane.
            if (i == 0) {
                continue
            }
            lp.slideable = widthRemaining < 0
            canSlide = canSlide or lp.slideable
            if (lp.slideable) {
                mSlideableView = child
            }
        }
        // Second pass. Resolve weight.
        // Child views overlap when the combined width of child views are unable to fit into
        // the available width. Each of child views is sized to fill all available space. If
        // there is no overlap, distribute the extra width proportionally to weight.
        if (canSlide || weightSum > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == GONE) {
                    continue
                }
                val lp = child.layoutParams as LayoutParams
                val skippedFirstPass = lp.width == 0 && lp.weight > 0
                val measuredWidth = if (skippedFirstPass) 0 else child.measuredWidth
                var newWidth = measuredWidth
                var childWidthSpec = 0
                if (canSlide) {
                    // Child view consumes available space if the combined width is unable to
                    // fit into the layout available width.
                    val horizontalMargin = lp.leftMargin + lp.rightMargin
                    newWidth = widthAvailable - horizontalMargin
                    childWidthSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY)
                } else if (lp.weight > 0) {
                    // Distribute the extra width proportionally similar to LinearLayout
                    val widthToDistribute = max(0, widthRemaining)
                    val addedWidth = (lp.weight * widthToDistribute / weightSum).toInt()
                    newWidth = measuredWidth + addedWidth
                    childWidthSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY)
                }
                val childHeightSpec = measureChildHeight(
                    child, heightMeasureSpec,
                    paddingTop + paddingBottom
                )
                if (measuredWidth != newWidth) {
                    child.measure(childWidthSpec, childHeightSpec)
                    val childHeight = child.measuredHeight
                    if (childHeight > layoutHeight) {
                        if (heightMode == MeasureSpec.AT_MOST) {
                            layoutHeight = min(childHeight, maxLayoutHeight)
                        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                            layoutHeight = childHeight
                        }
                    }
                }
            }
        }

        // At this point, all child views have been measured. Calculate the device fold position
        // in the view. Update the split position to where the fold when it exists.
        val leftSplitBounds = tmpRect
        val rightSplitBounds = tmpRect2
        val hasFold = foldBoundsCalculator.splitViewPositions(foldingFeature, this, leftSplitBounds, rightSplitBounds)
        if (hasFold && !canSlide) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == GONE) {
                    continue
                }
                val splitView = if (i == 0) leftSplitBounds else rightSplitBounds
                val lp = child.layoutParams as LayoutParams

                // If child view is unable to fit in the separating view, expand the child view to fill
                // available space.
                val horizontalMargin = lp.leftMargin + lp.rightMargin
                val childHeightSpec = MeasureSpec.makeMeasureSpec(child.measuredHeight, MeasureSpec.EXACTLY)
                var childWidthSpec = MeasureSpec.makeMeasureSpec(splitView.width(), MeasureSpec.AT_MOST)
                child.measure(childWidthSpec, childHeightSpec)
                if ((child.minimumWidth != 0 && splitView.width() < child.minimumWidth)) {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(widthAvailable - horizontalMargin, MeasureSpec.EXACTLY)
                    child.measure(childWidthSpec, childHeightSpec)
                    // Skip first child (list pane), the list pane is always a non-sliding pane.
                    if (i == 0) {
                        continue
                    }
                    lp.slideable = true
                    canSlide = lp.slideable
                    mSlideableView = child
                } else {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(splitView.width(), MeasureSpec.EXACTLY)
                    child.measure(childWidthSpec, childHeightSpec)
                }
            }
        }
        val measuredHeight = layoutHeight + paddingTop + paddingBottom
        setMeasuredDimension(widthSize, measuredHeight)
        isSlideable = canSlide
        isTwoPaneMode = !isSlideable
        if (previousTwoPaneMode != isTwoPaneMode) {
            dispatchTwoPaneModeChanged()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val isLayoutRtl = isLayoutRtlSupport
        val width = r - l
        val paddingStart = if (isLayoutRtl) getPaddingRight() else getPaddingLeft()
        val paddingEnd = if (isLayoutRtl) getPaddingLeft() else getPaddingRight()
        val paddingTop = paddingTop
        val childCount = childCount
        var xStart = paddingStart
        var nextXStart = xStart
        if (mFirstLayout) {
            isOpened = isSlideable && mPreservedOpenState
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }
            val lp = child.layoutParams as LayoutParams
            val childWidth = child.measuredWidth
            val offset = 0
            if (lp.slideable) {
                val margin = lp.leftMargin + lp.rightMargin
                val range = (min(nextXStart, (width - paddingEnd)) - xStart - margin)
                val lpMargin = if (isLayoutRtl) lp.rightMargin else lp.leftMargin
                val pos = if (isOpened || foldingFeature == null) 0 else range
                xStart += pos + lpMargin
            } else {
                xStart = nextXStart
            }
            val childRight: Int
            val childLeft: Int
            if (isLayoutRtl) {
                childRight = width - xStart + offset
                childLeft = childRight - childWidth
            } else {
                childLeft = xStart - offset
                childRight = childLeft + childWidth
            }
            val childBottom = paddingTop + child.measuredHeight
            child.layout(childLeft, paddingTop, childRight, childBottom)

            // If a folding feature separates the content, we use its width as the extra
            // offset for the next child, in order to avoid rendering the content under it.
            var nextXOffset = 0
            if (foldingFeature != null && foldingFeature!!.orientation == VERTICAL && foldingFeature!!.isSeparating) {
                nextXOffset = foldingFeature!!.bounds.width()
            }
            nextXStart += child.width + abs(nextXOffset)
        }
        mFirstLayout = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Recalculate sliding panes and their details
        if (w != oldw) {
            mFirstLayout = true
        }
    }

    override fun requestChildFocus(child: View, focused: View) {
        super.requestChildFocus(child, focused)
        if (!isInTouchMode() && !isSlideable) {
            mPreservedOpenState = child === mSlideableView
        }
    }

    /**
     * Close the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now closed/in the process of closing
     */
    fun closePane(): Boolean {
        if (!isSlideable) {
            mPreservedOpenState = false
        }
        if (mFirstLayout || slideTo(false)) {
            mPreservedOpenState = false
            return true
        }
        return false
    }

    /**
     * Open the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now open/in the process of opening
     */
    fun openPane(): Boolean {
        if (!isSlideable) {
            mPreservedOpenState = true
        }
        if (mFirstLayout || slideTo(true)) {
            mPreservedOpenState = true
            return true
        }
        return false
    }

    /**
     * Open the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    override fun open() {
        openPane()
    }

    /**
     * Close the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    override fun close() {
        closePane()
    }

    /**
     * Check if the detail view is completely open. It can be open either because the slider
     * itself is open revealing the detail view, or if all content fits without sliding.
     *
     * @return true if the detail view is completely open
     */
    override fun isOpen(): Boolean = !isSlideable || isOpened

    /**
     * @param opened position to switch to
     */
    fun slideTo(opened: Boolean): Boolean {
        if (!isSlideable) {
            // Nothing to do.
            return false
        }
        val slideableView = mSlideableView
        isOpened = opened
        mFirstLayout = true
        requestLayout()
        invalidate()
        if (opened) dispatchOnPanelOpened(slideableView!!) else dispatchOnPanelClosed(slideableView!!)
        mPreservedOpenState = opened
        return true
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams = LayoutParams()

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams =
        if (p is MarginLayoutParams) LayoutParams(p) else LayoutParams(p)

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean =
        p is LayoutParams && super.checkLayoutParams(p)

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams = LayoutParams(context, attrs)

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.isOpen = if (isSlideable) isOpen else mPreservedOpenState
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        if (state.isOpen) {
            openPane()
        } else {
            closePane()
        }
        mPreservedOpenState = state.isOpen
    }

    class LayoutParams : MarginLayoutParams {
        /**
         * The weighted proportion of how much of the leftover space
         * this child should consume after measurement.
         */
        var weight = 0f

        /**
         * True if this pane is the slideable pane in the layout.
         */
        var slideable = false

        constructor() : super(MATCH_PARENT, MATCH_PARENT)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: ViewGroup.LayoutParams) : super(source)
        constructor(source: MarginLayoutParams) : super(source)
        constructor(source: LayoutParams) : super(source) {
            weight = source.weight
        }

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            val a = c.obtainStyledAttributes(attrs, ATTRS)
            weight = a.getFloat(0, 0f)
            a.recycle()
        }

        companion object {
            private val ATTRS = intArrayOf(
                android.R.attr.layout_weight
            )
        }
    }

    internal class SavedState : AbsSavedState {
        var isOpen = false

        constructor(superState: Parcelable?) : super(superState!!)
        constructor(i: Parcel, loader: ClassLoader?) : super(i, loader) {
            isOpen = i.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (isOpen) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : ClassLoaderCreator<SavedState> {
                override fun createFromParcel(i: Parcel, loader: ClassLoader): SavedState = SavedState(i, null)
                override fun createFromParcel(i: Parcel): SavedState = SavedState(i, null)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    internal inner class AccessibilityDelegate : AccessibilityDelegateCompat() {
        private val mTmpRect = Rect()
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
            val superNode = AccessibilityNodeInfoCompat.obtain(info)
            super.onInitializeAccessibilityNodeInfo(host, superNode)
            copyNodeInfoNoChildren(info, superNode)
            @Suppress("Deprecation")
            superNode.recycle()
            info.className = ACCESSIBILITY_CLASS_NAME
            info.setSource(host)
            val parent = host.parentForAccessibility
            if (parent is View) {
                info.setParent(parent as View)
            }

            // This is a best-approximation of addChildrenForAccessibility()
            // that accounts for filtering.
            val childCount = childCount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == VISIBLE) {
                    // Force importance to "yes" since the value is unable to be read.
                    child.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
                    info.addChild(child)
                }
            }
        }

        override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
            super.onInitializeAccessibilityEvent(host, event)
            event.setClassName(ACCESSIBILITY_CLASS_NAME)
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private fun copyNodeInfoNoChildren(dest: AccessibilityNodeInfoCompat, src: AccessibilityNodeInfoCompat) {
            val rect = mTmpRect
            src.getBoundsInScreen(rect)
            dest.setBoundsInScreen(rect)
            dest.isVisibleToUser = src.isVisibleToUser
            dest.packageName = src.packageName
            dest.className = src.className
            dest.contentDescription = src.contentDescription
            dest.isEnabled = src.isEnabled
            dest.isClickable = src.isClickable
            dest.isFocusable = src.isFocusable
            dest.isFocused = src.isFocused
            dest.isAccessibilityFocused = src.isAccessibilityFocused
            dest.isSelected = src.isSelected
            dest.isLongClickable = src.isLongClickable
            @Suppress("Deprecation")
            dest.addAction(src.actions)
            dest.movementGranularities = src.movementGranularities
        }
    }

    private val isLayoutRtlSupport: Boolean
        get() = layoutDirection == View.LAYOUT_DIRECTION_RTL


    /**
     * Utility for calculating layout positioning of child views relative to a [FoldingFeature].
     * This class is not thread-safe.
     */
    private class FoldBoundsCalculator {
        private val tmpIntArray = IntArray(2)
        private val splitViewPositionsTmpRect = Rect()
        private val getFoldBoundsInViewTmpRect = Rect()
        /**
         * Returns `true` if there is a split and [outLeftRect] and [outRightRect] contain the split
         * positions; false if there is not a compatible split available, [outLeftRect] and
         * [outRightRect] will remain unmodified.
         */
        fun splitViewPositions(
            foldingFeature: FoldingFeature?,
            parentView: View,
            outLeftRect: Rect,
            outRightRect: Rect,
        ): Boolean {
            if (foldingFeature == null) return false
            if (!foldingFeature.isSeparating) return false
            // Don't support horizontal fold in list-detail view layout
            if (foldingFeature.bounds.left == 0) return false
            // vertical split
            val splitPosition = splitViewPositionsTmpRect
            if (foldingFeature.bounds.top == 0 &&
                getFoldBoundsInView(foldingFeature, parentView, splitPosition)
            ) {
                outLeftRect.set(
                    parentView.paddingLeft,
                    parentView.paddingTop,
                    max(parentView.paddingLeft, splitPosition.left),
                    parentView.height - parentView.paddingBottom
                )
                val rightBound = parentView.width - parentView.paddingRight
                outRightRect.set(
                    min(rightBound, splitPosition.right),
                    parentView.paddingTop,
                    rightBound,
                    parentView.height - parentView.paddingBottom
                )
                return true
            }
            return false
        }
        /**
         * Returns `true` if [foldingFeature] overlaps with [view] and writes the bounds to [outRect].
         */
        private fun getFoldBoundsInView(
            foldingFeature: FoldingFeature,
            view: View,
            outRect: Rect
        ): Boolean {
            val viewLocationInWindow = tmpIntArray
            view.getLocationInWindow(viewLocationInWindow)
            val x = viewLocationInWindow[0]
            val y = viewLocationInWindow[1]
            val viewRect = getFoldBoundsInViewTmpRect.apply {
                set(x, y, x + view.width, y + view.width)
            }
            val foldRectInView = outRect.apply { set(foldingFeature.bounds) }
            // Translate coordinate space of split from window coordinate space to current view
            // position in window
            val intersects = foldRectInView.intersect(viewRect)
            // Check if the split is overlapped with the view
            if (foldRectInView.width() == 0 && foldRectInView.height() == 0 || !intersects) {
                return false
            }
            foldRectInView.offset(-x, -y)
            return true
        }
    }

    companion object {
        private const val TAG = "TwoPaneLayout"

        /** Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage.  */
        private const val ACCESSIBILITY_CLASS_NAME = "cx.ring.views.twopane.TwoPaneLayout"

        private fun measureChildHeight(child: View, spec: Int, padding: Int): Int {
            val lp = child.layoutParams as LayoutParams
            val childHeightSpec: Int
            val skippedFirstPass = lp.width == 0 && lp.weight > 0
            childHeightSpec = if (skippedFirstPass) {
                // This was skipped the first time; figure out a real height spec.
                getChildMeasureSpec(spec, padding, lp.height)
            } else {
                MeasureSpec.makeMeasureSpec(child.measuredHeight, MeasureSpec.EXACTLY)
            }
            return childHeightSpec
        }

        private fun getActivityOrNull(context: Context): Activity? {
            var iterator: Context? = context
            while (iterator is ContextWrapper) {
                if (iterator is Activity) {
                    return iterator
                }
                iterator = iterator.baseContext
            }
            return null
        }
    }
}
