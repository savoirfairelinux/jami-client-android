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
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.AndroidFileUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class HomeAccountCreationFragment extends BaseSupportFragment<HomeAccountCreationPresenter> implements HomeAccountCreationView {
    private static final int ARCHIVE_REQUEST_CODE = 42;

    public static final String TAG = HomeAccountCreationFragment.class.getSimpleName();

    @BindView(R.id.ring_add_account)
    protected Button mLinkButton;

    @BindView(R.id.ring_create_btn)
    protected Button mCreateButton;

    @Override
    public int getLayout() {
        return R.layout.frag_acc_home_create;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);
    }

    @OnClick(R.id.ring_add_account)
    public void linkAccountClicked() {
        presenter.clickOnLinkAccount();
    }

    @OnClick(R.id.ring_create_btn)
    public void createAccountClicked() {
        presenter.clickOnCreateAccount();
    }

    @OnClick(R.id.account_connect_server)
    public void connectAccountClicked() {
        presenter.clickOnConnectAccount();
    }

    @Override
    public void goToAccountCreation() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        Fragment fragment = RingAccountCreationFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @Override
    public void goToAccountLink() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(true);
        Fragment fragment = RingLinkAccountFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @Override
    public void goToAccountConnect() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(true);
        Fragment fragment = JamiAccountConnectFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @OnClick(R.id.ring_import_account)
    public void performFileSearch() {
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
                                Fragment fragment = RingLinkAccountFragment.newInstance(ringAccountViewModel);
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
