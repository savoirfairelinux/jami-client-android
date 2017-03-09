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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.model.TrustRequest;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public class ContactRequestsAdapter extends RecyclerView.Adapter<ContactRequestView> {
    static final String TAG = ContactRequestsAdapter.class.getSimpleName();

    private List<TrustRequest> mTrustRequests;
    private Context mContext;
    private PendingContactRequestsPresenter mPresenter;

    public ContactRequestsAdapter(Context context, List<TrustRequest> trustRequests, PendingContactRequestsPresenter presenter) {
        mContext = context;
        mTrustRequests = trustRequests;
        mPresenter = presenter;
    }

    public void replaceAll(List<TrustRequest> trustRequests) {
        mTrustRequests = trustRequests;
        notifyDataSetChanged();
    }

    @Override
    public ContactRequestView onCreateViewHolder(ViewGroup parent, int viewType) {
        ContactRequestView viewHolder;
        View holderView;

        holderView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact_request, parent, false);
        viewHolder = new ContactRequestView(holderView, mPresenter);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ContactRequestView holder, int position) {
        TrustRequest trustRequest = mTrustRequests.get(position);

        //default photo
        Drawable photo = ResourcesCompat.getDrawable(holder.itemView.getResources(), R.drawable.ic_contact_picture, null);

        VCard vcard = trustRequest.getVCard();
        if (vcard != null) {
            if (!vcard.getPhotos().isEmpty()) {
                byte[] image = vcard.getPhotos().get(0).getData();
                Bitmap photoBitmap = BitmapUtils.cropImageToCircle(image);
                holder.mPhoto.setImageBitmap(photoBitmap);
            } else {
                holder.mPhoto.setImageDrawable(photo);
            }
        } else {
            holder.mPhoto.setImageDrawable(photo);
        }

        String fullname = trustRequest.getFullname();
        String username = trustRequest.getDisplayname();
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

        holder.setContactId(trustRequest.getContactId());
    }

    @Override
    public int getItemCount() {
        return mTrustRequests.size();
    }
}
