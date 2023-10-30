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
package net.jami.contactrequests

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

class BlockListPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val contactService: ContactService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<BlockListView>() {
    private var mAccountID: String? = null

    private fun updateList(list: Collection<ContactViewModel>) {
        val view = view ?: return
        if (list.isEmpty()) {
            view.hideListView()
            view.displayEmptyListMessage(true)
        } else {
            view.updateView(list)
            view.displayEmptyListMessage(false)
        }
    }

    fun setAccountId(accountID: String) {
        if (view == null) {
            return
        }
        mCompositeDisposable.clear()
        mCompositeDisposable.add(mAccountService
            .getAccountSingle(accountID)
            .flatMapObservable(Account::bannedContactsUpdates)
            .switchMapSingle { contacts -> contactService.getLoadedContact(accountID, contacts) }
            .observeOn(mUiScheduler)
            .subscribe({ list -> updateList(list) })
            { e: Throwable -> Log.e(TAG, "Error showing blacklist", e) })
        mAccountID = accountID
    }

    fun unblockClicked(contact: Contact) {
        val contactId = contact.uri.rawRingId
        mAccountService.addContact(mAccountID!!, contactId)
    }

    companion object {
        private val TAG = BlockListPresenter::class.simpleName!!
    }
}