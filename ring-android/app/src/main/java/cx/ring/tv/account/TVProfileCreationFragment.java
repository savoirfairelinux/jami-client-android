/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import java.util.List;

import cx.ring.R;
import cx.ring.account.ProfileCreationFragment;
import cx.ring.account.ProfileCreationPresenter;
import cx.ring.account.ProfileCreationView;
import cx.ring.account.RingAccountCreationFragment;
import cx.ring.account.RingAccountViewModelImpl;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.tv.camera.CustomCameraActivity;

public class TVProfileCreationFragment extends RingGuidedStepFragment<ProfileCreationPresenter>
        implements ProfileCreationView {

    public static final int REQUEST_CODE_PHOTO = 1;
    public static final int REQUEST_CODE_GALLERY = 2;
    public static final int REQUEST_PERMISSION_CAMERA = 3;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 4;
    private static final int USER_NAME = 1;
    private static final int GALLERY = 2;
    private static final int CAMERA = 3;
    private static final int NEXT = 4;

    private Bitmap mSourcePhoto;

    public static GuidedStepFragment newInstance(RingAccountViewModelImpl pRingAccountViewModel) {
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
                    byte[] input = data.getExtras().getByteArray("data");
                    Bitmap original = BitmapFactory.decodeByteArray(input, 0, input.length);
                    updatePhoto(original);
                }
                break;
            case ProfileCreationFragment.REQUEST_CODE_GALLERY:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    updatePhoto(data.getData());
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        super.onViewCreated(view, savedInstanceState);

        RingAccountViewModelImpl ringAccountViewModel = (RingAccountViewModelImpl) getArguments().get(RingAccountCreationFragment.KEY_RING_ACCOUNT);
        presenter.initPresenter(ringAccountViewModel);

        if (ringAccountViewModel.getPhoto() != null) {
            getGuidanceStylist().getIconView().setImageBitmap(ringAccountViewModel.getPhoto());
        }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_create_title);
        String breadcrumb = "";
        String description = getString(R.string.profile_message_warning);
        Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_contact_picture);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        String desc = getString(R.string.account_creation_profile);
        String editdesc = getString(R.string.profile_name_hint);
        addEditTextAction(actions, USER_NAME, desc, editdesc, "");
        addAction(actions, CAMERA, getActivity().getResources().getString(R.string.take_a_photo), "");
        addAction(actions, GALLERY, getActivity().getResources().getString(R.string.open_the_gallery), "");
        addAction(actions, NEXT, getActivity().getResources().getString(R.string.wizard_next), "", true);
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
            startActivityForResult(intent, REQUEST_CODE_GALLERY);
        } catch (ActivityNotFoundException e) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setPositiveButton(android.R.string.ok, null);
            dialogBuilder.setTitle(R.string.gallery_error_title)
                    .setMessage(R.string.gallery_error_message);
            dialogBuilder.show();
        }
    }

    @Override
    public void goToPhotoCapture() {
        Intent intent = new Intent(getActivity(), CustomCameraActivity.class);
        startActivityForResult(intent, REQUEST_CODE_PHOTO);
    }

    @Override
    public void askStoragePermission() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSION_READ_STORAGE);
    }

    @Override
    public void askPhotoPermission() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_PERMISSION_CAMERA);
    }

    @Override
    public void goToNext(RingAccountViewModel ringAccountViewModel) {
        GuidedStepFragment next;
        if (ringAccountViewModel.isLink()) {
            next = TVRingLinkAccountFragment.newInstance((RingAccountViewModelImpl) ringAccountViewModel);
        } else {
            next = TVRingAccountCreationFragment.newInstance((RingAccountViewModelImpl) ringAccountViewModel);
        }
        GuidedStepFragment.add(getFragmentManager(), next);
    }

    @Override
    public void photoUpdate(RingAccountViewModel ringAccountViewModel) {
        ((RingAccountViewModelImpl) ringAccountViewModel).setPhoto(mSourcePhoto);
        RingAccountViewModelImpl model = (RingAccountViewModelImpl) ringAccountViewModel;
        getGuidanceStylist().getIconView().setImageBitmap(model.getPhoto());
    }

    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if (action.getId() == USER_NAME) {
            String username = action.getEditDescription().toString();
            presenter.fullNameUpdated(username);
        } else if (action.getId() == CAMERA) {
            presenter.cameraClick();
        } else if (action.getId() == GALLERY) {
            presenter.galleryClick();
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    public void updatePhoto(Uri uriImage) {
        updatePhoto(ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage));
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        presenter.photoUpdated();
    }


}