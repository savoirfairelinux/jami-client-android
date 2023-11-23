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

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject

@DatabaseTable(tableName = Interaction.TABLE_NAME)
open class Interaction {
    var account: String? = null
    var isIncoming = false
    var contact: Contact? = null
    var replyToId: String? = null
    var replyTo: Single<Interaction>? = null
    var edit: String? = null
    var reactToId: String? = null
    var reactTo: Interaction? = null
    var reactions: MutableList<Interaction> = ArrayList()
    private var reactionSubject: Subject<List<Interaction>> = BehaviorSubject.createDefault(reactions)

    var history: MutableList<Interaction> = ArrayList<Interaction>(1).apply { add(this@Interaction) }
    private var historySubject: Subject<List<Interaction>> = BehaviorSubject.createDefault(history)

    val reactionObservable: Observable<List<Interaction>>
        get() = reactionSubject.switchMap { i ->
            if (i.isEmpty())
                Observable.just(emptyList())
            else {
                Observable.combineLatest(i.map { it.lastElement }) { a -> a.mapNotNull {
                    it as Interaction
                    return@mapNotNull if (it.body.isNullOrEmpty()) null else it
                } }
            }
        }

    val historyObservable: Observable<List<Interaction>>
        get() = historySubject
    var lastElement: Observable<Interaction> = historyObservable
        .map { it.lastOrNull() ?: this }

    @DatabaseField(generatedId = true, columnName = COLUMN_ID, index = true)
    var id = 0

    @DatabaseField(columnName = COLUMN_AUTHOR, index = true)
    var author: String? = null

    @DatabaseField(columnName = COLUMN_CONVERSATION, foreignColumnName = ConversationHistory.COLUMN_CONVERSATION_ID, foreign = true)
    var conversation: ConversationHistory? = null

    @DatabaseField(columnName = COLUMN_TIMESTAMP, index = true)
    var timestamp: Long = 0

    @DatabaseField(columnName = COLUMN_BODY)
    var body: String? = null

    @DatabaseField(columnName = COLUMN_TYPE)
    var mType: String? = null

    @DatabaseField(columnName = COLUMN_STATUS)
    var mStatus = InteractionStatus.UNKNOWN.toString()

    @DatabaseField(columnName = COLUMN_DAEMON_ID)
    var daemonId: Long? = null

    /** True if this interaction has been displayed by the user */
    @DatabaseField(columnName = COLUMN_IS_READ)
    var mIsRead = 0

    @DatabaseField(columnName = COLUMN_EXTRA_FLAG)
    var mExtraFlag = JsonObject().toString()

    var isNotified = false
    val displayedContacts: MutableList<String> = ArrayList()

    // Swarm
    var conversationId: String? = null
        private set
    var messageId: String? = null
        private set
    var parentId: String? = null
        private set

    /* Needed by ORMLite */
    constructor()
    constructor(accountId: String) {
        account = accountId
        type = InteractionType.INVALID
    }

    constructor(conversation: Conversation, type: InteractionType) {
        this.conversation = conversation
        account = conversation.accountId
        mType = type.toString()
    }

    constructor(
        id: String,
        author: String?,
        conversation: ConversationHistory?,
        timestamp: String,
        body: String?,
        type: String?,
        status: String,
        daemonId: String?,
        isRead: String,
        extraFlag: String
    ) {
        this.id = id.toInt()
        this.author = author
        this.conversation = conversation
        this.timestamp = timestamp.toLong()
        this.body = body
        mType = type
        mStatus = status
        try {
            this.daemonId = daemonId?.toLong()
        } catch (e: NumberFormatException) {
            this.daemonId = 0L
        }
        mIsRead = isRead.toInt()
        mExtraFlag = extraFlag
    }

    fun read() {
        mIsRead = 1
    }

    var type: InteractionType
        get() = if (mType != null) InteractionType.fromString(mType!!) else InteractionType.INVALID
        set(type) {
            mType = type.toString()
        }
    var status: InteractionStatus
        get() = InteractionStatus.fromString(mStatus)
        set(status) {
            if (status == InteractionStatus.DISPLAYED) mIsRead = 1
            mStatus = status.toString()
        }
    val extraFlag: JsonObject
        get() = toJson(mExtraFlag)

