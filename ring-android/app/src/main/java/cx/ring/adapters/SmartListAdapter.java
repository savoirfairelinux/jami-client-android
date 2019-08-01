/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.model.Interaction;
import cx.ring.model.Interaction.InteractionType;
import cx.ring.model.Interaction.InteractionStatus;
import cx.ring.model.SipCall;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.viewholders.SmartListViewHolder;
import cx.ring.views.AvatarDrawable;

public class SmartListAdapter extends RecyclerView.Adapter<SmartListViewHolder> {

    private List<SmartListViewModel> mSmartListViewModels = new ArrayList<>();
    private SmartListViewHolder.SmartListListeners listener;

    public SmartListAdapter(List<SmartListViewModel> smartListViewModels, SmartListViewHolder.SmartListListeners listener) {
        this.listener = listener;
        if (smartListViewModels != null)
            mSmartListViewModels.addAll(smartListViewModels);
    }

    @NonNull
    @Override
    public SmartListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_smartlist, parent, false);
        return new SmartListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SmartListViewHolder holder, int position) {
        final SmartListViewModel smartListViewModel = mSmartListViewModels.get(position);
        CallContact contact = smartListViewModel.getContact();
        //Log.w("SmartListAdapter", "onBindViewHolder " + position + " " + holder.hasUnreadTextMessage());

        holder.convParticipants.setText(smartListViewModel.getContactName());

        long lastInteraction = smartListViewModel.getLastInteractionTime();
        String lastInteractionStr = lastInteraction == 0 ?
                "" : DateUtils.getRelativeTimeSpanString(lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL).toString();

        holder.convTime.setText(lastInteractionStr);
        if (smartListViewModel.hasOngoingCall()) {
            holder.convStatus.setText(holder.itemView.getContext().getString(R.string.ongoing_call));
        } else if (smartListViewModel.getLastEvent() != null) {
            holder.convStatus.setText(getLastEventSummary(smartListViewModel.getLastEvent(), holder.itemView.getContext()));
        } else {
            holder.convStatus.setVisibility(View.GONE);
        }

        if (smartListViewModel.hasUnreadTextMessage()) {
            holder.convParticipants.setTypeface(null, Typeface.BOLD);
            holder.convTime.setTypeface(null, Typeface.BOLD);
            holder.convStatus.setTypeface(null, Typeface.BOLD);
        } else {
            holder.convParticipants.setTypeface(null, Typeface.NORMAL);
            holder.convTime.setTypeface(null, Typeface.NORMAL);
            holder.convStatus.setTypeface(null, Typeface.NORMAL);
        }

        holder.photo.setImageDrawable(new AvatarDrawable(holder.photo.getContext(), contact));
        //AvatarFactory.loadGlideAvatar(holder.photo, contact);
        holder.online.setVisibility(smartListViewModel.isOnline() ? View.VISIBLE : View.GONE);
        holder.bind(listener, smartListViewModel);
    }

    @Override
    public int getItemCount() {
        return mSmartListViewModels.size();
    }

    public void update(List<SmartListViewModel> viewModels) {
        final List<SmartListViewModel> old = mSmartListViewModels;
        mSmartListViewModels = viewModels == null ? new ArrayList<>() : viewModels;
        if (old != null && viewModels != null) {
            DiffUtil.calculateDiff(new SmartListDiffUtil(old, viewModels)).dispatchUpdatesTo(this);
        } else {
            notifyDataSetChanged();
        }
    }

    public void update(SmartListViewModel smartListViewModel) {
        for (int i = 0; i < mSmartListViewModels.size(); i++) {
            SmartListViewModel old = mSmartListViewModels.get(i);
            if (old.getContact() == smartListViewModel.getContact()) {
                mSmartListViewModels.set(i, smartListViewModel);
                notifyItemChanged(i);
            }
        }
    }


    private String getLastEventSummary(Interaction e, Context context) {
        if (e.getType().equals(InteractionType.TEXT)) {
            if (e.isIncoming()) {
                return e.getBody();
            } else {
                return context.getText(R.string.you_txt_prefix) + " " + e.getBody();
            }
        } else if (e.getType().equals(InteractionType.CALL)) {
            SipCall call = new SipCall(e);
            if (call.isMissed())
                return call.isIncoming() ?
                        context.getString(R.string.notif_missed_incoming_call) :
                        context.getString(R.string.notif_missed_outgoing_call);
            else
                return call.isIncoming() ?
                        String.format(context.getString(R.string.hist_in_call), call.getDurationString()) :
                        String.format(context.getString(R.string.hist_out_call), call.getDurationString());
        } else if (e.getType().equals(InteractionType.CONTACT)) {
            if ((e.getAuthor() == null && e.getStatus() == InteractionStatus.INVALID) || e.getStatus().equals(InteractionStatus.SUCCEEDED)) {
                return context.getString(R.string.hist_contact_added);
            } else if (e.getAuthor() != null && e.getStatus().equals(InteractionStatus.UNKNOWN)) {
                return context.getString(R.string.hist_invitation_received);
            }
        } else if (e.getType().equals(InteractionType.DATA_TRANSFER)) {
            if (!e.isIncoming()) {
                return context.getString(R.string.hist_file_sent);
            } else {
                return context.getString(R.string.hist_file_received);
            }
        }
        return null;
    }
}