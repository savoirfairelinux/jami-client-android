/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.account.AccountWizardPresenter;
import cx.ring.account.AccountWizardView;
import cx.ring.account.RingAccountViewModelImpl;
import cx.ring.application.RingApplication;
import cx.ring.model.AccountConfig;
import cx.ring.mvp.BaseActivity;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.tv.main.HomeActivity;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.Log;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;

public class TVAccountWizard
        extends BaseActivity<AccountWizardPresenter>
        implements AccountWizardView {
    public static final String PROFILE_TAG = "Profile";
    static final String TAG = TVAccountWizard.class.getName();
    private TVProfileCreationFragment mProfileFragment = new TVProfileCreationFragment();
    private TVHomeAccountCreationFragment mHomeFragment = new TVHomeAccountCreationFragment();

    private ProgressDialog mProgress = null;
    private boolean mLinkAccount = false;
    private String mFullname;
    private String mAccountType;
    private AlertDialog mAlertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // dependency injection
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);


        Intent intent = getIntent();
        if (intent != null) {
            mAccountType = intent.getAction();
        }
        if (mAccountType == null) {
            mAccountType = AccountConfig.ACCOUNT_TYPE_RING;
        }

        if (savedInstanceState == null) {
            GuidedStepFragment.addAsRoot(this, mHomeFragment, android.R.id.content);
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

    public void createAccount(RingAccountViewModel ringAccountViewModel) {
        if (ringAccountViewModel.getFullName() == null || ringAccountViewModel.getFullName().isEmpty()) {
            ringAccountViewModel.setFullName(ringAccountViewModel.getUsername());
        }
        if (ringAccountViewModel.isLink()) {
            presenter.initRingAccountLink(ringAccountViewModel,
                    getText(R.string.ring_account_default_name).toString());
        } else {
            presenter.initRingAccountCreation(ringAccountViewModel,
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
        runOnUiThread(() -> Toast.makeText(TVAccountWizard.this, "Error creating account", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void blockOrientation() {
        //Noop on TV
    }


    @Override
    public void finish(final boolean affinity) {
        runOnUiThread(() -> {
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
        });
    }

    @Override
    public void saveProfile(final String accountID, final RingAccountViewModel ringAccountViewModel) {
        runOnUiThread(() -> {
            RingAccountViewModelImpl ringAccountViewModelImpl = (RingAccountViewModelImpl) ringAccountViewModel;

            VCard vcard = new VCard();
            vcard.setFormattedName(new FormattedName(ringAccountViewModelImpl.getFullName()));
            vcard.setUid(new Uid(ringAccountViewModelImpl.getUsername()));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (ringAccountViewModelImpl.getPhoto() != null) {
                ringAccountViewModelImpl.getPhoto().compress(Bitmap.CompressFormat.PNG, 100, stream);
                Photo photoVCard = new Photo(stream.toByteArray(), ImageType.PNG);
                vcard.removeProperties(Photo.class);
                vcard.addPhoto(photoVCard);
            }
            vcard.removeProperties(RawProperty.class);
            VCardUtils.saveLocalProfileToDisk(vcard, accountID, getFilesDir());
        });
    }

    @Override
    public void displayGenericError() {
        runOnUiThread(() -> {

            if (mAlertDialog != null && mAlertDialog.isShowing()) {
                return;
            }
            mAlertDialog = new AlertDialog.Builder(TVAccountWizard.this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setTitle(R.string.account_cannot_be_found_title)
                    .setMessage(R.string.account_cannot_be_found_message)
                    .show();
        });
    }

    @Override
    public void displayNetworkError() {
        runOnUiThread(() -> {

            if (mAlertDialog != null && mAlertDialog.isShowing()) {
                return;
            }
            mAlertDialog = new AlertDialog.Builder(TVAccountWizard.this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setTitle(R.string.account_no_network_title)
                    .setMessage(R.string.account_no_network_message)
                    .show();
        });
    }

    @Override
    public void displayCannotBeFoundError() {
        runOnUiThread(() -> {
            if (mAlertDialog != null && mAlertDialog.isShowing()) {
                return;
            }
            mAlertDialog = new AlertDialog.Builder(TVAccountWizard.this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setTitle(R.string.account_cannot_be_found_title)
                    .setMessage(R.string.account_cannot_be_found_message)
                    .show();
        });
    }

    @Override
    public void displaySuccessDialog() {
        runOnUiThread(() -> {
            if (mAlertDialog != null && mAlertDialog.isShowing()) {
                return;
            }
            mAlertDialog = new AlertDialog.Builder(TVAccountWizard.this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setTitle(R.string.account_device_added_title)
                    .setMessage(R.string.account_device_added_message)
                    .setOnDismissListener(dialogInterface -> {
                        setResult(Activity.RESULT_OK, new Intent());
                        //unlock the screen orientation
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        startActivity(new Intent(TVAccountWizard.this, HomeActivity.class));
                        finish();
                    })
                    .show();
        });
    }
}
