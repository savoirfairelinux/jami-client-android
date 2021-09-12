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
package net.jami.model

import ezvcard.VCard
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.io.File
import java.io.Serializable

abstract class AccountCreationModel : Serializable {
    var managementServer: String? = null
    private var mFullName = ""
    var username = ""
    var password = ""
    private var mPin = ""
    var archive: File? = null
    var isLink = false
    var isPush = true

    @Transient
    var newAccount: Account? = null
        set(account) {
            field = account
            profile.onNext(this)
        }

    @Transient
    open var photo: Any? = null
        set(photo) {
            field = photo
            profile.onNext(this)
        }

    @Transient
    var accountObservable: Observable<Account>? = null

    @Transient
    protected val profile: Subject<AccountCreationModel> = BehaviorSubject.createDefault(this)

    var fullName: String
        get() = mFullName
        set(fullName) {
            mFullName = fullName
            profile.onNext(this)
        }
    var pin: String
        get() = mPin
        set(pin) {
            mPin = pin.toUpperCase()
        }

    abstract fun toVCard(): Single<VCard>

    val profileUpdates: Observable<AccountCreationModel>
        get() = profile
}