/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
package cx.ring.adapters;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.BitmapUtils;
import cx.ring.viewholders.SmartListViewHolder;

public class SmartListAdapter extends RecyclerView.Adapter<SmartListViewHolder> {

    private ArrayList<SmartListViewModel> mSmartListViewModels;
    private SmartListViewHolder.SmartListListeners listener;

    public SmartListAdapter(ArrayList<SmartListViewModel> smartListViewModels, SmartListViewHolder.SmartListListeners listener) {
        this.listener = listener;
        this.mSmartListViewModels = new ArrayList<>();
        this.mSmartListViewModels.addAll(smartListViewModels);
    }

    @Override
    public SmartListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_smartlist, parent, false);

        return new SmartListViewHolder(v);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(SmartListViewHolder holder, int position) {
        final SmartListViewModel smartListViewModel = mSmartListViewModels.get(position);

        holder.convParticipants.setText(smartListViewModel.getContactName());
        long lastInteraction = smartListViewModel.getLastInteractionTime();
        holder.convTime.setText(lastInteraction == 0 ? "" :
                DateUtils.getRelativeTimeSpanString(System.currentTimeMillis() - lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        if (smartListViewModel.hasOngoingCall()) {
            holder.convStatus.setText(holder.itemView.getContext().getString(R.string.ongoing_call));
        } else if (smartListViewModel.getLastInteraction() != null) {
            holder.convStatus.setText(getLastInteractionSummary(smartListViewModel.getLastEntryType(),
                    smartListViewModel.getLastInteraction(),
                    holder.itemView.getContext()));
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

        if (smartListViewModel.getPhotoUri() != null
                && !smartListViewModel.getPhotoUri().equals("")) {
            holder.photo.setImageBitmap(BitmapUtils.cropImageToCircle(BitmapUtils.stringToBytes(smartListViewModel.getPhotoUri())));
        } else {
            holder.photo.setImageResource(R.drawable.ic_contact_picture);
        }

        holder.bind(listener, smartListViewModel);
    }

    @Override
    public void onBindViewHolder(SmartListViewHolder holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemCount() {
        return mSmartListViewModels.size();
    }

    public void update(ArrayList<SmartListViewModel> smartListViewModels) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SmartListDiffUtil(this.mSmartListViewModels, smartListViewModels));

        this.mSmartListViewModels.clear();
        this.mSmartListViewModels.addAll(smartListViewModels);

        diffResult.dispatchUpdatesTo(this);
    }

    private String getLastInteractionSummary(int type, String lastInteraction, Context context) {
        switch (type) {
            case SmartListViewModel.TYPE_INCOMING_CALL:
                return String.format(context.getString(R.string.hist_in_call), lastInteraction);
            case SmartListViewModel.TYPE_OUTGOING_CALL:
                return String.format(context.getString(R.string.hist_out_call), lastInteraction);
            case SmartListViewModel.TYPE_INCOMING_MESSAGE:
                return lastInteraction;
            case SmartListViewModel.TYPE_OUTGOING_MESSAGE:
                return context.getText(R.string.you_txt_prefix) + " " + lastInteraction;
            default:
                return null;
        }
    }
}