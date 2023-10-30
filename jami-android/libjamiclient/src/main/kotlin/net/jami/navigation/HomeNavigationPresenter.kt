/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import ezvcard.VCard
import ezvcard.property.Photo
import ezvcard.property.RawProperty
import ezvcard.property.Uid
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.VCardService
import net.jami.utils.Log
import net.jami.utils.VCardUtils
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

    fun saveVCardPhoto(photo: Single<Photo>) {
        val account = mAccountService.currentAccount!!
        val accountId = account.accountId
        val ringId = account.username
        val filesDir = mDeviceRuntimeService.provideFilesDir()
        mCompositeDisposable.add(Single.zip(
            VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId).subscribeOn(Schedulers.io()),
            photo.subscribeOn(Schedulers.io())
        ) { vcard: VCard, pic: Photo ->
            vcard.apply {
                uid = Uid(ringId)
                removeProperties(Photo::class.java)
                addPhoto(pic)
                removeProperties(RawProperty::class.java)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe({ vcard ->
                account.loadedProfile = mVCardService.loadVCardProfile(vcard).cache()
                VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }) { e: Throwable -> Log.e(TAG, "Error saving vCard !", e) })
    }

    fun saveVCardFormattedName(username: String?) {
        val account = mAccountService.currentAccount!!
        val accountId = account.accountId
        val filesDir = mDeviceRuntimeService.provideFilesDir()
        mCompositeDisposable.add(VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId)
            .doOnSuccess { vcard: VCard ->
                vcard.setFormattedName(username)
                vcard.removeProperties(RawProperty::class.java)
            }
            .subscribeOn(Schedulers.io())
            .subscribe({ vcard ->
                account.loadedProfile = mVCardService.loadVCardProfile(vcard).cache()
                VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }) { e: Throwable -> Log.e(TAG, "Error saving vCard !", e) })
    }

    fun saveVCard(account: Account, username: String?, photo: Single<Photo>) {
        val accountId = account.accountId
        val ringId = account.username
        val filesDir = mDeviceRuntimeService.provideFilesDir()
        mCompositeDisposable.add(Single.zip(
            VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId).subscribeOn(Schedulers.io()),
            photo
        ) { vcard: VCard, pic: Photo ->
            vcard.uid = Uid(ringId)
            if (!username.isNullOrEmpty()) {
                vcard.setFormattedName(username)
            }
            vcard.removeProperties(Photo::class.java)
            vcard.addPhoto(pic)
            vcard.removeProperties(RawProperty::class.java)
            account.loadedProfile = mVCardService.loadVCardProfile(vcard).cache()
            vcard
        }
            .flatMap { vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir) }
            .subscribeOn(Schedulers.io())
            .subscribe({}) { e: Throwable -> Log.e(TAG, "Error saving vCard !", e) })
    }

    fun getUri(account: Account, defaultNameSip: CharSequence): String? =
        if (account.isIP2IP) defaultNameSip.toString() else account.displayUri

    fun cameraClicked() {
        if (mDeviceRuntimeService.hasVideoPermission())
            view?.gotToImageCapture()
        else
            view?.askCameraPermission()
    }

    fun galleryClicked() {
        view?.goToGallery()
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