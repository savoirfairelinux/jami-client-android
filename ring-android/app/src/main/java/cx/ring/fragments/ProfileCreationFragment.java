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

package cx.ring.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.client.AccountWizard;
import cx.ring.utils.BitmapUtils;

public class ProfileCreationFragment extends Fragment {
    static final String TAG = ProfileCreationFragment.class.getSimpleName();
    private static final String[] PROFILE_PROJECTION = new String[]{ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY, ContactsContract.Profile.PHOTO_ID};
    public static final int REQUEST_CODE_PHOTO = 1;
    public static final int REQUEST_CODE_GALLERY = 2;
    public static final int REQUEST_PERMISSION_CAMERA = 3;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 4;

    @BindView(R.id.profile_photo)
    ImageView mPhotoView;

    @BindView(R.id.user_name)
    EditText mFullnameView;

    @BindView(R.id.gallery)
    ImageButton mGalleryButton;

    @BindView(R.id.camera)
    ImageButton mCameraButton;

    @BindView(R.id.next_create_account)
    Button mNextButton;

    @BindView(R.id.last_create_account)
    Button mLastButton;

    private Bitmap mSourcePhoto;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_profile_create, parent, false);
        ButterKnife.bind(this, view);

        initProfile();
        if (mPhotoView.getDrawable() == null) {
            mPhotoView.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_contact_picture));
            mSourcePhoto = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.ic_contact_picture);
        }
        if (TextUtils.isEmpty(mFullnameView.getText().toString())) {
            mFullnameView.setText(R.string.unknown);
        }
        return view;
    }

    private void initProfile() {
        //~ Checking the state of the READ_CONTACTS permission
        boolean hasReadContactsPermission = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if (hasReadContactsPermission) {
            Cursor mProfileCursor = getActivity().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, PROFILE_PROJECTION, null, null, null);
            if (mProfileCursor != null) {
                if (mProfileCursor.moveToFirst()) {
                    String displayName = mProfileCursor.getString(mProfileCursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME_PRIMARY));
                    Long photoID = mProfileCursor.getLong(mProfileCursor.getColumnIndex(ContactsContract.Profile.PHOTO_ID));
                    Uri photoUri = ContentUris.withAppendedId(ContactsContract.Profile.CONTENT_URI, photoID);
                    try {
                        InputStream inputStream = getActivity().getContentResolver().openInputStream(photoUri);
                        Drawable photo = Drawable.createFromStream(inputStream, photoUri.toString() );
                        mSourcePhoto = ((BitmapDrawable) photo).getBitmap();
                        mPhotoView.setImageDrawable(photo);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception setting profile picture", e);
                    }

                    if (TextUtils.isEmpty(displayName)) {
                        displayName = getActivity().getResources().getString(R.string.unknown);
                    }

                    mFullnameView.setText(displayName);
                }
                mProfileCursor.close();
            }
        } else {
            Log.d(TAG, "READ_CONTACTS permission is not granted.");
        }
    }

    public void updatePhoto(Uri uriImage) {
        Bitmap imageProfile = ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage);
        updatePhoto(imageProfile);
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        mPhotoView.setImageBitmap(BitmapUtils.cropImageToCircle(image));
    }

    @OnClick(R.id.gallery)
    public void galleryClicked() {
        boolean hasPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            getActivity().startActivityForResult(intent, REQUEST_CODE_GALLERY);
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_READ_STORAGE);
        }
    }

    @OnClick(R.id.camera)
    public void cameraClicked() {
        boolean hasPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            getActivity().startActivityForResult(intent, REQUEST_CODE_PHOTO);
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CAMERA);
        }
    }

    @OnClick(R.id.next_create_account)
    public void nextClicked() {
        String fullname = TextUtils.isEmpty(mFullnameView.getText().toString()) ? getString(R.string.unknown) : mFullnameView.getText().toString().trim();
        ((AccountWizard) getActivity()).profileNext(fullname, mSourcePhoto);
    }

    @OnClick(R.id.last_create_account)
    public void lastClicked() {
        ((AccountWizard) getActivity()).profileLast();
    }
}
