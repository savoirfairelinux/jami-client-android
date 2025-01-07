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
package net.jami.settings

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.utils.Log
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class AdvancedAccountPresenter @Inject constructor(
    private var accountService: AccountService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<AdvancedAccountView>() {
    private var mAccount: Account? = null
    fun init(accountId: String) {
        mAccount = accountService.getAccount(accountId)?.also { account ->
            view?.initView(account.config, networkInterfaces)
            mCompositeDisposable.add(account.volatileDetails
                .observeOn(uiScheduler)
                .subscribe { details ->
                    view?.updateVolatileDetails(details)
                })
        }
    }

    fun twoStatePreferenceChanged(configKey: ConfigKey, newValue: Any) {
        mAccount!!.setDetail(configKey, newValue.toString())
        updateAccount()
    }

    fun passwordPreferenceChanged(configKey: ConfigKey, newValue: Any) {
        mAccount!!.setDetail(configKey, newValue.toString())
        updateAccount()
    }

    fun preferenceChanged(configKey: ConfigKey, newValue: Any) {
        var newValue = newValue
        if (configKey === ConfigKey.AUDIO_PORT_MAX || configKey === ConfigKey.AUDIO_PORT_MIN) {
            newValue = adjustRtpRange(Integer.valueOf(newValue as String))
        }
        mAccount!!.setDetail(configKey, newValue.toString())
        updateAccount()
    }

    private fun updateAccount() {
        accountService.setCredentials(mAccount!!.accountId, mAccount!!.credentialsHashMapList)
        accountService.setAccountDetails(mAccount!!.accountId, mAccount!!.details)
    }

    private fun adjustRtpRange(newValue: Int): String {
        if (newValue < 1024) {
            return "1024"
        }
        return if (newValue > 65534) "65534" else newValue.toString()
    }

    private val networkInterfaces: ArrayList<CharSequence>
        get() {
            val result = ArrayList<CharSequence>()
            result.add("default")
            try {
                val list = NetworkInterface.getNetworkInterfaces()
                while (list.hasMoreElements()) {
                    val i = list.nextElement()
                    if (i.isUp) {
                        result.add(i.displayName)
                    }
                }
            } catch (e: SocketException) {
                Log.e(TAG, "Error enumerating interfaces: ", e)
            }
            return result
        }

    companion object {
        val TAG = AdvancedAccountPresenter::class.simpleName!!
    }
}