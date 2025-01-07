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

import net.jami.model.Account
import net.jami.model.AccountCredentials
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import javax.inject.Inject

class SecurityAccountPresenter @Inject constructor(private val mAccountService: AccountService) :
    RootPresenter<SecurityAccountView>() {

    private var mAccount: Account? = null

    fun init(accountId: String) {
        val account = mAccountService.getAccount(accountId)
        mAccount = account
        if (account != null) {
            val view = view ?: return
            view.removeAllCredentials()
            view.addAllCredentials(account.credentials)
            val methods = mAccountService.tlsSupportedMethods
            val tlsMethods = methods.toTypedArray()
            view.setDetails(account.config, tlsMethods)
        }
    }

    fun credentialEdited(old: AccountCredentials, newCreds: AccountCredentials?) {
        mAccount!!.removeCredential(old)
        if (newCreds != null) {
            // There is a new value for this credentials it means it has been edited (otherwise deleted)
            mAccount!!.addCredential(newCreds)
        }
        mAccountService.setCredentials(mAccount!!.accountId, mAccount!!.credentialsHashMapList)
        mAccountService.setAccountDetails(mAccount!!.accountId, mAccount!!.details)
        view!!.removeAllCredentials()
        view!!.addAllCredentials(mAccount!!.credentials)
    }

    fun credentialAdded(old: AccountCredentials?, newCreds: AccountCredentials?) {
        mAccount!!.addCredential(newCreds!!)
        mAccountService.setCredentials(mAccount!!.accountId, mAccount!!.credentialsHashMapList)
        mAccountService.setAccountDetails(mAccount!!.accountId, mAccount!!.details)
        view!!.removeAllCredentials()
        view!!.addAllCredentials(mAccount!!.credentials)
    }

    fun tlsChanged(key: ConfigKey?, newValue: Any?) {
        if (newValue is Boolean) {
            mAccount!!.setDetail(key!!, (newValue as Boolean?)!!)
        } else {
            mAccount!!.setDetail(key!!, newValue as String?)
        }
        mAccountService.setCredentials(mAccount!!.accountId, mAccount!!.credentialsHashMapList)
        mAccountService.setAccountDetails(mAccount!!.accountId, mAccount!!.details)
    }

    fun fileActivityResult(key: ConfigKey?, filePath: String?) {
        mAccount!!.setDetail(key!!, filePath)
        mAccountService.setCredentials(mAccount!!.accountId, mAccount!!.credentialsHashMapList)
        mAccountService.setAccountDetails(mAccount!!.accountId, mAccount!!.details)
    }
}