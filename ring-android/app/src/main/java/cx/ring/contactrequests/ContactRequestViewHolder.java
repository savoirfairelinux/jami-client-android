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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;

public class ContactRequestViewHolder extends RecyclerView.ViewHolder {

    private ContactRequestListeners clickListener;
    private PendingContactRequestsViewModel viewModel;

    @BindView(R.id.rlContactRequest)
    protected RelativeLayout rlContactRequest;

    @BindView(R.id.photo)
    protected ImageView mPhoto;

    @BindView(R.id.display_name)
    protected TextView mDisplayname;

    @BindView(R.id.fullname)
    protected TextView mFullname;

    @BindView(R.id.username)
    protected TextView mUsername;

    @BindView(R.id.name_layout)
    protected LinearLayout mNamelayout;

    ContactRequestViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bind(final ContactRequestListeners clickListener, final PendingContactRequestsViewModel viewModel) {
        this.clickListener = clickListener;
        this.viewModel = viewModel;
    }

    @OnClick(R.id.rlContactRequest)
    public void rlContactRequestClick() {
        clickListener.onContactRequestClick(viewModel);
    }

    public interface ContactRequestListeners {
        void onContactRequestClick(PendingContactRequestsViewModel viewModel);
    }
}
