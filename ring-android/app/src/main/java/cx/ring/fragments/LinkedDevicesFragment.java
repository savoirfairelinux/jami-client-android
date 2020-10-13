/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.account.AccountEditionFragment;
import cx.ring.account.ConfirmRevocationDialog;
import cx.ring.account.DeviceAdapter;
import cx.ring.account.LinkedDevicesPresenter;
import cx.ring.account.LinkedDevicesView;
import cx.ring.account.RenameDeviceDialog;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragLinkedDevicesBinding;
import cx.ring.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.services.AccountService;

public class LinkedDevicesFragment extends BaseSupportFragment<LinkedDevicesPresenter> implements
        LinkedDevicesView,
        RenameDeviceDialog.RenameDeviceListener,
        DeviceAdapter.DeviceRevocationListener,
        ConfirmRevocationDialog.ConfirmRevocationListener {

    public static final String TAG = LinkedDevicesFragment.class.getSimpleName();
    private static final String FRAGMENT_DIALOG_REVOCATION = TAG + ".dialog.deviceRevocation";
    private static final String FRAGMENT_DIALOG_RENAME = TAG + ".dialog.deviceRename";

    public static LinkedDevicesFragment newInstance(@NonNull String accountId) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        LinkedDevicesFragment linkedDevicesFragment = new LinkedDevicesFragment();
        linkedDevicesFragment.setArguments(bundle);
        return linkedDevicesFragment;
    }

    private FragLinkedDevicesBinding mBinding;
    private DeviceAdapter mDeviceAdapter;
    private boolean mAccountHasPassword = true;
    private ProgressDialog mWaitDialog;

    @Inject
    AccountService mAccountService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragLinkedDevicesBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();

        String mAccountId = args.getString(AccountEditionFragment.ACCOUNT_ID_KEY);
        Account account = mAccountService.getAccount(mAccountId);
        mAccountHasPassword = account.hasPassword();
        presenter.setAccountId(mAccountId);

        if (mDeviceAdapter == null) {
            mDeviceAdapter = new DeviceAdapter(requireContext(), account.getDevices(), account.getDeviceId(), LinkedDevicesFragment.this);
            mBinding.devicesList.setAdapter(mDeviceAdapter);
        } else {
            mDeviceAdapter.setData(account.getDevices(), account.getDeviceId());
        }
    }

    @Override
    public void accountChanged(Account account) {

    }

    @Override
    public void showPasswordError() {
        dismissWaitDialog();
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
        args.putBoolean(ConfirmRevocationDialog.HAS_PASSWORD_KEY, mAccountHasPassword);
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

    @Override
    public void onDeviceRename(String newName) {
        Log.d(TAG, "onDeviceRename: " + presenter.getDeviceName() + " -> " + newName);
        presenter.renameDevice(newName);
    }

    @Override
    public void showRevokingProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.revoke_device_wait_title),
                getString(R.string.revoke_device_wait_message));
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

    private void dismissWaitDialog() {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }

    @Override
    public void updateDeviceList(final Map<String, String> devices, final String currentDeviceId) {
        if (mDeviceAdapter == null) {
            return;
        }
        mDeviceAdapter.setData(devices, currentDeviceId);
    }

}
