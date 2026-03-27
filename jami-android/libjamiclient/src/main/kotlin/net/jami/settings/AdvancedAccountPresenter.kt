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
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.services.PreferencesService
import net.jami.utils.Log
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class AdvancedAccountPresenter @Inject constructor(
    private var accountService: AccountService,
    private val preferencesService: PreferencesService,
    private val deviceService: DeviceRuntimeService,
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
        mAccount?.setDetail(configKey, newValue.toString())
        updateAccount()
    }

    fun passwordPreferenceChanged(configKey: ConfigKey, newValue: Any) {
        mAccount?.setDetail(configKey, newValue.toString())
        updateAccount()
    }

    fun preferenceChanged(configKey: ConfigKey, newValue: Any) {
        val value = if (configKey === ConfigKey.AUDIO_PORT_MAX || configKey === ConfigKey.AUDIO_PORT_MIN) {
            try {
                adjustRtpRange(Integer.valueOf(newValue as String))
            } catch (_: NumberFormatException) {
                return
            }
        } else {
            newValue.toString()
        }
        mAccount?.setDetail(configKey, value)
        updateAccount()
    }

    fun resetToDefaults() {
        val account = mAccount ?: return
        mCompositeDisposable.add(
            accountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
                .observeOn(uiScheduler)
                .subscribe({ templateDetails ->
                    val resetKeys = listOf(
                        ConfigKey.RINGNS_HOST,
                        ConfigKey.ACCOUNT_HOSTNAME,
                        ConfigKey.PROXY_SERVER,
                        ConfigKey.PROXY_LIST_ENABLED,
                        ConfigKey.PROXY_SERVER_LIST,
                        ConfigKey.ACCOUNT_PEER_DISCOVERY,
                        ConfigKey.ACCOUNT_UPNP_ENABLE,
                        ConfigKey.TURN_ENABLE,
                        ConfigKey.TURN_SERVER,
                        ConfigKey.TURN_USERNAME,
                        ConfigKey.TURN_PASSWORD,
                        ConfigKey.AUDIO_PORT_MIN,
                        ConfigKey.AUDIO_PORT_MAX,
                    )
                    for (key in resetKeys) {
                        templateDetails[key.key]?.let { account.setDetail(key, it) }
                    }
                    account.setDetail(ConfigKey.ACCOUNT_UPNP_ENABLE, AccountConfig.TRUE_STR)
                    val pushEnabled = preferencesService.settings.enablePushNotifications
                    account.setDetail(ConfigKey.PROXY_ENABLED, if (pushEnabled) AccountConfig.TRUE_STR else AccountConfig.FALSE_STR)
                    if (pushEnabled) {
                        deviceService.pushToken?.let { (token, topic) ->
                            account.setDetail(ConfigKey.PROXY_PUSH_TOKEN, token)
                            account.setDetail(ConfigKey.PROXY_PUSH_TOPIC, topic)
                            account.setDetail(ConfigKey.PROXY_PUSH_PLATFORM, deviceService.pushPlatform)
                        }
                    }
                    updateAccount()
                    view?.refreshView(account.config)
                }, { e -> Log.e(TAG, "resetToDefaults failed", e) })
        )
    }

    private fun updateAccount() {
        val account = mAccount ?: return
        accountService.setCredentials(account.accountId, account.credentialsHashMapList)
        accountService.setAccountDetails(account.accountId, account.details)
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