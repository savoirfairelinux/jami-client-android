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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.TvFragShareBinding;
import cx.ring.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.GenericView;
import cx.ring.services.VCardServiceImpl;
import cx.ring.share.SharePresenter;
import cx.ring.share.ShareViewModel;
import cx.ring.utils.Log;
import cx.ring.utils.QRCodeUtils;
import cx.ring.views.AvatarDrawable;
import io.reactivex.disposables.CompositeDisposable;

public class TVShareFragment extends BaseSupportFragment<SharePresenter> implements GenericView<ShareViewModel> {

    private TvFragShareBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = TvFragShareBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
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
        final QRCodeUtils.QRCodeData qrCodeData = viewModel.getAccountQRCodeData(0x00000000, 0xFFFFFFFF);
        getUserAvatar(viewModel.getAccount());

        if (binding == null) {
            return;
        }

        if (qrCodeData == null) {
            binding.qrImage.setVisibility(View.INVISIBLE);

        } else {
            Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth(), qrCodeData.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), 0, 0, qrCodeData.getWidth(), qrCodeData.getHeight());
            binding.qrImage.setImageBitmap(bitmap);
            binding.shareQrInstruction.setText(R.string.share_message);
            binding.qrImage.setVisibility(View.VISIBLE);
        }
    }

    private void getUserAvatar(Account account) {
        disposable.add(VCardServiceImpl
                .loadProfile(account)
                .doOnSuccess(profile -> {
                    binding.shareUri.setVisibility(View.VISIBLE);
                    if (profile.first != null && !profile.first.isEmpty()) {
                        binding.shareUri.setText(profile.first);
                    } else {
                        binding.shareUri.setText(account.getDisplayUri());
                    }
                })
                .flatMap(p -> AvatarDrawable.load(getActivity(), account))
                .subscribe(a -> {
                    binding.qrUserPhoto.setVisibility(View.VISIBLE);
                    binding.qrUserPhoto.setImageDrawable(a);
                }, e-> Log.e(TVShareFragment.class.getSimpleName(), e.getMessage())));
    }
}
