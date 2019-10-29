/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.appcompat.app.AlertDialog;

import android.widget.Toast;

import java.io.File;

import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.account.AccountCreationModelImpl;
import cx.ring.account.AccountWizardPresenter;
import cx.ring.account.AccountWizardView;
import cx.ring.application.JamiApplication;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.BaseActivity;
import cx.ring.tv.main.HomeActivity;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class TVAccountWizard
        extends BaseActivity<AccountWizardPresenter>
        implements AccountWizardView {
    static final String TAG = TVAccountWizard.class.getName();
    private TVHomeAccountCreationFragment mHomeFragment = new TVHomeAccountCreationFragment();

    private ProgressDialog mProgress = null;
    private boolean mLinkAccount = false;
    private String mAccountType;
    private AlertDialog mAlertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        JamiApplication.getInstance().getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        JamiApplication.getInstance().startDaemon();

        Intent intent = getIntent();
        if (intent != null) {
            mAccountType = intent.getAction();
        }
        if (mAccountType == null) {
            mAccountType = AccountConfig.ACCOUNT_TYPE_RING;
        }

        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, mHomeFragment, android.R.id.content);
        } else {
            mLinkAccount = savedInstanceState.getBoolean("mLinkAccount");
        }

        presenter.init(getIntent().getAction() != null ? getIntent().getAction() : AccountConfig.ACCOUNT_TYPE_RING);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    public void createAccount(AccountCreationModel accountCreationModel) {
        if (accountCreationModel.isLink()) {
            presenter.initRingAccountLink(accountCreationModel,
                    getText(R.string.ring_account_default_name).toString());
        } else {
            presenter.initRingAccountCreation(accountCreationModel,
                    getText(R.string.ring_account_default_name).toString());
        }
    }

    @Override
    public void goToHomeCreation() {

    }

    @Override
    public void goToSipCreation() {

    }

    @Override
    public void onBackPressed() {
        GuidedStepSupportFragment fragment = GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(getSupportFragmentManager());
        if (fragment instanceof TVProfileCreationFragment)
            finish();
        else
            super.onBackPressed();
    }

    @Override
    public void goToProfileCreation(AccountCreationModel accountCreationModel) {
        GuidedStepSupportFragment.add(getSupportFragmentManager(), TVProfileCreationFragment.newInstance((AccountCreationModelImpl) accountCreationModel));
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
        Toast.makeText(TVAccountWizard.this, "Error creating account", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void blockOrientation() {
        //Noop on TV
    }

    @Override
    public void finish(final boolean affinity) {
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

    @Override
    public Single<VCard> saveProfile(final Account account, final AccountCreationModel accountCreationModel) {
        File filedir = getFilesDir();
        return accountCreationModel.toVCard()
                .flatMap(vcard -> {
                    account.setProfile(vcard);
                    return VCardUtils.saveLocalProfileToDisk(vcard, account.getAccountID(), filedir);
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public void displayGenericError() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            return;
        }
        mAlertDialog = new AlertDialog.Builder(TVAccountWizard.this)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(R.string.account_cannot_be_found_title)
                .setMessage(R.string.account_cannot_be_found_message)
                .show();
    }

    @Override
    public void displayNetworkError() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            return;
        }
        mAlertDialog = new AlertDialog.Builder(TVAccountWizard.this)
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
        mAlertDialog = new AlertDialog.Builder(TVAccountWizard.this)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(R.string.account_cannot_be_found_title)
                .setMessage(R.string.account_cannot_be_found_message)
                .show();
    }

    @Override
    public void displaySuccessDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            return;
        }
        setResult(Activity.RESULT_OK, new Intent());
        //startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void profileCreated(AccountCreationModel accountCreationModel, boolean saveProfile) {
        presenter.profileCreated(accountCreationModel, saveProfile);
    }
}
