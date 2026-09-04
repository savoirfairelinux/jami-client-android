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

class ConfParticipantAdapter(
    private var calls: List<ParticipantInfo>,
    private val onSelectedCallback: ConfParticipantSelected
) :
    RecyclerView.Adapter<ParticipantView>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantView {
        return ParticipantView(
            participantBinding = ItemConferenceParticipantBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ParticipantView, position: Int) {
        val info = calls[position]
        val contact = info.contact

        val context = holder.itemView.context
        val call = info.call
        val participantBinding = holder.participantBinding
        val displayName = TextUtils.ellipsize(
            contact.displayName,
            participantBinding.displayName.paint,
            participantBinding.displayName.maxWidth.toFloat(),
            TextUtils.TruncateAt.MIDDLE
        )
        if (call != null && info.pending) {
            participantBinding.displayName.text = String.format(
                "%s\n%s",
                displayName,
                context.getText(CallFragment.callStateToHumanState(call.callStatus))
            )
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
        participantBinding.photo.setImageDrawable(
            AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
                .withPresence(false)
                .build(context)
        )

        participantBinding.muteParticipant.setImageResource(
            if (info.audioModeratorMuted || info.audioLocalMuted)
                R.drawable.baseline_mic_off_24
            else
                R.drawable.baseline_mic_on_24
        )

        participantBinding.muteParticipant.setOnClickListener {
            onSelectedCallback.onParticipantSelected(info, ParticipantAction.Mute)
            participantBinding.muteParticipant.setImageResource(
                if (info.audioModeratorMuted || info.audioLocalMuted)
                    R.drawable.baseline_mic_off_24
                else
                    R.drawable.baseline_mic_on_24
            )
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

    override fun getItemCount(): Int = calls.size

    fun updateFromCalls(contacts: List<ParticipantInfo>) {
        val oldCalls = calls
        calls = contacts
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldCalls.size

            override fun getNewListSize(): Int = contacts.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldCalls[oldItemPosition].hasSameIdentity(contacts[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                false
        }).dispatchUpdatesTo(this)
    }

    /**
     * A participant is identified by the fields that can tell two rows of a
     * single list apart, because no immutable unique identifier is available.
     *
     * `sinkId` is the daemon's stream ID and is required: the conference info
     * holds one entry per media stream, so a participant sharing a screen
     * alongside their camera produces two rows with the same URI, device and
     * call, distinguished only by their stream.
     *
     * `pending` is required for the same reason: the displayed list
     * concatenates conference participants with the calls still being placed,
     * so the same contact may briefly appear once from each source.
     */
    private fun ParticipantInfo.hasSameIdentity(other: ParticipantInfo): Boolean =
        contact.contact.uri == other.contact.contact.uri &&
                device == other.device &&
                sinkId == other.sinkId &&
                call?.id == other.call?.id &&
                pending == other.pending

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