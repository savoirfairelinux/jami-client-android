/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.viewholders

import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemSmartlistBinding
import cx.ring.views.AvatarDrawable
import net.jami.model.*
import net.jami.smartlist.ConversationItemViewModel

class ContactPickerViewHolder(b: ItemSmartlistBinding) :
    RecyclerView.ViewHolder(b.root) {
    private val binding: ItemSmartlistBinding = b
    private var currentUri: Uri? = null

    private fun fadeIn() = AlphaAnimation(0f, 1f).apply {
        interpolator = DecelerateInterpolator()
        duration = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    }

    fun bind(clickListener: ContactPickerListeners, conversation: ConversationItemViewModel) {
        if (conversation.uri != currentUri) {
            currentUri = conversation.uri
            binding.convLastItem.text = ""
            binding.convLastTime.text = ""
            binding.convParticipant.text = ""
            binding.photo.setImageDrawable(null)
        }

        itemView.setOnClickListener { clickListener.onItemClick(conversation) }
        itemView.setOnLongClickListener {
            clickListener.onItemLongClick(conversation)
            true
        }

        binding.convParticipant.text = conversation.title
        val fade = binding.photo.drawable !is AvatarDrawable
        binding.photo.setImageDrawable(AvatarDrawable.Builder()
            .withViewModel(conversation)
            .withCircleCrop(true)
            .build(binding.photo.context))
        if (fade)
            binding.photo.startAnimation(fadeIn())
    }

    fun unbind() {
        binding.photo.setImageDrawable(null)
    }

    interface ContactPickerListeners {
        fun onItemClick(item: ConversationItemViewModel)
        fun onItemLongClick(item: ConversationItemViewModel)
    }
}