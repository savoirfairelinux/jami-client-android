package cx.ring.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColor
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import cx.ring.R

data class Message(
    val messageText: String,
    val messageTime: String,
    val messageEdited: Boolean,
)

enum class Case {
    CASE_1, // Message is too long and time should be displayed on a new line.
    CASE_2, // Message is too long but time can be displayed next to the last line.
    CASE_3, // Message is not too long and time can be displayed on the same line.
}

/**
 * Custom view that displays a message with time and edited indicator.
 */
class CustomMessageBubble(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private lateinit var messageText: TextView
    private lateinit var messageTime: TextView
    private lateinit var messageEdited: TextView

    private lateinit var calculatedCase: Case
    private var maximumLineWidth: Int = 0

    // TODO: Move all these values to resources.
    val textColor = Color.WHITE
    val infoTextColor = Color.parseColor("#99FFFFFF")
    val infoTextSize = 12f
    val infoLeftPadding = resources.displayMetrics.density.toInt() * 8
    val editedDrawable = ContextCompat.getDrawable(this.context, R.drawable.pen_black_24dp)
    val editedDrawablePadding = resources.displayMetrics.density.toInt() * 2
    val editedText = "Edited"

    init {
        initialize()
        context.theme.obtainStyledAttributes(attrs, R.styleable.CustomMessageBubble, 0, 0).apply {
            try {
                val message = getString(R.styleable.CustomMessageBubble_message)
                val time = getString(R.styleable.CustomMessageBubble_time)
                val edited = getBoolean(R.styleable.CustomMessageBubble_edited, false)
                val color = getColor(R.styleable.CustomMessageBubble_android_textColor, textColor)
                val colorAlpha60 = ColorUtils.setAlphaComponent(color, 0x99)
                messageText.text = message
                messageText.setTextColor(color)
                messageTime.text = time
                messageTime.setTextColor(colorAlpha60)
                messageEdited.isVisible = edited
                messageEdited.setTextColor(colorAlpha60)
                messageEdited.compoundDrawableTintList = ColorStateList.valueOf(colorAlpha60)
            } finally {
                recycle()
            }
        }
    }

    private fun initialize() {



        this.messageText = BaselineLastLineTextView(context).apply {
            setTextColor(textColor)
        }
        this.messageTime = TextView(context).apply {
            setTextColor(infoTextColor)
            textSize = infoTextSize
            updatePadding(left = infoLeftPadding)
        }
        this.messageEdited = TextView(context).apply {
            text = editedText
            textSize = infoTextSize
            updatePadding(left = infoLeftPadding)
            setTextColor(infoTextColor)
            setCompoundDrawablesWithIntrinsicBounds(editedDrawable, null, null, null)
            compoundDrawablePadding = editedDrawablePadding
            TextViewCompat.setCompoundDrawableTintList(
                this,
                ColorStateList.valueOf(infoTextColor)
            )
        }

        addView(this.messageText)
        addView(this.messageTime)
        addView(this.messageEdited)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        // Measure the maximum line width of the message text.
        maximumLineWidth = (0..<messageText.lineCount)
            .maxOfOrNull { messageText.layout.getLineWidth(it) }?.toInt() ?: 0

        // Get current layout case.
        calculatedCase = getCase(MeasureSpec.getSize(widthMeasureSpec))

        val desiredWidth = calculateDesiredWidth(calculatedCase)
        val desiredHeight = calculateDesiredHeight(calculatedCase)

        val measuredWidth = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val measuredHeight = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun calculateDesiredWidth(case: Case): Int = when (case) {
        Case.CASE_1 -> {
            val timeAndEditedWidth = if (messageEdited.isVisible) messageEdited.measuredWidth else 0
            maxOf(maximumLineWidth, messageTime.measuredWidth + timeAndEditedWidth)
        }
        Case.CASE_2 -> {
            // The message has multiple lines and the time should be displayed next to the last
            // line.
            // The width should be at least the last line width + time width + edited width.
            // The width should be at most the maximum line width.
            val minimumLineWidthRequired =
                messageText.layout.getLineWidth(messageText.lineCount - 1).toInt() +
                        messageTime.measuredWidth +
                        if (messageEdited.isVisible) messageEdited.measuredWidth else 0
            maxOf(maximumLineWidth, minimumLineWidthRequired)
        }

        Case.CASE_3 -> maximumLineWidth + messageTime.measuredWidth +
                if (messageEdited.isVisible) messageEdited.measuredWidth else 0
    } + paddingStart + paddingEnd

    private fun calculateDesiredHeight(case: Case): Int = when (case) {
        Case.CASE_1 -> messageText.measuredHeight +
                maxOf(
                    messageTime.measuredHeight,
                    if (messageEdited.isVisible) messageEdited.measuredHeight else 0
                )

        Case.CASE_2, Case.CASE_3 -> maxOf(
            messageText.measuredHeight,
            messageTime.measuredHeight,
            if (messageEdited.isVisible) messageEdited.measuredHeight else 0
        )
    } + paddingTop + paddingBottom

    fun update(message: Message) {
        messageText.text = message.messageText
        messageTime.text = message.messageTime
        messageEdited.visibility = if (message.messageEdited) VISIBLE else GONE
    }

    fun updateEmoji(message:Message){
        messageText.text = message.messageText
        messageTime.isVisible = false
        messageEdited.isVisible = false
    }

    private fun getCase(bubbleMaximumWidth: Int): Case {
        val messageLineCount = messageText.lineCount
        val messageTextWidth = maximumLineWidth
        val messageTimeWidth = messageTime.measuredWidth
        val messageEditedWidth = if (messageEdited.isVisible) messageEdited.measuredWidth else 0
        val messageInfoWidth = messageTimeWidth + messageEditedWidth
        val horizontalPadding = paddingStart + paddingEnd

        return if (bubbleMaximumWidth < messageTextWidth + messageInfoWidth + horizontalPadding) {
            val messageTextLastLineWidth = messageText.layout.getLineWidth(messageLineCount - 1)
            if (bubbleMaximumWidth < messageTextLastLineWidth + messageInfoWidth + horizontalPadding)
                Case.CASE_1 // Message is too long and time should be displayed on a new line.
            else Case.CASE_2 // Message is too long but time can be displayed next to the last line.
        } else Case.CASE_3 // Message is not too long and time can be displayed on the same line.
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val textTop: Int
        val textEnd: Int
        val textStart: Int
        val textBottom: Int

        val timeTop: Int
        val timeEnd: Int
        val timeStart: Int
        val timeBottom: Int

        val editedTop: Int
        val editedEnd: Int
        val editedStart: Int
        val editedBottom: Int

        when (calculatedCase) {
            Case.CASE_1 -> {
                // Case 1: message is too long and time should be displayed on a new line.
                textTop = paddingTop
                textBottom = textTop + messageText.measuredHeight
                textStart = paddingStart
                textEnd = textStart + maximumLineWidth

                timeTop = textBottom
                timeBottom = timeTop + messageTime.measuredHeight
                timeEnd = measuredWidth - paddingEnd
                timeStart = timeEnd- messageTime.measuredWidth

                editedTop = textBottom
                editedBottom = editedTop + messageEdited.measuredHeight
                editedEnd = timeStart
                editedStart = timeStart - messageEdited.measuredWidth
            }

            Case.CASE_2 -> {
                // Case 2: message is too long but time can be displayed next to the last line.
                val minimumLineWidthRequired =
                    messageText.layout.getLineWidth(messageText.lineCount - 1).toInt() +
                            messageTime.measuredWidth +
                            if (messageEdited.isVisible) messageEdited.measuredWidth else 0
                val finalLineWidth = maxOf(maximumLineWidth, minimumLineWidthRequired)

                timeTop = messageText.baseline - messageTime.baseline + paddingTop
                timeBottom = timeTop + messageTime.measuredHeight
                timeEnd = measuredWidth - paddingEnd
                timeStart = timeEnd - messageTime.measuredWidth

                editedTop = timeTop
                editedBottom = timeBottom
                editedEnd = timeStart
                editedStart = editedEnd - messageEdited.measuredWidth

                textTop = paddingTop
                textBottom = textTop + messageText.measuredHeight
                textStart = paddingStart
                textEnd = textStart + finalLineWidth
            }

            Case.CASE_3 -> {
                // Case 3: message is not too long and time can be displayed on the same line.
                timeTop = messageText.baseline - messageTime.baseline + paddingTop
                timeBottom = timeTop + messageTime.measuredHeight
                timeEnd = measuredWidth
                timeStart = timeEnd - messageTime.measuredWidth - paddingEnd

                editedTop = timeTop
                editedBottom = timeBottom
                editedEnd = timeStart
                editedStart = editedEnd -
                        if (messageEdited.isVisible) messageEdited.measuredWidth else 0

                textTop = paddingTop
                textBottom = textTop + messageText.measuredHeight
                textEnd = editedStart
                textStart = textEnd - maximumLineWidth
            }
        }
        messageTime.layout(timeStart, timeTop, timeEnd, timeBottom)
        messageText.layout(textStart, textTop, textEnd, textBottom)
        if (messageEdited.isVisible)
            messageEdited.layout(editedStart, editedTop, editedEnd, editedBottom)
    }
}

/**
 * Custom TextView that returns the baseline of the last line.
 */
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