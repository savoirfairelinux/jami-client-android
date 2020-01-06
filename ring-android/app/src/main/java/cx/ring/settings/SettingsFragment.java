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
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.account.RingAccountSummaryFragment;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.model.Settings;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.GenericView;
import cx.ring.utils.DeviceUtils;
import cx.ring.views.BoundedScrollView;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
public class SettingsFragment extends BaseSupportFragment<SettingsPresenter> implements GenericView<Settings>, ViewTreeObserver.OnScrollChangedListener {

    private static final int SCROLL_DIRECTION_UP = -1;

    @BindView(R.id.settings_push_notifications_layout)
    ViewGroup mGroupPushNotifications;
    @BindView(R.id.settings_push_notifications)
    Switch mViewPushNotifications;
    @BindView(R.id.settings_startup)
    Switch mViewStartup;
    @BindView(R.id.settings_persistNotification)
    Switch mViewPersistNotif;
    @BindView(R.id.settings_video_layout)
    View settings_video_layout;
    @BindView(R.id.settings_dark_theme)
    Switch mDarkTheme;
    @BindView(R.id.scrollview)
    BoundedScrollView mScrollView;

    private boolean mIsRefreshingViewFromPresenter;

    @Override
    public int getLayout() {
        return R.layout.frag_settings;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);
        mDarkTheme.setChecked(presenter.getDarkMode());
        if (TextUtils.isEmpty(JamiApplication.getInstance().getPushToken())) {
            mGroupPushNotifications.setVisibility(View.GONE);
        }
        // loading preferences
        presenter.loadSettings();
        if (DeviceUtils.isTablet(getContext())) {
            Toolbar toolbar = getActivity().findViewById(R.id.main_toolbar);
            TextView title = toolbar.findViewById(R.id.contact_title);
            ImageView logo = toolbar.findViewById(R.id.contact_image);

            logo.setVisibility(View.GONE);
            title.setText(R.string.menu_item_settings);
            title.setTextSize(19);
            title.setTypeface(null, Typeface.BOLD);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) title.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_TOP);
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            title.setLayoutParams(params);
        } else {
            ((HomeActivity) getActivity()).setToolbarState(R.string.menu_item_settings);
        }

        mScrollView.getViewTreeObserver().addOnScrollChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG);
        if (existingFragment == null) {
            ((HomeActivity) getActivity()).goToSettings();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @OnCheckedChanged(R.id.settings_dark_theme)
    public void onDarkThemeChanged(CompoundButton button, boolean isChecked) {
        presenter.setDarkMode(isChecked);
    }

    @OnCheckedChanged({
        R.id.settings_push_notifications,
        R.id.settings_startup,
        R.id.settings_persistNotification
    })
    public void onSettingsCheckedChanged(CompoundButton button, boolean isChecked) {
        if (!mIsRefreshingViewFromPresenter)
            saveSettings();
    }

    private void saveSettings() {
        Settings newSettings = new Settings();

        newSettings.setAllowRingOnStartup(mViewStartup.isChecked());
        newSettings.setAllowPushNotifications(mViewPushNotifications.isChecked());
        newSettings.setAllowPersistentNotification(mViewPersistNotif.isChecked());

        // save settings according to UI inputs
        presenter.saveSettings(newSettings);
    }

    @OnClick(R.id.settings_video_layout)
    void onVideoClick() {
        HomeActivity activity = (HomeActivity) getActivity();
        if (activity != null) {
            activity.goToVideoSettings();
        }
    }

    @OnClick(R.id.settings_clear_history)
    public void onClearHistoryClick() {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(getString(R.string.clear_history_dialog_title))
                .setMessage(getString(R.string.clear_history_dialog_message))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    // ask the presenter to clear history
                    presenter.clearHistory();
                    Snackbar.make(getView(),
                            getString(R.string.clear_history_completed),
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    //~ Empty
                })
                .show();
    }

    /**
     * Presents a Toast explaining why the Read Contacts permission is required to display the devi-
     * ces contacts in Ring.
     */
    private void presentReadContactPermissionExplanationToast() {
        Activity activity = getActivity();
        if (null != activity) {
            String toastMessage = getString(R.string.permission_dialog_read_contacts_message);
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Presents a Toast explaining why the Write Call Log permission is required to enable the cor-
     * responding feature.
     */
    private void presentWriteCallLogPermissionExplanationToast() {
        Activity activity = getActivity();
        if (null != activity) {
            String toastMessage = getString(R.string.permission_dialog_write_call_log_message);
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void showViewModel(Settings viewModel) {
        mIsRefreshingViewFromPresenter = true;
        mViewPushNotifications.setChecked(viewModel.isAllowPushNotifications());
        mViewPersistNotif.setChecked(viewModel.isAllowPersistentNotification());
        mViewStartup.setChecked(viewModel.isAllowRingOnStartup());
        mIsRefreshingViewFromPresenter = false;
    }

    @Override
    public void onScrollChanged() {
        if (mScrollView != null) {
            ((HomeActivity) getActivity()).setToolbarElevation(mScrollView.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

}
