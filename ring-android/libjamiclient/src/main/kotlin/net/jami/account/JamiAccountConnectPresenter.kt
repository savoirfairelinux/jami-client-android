/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.account

import net.jami.model.AccountCreationModel
import net.jami.mvp.RootPresenter
import net.jami.utils.StringUtils
import javax.inject.Inject

class JamiAccountConnectPresenter @Inject constructor() : RootPresenter<JamiConnectAccountView>() {
    private var mAccountCreationModel: AccountCreationModel? = null
    fun init(accountCreationModel: AccountCreationModel?) {
        mAccountCreationModel = accountCreationModel
        if (mAccountCreationModel == null) {
            view?.cancel()
            return
        }
        /*boolean hasArchive = mAccountCreationModel.getArchive() != null;
        JamiConnectAccountView view = getView();
        if (view != null) {
            view.showPin(!hasArchive);
            view.enableLinkButton(hasArchive);
        }*/
    }

    fun passwordChanged(password: String) {
        mAccountCreationModel!!.password = password
        showConnectButton()
    }

    fun usernameChanged(username: String) {
        mAccountCreationModel!!.username = username
        showConnectButton()
    }

    fun serverChanged(server: String?) {
        mAccountCreationModel!!.managementServer = server
        showConnectButton()
    }

    fun connectClicked() {
        if (isFormValid) {
            view?.createAccount(mAccountCreationModel!!)
        }
    }

    private fun showConnectButton() {
        view?.enableConnectButton(isFormValid)
    }

    private val isFormValid: Boolean
        get() = !StringUtils.isEmpty(mAccountCreationModel!!.password)
                && !StringUtils.isEmpty(mAccountCreationModel!!.username)
                && !StringUtils.isEmpty(mAccountCreationModel!!.managementServer)
}