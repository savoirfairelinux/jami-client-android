/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import javax.inject.Inject

class JamiLinkAccountPresenter @Inject constructor() : RootPresenter<JamiLinkAccountView>() {
    private var mAccountCreationModel: AccountCreationModel? = null

    fun init(accountCreationModel: AccountCreationModel?) {
        mAccountCreationModel = accountCreationModel
        if (mAccountCreationModel == null) {
            view?.cancel()
            return
        }
        val hasArchive = mAccountCreationModel?.archive != null
        val view = view
        if (view != null) {
            view.showPin(!hasArchive)
            view.enableLinkButton(hasArchive)
        }
    }

    fun passwordChanged(password: String) {
        mAccountCreationModel?.password = password
        showHideLinkButton()
    }

    fun pinChanged(pin: String) {
        mAccountCreationModel?.pin = pin
        showHideLinkButton()
    }

    fun linkClicked() {
        if (isFormValid) {
            view?.createAccount(mAccountCreationModel!!)
        }
    }

    private fun showHideLinkButton() {
        view?.enableLinkButton(isFormValid)
    }

    private val isFormValid: Boolean
        get() = mAccountCreationModel?.archive != null || mAccountCreationModel!!.pin.isNotEmpty()
}