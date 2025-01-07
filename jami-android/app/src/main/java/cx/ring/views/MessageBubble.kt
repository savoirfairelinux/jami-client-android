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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import cx.ring.R
import kotlin.math.ceil
import kotlin.math.max

/**
 * View that displays a text message with time and edited indicator.
 */
class MessageBubble(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private var calculatedCase: Case = Case.NEW_LINE
    private var measuredLastLineWidth: Int = 0

    // Dimensions
    private val infoTextSize = resources.getDimension(R.dimen.custom_message_bubble_info_text_size)
    private val defaultTextSize =
        resources.getDimension(R.dimen.custom_message_bubble_default_text_size)
    private val emojiOnlyTextSize = resources
        .getDimension(R.dimen.custom_message_bubble_emoji_only_text_size)
    private val infoLeftPadding = resources
        .getDimensionPixelSize(R.dimen.custom_message_bubble_info_left_padding)

    // Colors
    @ColorInt
    private var defaultTextColor: Int = Color.BLACK
    @ColorInt
    private var contrastedDefaultTextColor = context.getColor(R.color.colorOnSurface)

    private val messageText = WrapWidthTextView(context).apply {
        id = R.id.bubble_message_text
        autoLinkMask = Linkify.WEB_URLS
        movementMethod = LinkMovementMethod.getInstance()
        setTextIsSelectable(true)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
    }
    private val messageTime = TextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        isSingleLine = true
        maxLines = 1
        setTextSize(TypedValue.COMPLEX_UNIT_PX, infoTextSize)
        updatePadding(left = infoLeftPadding)
    }
    private val messageEdited = TextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        isSingleLine = true
        maxLines = 1
        setTextSize(TypedValue.COMPLEX_UNIT_PX, infoTextSize)
        updatePadding(left = infoLeftPadding)
        setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(context, R.drawable.pen_black_24dp), null, null, null)
        compoundDrawablePadding = resources
            .getDimensionPixelSize(R.dimen.custom_message_bubble_edited_drawable_padding)
    }
    private var linkPreview: ViewGroup? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.MessageBubble).use { a ->
            val message = a.getString(R.styleable.MessageBubble_message)
            val time = a.getString(R.styleable.MessageBubble_time)
            val edited = a.getBoolean(R.styleable.MessageBubble_edited, false)
            defaultTextColor = a.getColor(R.styleable.MessageBubble_android_textColor, context.getColor(R.color.colorOnSurface))
            messageText.text = message
            messageTime.text = time
            messageEdited.isVisible = edited
            updateTextColor(defaultTextColor)
        }
        addView(messageText)
        addView(messageTime)
        addView(messageEdited)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        super.setOnLongClickListener(l)
        messageText.setOnLongClickListener { l?.onLongClick(this) ?: false }
    }

    fun getText(): CharSequence = messageText.text

    /**
     * Updates the tint color of the bubble.
     */
    fun setBubbleColor(@ColorInt color: Int?) {
        if (color == null) {
            background?.setTintList(null)
            backgroundTintList = null
            return
        }
        background?.setTintList(ColorStateList.valueOf(color))
        backgroundTintList = ColorStateList.valueOf(color)
    }

    /**
     * Updates the view to display a standard message.
     */
    fun updateStandard(message: Spanned, time: String, messageIsEdited: Boolean) {
        messageEdited.isVisible = messageIsEdited
        messageText.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
            setTypeface(null, Typeface.NORMAL)
            setTextIsSelectable(true)
            text = message
        }
        messageTime.text = time
        updateTextColor(defaultTextColor)
    }

    /**
     * Updates the view to display a deleted message.
     */
    fun updateDeleted(time: String, username: String) {
        messageEdited.visibility = GONE
        messageTime.text = time
        messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
        messageText.setTypeface(null, Typeface.ITALIC)
        messageText.text = String.format(context.getString(R.string.conversation_message_deleted), username)
        messageText.setTextIsSelectable(false)

        background.setTint(context.getColor(R.color.conversation_secondary_background))
        updateTextColor(context.getColor(R.color.msg_display_name))
    }

    /**
     * Updates the view to display an emoji message.
     */
    fun updateEmoji(text: String, time: String, messageIsEdited: Boolean) {
        messageEdited.isVisible = messageIsEdited
        messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, emojiOnlyTextSize)
        messageText.setTypeface(null, Typeface.NORMAL)
        messageText.text = text
        messageText.setTextIsSelectable(true)
        messageTime.text = time
        // Emoji is not in the bubble, so should be contrasted with conversation background.
        updateTextColor(contrastedDefaultTextColor)
    }

    /**
     * Updates the color of the text.
     * The time and edited text have opacity added.
     */
    private fun updateTextColor(@ColorInt color: Int) {
        val colorAlpha60 = ColorUtils.setAlphaComponent(color, 0x99)
        messageText.setTextColor(color)
        messageTime.setTextColor(colorAlpha60)
        TextViewCompat.setCompoundDrawableTintList(
            messageEdited,
            ColorStateList.valueOf(colorAlpha60)
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (linkPreview == null) {
            linkPreview = findViewById(R.id.link_preview)
        }

        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        // Get current layout case.
        measuredLastLineWidth = if (messageText.lineCount > 0) ceil(messageText.layout.getLineWidth(messageText.lineCount - 1)).toInt() else 0
        calculatedCase = getCase(width)

        val desiredWidth = calculateDesiredWidth(calculatedCase)
        val desiredHeight = calculateDesiredHeight(calculatedCase)

        val link = linkPreview
        if (link?.visibility == VISIBLE) {
            val lp: LayoutParams = link.layoutParams
            val childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width)
            val childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, paddingTop, lp.height)
            link.measure(childWidthMeasureSpec, childHeightMeasureSpec)

            val previewWidth = linkPreview?.measuredWidth ?: 0
            val previewHeight = linkPreview?.measuredHeight ?: 0
            val measuredWidth = resolveSizeAndState(max(previewWidth, desiredWidth), widthMeasureSpec, messageText.measuredWidthAndState or link.measuredWidthAndState)
            val measuredHeight = resolveSizeAndState(desiredHeight + previewHeight, heightMeasureSpec, messageText.measuredHeightAndState or link.measuredHeightAndState)
            setMeasuredDimension(measuredWidth, measuredHeight)
        } else {
            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
            val measuredWidth = resolveSizeAndState(desiredWidth, widthSpec, messageText.measuredWidthAndState)
            val measuredHeight = resolveSizeAndState(desiredHeight, heightMeasureSpec, messageText.measuredHeightAndState)
            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }

    private fun calculateDesiredWidth(case: Case): Int {
        val timeAndEditedWidth = messageTime.measuredWidth +
            if (messageEdited.isVisible) messageEdited.measuredWidth else 0
        return when (case) {
            Case.NEW_LINE -> maxOf(messageText.measuredWidth, timeAndEditedWidth)
            Case.LAST_LINE -> maxOf(messageText.measuredWidth, measuredLastLineWidth + timeAndEditedWidth)
            Case.SINGLE_LINE -> messageText.measuredWidth + timeAndEditedWidth
        } + paddingRight + paddingLeft
    }

    private fun calculateDesiredHeight(case: Case): Int = when (case) {
        Case.NEW_LINE -> messageText.measuredHeight +
                maxOf(messageTime.measuredHeight,
                    if (messageEdited.isVisible) messageEdited.measuredHeight else 0)
        Case.LAST_LINE -> messageText.measuredHeight
        Case.SINGLE_LINE -> maxOf(messageText.measuredHeight, messageTime.measuredHeight, if (messageEdited.isVisible) messageEdited.measuredHeight else 0)
    } + paddingTop + paddingBottom

    private fun getCase(bubbleMaximumWidth: Int): Case {
        val messageTextWidth = messageText.measuredWidth
        val messageTimeWidth = messageTime.measuredWidth
        val messageEditedWidth = if (messageEdited.isVisible) messageEdited.measuredWidth else 0
        val messageInfoWidth = messageTimeWidth + messageEditedWidth
        val horizontalPadding = paddingLeft + paddingRight

        return if (messageText.lineCount > 1 || bubbleMaximumWidth < messageTextWidth + messageInfoWidth + horizontalPadding) {
            if (bubbleMaximumWidth < measuredLastLineWidth + messageInfoWidth + horizontalPadding)
                Case.NEW_LINE // Message is too long and time should be displayed on a new line.
            else Case.LAST_LINE // Message is too long but time can be displayed next to the last line.
        } else Case.SINGLE_LINE // Message is not too long and time can be displayed on the same line.
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Message always starts on the top-left side of the bubble.
        val textLeft: Int = paddingLeft
        val textRight: Int
        val textTop: Int = paddingTop
        val textBottom: Int = textTop + messageText.measuredHeight
        // Time is always displayed on the bottom-right side of the bubble.
        val timeEnd: Int = measuredWidth - paddingRight
        val timeStart: Int = timeEnd - messageTime.measuredWidth
        val timeBottom: Int
        // Edited is always aligned with the time.
        val editedEnd: Int = timeStart
        val editedStart: Int = editedEnd - messageEdited.measuredWidth

        when (calculatedCase) {
            Case.NEW_LINE -> {
                // Case 1: message is too long and time should be displayed on a new line.
                textRight = textLeft + messageText.measuredWidth
                timeBottom = textBottom + messageTime.measuredHeight
            }
            Case.LAST_LINE -> {
                // Case 2: message is too long but time can be displayed next to the last line.
                val minimumLineWidthRequired = measuredLastLineWidth + messageTime.measuredWidth +
                            if (messageEdited.isVisible) messageEdited.measuredWidth else 0
                val finalLineWidth = maxOf(messageText.measuredWidth, minimumLineWidthRequired)
                textRight = textLeft + finalLineWidth
                timeBottom = textBottom
            }
            Case.SINGLE_LINE -> {
                // Case 3: message is not too long and time can be displayed on the same line.
                textRight = editedStart
                timeBottom = textBottom
            }
        }
        val timeTop: Int = timeBottom - messageTime.measuredHeight

        messageTime.layout(timeStart, timeTop, timeEnd, timeBottom)
        messageText.layout(textLeft, textTop, textRight, textBottom)
        if (messageEdited.isVisible)
            messageEdited.layout(editedStart, timeTop, editedEnd, timeBottom)

        linkPreview?.let {
            val previewWidth = it.measuredWidth
            val previewHeight = it.measuredHeight
            val previewLeft = 0
            val previewTop = measuredHeight - previewHeight
            it.layout(previewLeft, previewTop, previewLeft + previewWidth, previewTop + previewHeight)
        }
    }

    private enum class Case {
        NEW_LINE, // Message is too long and time should be displayed on a new line.
        LAST_LINE, // Message is too long but time can be displayed next to the last line.
        SINGLE_LINE, // Message is not too long and time can be displayed on the same line.
    }
}