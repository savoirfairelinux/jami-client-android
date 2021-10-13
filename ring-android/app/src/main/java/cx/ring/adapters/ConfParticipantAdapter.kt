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

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.databinding.ItemConferenceAddBinding
import cx.ring.databinding.ItemConferenceParticipantBinding
import cx.ring.fragments.CallFragment
import cx.ring.views.AvatarDrawable
import cx.ring.views.ParticipantView
import net.jami.model.Conference.ParticipantInfo
import net.jami.utils.Log
import java.util.*

class ConfParticipantAdapter(private var calls: List<ParticipantInfo>, private val onSelectedCallback: ConfParticipantSelected) :
    RecyclerView.Adapter<ParticipantView>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantView {
        if (viewType == 0){
            return ParticipantView(addBinding = ItemConferenceAddBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        return ParticipantView(participantBinding = ItemConferenceParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        //Log.w("ConfParticipantAdapter", "DEBUG onBindViewHolder ---------------- >> position: $position" )
        return when(position) {
            0 -> 0
            else -> 1
        }
    }

    override fun onBindViewHolder(holder: ParticipantView, position: Int) {
       // Log.w("ConfParticipantAdapter", "DEBUG onBindViewHolder ---------------- >> position: $position" )
        if (position == 0) {
            val addBinding = holder.addBinding!!
            addBinding.addParticipantRow.setOnClickListener {
                onSelectedCallback.onAddParticipant()
                Log.w("ConfParticipantAdapter", "DEBUG  ---------- > addParticipantRow clicked; participantView info: ${holder} ")
            }
            return
        }

        val info = calls[position - 1]
        val contact = info.contact
        //Log.w("ConfParticipantAdapter", "DEBUG onBindViewHolder ---------------- >> calls.size: ${calls.size}, calls: $calls, calls[${position}]: ${info.contact.displayName} " )
        //Log.w("ConfParticipantAdapter", "DEBUG onBindViewHolder ---------------- >> calls: $calls, calls[${position}]: ${info.contact.displayName} " )

        val context = holder.itemView.context
        val call = info.call
        val participantBinding = holder.participantBinding!!
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

        holder.disposable?.dispose()
        participantBinding.photo.setImageDrawable(AvatarDrawable.Builder()
            .withContact(contact)
            .withCircleCrop(true)
            .withPresence(false)
            .build(context))

        //Log.w("ConfParticipantAdapter", "DEBUG mute status ---------------- >>  participant name: ${info.contact.displayName}, audioMuted: ${info.audioModeratorMuted}" )
        participantBinding.muteParticipant.let {
            it.setImageResource(if (info.audioModeratorMuted || info.audioLocalMuted) R.drawable.baseline_mic_off_24 else R.drawable.baseline_mic_24)
        }

        participantBinding.muteParticipant.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.Mute)
            participantBinding.muteParticipant.let {
                it.setImageResource(if (info.audioModeratorMuted || info.audioLocalMuted) R.drawable.baseline_mic_off_24 else R.drawable.baseline_mic_24)
            }
            Log.w("ConfParticipantAdapter", "DEBUG mute onclick 2 ---------------- >>  participant name: ${info.contact.displayName}, isAudioMuted: ${info.audioModeratorMuted}" )
        }

        participantBinding.extendParticipant.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.Extend)

            Log.w("ConfParticipantAdapter", "DEBUG  ---------- > extendParticipant clicked")
        }

        participantBinding.kickParticipant.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.Hangup)

            Log.w("ConfParticipantAdapter", "DEBUG  ---------- > kickbutton clicked")
        }

        holder.itemView.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.ShowDetails)
        }
    }

    override fun getItemId(position: Int): Long {
        //Log.w("ConfParticipantAdapter", "DEBUG getItemId ---------------- >> position:$position " )
        if (position == 0) {
            return Long.MAX_VALUE;
        }
        val info = calls[position - 1]
        return Objects.hash(info.contact.uri, info.call?.daemonIdString).toLong()
    }

    override fun getItemCount(): Int {
        return calls.size + 1
    }

    fun updateFromCalls(contacts: List<ParticipantInfo>) {
        val oldCalls = calls
        Log.w("ConfParticipantAdapter", "DEBUG updateFromCalls ---------------- >> oldCalls: $oldCalls, contacts:$contacts" )

        calls = contacts
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldCalls.size + 1
            }

            override fun getNewListSize(): Int {
                return contacts.size + 1
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (oldItemPosition == 0 && newItemPosition == 0)
                    return true
                if (oldItemPosition == 0 || newItemPosition == 0)
                    return false
                //Log.w("ConfParticipantAdapter", "DEBUG areItemsTheSame ---------------- >> oldCalls[oldItemPosition - 1].contact:${oldCalls[oldItemPosition - 1].contact} =?= contacts[newItemPosition - 1].contact: ${contacts[newItemPosition - 1].contact} " )
                return oldCalls[oldItemPosition -1].contact === contacts[newItemPosition -1].contact
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return false
            }
        }).dispatchUpdatesTo(this)
    }

    enum class ParticipantAction {
        ShowDetails, Mute, Extend, Hangup
    }

    interface ConfParticipantSelected {
        fun onAddParticipant();
        fun onParticipantSelected(contact: ParticipantInfo, action: ParticipantAction)
    }
}