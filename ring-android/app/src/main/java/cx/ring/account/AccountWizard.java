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

package cx.ring.account;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;
import cx.ring.fragments.AccountMigrationFragment;
import cx.ring.fragments.SIPAccountCreationFragment;
import cx.ring.model.AccountConfig;
import cx.ring.mvp.BaseActivity;
import cx.ring.utils.Log;
import cx.ring.utils.VCardUtils;
import cx.ring.views.WizardViewPager;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;

public class AccountWizard extends BaseActivity<AccountWizardPresenter> implements AccountWizardView {
    public static final String PROFILE_TAG = "Profile";
    static final String TAG = AccountWizard.class.getName();
    @BindView(R.id.pager)
    WizardViewPager mViewPager;
    private ProfileCreationFragment mProfileFragment;
    private HomeAccountCreationFragment mHomeFragment;
    private ProgressDialog mProgress = null;
    private boolean mLinkAccount = false;
    private Bitmap mPhotoProfile;
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

        mViewPager.setAdapter(new WizardPagerAdapter(getFragmentManager()));
        mViewPager.getAdapter().notifyDataSetChanged();

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

        presenter.init(getIntent().getAction() != null ? getIntent().getAction() : AccountConfig.ACCOUNT_TYPE_RING);

        if (savedInstanceState == null) {
            if (accountToMigrate != null) {
                mViewPager.setVisibility(View.GONE);
                Bundle args = new Bundle();
                args.putString(AccountMigrationFragment.ACCOUNT_ID, getIntent().getData().getLastPathSegment());
                Fragment fragment = new AccountMigrationFragment();
                fragment.setArguments(args);
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.migration_container, fragment)
                        .commit();
            } else {
                mViewPager.setOffscreenPageLimit(4);
            }
        } else {
            mProfileFragment = (ProfileCreationFragment) getFragmentManager().getFragment(savedInstanceState, PROFILE_TAG);
            mFullname = savedInstanceState.getString("mFullname");
            byte[] bytes = savedInstanceState.getByteArray("mPhotoProfile");
            if (bytes != null && bytes.length > 0) {
                mPhotoProfile = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            mLinkAccount = savedInstanceState.getBoolean("mLinkAccount");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mProfileFragment.isAdded()) {
            getFragmentManager().putFragment(outState, PROFILE_TAG, mProfileFragment);
        }
        outState.putString("mFullname", mFullname);
        if (mPhotoProfile != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            mPhotoProfile.compress(Bitmap.CompressFormat.PNG, 100, stream);
            outState.putByteArray("mPhotoProfile", stream.toByteArray());
        }
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
        FragmentManager fragmentManager = getFragmentManager();
        if (mViewPager.getCurrentItem() > 0) {
            fragmentManager.popBackStack();
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            return;
        }

        presenter.backPressed();
    }

    @Override
    public void saveProfile(String accountID, String username) {
        VCard vcard = new VCard();
        vcard.setFormattedName(new FormattedName(mFullname));
        vcard.setUid(new Uid(username));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (mPhotoProfile != null) {
            mPhotoProfile.compress(Bitmap.CompressFormat.PNG, 100, stream);
            Photo photoVCard = new Photo(stream.toByteArray(), ImageType.PNG);
            vcard.removeProperties(Photo.class);
            vcard.addPhoto(photoVCard);
        }
        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountID, getFilesDir());
    }

    public void newAccount(boolean linkAccount) {
        Log.d(TAG, "new account. linkAccount is " + linkAccount);
        mLinkAccount = linkAccount;
        mViewPager.getAdapter().notifyDataSetChanged();
        mViewPager.setCurrentItem(1);
    }

    public void profileLast() {
        mViewPager.setCurrentItem(0);
    }

    public void profileNext(String fullname, Bitmap photo) {
        mPhotoProfile = photo;
        mFullname = fullname;

        mViewPager.setCurrentItem(2);
    }

    public void accountLast() {
        mViewPager.setCurrentItem(1);
    }

    public void createAccount(String username, String pin, String password) {
        mUsername = username;
        mPassword = password;
        mPin = pin;
        createAccount();
    }

    public void createAccount() {
        if (mLinkAccount) {
            presenter.initRingAccountLink(mPin, mPassword, getText(R.string.ring_account_default_name).toString());
        } else {
            presenter.initRingAccountCreation(mUsername, mPassword, getText(R.string.ring_account_default_name).toString());
        }
    }

    @Override
    public void displayProgress(final boolean display) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (display) {
                    mProgress = new ProgressDialog(AccountWizard.this);
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
        });
    }

    @Override
    public void displayCreationError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AccountWizard.this, "Error creating account", Toast.LENGTH_SHORT).show();

            }
        });
    }


    @Override
    public void blockOrientation() {
        //orientation is locked during the create of account to avoid the destruction of the thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        });
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AccountWizard.this);
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AccountWizard.this);
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AccountWizard.this);
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AccountWizard.this);
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

    private class WizardPagerAdapter extends FragmentStatePagerAdapter {

        WizardPagerAdapter(FragmentManager fm) {
            super(fm);
            mHomeFragment = new HomeAccountCreationFragment();
            if (mProfileFragment == null) {
                mProfileFragment = new ProfileCreationFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "getItem");
            switch (position) {
                case 0:
                    if (AccountConfig.ACCOUNT_TYPE_SIP.equals(mAccountType)) {
                        return new SIPAccountCreationFragment();
                    } else {
                        return mHomeFragment;
                    }
                case 1:
                    return mProfileFragment;
                case 2:
                    if (mLinkAccount) {
                        return new RingLinkAccountFragment();
                    } else {
                        return new RingAccountCreationFragment();
                    }
                default:
                    return null;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            Log.d(TAG, "getItemPosition");
            return POSITION_NONE;
        }
    }

}
