/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragAccProfileCreateBinding;

import net.jami.account.ProfileCreationPresenter;
import net.jami.account.ProfileCreationView;
import net.jami.model.Account;
import net.jami.mvp.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.core.Single;

@AndroidEntryPoint
public class ProfileCreationFragment extends BaseSupportFragment<ProfileCreationPresenter, ProfileCreationView> implements ProfileCreationView {
    public static final String TAG = ProfileCreationFragment.class.getSimpleName();

    public static final int REQUEST_CODE_PHOTO = 1;
    public static final int REQUEST_CODE_GALLERY = 2;
    public static final int REQUEST_PERMISSION_CAMERA = 3;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 4;

    private AccountCreationModel model;
    private Uri tmpProfilePhotoUri;
    private FragAccProfileCreateBinding binding;

    public static ProfileCreationFragment newInstance(AccountCreationModelImpl model) {
        ProfileCreationFragment fragment = new ProfileCreationFragment();
        fragment.model = model;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccProfileCreateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);

        if (model == null) {
            getActivity().finish();
            return;
        }
        if (binding.profilePhoto.getDrawable() == null) {
            binding.profilePhoto.setImageDrawable(
                    new AvatarDrawable.Builder()
                            .withNameData(model.getFullName(), model.getUsername())
                            .withCircleCrop(true)
                            .build(view.getContext())
            );
        }
        presenter.initPresenter(model);

        binding.gallery.setOnClickListener(v -> presenter.galleryClick());
        binding.camera.setOnClickListener(v -> presenter.cameraClick());
        binding.nextCreateAccount.setOnClickListener(v -> presenter.nextClick());
        binding.skipCreateAccount.setOnClickListener(v -> presenter.skipClick());
        binding.username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.fullNameUpdated(s.toString());
            }
        });
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

    @Override
    public void displayProfileName(String profileName) {
        binding.username.setText(profileName);
    }

    @Override
    public void goToGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE_GALLERY);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.gallery_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void goToPhotoCapture() {
        try {
            Context context = requireContext();
            File file = AndroidFileUtils.createImageFile(context);
            Uri uri = ContentUriHandler.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, file);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .putExtra("android.intent.extras.CAMERA_FACING", 1)
                    .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    .putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            tmpProfilePhotoUri = uri;
            startActivityForResult(intent, REQUEST_CODE_PHOTO);
        } catch (IOException e) {
            Log.e(TAG, "Can't create temp file", e);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Could not start activity");
        }
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
        binding.profilePhoto.setImageDrawable(
                new AvatarDrawable.Builder()
                        .withPhoto(model.getPhoto())
                        .withNameData(accountCreationModel.getFullName(), accountCreationModel.getUsername())
                        .withId(newAccount == null ? null : newAccount.getUsername())
                        .withCircleCrop(true)
                        .build(getContext())
        );
    }

    public AccountCreationModel getModel() {
        return model;
    }
}
