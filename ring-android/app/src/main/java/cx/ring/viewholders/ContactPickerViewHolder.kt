/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
import cx.ring.databinding.ItemSmartlistHeaderBinding
import cx.ring.views.AvatarDrawable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.*
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel

class ContactPickerViewHolder : RecyclerView.ViewHolder {
    private val binding: ItemSmartlistBinding?
    private val headerBinding: ItemSmartlistHeaderBinding?
    private val compositeDisposable = CompositeDisposable()
    private var currentUri: Uri? = null

    constructor(b: ItemSmartlistBinding, parentDisposable: CompositeDisposable) : super(b.root) {
        binding = b
        headerBinding = null
        parentDisposable.add(compositeDisposable)
    }

    constructor(b: ItemSmartlistHeaderBinding, parentDisposable: CompositeDisposable) : super(b.root) {
        binding = null
        headerBinding = b
        parentDisposable.add(compositeDisposable)
    }

    private fun fadeIn() = AlphaAnimation(0f, 1f).apply {
        interpolator = DecelerateInterpolator()
        duration = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    }

    fun bind(clickListener: ContactPickerListeners, conversation: ConversationItemViewModel) {
        compositeDisposable.clear()
        if (binding != null) {
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

            binding.convParticipant.text = conversation.contactName
            val fade = binding.photo.drawable !is AvatarDrawable
            binding.photo.setImageDrawable(AvatarDrawable.Builder()
                .withViewModel(conversation)
                .withCircleCrop(true)
                .build(binding.photo.context))
            if (fade)
                binding.photo.startAnimation(fadeIn())
        }
    }

    fun unbind() {
        binding?.photo?.setImageDrawable(null)
        compositeDisposable.clear()
    }

    interface ContactPickerListeners {
        fun onItemClick(item: ConversationItemViewModel)
        fun onItemLongClick(item: ConversationItemViewModel)
    }
}