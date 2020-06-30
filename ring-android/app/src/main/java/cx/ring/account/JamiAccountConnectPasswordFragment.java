/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 * Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.account;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragAccJamiConnectPasswordBinding;
import cx.ring.databinding.FragAccJamiLinkPasswordBinding;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;

public class JamiAccountConnectPasswordFragment extends BaseSupportFragment<JamiAccountConnectPresenter>
        implements JamiConnectAccountView {

    public static final String TAG = JamiAccountConnectPasswordFragment.class.getSimpleName();
    private AccountCreationModel model;
    private FragAccJamiConnectPasswordBinding mBinding;

    public static JamiAccountConnectPasswordFragment newInstance(AccountCreationModel ringAccountViewModel) {
        JamiAccountConnectPasswordFragment fragment = new JamiAccountConnectPasswordFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccJamiConnectPasswordBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
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
    protected void initPresenter(JamiAccountConnectPresenter presenter) {
        presenter.init(model);
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