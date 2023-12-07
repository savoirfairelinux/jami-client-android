package cx.ring.adapters

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class BaselineLastLineTextView : AppCompatTextView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun getBaseline(): Int {
        val layout = layout ?: return super.getBaseline()
        val baselineOffset = super.getBaseline() - layout.getLineBaseline(0)
        return baselineOffset + layout.getLineBaseline(layout.lineCount - 1)
    }
}