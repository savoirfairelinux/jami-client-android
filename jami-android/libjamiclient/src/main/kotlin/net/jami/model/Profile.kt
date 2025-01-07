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

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

open class Profile(val displayName: String?, val avatar: Any?, val description: String? = null) {
    companion object {
        val EMPTY_PROFILE = Profile(null, null)
        val EMPTY_PROFILE_SINGLE: Single<Profile> = Single.just(EMPTY_PROFILE)
    }
}

class ContactViewModel(val contact: Contact, val profile: Profile, val registeredName: String? = null, val presence: Contact.PresenceStatus = Contact.PresenceStatus.OFFLINE) {
    val displayUri: String
        get() = registeredName ?: contact.uri.toString()
    val displayName: String
        get() = profile.displayName?.ifBlank { displayUri } ?: displayUri

    fun matches(query: String): Boolean =
        profile.displayName != null && profile.displayName.lowercase().contains(query)
                || registeredName != null && registeredName.contains(query)
                || contact.uri.toString().contains(query)

    override fun toString(): String = displayUri

    companion object {
        val EMPTY_VM: Observable<ContactViewModel> =
            Observable.just(ContactViewModel(Contact(Uri.fromId("")), Profile.EMPTY_PROFILE))
    }
}
