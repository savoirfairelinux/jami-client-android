/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.account;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

public class ProfileCreationPresenter extends RootPresenter<ProfileCreationView> {

    public static final String TAG = ProfileCreationPresenter.class.getSimpleName();

    private final DeviceRuntimeService mDeviceRuntimeService;
    private final HardwareService mHardwareService;
    private final Scheduler mUiScheduler;

    private AccountCreationModel mAccountCreationModel;

    @Inject
    public ProfileCreationPresenter(DeviceRuntimeService deviceRuntimeService, HardwareService hardwareService, Scheduler uiScheduler) {
        mDeviceRuntimeService = deviceRuntimeService;
        mHardwareService = hardwareService;
        mUiScheduler = uiScheduler;
    }

    public void initPresenter(AccountCreationModel accountCreationModel) {
        Log.w(TAG, "initPresenter");
        mAccountCreationModel = accountCreationModel;
        if (mDeviceRuntimeService.hasContactPermission()) {
            String profileName = mDeviceRuntimeService.getProfileName();
            if (profileName != null) {
                getView().displayProfileName(profileName);
            }
        } else {
            Log.d(TAG, "READ_CONTACTS permission is not granted.");
        }
        mCompositeDisposable.add(accountCreationModel
                .getProfileUpdates()
                .observeOn(mUiScheduler)
                .subscribe(model -> {
                    ProfileCreationView view = getView();
                    if (view != null)
                        view.setProfile(model);
                }));
    }

    public void fullNameUpdated(String fullName) {
        if (mAccountCreationModel != null)
            mAccountCreationModel.setFullName(fullName);
    }

    public void photoUpdated(Single<Object> bitmap) {
        mCompositeDisposable.add(bitmap
                .subscribe(b -> mAccountCreationModel.setPhoto(b),
                           e -> Log.e(TAG, "Can't load image", e)));
    }

    public void galleryClick() {
        boolean hasPermission = mDeviceRuntimeService.hasGalleryPermission();
        if (hasPermission) {
            getView().goToGallery();
        } else {
            getView().askStoragePermission();
        }
    }

    public void cameraClick() {
        boolean hasPermission = mDeviceRuntimeService.hasVideoPermission() &&
                mDeviceRuntimeService.hasWriteExternalStoragePermission();
        if (hasPermission) {
            getView().goToPhotoCapture();
        } else {
            getView().askPhotoPermission();
        }
    }

    public void cameraPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.isVideoAvailable()) {
            mHardwareService.initVideo().subscribe();
        }
    }

    public void nextClick() {
        getView().goToNext(mAccountCreationModel, true);
    }

    public void skipClick() {
        getView().goToNext(mAccountCreationModel, false);
    }
}
