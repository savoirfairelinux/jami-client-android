/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemSmartlistBinding
import cx.ring.databinding.ItemSmartlistHeaderBinding
import cx.ring.viewholders.SmartListViewHolder
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.smartlist.ConversationItemViewModel

class SmartListAdapter(
    conversationItemViewModels: List<ConversationItemViewModel>?,
    private val listener: SmartListListeners,
    private val mDisposable: CompositeDisposable
) : RecyclerView.Adapter<SmartListViewHolder>() {
    private var mConversationItemViewModels: MutableList<ConversationItemViewModel> = if (conversationItemViewModels != null) ArrayList(conversationItemViewModels) else ArrayList()
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val itemBinding = ItemSmartlistBinding.inflate(layoutInflater, parent, false)
            SmartListViewHolder(itemBinding, mDisposable)
        } else {
            val itemBinding = ItemSmartlistHeaderBinding.inflate(layoutInflater, parent, false)
            SmartListViewHolder(itemBinding, mDisposable)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val smartListViewModel = mConversationItemViewModels[position]
        return if (smartListViewModel.headerTitle == ConversationItemViewModel.Title.None) 0 else 1
    }

    override fun onViewRecycled(holder: SmartListViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun onBindViewHolder(holder: SmartListViewHolder, position: Int) {
        holder.bind(listener, mConversationItemViewModels[position])
    }

    override fun getItemCount(): Int {
        return mConversationItemViewModels.size
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    fun update(viewModels: MutableList<ConversationItemViewModel>?) {
        val old: List<ConversationItemViewModel> = mConversationItemViewModels
        mConversationItemViewModels = viewModels ?: ArrayList()
        if (viewModels != null) {
            val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
            DiffUtil.calculateDiff(SmartListDiffUtil(old, viewModels))
                .dispatchUpdatesTo(this)
            recyclerView?.layoutManager?.onRestoreInstanceState(recyclerViewState)
        } else {
            notifyDataSetChanged()
        }
    }

    fun update(conversationItemViewModel: ConversationItemViewModel) {
        for (i in mConversationItemViewModels.indices) {
            val old = mConversationItemViewModels[i]
            if (old.contacts === conversationItemViewModel.contacts) {
                mConversationItemViewModels[i] = conversationItemViewModel
                notifyItemChanged(i)
                return
            }
        }
    }
}