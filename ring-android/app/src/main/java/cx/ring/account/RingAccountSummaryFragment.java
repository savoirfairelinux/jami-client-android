/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
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
import android.widget.LinearLayout;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.services.AccountService;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.views.LinkNewDeviceLayout;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class RingAccountSummaryFragment extends BaseSupportFragment<RingAccountSummaryPresenter> implements BackHandlerInterface,
        RegisterNameDialog.RegisterNameDialogListener,
        RenameDeviceDialog.RenameDeviceListener,
        DeviceAdapter.DeviceRevocationListener,
        ConfirmRevocationDialog.ConfirmRevocationListener,
        RingAccountSummaryView, ChangePasswordDialog.PasswordChangedListener, BackupAccountDialog.UnlockAccountListener {

    public static final String TAG = RingAccountSummaryFragment.class.getSimpleName();
    private static final String FRAGMENT_DIALOG_REVOCATION = TAG + ".dialog.deviceRevocation";
    private static final String FRAGMENT_DIALOG_RENAME = TAG + ".dialog.deviceRename";
    private static final String FRAGMENT_DIALOG_PASSWORD = TAG + ".dialog.changePassword";
    private static final String FRAGMENT_DIALOG_BACKUP = TAG + ".dialog.backup";
    private static final int WRITE_REQUEST_CODE = 43;

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

    @BindView(R.id.layout_account_options)
    LinearLayout mAccountOptionsLayout;

    @BindView(R.id.change_password_btn)
    Button mChangePasswordBtn;

    @BindView(R.id.layout_add_device)
    LinearLayout mAddAccountLayout;

    @BindView(R.id.export_account_btn)
    Button mExportButton;

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
    Chip mAccountStatus;

    /*
    Declarations
    */
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog mWaitDialog;
    private boolean mAccountHasPassword = true;
    private boolean mAccountHasManager = true;
    private String mBestName = "";
    private String mAccountId = "";
    private File mCacheArchive = null;

    @Inject
    AccountService mAccountService;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
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

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                if (uri != null) {
                    if (mCacheArchive != null) {
                        AndroidFileUtils.moveToUri(requireContext().getContentResolver(), mCacheArchive, uri)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {}, e -> {
                                View v = getView();
                                if (v != null)
                                    Snackbar.make(v, "Can't export archive: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                    }
                }
            }
        }
    }

    @Override
    public void accountChanged(@NonNull final Account account) {
        if (mDeviceAdapter == null) {
            mDeviceAdapter = new DeviceAdapter(requireContext(), account.getDevices(), account.getDeviceId(),
                    RingAccountSummaryFragment.this);
            mDeviceList.setAdapter(mDeviceAdapter);
        } else {
            mDeviceAdapter.setData(account.getDevices(), account.getDeviceId());
        }

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
        mAccountHasPassword = account.hasPassword();
        mAccountHasManager = account.hasManager();

        mChangePasswordBtn.setText(mAccountHasPassword ? R.string.account_password_change : R.string.account_password_set);

        mAccountSwitch.setChecked(account.isEnabled());
        mAccountNameTxt.setText(account.getAlias());
        mAccountIdTxt.setText(account.getUsername());
        mAccountId = account.getAccountID();
        mBestName = account.getRegisteredName();
        if (mBestName.isEmpty()) {
            mBestName = account.getAlias();
            if (mBestName.isEmpty()) {
                mBestName = account.getUsername();
            }
        }
        mBestName = mBestName + ".gz";
        String username = account.getRegisteredName();
        boolean currentRegisteredName = account.registeringUsername;
        boolean hasRegisteredName = !currentRegisteredName && username != null && !username.isEmpty();
        registeringNameGroup.setVisibility(currentRegisteredName ? View.VISIBLE : View.GONE);
        mRegisterNameGroup.setVisibility((!hasRegisteredName && !currentRegisteredName) ? View.VISIBLE : View.GONE);
        mRegisteredNameGroup.setVisibility(hasRegisteredName ? View.VISIBLE : View.GONE);
        if (hasRegisteredName) {
            mAccountUsernameTxt.setText(username);
        }

        int color = R.color.red_400;
        String status;

        if (account.isEnabled()) {
            if (account.isTrying()) {
                color = R.color.orange_400;
                status = getString(R.string.account_status_connecting);
            } else if (account.needsMigration()) {
                status = getString(R.string.account_update_needed);
            } else if (account.isInError()) {
                status = getString(R.string.account_status_connection_error);
            } else if (account.isRegistered()) {
                status = getString(R.string.account_status_online);
                color = R.color.green_400;
            } else {
                status = getString(R.string.account_status_unknown);
            }
        } else {
            color = R.color.grey_400;
            status = getString(R.string.account_status_offline);
        }

        mAccountStatus.setText(status);
        mAccountStatus.setChipBackgroundColorResource(color);

        mPasswordLayout.setVisibility(mAccountHasPassword ? View.VISIBLE : View.GONE);
        mAddAccountLayout.setVisibility(mAccountHasManager ? View.GONE : View.VISIBLE);
        mAccountOptionsLayout.setVisibility(mAccountHasManager ? View.GONE : View.VISIBLE);
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
        dismissWaitDialog();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_export_end_network_title)
                .setMessage(R.string.account_export_end_network_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPasswordError() {
        dismissWaitDialog();
        mPasswordLayout.setError(getString(R.string.account_export_end_decryption_message));
        mRingPassword.setText("");
    }

    @Override
    public void showGenericError() {
        dismissWaitDialog();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_export_end_error_title)
                .setMessage(R.string.account_export_end_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public boolean isDisplayingWizard() {
        return mLinkAccountView.getVisibility() == View.VISIBLE;
    }

    @OnEditorAction(R.id.ring_password)
    boolean onPasswordEditorAction(TextView pwd, int actionId, KeyEvent event) {
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
    void onClickStart() {
        mPasswordLayout.setError(null);
        String password = mRingPassword.getText().toString();
        presenter.startAccountExport(password);
    }

    @OnClick(R.id.export_account_btn)
    void onClickExport() {
        if (mAccountHasPassword) {
            onBackupAccount();
        } else {
            onUnlockAccount(mAccountId, "");
        }
    }

    @OnClick(R.id.account_switch)
    void onToggleAccount() {
        presenter.enableAccount(mAccountSwitch.isChecked());
    }

    @OnClick(R.id.register_name_btn)
    void showUsernameRegistrationPopup() {
        Bundle args = new Bundle();
        args.putString(AccountEditionActivity.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY));
        args.putBoolean(AccountEditionActivity.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        RegisterNameDialog registrationDialog = new RegisterNameDialog();
        registrationDialog.setArguments(args);
        registrationDialog.setListener(this);
        registrationDialog.show(requireFragmentManager(), TAG);
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
    public void showPasswordProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.export_account_wait_title),
                getString(R.string.account_password_change_wait_message));
    }

    @Override
    public int getLayout() {
        return R.layout.frag_acc_summary;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    private void dismissWaitDialog() {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }


    @Override
    public void showPIN(final String pin) {
        hideWizard();
        mLinkAccountView.setVisibility(View.VISIBLE);
        mPasswordLayout.setVisibility(View.GONE);
        mEndBtn.setVisibility(View.VISIBLE);
        mStartBtn.setVisibility(View.GONE);
        dismissWaitDialog();
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
    public void passwordChangeEnded(boolean ok) {
        dismissWaitDialog();
        if (!ok) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.account_device_revocation_wrong_password)
                    .setMessage(R.string.account_export_end_decryption_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    @Override
    public void deviceRevocationEnded(final String device, final int status) {
        dismissWaitDialog();
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
        new MaterialAlertDialogBuilder(requireContext())
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

    private void createFile(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    @Override
    public void displayCompleteArchive(File dest)  {
        String type = AndroidFileUtils.getMimeType(dest.getAbsolutePath());
        mCacheArchive = dest;
        dismissWaitDialog();
        createFile(type, mBestName);
    }

    @Override
    public void onConfirmRevocation(String deviceId, String password) {
        presenter.revokeDevice(deviceId, password);
    }

    public void onBackupAccount() {
        BackupAccountDialog dialog = new BackupAccountDialog();
        Bundle args = new Bundle();
        args.putString(AccountEditionActivity.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY));
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_BACKUP);
    }

    @Override
    public void onDeviceRevocationAsked(String deviceId) {
        ConfirmRevocationDialog dialog = new ConfirmRevocationDialog();
        Bundle args = new Bundle();
        args.putString(ConfirmRevocationDialog.DEVICEID_KEY, deviceId);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_REVOCATION);
    }

    @Override
    public void onDeviceRename() {
        final String dev_name = presenter.getDeviceName();
        RenameDeviceDialog dialog = new RenameDeviceDialog();
        Bundle args = new Bundle();
        args.putString(RenameDeviceDialog.DEVICENAME_KEY, dev_name);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_RENAME);
    }

    @OnClick(R.id.change_password_btn)
    public void onPasswordChangeAsked() {
        ChangePasswordDialog dialog = new ChangePasswordDialog();
        Bundle args = new Bundle();
        args.putString(AccountEditionActivity.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY));
        args.putBoolean(AccountEditionActivity.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_PASSWORD);
    }

    @Override
    public void onDeviceRename(String newName) {
        Log.d(TAG, "onDeviceRename: " + presenter.getDeviceName() + " -> " + newName);
        presenter.renameDevice(newName);
    }

    @Override
    public void onPasswordChanged(String oldPassword, String newPassword) {
        presenter.changePassword(oldPassword, newPassword);
    }

    @Override
    public void onUnlockAccount(String accountId, String password) {
        Context context = requireContext();
        File cacheDir = new File(context.getExternalCacheDir(), "archives");
        cacheDir.mkdirs();
        if (!cacheDir.canWrite())
            Log.w(TAG, "Can't write to: " + cacheDir);
        File dest = new File(cacheDir, mBestName);
        if (dest.exists())
            dest.delete();
        presenter.downloadAccountsArchive(dest, password);
    }
}
