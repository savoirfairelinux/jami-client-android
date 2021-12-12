package cx.ring.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import cx.ring.R

class BadgeView : AppCompatTextView {

    private var mStrokeWidth = 0f
    private val mPadding = 0
    private var mBadgeColor = R.color.red_500
    private var mStrokeColor = R.color.white
    private var mDiameter = 0
    private var mRadius = 0
    private val mCirclePaint = Paint()
    private val mStrokePaint = Paint()

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
    }

    override fun onDraw(canvas: Canvas) {
        val h = this.height
        val w = this.width

        mDiameter = if (h > w) h else w
        mRadius = mDiameter / 2

        this.height = mDiameter
        this.width = mDiameter

        mCirclePaint.color = resources.getColor(mBadgeColor)
        mCirclePaint.flags = Paint.ANTI_ALIAS_FLAG
        mStrokePaint.color = resources.getColor(mStrokeColor)
        mStrokePaint.flags = Paint.ANTI_ALIAS_FLAG

        canvas.drawCircle(mRadius.toFloat(), mRadius.toFloat(), (mRadius + mPadding).toFloat(), mStrokePaint)
        canvas.drawCircle(mRadius.toFloat(), mRadius.toFloat(), mRadius - mStrokeWidth + mPadding, mCirclePaint)

        super.onDraw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val r = Math.max(measuredWidth, measuredHeight)
        setMeasuredDimension(r, r)
    }

    fun setStrokeWidth(dp: Int) {
        val scale = context.resources.displayMetrics.density
        mStrokeWidth = dp * scale
    }

    fun setStrokeColor(color: Int) {
        mStrokeColor = color
    }

    fun setBadgeColor(color: Int) {
        mBadgeColor = color
    }
}