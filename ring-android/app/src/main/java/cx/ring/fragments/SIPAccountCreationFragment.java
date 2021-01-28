/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package cx.ring.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragAccSipCreateBinding;
import cx.ring.mvp.BaseSupportFragment;
import net.jami.mvp.SIPCreationView;
import net.jami.wizard.SIPCreationPresenter;

public class SIPAccountCreationFragment extends BaseSupportFragment<SIPCreationPresenter> implements SIPCreationView {
    public static final String TAG = SIPAccountCreationFragment.class.getSimpleName();

    private ProgressDialog mProgress = null;
    private FragAccSipCreateBinding binding = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccSipCreateBinding.inflate(inflater, container, false);
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
        binding.password.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.createSipButton.callOnClick();
            }
            return false;
        });
        binding.createSipButton.setOnClickListener(v -> createSIPAccount(false));
    }

    /**
     * Start the creation process in the presenter
     *
     * @param bypassWarnings boolean stating if we want to display warning to the user or create the account anyway
     */
    private void createSIPAccount(boolean bypassWarnings) {
        //orientation is locked during the create of account to avoid the destruction of the thread
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        String hostname = binding.hostname.getText().toString();
        String proxy = binding.proxy.getText().toString();
        String username = binding.username.getText().toString();
        String password = binding.password.getText().toString();
        presenter.startCreation(hostname, proxy, username, password, bypassWarnings);
    }

    @Override
    public void showUsernameError() {
        binding.username.setError(getString(R.string.error_field_required));
        binding.username.requestFocus();
    }

    @Override
    public void showLoading() {
        mProgress = new ProgressDialog(getActivity());
        mProgress.setTitle(R.string.dialog_wait_create);
        mProgress.setMessage(getString(R.string.dialog_wait_create_details));
        mProgress.setCancelable(false);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
    }

    @Override
    public void resetErrors() {
        binding.password.setError(null);
    }

    @Override
    public void showPasswordError() {
        binding.password.setError(getString(R.string.error_field_required));
        binding.password.requestFocus();
    }

    @Override
    public void showIP2IPWarning() {
        showDialog(getActivity().getString(R.string.dialog_warn_ip2ip_account_title),
                getActivity().getString(R.string.dialog_warn_ip2ip_account_message),
                getActivity().getString(android.R.string.ok),
                getActivity().getString(android.R.string.cancel),
                (dialog, which) -> {
                    dialog.dismiss();
                    createSIPAccount(true);
                },
                null);
    }

    @Override
    public void showRegistrationError() {
        showDialog(getActivity().getString(R.string.account_sip_cannot_be_registered),
                getActivity().getString(R.string.account_sip_cannot_be_registered_message),
                getActivity().getString(android.R.string.ok),
                getActivity().getString(R.string.account_sip_register_anyway),
                (dialog, which) -> presenter.removeAccount(),
                (dialog, id) -> {
                    getActivity().setResult(Activity.RESULT_OK, new Intent());
                    getActivity().finish();
                });
    }

    @Override
    public void showRegistrationNetworkError() {
        showDialog(getActivity().getString(R.string.account_no_network_title),
                getActivity().getString(R.string.account_no_network_message),
                getActivity().getString(android.R.string.ok),
                getActivity().getString(R.string.account_sip_register_anyway),
                (dialog, which) -> presenter.removeAccount(),
                (dialog, id) -> {
                    getActivity().setResult(Activity.RESULT_OK, new Intent());
                    getActivity().finish();
                });
    }

    @Override
    public void showRegistrationSuccess() {
        showDialog(getActivity().getString(R.string.account_sip_success_title),
                getActivity().getString(R.string.account_sip_success_message),
                getActivity().getString(android.R.string.ok),
                null,
                (dialog, which) -> {
                    getActivity().setResult(Activity.RESULT_OK, new Intent());
                    getActivity().finish();
                },
                null);
    }

    public void showDialog(final String title,
                           final String message,
                           final String positive,
                           final String negative,
                           final DialogInterface.OnClickListener listenerPositive,
                           final DialogInterface.OnClickListener listenerNegative) {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }

        //orientation is locked during the create of account to avoid the destruction of the thread
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        new MaterialAlertDialogBuilder(requireContext())
                .setPositiveButton(positive, listenerPositive)
                .setNegativeButton(negative, listenerNegative)
                .setTitle(title).setMessage(message)
                .setOnDismissListener(dialog -> {
                    //unlock the screen orientation
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                })
                .show();
    }
}
