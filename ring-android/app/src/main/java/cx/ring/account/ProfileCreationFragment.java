/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountWizard;
import cx.ring.mvp.BaseFragment;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.BitmapUtils;

public class ProfileCreationFragment extends BaseFragment<ProfileCreationPresenter> implements ProfileCreationView {
    static final String TAG = ProfileCreationFragment.class.getSimpleName();
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

    @BindView(R.id.last_create_account)
    protected Button mLastButton;

    private Bitmap mSourcePhoto;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_profile_create, parent, false);
        ButterKnife.bind(this, view);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        if (savedInstanceState != null) {
            byte[] bytes = savedInstanceState.getByteArray(PHOTO_TAG);
            if (bytes != null && bytes.length > 0) {
                mSourcePhoto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }

        presenter.initPresenter();
        if (mPhotoView.getDrawable() == null) {
            if (mSourcePhoto == null) {
                mSourcePhoto = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.ic_contact_picture);
            }
            mPhotoView.setImageBitmap(BitmapUtils.cropImageToCircle(mSourcePhoto));
        }
        return view;
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

    public void updatePhoto(Uri uriImage) {
        updatePhoto(ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage));
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        mPhotoView.setImageBitmap(image != null ? BitmapUtils.cropImageToCircle(image) : null);
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

    @OnClick(R.id.last_create_account)
    public void lastClicked() {
        presenter.lastClick();
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
    public void goToNext() {
        String fullname = mFullnameView.getText().toString().trim();
        ((AccountWizard) getActivity()).profileNext(fullname, mSourcePhoto);
    }

    @Override
    public void goToLast() {
        ((AccountWizard) getActivity()).profileLast();
    }
}
