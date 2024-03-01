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
import net.jami.model.Interaction
import net.jami.utils.Log

class MessageStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val iconSize = resources.getDimensionPixelSize(R.dimen.conversation_status_icon_size)
//    private val iconTint by lazy(LazyThreadSafetyMode.NONE) {
//        ColorStateList.valueOf(
//            ContextCompat.getColor(
//                context,
//                R.color.grey_500
//            )
//        )
//    }
    private val iconTint: ColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_500))
    //  Todo : private val iconTint: ColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_500))

    // Add or remove views to match the given count (corresponds to the number of contacts).
    private fun resize(count: Int) {
        if (count == 0) removeAllViews()
        else if (childCount > count)
            while (childCount > count)
                removeViewAt(childCount - 1)
        else if (childCount < count) {
            var i = childCount
            while (childCount < count) {
                addView(ImageView(context).apply {
                    layoutParams = LayoutParams(iconSize, iconSize).apply {
                        marginStart = if (i != 0) -iconSize / 3 else 0
                    }
                })
                i++
            }
        }
    }

    fun updateDisplayed(){}

    fun updateSending(){
        resize(1)
        (getChildAt(0) as ImageView).apply {
            setImageResource(R.drawable.sent)
            ImageViewCompat.setImageTintList(this, iconTint)
        }
    }

    fun updateSuccess(){
        resize(1)
        (getChildAt(0) as ImageView).apply {
            setImageResource(R.drawable.receive)
            ImageViewCompat.setImageTintList(this, iconTint)
        }

    }

    fun update(
        seenBy: Collection<ContactViewModel>,
        statusMap: Map<String, Interaction.MessageStates>,
        @IdRes resId: Int = View.NO_ID
    ) {



        val isSuccess = statusMap.values.find { it == Interaction.MessageStates.SUCCESS } != null
        val isSending = statusMap.isEmpty() or (statusMap.values.find { it == Interaction.MessageStates.SENDING } != null)
        val displayedList = statusMap.entries.filter { it.value == Interaction.MessageStates.DISPLAYED }.map { it.key }
        val displayedCount = displayedList.size
        val isDisplayed = displayedCount > 0

        val showStatus = (isSuccess || isSending ) && !isDisplayed

        Log.w("devdebug", "MessageStatusView.statusMap(): $statusMap, sucess: $isSuccess, sending: $isSending, displayed : $isDisplayed")

//        val showStatus = seenBy.isEmpty()
//                && (
//                status == Interaction.InteractionStatus.SUCCESS
//                        || status == Interaction.InteractionStatus.SENDING
//                )
        if (showStatus) {
            resize(1)
            (getChildAt(0) as ImageView).apply {
                setImageResource(
                    if(isSuccess) R.drawable.receive
                    else if(isSending) R.drawable.sent
                    else -1
//                    when (status) {
//                        Interaction.InteractionStatus.SENDING -> R.drawable.sent
//                        Interaction.InteractionStatus.SUCCESS -> R.drawable.receive
//                        else -> -1
//                    }
                )
                ImageViewCompat.setImageTintList(this, iconTint)
            }
        } else {
            Log.w("devdebug", "seenBy: $seenBy")
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
                }
            }
        }

        // Position the avatars correctly depending on the layout type.
        val fitRight = showStatus || seenBy.size < 2
        when (layoutParams) {
            is RelativeLayout.LayoutParams -> {
                val params = layoutParams as RelativeLayout.LayoutParams? ?: return
                if (fitRight) {
                    // Put the avatar on the right of the message if there is only one contact
                    params.removeRule(RelativeLayout.BELOW)
                    params.addRule(RelativeLayout.ALIGN_BOTTOM, resId)
                } else {
                    // Put the avatars below the message if there are multiple contacts
                    params.removeRule(RelativeLayout.ALIGN_BOTTOM)
                    params.addRule(RelativeLayout.BELOW, resId)
                }
                layoutParams = params
            }

            is ConstraintLayout.LayoutParams -> {
                val params = layoutParams as ConstraintLayout.LayoutParams? ?: return
                if (fitRight) {
                    // Put the avatar on the right of the message if there is only one contact
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    params.topToBottom = ConstraintLayout.LayoutParams.UNSET
                } else {
                    // Put the avatars below the message if there are multiple contacts
                    params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    params.topToBottom = resId
                }
                layoutParams = params
            }

            else -> Log.w(TAG, "Error layout params.")
        }
    }

    companion object {
        val TAG = MessageStatusView::class.simpleName!!
    }
}
