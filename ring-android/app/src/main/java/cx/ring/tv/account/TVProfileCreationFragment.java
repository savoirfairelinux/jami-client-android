/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.account;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.List;

import cx.ring.R;
import cx.ring.account.AccountCreationModelImpl;
import cx.ring.account.ProfileCreationFragment;
import cx.ring.account.ProfileCreationPresenter;
import cx.ring.account.ProfileCreationView;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.tv.camera.CustomCameraActivity;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.views.AvatarDrawable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class TVProfileCreationFragment extends RingGuidedStepFragment<ProfileCreationPresenter>
        implements ProfileCreationView {

    private static final int USER_NAME = 1;
    private static final int GALLERY = 2;
    private static final int CAMERA = 3;
    private static final int NEXT = 4;

    private AccountCreationModelImpl mModel;
    private int iconSize = -1;

    public static GuidedStepSupportFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        TVProfileCreationFragment fragment = new TVProfileCreationFragment();
        fragment.mModel = ringAccountViewModel;
        return fragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case ProfileCreationFragment.REQUEST_CODE_PHOTO:
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    presenter.photoUpdated(Single.just(intent).map(i -> i.getExtras().get("data")));
                }
                break;
            case ProfileCreationFragment.REQUEST_CODE_GALLERY:
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    presenter.photoUpdated(AndroidFileUtils.loadBitmap(getActivity(), intent.getData()).map(b -> (Object)b));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, intent);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ProfileCreationFragment.REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.cameraClick();
                }
                break;
            case ProfileCreationFragment.REQUEST_PERMISSION_READ_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.galleryClick();
                }
                break;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onViewCreated(view, savedInstanceState);

        if (mModel == null) {
            getActivity().finish();
            return;
        }

        iconSize = (int) getResources().getDimension(R.dimen.tv_avatar_size);
        presenter.initPresenter(mModel);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_create_title);
        String breadcrumb = "";
        String description = getString(R.string.profile_message_warning);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, new AvatarDrawable(getContext(), (Bitmap) null, mModel == null ? null : mModel.getFullName(), mModel == null ? null : mModel.getUsername(), null, true));
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addEditTextAction(getActivity(), actions, USER_NAME, R.string.profile_name_hint, R.string.profile_name_hint);
        addAction(getActivity(), actions, CAMERA, getActivity().getResources().getString(R.string.take_a_photo), "");
        addAction(getActivity(), actions, GALLERY, getActivity().getResources().getString(R.string.open_the_gallery), "");
        addAction(getActivity(), actions, NEXT, getActivity().getResources().getString(R.string.wizard_next), "", true);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CAMERA) {
            presenter.cameraClick();
        } else if (action.getId() == GALLERY) {
            presenter.galleryClick();
        } else if (action.getId() == NEXT) {
            presenter.nextClick();
        }
    }

    @Override
    public void displayProfileName(String profileName) {
        findActionById(USER_NAME).setEditDescription(profileName);
        notifyActionChanged(findActionPositionById(USER_NAME));
    }

    @Override
    public void goToGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, ProfileCreationFragment.REQUEST_CODE_GALLERY);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(getActivity())
                    .setPositiveButton(android.R.string.ok, null)
                    .setTitle(R.string.gallery_error_title)
                    .setMessage(R.string.gallery_error_message)
                    .show();
        }
    }

    @Override
    public void goToPhotoCapture() {
        Intent intent = new Intent(getActivity(), CustomCameraActivity.class);
        startActivityForResult(intent, ProfileCreationFragment.REQUEST_CODE_PHOTO);
    }

    @Override
    public void askStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                ProfileCreationFragment.REQUEST_PERMISSION_READ_STORAGE);
    }

    @Override
    public void askPhotoPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                ProfileCreationFragment.REQUEST_PERMISSION_CAMERA);
    }

    @Override
    public void goToNext(AccountCreationModel accountCreationModel, boolean saveProfile) {
        Activity wizardActivity = getActivity();
        if (wizardActivity instanceof TVAccountWizard) {
            TVAccountWizard wizard = (TVAccountWizard) wizardActivity;
            wizard.profileCreated(accountCreationModel, saveProfile);
        }
    }

    @Override
    public void setProfile(AccountCreationModel accountCreationModel) {
        AccountCreationModelImpl model = ((AccountCreationModelImpl) accountCreationModel);
        Account newAccount = model.getNewAccount();
        AvatarDrawable avatar = new AvatarDrawable(getContext(), model.getPhoto(), accountCreationModel.getFullName(), accountCreationModel.getUsername(), newAccount == null ? null : newAccount.getUsername(), true);
        avatar.setInSize(iconSize);
        getGuidanceStylist().getIconView().setImageDrawable(avatar);
    }

    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        switch ((int) action.getId()){
            case USER_NAME:
                String username = action.getEditTitle().toString();
                presenter.fullNameUpdated(username);
                if (username.isEmpty())
                    action.setTitle(getString(R.string.profile_name_hint));
                else
                    action.setTitle(username);
                break;
            case CAMERA:
                presenter.cameraClick();
                break;
            case GALLERY:
                presenter.galleryClick();
                break;
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    @Override
    public void onGuidedActionEditCanceled(GuidedAction action) {
        if ((int) action.getId() == USER_NAME) {
            String username = action.getEditTitle().toString();
            presenter.fullNameUpdated(username);
            if (TextUtils.isEmpty(username))
                action.setTitle(getString(R.string.profile_name_hint));
            else
                action.setTitle(username);
        }
        super.onGuidedActionEditCanceled(action);
    }
}