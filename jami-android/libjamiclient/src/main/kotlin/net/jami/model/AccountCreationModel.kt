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
package net.jami.model

import ezvcard.VCard
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.io.File

class AccountCreationModel {
    var managementServer: String? = null
    var username = ""
    var password = ""
    var archive: File? = null
    var isLink = false
    var isPush = true

    var newAccount: Account? = null
        set(account) {
            field = account
            profile.onNext(this)
        }

    open var photo: Any? = null
        set(photo) {
            field = photo
            profile.onNext(this)
        }

    var accountObservable: Observable<Account>? = null

    protected val profile: Subject<AccountCreationModel> = BehaviorSubject.createDefault(this)

    var fullName: String = ""
        set(fullName) {
            field = fullName
            profile.onNext(this)
        }
    var pin: String = ""
        set(pin) {
            field = pin.uppercase()
        }

    val profileUpdates: Observable<AccountCreationModel>
        get() = profile
}
