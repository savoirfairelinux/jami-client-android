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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.account;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.mvp.BaseFragment;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.utils.BitmapUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ProfileCreationFragment extends BaseFragment<ProfileCreationPresenter> implements ProfileCreationView, TextWatcher {
    public static final String TAG = ProfileCreationFragment.class.getSimpleName();
    public static final String KEY_IS_LINK = "IS_LINK";

    public static final int REQUEST_CODE_PHOTO = 1;
    public static final int REQUEST_CODE_GALLERY = 2;
    public static final int REQUEST_PERMISSION_CAMERA = 3;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 4;

    public static final String PHOTO_TAG = "Photo";

    @BindView(R.id.profile_photo)
    protected ImageView mPhotoView;

    @BindView(R.id.user_name)
    protected EditText mFullnameView;

    @BindView(R.id.gallery)
    protected ImageButton mGalleryButton;

    @BindView(R.id.camera)
    protected ImageButton mCameraButton;

    @BindView(R.id.next_create_account)
    protected Button mNextButton;

    private Bitmap mSourcePhoto;

    public static ProfileCreationFragment newInstance(boolean isLink) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_IS_LINK, isLink);
        ProfileCreationFragment fragment = new ProfileCreationFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getLayout() {
        return R.layout.frag_acc_profile_create;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setRetainInstance(true);

        if (savedInstanceState != null) {
            byte[] bytes = savedInstanceState.getByteArray(PHOTO_TAG);
            if (bytes != null && bytes.length > 0) {
                mSourcePhoto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }

        if (mPhotoView.getDrawable() == null) {
            if (mSourcePhoto == null) {
                mSourcePhoto = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.ic_contact_picture_fallback);
            }
            mPhotoView.setImageBitmap(BitmapUtils.cropImageToCircle(mSourcePhoto));
        }

        RingAccountViewModelImpl ringAccountViewModel = new RingAccountViewModelImpl();
        boolean isLink = getArguments().getBoolean(KEY_IS_LINK, false);
        ringAccountViewModel.setLink(isLink);

        presenter.initPresenter(ringAccountViewModel);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSourcePhoto != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            mSourcePhoto.compress(Bitmap.CompressFormat.PNG, 100, stream);
            outState.putByteArray(PHOTO_TAG, stream.toByteArray());
        }
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

    public void updatePhoto(Uri uriImage) {
        ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updatePhoto, e -> Log.e(TAG, "Error loading image", e));
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        presenter.photoUpdated();
    }

    @OnClick(R.id.gallery)
    public void galleryClicked() {
        presenter.galleryClick();
    }

    @OnClick(R.id.camera)
    public void cameraClicked() {
        presenter.cameraClick();
    }

    @OnClick(R.id.next_create_account)
    public void nextClicked() {
        presenter.nextClick();
    }

    @Override
    public void displayProfileName(String profileName) {
        mFullnameView.setText(profileName);
    }

    @Override
    public void goToGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    public void goToPhotoCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
        if (ringAccountViewModel.isLink()) {
            Fragment fragment = RingLinkAccountFragment.newInstance((RingAccountViewModelImpl) ringAccountViewModel);
            replaceFragmentWithSlide(fragment, R.id.wizard_container);
        } else {
            Fragment fragment = RingAccountCreationFragment.newInstance((RingAccountViewModelImpl) ringAccountViewModel);
            replaceFragmentWithSlide(fragment, R.id.wizard_container);
        }
    }

    @Override
    public void photoUpdate(RingAccountViewModel ringAccountViewModel) {
        ((RingAccountViewModelImpl) ringAccountViewModel).setPhoto(mSourcePhoto);
        mPhotoView.setImageBitmap(mSourcePhoto != null ? BitmapUtils.cropImageToCircle(mSourcePhoto) : null);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @OnTextChanged(value = R.id.user_name, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void afterTextChanged(Editable txt) {
        presenter.fullNameUpdated(txt.toString());
    }
}
