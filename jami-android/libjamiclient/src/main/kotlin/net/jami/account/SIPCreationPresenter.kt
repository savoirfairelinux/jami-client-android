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
package net.jami.account

import ezvcard.VCard
import ezvcard.property.FormattedName
import ezvcard.property.RawProperty
import ezvcard.property.Uid
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.DisposableObserver
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.Profile
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.utils.Log
import net.jami.utils.VCardUtils
import javax.inject.Inject
import javax.inject.Named

class SIPCreationPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mDeviceService: DeviceRuntimeService,
    @Named("UiScheduler")
    private var mUiScheduler: Scheduler
) : RootPresenter<SIPCreationView>() {
    private var mAccount: Account? = null

    /**
     * Attempts to register the account specified by the form. If there are form errors (invalid or missing fields, etc.), the
     * errors are presented and no actual creation attempt is made.
     *
     * @param hostname      hostname account value
     * @param username      username account value
     * @param password      password account value
     * @param bypassWarning Report eventual warning to the user
     */
    fun startCreation(hostname: String?, proxy: String?, username: String?, password: String?, bypassWarning: Boolean) {
        view?.resetErrors()

        // Store values at the time of the login attempt.
        var warningIPAccount = false
        if (hostname != null && hostname.isEmpty()) {
            warningIPAccount = true
        }
        if (!warningIPAccount && (password == null || password.trim { it <= ' ' }.isEmpty())) {
            view?.showPasswordError()
            return
        }
        if (!warningIPAccount && (username == null || username.trim { it <= ' ' }.isEmpty())) {
            view?.showUsernameError()
            return
        }
        if (warningIPAccount && !bypassWarning) {
            view?.showIP2IPWarning()
        } else {
            val accountDetails = initAccountDetails()
            if (username != null)
                accountDetails[ConfigKey.ACCOUNT_ALIAS.key] = username
            if (hostname != null && hostname.isNotEmpty()) {
                accountDetails[ConfigKey.ACCOUNT_HOSTNAME.key] = hostname
                if (proxy != null)
                    accountDetails[ConfigKey.ACCOUNT_ROUTESET.key] = proxy
                if (username != null)
                    accountDetails[ConfigKey.ACCOUNT_USERNAME.key] = username
                if (password != null)
                    accountDetails[ConfigKey.ACCOUNT_PASSWORD.key] = password
            }
            accountDetails[ConfigKey.SRTP_KEY_EXCHANGE.key] = ""
            registerAccount(accountDetails)
        }
    }

    fun removeAccount() {
        mAccount?.let { account ->
            mAccountService.removeAccount(account.accountId)
            mAccount = null
        }
    }

    private fun registerAccount(accountDetails: Map<String, String>) {
        view?.showLoading()
        mCompositeDisposable.add(
            mAccountService.addAccount(accountDetails)
                .observeOn(mUiScheduler)
                .subscribeWith(object : DisposableObserver<Account>() {
                    override fun onNext(account: Account) {
                        mAccount = account
                        when (account.registrationState) {
                            AccountConfig.RegistrationState.REGISTERED -> {
                                saveProfile(account.accountId)
                                view?.showRegistrationSuccess()
                                dispose()
                            }
                            AccountConfig.RegistrationState.ERROR_NETWORK -> {
                                view?.showRegistrationNetworkError()
                                dispose()
                            }
                            AccountConfig.RegistrationState.TRYING, AccountConfig.RegistrationState.UNREGISTERED -> return
                            else -> {
                                view?.showRegistrationError()
                                dispose()
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        view?.showRegistrationError()
                        dispose()
                    }

                    override fun onComplete() {
                        dispose()
                    }
                })
        )
    }

    private fun initAccountDetails(): MutableMap<String, String> {
        val accountDetails: MutableMap<String, String> =
            mAccountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_SIP).blockingGet()
        for ((key, value) in accountDetails) {
            Log.d(TAG, "Default account detail: $key -> $value")
        }
        accountDetails[ConfigKey.VIDEO_ENABLED.key] = true.toString()

        //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
        //~ This will have to be removed when it will be supported.
        accountDetails[ConfigKey.ACCOUNT_DTMF_TYPE.key] = "sipinfo"
        return accountDetails
    }

    private fun saveProfile(accountID: String) {
        val account = mAccount ?: return
        val vcard = VCard()
        var formattedName = account.username
        if (formattedName == null || formattedName.isEmpty()) {
            formattedName = account.alias
        }
        vcard.formattedName = FormattedName(formattedName)
        val vcardUid = formattedName + accountID
        vcard.uid = Uid(vcardUid)
        vcard.removeProperties(RawProperty::class.java)
        VCardUtils.saveLocalProfileToDisk(vcard, accountID, mDeviceService.provideFilesDir())
            .subscribe()
        account.loadedProfile = Single.just(Profile(formattedName, null))
    }

    companion object {
        private val TAG = SIPCreationPresenter::class.simpleName!!
    }
}