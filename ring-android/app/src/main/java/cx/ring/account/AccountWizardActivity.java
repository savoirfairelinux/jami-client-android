/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.account;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.fragments.AccountMigrationFragment;
import cx.ring.fragments.SIPAccountCreationFragment;

import net.jami.account.AccountWizardPresenter;
import net.jami.account.AccountWizardView;
import net.jami.model.Account;
import net.jami.model.AccountConfig;
import cx.ring.mvp.BaseActivity;
import net.jami.mvp.AccountCreationModel;
import net.jami.utils.VCardUtils;

import dagger.hilt.android.AndroidEntryPoint;
import ezvcard.VCard;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class AccountWizardActivity extends BaseActivity<AccountWizardPresenter> implements AccountWizardView {
    static final String TAG = AccountWizardActivity.class.getName();

    private ProgressDialog mProgress = null;
    private String mAccountType;
    private AlertDialog mAlertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JamiApplication.getInstance().startDaemon();
        setContentView(R.layout.activity_wizard);

        String accountToMigrate = null;
        Intent intent = getIntent();
        if (intent != null) {
            mAccountType = intent.getAction();
            Uri path = intent.getData();
            if (path != null) {
                accountToMigrate = path.getLastPathSegment();
            }
        }
        if (mAccountType == null) {
            mAccountType = AccountConfig.ACCOUNT_TYPE_RING;
        }

        if (savedInstanceState == null) {
            if (accountToMigrate != null) {
                Bundle args = new Bundle();
                args.putString(AccountMigrationFragment.ACCOUNT_ID, getIntent().getData().getLastPathSegment());
                Fragment fragment = new AccountMigrationFragment();
                fragment.setArguments(args);
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.wizard_container, fragment)
                        .commit();
            } else {
                presenter.init(getIntent().getAction() != null ? getIntent().getAction() : AccountConfig.ACCOUNT_TYPE_RING);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.setOnDismissListener(null);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        super.onDestroy();
    }

    @Override
    public Single<VCard> saveProfile(final Account account, final AccountCreationModel accountCreationModel) {
        File filedir = getFilesDir();
        return accountCreationModel.toVCard()
                .flatMap(vcard -> {
                    account.resetProfile();
                    return VCardUtils.saveLocalProfileToDisk(vcard, account.getAccountID(), filedir);
                })
                .subscribeOn(Schedulers.io());
    }

    public void createAccount(AccountCreationModel accountCreationModel) {
        if (!TextUtils.isEmpty(accountCreationModel.getManagementServer())) {
            presenter.initJamiAccountConnect(accountCreationModel,
                    getText(R.string.ring_account_default_name).toString());
        } else if (accountCreationModel.isLink()) {
            presenter.initJamiAccountLink(accountCreationModel,
                    getText(R.string.ring_account_default_name).toString());
        } else {
            presenter.initJamiAccountCreation(accountCreationModel,
                    getText(R.string.ring_account_default_name).toString());
        }
    }

    @Override
    public void goToHomeCreation() {
        Fragment fragment = new HomeAccountCreationFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.wizard_container, fragment, HomeAccountCreationFragment.TAG)
                .commit();
    }

    @Override
    public void goToSipCreation() {
        Fragment fragment = new SIPAccountCreationFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.wizard_container, fragment, SIPAccountCreationFragment.Companion.getTAG())
                .commit();
    }

    @Override
    public void goToProfileCreation(AccountCreationModel model) {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.size() > 0) {
            Fragment fragment =fragments.get(0);
            if (fragment instanceof JamiLinkAccountFragment) {
                ((JamiLinkAccountFragment) fragment).scrollPagerFragment(model);
            } else if (fragment instanceof JamiAccountConnectFragment) {
                profileCreated(model, false);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.wizard_container);
        if (fragment instanceof ProfileCreationFragment)
            finish();
        else
            super.onBackPressed();
    }

    @Override
    public void displayProgress(final boolean display) {
        if (display) {
            mProgress = new ProgressDialog(AccountWizardActivity.this);
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
        Toast.makeText(AccountWizardActivity.this, "Error creating account", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void blockOrientation() {
        //orientation is locked during the create of account to avoid the destruction of the thread
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    @Override
    public void finish(final boolean affinity) {
        if (affinity) {
            startActivity(new Intent(AccountWizardActivity.this, HomeActivity.class));
            finish();
        } else {
            finishAffinity();
        }
    }

    @Override
    public void displayGenericError() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            return;
        }
        mAlertDialog = new MaterialAlertDialogBuilder(AccountWizardActivity.this)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(R.string.account_cannot_be_found_title)
                .setMessage(R.string.account_export_end_decryption_message)
                .show();
    }

    @Override
    public void displayNetworkError() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            return;
        }
        mAlertDialog = new MaterialAlertDialogBuilder(AccountWizardActivity.this)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(R.string.account_no_network_title)
                .setMessage(R.string.account_no_network_message)
                .show();
    }

    @Override
    public void displayCannotBeFoundError() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            return;
        }
        mAlertDialog = new MaterialAlertDialogBuilder(AccountWizardActivity.this)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(R.string.account_cannot_be_found_title)
                .setMessage(R.string.account_cannot_be_found_message)
                .setOnDismissListener(dialogInterface -> getSupportFragmentManager().popBackStack())
                .show();
    }

    @Override
    public void displaySuccessDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            return;
        }
        setResult(Activity.RESULT_OK, new Intent());
        //unlock the screen orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        presenter.successDialogClosed();
    }

    public void profileCreated(AccountCreationModel accountCreationModel, boolean saveProfile) {
        presenter.profileCreated(accountCreationModel, saveProfile);
    }
}
