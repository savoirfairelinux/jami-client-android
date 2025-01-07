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

import net.jami.model.AccountCreationModel
import net.jami.mvp.RootPresenter
import javax.inject.Inject

class JamiAccountConnectPresenter @Inject constructor() : RootPresenter<JamiConnectAccountView>() {
    private var accountCreationModel: AccountCreationModel? = null
    fun init(model: AccountCreationModel?) {
        if (model == null) {
            view?.cancel()
            return
        }
        accountCreationModel = model
        /*boolean hasArchive = mAccountCreationModel.getArchive() != null;
        JamiConnectAccountView view = getView();
        if (view != null) {
            view.showPin(!hasArchive);
            view.enableLinkButton(hasArchive);
        }*/
    }

    fun passwordChanged(password: String) {
        accountCreationModel?.password = password
        showConnectButton()
    }

    fun usernameChanged(username: String) {
        accountCreationModel!!.username = username
        showConnectButton()
    }

    fun serverChanged(server: String?) {
        accountCreationModel!!.managementServer = server
        showConnectButton()
    }

    fun connectClicked() {
        if (isFormValid) {
            view?.createAccount()
        }
    }

    private fun showConnectButton() {
        view?.enableConnectButton(isFormValid)
    }

    private val isFormValid: Boolean
        get() = accountCreationModel!!.password.isNotEmpty()
                && accountCreationModel!!.username.isNotBlank()
                && !accountCreationModel!!.managementServer.isNullOrEmpty()
}