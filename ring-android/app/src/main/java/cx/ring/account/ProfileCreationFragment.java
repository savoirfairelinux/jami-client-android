/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

import androidx.core.content.FileProvider;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cx.ring.R;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.model.Account;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.views.AvatarDrawable;
import io.reactivex.Single;

public class ProfileCreationFragment extends BaseSupportFragment<ProfileCreationPresenter> implements ProfileCreationView {
    public static final String TAG = ProfileCreationFragment.class.getSimpleName();

    public static final int REQUEST_CODE_PHOTO = 1;
    public static final int REQUEST_CODE_GALLERY = 2;
    public static final int REQUEST_PERMISSION_CAMERA = 3;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 4;

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

    private AccountCreationModel model;
    private Uri tmpProfilePhotoUri;

    public static ProfileCreationFragment newInstance(AccountCreationModelImpl model) {
        ProfileCreationFragment fragment = new ProfileCreationFragment();
        fragment.model = model;
        return fragment;
    }

    @Override
    public int getLayout() {
        return R.layout.frag_acc_profile_create;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);

        if (model == null) {
            getActivity().finish();
            return;
        }
        if (mPhotoView.getDrawable() == null) {
            mPhotoView.setImageDrawable(new AvatarDrawable(view.getContext(), (Bitmap) null, model.getFullName(), model.getUsername(), null, true));
        }
        presenter.initPresenter(model);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CODE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    if (tmpProfilePhotoUri == null) {
                        if (intent != null) {
                            Bundle bundle = intent.getExtras();
                            Bitmap b = bundle == null ? null : (Bitmap) bundle.get("data");
                            if (b != null) {
                                presenter.photoUpdated(Single.just(b));
                            }
                        }
                    } else {
                        presenter.photoUpdated(AndroidFileUtils.loadBitmap(getContext(), tmpProfilePhotoUri).map(b -> (Object)b));
                    }
                }
                break;
            case REQUEST_CODE_GALLERY:
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
                    presenter.cameraPermissionChanged(true);
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

    @OnClick(R.id.gallery)
    void galleryClicked() {
        presenter.galleryClick();
    }

    @OnClick(R.id.camera)
    void cameraClicked() {
        presenter.cameraClick();
    }

    @OnClick(R.id.next_create_account)
    void nextClicked() {
        presenter.nextClick();
    }

    @OnClick(R.id.skip_create_account)
    void skipClicked() {
        presenter.skipClick();
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
        try {
            Context context = requireContext();
            File file = AndroidFileUtils.createImageFile(context);
            Uri uri = FileProvider.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            tmpProfilePhotoUri = uri;
        } catch (IOException e) {
            Log.e(TAG, "Can't create temp file", e);
        }
        startActivityForResult(intent, REQUEST_CODE_PHOTO);
    }

    @Override
    public void askStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_STORAGE);
    }

    @Override
    public void askPhotoPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CAMERA);
    }

    @Override
    public void goToNext(AccountCreationModel accountCreationModel, boolean saveProfile) {
        Activity wizardActivity = getActivity();
        if (wizardActivity instanceof AccountWizardActivity) {
            AccountWizardActivity wizard = (AccountWizardActivity) wizardActivity;
            wizard.profileCreated(accountCreationModel, saveProfile);
        }
    }

    @Override
    public void setProfile(AccountCreationModel accountCreationModel) {
        AccountCreationModelImpl model = ((AccountCreationModelImpl) accountCreationModel);
        Account newAccount = model.getNewAccount();
        mPhotoView.setImageDrawable(new AvatarDrawable(getContext(), model.getPhoto(), accountCreationModel.getFullName(), accountCreationModel.getUsername(), newAccount == null ? null : newAccount.getUsername(), true));
    }

    @OnTextChanged(value = R.id.user_name, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void afterTextChanged(Editable txt) {
        presenter.fullNameUpdated(txt.toString());
    }
}
