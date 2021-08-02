/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.TvFragShareBinding;
import net.jami.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import net.jami.mvp.GenericView;
import cx.ring.services.VCardServiceImpl;
import net.jami.share.SharePresenter;
import net.jami.share.ShareViewModel;
import net.jami.utils.Log;
import net.jami.utils.QRCodeUtils;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class TVShareFragment extends BaseSupportFragment<SharePresenter, GenericView<ShareViewModel>> implements GenericView<ShareViewModel> {

    private TvFragShareBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = TvFragShareBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        disposable.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    @Override
    public void showViewModel(final ShareViewModel viewModel) {
        if (binding == null)
            return;

        final QRCodeUtils.QRCodeData qrCodeData = viewModel.getAccountQRCodeData(0x00000000, 0xFFFFFFFF);
        getUserAvatar(viewModel.getAccount());

        if (qrCodeData == null) {
            binding.qrImage.setVisibility(View.INVISIBLE);
        } else {
            int pad = 56;
            Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth() + (2 * pad), qrCodeData.getHeight() + (2 * pad), Bitmap.Config.ARGB_8888);
            bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), pad, pad, qrCodeData.getWidth(), qrCodeData.getHeight());
            binding.qrImage.setImageBitmap(bitmap);
            binding.shareQrInstruction.setText(R.string.share_message);
            binding.qrImage.setVisibility(View.VISIBLE);
        }
    }

    private void getUserAvatar(Account account) {
        disposable.add(VCardServiceImpl.Companion
                .loadProfile(requireContext(), account)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(profile -> {
                    if (binding != null) {
                        binding.shareUri.setVisibility(View.VISIBLE);
                        binding.shareUri.setText(account.getDisplayUsername());
                    }
                })
                .map(p -> AvatarDrawable.build(requireContext(), account, p, true))
                .subscribe(a -> {
                    if (binding != null) {
                        binding.qrUserPhoto.setVisibility(View.VISIBLE);
                        binding.qrUserPhoto.setImageDrawable(a);
                    }
                }, e-> Log.e(TVShareFragment.class.getSimpleName(), e.getMessage())));
    }
}
