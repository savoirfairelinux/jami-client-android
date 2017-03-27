/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.contactrequests;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import cx.ring.R;
import cx.ring.model.CallContact;

public class BlackListAdapter extends RecyclerView.Adapter<BlackListView> {
    private List<CallContact> mContacts;
    private Context mContext;
    private BlackListPresenter mPresenter;

    public BlackListAdapter(Context context, List<CallContact> contacts, BlackListPresenter presenter) {
        mContacts = contacts;
        mContext = context;
        mPresenter = presenter;
    }

    public void replaceAll(List<CallContact> contacts) {
        mContacts = contacts;
        notifyDataSetChanged();
    }

    @Override
    public BlackListView onCreateViewHolder(ViewGroup parent, int viewType) {
        BlackListView viewHolder;
        View holderView;

        holderView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact_blacklist, parent, false);
        viewHolder = new BlackListView(holderView, mPresenter);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(BlackListView holder, int position) {
        CallContact contact = mContacts.get(position);
        //default photo
        Drawable photo = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_contact_picture, null);
        holder.mPhoto.setImageDrawable(photo);

        holder.mDisplayname.setText(contact.getDisplayName());

        holder.setContactId(contact.getDisplayName());
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }
}
