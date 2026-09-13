/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemChannelMemberBinding
import cx.ring.views.AvatarDrawable
import net.jami.smartlist.ConversationItemViewModel

class ChannelMemberAdapter(
    private val onClick: (ConversationItemViewModel) -> Unit,
) : RecyclerView.Adapter<ChannelMemberAdapter.ViewHolder>() {
    private var members = emptyList<ConversationItemViewModel>()

    fun submitList(items: List<ConversationItemViewModel>) {
        members = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemChannelMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(members[position])

    override fun getItemCount() = members.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    inner class ViewHolder(private val binding: ItemChannelMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(member: ConversationItemViewModel) {
            binding.memberName.text = member.title
            binding.memberAddress.text = member.getContact()?.displayUri ?: member.uri.rawUriString
            binding.memberPhoto.setAvatar(
                AvatarDrawable.Builder()
                    .withViewModel(member)
                    .withCircleCrop(true)
                    .build(binding.memberPhoto.context)
            )
            binding.root.setOnClickListener { onClick(member) }
        }

        fun unbind() {
            binding.memberPhoto.setAvatar(null)
        }
    }
}
