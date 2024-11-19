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

import net.jami.services.AccountService

interface LinkAccountImportView: UpdateDeviceAuthState {
    fun enableLinkButton(enable: Boolean)
//    fun showPin(show: Boolean)
    fun createAccount()
    fun cancel()

}

interface UpdateDeviceAuthState {
    fun showLoadingToken()
    fun showTokenAvailable(token: String) // Todo: what is the exact format of the Token? jami-auth://[a-z0-9]{40}:[a-z0-9]{6} regex = jami-auth:\/\/[a-z0-9]{40}\/[a-z0-9]{6}
    fun showConnecting()
    fun showAuthenticating()
    fun showDone()
    fun showError(linkDeviceError: AccountService.DeviceAuthStateError)
}
