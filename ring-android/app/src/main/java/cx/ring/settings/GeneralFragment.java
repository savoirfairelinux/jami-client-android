/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author:     Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.Toast;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.account.AccountEditionFragment;
import cx.ring.account.JamiAccountSummaryFragment;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.daemon.Ringservice;
import cx.ring.databinding.FragSettingsBinding;
import cx.ring.model.Account;
import cx.ring.model.Settings;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.GenericView;
import cx.ring.services.AccountService;

public class GeneralFragment extends BaseSupportFragment<SettingsPresenter> implements GenericView<Settings>, ViewTreeObserver.OnScrollChangedListener {

    private static final int SCROLL_DIRECTION_UP = -1;

    public static GeneralFragment newInstance(@NonNull String accountId) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        GeneralFragment generalFragment = new GeneralFragment();
        generalFragment.setArguments(bundle);
        return generalFragment;
    }

    private FragSettingsBinding mBinding;
    private Settings currentSettings = null;

    private boolean mIsRefreshingViewFromPresenter = true;

    @Inject
    AccountService mAccountService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragSettingsBinding.inflate(inflater, container, false);
        ((JamiApplication) requireActivity().getApplication()).getInjectionComponent().inject(this);
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

        mBinding.settingsDarkTheme.setChecked(presenter.getDarkMode());
        mBinding.settingsPluginsSwitch.setChecked(Ringservice.getPluginsEnabled());
        mBinding.settingsChangePassword.setVisibility(account.hasManager() ? View.GONE : View.VISIBLE);
        mBinding.settingsExport.setVisibility(account.hasManager() ? View.GONE : View.VISIBLE);
        mBinding.systemChangePasswordTitle.setText(account.hasPassword()? R.string.account_password_change : R.string.account_password_set);
        if (TextUtils.isEmpty(JamiApplication.getInstance().getPushToken())) {
            mBinding.settingsPushNotificationsLayout.setVisibility(View.GONE);
        }
        // loading preferences
        presenter.loadSettings();

        mBinding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        mBinding.settingsDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> presenter.setDarkMode(isChecked));
        mBinding.settingsPluginsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Ringservice.setPluginsEnabled(isChecked));

        CompoundButton.OnCheckedChangeListener save = (buttonView, isChecked) -> {
            if (!mIsRefreshingViewFromPresenter)
                saveSettings();
        };
        mBinding.settingsPushNotifications.setOnCheckedChangeListener(save);
        mBinding.settingsStartup.setOnCheckedChangeListener(save);
        mBinding.settingsPersistNotification.setOnCheckedChangeListener(save);
        mBinding.settingsChangePassword.setOnClickListener(v -> ((JamiAccountSummaryFragment) getParentFragment()).onPasswordChangeAsked());
        mBinding.settingsExport.setOnClickListener(v -> ((JamiAccountSummaryFragment) getParentFragment()).onClickExport());

        mBinding.settingsVideoLayout.setOnClickListener(v -> {
            JamiAccountSummaryFragment summaryFragment = ((JamiAccountSummaryFragment) GeneralFragment.this.getParentFragment());
            if (summaryFragment != null) {
                summaryFragment.goToVideoSettings();
            }
        });

        mBinding.settingsClearHistory.setOnClickListener(v -> new MaterialAlertDialogBuilder(view.getContext())
                .setTitle(getString(R.string.clear_history_dialog_title))
                .setMessage(getString(R.string.clear_history_dialog_message))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    // ask the presenter to clear history
                    presenter.clearHistory();
                    Snackbar.make(view,
                            getString(R.string.clear_history_completed),
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    //~ Empty
                })
                .show());
        mBinding.settingsPluginsLayout.setOnClickListener(v -> {
            if (Ringservice.getPluginsEnabled()){
                JamiAccountSummaryFragment summaryFragment = ((JamiAccountSummaryFragment) GeneralFragment.this.getParentFragment());
                if (summaryFragment != null) {
                    summaryFragment.goToPluginsListSettings();
                }
            }
        });

        mBinding.settingsBlackList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JamiAccountSummaryFragment summaryFragment = ((JamiAccountSummaryFragment) GeneralFragment.this.getParentFragment());
                if (summaryFragment != null) {
                    summaryFragment.goToBlackList(accountId);
                }
            }
        });
    }

    private void saveSettings() {
        Settings newSettings = new Settings(currentSettings);
        newSettings.setAllowRingOnStartup(mBinding.settingsStartup.isChecked());
        newSettings.setAllowPushNotifications(mBinding.settingsPushNotifications.isChecked());
        newSettings.setAllowPersistentNotification(mBinding.settingsPersistNotification.isChecked());

        // save settings according to UI inputs
        presenter.saveSettings(newSettings);
    }

    private void presentReadContactPermissionExplanationToast() {
        Toast.makeText(requireContext(), getString(R.string.permission_dialog_read_contacts_message), Toast.LENGTH_LONG).show();
    }

    private void presentWriteCallLogPermissionExplanationToast() {
        Toast.makeText(requireContext(), getString(R.string.permission_dialog_write_call_log_message), Toast.LENGTH_LONG).show();
    }

    @Override
    public void showViewModel(Settings viewModel) {
        currentSettings = viewModel;
        mIsRefreshingViewFromPresenter = true;
        mBinding.settingsPushNotifications.setChecked(viewModel.isAllowPushNotifications());
        mBinding.settingsPersistNotification.setChecked(viewModel.isAllowPersistentNotification());
        mBinding.settingsStartup.setChecked(viewModel.isAllowOnStartup());
        mIsRefreshingViewFromPresenter = false;
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
