/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.account;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragAccHomeCreateBinding;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.AndroidFileUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class HomeAccountCreationFragment extends BaseSupportFragment<HomeAccountCreationPresenter> implements HomeAccountCreationView {
    private static final int ARCHIVE_REQUEST_CODE = 42;

    public static final String TAG = HomeAccountCreationFragment.class.getSimpleName();

    private FragAccHomeCreateBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccHomeCreateBinding.inflate(inflater, container, false);
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
        setRetainInstance(true);
        binding.ringAddAccount.setOnClickListener(v -> presenter.clickOnLinkAccount());
        binding.ringCreateBtn.setOnClickListener(v -> presenter.clickOnCreateAccount());
        binding.accountConnectServer.setOnClickListener(v -> presenter.clickOnConnectAccount());
        binding.ringImportAccount.setOnClickListener(v -> performFileSearch());
    }

    @Override
    public void goToAccountCreation() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        Fragment fragment = JamiAccountCreationFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @Override
    public void goToAccountLink() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(true);
        Fragment fragment = JamiLinkAccountFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @Override
    public void goToAccountConnect() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(true);
        Fragment fragment = JamiAccountConnectFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    private void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, ARCHIVE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                if (uri != null) {
                    AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(file -> {
                                AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
                                ringAccountViewModel.setLink(true);
                                ringAccountViewModel.setArchive(file);
                                Fragment fragment = JamiLinkAccountFragment.newInstance(ringAccountViewModel);
                                replaceFragmentWithSlide(fragment, R.id.wizard_container);
                            }, e-> {
                                View v = getView();
                                if (v != null)
                                    Snackbar.make(v, "Can't import archive: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                }
            }
        }
    }
}
