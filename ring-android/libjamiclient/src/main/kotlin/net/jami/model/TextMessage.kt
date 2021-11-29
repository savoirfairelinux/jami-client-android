/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.model

import java.lang.NumberFormatException

class TextMessage : Interaction {
    constructor(author: String?, account: String, daemonId: String?, conversation: ConversationHistory?, message: String) {
        this.author = author
        this.account = account
        if (daemonId != null) {
            try {
                this.daemonId = daemonId.toLong()
            } catch (e: NumberFormatException) {
                try {
                    this.daemonId = daemonId.toLong(16)
                } catch (e2: NumberFormatException) {
                    this.daemonId = 0L
                }
            }
        }
        timestamp = System.currentTimeMillis()
        mType = InteractionType.TEXT.toString()
        this.conversation = conversation
        isIncoming = author != null
        body = message
    }

    constructor(author: String?, account: String, timestamp: Long, conversation: ConversationHistory?, message: String, isIncoming: Boolean) {
        this.author = author
        this.account = account
        this.timestamp = timestamp
        mType = InteractionType.TEXT.toString()
        this.conversation = conversation
        this.isIncoming = isIncoming
        body = message
    }

    constructor(interaction: Interaction) {
        id = interaction.id
        author = interaction.author
        timestamp = interaction.timestamp
        mType = interaction.type.toString()
        mStatus = interaction.status.toString()
        conversation = interaction.conversation
        isIncoming = author != null
        daemonId = interaction.daemonId
        body = interaction.body
        mIsRead = if (interaction.isRead) 1 else 0
        account = interaction.account
        contact = interaction.contact
    }
}