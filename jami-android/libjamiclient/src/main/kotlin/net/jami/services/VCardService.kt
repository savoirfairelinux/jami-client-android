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
package net.jami.services

import ezvcard.VCard
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.Profile
import java.io.File

abstract class VCardService {
    abstract fun loadProfile(account: Account): Observable<Profile>
    abstract fun loadSmallVCard(accountId: String, maxSize: Int): Maybe<VCard>
    fun loadSmallVCardWithDefault(accountId: String, maxSize: Int): Single<VCard> {
        return loadSmallVCard(accountId, maxSize)
            .switchIfEmpty(Single.fromCallable { VCard() })
    }

    abstract fun saveVCardProfile(accountId: String, uri: String?, displayName: String?, picture: String?): Single<VCard>

    abstract fun loadVCardProfile(vcard: VCard): Single<Profile>
    abstract fun peerProfileReceived(accountId: String, peerId: String, vcard: File): Single<Profile>
    abstract fun loadConversationProfile(info: Map<String, String>): Single<Profile>
    abstract fun accountProfileReceived(accountId: String, vcardFile: File): Single<Profile>
    abstract fun base64ToBitmap(base64: String?): Any?

    companion object {
        const val MAX_SIZE_SIP = 256 * 1024
        const val MAX_SIZE_REQUEST = 16 * 1024
    }
}