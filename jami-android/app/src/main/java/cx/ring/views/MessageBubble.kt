/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
import android.content.res.ColorStateList
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import cx.ring.R

/**
 * View that displays a text message with time and edited indicator.
 */
class MessageBubble(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private var calculatedCase: Case = Case.NEW_LINE

    // Dimensions
    private val infoTextSize = resources.getDimension(R.dimen.custom_message_bubble_info_text_size)
    private val defaultTextSize =
        resources.getDimension(R.dimen.custom_message_bubble_default_text_size)
    private val emojiOnlyTextSize = resources
        .getDimension(R.dimen.custom_message_bubble_emoji_only_text_size)
    private val infoLeftPadding = resources
        .getDimensionPixelSize(R.dimen.custom_message_bubble_info_left_padding)

    // Colors
    private val defaultTextColor: Int
    private var contrastedDefaultTextColor = context.getColor(R.color.colorOnSurface)

    private val messageText = WrapWidthTextView(context).apply {
        movementMethod = LinkMovementMethod.getInstance()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
    }
    private val messageTime = TextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, infoTextSize)
        updatePadding(left = infoLeftPadding)
    }
    private val messageEdited = TextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, infoTextSize)
        updatePadding(left = infoLeftPadding)
        setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(context, R.drawable.pen_black_24dp), null, null, null)
        compoundDrawablePadding = resources
            .getDimensionPixelSize(R.dimen.custom_message_bubble_edited_drawable_padding)
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.MessageBubble).use { a ->
            val message = a.getString(R.styleable.MessageBubble_message)
            val time = a.getString(R.styleable.MessageBubble_time)
            val edited = a.getBoolean(R.styleable.MessageBubble_edited, false)
            defaultTextColor = a.getColor(R.styleable.MessageBubble_android_textColor, context.getColor(R.color.colorOnSurface))
            messageText.text = message
            messageTime.text = time
            messageEdited.isVisible = edited
            updateColor(defaultTextColor)
        }
        addView(messageText)
        addView(messageTime)
        addView(messageEdited)
    }

    fun getText(): CharSequence = messageText.text

    /**
     * Updates the view to display a standard message.
     */
    fun updateStandard(text: Spanned, time: String, messageIsEdited: Boolean) {
        messageEdited.isVisible = messageIsEdited
        messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
        messageText.text = text
        messageTime.text = time
        updateColor(defaultTextColor)
    }

    /**
     * Updates the view to display a deleted message.
     */
    fun updateDeleted(time: String, username: String) {
        messageEdited.visibility = GONE
        messageTime.text = time
        messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
        messageText.text = String.format(context.getString(R.string.conversation_message_deleted), username)
        updateColor(defaultTextColor)
    }

    /**
     * Updates the view to display an emoji message.
     */
    fun updateEmoji(text: String, time: String, messageIsEdited: Boolean) {
        messageEdited.isVisible = messageIsEdited
        messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, emojiOnlyTextSize)
        messageText.text = text
        messageTime.text = time
        // Emoji is not in the bubble, so should be contrasted with conversation background.
        updateColor(contrastedDefaultTextColor)
    }

    /**
     * Updates the color of the text.
     * The time and edited text have opacity added.
     */
    private fun updateColor(color: Int) {
        val colorAlpha60 = ColorUtils.setAlphaComponent(color, 0x99)
        messageText.setTextColor(color)
        messageTime.setTextColor(colorAlpha60)
        TextViewCompat.setCompoundDrawableTintList(
            messageEdited,
            ColorStateList.valueOf(colorAlpha60)
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        // Get current layout case.
        calculatedCase = getCase(width)

        val desiredWidth = calculateDesiredWidth(calculatedCase)
        val desiredHeight = calculateDesiredHeight(calculatedCase)

        val widthAtMostSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
        val measuredWidth = resolveSizeAndState(desiredWidth, widthAtMostSpec, messageText.measuredWidthAndState)
        val measuredHeight = resolveSizeAndState(desiredHeight, heightMeasureSpec, messageText.measuredHeightAndState)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun calculateDesiredWidth(case: Case): Int = when (case) {
        Case.NEW_LINE -> {
            val timeAndEditedWidth = if (messageEdited.isVisible) messageEdited.measuredWidth else 0
            maxOf(messageText.measuredWidth, messageTime.measuredWidth + timeAndEditedWidth)
        }
        Case.LAST_LINE -> messageText.measuredWidth
        Case.SINGLE_LINE -> messageText.measuredWidth + messageTime.measuredWidth +
                if (messageEdited.isVisible) messageEdited.measuredWidth else 0
    } + paddingRight + paddingLeft

    private fun calculateDesiredHeight(case: Case): Int = when (case) {
        Case.NEW_LINE -> messageText.measuredHeight +
                maxOf(messageTime.measuredHeight,
                    if (messageEdited.isVisible) messageEdited.measuredHeight else 0)
        Case.LAST_LINE, Case.SINGLE_LINE -> messageText.measuredHeight
    } + paddingTop + paddingBottom

    private fun getCase(bubbleMaximumWidth: Int): Case {
        val messageTextWidth = messageText.measuredWidth
        val messageTimeWidth = messageTime.measuredWidth
        val messageEditedWidth = if (messageEdited.isVisible) messageEdited.measuredWidth else 0
        val messageInfoWidth = messageTimeWidth + messageEditedWidth
        val horizontalPadding = paddingLeft + paddingRight

        val case = if (messageText.lineCount > 1 || bubbleMaximumWidth < messageTextWidth + messageInfoWidth + horizontalPadding) {
            val messageTextLastLineWidth = messageText.layout.getLineWidth(messageText.lineCount - 1)
            if (bubbleMaximumWidth < messageTextLastLineWidth + messageInfoWidth + horizontalPadding)
                Case.NEW_LINE // Message is too long and time should be displayed on a new line.
            else Case.LAST_LINE // Message is too long but time can be displayed next to the last line.
        } else Case.SINGLE_LINE // Message is not too long and time can be displayed on the same line.
        return case
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Message always starts on the top-left side of the bubble.
        val textLeft: Int = paddingLeft
        val textRight: Int
        val textTop: Int = paddingTop
        val textBottom: Int
        // Time is always displayed on the bottom-right side of the bubble.
        val timeEnd: Int = measuredWidth - paddingRight
        val timeStart: Int = timeEnd - messageTime.measuredWidth
        val timeBottom: Int = measuredHeight - paddingBottom
        val timeTop: Int = timeBottom - messageTime.measuredHeight
        // Edited is always aligned with the time.
        val editedEnd: Int = timeStart
        val editedStart: Int = editedEnd - messageEdited.measuredWidth

        when (calculatedCase) {
            Case.NEW_LINE -> {
                // Case 1: message is too long and time should be displayed on a new line.
                textBottom = textTop + messageText.measuredHeight
                textRight = textLeft + messageText.measuredWidth
            }
            Case.LAST_LINE -> {
                // Case 2: message is too long but time can be displayed next to the last line.
                val minimumLineWidthRequired =
                    messageText.layout.getLineWidth(messageText.lineCount - 1).toInt() +
                            messageTime.measuredWidth +
                            if (messageEdited.isVisible) messageEdited.measuredWidth else 0
                val finalLineWidth = maxOf(messageText.measuredWidth, minimumLineWidthRequired)
                textBottom = textTop + messageText.measuredHeight
                textRight = textLeft + finalLineWidth
            }
            Case.SINGLE_LINE -> {
                // Case 3: message is not too long and time can be displayed on the same line.
                textBottom = textTop + messageText.measuredHeight
                textRight = editedStart
            }
        }
        messageTime.layout(timeStart, timeTop, timeEnd, timeBottom)
        messageText.layout(textLeft, textTop, textRight, textBottom)
        if (messageEdited.isVisible)
            messageEdited.layout(editedStart, timeTop, editedEnd, timeBottom)
    }

    private enum class Case {
        NEW_LINE, // Message is too long and time should be displayed on a new line.
        LAST_LINE, // Message is too long but time can be displayed next to the last line.
        SINGLE_LINE, // Message is not too long and time can be displayed on the same line.
    }
}