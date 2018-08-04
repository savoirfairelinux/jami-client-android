/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import java.io.ByteArrayOutputStream;
import java.util.List;

import cx.ring.R;
import cx.ring.account.ProfileCreationFragment;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.contacts.AvatarFactory;
import cx.ring.model.Account;
import cx.ring.navigation.RingNavigationPresenter;
import cx.ring.navigation.RingNavigationView;
import cx.ring.navigation.RingNavigationViewModel;
import cx.ring.tv.camera.CustomCameraActivity;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;

public class TVProfileEditingFragment extends RingGuidedStepFragment<RingNavigationPresenter>
        implements RingNavigationView {

    private static final int USER_NAME = 1;
    private static final int GALLERY = 2;
    private static final int CAMERA = 3;

    private List<GuidedAction> actions;

    public static GuidedStepSupportFragment newInstance() {
        return new TVProfileEditingFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ProfileCreationFragment.REQUEST_CODE_PHOTO:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Bundle extras = data.getExtras();
                    if (extras == null) {
                        Log.e(TAG, "onActivityResult: Not able to get picture from extra");
                        return;
                    }
                    updatePhoto((Bitmap) extras.get("data"));
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ProfileCreationFragment.REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.cameraClicked();
                }
                break;
            case ProfileCreationFragment.REQUEST_PERMISSION_READ_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.galleryClicked();
                }
                break;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.profile);
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
        addEditTextAction(getActivity(), actions, USER_NAME, R.string.account_edit_profile, R.string.profile_name_hint);
        addAction(getActivity(), actions, CAMERA, getActivity().getResources().getString(R.string.take_a_photo), "");
        addAction(getActivity(), actions, GALLERY, getActivity().getResources().getString(R.string.open_the_gallery), "");
        this.actions = actions;
    }

    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if (action.getId() == USER_NAME) {
            String username = action.getEditTitle().toString();
            presenter.saveVCardFormattedName(username);
        } else if (action.getId() == CAMERA) {
            presenter.cameraClicked();
        } else if (action.getId() == GALLERY) {
            presenter.galleryClicked();
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CAMERA) {
            presenter.cameraClicked();
        } else if (action.getId() == GALLERY) {
            presenter.galleryClicked();
        }
    }

    @Override
    public void showViewModel(RingNavigationViewModel viewModel) {
        // displays account available info
        VCard vcard = viewModel.getVcard();
        Account account = viewModel.getAccount();
        if (account == null) {
            Log.e(TAG, "Not able to get current account");
            return;
        }

        String alias = presenter.getAlias(account);
        GuidedAction action = actions.isEmpty() ? null : actions.get(0);
        if (action != null && action.getId() == USER_NAME) {
            if (TextUtils.isEmpty(alias)) {
                action.setEditTitle("");
                action.setTitle(getString(R.string.account_edit_profile));

            } else {
                action.setEditTitle(alias);
                action.setTitle(alias);
            }
            notifyActionChanged(0);
        }

        if (TextUtils.isEmpty(alias))
            getGuidanceStylist().getTitleView().setText(R.string.profile);
        else
            getGuidanceStylist().getTitleView().setText(alias);

        if (vcard == null || vcard.getPhotos().isEmpty()) {
            getGuidanceStylist().getIconView().setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_contact_picture_fallback));
            return;
        }

        Drawable contactPicture = AvatarFactory.getAvatar(
                getActivity(),
                vcard.getPhotos().get(0).getData(),
                account.getDisplayUsername(),
                account.getUri());

        Glide.with(getActivity())
                .load(contactPicture)
                .apply(AvatarFactory.getGlideOptions(true, false))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(getGuidanceStylist().getIconView());
    }

    @Override
    public void updateModel(Account account) {
    }

    @Override
    public void gotToImageCapture() {
        Intent intent = new Intent(getActivity(), CustomCameraActivity.class);
        startActivityForResult(intent, ProfileCreationFragment.REQUEST_CODE_PHOTO);
    }

    @Override
    public void askCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                ProfileCreationFragment.REQUEST_PERMISSION_CAMERA);
    }

    @Override
    public void askGalleryPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                ProfileCreationFragment.REQUEST_PERMISSION_READ_STORAGE);
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

    public void updatePhoto(Uri uriImage) {
        updatePhoto(ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage));
    }

    public void updatePhoto(Bitmap image) {
        if (image == null) {
            Log.w(TAG, "updatePhoto: null photo");
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        Photo photo = new Photo(stream.toByteArray(), ImageType.PNG);

        AvatarFactory.clearCache();
        presenter.saveVCardPhoto(photo);
    }
}