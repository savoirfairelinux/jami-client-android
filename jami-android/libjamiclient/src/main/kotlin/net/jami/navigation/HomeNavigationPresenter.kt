/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.navigation

import io.reactivex.rxjava3.core.Scheduler
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.VCardService
import net.jami.utils.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class HomeNavigationPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mHardwareService: HardwareService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mVCardService: VCardService,
    @param:Named("UiScheduler")
    private val mUiScheduler: Scheduler
) : RootPresenter<HomeNavigationView>() {

    override fun bindView(view: HomeNavigationView) {
        super.bindView(view)
        mCompositeDisposable.add(mAccountService.currentProfileAccountSubject
            .observeOn(mUiScheduler)
            .subscribe({ accountProfile ->
                this.view?.showViewModel(HomeNavigationViewModel(accountProfile.first, accountProfile.second))
            }) { e: Throwable -> Log.e(TAG, "Error loading account list !", e) })
    }

    fun cameraClicked() {
        if (mDeviceRuntimeService.hasVideoPermission())
            view?.gotToImageCapture()
        else
            view?.askCameraPermission()
    }

    fun galleryClicked() {
        view?.goToGallery()
    }

    fun updateProfile(displayName: String, avatar: String? = null, fileType: String = "") {
        val account = mAccountService.currentAccount ?: return
        if (avatar == null)
            mAccountService.updateProfile(account.accountId, displayName, "", "")
        else
            mAccountService.updateProfile(account.accountId, displayName, avatar, fileType)
    }
    fun updateProfile(displayName: String, avatar: File, fileType: String) {
        val account = mAccountService.currentAccount ?: return
        mAccountService.updateProfile(account.accountId, displayName, avatar, fileType)
    }

    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .subscribe()
        }
    }

    companion object {
        private val TAG = HomeNavigationPresenter::class.simpleName!!
    }
}