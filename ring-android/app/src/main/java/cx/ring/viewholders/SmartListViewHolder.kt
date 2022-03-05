/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.viewholders

import android.content.Context
import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.databinding.ItemSmartlistBinding
import cx.ring.databinding.ItemSmartlistHeaderBinding
import cx.ring.utils.TextUtils
import cx.ring.views.AvatarDrawable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Call
import net.jami.model.ContactEvent
import net.jami.model.Conversation
import net.jami.model.Interaction
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel

class SmartListViewHolder : RecyclerView.ViewHolder {
    private val binding: ItemSmartlistBinding?
    private val headerBinding: ItemSmartlistHeaderBinding?
    private val compositeDisposable = CompositeDisposable()

    constructor(b: ItemSmartlistBinding, parentDisposable: CompositeDisposable) : super(b.root) {
        binding = b
        headerBinding = null
        parentDisposable.add(compositeDisposable)
    }

    constructor(b: ItemSmartlistHeaderBinding, parentDisposable: CompositeDisposable) : super(b.root) {
        binding = null
        headerBinding = b
        parentDisposable.add(compositeDisposable)
    }

    fun bindHeader(title: ConversationItemViewModel.Title) {
        headerBinding?.headerTitle?.setText(when(title) {
            ConversationItemViewModel.Title.Conversations -> R.string.navigation_item_conversation
            else -> R.string.search_results_public_directory
        })
    }

    private fun fadeIn() = AlphaAnimation(0f, 1f).apply {
        interpolator = DecelerateInterpolator()
        duration = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    }

    fun bind(conversationFacade: ConversationFacade, clickListener: SmartListListeners, conversation: Conversation) {
        //Log.w("SmartListViewHolder", "bind " + smartListViewModel.getContact() + " " +smartListViewModel.showPresence());
        compositeDisposable.clear()
        if (binding != null) {
            itemView.setOnClickListener { clickListener.onItemClick(conversation) }
            itemView.setOnLongClickListener {
                clickListener.onItemLongClick(conversation)
                true
            }

            compositeDisposable.add(conversation.currentStateSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    val fade = !binding.convLastItem.isVisible || binding.convLastItem.text.isBlank()
                    val lastEvent = state.first
                    val lastInteraction = lastEvent.timestamp
                    binding.convLastTime.text = if (lastInteraction == 0L) ""
                    else DateUtils.getRelativeTimeSpanString(lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL)
                    if (state.second) {
                        binding.convLastItem.visibility = View.VISIBLE
                        binding.convLastItem.text = itemView.context.getString(R.string.ongoing_call)
                    } else if (lastEvent != null) {
                        binding.convLastItem.visibility = View.VISIBLE
                        binding.convLastItem.text = getLastEventSummary(lastEvent, itemView.context)
                    } else {
                        binding.convLastItem.visibility = View.GONE
                    }
                    if (!lastEvent.isRead) {
                        binding.convParticipant.setTypeface(null, Typeface.BOLD)
                        binding.convLastTime.setTypeface(null, Typeface.BOLD)
                        binding.convLastItem.setTypeface(null, Typeface.BOLD)
                    } else {
                        binding.convParticipant.setTypeface(null, Typeface.NORMAL)
                        binding.convLastTime.setTypeface(null, Typeface.NORMAL)
                        binding.convLastItem.setTypeface(null, Typeface.NORMAL)
                    }
                    if (fade) {
                        binding.convInfo.startAnimation(fadeIn())
                    }
                })

            compositeDisposable.add(conversationFacade.observeConversation(conversation, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { conversationItemViewModel ->
                    binding.convParticipant.text = conversationItemViewModel.contactName
                    val fade = binding.photo.drawable !is AvatarDrawable
                    binding.photo.setImageDrawable(AvatarDrawable.Builder()
                        .withViewModel(conversationItemViewModel)
                        .withCircleCrop(true)
                        .build(binding.photo.context))
                    if (fade)
                        binding.photo.startAnimation(fadeIn())
                })

            compositeDisposable.add(conversation.getVisible().observeOn(AndroidSchedulers.mainThread()).subscribe { activated ->
                binding.itemLayout.isActivated = activated
            })
        }
    }

    fun unbind() {
        binding?.photo?.setImageDrawable(null)
        compositeDisposable.clear()
    }

    private fun getLastEventSummary(e: Interaction, context: Context): String? {
        if (e.type == Interaction.InteractionType.TEXT) {
            return if (e.isIncoming) {
                e.body
            } else {
                context.getText(R.string.you_txt_prefix).toString() + " " + e.body
            }
        } else if (e.type == Interaction.InteractionType.CALL) {
            val call = e as Call
            return if (call.isMissed) if (call.isIncoming) context.getString(R.string.notif_missed_incoming_call) else context.getString(
                R.string.notif_missed_outgoing_call
            ) else if (call.isIncoming) String.format(
                context.getString(R.string.hist_in_call),
                call.durationString
            ) else String.format(context.getString(R.string.hist_out_call), call.durationString)
        } else if (e.type == Interaction.InteractionType.CONTACT) {
            val contactEvent = e as ContactEvent
            if (contactEvent.event == ContactEvent.Event.ADDED) {
                return context.getString(R.string.hist_contact_added)
            } else if (contactEvent.event == ContactEvent.Event.INCOMING_REQUEST) {
                return context.getString(R.string.hist_invitation_received)
            }
        } else if (e.type == Interaction.InteractionType.DATA_TRANSFER) {
            return if (e.status == Interaction.InteractionStatus.TRANSFER_FINISHED) {
                if (!e.isIncoming) {
                    context.getString(R.string.hist_file_sent)
                } else {
                    context.getString(R.string.hist_file_received)
                }
            } else TextUtils.getReadableFileTransferStatus(context, e.status)
        }
        return null
    }

    interface SmartListListeners {
        fun onItemClick(item: Conversation)
        fun onItemLongClick(item: Conversation)
    }
}