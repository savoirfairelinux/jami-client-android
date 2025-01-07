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
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.viewholders.BlockListViewHolder
import cx.ring.viewholders.BlockListViewHolder.BlockListListeners
import net.jami.model.ContactViewModel

class BlockListAdapter(viewModels: Collection<ContactViewModel>, listener: BlockListListeners) :
    RecyclerView.Adapter<BlockListViewHolder>() {
    private val mListener: BlockListListeners = listener
    private val mBlacklisted: ArrayList<ContactViewModel> = ArrayList(viewModels)
    fun replaceAll(viewModels: Collection<ContactViewModel>) {
        val diffCallback = ContactDiffCallback(mBlacklisted, viewModels.toList())
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        mBlacklisted.clear()
        mBlacklisted.addAll(viewModels)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockListViewHolder {
        val holderView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_blacklist, parent, false)
        return BlockListViewHolder(holderView)
    }

    override fun onBindViewHolder(holder: BlockListViewHolder, position: Int) {
        val contact = mBlacklisted[position]
        holder.bind(mListener, contact)
    }

    override fun getItemCount(): Int {
        return mBlacklisted.size
    }

}

class ContactDiffCallback(
    private val oldList: List<ContactViewModel>,
    private val newList: List<ContactViewModel>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].contact.uri == newList[newItemPosition].contact.uri
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].contact.uri == newList[newItemPosition].contact.uri
    }
}