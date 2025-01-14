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
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import cx.ring.R
import net.jami.model.ContactViewModel
import net.jami.utils.Log

/**
 * MessageStatusView display the status of a message (sending, sent, displayed).
 * Sending and sent Status are displayed as icons, while Displayed status is displayed as avatars.
 */
class MessageStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    @IdRes
    private var attachedMessage: Int = View.NO_ID
    private val iconSize = resources.getDimensionPixelSize(R.dimen.conversation_status_icon_size)
    private val iconTint: ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_500))

    enum class IconState { NONE, SENDING, SUCCESS, DISPLAYED }
    var iconState = IconState.NONE
        private set

    // Add or remove views to match the given count.
    // "Sending" or "Sent" need 1 view, "Displayed" needs as many views as there are contacts.
    private fun resize(count: Int) {
        if (count == childCount) return

        // Update layout only if there is a change in the mode (empty, single or multiple).
        if (childCount == 0 || (count == 1 && childCount > 1) || (count > 1 && childCount == 1))
            layout(count)

        if (count == 0) removeAllViews()
        else if (childCount > count) while (childCount > count) removeViewAt(childCount - 1)
        else if (childCount < count) repeat(count - childCount) {
            addView(ImageView(context).apply {
                layoutParams = LayoutParams(iconSize, iconSize).apply {
                    marginStart = if (it != 0) -iconSize / 3 else 0
                }
            })
        }
    }

    // Layout the views depending on the count and the layout type.
    // If one view is displayed, it is put on the right of the message.
    // If multiple views are displayed, they are put below the message.
    private fun layout(count: Int) {
        val fitRight = count < 2
        when (layoutParams) {
            is RelativeLayout.LayoutParams -> {
                val params = layoutParams as RelativeLayout.LayoutParams? ?: return
                if (fitRight) {
                    // Put the view on the right of the message.
                    params.removeRule(RelativeLayout.BELOW)
                    params.addRule(RelativeLayout.ALIGN_BOTTOM, attachedMessage)
                } else {
                    // Put the view below the message.
                    params.removeRule(RelativeLayout.ALIGN_BOTTOM)
                    params.addRule(RelativeLayout.BELOW, attachedMessage)
                }
                layoutParams = params
            }

            is ConstraintLayout.LayoutParams -> {
                val params = layoutParams as ConstraintLayout.LayoutParams? ?: return
                if (fitRight) {
                    // Put the view on the right of the message.
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    params.topToBottom = ConstraintLayout.LayoutParams.UNSET
                } else {
                    // Put the view below the message.
                    params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    params.topToBottom = attachedMessage
                }
                layoutParams = params
            }

            else -> Log.w(TAG, "Error layout params.")
        }
    }

    fun attachToMessage(@IdRes resId: Int) {
        attachedMessage = resId
        layout(childCount)
    }

    fun updateNone() {
        visibility = View.GONE
        iconState = IconState.NONE
    }

    fun updateSending() {
        resize(1)
        (getChildAt(0) as ImageView).apply {
            setImageResource(R.drawable.sent)
            ImageViewCompat.setImageTintList(this, iconTint)
            iconState = IconState.SENDING
        }
        visibility = View.VISIBLE
    }

    fun updateSuccess() {
        resize(1)
        (getChildAt(0) as ImageView).apply {
            setImageResource(R.drawable.receive)
            ImageViewCompat.setImageTintList(this, null)
            iconState = IconState.SUCCESS
        }
        visibility = View.VISIBLE
    }

    fun updateDisplayed(seenBy: Collection<ContactViewModel>) {
        resize(seenBy.size)
        seenBy.forEachIndexed { index, contact ->
            (getChildAt(index) as ImageView).apply {
                imageTintList = null
                setImageDrawable(
                    AvatarDrawable.Builder()
                        .withCircleCrop(true)
                        .withContact(contact)
                        .withPresence(false)
                        .build(context)
                )
                iconState = IconState.DISPLAYED
            }
        }
        visibility = View.VISIBLE
    }

    companion object {
        val TAG = MessageStatusView::class.simpleName!!
    }
}
