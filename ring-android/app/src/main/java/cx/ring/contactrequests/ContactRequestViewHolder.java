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

import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;

public class ContactRequestViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.button_accept)
    protected AppCompatButton mButtonAccept;

    @BindView(R.id.button_refuse)
    protected AppCompatButton mButtonRefuse;

    @BindView(R.id.button_block)
    protected AppCompatButton mButtonBlock;

    @BindView(R.id.photo)
    protected AppCompatImageView mPhoto;

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

    public void bind(final ContactRequestListeners clickListerner, final PendingContactRequestsViewModel viewModel) {
        mButtonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListerner.onAcceptClick(viewModel);
            }
        });

        mButtonRefuse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListerner.onRefuseClick(viewModel);
            }
        });

        mButtonBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListerner.onBlockClick(viewModel);
            }
        });
    }

    public interface ContactRequestListeners {
        void onAcceptClick(PendingContactRequestsViewModel viewModel);

        void onRefuseClick(PendingContactRequestsViewModel viewModel);

        void onBlockClick(PendingContactRequestsViewModel viewModel);
    }
}
