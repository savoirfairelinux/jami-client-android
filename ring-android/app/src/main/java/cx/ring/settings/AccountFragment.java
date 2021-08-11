/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author:     AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.account.AccountEditionFragment;
import cx.ring.account.JamiAccountSummaryFragment;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragAccountBinding;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import net.jami.services.AccountService;

@AndroidEntryPoint
public class AccountFragment extends Fragment implements ViewTreeObserver.OnScrollChangedListener {
    public static final String TAG = AccountFragment.class.getSimpleName();
    private static final int SCROLL_DIRECTION_UP = -1;

    public static AccountFragment newInstance(@NonNull String accountId) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        AccountFragment accountFragment = new AccountFragment();
        accountFragment.setArguments(bundle);
        return accountFragment;
    }

    private FragAccountBinding mBinding;
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Inject
    AccountService mAccountService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccountBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
        mDisposable.clear();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mBinding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        mBinding.settingsChangePassword.setOnClickListener(v -> ((JamiAccountSummaryFragment) getParentFragment()).onPasswordChangeAsked());
        mBinding.settingsExport.setOnClickListener(v -> ((JamiAccountSummaryFragment) getParentFragment()).onClickExport());

        String accountId = getArguments() != null ? getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY) : null;
        mDisposable.add(mAccountService.getAccountSingle(accountId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(account -> {
                    mBinding.settingsChangePassword.setVisibility(account.hasManager() ? View.GONE : View.VISIBLE);
                    mBinding.settingsExport.setVisibility(account.hasManager() ? View.GONE : View.VISIBLE);
                    mBinding.systemChangePasswordTitle.setText(account.hasPassword()? R.string.account_password_change : R.string.account_password_set);
                    mBinding.settingsDeleteAccount.setOnClickListener(v -> {
                        AlertDialog deleteDialog = createDeleteDialog(account.getAccountID());
                        deleteDialog.show();
                    });
                    mBinding.settingsBlackList.setOnClickListener(v -> {
                        JamiAccountSummaryFragment summaryFragment = ((JamiAccountSummaryFragment) getParentFragment());
                        if (summaryFragment != null) {
                            summaryFragment.goToBlackList(account.getAccountID());
                        }
                    });
                }, e -> {
                    JamiAccountSummaryFragment summaryFragment = ((JamiAccountSummaryFragment) getParentFragment());
                    if (summaryFragment != null) {
                        summaryFragment.popBackStack();
                    }
                }));
    }

    @Override
    public void onScrollChanged() {
        if (mBinding != null) {
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).setToolbarElevation(mBinding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

    @NonNull
    private AlertDialog createDeleteDialog(String accountId) {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.account_delete_dialog_message)
                .setTitle(R.string.account_delete_dialog_title)
                .setPositiveButton(R.string.menu_delete, (dialog, whichButton) -> mAccountService.removeAccount(accountId))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        Activity activity = getActivity();
        if (activity != null)
            alertDialog.setOwnerActivity(getActivity());
        return alertDialog;
    }

}
