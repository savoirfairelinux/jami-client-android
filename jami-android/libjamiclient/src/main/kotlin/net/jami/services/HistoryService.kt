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
package net.jami.services

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.*
import net.jami.model.Interaction.InteractionStatus
import net.jami.utils.Log
import java.util.*

abstract class HistoryService {
    val scheduler: Scheduler = Schedulers.single()
    protected abstract fun getConnectionSource(dbName: String): ConnectionSource?
    protected abstract fun getInteractionDataDao(dbName: String): Dao<Interaction, Int>
    protected abstract fun getConversationDataDao(dbName: String): Dao<ConversationHistory, Int>
    protected abstract fun getHelper(dbName: String): Any?

    abstract fun setMessageNotified(accountId: String, conversationUri: Uri, lastId: String)
    abstract fun getLastMessageNotified(accountId: String, conversationUri: Uri): String?

    protected abstract fun deleteAccountHistory(accountId: String)
    fun clearHistory(accountId: String): Completable = Completable
        .fromAction { deleteAccountHistory(accountId) }
        .subscribeOn(scheduler)

    /**
     * Clears a conversation's history
     *
     * @param contactId          the participant's contact ID
     * @param accountId          the user's contact ID
     * @param deleteConversation true to completely delete the conversation including contact events
     * @return
     */
    fun clearHistory(contactId: String, accountId: String, deleteConversation: Boolean): Completable =
        if (accountId.isEmpty()) Completable.complete() else Completable.fromAction {
            var deleted = 0
            val conversation = getConversationDataDao(accountId).queryBuilder()
                .where().eq(ConversationHistory.COLUMN_PARTICIPANT, contactId).queryForFirst() ?: return@fromAction
            val deleteBuilder = getInteractionDataDao(accountId).deleteBuilder()
            if (deleteConversation) {
                // complete delete, remove conversation and all interactions
                deleteBuilder.where().eq(Interaction.COLUMN_CONVERSATION, conversation.id)
                getConversationDataDao(accountId).deleteById(conversation.id)
            } else {
                // keep conversation and contact event interactions
                deleteBuilder.where()
                    .eq(Interaction.COLUMN_CONVERSATION, conversation.id).and()
                    .ne(Interaction.COLUMN_TYPE, Interaction.InteractionType.CONTACT.toString())
            }
            deleted += deleteBuilder.delete()
            Log.w(TAG, "clearHistory: removed $deleted elements")
        }.subscribeOn(scheduler)

    /**
     * Clears all interactions in the app. Maintains contact events and actual conversations.
     *
     * @param accounts the list of accounts in the app
     * @return a completable
     */
    fun clearHistory(accounts: List<Account>): Completable = Completable.fromAction {
        for (account in accounts) {
            getInteractionDataDao(account.accountId).deleteBuilder().let { deleteBuilder ->
                deleteBuilder.where().ne(Interaction.COLUMN_TYPE, Interaction.InteractionType.CONTACT.toString())
                deleteBuilder.delete()
            }
        }
    }.subscribeOn(scheduler)

    fun updateInteraction(interaction: Interaction, accountId: String): Completable = Completable
        .fromAction { getInteractionDataDao(accountId).update(interaction) }
        .subscribeOn(scheduler)

    fun deleteInteraction(id: Int, accountId: String): Completable = Completable
        .fromAction { getInteractionDataDao(accountId).deleteById(id) }
        .subscribeOn(scheduler)

    /**
     * Inserts an interaction into the database, and if necessary, a conversation
     *
     * @param accountId    the user's account ID
     * @param conversation the conversation
     * @param interaction  the interaction to insert
     * @return a conversation single
     */
    fun insertInteraction(accountId: String, conversation: Conversation, interaction: Interaction): Completable = Completable.fromAction {
        Log.d(TAG, "Inserting interaction for account -> $accountId")
        val conversationDataDao = getConversationDataDao(accountId)
        val history = conversationDataDao.queryBuilder().where().eq(ConversationHistory.COLUMN_PARTICIPANT, conversation.participant).queryForFirst() ?:
        conversationDataDao.createIfNotExists(ConversationHistory(conversation.participant!!))!!
        //interaction.setConversation(conversation);
        conversation.id = history.id
        getInteractionDataDao(accountId).create(interaction)
    }
        .doOnError { e: Throwable -> Log.e(TAG, "Can't insert interaction", e) }
        .subscribeOn(scheduler)

