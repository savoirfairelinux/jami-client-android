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
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemContactBlacklistBinding
import net.jami.model.Contact
import cx.ring.views.AvatarFactory
import net.jami.model.ContactViewModel

class BlockListViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
    private val binding: ItemContactBlacklistBinding = ItemContactBlacklistBinding.bind(view)
    fun bind(clickListener: BlockListListeners, contact: ContactViewModel) {
        AvatarFactory.loadGlideAvatar(binding.photo, contact)
        binding.displayName.text = contact.displayUri
        binding.unblock.setOnClickListener { clickListener.onUnblockClicked(contact.contact) }
    }

    interface BlockListListeners {
        fun onUnblockClicked(contact: Contact)
    }
}