/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragAccRingLinkBinding;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.AccountCreationModel;

public class JamiLinkAccountFragment extends BaseSupportFragment<JamiLinkAccountPresenter> implements JamiLinkAccountView {

    public static final String TAG = JamiLinkAccountFragment.class.getSimpleName();
    private AccountCreationModel model;
    private FragAccRingLinkBinding binding;

    public static JamiLinkAccountFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        JamiLinkAccountFragment fragment = new JamiLinkAccountFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccRingLinkBinding.inflate(inflater, container, false);
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
        binding.linkButton.setOnClickListener(v -> presenter.linkClicked());
        binding.ringAddPin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.linkClicked();
            }
            return false;
        });
        binding.ringAddPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.pinChanged(s.toString());
            }
        });
        binding.ringExistingPassword.addTextChangedListener(new TextWatcher() {
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
    protected void initPresenter(JamiLinkAccountPresenter presenter) {
        presenter.init(model);
    }

    @Override
    public void enableLinkButton(boolean enable) {
        binding.linkButton.setEnabled(enable);
    }

    @Override
    public void showPin(boolean show) {
        binding.pinBox.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.pinHelpMessage.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.linkButton.setText(show ? R.string.account_link_device : R.string.account_link_archive_button);
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