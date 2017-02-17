/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.SwitchCompat;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountEditionActivity;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.Account;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.views.LinkNewDeviceLayout;

public class RingAccountSummaryFragment extends Fragment implements BackHandlerInterface,
        RegisterNameDialog.RegisterNameDialogListener,
        RenameDeviceDialog.RenameDeviceListener,
        DeviceAdapter.DeviceRevocationListener,
        ConfirmRevocationDialog.ConfirmRevocationListener,
        RingAccountSummaryView {

    private static final String TAG = RingAccountSummaryFragment.class.getSimpleName();

    @Inject
    RingAccountSummaryPresenter mRingAccountSummaryPresenter;

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

    @Override
    public void onResume() {
        super.onResume();
        mRingAccountSummaryPresenter.bindView(this);

        if (getArguments() == null || getArguments().getString(AccountEditionActivity.ACCOUNTID_KEY) == null) {
            return;
        }
        mRingAccountSummaryPresenter.setAccountId(getArguments().getString(AccountEditionActivity.ACCOUNTID_KEY));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // view unbinding
        mRingAccountSummaryPresenter.unbindView();
    }

    @Override
    public void accountChanged(final Account account) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
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

                int color = ContextCompat.getColor(getActivity(), R.color.holo_red_light);
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
                        color = ContextCompat.getColor(getActivity(), R.color.holo_green_dark);
                    } else {
                        status = getString(R.string.account_status_unknown);
                    }
                } else {
                    color = ContextCompat.getColor(getActivity(), R.color.darker_gray);
                    status = getString(R.string.account_status_offline);
                }

                mAccountStatus.setText(status);
                Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.static_rounded_background);
                Drawable wrapped = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(wrapped, color);
                mAccountStatus.setBackground(wrapped);
            }
        });
    }

    /*
    BackHandlerInterface
    */
    @Override
    public boolean onBackPressed() {
        if (isDisplayingWizard()) {
            hideWizard();
            return true;
        }
        return false;
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
        mPasswordLayout.setVisibility(View.VISIBLE);
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
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mWaitDialog.dismiss();
                AlertDialog.Builder errorDialog = new AlertDialog.Builder(getActivity());
                errorDialog.setTitle(R.string.account_export_end_network_title)
                        .setMessage(R.string.account_export_end_network_message);
                errorDialog.setPositiveButton(android.R.string.ok, null);
                errorDialog.show();
            }
        });
    }

    @Override
    public void showPasswordError() {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mWaitDialog.dismiss();
                mPasswordLayout.setError(getString(R.string.account_export_end_decryption_message));
                mRingPassword.setText("");
            }
        });
    }

    @Override
    public void showGenericError() {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mWaitDialog.dismiss();
                AlertDialog.Builder errorDialog = new AlertDialog.Builder(getActivity());
                errorDialog.setTitle(R.string.account_export_end_error_title)
                        .setMessage(R.string.account_export_end_error_message);
                errorDialog.setPositiveButton(android.R.string.ok, null);
                errorDialog.show();
            }
        });
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
        if (mRingPassword.getText().length() == 0) {
            mPasswordLayout.setError(getString(R.string.account_enter_password));
        } else if (mRingPassword.getText().length() < 6) {
            mPasswordLayout.setError(getString(R.string.error_password_char_count));
        } else {
            mRingAccountSummaryPresenter.startAccountExport(mRingPassword.getText().toString());
        }
    }

    @OnClick(R.id.account_switch)
    public void onToggleAccount() {
        mRingAccountSummaryPresenter.enableAccount(mAccountSwitch.isChecked());
    }

    @OnClick(R.id.register_name_btn)
    public void showUsernameRegistrationPopup() {
        RegisterNameDialog registrationDialog = new RegisterNameDialog();
        registrationDialog.setListener(this);
        registrationDialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onRegisterName(String name, String password) {
        mRingAccountSummaryPresenter.registerName(name, password);
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

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        ViewGroup devLayout = (ViewGroup) inflater.inflate(R.layout.frag_device_list, container, false);

        ButterKnife.bind(this, devLayout);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        mLinkAccountView.setContainer(this);
        hidePopWizard();

        return devLayout;
    }

    @Override
    public void showPIN(final String pin) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Override
    public void updateDeviceList(final Map<String, String> devices, final String currentDeviceId) {
        if (mDeviceAdapter == null) {
            return;
        }
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mDeviceAdapter.setData(devices, currentDeviceId);
            }
        });
    }

    @Override
    public void onConfirmRevocation(String deviceId, String password) {
        mRingAccountSummaryPresenter.revokeDevice(deviceId, password);
    }

    @Override
    public void onDeviceRevocationAsked(String deviceId) {
        ConfirmRevocationDialog dialog = new ConfirmRevocationDialog();
        Bundle args = new Bundle();
        args.putString(ConfirmRevocationDialog.DEVICEID_KEY, deviceId);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onDeviceRename() {
        final String dev_name = mRingAccountSummaryPresenter.getDeviceName();
        Log.w(TAG, "onDeviceRename " + dev_name);
        RenameDeviceDialog dialog = new RenameDeviceDialog();
        Bundle args = new Bundle();
        args.putString(RenameDeviceDialog.DEVICENAME_KEY, dev_name);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onDeviceRename(String newName) {
        Log.w(TAG, "onDeviceRename " + mRingAccountSummaryPresenter.getDeviceName() + " -> " + newName);
        mRingAccountSummaryPresenter.renameDevice(newName);
    }
}
