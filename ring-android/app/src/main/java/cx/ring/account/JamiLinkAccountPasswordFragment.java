/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import cx.ring.databinding.FragAccJamiLinkPasswordBinding;

import net.jami.account.JamiLinkAccountPresenter;
import net.jami.account.JamiLinkAccountView;
import net.jami.mvp.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class JamiLinkAccountPasswordFragment extends BaseSupportFragment<JamiLinkAccountPresenter, JamiLinkAccountView>
        implements JamiLinkAccountView {

    public static final String TAG = JamiLinkAccountPasswordFragment.class.getSimpleName();
    private AccountCreationModel model;
    private FragAccJamiLinkPasswordBinding mBinding;

    public static JamiLinkAccountPasswordFragment newInstance(AccountCreationModel ringAccountViewModel) {
        JamiLinkAccountPasswordFragment fragment = new JamiLinkAccountPasswordFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (model == null)
            return null;
        mBinding = FragAccJamiLinkPasswordBinding.inflate(inflater, container, false);
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
        mBinding.linkButton.setOnClickListener(v -> presenter.linkClicked());
        mBinding.ringAddPin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.linkClicked();
            }
            return false;
        });
        mBinding.ringAddPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.pinChanged(s.toString());
            }
        });
        mBinding.ringExistingPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.passwordChanged(s.toString());
            }
        });
    }

    @Override
    protected void initPresenter(net.jami.account.JamiLinkAccountPresenter presenter) {
        presenter.init(model);
    }

    @Override
    public void enableLinkButton(boolean enable) {
        mBinding.linkButton.setEnabled(enable);
    }

    @Override
    public void showPin(boolean show) {
        mBinding.pinBox.setVisibility(show ? View.VISIBLE : View.GONE);
        mBinding.pinHelpMessage.setVisibility(show ? View.VISIBLE : View.GONE);
        mBinding.linkButton.setText(show ? R.string.account_link_device : R.string.account_link_archive_button);
    }

    @Override
    public void createAccount(AccountCreationModel accountCreationModel) {
        ((AccountWizardActivity) requireActivity()).createAccount(accountCreationModel);
        InputMethodManager imm = (InputMethodManager) requireActivity().
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mBinding.ringExistingPassword.getWindowToken(), 0);
    }

    @Override
    public void cancel() {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null) {
            wizardActivity.onBackPressed();
        }
    }

}