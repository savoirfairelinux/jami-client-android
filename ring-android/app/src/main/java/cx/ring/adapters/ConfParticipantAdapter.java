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
package cx.ring.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cx.ring.fragments.CallFragment;
import cx.ring.views.AvatarFactory;
import cx.ring.databinding.ItemConferenceParticipantBinding;

import net.jami.model.Conference;
import net.jami.model.Contact;
import net.jami.model.Call;
import cx.ring.views.ParticipantView;

public class ConfParticipantAdapter extends RecyclerView.Adapter<ParticipantView> {
    protected final ConfParticipantAdapter.ConfParticipantSelected onSelectedCallback;
    private List<Conference.ParticipantInfo> calls = null;

    public ConfParticipantAdapter(@NonNull ConfParticipantSelected cb) {
        onSelectedCallback = cb;
    }

    @NonNull
    @Override
    public ParticipantView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ParticipantView(ItemConferenceParticipantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantView holder, int position) {
        final Conference.ParticipantInfo info = calls.get(position);
        final Contact contact = info.contact;

        final Context context = holder.itemView.getContext();
        if (info.call != null && info.call.getCallStatus() != Call.CallStatus.CURRENT)  {
            holder.binding.displayName.setText(String.format("%s\n%s", contact.getDisplayName(), context.getText(CallFragment.callStateToHumanState(info.call.getCallStatus()))));
            holder.binding.photo.setAlpha(.5f);
        } else {
            holder.binding.displayName.setText(contact.getDisplayName());
            holder.binding.photo.setAlpha(1f);
        }

        if (holder.disposable != null)
            holder.disposable.dispose();
        holder.disposable = AvatarFactory.getAvatar(context, contact)
                .subscribe(holder.binding.photo::setImageDrawable);
        holder.itemView.setOnClickListener(view -> onSelectedCallback.onParticipantSelected(view, info));
    }

    @Override
    public int getItemCount() {
        return calls == null ? 0 : calls.size();
    }

    public void updateFromCalls(@NonNull final List<Conference.ParticipantInfo> contacts) {
        final List<Conference.ParticipantInfo> oldCalls = calls;
        calls = contacts;
        if (oldCalls != null) {
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldCalls.size();
                }

                @Override
                public int getNewListSize() {
                    return contacts.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldCalls.get(oldItemPosition) == contacts.get(newItemPosition);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return false;
                }
            }).dispatchUpdatesTo(this);
        } else {
            notifyDataSetChanged();
        }
    }

    public interface ConfParticipantSelected {
        void onParticipantSelected(View view, Conference.ParticipantInfo contact);
    }
}
