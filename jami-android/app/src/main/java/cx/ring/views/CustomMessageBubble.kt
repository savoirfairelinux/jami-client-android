package cx.ring.views

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import cx.ring.R
import cx.ring.linkpreview.PreviewData

/**
 * Custom view that displays a message with time and edited indicator.
 */
class CustomMessageBubble(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private val messageText = AppCompatTextView(context)
    private val messageTime = TextView(context)
    private val messageEdited = TextView(context)

    private val linkPreviewVerticalPadding = resources
        .getDimensionPixelSize(R.dimen.custom_message_bubble_link_preview_vertical_padding)
    private val linkPreviewHorizontalPadding = resources
        .getDimensionPixelSize(R.dimen.custom_message_bubble_link_preview_horizontal_padding)
    private val linkPreviewTitle = TextView(context).apply {
        visibility = GONE
        updatePadding(
            top = linkPreviewVerticalPadding,
            bottom = linkPreviewVerticalPadding,
            right = linkPreviewHorizontalPadding,
            left = linkPreviewHorizontalPadding
        )
    }
    private val linkPreviewDescription = TextView(context).apply {
        visibility = GONE
        maxLines = 4
        updatePadding(
            top = linkPreviewVerticalPadding,
            bottom = linkPreviewVerticalPadding,
            right = linkPreviewHorizontalPadding,
            left = linkPreviewHorizontalPadding
        )
    }
    private val linkPreviewUrl = TextView(context).apply {
        visibility = GONE
        updatePadding(
            top = linkPreviewVerticalPadding,
            bottom = linkPreviewVerticalPadding,
            right = linkPreviewHorizontalPadding,
            left = linkPreviewHorizontalPadding
        )
    }
    private val linkPreviewImage = ImageView(context).apply {
        layoutParams = LayoutParams(
            resources.getDimensionPixelSize(R.dimen.link_preview_image_size),
            resources.getDimensionPixelSize(R.dimen.link_preview_image_size)
        ).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
//        updateLayoutParams {
////            width = resources.getDimensionPixelSize(R.dimen.link_preview_image_size)
////            height = resources.getDimensionPixelSize(R.dimen.link_preview_image_size)
//            scaleType = ImageView.ScaleType.CENTER_CROP
//        }
        visibility = GONE
    }
    private var isLinkPreview = false

    private var calculatedCase: Case = Case.CASE_NEW_LINE
    private var maximumLineWidth: Int = 0

    // Dimensions
    private val infoTextSize = resources.getDimension(R.dimen.custom_message_bubble_info_text_size)
    private val defaultTextSize =
        resources.getDimension(R.dimen.custom_message_bubble_default_text_size)
    private val emojiOnlyTextSize = resources
        .getDimension(R.dimen.custom_message_bubble_emoji_only_text_size)
    private val infoLeftPadding = resources
        .getDimensionPixelSize(R.dimen.custom_message_bubble_info_left_padding)
    private val editedDrawablePadding = resources
        .getDimensionPixelSize(R.dimen.custom_message_bubble_edited_drawable_padding)

    // Colors
    private var defaultTextColor = context.getColor(R.color.colorOnSurface)
    private var contrastedDefaultTextColor = context.getColor(R.color.colorOnSurface)

    init {
        this.messageText.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
        }
        this.messageTime.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, infoTextSize)
            updatePadding(left = infoLeftPadding)
        }
        this.messageEdited.apply {
            val editedDrawable = ContextCompat.getDrawable(this.context, R.drawable.pen_black_24dp)
            updatePadding(left = infoLeftPadding)
            setCompoundDrawablesWithIntrinsicBounds(
                editedDrawable, null, null, null
            )
            compoundDrawablePadding = editedDrawablePadding
        }

        addView(this.messageText)
        addView(this.messageTime)
        addView(this.messageEdited)
        addView(this.linkPreviewTitle)
        addView(this.linkPreviewDescription)
        addView(this.linkPreviewUrl)
        addView(this.linkPreviewImage)

        context.theme.obtainStyledAttributes( // Get custom attributes and apply them.
            attrs, R.styleable.CustomMessageBubble, 0, 0
        ).apply {
            try {
                val message = getString(R.styleable.CustomMessageBubble_message)
                val time = getString(R.styleable.CustomMessageBubble_time)
                val edited = getBoolean(R.styleable.CustomMessageBubble_edited, false)
                val withLinkPreview = getBoolean(R.styleable.CustomMessageBubble_withLinkPreview, false)
                defaultTextColor =
                    getColor(R.styleable.CustomMessageBubble_android_textColor, defaultTextColor)
                messageText.text = message
                messageTime.text = time
                messageEdited.isVisible = edited
                isLinkPreview = withLinkPreview
                if(isLinkPreview) {
                    linkPreviewTitle.visibility = VISIBLE
                    linkPreviewDescription.visibility = VISIBLE
                    linkPreviewUrl.visibility = VISIBLE
                    linkPreviewTitle.text = resources.getString(R.string.fake_link_preview_title)
                    linkPreviewDescription.text =
                        resources.getString(R.string.fake_link_preview_description)
                    linkPreviewUrl.text = resources.getString(R.string.fake_link_preview_url)
                    linkPreviewImage.setImageResource(R.drawable.jami_banner)
                }

                updateColor(defaultTextColor)
            } finally {
                recycle()
            }
        }
    }

    fun getText(): CharSequence {
        return messageText.text
    }

    /**
     * Updates the view to display a standard message.
     */
    fun updateStandard(
        messageText: Spanned,
        messageTime: String,
        messageIsEdited: Boolean,
        previewData: PreviewData? = null,
    ) {
        isLinkPreview = previewData != null
        if (previewData != null) {
            Log.w("devdebug", "updateStandard: LINKPREVIEW ON previewData: $previewData")
            // Update the image preview
            if (previewData.imageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(previewData.imageUrl)
                    .centerCrop()
                    .into(linkPreviewImage)
                linkPreviewImage.visibility = View.VISIBLE
            } else linkPreviewImage.visibility = View.GONE

            // Update title, description and url
            linkPreviewTitle.text = previewData.title
            linkPreviewTitle.visibility = View.VISIBLE
            if (previewData.description.isNotEmpty()) {
                linkPreviewDescription.visibility = View.VISIBLE
                linkPreviewDescription.text = previewData.description
            } else linkPreviewDescription.visibility = View.GONE
            val url = Uri.parse(previewData.baseUrl)
            linkPreviewUrl.text = url.host
            linkPreviewUrl.visibility = View.VISIBLE

            // Manage click to open the link
            listOf(linkPreviewImage, linkPreviewTitle, linkPreviewDescription, linkPreviewUrl)
                .forEach {
                    it.setOnClickListener {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url))
                    }
                }
        }
        else {
            Log.w("devdebug", "updateStandard: LINKPREVIEW OFF")
            linkPreviewTitle.visibility = View.GONE
            linkPreviewDescription.visibility = View.GONE
            linkPreviewUrl.visibility = View.GONE
            linkPreviewImage.visibility = View.GONE
        }

        this.messageEdited.isVisible = messageIsEdited
        this.messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
        this.messageText.text = messageText
        this.messageTime.text = messageTime
        updateColor(defaultTextColor)
    }

    /**
     * Updates the view to display a deleted message.
     */
    fun updateDeleted(messageTime: String) {
        messageEdited.visibility = GONE
        this.messageTime.text = messageTime
        messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize)
        messageText.text = resources.getString(R.string.conversation_message_deleted)
        updateColor(defaultTextColor)
    }

    /**
     * Updates the view to display an emoji message.
     */
    fun updateEmoji(messageText: String, messageTime: String, messageIsEdited: Boolean) {
        this.messageEdited.isVisible = messageIsEdited
        this.messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, emojiOnlyTextSize)
        this.messageText.text = messageText
        this.messageTime.text = messageTime
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

        // Measure the maximum line width of the message text.
        maximumLineWidth = (0..<messageText.lineCount)
            .maxOfOrNull { messageText.layout.getLineWidth(it) }?.toInt() ?: 0

        val desiredWidth: Int
        val desiredHeight: Int

        // Get current layout case.
        if (isLinkPreview) {
            calculatedCase = getCase(MeasureSpec.getSize(widthMeasureSpec)).let {
                when (it) {
                    Case.CASE_NEW_LINE -> Case.CASE_NEW_LINE
                    Case.CASE_LAST_LINE -> Case.CASE_LAST_LINE
                    Case.CASE_SINGLE_LINE -> Case.CASE_LAST_LINE
                }
            }
            desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
            Log.w("devdebug", "desiredWidth: linkPreviewTitle.measuredHeight: ${linkPreviewTitle.measuredHeight} isVisible: ${linkPreviewTitle.isVisible}")
            Log.w("devdebug", "desiredWidth: linkPreviewDescription.measuredHeight: ${linkPreviewDescription.measuredHeight} isVisible: ${linkPreviewDescription.isVisible}")
            Log.w("devdebug", "desiredWidth: linkPreviewUrl.measuredHeight: ${linkPreviewUrl.measuredHeight} isVisible: ${linkPreviewUrl.isVisible}")
            Log.w("devdebug", "desiredWidth: linkPreviewImage.measuredHeight: ${linkPreviewImage.measuredHeight} isVisible: ${linkPreviewImage.isVisible}")
            desiredHeight = calculateDesiredHeight(calculatedCase) + maxOf(
                linkPreviewTitle.measuredHeight +
                linkPreviewDescription.measuredHeight +
                linkPreviewUrl.measuredHeight,
                linkPreviewImage.measuredHeight)
        } else {
            calculatedCase = getCase(MeasureSpec.getSize(widthMeasureSpec))
            desiredWidth = calculateDesiredWidth(calculatedCase)
            desiredHeight = calculateDesiredHeight(calculatedCase)
        }

