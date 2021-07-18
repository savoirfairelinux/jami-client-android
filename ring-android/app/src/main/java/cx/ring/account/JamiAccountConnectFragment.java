/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragAccJamiConnectBinding;

import net.jami.account.JamiAccountConnectPresenter;
import net.jami.account.JamiConnectAccountView;
import net.jami.mvp.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class JamiAccountConnectFragment extends BaseSupportFragment<JamiAccountConnectPresenter, JamiConnectAccountView> implements JamiConnectAccountView {
    public static final String TAG = JamiAccountConnectFragment.class.getSimpleName();

    private AccountCreationModel model;
    private FragAccJamiConnectBinding mBinding;

    public static JamiAccountConnectFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        JamiAccountConnectFragment fragment = new JamiAccountConnectFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccJamiConnectBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    protected void initPresenter(net.jami.account.JamiAccountConnectPresenter presenter) {
        presenter.init(model);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.connectButton.setOnClickListener(v -> presenter.connectClicked());
        mBinding.usernameTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.usernameChanged(s.toString());
            }
        });
        mBinding.passwordTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.passwordChanged(s.toString());
            }
        });
        mBinding.promptServer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.serverChanged(s.toString());
            }
        });

        mBinding.passwordTxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.connectClicked();
            }
            return false;
        });
    }

    @Override
    public void enableConnectButton(boolean enable) {
        mBinding.connectButton.setEnabled(enable);
    }

    @Override
    public void createAccount(AccountCreationModel accountCreationModel) {
        ((AccountWizardActivity) requireActivity()).createAccount(accountCreationModel);
    }

    @Override
    public void cancel() {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null) {
            wizardActivity.onBackPressed();
        }
    }
}