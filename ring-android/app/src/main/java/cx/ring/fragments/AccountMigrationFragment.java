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
package cx.ring.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import java.util.HashMap;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragAccountMigrationBinding;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import net.jami.model.Account;
import net.jami.model.AccountConfig;
import net.jami.model.ConfigKey;
import net.jami.services.AccountService;

@AndroidEntryPoint
public class AccountMigrationFragment extends Fragment {
    public static final String ACCOUNT_ID = "ACCOUNT_ID";
    static final String TAG = AccountMigrationFragment.class.getSimpleName();
    @Inject
    AccountService mAccountService;

    private FragAccountMigrationBinding binding;

    private String mAccountId;
    private ProgressDialog mProgress = null;

    private boolean migratingAccount = false;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposableBag.clear();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        binding = FragAccountMigrationBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.ringPassword.setOnEditorActionListener((v, actionId, event) -> actionId == EditorInfo.IME_ACTION_NEXT && checkPassword(v, null));
        binding.ringPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkPassword((TextView) v, null);
            }
        });

        binding.ringMigrateBtn.setOnClickListener(v -> initAccountMigration(binding.ringPassword.getText().toString()));
        binding.deleteBtn.setOnClickListener(v -> initAccountDelete());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments() != null) {
            mAccountId = getArguments().getString(ACCOUNT_ID);
        }
    }

    private void initAccountDelete() {
        if (migratingAccount)
            return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_delete_dialog_title)
                .setMessage(R.string.account_delete_dialog_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.menu_delete, (d, w) -> deleteAccount())
                .create()
                .show();
    }

    private void deleteAccount() {
        mAccountService.removeAccount(mAccountId);
    }

    private void initAccountMigration(String password) {
        if (migratingAccount)
            return;
        migratingAccount = true;

        //orientation is locked during the migration of account to avoid the destruction of the thread
        Activity activity = getActivity();
        if (activity != null)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        mProgress = new ProgressDialog(getActivity());
        mProgress.setTitle(R.string.dialog_wait_update);
        mProgress.setMessage(getString(R.string.dialog_wait_update_details));
        mProgress.setCancelable(false);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        final Account account = mAccountService.getAccount(mAccountId);
        HashMap<String, String> details = account.getDetails();
        details.put(ConfigKey.ARCHIVE_PASSWORD.key(), password);

        mAccountService.setAccountDetails(account.getAccountID(), details);

        mDisposableBag.add(mAccountService
                .migrateAccount(mAccountId, password)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleMigrationState));
    }

    private boolean checkPassword(@NonNull TextView pwd, TextView confirm) {
        boolean error = false;
        if (pwd.getText().length() == 0) {
            error = true;
        } else {
            if (pwd.getText().length() < 6) {
                pwd.setError(getString(R.string.error_password_char_count));
                error = true;
            } else {
                pwd.setError(null);
            }
        }
        if (confirm != null) {
            if (!pwd.getText().toString().equals(confirm.getText().toString())) {
                confirm.setError(getString(R.string.error_passwords_not_equals));
                confirm.requestFocus();
                error = true;
            } else {
                confirm.setError(null);
            }
        }
        return error;
    }

    private void handleMigrationState(String newState) {
        migratingAccount = false;

        if (TextUtils.isEmpty(newState)) {
            if (mProgress != null) {
                mProgress.dismiss();
                mProgress = null;
            }
            return;
        }

        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        dialogBuilder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
            //do things
        });
        boolean success = false;
        if (AccountConfig.STATE_INVALID.equals(newState)) {
            dialogBuilder.setTitle(R.string.account_cannot_be_found_title)
                    .setMessage(R.string.account_cannot_be_updated_message);
        } else {
            dialogBuilder.setTitle(R.string.account_device_updated_title)
                    .setMessage(R.string.account_device_updated_message);
            success = true;
        }
        AlertDialog dialogSuccess = dialogBuilder.show();
        if (success) {
            dialogSuccess.setOnDismissListener(dialogInterface -> {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.setResult(Activity.RESULT_OK, new Intent());
                    //unlock the screen orientation
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    activity.finish();
                }
            });
        }
    }

}