/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.client;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.fragments.AccountCreationFragment;
import cx.ring.fragments.AccountMigrationFragment;
import cx.ring.fragments.RingAccountCreationFragment;
import cx.ring.fragments.RingAccountLoginFragment;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

public class AccountWizard extends AppCompatActivity implements LocalService.Callbacks {
    static final String TAG = AccountWizard.class.getName();
    private boolean mBound = false;
    private LocalService mService;
    private boolean mCreatingAccount = false;

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = ((LocalService.LocalBinder) binder).getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // nothing to be done here
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        ringChoose();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged " + newConfig);
        super.onConfigurationChanged(newConfig);
    }

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                checkAccountPresence();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Ensures that the user has at least one account when exiting this Activity
     * If not, exit the app
     */
    private void checkAccountPresence() {
        if (mBound && !mService.getAccounts().isEmpty()) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() >= 1) {
                fm.popBackStack();
            } else {
                finish();
            }
        } else {
            mService.stopSelf();
            finishAffinity();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() >= 1) {
            fm.popBackStack();
            return;
        }
        super.onBackPressed();
    }

    @SuppressWarnings("unchecked")
    public void initAccountCreation(boolean isRingAccount, String newUsername, String pin, String password, String host) {
        final String accountType = isRingAccount ? AccountConfig.ACCOUNT_TYPE_RING : AccountConfig.ACCOUNT_TYPE_SIP;

        try {
            HashMap<String, String> accountDetails = (HashMap<String, String>) mService.getRemoteService().getAccountTemplate(accountType);
            for (Map.Entry<String, String> e : accountDetails.entrySet()) {
                Log.d(TAG, "Default account detail: " + e.getKey() + " -> " + e.getValue());
            }
            //~ Checking the state of the Camera permission to enable Video or not.
            boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            accountDetails.put(ConfigKey.VIDEO_ENABLED.key(), Boolean.toString(hasCameraPermission));

            //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
            //~ This will have to be removed when it will be supported.
            accountDetails.put(ConfigKey.ACCOUNT_DTMF_TYPE.key(), getString(R.string.account_sip_dtmf_type_sipinfo));

            if (isRingAccount) {
                accountDetails.put(ConfigKey.ACCOUNT_ALIAS.key(), "Ring");
                accountDetails.put(ConfigKey.ACCOUNT_HOSTNAME.key(), "bootstrap.ring.cx");
                if (newUsername != null && !newUsername.isEmpty()) {
                    accountDetails.put(ConfigKey.ACCOUNT_REGISTERED_NAME.key(), newUsername);
                }
                if (password != null && !password.isEmpty()) {
                    accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), password);
                }
                if (pin != null && !pin.isEmpty()) {
                    accountDetails.put(ConfigKey.ARCHIVE_PIN.key(), pin);
                }
                // Enable UPNP by default for Ring accounts
                accountDetails.put(ConfigKey.ACCOUNT_UPNP_ENABLE.key(), AccountConfig.TRUE_STR);
                createNewAccount(accountDetails, newUsername);
            } else {
                accountDetails.put(ConfigKey.ACCOUNT_ALIAS.key(), newUsername);
                if (!TextUtils.isEmpty(host)) {
                    accountDetails.put(ConfigKey.ACCOUNT_HOSTNAME.key(), host);
                    accountDetails.put(ConfigKey.ACCOUNT_USERNAME.key(), newUsername);
                    accountDetails.put(ConfigKey.ACCOUNT_PASSWORD.key(), password);
                }
                createNewAccount(accountDetails, null);
            }

        } catch (RemoteException e) {
            Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error creating account", e);
        }

    }

    public void ringChoose() {
        boolean migrate = getIntent().getData() != null && !TextUtils.isEmpty(getIntent().getData().getLastPathSegment());

        Bundle args = new Bundle();
        Fragment fragment;
        if (migrate) {
            args.putString(AccountMigrationFragment.ACCOUNT_ID, getIntent().getData().getLastPathSegment());
            fragment = new AccountMigrationFragment();
            fragment.setArguments(args);
        } else {
            fragment = new AccountCreationFragment();
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void ringCreate(boolean switchFragment) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (!switchFragment) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction
                .replace(R.id.fragment_container, new RingAccountCreationFragment())
                .commit();
    }

    public void ringLogin(boolean switchFragment) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (!switchFragment) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction
                .replace(R.id.fragment_container, new RingAccountLoginFragment())
                .commit();
    }

    private class CreateAccountTask extends AsyncTask<HashMap<String, String>, Void, String> {
        private ProgressDialog progress = null;
        private final String username;
        private final Context ctx;

        CreateAccountTask(String registerUsername, Context c) {
            Log.d(TAG, "CreateAccountTask ");
            username = registerUsername;
            ctx = c;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(ctx);
            progress.setTitle(R.string.dialog_wait_create);
            progress.setMessage(ctx.getString(R.string.dialog_wait_create_details));
            progress.setCancelable(false);
            progress.setCanceledOnTouchOutside(false);
            progress.show();
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(HashMap<String, String>... accs) {
            final Account account = mService.createAccount(accs[0]);
            account.stateListener = new Account.OnStateChangedListener() {
                @Override
                public void stateChanged(String state, int code) {
                    Log.i(TAG, "stateListener -> stateChanged " + state + " " + code);
                    if (!AccountConfig.STATE_INITIALIZING.contentEquals(state)) {
                        account.stateListener = null;
                        if (progress != null) {
                            if (progress.isShowing()) {
                                progress.dismiss();
                            }
                            progress = null;
                        }
                        //Intent resultIntent = new Intent();
                        AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
                        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                            }
                        });
                        boolean success = false;
                        switch (state) {
                            case AccountConfig.STATE_ERROR_GENERIC:
                                dialog.setTitle(R.string.account_cannot_be_found_title)
                                        .setMessage(R.string.account_cannot_be_found_message);
                                break;
                            case AccountConfig.STATE_ERROR_NETWORK:
                                dialog.setTitle(R.string.account_no_network_title)
                                        .setMessage(R.string.account_no_network_message);
                                break;
                            default:
                                dialog.setTitle(R.string.account_device_added_title)
                                        .setMessage(R.string.account_device_added_message);
                                success = true;
                                break;
                        }
                        AlertDialog alertDialog = dialog.show();
                        if (success) {
                            if (!TextUtils.isEmpty(username)) {
                                Log.i(TAG, "Account created, registering " + username);
                                mService.registerName(account, "", username, new LocalService.NameRegistrationCallback() {
                                    @Override
                                    public void onRegistered(String name) {
                                        Log.i(TAG, "Account wizard, onRegistered " + name);
                                    }

                                    @Override
                                    public void onError(String name, CharSequence err) {
                                        Log.w(TAG, "Account wizard, onError " + name);
                                    }
                                });
                            }

                            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    setResult(Activity.RESULT_OK, new Intent());
                                    finish();
                                }
                            });
                        }
                    }
                }
            };
            mCreatingAccount = false;
            return account.getAccountID();
        }
    }

    private void createNewAccount(HashMap<String, String> accountDetails, String registerName) {
        if (mCreatingAccount) {
            return;
        }

        mCreatingAccount = true;

        //noinspection unchecked
        new CreateAccountTask(registerName, this).execute(accountDetails);
    }

    @Override
    public IDRingService getRemoteService() {
        return mService.getRemoteService();
    }

    @Override
    public LocalService getService() {
        return mService;
    }

}
