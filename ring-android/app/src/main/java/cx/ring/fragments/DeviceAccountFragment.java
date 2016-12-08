/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
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
package cx.ring.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.daemon.StringMap;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.Account;
import cx.ring.model.DaemonEvent;
import cx.ring.services.AccountService;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.views.LinkNewDeviceLayout;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;

public class DeviceAccountFragment extends Fragment implements AccountChangedListener,
        BackHandlerInterface,
        RegisterNameDialog.RegisterNameDialogListener,
        Observer<DaemonEvent> {

    private static final String TAG = DeviceAccountFragment.class.getSimpleName();
    private static final int PIN_GENERATION_SUCCESS = 0;
    private static final int PIN_GENERATION_WRONG_PASSWORD = 1;
    private static final int PIN_GENERATION_NETWORK_ERROR = 2;

    @Inject
    AccountService mAccountService;

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
    private AccountCallbacks mCallbacks = DUMMY_CALLBACKS;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog mWaitDialog;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (AccountCallbacks) activity;
        mCallbacks.addOnAccountChanged(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mCallbacks != null) {
            mCallbacks.removeOnAccountChanged(this);
        }
        mCallbacks = DUMMY_CALLBACKS;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAccountService.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAccountService.removeObserver(this);
    }

    @Override
    public void accountChanged(Account account) {
        mDeviceAdapter = new DeviceAdapter(getActivity(), account.getDevices());
        mDeviceList.setAdapter(mDeviceAdapter);

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

    public boolean isDisplayingWizard() {
        return mLinkAccountView.getVisibility() == View.VISIBLE;
    }

    private void showGeneratingResult(String pin) {
        hideWizard();
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
        mRingPassword.setError(null);
        if (mRingPassword.getText().length() == 0) {
            mRingPassword.setError(getString(R.string.account_enter_password));
        } else if (mRingPassword.getText().length() < 6) {
            mRingPassword.setError(getString(R.string.error_password_char_count));
        } else {
            new ExportOnRingTask().execute(mRingPassword.getText().toString());
        }
    }

    @OnClick(R.id.account_switch)
    public void onToggleAccount() {
        if (mCallbacks == null) {
            Log.w(TAG, "Can't toggle account state, callback is null");
            return;
        }
        mCallbacks.getAccount().setEnabled(mAccountSwitch.isChecked());
        mCallbacks.saveAccount();
    }

    @OnClick(R.id.register_name_btn)
    public void showUsernameRegistrationPopup() {
        RegisterNameDialog registrationDialog = new RegisterNameDialog();
        registrationDialog.setListener(this);
        registrationDialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onRegisterName(String name, String password) {
        final Account account = mCallbacks.getAccount();
        mAccountService.registerName(account, password, name);
        accountChanged(account);
    }

    @Override
    public void update(Observable observable, DaemonEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case KNOWN_DEVICES_CHANGED:
                handleKnownDevices(event);
                break;
            case NAME_REGISTRATION_ENDED:
                accountChanged(mCallbacks.getAccount());
                break;
            case EXPORT_ON_RING_ENDED:
                handleExportEnded(event);
                break;
            default:
                Log.d(TAG, "This event " + event.getEventType() + " is not handled here");
                break;
        }
    }

    private void handleExportEnded(DaemonEvent event) {

        String accountId = event.getEventInput(DaemonEvent.EventInput.ACCOUNT_ID, String.class);
        Account currentAccount = mCallbacks.getAccount();
        if (currentAccount != null && currentAccount.getAccountID().equals(accountId)) {
            final int code = event.getEventInput(DaemonEvent.EventInput.CODE, Integer.class);
            final String pin = event.getEventInput(DaemonEvent.EventInput.PIN, String.class);
            if (mDeviceAdapter != null) {
                RingApplication.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWaitDialog.dismiss();
                        if (code == PIN_GENERATION_SUCCESS) {
                            showGeneratingResult(pin);
                            return;
                        }
                        AlertDialog.Builder errorDialog = new AlertDialog.Builder(getActivity());
                        switch (code) {
                            case PIN_GENERATION_WRONG_PASSWORD:
                                mRingPassword.setError(getString(R.string.account_export_end_decryption_message));
                                mRingPassword.setText("");
                                return;
                            case PIN_GENERATION_NETWORK_ERROR:
                                errorDialog.setTitle(R.string.account_export_end_network_title)
                                        .setMessage(R.string.account_export_end_network_message);
                                errorDialog.setPositiveButton(android.R.string.ok, null);
                                break;
                            default:
                                errorDialog.setTitle(R.string.account_export_end_error_title)
                                        .setMessage(R.string.account_export_end_error_message);
                                errorDialog.setPositiveButton(android.R.string.ok, null);
                                break;
                        }
                        errorDialog.show();
                    }
                });
            }
        }
    }

    private void handleKnownDevices(DaemonEvent event) {
        String accountId = event.getEventInput(DaemonEvent.EventInput.ACCOUNT_ID, String.class);
        Account currentAccount = mCallbacks.getAccount();
        if (currentAccount != null && currentAccount.getAccountID().equals(accountId)) {
            final StringMap devices = event.getEventInput(DaemonEvent.EventInput.DEVICES, StringMap.class);
            if (mDeviceAdapter != null) {
                RingApplication.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceAdapter.setData(devices.toNative());
                    }
                });
            }
        }
    }

    class DeviceAdapter extends BaseAdapter {
        private final Context mCtx;
        private final ArrayList<Map.Entry<String, String>> devices = new ArrayList<>();

        DeviceAdapter(Context c, Map<String, String> devs) {
            mCtx = c;
            setData(devs);
        }

        void setData(Map<String, String> devs) {
            devices.clear();
            if (devs != null && !devs.isEmpty()) {
                devices.ensureCapacity(devs.size());
                for (Map.Entry<String, String> e : devs.entrySet()) {
                    devices.add(e);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(mCtx).inflate(R.layout.item_device, parent, false);
            }

            TextView devId = (TextView) view.findViewById(R.id.txt_device_id);
            devId.setText(devices.get(i).getKey());

            TextView devName = (TextView) view.findViewById(R.id.txt_device_label);
            devName.setText(devices.get(i).getValue());

            return view;
        }
    }

    private class ExportOnRingTask extends AsyncTask<String, Void, String> {
        private final Account account = mCallbacks.getAccount();

        @Override
        protected void onPreExecute() {
            mWaitDialog = ProgressDialog.show(getActivity(),
                    getString(R.string.export_account_wait_title),
                    getString(R.string.export_account_wait_message));

        }

        @Override
        protected String doInBackground(String... params) {
            return mAccountService.exportOnRing(account.getAccountID(), params[0]);
        }
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

        Account account = mCallbacks.getAccount();
        if (account != null) {
            accountChanged(account);
        }

        mLinkAccountView.setContainer(this);
        hidePopWizard();

        return devLayout;
    }
}
