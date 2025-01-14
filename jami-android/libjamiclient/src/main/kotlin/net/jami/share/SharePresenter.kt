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
package net.jami.share

import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.utils.QRCodeUtils
import javax.inject.Inject
import javax.inject.Named

interface ShareView

class SharePresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mContactService: ContactService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<ShareView>() {

    fun loadContact(
        contact: Uri,
        onContactLoaded: (ContactViewModel) -> Unit
    ) {
        mCompositeDisposable.add(
            mContactService
                .getLoadedContact(mAccountService.currentAccount!!.accountId, contact.uri)
                .observeOn(mUiScheduler)
                .subscribe(
                    { loadedContact -> onContactLoaded(loadedContact) },
                    { error -> error.printStackTrace() }
                )
        )
    }

    fun loadQRCodeData(
        contact: Uri,
        foregroundColor: Int,
        backgroundColor: Int,
        onQRCodeDataLoaded: (QRCodeUtils.QRCodeData) -> Unit
    ) {
        mCompositeDisposable.add(
            Maybe.fromCallable {
                QRCodeUtils.encodeStringAsQRCodeData(
                    contact.uri,
                    foregroundColor,
                    backgroundColor
                )
            }.observeOn(mUiScheduler)
                .subscribe(
                    { qrCodeData -> onQRCodeDataLoaded(qrCodeData) },
                    { error -> error.printStackTrace() }
                )
        )
    }
}