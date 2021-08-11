/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
package net.jami.model

import ezvcard.Ezvcard
import ezvcard.VCard
import net.jami.utils.StringUtils

class TrustRequest {
    val accountId: String
    private var mContactUsername: String? = null
    val uri: Uri
    var conversationId: String?
    var vCard: VCard? = null
    var message: String? = null
    val timestamp: Long
    var isNameResolved = false
        private set

    constructor(accountId: String, uri: Uri, received: Long, payload: String?, conversationId: String?) {
        this.accountId = accountId
        this.uri = uri
        this.conversationId = if (StringUtils.isEmpty(conversationId)) null else conversationId
        timestamp = received
        vCard = if (payload == null) null else Ezvcard.parse(payload).first()
        message = null
    }

    constructor(accountId: String, info: Map<String, String>) : this(accountId, Uri.fromId(info["from"]!!),
        java.lang.Long.decode(info["received"]) * 1000L, info["payload"], info["conversationId"])

    constructor(accountId: String, contactUri: Uri, conversationId: String?) {
        this.accountId = accountId
        uri = contactUri
        this.conversationId = conversationId
        timestamp = 0
    }

    val fullname: String
        get() {
            var fullname = ""
            if (vCard != null && vCard!!.formattedName != null) {
                fullname = vCard!!.formattedName.value
            }
            return fullname
        }
    val displayname: String?
        get() {
            val hasUsername = mContactUsername != null && !mContactUsername!!.isEmpty()
            return if (hasUsername) mContactUsername else uri.toString()
        }

    fun setUsername(username: String?) {
        mContactUsername = username
        isNameResolved = true
    }

    companion object {
        private val TAG = TrustRequest::class.simpleName!!
    }
}