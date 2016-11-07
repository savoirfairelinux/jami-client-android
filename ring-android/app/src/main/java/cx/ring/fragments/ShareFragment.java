/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package cx.ring.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.model.account.Account;
import cx.ring.utils.QRCodeUtils;
import cx.ring.views.MenuHeaderView;

public class ShareFragment extends Fragment implements MenuHeaderView.MenuHeaderAccountSelectionListener {

    public static final String ARG_URI = "ShareFragment.URI";

    @BindView(R.id.share_instruction)
    TextView mShareInstruction;

    @BindView(R.id.qr_image)
    ImageView mQrImage;

    @BindString(R.string.share_message)
    String mShareMessage;

    @BindString(R.string.share_message_no_account)
    String mShareMessageNoAccount;

    @BindString(R.string.account_contact_me)
    String mAccountCountactMe;

    @BindString(R.string.share_via)
    String mShareVia;

    @BindView(R.id.share_button)
    AppCompatButton mShareButton;

    String mUriToShow;
    String mBlockchainUsername;

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_share);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_share, parent, false);

        ButterKnife.bind(this, inflatedView);

        if (getArguments() != null) {
            mUriToShow = getArguments().getString(ARG_URI);
        }

        mQrImage.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updateView();
            }
        });

        return inflatedView;
    }

    private void updateView() {

        if (mQrImage == null || mShareButton == null || mShareInstruction == null) {
            return;
        }

        if (!TextUtils.isEmpty(mUriToShow)) {
            Bitmap qrBitmap = QRCodeUtils.encodeStringAsQrBitmap(mUriToShow, mQrImage.getMeasuredWidth());
            mQrImage.setImageBitmap(qrBitmap);
            mQrImage.setVisibility(View.VISIBLE);
            mShareButton.setEnabled(true);
            mShareInstruction.setText(mShareMessage);
        } else {
            mShareButton.setEnabled(false);
            mShareInstruction.setText(mShareMessageNoAccount);
            mQrImage.setVisibility(View.INVISIBLE);
        }
    }

    @OnClick(R.id.share_button)
    public void shareRingAccount() {
        if (!TextUtils.isEmpty(mUriToShow)) {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mAccountCountactMe);
            if (!TextUtils.isEmpty(mBlockchainUsername)) {
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.account_share_body_with_username, mUriToShow, mBlockchainUsername));
            } else {
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.account_share_body, mUriToShow));
            }
            startActivity(Intent.createChooser(sharingIntent, mShareVia));
        }
    }

    @Override
    public void accountSelected(Account account) {
        if (account != null) {
            mUriToShow = account.getShareURI();
            mBlockchainUsername = account.getRegisteredName();
            updateView();
        }
    }
}
