/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import net.jami.services.VCardService
import net.jami.utils.Log
import net.jami.utils.StringUtils.isEmpty
import net.jami.utils.VCardUtils.loadLocalProfileFromDiskWithDefault
import net.jami.utils.VCardUtils.saveLocalProfileToDisk
import java.io.File
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Named

class JamiAccountSummaryPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mVcardService: VCardService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<JamiAccountSummaryView>() {
    private var mAccountID: String? = null

    fun registerName(name: String?, password: String?) {
        val account = mAccountService.getAccount(mAccountID) ?: return
        mAccountService.registerName(account, password, name)
        view?.accountChanged(account)
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
        mAccountService.getAccount(accountId)?.let { account -> view?.accountChanged(account) }
        mCompositeDisposable.add(mAccountService.getObservableAccountUpdates(accountId)
            .observeOn(mUiScheduler)
            .subscribe { a: Account -> view?.accountChanged(a) })
    }

    fun enableAccount(newValue: Boolean) {
        val account = mAccountService.getAccount(mAccountID)
        if (account == null) {
            Log.w(TAG, "account not found!")
            return
        }
        account.isEnabled = newValue
        mAccountService.setAccountEnabled(account.accountID, newValue)
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        view?.showPasswordProgressDialog()
        mCompositeDisposable.add(mAccountService.setAccountPassword(mAccountID!!, oldPassword, newPassword)
            .observeOn(mUiScheduler)
            .subscribe({ view?.passwordChangeEnded(true) })
            { view?.passwordChangeEnded(false) })
    }

    val deviceName: String?
        get() {
            val account = mAccountService.getAccount(mAccountID)
            if (account == null) {
                Log.w(TAG, "account not found!")
                return null
            }
            return account.deviceName
        }

    fun downloadAccountsArchive(dest: File, password: String?) {
        view?.showExportingProgressDialog()
        mCompositeDisposable.add(
            mAccountService.exportToFile(mAccountID!!, dest.absolutePath, password!!)
                .observeOn(mUiScheduler)
                .subscribe({ view?.displayCompleteArchive(dest) })
                { view?.passwordChangeEnded(false) })
    }

    fun saveVCardFormattedName(username: String?) {
        val account = mAccountService.getAccount(mAccountID)
        val filesDir = mDeviceRuntimeService.provideFilesDir()
        mCompositeDisposable.add(loadLocalProfileFromDiskWithDefault(filesDir, mAccountID!!)
            .doOnSuccess { vcard: VCard ->
                vcard.setFormattedName(username)
                vcard.removeProperties(RawProperty::class.java)
            }
            .flatMap { vcard: VCard -> saveLocalProfileToDisk(vcard, mAccountID!!, filesDir) }
            .subscribeOn(Schedulers.io())
            .subscribe({ vcard: VCard -> account?.loadedProfile = mVcardService.loadVCardProfile(vcard).cache() })
            { e: Throwable -> Log.e(TAG, "Error saving vCard !", e) })
    }

    fun saveVCard(username: String?, photo: Single<Photo>) {
        val account = mAccountService.getAccount(mAccountID)!!
        val ringId = account.username
        val filesDir = mDeviceRuntimeService.provideFilesDir()
        mCompositeDisposable.add(Single.zip(
            loadLocalProfileFromDiskWithDefault(filesDir, mAccountID!!).subscribeOn(Schedulers.io()),
            photo, { vcard: VCard, pic: Photo ->
                vcard.uid = Uid(ringId)
                if (!isEmpty(username)) {
                    vcard.setFormattedName(username)
                }
                vcard.removeProperties(Photo::class.java)
                vcard.addPhoto(pic)
                vcard.removeProperties(RawProperty::class.java)
                vcard
            })
            .flatMap { vcard: VCard -> saveLocalProfileToDisk(vcard, mAccountID!!, filesDir) }
            .subscribeOn(Schedulers.io())
            .subscribe({ vcard: VCard -> account.loadedProfile = mVcardService.loadVCardProfile(vcard).cache() })
            { e: Throwable -> Log.e(TAG, "Error saving vCard !", e) })
    }

    fun cameraClicked() {
        val hasPermission = mDeviceRuntimeService.hasVideoPermission() && mDeviceRuntimeService.hasWriteExternalStoragePermission()
        val view = view
        if (view != null) {
            if (hasPermission) {
                view.gotToImageCapture()
            } else {
                view.askCameraPermission()
            }
        }
    }

    fun galleryClicked() {
        val hasPermission = mDeviceRuntimeService.hasGalleryPermission()
        if (hasPermission) {
            view!!.goToGallery()
        } else {
            view!!.askGalleryPermission()
        }
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

    fun revokeDevice(deviceId: String?, password: String?) {
        view?.showRevokingProgressDialog()
        mCompositeDisposable.add(mAccountService
            .revokeDevice(mAccountID!!, password!!, deviceId!!)
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

    val account: Account?
        get() = mAccountService.getAccount(mAccountID)

    companion object {
        private val TAG = JamiAccountSummaryPresenter::class.simpleName!!
    }
}