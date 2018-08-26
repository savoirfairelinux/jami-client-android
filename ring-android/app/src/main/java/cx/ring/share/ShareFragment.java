/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
package cx.ring.share;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.mvp.BaseFragment;
import cx.ring.mvp.GenericView;
import cx.ring.utils.QRCodeUtils;

public class ShareFragment extends BaseFragment<SharePresenter> implements GenericView<ShareViewModel> {

    @BindView(R.id.share_instruction)
    protected TextView mShareInstruction;

    @BindView(R.id.qr_image)
    protected ImageView mQrImage;

    @BindString(R.string.share_message)
    protected String mShareMessage;

    @BindString(R.string.share_message_no_account)
    protected String mShareMessageNoAccount;

    @BindString(R.string.account_contact_me)
    protected String mAccountCountactMe;

    @BindString(R.string.share_via)
    protected String mShareVia;

    @BindView(R.id.share_button)
    protected AppCompatButton mShareButton;

    private String mUriToShow;
    private int mQRCodeSize = 0;

    @Override
    public int getLayout() {
        return R.layout.frag_share;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);

        mQrImage.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            mQRCodeSize = mQrImage.getMeasuredWidth();

            // when view is ready, we search for contact infos to display
            presenter.loadContactInformation();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_share);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @OnClick(R.id.share_button)
    public void shareRingAccount() {
        if (!TextUtils.isEmpty(mUriToShow)) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mAccountCountactMe);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.account_share_body, mUriToShow));
            startActivity(Intent.createChooser(sharingIntent, mShareVia));
        }
    }

    //region View Methods Implementation
    @Override
    public void showViewModel(final ShareViewModel viewModel) {
        final QRCodeUtils.QRCodeData qrCodeData = viewModel.getAccountQRCodeData();

        if (mQrImage == null || mShareInstruction == null) {
            return;
        }

        RingApplication.uiHandler.post(() -> {
            if (qrCodeData == null || mQRCodeSize <= 0) {
                mQrImage.setVisibility(View.INVISIBLE);
                mShareInstruction.setText(mShareMessageNoAccount);
            } else {
                Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth(), qrCodeData.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), 0, 0, qrCodeData.getWidth(), qrCodeData.getHeight());
                mQrImage.setImageBitmap(bitmap);
                mShareInstruction.setText(mShareMessage);
                mQrImage.setVisibility(View.VISIBLE);
            }

            mUriToShow = viewModel.getAccountShareUri();
            if (TextUtils.isEmpty(mUriToShow)) {
                mShareButton.setEnabled(false);
            } else {
                mShareButton.setEnabled(true);
            }
        });
    }
    //endregion
}
