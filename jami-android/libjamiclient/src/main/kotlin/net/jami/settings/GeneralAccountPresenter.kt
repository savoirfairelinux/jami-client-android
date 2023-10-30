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
package net.jami.settings

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.HardwareService
import net.jami.services.PreferencesService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

class GeneralAccountPresenter @Inject internal constructor(
    private val mAccountService: AccountService,
    private val mHardwareService: HardwareService,
    private val mPreferenceService: PreferencesService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<GeneralAccountView>() {
    private var mAccount: Account? = null

    // Init with current account
    fun init() {
        init(mAccountService.currentAccount)
    }

    fun init(accountId: String?) {
        init(mAccountService.getAccount(accountId))
    }

    private fun init(account: Account?) {
        mCompositeDisposable.clear()
        mAccount = account
        if (account != null) {
            if (account.isJami) {
                view!!.addJamiPreferences(account.accountId)
            } else {
                view!!.addSipPreferences()
            }
            view!!.accountChanged(account)
            mCompositeDisposable.add(mAccountService.getObservableAccount(account.accountId)
                .observeOn(mUiScheduler)
                .subscribe { acc: Account -> view!!.accountChanged(acc) })
            mCompositeDisposable.add(
                mHardwareService.maxResolutions
                    .observeOn(mUiScheduler)
                    .subscribe({ res: Pair<Int?, Int?> ->
                            if (res.first == null || res.second == null) {
                                view?.updateResolutions(null, mPreferenceService.resolution)
                            } else {
                                view?.updateResolutions(Pair(res.first!!, res.second!!), mPreferenceService.resolution)
                            }
                        },
                        { view?.updateResolutions(null, mPreferenceService.resolution) })
            )
        } else {
            Log.e(TAG, "init: No currentAccount available")
            view?.finish()
        }
    }

    fun setEnabled(enabled: Boolean) {
        mAccount!!.isEnabled = enabled
        mAccountService.setAccountEnabled(mAccount!!.accountId, enabled)
    }

    fun twoStatePreferenceChanged(configKey: ConfigKey, newValue: Any) {
        if (configKey === ConfigKey.ACCOUNT_ENABLE) {
            setEnabled(newValue as Boolean)
        } else {
            mAccount!!.setDetail(configKey, newValue.toString())
            updateAccount()
        }
    }

    fun passwordPreferenceChanged(configKey: ConfigKey, newValue: Any) {
        if (mAccount!!.isSip) {
            mAccount!!.credentials[0].setDetail(configKey, newValue.toString())
        }
        updateAccount()
    }

    fun userNameChanged(configKey: ConfigKey, newValue: Any) {
        if (mAccount!!.isSip) {
            mAccount!!.setDetail(configKey, newValue.toString())
            mAccount!!.credentials[0].setDetail(configKey, newValue.toString())
        }
        updateAccount()
    }

    fun preferenceChanged(configKey: ConfigKey, newValue: Any) {
        mAccount!!.setDetail(configKey, newValue.toString())
        updateAccount()
    }

    private fun updateAccount() {
        mAccountService.setCredentials(mAccount!!.accountId, mAccount!!.credentialsHashMapList)
        mAccountService.setAccountDetails(mAccount!!.accountId, mAccount!!.details)
    }

    fun removeAccount() {
        mAccountService.removeAccount(mAccount!!.accountId)
    }

    companion object {
        private val TAG = GeneralAccountPresenter::class.simpleName!!
    }
}