/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import cx.ring.databinding.ItemSmartlistHeaderBinding;
import net.jami.smartlist.SmartListViewModel;
import cx.ring.viewholders.SmartListViewHolder;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SmartListAdapter extends RecyclerView.Adapter<SmartListViewHolder> {

    private List<SmartListViewModel> mSmartListViewModels = new ArrayList<>();
    private final SmartListViewHolder.SmartListListeners listener;
    private final CompositeDisposable mDisposable;
    private RecyclerView recyclerView;

    public SmartListAdapter(List<SmartListViewModel> smartListViewModels, SmartListViewHolder.SmartListListeners listener, CompositeDisposable disposable) {
        this.listener = listener;
        mDisposable = disposable;
        if (smartListViewModels != null)
            mSmartListViewModels.addAll(smartListViewModels);
    }

    @NonNull
    @Override
    public SmartListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            ItemSmartlistBinding itemBinding = ItemSmartlistBinding.inflate(layoutInflater, parent, false);
            return new SmartListViewHolder(itemBinding, mDisposable);
        } else {
            ItemSmartlistHeaderBinding itemBinding = ItemSmartlistHeaderBinding.inflate(layoutInflater, parent, false);
            return new SmartListViewHolder(itemBinding, mDisposable);
        }
    }

    @Override
    public int getItemViewType(int position) {
        final SmartListViewModel smartListViewModel = mSmartListViewModels.get(position);
        return smartListViewModel.getHeaderTitle() == SmartListViewModel.Title.None ? 0 : 1;
    }

    @Override
    public void onViewRecycled(@NonNull SmartListViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }

    @Override
    public void onBindViewHolder(@NonNull SmartListViewHolder holder, int position) {
        holder.bind(listener, mSmartListViewModels.get(position));
    }

    @Override
    public int getItemCount() {
        return mSmartListViewModels.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    public void update(List<SmartListViewModel> viewModels) {
        //Log.w("SmartListAdapter", "update " + (viewModels == null ? null : viewModels.size()));
        final List<SmartListViewModel> old = mSmartListViewModels;
        mSmartListViewModels = viewModels == null ? new ArrayList<>() : viewModels;
        if (old != null && viewModels != null) {
            Parcelable recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            DiffUtil.calculateDiff(new SmartListDiffUtil(old, viewModels))
                    .dispatchUpdatesTo(this);
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        } else {
            notifyDataSetChanged();
        }
    }

    public void update(SmartListViewModel smartListViewModel) {
        for (int i = 0; i < mSmartListViewModels.size(); i++) {
            SmartListViewModel old = mSmartListViewModels.get(i);
            if (old.getContacts() == smartListViewModel.getContacts()) {
                mSmartListViewModels.set(i, smartListViewModel);
                notifyItemChanged(i);
                return;
            }
        }
    }
}