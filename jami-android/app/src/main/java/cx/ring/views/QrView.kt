/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import cx.ring.R
import cx.ring.utils.BitmapUtils
import net.jami.utils.QRCodeUtils
import kotlin.math.min

class QrView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    @ColorInt
    private var foregroundColor: Int
    @ColorInt
    private var backgroundColor: Int

    var text: String? = null
        set(value) {
            field = value
            refresh()
        }

    private var mQrCodeBitmap: Bitmap? = null
    private val qrDestRect: Rect = Rect()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.QrView)
        foregroundColor = typedArray.getColor(R.styleable.QrView_foregroundColor, context.getColor(R.color.black))
        backgroundColor = typedArray.getColor(R.styleable.QrView_backgroundColor, context.getColor(R.color.qr_background))
        text = typedArray.getString(R.styleable.QrView_text)
        typedArray.recycle()
    }

    fun setForegroundColor(value: Int) {
        foregroundColor = value
        refresh()
    }

    override fun setBackgroundColor(value: Int) {
        backgroundColor = value
        refresh()
    }

    private fun refresh() {
        mQrCodeBitmap?.recycle()
        val data = QRCodeUtils.encodeStringAsQRCodeData(text, foregroundColor, backgroundColor)
        mQrCodeBitmap = if (data != null) BitmapUtils.qrToBitmap(data) else null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        mQrCodeBitmap?.let { canvas.drawBitmap(it, null, qrDestRect, null) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val paddingW = paddingLeft + paddingRight
        val paddingH = paddingTop + paddingBottom
        val size = min(measuredWidth - paddingW, measuredHeight - paddingH)
        setMeasuredDimension(size + paddingW, size + paddingH)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val width = w - paddingLeft - paddingRight
        val height = h - paddingTop - paddingBottom
        val size = min(width, height)
        qrDestRect.set(
            (width - size) / 2 + paddingLeft,
            (height - size) / 2 + paddingTop,
            (width + size) / 2 + paddingLeft,
            (height + size) / 2 + paddingTop
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mQrCodeBitmap?.recycle()
    }
}