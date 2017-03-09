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
import butterknife.OnClick;
import cx.ring.R;

public class ContactRequestView extends RecyclerView.ViewHolder {
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

    private String mContactId;
    private PendingContactRequestsPresenter mPresenter;

    ContactRequestView(View view, PendingContactRequestsPresenter presenter) {
        super(view);
        ButterKnife.bind(this, view);
        mPresenter = presenter;
    }

    public void setContactId(String contactId) {
        mContactId = contactId;
    }

    @OnClick(R.id.button_accept)
    public void acceptClicked() {
        mPresenter.acceptTrustRequest(mContactId);
    }

    @OnClick(R.id.button_refuse)
    public void refuseClicked() {
        mPresenter.refuseTrustRequest(mContactId);
    }

    @OnClick(R.id.button_block)
    public void blockClicked() {
        mPresenter.blockTrustRequest(mContactId);
    }
}
