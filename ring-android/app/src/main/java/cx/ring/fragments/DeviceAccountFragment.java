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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.client.AccountEditionActivity;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.model.account.Account;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.views.LinkNewDeviceLayout;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;

public class DeviceAccountFragment extends Fragment implements AccountChangedListener, BackHandlerInterface {

    private static final String TAG = DeviceAccountFragment.class.getSimpleName();

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

    @BindView(R.id.account_edit_btn)
    View editBtn;

    @BindView(R.id.account_alias_txt)
    TextView accNameTxt;

    @BindView(R.id.account_id_txt)
    TextView accIdTxt;

    @BindView(R.id.registred_name_txt)
    TextView accUsernameTxt;

    @BindView(R.id.register_name_btn)
    Button registerNameBtn;

    @BindView(R.id.group_registering_name)
    ViewGroup registeringNameGroup;

    @BindView(R.id.group_register_name)
    ViewGroup registerNameGroup;

    @BindView(R.id.group_registered_name)
    ViewGroup registeredNameGroup;

    private DeviceAdapter adapter;

    @BindView(R.id.device_list)
    ListView mDeviceList;

    @BindView(R.id.password_layout)
    TextInputLayout mPasswordLayout;
    /*
    Declarations
    */
    private AccountCallbacks mCallbacks = DUMMY_CALLBACKS;
    private DeviceAdapter mDeviceAdapter;

    /*
    Fragment LifeCycle
     */
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup devLayout = (ViewGroup) inflater.inflate(R.layout.frag_device_list, container, false);

        ButterKnife.bind(this, devLayout);

        Account account = mCallbacks.getAccount();
        if (account != null) {
            accountChanged(account);
        }
        mLinkAccountView.setContainer(this);
        hidePopWizard();
        return devLayout;
    }

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
            Account account = mCallbacks.getAccount();
            account.devicesListener = null;
        }
        mCallbacks = DUMMY_CALLBACKS;
    }

    @Override
    public void accountChanged(Account account) {
        mDeviceAdapter = new DeviceAdapter(getActivity(), account.getDevices());
        mDeviceList.setAdapter(mDeviceAdapter);

        accNameTxt.setText(account.getAlias());
        accIdTxt.setText(account.getUsername());
        String username = account.getRegisteredName();
        boolean cur_reg_name = account.registeringUsername;
        boolean has_reg_name = !cur_reg_name && username != null && !username.isEmpty();
        registeringNameGroup.setVisibility(cur_reg_name ? View.VISIBLE : View.GONE);
        registerNameGroup.setVisibility((!has_reg_name && !cur_reg_name) ? View.VISIBLE : View.GONE);
        registeredNameGroup.setVisibility(has_reg_name ? View.VISIBLE : View.GONE);
        if (has_reg_name) {
            accUsernameTxt.setText(username);
        }
        account.devicesListener = new Account.OnDevicesChangedListener() {
            @Override
            public void devicesChanged(Map<String, String> devices) {
                if (mDeviceAdapter != null) {
                    mDeviceAdapter.setData(devices);
                }
            }
        };
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
            final ProgressDialog wait = ProgressDialog.show(getActivity(),
                    getString(R.string.export_account_wait_title),
                    getString(R.string.export_account_wait_message));

            account.exportListener = new Account.OnExportEndedListener() {
                final static int PIN_GENERATION_SUCCESS = 0;
                final static int PIN_GENERATION_WRONG_PASSWORD = 1;
                final static int PIN_GENERATION_NETWORK_ERROR = 2;

                @Override
                public void exportEnded(int code, String pin) {
                    account.exportListener = null;
                    wait.dismiss();
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
            };
        }

        @Override
        protected String doInBackground(String... params) {
            String pin = null;
            try {
                pin = mCallbacks.getRemoteService().exportOnRing(account.getAccountID(), params[0]);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while exporting in background", e);
            }
            return pin;
        }
    }

<<<<<<< HEAD
=======
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;
        ViewGroup devLayout = (ViewGroup) inflater.inflate(R.layout.frag_device_list, container, false);

        ButterKnife.bind(this, devLayout);

        Account acc = mCallbacks.getAccount();
        if (acc != null) {
            accountChanged(acc);
        }

        return devLayout;
    }

    @OnClick(R.id.account_edit_btn)
    public void editAccount() {
        ((AccountEditionActivity)getActivity()).editAdvanced();
    }

    @OnClick(R.id.btn_add_device)
    @SuppressWarnings("unused")
    public void addDevice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final ViewGroup v = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_new_device, null);
        final TextView pwd = (TextView) v.findViewById(R.id.pwd_txt);
        builder.setMessage(R.string.account_new_device_message)
                .setTitle(R.string.account_new_device)
                .setPositiveButton(R.string.account_export, null)
                .setNegativeButton(android.R.string.cancel, null).setView(v);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.i(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (pwd.getText().length() == 0) {
                        pwd.setError(getString(R.string.account_enter_password));
                    } else {
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
                        return true;
                    }
                }
                return false;
            }
        });
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pwd.setError(null);
                if (pwd.getText().length() == 0) {
                    pwd.setError(getString(R.string.account_enter_password));
                } else {
                    alertDialog.dismiss();
                    new ExportOnRingTask().execute(pwd.getText().toString());
                }
            }
        });
    }

>>>>>>> 0c6948f... ui: add name registration
}
