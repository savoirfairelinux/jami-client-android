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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.CircleTransform;
import ezvcard.VCard;

public class ContactRequestsAdapter extends RecyclerView.Adapter<ContactRequestViewHolder> {
    static final String TAG = ContactRequestsAdapter.class.getSimpleName();

    private ArrayList<PendingContactRequestsViewModel> mContactRequestsViewModels;
    private ContactRequestViewHolder.ContactRequestListeners mListener;

    public ContactRequestsAdapter(ArrayList<PendingContactRequestsViewModel> viewModels, ContactRequestViewHolder.ContactRequestListeners listener) {
        mContactRequestsViewModels = new ArrayList<>(viewModels);
        mListener = listener;
    }

    public void replaceAll(ArrayList<PendingContactRequestsViewModel> list) {
        mContactRequestsViewModels.clear();
        mContactRequestsViewModels.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public ContactRequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View holderView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact_request, parent, false);

        return new ContactRequestViewHolder(holderView);
    }

    @Override
    public void onBindViewHolder(ContactRequestViewHolder holder, int position) {
        final PendingContactRequestsViewModel viewModel = mContactRequestsViewModels.get(position);

        VCard vcard = viewModel.getVCard();
        if (vcard != null) {
            if (!vcard.getPhotos().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(vcard.getPhotos().get(0).getData())
                        .placeholder(R.drawable.ic_contact_picture)
                        .crossFade()
                        .transform(new CircleTransform(holder.itemView.getContext()))
                        .error(R.drawable.ic_contact_picture)
                        .into(holder.mPhoto);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.ic_contact_picture)
                        .into(holder.mPhoto);
            }
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_contact_picture)
                    .into(holder.mPhoto);
        }

        String fullname = viewModel.getFullname();
        String username = viewModel.getUsername();
        if (!TextUtils.isEmpty(fullname)) {
            holder.mDisplayname.setVisibility(View.GONE);
            holder.mNamelayout.setVisibility(View.VISIBLE);
            holder.mFullname.setText(fullname);
            holder.mUsername.setText(username);
        } else {
            holder.mDisplayname.setVisibility(View.VISIBLE);
            holder.mNamelayout.setVisibility(View.GONE);
            holder.mDisplayname.setText(username);
        }

        holder.bind(mListener, viewModel);
    }

    @Override
    public int getItemCount() {
        return mContactRequestsViewModels.size();
    }
}
