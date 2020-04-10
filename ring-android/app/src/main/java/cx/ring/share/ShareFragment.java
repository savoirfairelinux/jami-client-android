/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragShareBinding;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.GenericView;
import cx.ring.utils.QRCodeUtils;

public class ShareFragment extends BaseSupportFragment<SharePresenter> implements GenericView<ShareViewModel> {

    private String mUriToShow;
    private boolean isShareLocked = false;
    private FragShareBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragShareBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        binding.shareButton.setOnClickListener(v -> {
            if (!isShareLocked) shareAccount();
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.qr_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_qr_share:
                if (!isShareLocked) {
                    shareAccount();
                    return true;
                } else {
                    return false;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareAccount() {
        if (!TextUtils.isEmpty(mUriToShow)) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.account_contact_me));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.account_share_body, mUriToShow, getText(R.string.app_website)));
            startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_via)));
        }
    }

    @Override
    public void showViewModel(final ShareViewModel viewModel) {
        if (binding == null)
            return;

        final QRCodeUtils.QRCodeData qrCodeData = viewModel.getAccountQRCodeData(0xFF000000, 0xFFFFFFFF);
        if (qrCodeData == null) {
            binding.qrImage.setVisibility(View.INVISIBLE);
            binding.shareInstruction.setText(R.string.share_message_no_account);
        } else {
            Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth(), qrCodeData.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), 0, 0, qrCodeData.getWidth(), qrCodeData.getHeight());
            binding.qrImage.setImageBitmap(bitmap);
            binding.shareInstruction.setText(R.string.share_message);
            binding.qrImage.setVisibility(View.VISIBLE);
        }

        mUriToShow = viewModel.getAccountDisplayUri();
        isShareLocked = TextUtils.isEmpty(mUriToShow);
    }
}
