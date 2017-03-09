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

package cx.ring.trustrequests;

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

public class TrustRequestsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final String TAG = TrustRequestsAdapter.class.getSimpleName();

    private List<TrustRequest> mTrustRequests;
    private Context mContext;

    public TrustRequestsAdapter(Context context, List<TrustRequest> trustRequests) {
        mContext = context;
        mTrustRequests = trustRequests;
    }

    public void replaceAll(List<TrustRequest> trustRequests) {
        mTrustRequests = trustRequests;
        notifyDataSetChanged();
    }

    class TrustRequestView extends RecyclerView.ViewHolder {
        @BindView(R.id.button_accept)
        AppCompatButton mButtonAccept;

        @BindView(R.id.button_refuse)
        AppCompatButton mButtonRefuse;

        @BindView(R.id.button_block)
        AppCompatButton mButtonBlock;

        @BindView(R.id.photo)
        AppCompatImageView mPhoto;

        @BindView(R.id.display_name)
        TextView mDisplayname;

        @BindView(R.id.fullname)
        protected TextView mFullname;

        @BindView(R.id.username)
        protected TextView mUsername;

        @BindView(R.id.name_layout)
        protected LinearLayout mNamelayout;

        TrustRequestView(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        @OnClick(R.id.button_accept)
        public void acceptClicked() {

        }

        @OnClick(R.id.button_refuse)
        public void refuseClicked() {

        }

        @OnClick(R.id.button_block)
        public void blockClicked() {

        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        View holderView;

        holderView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trust_request, parent, false);
        viewHolder = new TrustRequestView(holderView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TrustRequest trustRequest = mTrustRequests.get(position);

        //default photo
        Drawable photo = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_contact_picture, null);

        VCard vcard = trustRequest.getVCard();
        Log.d(TAG, "load vcard " + VCardUtils.vcardToString(vcard));
        if (vcard != null) {
            if (!vcard.getPhotos().isEmpty()) {
                byte[] image = vcard.getPhotos().get(0).getData();
                Bitmap photoBitmap = BitmapUtils.cropImageToCircle(image);
                ((TrustRequestView) holder).mPhoto.setImageBitmap(photoBitmap);
            } else {
                ((TrustRequestView) holder).mPhoto.setImageDrawable(photo);
            }
        } else {
            ((TrustRequestView) holder).mPhoto.setImageDrawable(photo);
        }

        String fullname = trustRequest.getFullname();
        String username = trustRequest.getDisplayname();
        if (!TextUtils.isEmpty(fullname)) {
            ((TrustRequestView) holder).mDisplayname.setVisibility(View.GONE);
            ((TrustRequestView) holder).mNamelayout.setVisibility(View.VISIBLE);
            ((TrustRequestView) holder).mFullname.setText(fullname);
            ((TrustRequestView) holder).mUsername.setText(username);
        } else {
            ((TrustRequestView) holder).mDisplayname.setVisibility(View.VISIBLE);
            ((TrustRequestView) holder).mNamelayout.setVisibility(View.GONE);
            ((TrustRequestView) holder).mDisplayname.setText(username);
        }
    }

    @Override
    public int getItemCount() {
        return mTrustRequests.size();
    }
}
