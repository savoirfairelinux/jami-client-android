/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
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
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemConferenceParticipantBinding
import cx.ring.fragments.CallFragment
import cx.ring.views.AvatarDrawable
import cx.ring.views.ParticipantView
import net.jami.model.Call
import net.jami.model.Conference.ParticipantInfo

class ConfParticipantAdapter(private val onSelectedCallback: ConfParticipantSelected) :
    RecyclerView.Adapter<ParticipantView>() {
    private var calls: List<ParticipantInfo>? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantView {
        return ParticipantView(
            ItemConferenceParticipantBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ParticipantView, position: Int) {
        val info = calls!![position]
        val contact = info.contact
        val context = holder.itemView.context
        if (info.call != null && info.call.callStatus != Call.CallStatus.CURRENT) {
            holder.binding.displayName.text = String.format(
                "%s\n%s",
                contact.displayName,
                context.getText(CallFragment.callStateToHumanState(info.call.callStatus))
            )
            holder.binding.photo.alpha = .5f
        } else {
            holder.binding.displayName.text = contact.displayName
            holder.binding.photo.alpha = 1f
        }
        if (holder.disposable != null) holder.disposable.dispose()
        holder.binding.photo.setImageDrawable(
            AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
                .withPresence(false)
                .build(context)
        )
        /*;
        holder.disposable = AvatarFactory.getAvatar(context, contact)
                .subscribe(holder.binding.photo::setImageDrawable);*/holder.itemView.setOnClickListener { view: View? ->
            onSelectedCallback.onParticipantSelected(
                view,
                info
            )
        }
    }

    override fun getItemCount(): Int {
        return if (calls == null) 0 else calls!!.size
    }

    fun updateFromCalls(contacts: List<ParticipantInfo>) {
        val oldCalls = calls
        calls = contacts
        if (oldCalls != null) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return oldCalls.size
                }

                override fun getNewListSize(): Int {
                    return contacts.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldCalls[oldItemPosition] === contacts[newItemPosition]
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return false
                }
            }).dispatchUpdatesTo(this)
        } else {
            notifyDataSetChanged()
        }
    }

    interface ConfParticipantSelected {
        fun onParticipantSelected(view: View?, contact: ParticipantInfo?)
    }
}