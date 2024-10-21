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
package net.jami.account

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.VCardService
import net.jami.utils.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class JamiAccountSummaryPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mHardwareService: HardwareService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<JamiAccountSummaryView>() {
    private var mAccountID: String? = null

    fun registerName(name: String, scheme: String, password: String) {
        val account = mAccountService.getAccount(mAccountID) ?: return
        mAccountService.registerName(account, name, scheme, password)
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

    fun downloadAccountsArchive(dest: File, scheme: String, password: String) {
        val accountId = mAccountID ?: return
        view?.showExportingProgressDialog()
        mCompositeDisposable.add(
            mAccountService.exportToFile(accountId, dest.absolutePath, scheme, password)
                .observeOn(mUiScheduler)
                .subscribe({ view?.displayCompleteArchive(dest) })
                { view?.passwordChangeEnded(accountId, false) })
    }

    fun updateProfile(displayName: String, avatar: String? = null, fileType: String = "") {
        val accountId = mAccountID ?: return
        if (avatar == null)
            mAccountService.updateProfile(accountId, displayName)
        else
            mAccountService.updateProfile(accountId, displayName, avatar, fileType)
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