    /**
     * Loads data required to load the smartlist. Only requires the most recent message or contact action.
     *
     * @param accountId required to query the appropriate account database
     * @return a list of the most recent interactions with each contact
     */
    fun getSmartlist(accountId: String): Single<List<Interaction>> = Single.fromCallable {
        Log.d(TAG, "Loading smartlist {$accountId}")
        // a raw query is done as MAX is not supported by ormlite without a raw query and a raw query cannot be combined with an orm query so a complete raw query is done
        // raw row mapper maps the sqlite result which is a list of strings, into the interactions object
        getInteractionDataDao(accountId).queryRaw("""
    SELECT * FROM (SELECT DISTINCT id, author, conversation, MAX(timestamp), body, type, status, daemon_id, is_read, extra_data from interactions GROUP BY interactions.conversation) as final
    JOIN conversations
    WHERE conversations.id = final.conversation
    GROUP BY final.conversation
    
    """.trimIndent(), { columnNames: Array<String>, resultColumns: Array<String> ->
                Interaction(
                    resultColumns[0],
                    resultColumns[1],
                    ConversationHistory(resultColumns[2].toInt(), resultColumns[12]),
                    resultColumns[3],
                    resultColumns[4],
                    resultColumns[5],
                    resultColumns[6],
                    resultColumns[7],
                    resultColumns[8],
                    resultColumns[9]
                )}).results
    }
        .subscribeOn(scheduler)
        .doOnError { e: Throwable -> Log.e(TAG, "Can't load smartlist from database", e) }
        .onErrorReturn { ArrayList() }

    /**
     * Retrieves an entire conversations history
     *
     * @param accountId      the user's account id
     * @param conversationId the conversation id
     * @return a conversation and all of its interactions
     */
    fun getConversationHistory(accountId: String, conversationId: Int): Single<List<Interaction>> = Single.fromCallable {
        Log.d(TAG, "Loading conversation history:  Account ID -> $accountId, ConversationID -> $conversationId")
        val interactionDataDao = getInteractionDataDao(accountId)
        interactionDataDao.query(interactionDataDao.queryBuilder()
                .orderBy(Interaction.COLUMN_TIMESTAMP, true)
                .where().eq(Interaction.COLUMN_CONVERSATION, conversationId)
                .prepare())
    }.subscribeOn(scheduler)
        .doOnError { e: Throwable -> Log.e(TAG, "Can't load conversation from database", e) }
        .onErrorReturn { ArrayList() }

    fun incomingMessage(accountId: String, daemonId: String?, from: String, message: String): Single<TextMessage> = Single.fromCallable {
        val fromUri = Uri.fromString(from).uri
        val conversationDataDao = getConversationDataDao(accountId)
        val conversation = conversationDataDao.queryBuilder().where().eq(ConversationHistory.COLUMN_PARTICIPANT, fromUri)
                .queryForFirst() ?: ConversationHistory(fromUri).apply {
            id = conversationDataDao.extractId(conversationDataDao.createIfNotExists(this))
        }
        val txt = TextMessage(fromUri, accountId, daemonId, conversation, message)
        txt.status = InteractionStatus.SUCCESS
        Log.w(TAG, "New text messsage " + txt.author + " " + txt.daemonId + " " + txt.body)
        getInteractionDataDao(accountId).create(txt)
        txt
    }.subscribeOn(scheduler)

    fun accountMessageStatusChanged(
        accountId: String,
        daemonId: String,
        peer: String,
        interactionStatus: InteractionStatus,
        messageState: Interaction.MessageStates,
    ): Single<TextMessage> = Single.fromCallable {
        val textList = getInteractionDataDao(accountId).queryForEq(Interaction.COLUMN_DAEMON_ID, daemonId)
        if (textList == null || textList.isEmpty()) {
            throw RuntimeException("accountMessageStatusChanged: not able to find message with id $daemonId in database")
        }
        val text = textList[0]
        val participant = Uri.fromString(peer).uri
        if (text.conversation!!.participant != participant) {
            throw RuntimeException("accountMessageStatusChanged: received an invalid text message")
        }
        val msg = TextMessage(text)
        msg.status = interactionStatus
        msg.statusMap = msg.statusMap.plus(accountId to messageState)
        getInteractionDataDao(accountId).update(msg)
        msg.account = accountId
        msg
    }.subscribeOn(scheduler)

    companion object {
        private val TAG = HistoryService::class.java.simpleName
    }
}