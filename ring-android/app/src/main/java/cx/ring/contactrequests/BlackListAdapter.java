/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.contactrequests;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.model.CallContact;

public class BlackListAdapter extends RecyclerView.Adapter<BlackListViewHolder> {

    private final BlackListViewHolder.BlackListListeners mListener;
    private final ArrayList<CallContact> mBlacklisted;

    public BlackListAdapter(List<CallContact> viewModels, BlackListViewHolder.BlackListListeners listener) {
        mBlacklisted = new ArrayList<>(viewModels);
        mListener = listener;
    }

    public void replaceAll(List<CallContact> viewModels) {
        mBlacklisted.clear();
        mBlacklisted.addAll(viewModels);
        notifyDataSetChanged();
    }

    @Override
    public BlackListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View holderView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact_blacklist, parent, false);

        return new BlackListViewHolder(holderView);
    }

    @Override
    public void onBindViewHolder(BlackListViewHolder holder, int position) {
        final CallContact contact = mBlacklisted.get(position);
        holder.bind(mListener, contact);
    }

    @Override
    public int getItemCount() {
        return mBlacklisted.size();
    }
}
