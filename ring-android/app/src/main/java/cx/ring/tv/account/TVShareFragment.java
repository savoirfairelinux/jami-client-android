/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
package cx.ring.tv.account;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import butterknife.BindString;
import butterknife.BindView;
import cx.ring.R;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.mvp.BaseFragment;
import cx.ring.mvp.GenericView;
import cx.ring.share.SharePresenter;
import cx.ring.share.ShareViewModel;
import cx.ring.utils.QRCodeUtils;

public class TVShareFragment extends BaseFragment<SharePresenter> implements GenericView<ShareViewModel> {

    @BindView(R.id.share_instruction)
    protected TextView mShareInstruction;

    @BindView(R.id.qr_image)
    protected ImageView mQrImage;

    @BindString(R.string.share_message)
    protected String mShareMessage;

    @Override
    public int getLayout() {
        return R.layout.tv_frag_share;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void showViewModel(final ShareViewModel viewModel) {
        final QRCodeUtils.QRCodeData qrCodeData = viewModel.getAccountQRCodeData();

        if (mQrImage == null || mShareInstruction == null) {
            return;
        }

        if (qrCodeData == null) {
            mQrImage.setVisibility(View.INVISIBLE);
        } else {
            Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth(), qrCodeData.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), 0, 0, qrCodeData.getWidth(), qrCodeData.getHeight());
            mQrImage.setImageBitmap(bitmap);
            mShareInstruction.setText(mShareMessage);
            mQrImage.setVisibility(View.VISIBLE);
        }

    }
}
