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
package cx.ring.fragments;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.jami.account.LinkDevicePresenter;
import net.jami.account.LinkDeviceView;
import net.jami.model.Account;

import cx.ring.R;
import cx.ring.account.AccountEditionFragment;
import cx.ring.databinding.FragLinkDeviceBinding;
import cx.ring.mvp.BaseBottomSheetFragment;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.KeyboardVisibilityManager;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LinkDeviceFragment extends BaseBottomSheetFragment<LinkDevicePresenter> implements LinkDeviceView {

    public static final String TAG = LinkDeviceFragment.class.getSimpleName();

    public static LinkDeviceFragment newInstance(String accountId) {
        LinkDeviceFragment fragment = new LinkDeviceFragment();

        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        fragment.setArguments(args);

        return fragment;
    }

    private FragLinkDeviceBinding mBinding = null;

    private boolean mAccountHasPassword = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = FragLinkDeviceBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String accountId = getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY);
            if (accountId != null) {
                presenter.setAccountId(accountId);
            }
        }

        mBinding.btnStartExport.setOnClickListener(v -> onClickStart());
        mBinding.ringPassword.setOnEditorActionListener(this::onPasswordEditorAction);
        mBinding.passwordLayout.setVisibility(mAccountHasPassword ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogINterface -> {
            if (DeviceUtils.isTablet(requireContext())) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
            }
        });
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        addGlobalLayoutListener(requireView());
    }

    private void addGlobalLayoutListener(final View view) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                setPeekHeight(v.getMeasuredHeight());
                v.removeOnLayoutChangeListener(this);
            }
        });
    }

    public void setPeekHeight(int peekHeight) {
        BottomSheetBehavior<?> behavior = getBottomSheetBehaviour();
        if (behavior == null) {
            return;
        }

        behavior.setPeekHeight(peekHeight);
    }

    private BottomSheetBehavior<?> getBottomSheetBehaviour() {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) ((View) requireView().getParent()).getLayoutParams();
        CoordinatorLayout.Behavior<?> behavior = layoutParams.getBehavior();
        if (behavior instanceof BottomSheetBehavior) {
            return (BottomSheetBehavior<?>) behavior;
        }
        return null;
    }

    @Override
    public void showExportingProgress() {
        mBinding.progressBar.setVisibility(View.VISIBLE);
        mBinding.accountLinkInfo.setVisibility(View.GONE);
        mBinding.btnStartExport.setVisibility(View.GONE);
        mBinding.passwordLayout.setVisibility(View.GONE);
    }

    @Override
    public void dismissExportingProgress() {
        mBinding.progressBar.setVisibility(View.GONE);
        mBinding.accountLinkInfo.setVisibility(View.VISIBLE);
        mBinding.btnStartExport.setVisibility(View.VISIBLE);
        mBinding.passwordLayout.setVisibility(mAccountHasPassword ? View.VISIBLE : View.GONE);
    }

    @Override
    public void accountChanged(Account account) {
        mAccountHasPassword = account.hasPassword();
    }

    @Override
    public void showNetworkError() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_export_end_network_title)
                .setMessage(R.string.account_export_end_network_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPasswordError() {
        mBinding.passwordLayout.setError(getString(R.string.account_export_end_decryption_message));
        mBinding.ringPassword.setText("");
    }

    @Override
    public void showGenericError() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_export_end_error_title)
                .setMessage(R.string.account_export_end_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPIN(String pin) {
        dismissExportingProgress();
        mBinding.ringPassword.setText("");
        mBinding.passwordLayout.setVisibility(View.GONE);
        mBinding.btnStartExport.setVisibility(View.GONE);
        String pined = getString(R.string.account_end_export_infos).replace("%%", pin);
        final SpannableString styledResultText = new SpannableString(pined);
        int pos = pined.lastIndexOf(pin);
        styledResultText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new StyleSpan(Typeface.BOLD), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new RelativeSizeSpan(2.8f), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mBinding.accountLinkInfo.setText(styledResultText);
        mBinding.accountLinkInfo.requestFocus();

        KeyboardVisibilityManager.hideKeyboard(getActivity());
    }

    private void onClickStart() {
        mBinding.passwordLayout.setError(null);
        String password = mBinding.ringPassword.getText().toString();
        presenter.startAccountExport(password);
    }

    private boolean onPasswordEditorAction(TextView pwd, int actionId, KeyEvent event) {
        Log.i(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (pwd.getText().length() == 0) {
                pwd.setError(getString(R.string.account_enter_password));
            } else {
                onClickStart();
                return true;
            }
        }
        return false;
    }

}