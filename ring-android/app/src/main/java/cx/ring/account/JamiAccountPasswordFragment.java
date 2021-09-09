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
import cx.ring.databinding.FragAccJamiPasswordBinding;

import net.jami.account.JamiAccountCreationPresenter;
import net.jami.account.JamiAccountCreationView;
import net.jami.model.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class JamiAccountPasswordFragment extends BaseSupportFragment<JamiAccountCreationPresenter, JamiAccountCreationView>
        implements JamiAccountCreationView {

    private static final String KEY_MODEL = "model";
    private AccountCreationModel model;
    private FragAccJamiPasswordBinding binding;

    private boolean mIsChecked = false;

    public static JamiAccountPasswordFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        JamiAccountPasswordFragment fragment = new JamiAccountPasswordFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (model != null)
            outState.putSerializable(KEY_MODEL, model);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);
        if (savedInstanceState != null && model == null) {
            model = (AccountCreationModelImpl) savedInstanceState.getSerializable(KEY_MODEL);
        }
        binding = FragAccJamiPasswordBinding.inflate(inflater, container, false);
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

        binding.createAccount.setOnClickListener(v -> presenter.createAccount());
        binding.ringPasswordSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mIsChecked = isChecked;
            if (isChecked) {
                binding.passwordTxtBox.setVisibility(View.VISIBLE);
                binding.ringPasswordRepeatTxtBox.setVisibility(View.VISIBLE);
                binding.placeholder.setVisibility(View.GONE);
                CharSequence password = binding.ringPassword.getText();
                presenter.passwordChanged(password == null ? null : password.toString(), binding.ringPasswordRepeat.getText());
            } else {
                binding.passwordTxtBox.setVisibility(View.GONE);
                binding.ringPasswordRepeatTxtBox.setVisibility(View.GONE);
                binding.placeholder.setVisibility(View.VISIBLE);
                presenter.passwordUnset();
            }
        });
        binding.ringPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.passwordChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.ringPasswordRepeat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.passwordConfirmChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.ringPasswordRepeat.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.createAccount();
            }
            return false;
        });
        binding.ringPasswordRepeat.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.createAccount.isEnabled()) {
                InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                presenter.createAccount();
                return true;
            }
            return false;
        });

        presenter.init(model);
    }

    @Override
    public void updateUsernameAvailability(UsernameAvailabilityStatus status) {
    }

    @Override
    public void showInvalidPasswordError(final boolean display) {
        if (display) {
            binding.passwordTxtBox.setError(getString(R.string.error_password_char_count));
        } else {
            binding.passwordTxtBox.setError(null);
        }
    }

    @Override
    public void showNonMatchingPasswordError(final boolean display) {
        if (display) {
            binding.ringPasswordRepeatTxtBox.setError(getString(R.string.error_passwords_not_equals));
        } else {
            binding.ringPasswordRepeatTxtBox.setError(null);
        }
    }

    @Override
    public void enableNextButton(final boolean enabled) {
        if (!mIsChecked) {
            binding.createAccount.setEnabled(true);
            return;
        }

        binding.createAccount.setEnabled(enabled);
    }

    @Override
    public void goToAccountCreation(AccountCreationModel accountCreationModel) {
        Activity wizardActivity = getActivity();
        if (wizardActivity instanceof AccountWizardActivity) {
            AccountWizardActivity wizard = (AccountWizardActivity) wizardActivity;
            wizard.createAccount(accountCreationModel);
            JamiAccountCreationFragment parent = (JamiAccountCreationFragment) getParentFragment();
            if (parent != null) {
                parent.scrollPagerFragment(accountCreationModel);
                InputMethodManager imm = (InputMethodManager) wizard.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(binding.ringPassword.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void cancel() {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null) {
            wizardActivity.onBackPressed();
        }
    }

    public void setUsername(String username) {
        model.setUsername(username);
    }

}
