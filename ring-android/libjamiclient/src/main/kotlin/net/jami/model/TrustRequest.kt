/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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

import io.reactivex.rxjava3.core.Single

class TrustRequest(
    val accountId: String,
    val from: Uri,
    val timestamp: Long,
    val conversationUri: Uri?,
    var profile: Single<Profile>? = null
)
{
    var message: String? = null
    var mode: Conversation.Mode = Conversation.Mode.OneToOne

    constructor(accountId: String, conversationUri: Uri?, info: Map<String, String>) : this(
        accountId,
        Uri.fromId(info["from"]!!),
        info["received"]!!.toLong() * 1000L,
        conversationUri
    ) {
        val title = info["title"]
        val descr = info["descr"]
        val avatar = info["avatar"]
        info["mode"]?.let { m -> mode = Conversation.Mode.values()[m.toInt()] }

        if (!title.isNullOrBlank()) {
            profile = Single.just(Profile(title, avatar))
        }
        message = descr
    }
}
