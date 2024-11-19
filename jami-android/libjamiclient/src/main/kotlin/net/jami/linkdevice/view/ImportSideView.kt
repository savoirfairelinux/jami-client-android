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
package net.jami.linkdevice.view

import net.jami.linkdevice.presenter.AuthError
import net.jami.linkdevice.presenter.ImportSidePresenter.InputError

interface ImportSideView {
    /**
     * Show the authentication token.
     * @param authenticationUri The authentication token. Null if not yet available.
     */
    fun showAuthenticationUri(authenticationUri: String?)

    /**
     * Show action required meaning the user needs to do something on the other side.
     */
    fun showActionRequired()

    /**
     * Show the importing account identity.
     * @param needPassword Whether the account needs a password.
     * @param jamiId The Jami ID of the imported account.
     * @param registeredName The registered name of the imported account (if any).
     */
    fun showAuthentication(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String? = null,
        error: InputError? = null
    )

    /**
     * Show the progress of the operation.
     */
    fun showInProgress()

    /**
     * Show the result of the operation.
     * @param error The error that occurred. Null if no error.
     */
    fun showResult(error: AuthError? = null)
}