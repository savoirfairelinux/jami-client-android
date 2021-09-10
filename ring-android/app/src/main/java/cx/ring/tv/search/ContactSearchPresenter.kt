/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.tv.search

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Uri.Companion.fromString
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.AccountService.RegisteredName
import net.jami.smartlist.SmartListViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class ContactSearchPresenter @Inject constructor(
    private val mAccountService: AccountService,
    @Named("UiScheduler") var mUiScheduler: Scheduler
) : RootPresenter<ContactSearchView>() {
    private var mContact: Contact? = null
    private val contactQuery = PublishSubject.create<String>()

    override fun bindView(view: ContactSearchView) {
        super.bindView(view)
        mCompositeDisposable.add(contactQuery
            .debounce(350, TimeUnit.MILLISECONDS)
            .switchMapSingle { q: String ->
                mAccountService.findRegistrationByName(mAccountService.currentAccount!!.accountID, "", q)
            }
            .observeOn(mUiScheduler)
            .subscribe { q: RegisteredName -> parseEventState(mAccountService.getAccount(q.accountId)!!, q.name, q.address, q.state) })
    }

    fun queryTextChanged(query: String) {
        if (query == "") {
            view?.clearSearch()
        } else {
            val currentAccount = mAccountService.currentAccount ?: return
            val uri = fromString(query)
            if (uri.isHexId) {
                mContact = currentAccount.getContactFromCache(uri)
                view!!.displayContact(currentAccount.accountID, mContact)
            } else {
                view!!.clearSearch()
                contactQuery.onNext(query)
            }
        }
    }

    private fun parseEventState(account: Account, name: String, address: String?, state: Int) {
        when (state) {
            0 -> {
                // on found
                mContact = account.getContactFromCache(address!!).apply { setUsername(name) }
                view?.displayContact(account.accountID, mContact)
            }
            1 -> {
                // invalid name
                val uriName = fromString(name)
                if (uriName.isHexId) {
                    mContact = account.getContactFromCache(uriName)
                    view?.displayContact(account.accountID, mContact)
                } else {
                    view?.clearSearch()
                }
            }
            else -> {
                // on error
                val uriAddress = fromString(address!!)
                if (uriAddress.isHexId) {
                    mContact = account.getContactFromCache(uriAddress)
                    view?.displayContact(account.accountID, mContact)
                } else {
                    view?.clearSearch()
                }
            }
        }
    }

    fun contactClicked(model: SmartListViewModel) {
        view?.displayContactDetails(model)
    }
}