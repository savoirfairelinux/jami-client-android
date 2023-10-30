/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemSmartlistBinding
import cx.ring.databinding.ItemSmartlistHeaderBinding
import cx.ring.viewholders.SmartListViewHolder
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Conversation
import net.jami.services.ConversationFacade

class SmartListAdapter(
    conversations: ConversationFacade.ConversationList?,
    private val listener: SmartListListeners,
    private val conversationFacade: ConversationFacade,
    private val disposable: CompositeDisposable
) : RecyclerView.Adapter<SmartListViewHolder>() {
    private var recyclerView: RecyclerView? = null
    private var itemCount: Int = 0
    private var searchHeaderIndex: Int = -1
    private var convHeaderIndex: Int = -1
    private var conversations = setItems(conversations ?: ConversationFacade.ConversationList())

    private fun setItems(list: ConversationFacade.ConversationList): ConversationFacade.ConversationList {
        itemCount = list.getCombinedSize()
        if (list.searchResult.result.isNotEmpty()) {
            searchHeaderIndex = 0
            convHeaderIndex = if (list.conversations.isEmpty()) -1 else list.searchResult.result.size + 1
        } else {
            searchHeaderIndex = -1
            convHeaderIndex = -1
        }
        return list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) SmartListViewHolder(ItemSmartlistBinding.inflate(layoutInflater, parent, false), disposable)
        else SmartListViewHolder(ItemSmartlistHeaderBinding.inflate(layoutInflater, parent, false), disposable)
    }

    override fun getItemViewType(position: Int): Int = when(position) {
        searchHeaderIndex, convHeaderIndex -> 1
        else -> 0
    }

    override fun onViewRecycled(holder: SmartListViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun onBindViewHolder(holder: SmartListViewHolder, position: Int) {
        conversations[position]?.let { conversation ->
            return holder.bind(conversationFacade, listener, conversation)
        }
        conversations.getHeader(position).let { title ->
            return holder.bindHeader(title)
        }
    }

    override fun getItemCount(): Int = itemCount

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    fun update(viewModels: List<Conversation>) {
        update(ConversationFacade.ConversationList(ArrayList(viewModels)))
    }

    fun update(viewModels: ConversationFacade.ConversationList) {
        val old: ConversationFacade.ConversationList = conversations
        conversations = setItems(viewModels)
        if (!viewModels.isEmpty()) {
            val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
            DiffUtil.calculateDiff(SmartListDiffUtil(old, viewModels))
                .dispatchUpdatesTo(this)
            recyclerView?.layoutManager?.onRestoreInstanceState(recyclerViewState)
        } else {
            notifyDataSetChanged()
        }
    }
}