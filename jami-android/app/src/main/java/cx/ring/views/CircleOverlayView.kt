package cx.ring.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CircleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val scrimPaint = Paint().apply {
        color = 0x99000000.toInt() // Semi-transparent black
        style = Paint.Style.FILL
    }

    private val holePath = Path()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = Math.min(width, height) / 2f // full diameter
            
            holePath.reset()
            // Outer rectangle
            holePath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            // Inner circle (hole)
            holePath.addCircle(centerX, centerY, radius, Path.Direction.CCW)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(holePath, scrimPaint)
    }
}
