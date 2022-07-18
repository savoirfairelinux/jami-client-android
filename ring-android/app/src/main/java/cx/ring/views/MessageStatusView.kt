package cx.ring.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import net.jami.model.ContactViewModel

class MessageStatusView : LinearLayout {
    val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, context.resources.displayMetrics).toInt()

    //var statusDrawable: Drawable? = null

    constructor(context: Context) : super(context) {
        //init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        //init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        //init(attrs, defStyle)
    }

    /*private fun init(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.AvatarStackView, defStyle, 0)
        if (a.hasValue(R.styleable.AvatarStackView_statusDrawable)) {
            statusDrawable = a.getDrawable(R.styleable.AvatarStackView_statusDrawable)
        }
        a.recycle()
    }*/

    private fun resize(count: Int) {
        if (count == 0) {
            removeAllViews()
        } else if (childCount > count) {
            while (childCount > count) {
                removeViewAt(childCount - 1)
            }
        } else if (childCount < count) {
            var i = childCount
            while (childCount < count) {
                addView(ImageView(context).apply {
                    layoutParams = LayoutParams(size, size).apply {
                        marginStart = if (i != 0) -size/3  else 0
                    }
                })
                i++
            }
        }
    }

    fun update(contacts: Collection<ContactViewModel>) {
        resize(contacts.size)
        contacts.forEachIndexed { index, contact ->
            (getChildAt(index) as ImageView).setImageDrawable(AvatarDrawable.Builder()
                .withCircleCrop(true)
                .withContact(contact)
                .withPresence(false)
                .build(context))
        }
    }
}