//        val desiredWidth =
//            if (isLinkPreview) MeasureSpec.getSize(widthMeasureSpec)
//            else calculateDesiredWidth(calculatedCase)
//        val desiredHeight =
//            if (isLinkPreview) calculateDesiredHeight(Case.CASE_NEW_LINE)
//            else calculateDesiredHeight(calculatedCase)

        val measuredWidth = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val measuredHeight =
            resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun calculateDesiredWidth(case: Case): Int = when (case) {
        Case.CASE_NEW_LINE -> {
            val timeAndEditedWidth = if (messageEdited.isVisible) messageEdited.measuredWidth else 0
            maxOf(maximumLineWidth, messageTime.measuredWidth + timeAndEditedWidth)
        }

        Case.CASE_LAST_LINE -> {
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

        Case.CASE_SINGLE_LINE -> maximumLineWidth + messageTime.measuredWidth +
                if (messageEdited.isVisible) messageEdited.measuredWidth else 0
    } + paddingStart + paddingEnd

    private fun calculateDesiredHeight(case: Case): Int = when (case) {
        Case.CASE_NEW_LINE -> messageText.measuredHeight +
                maxOf(
                    messageTime.measuredHeight,
                    if (messageEdited.isVisible) messageEdited.measuredHeight else 0
                )

        Case.CASE_LAST_LINE, Case.CASE_SINGLE_LINE -> maxOf(
            messageText.measuredHeight,
            messageTime.measuredHeight,
            if (messageEdited.isVisible) messageEdited.measuredHeight else 0
        )
    } + paddingTop + paddingBottom

    private fun getCase(bubbleMaximumWidth: Int): Case {
        val messageLineCount = messageText.lineCount
        val messageTextWidth = maximumLineWidth
        val messageTimeWidth = messageTime.measuredWidth
        val messageEditedWidth = if (messageEdited.isVisible) messageEdited.measuredWidth else 0
        val messageInfoWidth = messageTimeWidth + messageEditedWidth
        val horizontalPadding = paddingStart + paddingEnd

        return if (bubbleMaximumWidth < messageTextWidth + messageInfoWidth + horizontalPadding) {
            val messageTextLastLineWidth =
                messageText.layout.getLineWidth(messageLineCount - 1)
            if (bubbleMaximumWidth < messageTextLastLineWidth + messageInfoWidth + horizontalPadding)
                Case.CASE_NEW_LINE // Message is too long and time should be displayed on a new line.
            else Case.CASE_LAST_LINE // Message is too long but time can be displayed next to the last line.
        } else Case.CASE_SINGLE_LINE // Message is not too long and time can be displayed on the same line.
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
            Case.CASE_NEW_LINE -> {
                // Case 1: message is too long and time should be displayed on a new line.
                textTop = paddingTop
                textBottom = textTop + messageText.measuredHeight
                textStart = paddingStart
                textEnd = textStart + maximumLineWidth

                timeTop = textBottom
                timeBottom = timeTop + messageTime.measuredHeight
                timeEnd = measuredWidth - paddingEnd
                timeStart = timeEnd - messageTime.measuredWidth

                editedTop = textBottom
                editedBottom = editedTop + messageEdited.measuredHeight
                editedEnd = timeStart
                editedStart = timeStart - messageEdited.measuredWidth
            }

            Case.CASE_LAST_LINE -> {
                // Case 2: message is too long but time can be displayed next to the last line.
                val minimumLineWidthRequired =
                    messageText.layout.getLineWidth(messageText.lineCount - 1).toInt() +
                            messageTime.measuredWidth +
                            if (messageEdited.isVisible) messageEdited.measuredWidth else 0
                val finalLineWidth = maxOf(maximumLineWidth, minimumLineWidthRequired)

                timeTop = messageText.getLastBaseline() - messageTime.baseline + paddingTop
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

            Case.CASE_SINGLE_LINE -> {
                // Case 3: message is not too long and time can be displayed on the same line.
                timeTop = messageText.getLastBaseline() - messageTime.baseline + paddingTop
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

        if (isLinkPreview) {
            val imageTop: Int = timeBottom
            val imageStart: Int = paddingStart
//            val imageBottom: Int = imageTop + linkPreviewImage.measuredHeight
            val imageEnd: Int = imageStart + linkPreviewImage.measuredWidth

            val titleTop: Int = timeBottom
            val titleEnd: Int = timeEnd
            val titleStart: Int = imageEnd
            val titleBottom: Int = titleTop + linkPreviewTitle.measuredHeight

            val descriptionTop: Int = titleBottom
            val descriptionEnd: Int = titleEnd
            val descriptionStart: Int = titleStart
            val descriptionBottom: Int = descriptionTop + linkPreviewDescription.measuredHeight

            val urlTop: Int = descriptionBottom
            val urlEnd: Int = descriptionEnd
            val urlStart: Int = descriptionStart
            val urlBottom: Int = urlTop + linkPreviewUrl.measuredHeight

            val imageBottom: Int = imageTop + linkPreviewImage.measuredHeight



            linkPreviewTitle.layout(titleStart, titleTop, titleEnd, titleBottom)
            linkPreviewDescription.layout(descriptionStart, descriptionTop, descriptionEnd, descriptionBottom)
            linkPreviewUrl.layout(urlStart, urlTop, urlEnd, urlBottom)
            linkPreviewImage.layout(imageStart, imageTop, imageEnd, imageBottom)
        }
    }

    private enum class Case {
        CASE_NEW_LINE, // Message is too long and time should be displayed on a new line.
        CASE_LAST_LINE, // Message is too long but time can be displayed next to the last line.
        CASE_SINGLE_LINE, // Message is not too long and time can be displayed on the same line.
    }

    /**
     * TextView extension that returns the baseline of the last line.
     */
    private fun TextView.getLastBaseline(): Int {
        val layout = layout ?: return baseline
        val baselineOffset = baseline - layout.getLineBaseline(0)
        return baselineOffset + layout.getLineBaseline(layout.lineCount - 1)
    }
}