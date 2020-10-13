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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.account.AccountEditionFragment;
import cx.ring.account.JamiAccountSummaryFragment;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.daemon.Ringservice;
import cx.ring.databinding.FragAccountBinding;
import cx.ring.model.Account;
import cx.ring.model.Settings;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.GenericView;
import cx.ring.services.AccountService;

import static cx.ring.daemon.Ringservice.getPluginsEnabled;
import static cx.ring.daemon.Ringservice.setPluginsEnabled;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
public class AccountFragment extends Fragment implements ViewTreeObserver.OnScrollChangedListener {

    private static final int SCROLL_DIRECTION_UP = -1;

    public static AccountFragment newInstance(@NonNull String accountId) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        AccountFragment accountFragment = new AccountFragment();
        accountFragment.setArguments(bundle);
        return accountFragment;
    }

    private FragAccountBinding mBinding;

    @Inject
    AccountService mAccountService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccountBinding.inflate(inflater, container, false);
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
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        String accountId = getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY);
        Account account = mAccountService.getAccount(accountId);

        mBinding.settingsChangePassword.setVisibility(account.hasManager() ? View.GONE : View.VISIBLE);
        mBinding.settingsExport.setVisibility(account.hasManager() ? View.GONE : View.VISIBLE);
        mBinding.systemChangePasswordTitle.setText(account.hasPassword()? R.string.account_password_change : R.string.account_password_set);

        mBinding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);

        mBinding.settingsChangePassword.setOnClickListener(v -> ((JamiAccountSummaryFragment) getParentFragment()).onPasswordChangeAsked());
        mBinding.settingsExport.setOnClickListener(v -> ((JamiAccountSummaryFragment) getParentFragment()).onClickExport());

        mBinding.settingsBlackList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JamiAccountSummaryFragment summaryFragment = ((JamiAccountSummaryFragment) AccountFragment.this.getParentFragment());
                if (summaryFragment != null) {
                    summaryFragment.goToBlackList(accountId);
                }
            }
        });
    }

    @Override
    public void onScrollChanged() {
        if (mBinding != null) {
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).setToolbarElevation(mBinding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

}
