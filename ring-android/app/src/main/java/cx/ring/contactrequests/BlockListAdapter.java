/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;

import cx.ring.R;
import net.jami.model.Contact;

public class BlockListAdapter extends RecyclerView.Adapter<BlockListViewHolder> {

    private final BlockListViewHolder.BlockListListeners mListener;
    private final ArrayList<Contact> mBlacklisted;

    public BlockListAdapter(Collection<Contact> viewModels, BlockListViewHolder.BlockListListeners listener) {
        mBlacklisted = new ArrayList<>(viewModels);
        mListener = listener;
    }

    public void replaceAll(Collection<Contact> viewModels) {
        mBlacklisted.clear();
        mBlacklisted.addAll(viewModels);
        notifyDataSetChanged();
    }

    @Override
    public BlockListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View holderView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact_blacklist, parent, false);

        return new BlockListViewHolder(holderView);
    }

    @Override
    public void onBindViewHolder(BlockListViewHolder holder, int position) {
        final Contact contact = mBlacklisted.get(position);
        holder.bind(mListener, contact);
    }

    @Override
    public int getItemCount() {
        return mBlacklisted.size();
    }
}
