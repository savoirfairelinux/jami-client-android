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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemContactBinding
import cx.ring.viewholders.ContactPickerViewHolder
import cx.ring.viewholders.ContactPickerViewHolder.ContactPickerListeners
import net.jami.smartlist.ConversationItemViewModel

class ContactPickerAdapter(
    private var conversations: List<ConversationItemViewModel>?,
    private val listener: ContactPickerListeners,
) : RecyclerView.Adapter<ContactPickerViewHolder>() {
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactPickerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ContactPickerViewHolder(ItemContactBinding.inflate(layoutInflater, parent, false))
    }

    override fun onViewRecycled(holder: ContactPickerViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun onBindViewHolder(holder: ContactPickerViewHolder, position: Int) {
        conversations!![position].let { conversation ->
            return holder.bind(listener, conversation)
        }
    }

    override fun getItemCount(): Int = conversations?.size ?: 0

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(conversations: List<ConversationItemViewModel>) {
        this.conversations = conversations
        notifyDataSetChanged()
    }

    fun update(conversation: ConversationItemViewModel) {
        val index = conversations!!.indexOf(conversation)
        notifyItemChanged(index)
    }

}