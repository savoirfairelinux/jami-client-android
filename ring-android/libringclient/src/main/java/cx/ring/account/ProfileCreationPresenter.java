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
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Observable;

public class ProfileCreationPresenter extends RootPresenter<ProfileCreationView> {

    public static final String TAG = ProfileCreationPresenter.class.getSimpleName();

    protected DeviceRuntimeService mDeviceRuntimeService;
    private AccountCreationModel mAccountCreationModel;
    private Account account = null;

    @Inject
    public ProfileCreationPresenter(DeviceRuntimeService deviceRuntimeService) {
        mDeviceRuntimeService = deviceRuntimeService;
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
        Observable<Account> accountObservable = accountCreationModel.getAccountObservable();
        if (accountObservable != null) {
            mCompositeDisposable.add(accountCreationModel
                    .getProfileUpdates()
                    .subscribe(model -> {
                        Log.w(TAG, "setProfile");
                        getView().setProfile(model);
                    }));
        } else {
            Log.w(TAG, "no account observable !");
        }
    }

    public void fullNameUpdated(String fullName) {
        mAccountCreationModel.setFullName(fullName);
    }

    public void photoUpdated() {
        ProfileCreationView view = getView();
        if (view != null)
            view.photoUpdate(mAccountCreationModel);
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

    public void nextClick() {
        getView().goToNext(mAccountCreationModel, true);
    }

    public void skipClick() {
        getView().goToNext(mAccountCreationModel, false);
    }
}
