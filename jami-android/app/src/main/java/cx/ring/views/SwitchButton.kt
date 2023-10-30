/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.CompoundButton
import androidx.core.content.res.ResourcesCompat
import cx.ring.R
import kotlin.math.*

class SwitchButton(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : CompoundButton(context, attrs, defStyle) {
    private var mBackColor: Int
    private val mThumbSize: Int
    private var mBackWidth = 0
    private var mBackHeight = 0
    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private val mClickTimeout: Int = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout()
    private var mThumbRadius = 0f
    private var mBackRadius = 0f
    private var mTextWidth = 0f
    private var mTextHeight = 0f
    private var mProgress = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mLastX = 0f
    private var mReady = false
    private var mCatch = false
    private var mShowImage = false
    private val mThumbPos = PointF()
    private val mPresentThumbPos = PointF()
    private val mBackRectF = RectF()
    private val mSafeRectF = RectF()
    private val mTextOnRectF = RectF()
    private val mTextOffRectF = RectF()
    private val mThumbMargin = RectF()
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mProgressAnimator = ValueAnimator.ofFloat(0f, 0f).apply {
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { valueAnimator -> progress = valueAnimator.animatedValue as Float }
        duration = DEFAULT_ANIMATION_DURATION.toLong()
    }
    private var mStatus: CharSequence?
    private var mOnLayout: Layout? = null
    private var mOffLayout: Layout? = null
    private val mImageDrawable: RotateDrawable
    private var mChangingState = false
    private var mChildOnCheckedChangeListener: OnCheckedChangeListener? = null

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    private fun makeLayout(text: CharSequence?): Layout {
        return StaticLayout(text, paint, ceil(Layout.getDesiredWidth(text, paint)).toInt(), Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mOnLayout == null && !TextUtils.isEmpty(mStatus)) {
            mOnLayout = makeLayout(mStatus)
        }
        if (mOffLayout == null && !TextUtils.isEmpty(mStatus)) {
            mOffLayout = makeLayout(mStatus)
        }
        val defaultWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_SWITCH_WIDTH.toFloat(), resources.displayMetrics)
        mTextWidth = defaultWidth
        val onHeight = if (mOnLayout != null) mOnLayout!!.height.toFloat() else 0f
        val offHeight = if (mOffLayout != null) mOffLayout!!.height.toFloat() else 0f
        mTextHeight = if (onHeight != 0f || offHeight != 0f) max(onHeight, offHeight) else 0f
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    /**
     * SwitchButton use this formula to determine the final size of thumb, background and itself.
     *
     *
     * textWidth = max(onWidth, offWidth)
     * thumbRange = thumbWidth * rangeRatio
     * textExtraSpace = textWidth + textExtra - (moveRange - thumbWidth + max(thumbMargin.left, thumbMargin.right) + textThumbInset)
     * backWidth = thumbRange + thumbMargin.left + thumbMargin.right + max(textExtraSpace, 0)
     * contentSize = thumbRange + max(thumbMargin.left, 0) + max(thumbMargin.right, 0) + max(textExtraSpace, 0)
     *
     * @param widthMeasureSpec widthMeasureSpec
     * @return measuredWidth
     */
    private fun measureWidth(widthMeasureSpec: Int): Int {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measuredWidth: Int
        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSize
            mBackWidth = widthSize - paddingLeft - paddingRight
        } else {
            /*
            If parent view want SwitchButton to determine it's size itself, we calculate the minimal
            size of it's content. Further more, we ignore the limitation of widthSize since we want
            to display SwitchButton in its actual size rather than compress the shape.
             */
            mBackWidth = max(ceil((mThumbSize + mThumbMargin.left + mThumbMargin.right + mTextWidth).toDouble()), 0)
            measuredWidth = mBackWidth + paddingLeft + paddingRight
        }
        return measuredWidth
    }

    private fun measureHeight(heightMeasureSpec: Int): Int {
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var measuredHeight = heightSize
        val contentSize: Int
        val textExtraSpace: Int
        if (heightMode == MeasureSpec.EXACTLY) {
            mBackHeight = ceil((mThumbSize + mThumbMargin.top + mThumbMargin.bottom)).toInt()
            mBackHeight = ceil(max(mBackHeight.toFloat(), mTextHeight)).toInt()
        } else {
            mBackHeight = ceil((mThumbSize + mThumbMargin.top + mThumbMargin.bottom).toDouble())
            textExtraSpace = ceil((mTextHeight - mBackHeight).toDouble())
            if (textExtraSpace > 0) {
                mBackHeight += textExtraSpace
            }
            contentSize = max(mThumbSize, mBackHeight)
            measuredHeight = max(contentSize, contentSize + paddingTop + paddingBottom)
            measuredHeight = max(measuredHeight, suggestedMinimumHeight)
        }
        return measuredHeight
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            setup()
        }
    }

    private fun setup() {
        if (mBackWidth == 0 || mBackHeight == 0) {
            return
        }
        mThumbRadius = mThumbSize / 2f
        mBackRadius = min(mBackWidth, mBackHeight) / 2f
        val contentWidth = measuredWidth - paddingLeft - paddingRight
        val contentHeight = measuredHeight - paddingTop - paddingBottom

        // max range of drawing content, when thumbMargin is negative, drawing range is larger than backWidth
        val drawingWidth = mBackWidth
        val drawingHeight = mBackHeight
        val thumbTop: Float = if (contentHeight <= drawingHeight) {
            paddingTop + mThumbMargin.top
        } else {
            // center vertical in content area
            paddingTop + mThumbMargin.top + (contentHeight - drawingHeight + 1) / 2f
        }
        val thumbLeft: Float = if (contentWidth <= mBackWidth) {
            paddingLeft + mThumbMargin.left
        } else {
            paddingLeft + mThumbMargin.left + (contentWidth - drawingWidth + 1) / 2f
        }
        mThumbPos.set(thumbLeft + mThumbRadius, thumbTop + mThumbRadius)
        val backLeft = thumbLeft - mThumbMargin.left
        mBackRectF.set(backLeft, thumbTop - mThumbMargin.top, backLeft + mBackWidth, thumbTop - mThumbMargin.top + mBackHeight)
        mSafeRectF.set(thumbLeft, 0f, mBackRectF.right - mThumbMargin.right - mThumbSize, 0f)
        val minBackRadius = min(mBackRectF.width(), mBackRectF.height()) / 2f
        mBackRadius = min(minBackRadius, mBackRadius)
        mOnLayout?.let { onLayout ->
            val onLeft = mBackRectF.left + (mBackRectF.width() - mThumbSize - mThumbMargin.right - onLayout.width) / 2f
            val onTop = mBackRectF.top + (mBackRectF.height() - onLayout.height) / 2
            mTextOnRectF.set(onLeft, onTop, onLeft + onLayout.width, onTop + onLayout.height)
        }
        mOffLayout?.let { offLayout ->
            val offLeft = mBackRectF.right - (mBackRectF.width() - mThumbSize - mThumbMargin.left - offLayout.width) / 2f - offLayout.width
            val offTop = mBackRectF.top + (mBackRectF.height() - offLayout.height) / 2
            mTextOffRectF.set(offLeft, offTop, offLeft + offLayout.width, offTop + offLayout.height)
        }
        val dWidth = mImageDrawable.intrinsicWidth
        val dHeight = mImageDrawable.intrinsicHeight
        val dTop = ceil((mBackRectF.top + (mBackRectF.height() - dHeight) / 2).toDouble())
        mImageDrawable.setBounds(
            ((mBackWidth - mThumbSize) / 2 - dWidth / 2),
            dTop,
            ((mBackWidth - mThumbSize) / 2 + dWidth / 2),
            dTop + dHeight
        )
        mReady = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!mReady) {
            setup()
        }
        if (!mReady) {
            return
        }
        mPaint.color = mBackColor
        canvas.drawRoundRect(mBackRectF, mBackRadius, mBackRadius, mPaint)

        // thumb
        mPresentThumbPos.set(mThumbPos)
        mPresentThumbPos.offset(mProgress * mSafeRectF.width(), 0f)
        mPaint.color = Color.WHITE
        canvas.drawCircle(mPresentThumbPos.x, mPresentThumbPos.y, mThumbRadius, mPaint)

        // image
        if (mShowImage) {
            mImageDrawable.draw(canvas)
        } else {
            // text
            val switchText = if (progress > 0.5) mOnLayout else mOffLayout
            val textRectF = if (progress > 0.5) mTextOnRectF else mTextOffRectF
            if (switchText != null) {
                val alpha: Float =
                    if (progress >= 0.75) progress * 4 - 3 else if (progress < 0.25) 1 - progress * 4 else 0f
                switchText.paint.alpha = (Color.alpha(currentTextColor) * alpha).toInt()
                canvas.save()
                canvas.translate(textRectF.left, textRectF.top)
                switchText.draw(canvas)
                canvas.restore()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !isClickable || !isFocusable || !mReady) {
            return false
        }
        val action = event.action
        val deltaX = event.x - mStartX
        val deltaY = event.y - mStartY
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = event.x
                mStartY = event.y
                mLastX = mStartX
                isPressed = true
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                progress += (x - mLastX) / mSafeRectF.width()
                mLastX = x
                if (!mCatch && (abs(deltaX) > mTouchSlop / 2f || abs(deltaY) > mTouchSlop / 2f)) {
                    if (deltaY == 0f || abs(deltaX) > abs(deltaY)) {
                        catchView()
                    } else if (abs(deltaY) > abs(deltaX)) {
                        return false
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mCatch = false
                isPressed = false
                val time = (event.eventTime - event.downTime).toFloat()
                if (abs(deltaX) < mTouchSlop && abs(deltaY) < mTouchSlop && time < mClickTimeout) {
                    performClick()
                } else {
                    val nextStatus = progress > 0.5f
                    if (nextStatus != isChecked) {
                        playSoundEffect(SoundEffectConstants.CLICK)
                        isChecked = nextStatus
                    } else {
                        animateToState(nextStatus)
                    }
                }
            }
            else -> {
            }
        }
        return true
    }

    private var progress: Float
        get() = mProgress
        set(progress) {
            var tempProgress = progress
            if (tempProgress > 1) {
                tempProgress = 1f
            } else if (tempProgress < 0) {
                tempProgress = 0f
            }
            mProgress = tempProgress
            invalidate()
        }

    override fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        mChildOnCheckedChangeListener = listener
    }

    private fun animateToState(checked: Boolean) {
        if (mProgressAnimator.isRunning) {
            mProgressAnimator.cancel()
        }
        mProgressAnimator.setFloatValues(mProgress, if (checked) 1f else 0f)
        mProgressAnimator.start()
    }

    private fun catchView() {
        val parent = parent
        parent?.requestDisallowInterceptTouchEvent(true)
        mCatch = true
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked == checked) {
            return
        }
        animateToState(checked)
        super.setChecked(checked)
    }

    fun setCheckedSilent(checked: Boolean) {
        mChangingState = true
        super.setChecked(checked)
        progress = if (checked) 1f else 0f
        mChangingState = false
    }

    var backColor: Int
        get() = mBackColor
        set(backColor) {
            mBackColor = backColor
            invalidate()
        }
    var status: CharSequence?
        get() = mStatus
        set(status) {
            mStatus = status
            mOnLayout = null
            mOffLayout = null
            mReady = false
            requestLayout()
            invalidate()
        }

    fun showImage(show: Boolean) {
        mShowImage = show
        invalidate()
    }

    fun startImageAnimation() {
        val anim = ObjectAnimator.ofInt(mImageDrawable, "level", 0, 10000)
        anim.duration = 500
        anim.repeatCount = ValueAnimator.INFINITE
        anim.start()
    }

    companion object {
        const val DEFAULT_THUMB_SIZE_DP = 20
        const val DEFAULT_THUMB_MARGIN_DP = 2
        const val DEFAULT_ANIMATION_DURATION = 250
        const val DEFAULT_SWITCH_WIDTH = 72
        private fun ceil(dimen: Double): Int {
            return kotlin.math.ceil(dimen).toInt()
        }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SwitchButton)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.SwitchButton, attrs, ta, defStyle, 0)
        }
        val backColor = ta.getColor(R.styleable.SwitchButton_backColor, resources.getColor(R.color.grey_400))
        val status = ta.getString(R.styleable.SwitchButton_status)
        ta.recycle()
        val layerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.rotate, context.theme) as LayerDrawable?
        mImageDrawable = layerDrawable!!.findDrawableByLayerId(R.id.progress) as RotateDrawable
        isFocusable = true
        isClickable = true
        mStatus = status
        mBackColor = backColor
        val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_THUMB_MARGIN_DP.toFloat(), resources.displayMetrics)
        mThumbMargin.set(margin, margin, margin, margin)
        mThumbSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_THUMB_SIZE_DP.toFloat(), resources.displayMetrics).toInt()
        // sync checked status
        progress = if (isChecked) 1f else 0f
        super.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (mChangingState) return@setOnCheckedChangeListener
            mChildOnCheckedChangeListener?.onCheckedChanged(buttonView, isChecked)
        }
    }
}