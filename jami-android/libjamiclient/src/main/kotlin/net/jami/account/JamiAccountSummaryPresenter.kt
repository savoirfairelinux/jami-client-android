/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package net.jami.account

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
import java.io.File
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Named

class JamiAccountSummaryPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mHardwareService: HardwareService,
    private val mVcardService: VCardService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<JamiAccountSummaryView>() {
    private var mAccountID: String? = null

    fun registerName(name: String, scheme: String, password: String) {
        val account = mAccountService.getAccount(mAccountID) ?: return
        mAccountService.registerName(account, name, scheme, password)
    }

    fun startAccountExport(password: String?) {
        if (view == null || mAccountID == null) {
            return
        }
        view?.showExportingProgressDialog()
        mCompositeDisposable.add(mAccountService
            .exportOnRing(mAccountID!!, password!!)
            .observeOn(mUiScheduler)
            .subscribe({ pin: String -> view?.showPIN(pin) }) { error: Throwable ->
                when (error) {
                    is IllegalArgumentException -> view?.showPasswordError()
                    is SocketException -> view?.showNetworkError()
                    else -> view?.showGenericError()
                }
            })
    }

    fun setAccountId(accountId: String) {
        mCompositeDisposable.clear()
        mAccountID = accountId
        mCompositeDisposable.add(mAccountService.getObservableAccountProfile(accountId)
            .observeOn(mUiScheduler)
            .subscribe({ a -> view?.accountChanged(a.first, a.second) }) { e -> Log.e(TAG, "Can't load account") })
    }

    fun enableAccount(newValue: Boolean) {
        val account = mAccountService.getAccount(mAccountID)
        if (account == null) {
            Log.w(TAG, "account not found!")
            return
        }
        account.isEnabled = newValue
        mAccountService.setAccountEnabled(account.accountId, newValue)
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val accountId = mAccountID ?: return
        view?.showPasswordProgressDialog()
        mCompositeDisposable.add(mAccountService.setAccountPassword(accountId, oldPassword, newPassword)
            .observeOn(mUiScheduler)
            .subscribe({ view?.passwordChangeEnded(accountId, true, newPassword) })
            { view?.passwordChangeEnded(accountId, false) })
    }

    val deviceName: String?
        get() = mAccountService.getAccount(mAccountID)?.deviceName

    fun downloadAccountsArchive(dest: File, scheme: String, password: String?) {
        val accountId = mAccountID ?: return
        view?.showExportingProgressDialog()
        mCompositeDisposable.add(
            mAccountService.exportToFile(accountId, dest.absolutePath, scheme, password!!)
                .observeOn(mUiScheduler)
                .subscribe({ view?.displayCompleteArchive(dest) })
                { view?.passwordChangeEnded(accountId, false) })
    }

    fun saveVCardFormattedName(username: String?) {
        val accountId = mAccountID ?: return
        val account = mAccountService.getAccount(accountId)
        val filesDir = mDeviceRuntimeService.provideFilesDir()
        mCompositeDisposable.add(VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId)
            .doOnSuccess { vcard: VCard ->
                val previousName = vcard.formattedName?.value
                if ((previousName.isNullOrEmpty() && username.isNullOrEmpty()) || previousName == username)
                    throw IllegalArgumentException("Name didn't change")
                vcard.setFormattedName(username)
                vcard.removeProperties(RawProperty::class.java)
                account?.loadedProfile = mVcardService.loadVCardProfile(vcard).cache()
            }
            .flatMap { vcard: VCard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir) }
            .subscribeOn(Schedulers.io())
            .subscribe({}) { e: Throwable -> Log.e(TAG, "Error saving vCard " + e.message) })
    }

    /**
     * Save the vCard to the disk.
     * @param username: the username to save, if null, the username will be removed from the vCard
     * @param photo: the photo to save, if null, the photo will be removed from the vCard
     */
    fun saveVCard(username: String?, photo: Single<Photo>?) {
        val accountId = mAccountID ?: return
        val account = mAccountService.getAccount(accountId)!!
        val ringId = account.username
        val filesDir = mDeviceRuntimeService.provideFilesDir()

        if (photo == null) {
            mCompositeDisposable.add(
                VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId)
                    .subscribeOn(Schedulers.io())
                    .map { vcard: VCard ->
                        vcard.uid = Uid(ringId)
                        if (!username.isNullOrEmpty()) vcard.setFormattedName(username)
                        vcard.removeProperties(Photo::class.java)
                        vcard.removeProperties(RawProperty::class.java)
                        vcard
                    }
                    .flatMap { vcard: VCard ->
                        VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir)
                    }
                    .subscribe({ vcard: VCard ->
                        account.loadedProfile = mVcardService.loadVCardProfile(vcard).cache()
                    }) { e: Throwable -> Log.e(TAG, "Error saving vCard !", e) }
            )
        } else {
            mCompositeDisposable.add(
                Single.zip(
                    VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId)
                        .subscribeOn(Schedulers.io()),
                    photo
                ) { vcard: VCard, pic: Photo ->
                    vcard.uid = Uid(ringId)
                    if (!username.isNullOrEmpty()) vcard.setFormattedName(username)
                    vcard.removeProperties(Photo::class.java)
                    vcard.addPhoto(pic)
                    vcard.removeProperties(RawProperty::class.java)
                    vcard
                }
                    .flatMap { vcard: VCard ->
                        VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir)
                    }
                    .subscribeOn(Schedulers.io())
                    .subscribe({ vcard: VCard ->
                        account.loadedProfile = mVcardService.loadVCardProfile(vcard).cache()
                    }) { e: Throwable -> Log.e(TAG, "Error saving vCard !", e) })
        }
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

    fun goToAccount() {
        view?.goToAccount(mAccountID!!)
    }

    fun goToMedia() {
        view?.goToMedia(mAccountID!!)
    }

    fun goToSystem() {
        view?.goToSystem(mAccountID!!)
    }

    fun goToAdvanced() {
        view?.goToAdvanced(mAccountID!!)
    }

    fun revokeDevice(deviceId: String, scheme: String, password: String) {
        view?.showRevokingProgressDialog()
        mCompositeDisposable.add(mAccountService
            .revokeDevice(mAccountID!!, deviceId, scheme, password)
            .observeOn(mUiScheduler)
            .subscribe { result: Int ->
                val account = mAccountService.getAccount(mAccountID)!!
                view?.deviceRevocationEnded(deviceId, result)
                view?.updateDeviceList(account.devices, account.deviceId)
            })
    }

    fun renameDevice(newName: String) {
        mAccountService.renameDevice(mAccountID!!, newName)
    }

    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .blockingAwait()
        }
    }

    val account: Account?
        get() = mAccountService.getAccount(mAccountID)

    companion object {
        private val TAG = JamiAccountSummaryPresenter::class.simpleName!!
    }
}