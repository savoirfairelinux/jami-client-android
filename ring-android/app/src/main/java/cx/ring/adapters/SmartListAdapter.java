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

import cx.ring.databinding.ItemSmartlistBinding;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.viewholders.SmartListViewHolder;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SmartListAdapter extends RecyclerView.Adapter<SmartListViewHolder> {

    private List<SmartListViewModel> mSmartListViewModels = new ArrayList<>();
    private SmartListViewHolder.SmartListListeners listener;
    private SmartListUpdateCallback updateCallback;
    private RecyclerView recyclerView;

    public SmartListAdapter(List<SmartListViewModel> smartListViewModels, SmartListViewHolder.SmartListListeners listener) {
        this.listener = listener;
        if (smartListViewModels != null)
            mSmartListViewModels.addAll(smartListViewModels);
    }

    @NonNull
    @Override
    public SmartListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemSmartlistBinding itemBinding = ItemSmartlistBinding.inflate(layoutInflater, parent, false);
        return new SmartListViewHolder(itemBinding);
    }

    @Override
    public void onViewRecycled(@NonNull SmartListViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }

    @Override
    public void onBindViewHolder(@NonNull SmartListViewHolder holder, int position) {
        final SmartListViewModel smartListViewModel = mSmartListViewModels.get(position);
        holder.bind(listener, smartListViewModel);
    }

    @Override
    public int getItemCount() {
        return mSmartListViewModels.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        updateCallback = new SmartListUpdateCallback(this);
    }

    class SmartListUpdateCallback implements ListUpdateCallback {
        SmartListAdapter adapter;
        SmartListUpdateCallback(SmartListAdapter adapter) {
            this.adapter = adapter;
        }
        private LinearLayoutManager getLayoutManager() {
            return (LinearLayoutManager)adapter.recyclerView.getLayoutManager();
        }
        @Override
        public void onChanged(int position, int count, Object payload) {
            adapter.notifyItemRangeChanged(position, count, payload);
        }
        @Override
        public void onInserted(int position, int count) {
            adapter.notifyItemRangeInserted(position, count);
            getLayoutManager().scrollToPositionWithOffset(position, 0);
        }
        @Override
        public void onMoved(int fromPosition, int toPosition) {
            LinearLayoutManager llm = getLayoutManager();
            int visible = llm.findFirstVisibleItemPosition();
            int offset = llm.getChildAt(visible).getTop();
            adapter.notifyItemMoved(fromPosition, toPosition);
            llm.scrollToPositionWithOffset(visible, offset);
        }
        @Override
        public void onRemoved(int position, int count) {
            adapter.notifyItemRangeRemoved(position, count);
        }
    }

    public void update(List<SmartListViewModel> viewModels) {
        final List<SmartListViewModel> old = mSmartListViewModels;
        mSmartListViewModels = viewModels == null ? new ArrayList<>() : viewModels;
        if (old != null && viewModels != null) {
            DiffUtil.calculateDiff(new SmartListDiffUtil(old, viewModels))
                    .dispatchUpdatesTo(updateCallback);
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
}