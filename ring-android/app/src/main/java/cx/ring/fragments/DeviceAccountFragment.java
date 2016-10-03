/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import cx.ring.R;
import cx.ring.client.AccountCallbacks;
import cx.ring.model.account.Account;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;

public class DeviceAccountFragment extends Fragment {

    private static final String TAG = DeviceAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";

    private AccountCallbacks mCallbacks = DUMMY_CALLBACKS;
    private DeviceAdapter adapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (AccountCallbacks) activity;
        Account acc =  mCallbacks.getAccount();
        if (acc != null)
           acc.devicesListener = new Account.OnDevicesChangedListener() {
                @Override
                public void devicesChanged(Map<String, String> devices) {
                    if (adapter != null) {
                        adapter.setData(devices);
                    }
                }
            };
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mCallbacks != null) {
            Account acc = mCallbacks.getAccount();
            acc.devicesListener = null;
        }
        mCallbacks = DUMMY_CALLBACKS;
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
                for (Map.Entry<String, String> e : devs.entrySet())
                    devices.add(e);
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
        private final Account acc = mCallbacks.getAccount();

        @Override
        protected void onPreExecute() {
            final ProgressDialog wait = ProgressDialog.show(getActivity(), "Please wait...", "Publishing new account information");
            acc.exportListener = new Account.OnExportEndedListener() {
                @Override
                public void exportEnded(int code, String pin) {
                    acc.exportListener = null;
                    wait.dismiss();
                    AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                    switch (code) {
                        case 0:
                            b.setTitle("PIN : " + pin)
                                    .setMessage("Use this PIN along with your main account password to associate your new device to your Ring account during the next 10 minutes.");
                            break;
                        case 1 :
                            b.setTitle("Decryption error")
                                    .setMessage("Couldn't unlock your account using the provided password.");
                            break;
                        case 2 :
                            b.setTitle("Network error")
                                    .setMessage("Couldn't export account on the network. Check your connectivity.");
                            break;
                        default:
                            b.setTitle("Error")
                                    .setMessage("Couldn't export account. An unknown error occurred.");
                    }
                    b.show();
                }
            };
        }

        @Override
        protected String doInBackground(String... params) {
            String pin = null;
            try {
                pin = mCallbacks.getRemoteService().exportOnRing(acc.getAccountID(), params[0]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return pin;
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;

        Account acc = mCallbacks.getAccount();
        if (acc == null) return null;

        ViewGroup dev_layout = (ViewGroup) inflater.inflate(R.layout.frag_device_list, container, false);

        FloatingActionButton newDevBtn = (FloatingActionButton) dev_layout.findViewById(R.id.btn_add_device);
        newDevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final ViewGroup v = (ViewGroup) inflater.inflate(R.layout.dialog_new_device, null);
                final TextView pwd = (TextView) v.findViewById(R.id.pwd_txt);
                builder.setMessage(R.string.account_new_device_message)
                        .setTitle(R.string.account_new_device)
                        .setPositiveButton(R.string.account_export, null)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).setView(v);
                final AlertDialog alertDialog = builder.create();
                alertDialog.show();
                pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            if (pwd.getText().length() == 0) {
                                pwd.setError("Enter password");
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
                            pwd.setError("Enter password");
                        } else {
                            alertDialog.dismiss();
                            new ExportOnRingTask().execute(pwd.getText().toString());
                        }
                    }
                });

            }
        });

        adapter = new DeviceAdapter(getActivity(), acc.getDevices());
        ListView deviceList = (ListView) dev_layout.findViewById(R.id.device_list);
        deviceList.setAdapter(adapter);
        return dev_layout;
    }

}
