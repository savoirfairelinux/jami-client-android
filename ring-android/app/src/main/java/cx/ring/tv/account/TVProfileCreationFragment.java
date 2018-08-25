/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

import cx.ring.R;
import cx.ring.account.ProfileCreationFragment;
import cx.ring.account.ProfileCreationPresenter;
import cx.ring.account.ProfileCreationView;
import cx.ring.account.RingAccountCreationFragment;
import cx.ring.account.RingAccountViewModelImpl;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.contacts.AvatarFactory;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.tv.camera.CustomCameraActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class TVProfileCreationFragment extends RingGuidedStepFragment<ProfileCreationPresenter>
        implements ProfileCreationView {

    private static final int USER_NAME = 1;
    private static final int GALLERY = 2;
    private static final int CAMERA = 3;
    private static final int NEXT = 4;

    private Bitmap mSourcePhoto;

    public static GuidedStepSupportFragment newInstance(RingAccountViewModelImpl pRingAccountViewModel) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(RingAccountCreationFragment.KEY_RING_ACCOUNT, pRingAccountViewModel);
        TVProfileCreationFragment fragment = new TVProfileCreationFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ProfileCreationFragment.REQUEST_CODE_PHOTO:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    updatePhoto((Bitmap) data.getExtras().get("data"));
                }
                break;
            case ProfileCreationFragment.REQUEST_CODE_GALLERY:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    updatePhoto(data.getData());
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
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

        RingAccountViewModelImpl ringAccountViewModel = (RingAccountViewModelImpl) getArguments().get(RingAccountCreationFragment.KEY_RING_ACCOUNT);
        presenter.initPresenter(ringAccountViewModel);

        if (ringAccountViewModel != null && ringAccountViewModel.getPhoto() != null) {
            getGuidanceStylist().getIconView().setImageBitmap(ringAccountViewModel.getPhoto());
        }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_create_title);
        String breadcrumb = "";
        String description = getString(R.string.profile_message_warning);
        Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_contact_picture_fallback);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
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
    public void goToNext(RingAccountViewModel ringAccountViewModel) {
        GuidedStepSupportFragment next;
        if (ringAccountViewModel.isLink()) {
            next = TVRingLinkAccountFragment.newInstance((RingAccountViewModelImpl) ringAccountViewModel);
        } else {
            next = TVRingAccountCreationFragment.newInstance((RingAccountViewModelImpl) ringAccountViewModel);
        }
        GuidedStepSupportFragment.add(getFragmentManager(), next);
    }

    @Override
    public void photoUpdate(RingAccountViewModel ringAccountViewModel) {
        ((RingAccountViewModelImpl) ringAccountViewModel).setPhoto(mSourcePhoto);

        RingAccountViewModelImpl model = (RingAccountViewModelImpl) ringAccountViewModel;

        Glide.with(getActivity())
                .load(model.getPhoto())
                .apply(AvatarFactory.getGlideOptions(true))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(getGuidanceStylist().getIconView());
    }

    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if (action.getId() == USER_NAME) {
            String username = action.getEditTitle().toString();
            presenter.fullNameUpdated(username);
            if (TextUtils.isEmpty(username))
                action.setTitle(getString(R.string.profile_name_hint));
            else
                action.setTitle(username);
        } else if (action.getId() == CAMERA) {
            presenter.cameraClick();
        } else if (action.getId() == GALLERY) {
            presenter.galleryClick();
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    public void updatePhoto(Uri uriImage) {
        ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updatePhoto, e -> Log.e(TAG, "Error loading image", e));
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        presenter.photoUpdated();
    }
}