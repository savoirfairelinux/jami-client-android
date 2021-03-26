/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.viewholders;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.jami.model.Call;
import net.jami.model.ContactEvent;
import net.jami.model.Interaction;
import net.jami.smartlist.SmartListViewModel;
import net.jami.utils.Log;

import java.util.concurrent.TimeUnit;

import cx.ring.R;
import cx.ring.databinding.ItemSmartlistBinding;
import cx.ring.databinding.ItemSmartlistHeaderBinding;
import cx.ring.utils.ResourceMapper;
import cx.ring.views.AvatarDrawable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class SmartListViewHolder extends RecyclerView.ViewHolder {
    public final ItemSmartlistBinding binding;
    public final ItemSmartlistHeaderBinding headerBinding;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public SmartListViewHolder(@NonNull ItemSmartlistBinding b, @NonNull CompositeDisposable parentDisposable) {
        super(b.getRoot());
        binding = b;
        headerBinding = null;
        parentDisposable.add(compositeDisposable);
    }

    public SmartListViewHolder(@NonNull ItemSmartlistHeaderBinding b, @NonNull CompositeDisposable parentDisposable) {
        super(b.getRoot());
        binding = null;
        headerBinding = b;
        parentDisposable.add(compositeDisposable);
    }

    public void bind(final SmartListListeners clickListener, final SmartListViewModel smartListViewModel) {
        //Log.w("SmartListViewHolder", "bind " + smartListViewModel.getContact() + " " +smartListViewModel.showPresence());
        compositeDisposable.clear();

        if (binding != null) {
            compositeDisposable.add(Observable.create(e -> itemView.setOnClickListener(e::onNext))
                    .throttleFirst(1000, TimeUnit.MILLISECONDS)
                    .subscribe(v -> clickListener.onItemClick(smartListViewModel)));
            compositeDisposable.add(smartListViewModel.getSelected().subscribe(selected -> {
                Log.w("SmartListViewHolder", "selected " + selected);
                binding.itemLayout.setActivated(selected);
            }));
            itemView.setOnLongClickListener(v -> {
                clickListener.onItemLongClick(smartListViewModel);
                return true;
            });

            binding.convParticipant.setText(smartListViewModel.getContactName());

            long lastInteraction = smartListViewModel.getLastInteractionTime();
            String lastInteractionStr = lastInteraction == 0 ?
                    "" : DateUtils.getRelativeTimeSpanString(lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL).toString();

            binding.convLastTime.setText(lastInteractionStr);
            if (smartListViewModel.hasOngoingCall()) {
                binding.convLastItem.setText(itemView.getContext().getString(R.string.ongoing_call));
            } else if (smartListViewModel.getLastEvent() != null) {
                binding.convLastItem.setText(getLastEventSummary(smartListViewModel.getLastEvent(), itemView.getContext()));
            } else {
                binding.convLastItem.setVisibility(View.GONE);
            }

            if (smartListViewModel.hasUnreadTextMessage()) {
                binding.convParticipant.setTypeface(null, Typeface.BOLD);
                binding.convLastTime.setTypeface(null, Typeface.BOLD);
                binding.convLastItem.setTypeface(null, Typeface.BOLD);
            } else {
                binding.convParticipant.setTypeface(null, Typeface.NORMAL);
                binding.convLastTime.setTypeface(null, Typeface.NORMAL);
                binding.convLastItem.setTypeface(null, Typeface.NORMAL);
            }

            binding.photo.setImageDrawable(new AvatarDrawable.Builder()
                    .withViewModel(smartListViewModel)
                    .withCircleCrop(true)
                    .build(binding.photo.getContext()));

        } else if (headerBinding != null) {
            headerBinding.headerTitle.setText(smartListViewModel.getHeaderTitle() == SmartListViewModel.Title.Conversations
                    ? R.string.navigation_item_conversation : R.string.search_results_public_directory);
        }
    }

    public void unbind() {
        compositeDisposable.clear();
    }

    private String getLastEventSummary(Interaction e, Context context) {
        if (e.getType() == (Interaction.InteractionType.TEXT)) {
            if (e.isIncoming()) {
                return e.getBody();
            } else {
                return context.getText(R.string.you_txt_prefix) + " " + e.getBody();
            }
        } else if (e.getType() == (Interaction.InteractionType.CALL)) {
            Call call = (Call) e;
            if (call.isMissed())
                return call.isIncoming() ?
                        context.getString(R.string.notif_missed_incoming_call) :
                        context.getString(R.string.notif_missed_outgoing_call);
            else
                return call.isIncoming() ?
                        String.format(context.getString(R.string.hist_in_call), call.getDurationString()) :
                        String.format(context.getString(R.string.hist_out_call), call.getDurationString());
        } else if (e.getType() == (Interaction.InteractionType.CONTACT)) {
            ContactEvent contactEvent = (ContactEvent) e;
            if (contactEvent.event == ContactEvent.Event.ADDED) {
                return context.getString(R.string.hist_contact_added);
            } else if (contactEvent.event == ContactEvent.Event.INCOMING_REQUEST) {
                return context.getString(R.string.hist_invitation_received);
            }
        } else if (e.getType() == (Interaction.InteractionType.DATA_TRANSFER)) {
            if (e.getStatus() == Interaction.InteractionStatus.TRANSFER_FINISHED) {
                if (!e.isIncoming()) {
                    return context.getString(R.string.hist_file_sent);
                } else {
                    return context.getString(R.string.hist_file_received);
                }
            }
            return ResourceMapper.getReadableFileTransferStatus(context, e.getStatus());
        }
        return null;
    }

    public interface SmartListListeners {
        void onItemClick(SmartListViewModel smartListViewModel);

        void onItemLongClick(SmartListViewModel smartListViewModel);
    }

}
