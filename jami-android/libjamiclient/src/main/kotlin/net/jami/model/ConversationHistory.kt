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
package net.jami.model

import com.j256.ormlite.table.DatabaseTable
import com.j256.ormlite.field.DatabaseField

@DatabaseTable(tableName = ConversationHistory.TABLE_NAME)
open class ConversationHistory {
    @DatabaseField(generatedId = true, columnName = COLUMN_CONVERSATION_ID, canBeNull = false)
    var id: Int? = null

    @DatabaseField(columnName = COLUMN_PARTICIPANT, index = true)
    var participant: String? = null

    @DatabaseField(columnName = COLUMN_EXTRA_DATA)
    var mExtraData: String? = null

    /* Needed by ORMLite */
    constructor()
    constructor(conversation: Conversation) {
        id = conversation.id
        participant = conversation.participant
    }

    constructor(id: Int, participant: String) {
        this.id = id
        this.participant = participant
    }

    constructor(participant: String) {
        this.participant = participant
    }

    companion object {
        const val TABLE_NAME = "conversations"
        const val COLUMN_CONVERSATION_ID = "id"
        const val COLUMN_PARTICIPANT = "participant"
        const val COLUMN_EXTRA_DATA = "extra_data"
    }
}