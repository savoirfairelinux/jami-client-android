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

import io.reactivex.rxjava3.core.Scheduler
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

/**
 * This this the old device side.
 */
class LinkAccountExportPresenter @Inject constructor(
    private val accountService: AccountService,
    @Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<LinkAccountExportView>() {
    private var mAccountID: String? = null

    fun setAccountId(accountID: String) {
        mCompositeDisposable.clear()
        mAccountID = accountID
    }

    fun exportToPeer(token: String) {
        Log.w("devdebug", "LinkDevicePresenter exportToPeer token: $token")
        accountService.exportToPeer(mAccountID!!, token)
        accountService.deviceAuthStateObservable
            .filter { it.accountId == mAccountID }
            .subscribe {
                updateDeviceAuthState(it)
            }.apply { mCompositeDisposable.add(this) }
    }

    private fun updateDeviceAuthState(deviceAuthResult: AccountService.DeviceAuthResult) {
        Log.w("devdebug", "LinkDevicePresenter deviceAuthStateObservable: ${deviceAuthResult.state}")
    }

    companion object {
        private val TAG = LinkAccountExportPresenter::class.simpleName!!
    }
}