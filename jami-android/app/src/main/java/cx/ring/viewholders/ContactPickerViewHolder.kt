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
package cx.ring.viewholders

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.widget.CheckBox
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemContactBinding
import cx.ring.views.AvatarDrawable
import net.jami.model.Uri
import net.jami.smartlist.ConversationItemViewModel

class ContactPickerViewHolder(private val binding: ItemContactBinding) :
    RecyclerView.ViewHolder(binding.root) {
    private var currentUri: Uri? = null
    private var isChecked = false

    init {
        ViewCompat.setAccessibilityDelegate(itemView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = CheckBox::class.java.name
                info.text = binding.displayName.text
                info.isCheckable = true
                info.isChecked = isChecked
            }
        })
    }

    private fun fadeIn() = AlphaAnimation(0f, 1f).apply {
        interpolator = DecelerateInterpolator()
        duration = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    }

    fun bind(
        conversation: ConversationItemViewModel,
        checked: Boolean,
        onClick: (ConversationItemViewModel) -> Unit
    ) {
        if (conversation.uri != currentUri) {
            currentUri = conversation.uri
            binding.photo.setAvatar(null)
        }

        itemView.setOnClickListener { onClick(conversation) }

        binding.displayName.text = conversation.title
        val selectionChanged = isChecked != checked
        isChecked = checked
        binding.contactSelected.isVisible = checked
        if (selectionChanged) {
            itemView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        }
        val fade = binding.photo.setAvatar(
            AvatarDrawable.Builder()
                .withViewModel(conversation)
                .withCheck(false)
                .withCircleCrop(true)
                .build(binding.photo.context)
        )
        if (fade)
            binding.photo.startAnimation(fadeIn())
    }

    fun unbind() {
        binding.photo.setAvatar(null)
    }
}
