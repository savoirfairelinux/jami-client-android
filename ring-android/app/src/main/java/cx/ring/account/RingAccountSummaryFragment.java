/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.account;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.material.textfield.TextInputLayout;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.Account;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.views.LinkNewDeviceLayout;

public class RingAccountSummaryFragment extends BaseSupportFragment<RingAccountSummaryPresenter> implements BackHandlerInterface,
        RegisterNameDialog.RegisterNameDialogListener,
        RenameDeviceDialog.RenameDeviceListener,
        DeviceAdapter.DeviceRevocationListener,
        ConfirmRevocationDialog.ConfirmRevocationListener,
        RingAccountSummaryView {

    public static final String TAG = RingAccountSummaryFragment.class.getSimpleName();
    private static final String FRAGMENT_DIALOG_REVOCATION = RingAccountSummaryFragment.class.getSimpleName() + ".dialog.deviceRevocation";

    /*
    UI Bindings
     */
    @BindView(R.id.linkaccount_container)
    LinkNewDeviceLayout mLinkAccountView;

    @BindView(R.id.ring_password)
    EditText mRingPassword;

    @BindView(R.id.btn_end_export)
    Button mEndBtn;

    @BindView(R.id.btn_start_export)
    Button mStartBtn;

    @BindView(R.id.account_link_info)
    TextView mExportInfos;

    @BindView(R.id.account_alias_txt)
    TextView mAccountNameTxt;

    @BindView(R.id.account_id_txt)
    TextView mAccountIdTxt;

    @BindView(R.id.registered_name_txt)
    TextView mAccountUsernameTxt;

    @BindView(R.id.register_name_btn)
    Button mRegisterNameBtn;

    @BindView(R.id.group_registering_name)
    ViewGroup registeringNameGroup;

    @BindView(R.id.group_register_name)
    ViewGroup mRegisterNameGroup;

    @BindView(R.id.group_registered_name)
    ViewGroup mRegisteredNameGroup;

    @BindView(R.id.device_list)
    ListView mDeviceList;

    @BindView(R.id.password_layout)
    TextInputLayout mPasswordLayout;

    @BindView(R.id.account_switch)
    SwitchCompat mAccountSwitch;

    @BindView(R.id.account_status)
    TextView mAccountStatus;

    /*
    Declarations
    */
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog mWaitDialog;
    private boolean mAccountHasPassword = true;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLinkAccountView.setContainer(this);
        hidePopWizard();
        if (getArguments() != null) {
            String accountId = getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY);
            if (accountId != null) {
                presenter.setAccountId(accountId);
            }
        }
    }

    @Override
    public void accountChanged(final Account account) {
        if (account == null) {
            Log.w(TAG, "No account to display!");
            return;
        }
        mDeviceAdapter = new DeviceAdapter(getActivity(), account.getDevices(), account.getDeviceId(),
                RingAccountSummaryFragment.this);
        mDeviceList.setAdapter(mDeviceAdapter);

        int totalHeight = 0;
        for (int i = 0; i < mDeviceAdapter.getCount(); i++) {
            View listItem = mDeviceAdapter.getView(i, null, mDeviceList);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams par = mDeviceList.getLayoutParams();
        par.height = totalHeight + (mDeviceList.getDividerHeight() * (mDeviceAdapter.getCount() - 1));
        mDeviceList.setLayoutParams(par);
        mDeviceList.requestLayout();

        mAccountSwitch.setChecked(account.isEnabled());
        mAccountNameTxt.setText(account.getAlias());
        mAccountIdTxt.setText(account.getUsername());
        String username = account.getRegisteredName();
        boolean currentRegisteredName = account.registeringUsername;
        boolean hasRegisteredName = !currentRegisteredName && username != null && !username.isEmpty();
        registeringNameGroup.setVisibility(currentRegisteredName ? View.VISIBLE : View.GONE);
        mRegisterNameGroup.setVisibility((!hasRegisteredName && !currentRegisteredName) ? View.VISIBLE : View.GONE);
        mRegisteredNameGroup.setVisibility(hasRegisteredName ? View.VISIBLE : View.GONE);
        if (hasRegisteredName) {
            mAccountUsernameTxt.setText(username);
        }

        int color = ContextCompat.getColor(getContext(), R.color.holo_red_light);
        String status;

        if (account.isEnabled()) {
            if (account.isTrying()) {
                status = getString(R.string.account_status_connecting);
            } else if (account.needsMigration()) {
                status = getString(R.string.account_update_needed);
            } else if (account.isInError()) {
                status = getString(R.string.account_status_connection_error);
            } else if (account.isRegistered()) {
                status = getString(R.string.account_status_online);
                color = ContextCompat.getColor(getContext(), R.color.holo_green_dark);
            } else {
                status = getString(R.string.account_status_unknown);
            }
        } else {
            color = ContextCompat.getColor(getContext(), R.color.darker_gray);
            status = getString(R.string.account_status_offline);
        }

        mAccountStatus.setText(status);
        Drawable wrapped = DrawableCompat.wrap(getContext().getDrawable(R.drawable.static_rounded_background));
        DrawableCompat.setTint(wrapped, color);
        mAccountStatus.setBackground(wrapped);

        mAccountHasPassword = account.getDetailBoolean(ConfigKey.ARCHIVE_HAS_PASSWORD);
        mPasswordLayout.setVisibility(mAccountHasPassword ? View.VISIBLE : View.GONE);
    }

    /*
    BackHandlerInterface
    */
    @Override
    public void onBackPressed() {
        if (isDisplayingWizard()) {
            hideWizard();
        }
    }

    /*
    Add a new device UI management
     */
    @OnClick({R.id.btn_add_device})
    @SuppressWarnings("unused")
    void flipForm() {
        if (!isDisplayingWizard()) {
            showWizard();
        } else {
            hideWizard();
        }
    }

    private void showWizard() {
        mLinkAccountView.setVisibility(View.VISIBLE);
        mPasswordLayout.setVisibility(mAccountHasPassword ? View.VISIBLE : View.GONE);
        mEndBtn.setVisibility(View.GONE);
        mStartBtn.setVisibility(View.VISIBLE);
        mExportInfos.setText(R.string.account_link_export_info);
    }

    @OnClick(R.id.btn_end_export)
    @SuppressWarnings("unused")
    public void hidePopWizard() {
        mLinkAccountView.setVisibility(View.GONE);
    }

    public void hideWizard() {
        mLinkAccountView.setVisibility(View.GONE);
        mRingPassword.setText("");
        KeyboardVisibilityManager.hideKeyboard(getActivity(), 0);
    }

    @Override
    public void showNetworkError() {
        mWaitDialog.dismiss();
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.account_export_end_network_title)
                .setMessage(R.string.account_export_end_network_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPasswordError() {
        mWaitDialog.dismiss();
        mPasswordLayout.setError(getString(R.string.account_export_end_decryption_message));
        mRingPassword.setText("");
    }

    @Override
    public void showGenericError() {
        mWaitDialog.dismiss();
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.account_export_end_error_title)
                .setMessage(R.string.account_export_end_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public boolean isDisplayingWizard() {
        return mLinkAccountView.getVisibility() == View.VISIBLE;
    }

    @OnEditorAction(R.id.ring_password)
    @SuppressWarnings("unused")
    public boolean onPasswordEditorAction(TextView pwd, int actionId, KeyEvent event) {
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

    @OnClick(R.id.btn_start_export)
    public void onClickStart() {
        mPasswordLayout.setError(null);
        String password = mRingPassword.getText().toString();
        presenter.startAccountExport(password);
    }

    @OnClick(R.id.account_switch)
    public void onToggleAccount() {
        presenter.enableAccount(mAccountSwitch.isChecked());
    }

    @OnClick(R.id.register_name_btn)
    public void showUsernameRegistrationPopup() {
        Bundle args = new Bundle();
        args.putString(AccountEditionActivity.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY));
        args.putBoolean(AccountEditionActivity.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        RegisterNameDialog registrationDialog = new RegisterNameDialog();
        registrationDialog.setArguments(args);
        registrationDialog.setListener(this);
        registrationDialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onRegisterName(String name, String password) {
        presenter.registerName(name, password);
    }

    @Override
    public void showExportingProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.export_account_wait_title),
                getString(R.string.export_account_wait_message));
    }

    @Override
    public void showRevokingProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.revoke_device_wait_title),
                getString(R.string.revoke_device_wait_message));
    }

    @Override
    public int getLayout() {
        return R.layout.frag_device_list;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void showPIN(final String pin) {
        hideWizard();
        mWaitDialog.dismiss();
        mLinkAccountView.setVisibility(View.VISIBLE);
        mPasswordLayout.setVisibility(View.GONE);
        mEndBtn.setVisibility(View.VISIBLE);
        mStartBtn.setVisibility(View.GONE);

        String pined = getString(R.string.account_end_export_infos).replace("%%", pin);
        final SpannableString styledResultText = new SpannableString(pined);
        int pos = pined.lastIndexOf(pin);
        styledResultText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new StyleSpan(Typeface.BOLD), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new RelativeSizeSpan(2.8f), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mExportInfos.setText(styledResultText);
        mExportInfos.requestFocus();

        KeyboardVisibilityManager.hideKeyboard(getActivity(), 0);
    }

    @Override
    public void updateDeviceList(final Map<String, String> devices, final String currentDeviceId) {
        if (mDeviceAdapter == null) {
            return;
        }
        mDeviceAdapter.setData(devices, currentDeviceId);
    }

    @Override
    public void deviceRevocationEnded(final String device, final int status) {
        mWaitDialog.dismiss();
        int message, title = R.string.account_device_revocation_error_title;
        switch (status) {
            case 0:
                title = R.string.account_device_revocation_success_title;
                message = R.string.account_device_revocation_success;
                break;
            case 1:
                message = R.string.account_device_revocation_wrong_password;
                break;
            case 2:
                message = R.string.account_device_revocation_unknown_device;
                break;
            default:
                message = R.string.account_device_revocation_error_unknown;
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    if (status == 1) {
                        onDeviceRevocationAsked(device);
                    }
                })
                .show();
    }

    @Override
    public void onConfirmRevocation(String deviceId, String password) {
        presenter.revokeDevice(deviceId, password);
    }

    @Override
    public void onDeviceRevocationAsked(String deviceId) {
        ConfirmRevocationDialog dialog = new ConfirmRevocationDialog();
        Bundle args = new Bundle();
        args.putString(ConfirmRevocationDialog.DEVICEID_KEY, deviceId);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getFragmentManager(), FRAGMENT_DIALOG_REVOCATION);
    }

    @Override
    public void onDeviceRename() {
        final String dev_name = presenter.getDeviceName();
        RenameDeviceDialog dialog = new RenameDeviceDialog();
        Bundle args = new Bundle();
        args.putString(RenameDeviceDialog.DEVICENAME_KEY, dev_name);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onDeviceRename(String newName) {
        Log.d(TAG, "onDeviceRename: " + presenter.getDeviceName() + " -> " + newName);
        presenter.renameDevice(newName);
    }
}
