/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import cx.ring.R
import kotlin.math.roundToInt

class ContactLabelsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FlexboxLayout(context, attrs) {
    private var displayedLabels: List<String>? = null
    private var compactMode = false

    init {
        alignItems = AlignItems.CENTER
        setDividerDrawable(GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setSize(dp(4), dp(4))
        })
        setShowDivider(SHOW_DIVIDER_MIDDLE)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun setLabels(labels: Collection<String>, compact: Boolean = false) {
        val ordered = labels.sorted()
        if (displayedLabels == ordered && compactMode == compact) return
        displayedLabels = ordered
        compactMode = compact
        removeAllViews()
        isVisible = ordered.isNotEmpty()
        contentDescription = context.getString(R.string.contact_labels_summary, ordered.joinToString(", "))
        flexWrap = if (compact) FlexWrap.NOWRAP else FlexWrap.WRAP
        justifyContent = if (compact) JustifyContent.FLEX_START else JustifyContent.FLEX_END
        val visible = if (compact) ordered.take(2) else ordered
        for (label in visible) addPill(label, compact)
        if (compact && ordered.isNotEmpty()) {
            addPill(context.getString(R.string.contact_labels_more, ordered.size - visible.size), compact, overflow = true)
            getChildAt(childCount - 1).isVisible = visible.size < ordered.size
        }
    }

    override fun getBaseline(): Int =
        if (childCount == 0) -1 else paddingTop + getChildAt(0).baseline

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!compactMode || childCount == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        // Leave at least half the title row available for the contact name.
        val width = if (mode == MeasureSpec.AT_MOST) MeasureSpec.getSize(widthMeasureSpec) / 2
            else MeasureSpec.getSize(widthMeasureSpec)
        val available = if (mode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
            else (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST)
        val childHeightSpec = getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom, LayoutParams.WRAP_CONTENT)
        val overflow = getChildAt(childCount - 1) as Chip
        val names = (0 until childCount - 1).map { getChildAt(it) }
        names.forEach { it.measure(childWidthSpec, childHeightSpec) }
        var shown = names.size
        while (true) {
            val remaining = checkNotNull(displayedLabels).size - shown
            val summary = context.getString(R.string.contact_labels_more, remaining)
            if (overflow.text.toString() != summary) overflow.text = summary
            overflow.isVisible = remaining > 0
            overflow.measure(childWidthSpec, childHeightSpec)
            val count = shown + if (overflow.isVisible) 1 else 0
            val needed = names.take(shown).sumOf { it.measuredWidth } +
                (if (overflow.isVisible) overflow.measuredWidth else 0) +
                dp(4) * (count - 1).coerceAtLeast(0)
            if (needed <= available || shown == 0) break
            shown--
        }
        names.forEachIndexed { index, view -> view.isVisible = index < shown }
        super.onMeasure(
            if (mode == MeasureSpec.AT_MOST) MeasureSpec.makeMeasureSpec(width, mode) else widthMeasureSpec,
            heightMeasureSpec
        )
    }

    private fun addPill(label: String, compact: Boolean, overflow: Boolean = false) {
        addView(Chip(context).apply {
            text = label
            textSize = 11f
            setTextColor(if (compact)
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
                else context.getColor(R.color.contact_label_details_text))
            chipBackgroundColor = ColorStateList.valueOf(context.getColor(
                if (compact) R.color.background_secondary else R.color.contact_label_details_background))
            chipStrokeWidth = 0f
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(dp(12).toFloat()).build()
            setEnsureMinTouchTargetSize(false)
            minWidth = 0
            minHeight = 0
            chipMinHeight = 0f
            chipStartPadding = dp(if (compact) 6 else 8).toFloat()
            chipEndPadding = chipStartPadding
            textStartPadding = 0f
            textEndPadding = 0f
            updatePadding(top = dp(4), bottom = dp(4), left = dp(4), right = dp(4))
            isCheckable = false
            isClickable = false
            isFocusable = false
            ellipsize = TextUtils.TruncateAt.END
            if (compact && !overflow) maxWidth = dp(96)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                minWidth = 0
                flexShrink = if (compact) 0f else 1f
            }
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
