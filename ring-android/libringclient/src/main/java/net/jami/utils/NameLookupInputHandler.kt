/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.jami.utils

import net.jami.services.AccountService
import java.lang.ref.WeakReference
import java.util.*

class NameLookupInputHandler(accountService: AccountService, accountId: String) {
    private val mAccountService: WeakReference<AccountService> = WeakReference(accountService)
    private val mAccountId: String = accountId
    private val timer = Timer(true)
    private var lastTask: NameTask? = null

    fun enqueueNextLookup(text: String) {
        lastTask?.cancel()
        lastTask = NameTask(text)
        timer.schedule(lastTask, WAIT_DELAY.toLong())
    }

    private inner class NameTask(private val mTextToLookup: String) : TimerTask() {
        override fun run() {
            mAccountService.get()?.lookupName(mAccountId, "", mTextToLookup)
        }
    }

    companion object {
        private const val WAIT_DELAY = 350
    }
}