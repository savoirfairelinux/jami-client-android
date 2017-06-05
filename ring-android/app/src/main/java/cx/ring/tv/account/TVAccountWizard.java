/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

package cx.ring.tv.account;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.account.AccountWizardPresenter;
import cx.ring.account.AccountWizardView;
import cx.ring.application.RingApplication;
import cx.ring.model.AccountConfig;
import cx.ring.mvp.BaseActivity;
import cx.ring.utils.Log;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;

public class TVAccountWizard
        extends BaseActivity<AccountWizardPresenter>
        implements AccountWizardView {
    public static final String PROFILE_TAG = "Profile";
    public static final int ACCOUNT_CREATE_REQUEST = 1;
    static final String TAG = TVAccountWizard.class.getName();
    private TVProfileCreationFragment mProfileFragment = new TVProfileCreationFragment();
    private TVHomeAccountCreationFragment mHomeFragment = new TVHomeAccountCreationFragment();

    private ProgressDialog mProgress = null;
    private boolean mLinkAccount = false;
    private String mFullname;
    private String mUsername;
    private String mPassword;
    private String mPin;
    private String mAccountType;
    private AlertDialog mAlertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard);
        ButterKnife.bind(this);


        Intent intent = getIntent();
        if (intent != null) {
            mAccountType = intent.getAction();
        }
        if (mAccountType == null) {
            mAccountType = AccountConfig.ACCOUNT_TYPE_RING;
        }

        if (savedInstanceState == null) {
            GuidedStepFragment.add(getFragmentManager(), mHomeFragment);
        } else {
            mProfileFragment = (TVProfileCreationFragment) getFragmentManager().getFragment(savedInstanceState, PROFILE_TAG);
            mFullname = savedInstanceState.getString("mFullname");
            mLinkAccount = savedInstanceState.getBoolean("mLinkAccount");
        }

        presenter.init(getIntent().getAction() != null ? getIntent().getAction() : AccountConfig.ACCOUNT_TYPE_RING);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mProfileFragment.isAdded()) {
            getFragmentManager().putFragment(outState, PROFILE_TAG, mProfileFragment);
        }
        outState.putString("mFullname", mFullname);
        outState.putBoolean("mLinkAccount", mLinkAccount);
    }

    @Override
    public void onDestroy() {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged " + newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return;
        }
        presenter.backPressed();
    }

    @Override
    public void saveProfile(String accountID, String username) {
        VCard vcard = new VCard();
        vcard.setFormattedName(new FormattedName(mFullname));
        vcard.setUid(new Uid(username));

        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountID, getFilesDir());
    }

    public void newAccount(boolean isNewAccount) {
        Log.d(TAG, "new account. linkAccount is " + isNewAccount);
        mLinkAccount = !isNewAccount;
        if (mLinkAccount) {
            GuidedStepFragment.add(getFragmentManager(), new TVProfileCreationFragment());
        } else {
            GuidedStepFragment.add(getFragmentManager(), new TVRingAccountCreationFragment());
        }
    }

    public void createAccount(String username, String pin, String password) {
        mUsername = username;
        mPassword = password;
        mPin = pin;
        if (mFullname == null) {
            mFullname = username;
        }
        createAccount();
    }

    public void createAccount() {
        if (mLinkAccount) {
            presenter.initRingAccountLink(mPin, mPassword, getText(R.string.ring_account_default_name).toString());
        } else {
            presenter.initRingAccountCreation(mUsername, mPassword, getText(R.string.ring_account_default_name).toString());
        }
    }

    public void profileNext(String fullname) {
        mFullname = fullname;
        if (mLinkAccount) {
            GuidedStepFragment.add(getFragmentManager(), new TVRingLinkAccountFragment());
        } else {
            GuidedStepFragment.add(getFragmentManager(), new TVRingAccountCreationFragment());
        }
    }

    @Override
    public void displayProgress(boolean display) {
        if (display) {
            mProgress = new ProgressDialog(this);
            mProgress.setTitle(R.string.dialog_wait_create);
            mProgress.setMessage(getString(R.string.dialog_wait_create_details));
            mProgress.setCancelable(false);
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.show();
        } else {
            if (mProgress != null) {
                if (mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                mProgress = null;
            }
        }

    }

    @Override
    public void displayCreationError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TVAccountWizard.this, "Error creating account", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void blockOrientation() {
        //Noop on TV
    }


    @Override
    public void finish(final boolean affinity) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (affinity) {
                    FragmentManager fm = getFragmentManager();
                    if (fm.getBackStackEntryCount() >= 1) {
                        fm.popBackStack();
                    } else {
                        finish();
                    }
                } else {
                    finishAffinity();
                }
            }
        });
    }

    @Override
    public void displayGenericError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    return;
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(TVAccountWizard.this);
                dialogBuilder.setPositiveButton(android.R.string.ok, null);
                dialogBuilder.setTitle(R.string.account_cannot_be_found_title)
                        .setMessage(R.string.account_cannot_be_found_message);
                mAlertDialog = dialogBuilder.show();
            }
        });
    }

    @Override
    public void displayNetworkError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    return;
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(TVAccountWizard.this);
                dialogBuilder.setPositiveButton(android.R.string.ok, null);
                dialogBuilder.setTitle(R.string.account_no_network_title)
                        .setMessage(R.string.account_no_network_message);
                mAlertDialog = dialogBuilder.show();
            }
        });
    }

    @Override
    public void displayCannotBeFoundError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    return;
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(TVAccountWizard.this);
                dialogBuilder.setPositiveButton(android.R.string.ok, null);
                dialogBuilder.setTitle(R.string.account_cannot_be_found_title)
                        .setMessage(R.string.account_cannot_be_found_message);
                mAlertDialog = dialogBuilder.show();
            }
        });
    }

    @Override
    public void displaySuccessDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    return;
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(TVAccountWizard.this);
                dialogBuilder.setPositiveButton(android.R.string.ok, null);
                dialogBuilder.setTitle(R.string.account_device_added_title)
                        .setMessage(R.string.account_device_added_message);
                mAlertDialog = dialogBuilder.show();
                mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        setResult(Activity.RESULT_OK, new Intent());
                        //unlock the screen orientation
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        finish();
                    }
                });
            }
        });

    }

}
