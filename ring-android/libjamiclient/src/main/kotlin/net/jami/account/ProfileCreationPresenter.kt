/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
package net.jami.account

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import net.jami.model.AccountCreationModel
import net.jami.mvp.RootPresenter
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

class ProfileCreationPresenter @Inject constructor(
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mHardwareService: HardwareService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<ProfileCreationView>() {
    private var mAccountCreationModel: AccountCreationModel? = null

    fun initPresenter(accountCreationModel: AccountCreationModel) {
        Log.w(TAG, "initPresenter")
        mAccountCreationModel = accountCreationModel
        if (mDeviceRuntimeService.hasContactPermission()) {
            val profileName = mDeviceRuntimeService.profileName
            if (profileName != null)
                view?.displayProfileName(profileName)
        } else {
            Log.d(TAG, "READ_CONTACTS permission is not granted.")
        }
        mCompositeDisposable.add(accountCreationModel
            .profileUpdates
            .observeOn(mUiScheduler)
            .subscribe { model -> view?.setProfile(model) })
    }

    fun fullNameUpdated(fullName: String) {
        mAccountCreationModel?.fullName = fullName
    }

    fun photoUpdated(bitmap: Single<Any>) {
        mCompositeDisposable.add(bitmap
            .subscribe({ b: Any -> mAccountCreationModel?.photo = b })
            { e: Throwable -> Log.e(TAG, "Can't load image", e) })
    }

    fun galleryClick() {
        val hasPermission = mDeviceRuntimeService.hasGalleryPermission()
        if (hasPermission) {
            view?.goToGallery()
        } else {
            view?.askStoragePermission()
        }
    }

    fun cameraClick() {
        if (mDeviceRuntimeService.hasVideoPermission()) {
            view?.goToPhotoCapture()
        } else {
            view?.askPhotoPermission()
        }
    }

    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .subscribe()
        }
    }

    fun nextClick() {
        view?.goToNext(mAccountCreationModel!!, true)
    }

    fun skipClick() {
        view?.goToNext(mAccountCreationModel!!, false)
    }

    companion object {
        val TAG = ProfileCreationPresenter::class.simpleName!!
    }
}