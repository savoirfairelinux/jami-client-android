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
package cx.ring.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import cx.ring.databinding.ItemContactBinding
import cx.ring.viewholders.ContactPickerViewHolder
import net.jami.smartlist.ConversationItemViewModel

class ContactPickerAdapter(
    private val onClick: (ConversationItemViewModel) -> Unit,
) : ListAdapter<ConversationItemViewModel, ContactPickerViewHolder>(ContactDiffCallback) {
    private var selectedContactUris: Set<String> = emptySet()

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactPickerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ContactPickerViewHolder(ItemContactBinding.inflate(layoutInflater, parent, false))
    }

    override fun onViewRecycled(holder: ContactPickerViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun onBindViewHolder(holder: ContactPickerViewHolder, position: Int) {
        val conversation = getItem(position)
        val checked = conversation.getContact()?.contact?.uri?.rawUriString in selectedContactUris
        holder.bind(conversation, checked, onClick)
    }

    fun setSelection(uris: Set<String>) {
        val changed = (selectedContactUris - uris) + (uris - selectedContactUris)
        selectedContactUris = uris.toSet()
        currentList.forEachIndexed { index, item ->
            if (item.getContact()?.contact?.uri?.rawUriString in changed) {
                notifyItemChanged(index)
            }
        }
    }

    private object ContactDiffCallback : DiffUtil.ItemCallback<ConversationItemViewModel>() {
        override fun areItemsTheSame(
            oldItem: ConversationItemViewModel,
            newItem: ConversationItemViewModel
        ): Boolean = oldItem.accountId == newItem.accountId && oldItem.uri == newItem.uri

        override fun areContentsTheSame(
            oldItem: ConversationItemViewModel,
            newItem: ConversationItemViewModel
        ): Boolean = oldItem == newItem
    }
}
