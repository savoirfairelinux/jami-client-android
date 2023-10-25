/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.databinding.ItemConferenceParticipantBinding
import cx.ring.fragments.CallFragment
import cx.ring.views.AvatarDrawable
import cx.ring.views.ParticipantView
import net.jami.model.Conference.ParticipantInfo
import java.util.*

class ConfParticipantAdapter(private var calls: List<ParticipantInfo>, private val onSelectedCallback: ConfParticipantSelected) :
    RecyclerView.Adapter<ParticipantView>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantView {
        return ParticipantView(participantBinding = ItemConferenceParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ParticipantView, position: Int) {
        val info = calls[position]
        val contact = info.contact

        val context = holder.itemView.context
        val call = info.call
        val participantBinding = holder.participantBinding
        val displayName = TextUtils.ellipsize(contact.displayName,
            participantBinding.displayName.paint,
            participantBinding.displayName.maxWidth.toFloat(),
            TextUtils.TruncateAt.MIDDLE)
        if (call != null && info.pending) {
            participantBinding.displayName.text = String.format("%s\n%s", displayName, context.getText(CallFragment.callStateToHumanState(call.callStatus)))
            participantBinding.photo.alpha = .5f
        } else {
            participantBinding.displayName.text = displayName
            participantBinding.photo.alpha = 1f
        }

        val isConference = calls.filterNot { it.pending }.size > 1
        participantBinding.muteParticipant.isVisible = isConference
        participantBinding.extendParticipant.isVisible = isConference
        participantBinding.kickParticipant.isVisible = isConference

        holder.disposable?.dispose()
        participantBinding.photo.setImageDrawable(AvatarDrawable.Builder()
            .withContact(contact)
            .withCircleCrop(true)
            .withPresence(false)
            .build(context))

        participantBinding.muteParticipant.let {
            it.setImageResource(if (info.audioModeratorMuted || info.audioLocalMuted) R.drawable.baseline_mic_off_24 else R.drawable.baseline_mic_on_24)
        }

        participantBinding.muteParticipant.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.Mute)
            participantBinding.muteParticipant.let {
                it.setImageResource(if (info.audioModeratorMuted || info.audioLocalMuted) R.drawable.baseline_mic_off_24 else R.drawable.baseline_mic_on_24)
            }
        }

        participantBinding.extendParticipant.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.Extend)
        }

        participantBinding.kickParticipant.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.Hangup)
        }

        holder.itemView.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.ShowDetails)
        }
    }

    override fun getItemId(position: Int): Long = calls[position].hashCode().toLong()

    override fun getItemCount(): Int = calls.size

    fun updateFromCalls(contacts: List<ParticipantInfo>) {
        val oldCalls = calls
        calls = contacts
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldCalls.size

            override fun getNewListSize(): Int = contacts.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldCalls[oldItemPosition].hashCode() == contacts[newItemPosition].hashCode()

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                false
        }).dispatchUpdatesTo(this)
    }

    enum class ParticipantAction {
        ShowDetails, Mute, Extend, Hangup
    }

    interface ConfParticipantSelected {
        fun onParticipantSelected(contact: ParticipantInfo, action: ParticipantAction)
    }

    companion object {
        val TAG = ConfParticipantAdapter::class.simpleName!!
    }
}