    fun toJson(value: String?): JsonObject {
        return JsonParser.parseString(value).asJsonObject
    }

    fun fromJson(json: JsonObject): String {
        return json.toString()
    }

    open val daemonIdString: String?
        get() = daemonId?.toString()

    val isRead: Boolean
        get() = mIsRead == 1

    val isSwarm: Boolean
        get() = !messageId.isNullOrEmpty()

    fun setSwarmInfo(conversationId: String) {
        this.conversationId = conversationId
        messageId = null
        parentId = null
    }

    fun setSwarmInfo(conversationId: String, messageId: String, parent: String?) {
        this.conversationId = conversationId
        this.messageId = messageId
        parentId = parent
    }

    fun addReaction(interaction: Interaction) {
        reactions.add(interaction)
        reactionSubject.onNext(ArrayList(reactions))
    }

    fun addReactions(interactions: List<Interaction>) {
        reactions.addAll(interactions)
        reactionSubject.onNext(ArrayList(reactions))
    }

    fun addEdit(interaction: Interaction, newMessage: Boolean) {
        history.remove(interaction)
        if (newMessage)
            history.add(interaction)
        else
            history.add(0, interaction)
        historySubject.onNext(ArrayList(history))
    }
    fun addEdits(interactions: List<Interaction>) {
        history.addAll(interactions)
        historySubject.onNext(ArrayList(history))
    }

    fun updateFrom(previous: Interaction) {
        history = previous.history
        historySubject = previous.historySubject
        lastElement = previous.lastElement
        reactions = previous.reactions
        reactionSubject = previous.reactionSubject
        //reactionObservable = previous.reactionObservable
    }

    var preview: Any? = null

    enum class InteractionStatus {
        UNKNOWN, SENDING, SUCCESS, DISPLAYED, INVALID, FAILURE, TRANSFER_CREATED, TRANSFER_ACCEPTED, TRANSFER_CANCELED, TRANSFER_ERROR, TRANSFER_UNJOINABLE_PEER, TRANSFER_ONGOING, TRANSFER_AWAITING_PEER, TRANSFER_AWAITING_HOST, TRANSFER_TIMEOUT_EXPIRED, TRANSFER_FINISHED, FILE_AVAILABLE;

        val isError: Boolean
            get() = this == TRANSFER_ERROR || this == TRANSFER_UNJOINABLE_PEER || this == TRANSFER_CANCELED || this == TRANSFER_TIMEOUT_EXPIRED || this == FAILURE
        val isOver: Boolean
            get() = isError || this == TRANSFER_FINISHED

        companion object {
            fun fromString(str: String): InteractionStatus = values().firstOrNull { it.name == str } ?: INVALID

            fun fromIntTextMessage(n: Int): InteractionStatus = try {
                values()[n]
            } catch (e: ArrayIndexOutOfBoundsException) {
                INVALID
            }

            fun fromIntFile(n: Int): InteractionStatus = when (n) {
                0 -> INVALID
                1 -> TRANSFER_CREATED
                2, 9 -> TRANSFER_ERROR
                3 -> TRANSFER_AWAITING_PEER
                4 -> TRANSFER_AWAITING_HOST
                5 -> TRANSFER_ONGOING
                6 -> TRANSFER_FINISHED
                7, 8, 10 -> TRANSFER_UNJOINABLE_PEER
                11 -> TRANSFER_TIMEOUT_EXPIRED
                else -> UNKNOWN
            }
        }
    }

    enum class InteractionType {
        INVALID, TEXT, CALL, CONTACT, DATA_TRANSFER;

        companion object {
            fun fromString(str: String) = try { valueOf(str) } catch (e: Exception) { INVALID }
        }
    }

    companion object {
        const val TABLE_NAME = "interactions"
        const val COLUMN_ID = "id"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_CONVERSATION = "conversation"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_BODY = "body"
        const val COLUMN_TYPE = "type"
        const val COLUMN_STATUS = "status"
        const val COLUMN_DAEMON_ID = "daemon_id"
        const val COLUMN_IS_READ = "is_read"
        const val COLUMN_EXTRA_FLAG = "extra_data"

        fun compare(a: Interaction?, b: Interaction?): Int {
            if (a == null) return if (b == null) 0 else -1
            return if (b == null) 1 else a.timestamp.compareTo(b.timestamp)
        }
    }
}