/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cx.ring.model.Account;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationHistory;
import cx.ring.model.Interaction;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public abstract class HistoryService {
    private static final String TAG = HistoryService.class.getSimpleName();

    private final Scheduler scheduler = Schedulers.single();

    protected abstract ConnectionSource getConnectionSource(String dbName);

    protected abstract Dao<Interaction, Integer> getInteractionDataDao(String dbName);

    protected abstract Dao<ConversationHistory, Integer> getConversationDataDao(String dbName);

    protected abstract Object getHelper(String dbName);

    protected abstract void migrateDatabase(List<String> accounts);

    protected abstract void deleteAccountHistory(String accountId);

    public Scheduler getScheduler() {
        return scheduler;
    }

   /* public Completable updateDataTransfer(DataTransfer dataTransfer) {
        return Completable.fromAction(() -> getDataHistoryDao(dataTransfer.getAccountId()).update(dataTransfer))
                .subscribeOn(scheduler);
    }
    */

    public Completable clearHistory(final String contactId, final String accountId) {
        if (StringUtils.isEmpty(accountId))
            return Completable.complete();
        return Completable.fromAction(() -> {
            int deleted = 0;
            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            conversationQueryBuilder.where().eq(ConversationHistory.COLUMN_PARTICIPANT, contactId);
            List<ConversationHistory> history = conversationQueryBuilder.query();

            if (history == null || history.isEmpty())
                return;

            ConversationHistory conversation = history.get(0);

            getConversationDataDao(accountId).deleteById(conversation.getId());

            DeleteBuilder<Interaction, Integer> deleteBuilder = getInteractionDataDao(accountId).deleteBuilder();
            deleteBuilder.where().eq(Interaction.COLUMN_CONVERSATION, conversation);
            deleted += deleteBuilder.delete();

            Log.w(TAG, "clearHistory: removed " + deleted + " elements");
        }).subscribeOn(scheduler);
    }

    public Completable clearHistory(List<Account> accounts) {
        return Completable.fromAction(() -> {
            for (Account account : accounts) {
                String accountId = account.getAccountID();
                TableUtils.clearTable(getConnectionSource(accountId), ConversationHistory.class);
                TableUtils.clearTable(getConnectionSource(accountId), Interaction.class);
            }
        }).subscribeOn(scheduler);
    }

    public Completable insertInteraction(Interaction interaction, String accountId) {
        Log.d(TAG, "Inserting interaction...");
        return Completable.fromAction(() -> getInteractionDataDao(accountId).create(interaction)).subscribeOn(scheduler);
    }


    public Completable insertConversation(ConversationHistory conversation, String accountId) {
        Log.d(TAG, "Inserting conversation...");
        return Completable.fromAction(() -> getConversationDataDao(accountId).createIfNotExists(conversation))
                .subscribeOn(scheduler);
    }

    // this should be removed TODO
    public Single<List<ConversationHistory>> insertOrGetConversation(ConversationHistory conversation, String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            conversationQueryBuilder.where().eq(ConversationHistory.COLUMN_PARTICIPANT, conversation.getParticipant());
            List<ConversationHistory> list = getConversationDataDao(accountId).query(conversationQueryBuilder.prepare());
            if (list == null || list.isEmpty()) {
                getConversationDataDao(accountId).create(conversation);
            }
            return (list == null || list.isEmpty()) ? getConversationDataDao(accountId).query(conversationQueryBuilder.prepare()) : list;
        }).doOnError(e -> Log.e(TAG, "Can't create conversation", e))
                .onErrorReturn(e -> new ArrayList<>());
    }

    // todo type
    public Completable insertAndGetConversation(ConversationHistory conversation, Interaction event, String accountId) {
        return Completable.fromAction(() -> {
            Log.d(TAG, "Insert conversation is running......");
            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            conversationQueryBuilder.where().eq(ConversationHistory.COLUMN_PARTICIPANT, conversation.getParticipant());
            List<ConversationHistory> list = getConversationDataDao(accountId).query(conversationQueryBuilder.prepare());
            if (list == null || list.isEmpty()) {
                getConversationDataDao(accountId).create(conversation);
            }
            event.setConversation(getConversationDataDao(accountId).query(conversationQueryBuilder.prepare()).get(0));


            QueryBuilder<Interaction, Integer> interactionQueryBuilder = getInteractionDataDao(accountId).queryBuilder();
            // TODO

            String data;
            if(event.getStatus() == null)
                data = null;
            else
                data = event.getStatus().toString();

            interactionQueryBuilder.where().eq(Interaction.COLUMN_TYPE, event.getType().toString()).and().eq(Interaction.COLUMN_STATUS, data).and().eq(Interaction.COLUMN_CONVERSATION, event.getConversation());

            List<Interaction> interactionList = getInteractionDataDao(accountId).query(interactionQueryBuilder.prepare());
            if (interactionList == null || interactionList.isEmpty()) {
                getInteractionDataDao(accountId).create(event);
            }

        }).doOnError(e -> Log.e(TAG, "Can't create conversation", e)).subscribeOn(scheduler);
    }


    public Completable updateInteraction(Interaction interaction, String accountId) {
        return Completable.fromAction(() -> getInteractionDataDao(accountId).update(interaction))
                .subscribeOn(scheduler);
    }

    public Completable deleteInteraction(int id, String accountId) {
        return Completable
                .fromAction(() -> getInteractionDataDao(accountId).deleteById(id))
                .subscribeOn(scheduler);
    }

    public Completable deleteConversation(int id, String accountId) {
        return Completable
                .fromAction(() -> getConversationDataDao(accountId).deleteById(id))
                .subscribeOn(scheduler);
    }

    /**
     * Loads data required to load the smartlist. Only requires the most recent message or contact action.
     *
     * @param accountId required to query the appropriate account database
     * @return a list of the most recent interactions with each contact
     */
    public Single<List<Interaction>> getSmartlist(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<Interaction, Integer> interactionQueryBuilder = getInteractionDataDao(accountId).queryBuilder();
            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            interactionQueryBuilder.distinct().groupBy(Interaction.COLUMN_CONVERSATION);
            return getInteractionDataDao(accountId).query(interactionQueryBuilder.join(conversationQueryBuilder).prepare());
        }).doOnError(e -> Log.e(TAG, "Can't load smartlist from database", e))
                .onErrorReturn(e -> new ArrayList<>());
    }


    public Single<List<ConversationHistory>> getAllConversations(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            return getConversationDataDao(accountId).query(conversationQueryBuilder.prepare());
        }).doOnError(e -> Log.e(TAG, "Can't load conversations", e))
                .onErrorReturn(e -> new ArrayList<>());
    }

    public Single<ConversationHistory> getConversation(final String accountId, final String contact) {
        return Single.fromCallable(() -> {
            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            conversationQueryBuilder.where().eq(ConversationHistory.COLUMN_PARTICIPANT, contact);
            List<ConversationHistory> historyList = getConversationDataDao(accountId).query(conversationQueryBuilder.prepare());
            if (historyList == null || historyList.isEmpty()) {
                ConversationHistory convo = new ConversationHistory(contact);
                insertConversation(convo, accountId).subscribe();
                return convo;
            } else
                return historyList.get(0);

        }).doOnError(e -> {
            Log.e(TAG, "Can't find conversation", e);
        }).onErrorReturn(e -> null);
    }

    public Single<List<Interaction>> getConversationHistory(final String accountId, final Conversation conversation, final Long offset) {
        return Single.fromCallable(() -> {
            QueryBuilder<Interaction, Integer> interactionQueryBuilder = getInteractionDataDao(accountId).queryBuilder();
            // interactionQueryBuilder.orderBy(Interaction.COLUMN_TIMESTAMP, false);

            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            conversationQueryBuilder.where().eq(ConversationHistory.COLUMN_CONVERSATION_ID, conversation.getId());
            // interactionQueryBuilder.offset(offset);
            return getInteractionDataDao(accountId).query(interactionQueryBuilder.join(conversationQueryBuilder).orderBy(Interaction.COLUMN_TIMESTAMP, true).prepare());
        }).doOnError(e -> Log.e(TAG, "Can't load conversation from database", e))
                .onErrorReturn(e -> new ArrayList<>());
    }

    public Single<TextMessage> incomingMessage(final String accountId, final String callId, final String from, final String message) {
        return Single.fromCallable(() -> {
            String f = new Uri(from).getUri();

            ConversationHistory conversation;
            try {
                conversation = getConversationDataDao(accountId).queryForEq(ConversationHistory.COLUMN_PARTICIPANT, f).get(0);
            } catch (IndexOutOfBoundsException | SQLException e) {
                conversation = new ConversationHistory(f);
                insertConversation(conversation, accountId).subscribe();
            }

            TextMessage txt = new TextMessage(f, conversation, message);
            txt.setDaemonId(callId);
            txt.setAccount(accountId);
            txt.setStatus(Interaction.InteractionStatus.SUCCEEDED);


            Log.w(TAG, "New text messsage " + txt.getAuthor() + " " + txt.getDaemonIdString() + " " + txt.getBody());
            getInteractionDataDao(accountId).create(txt);
            return txt;
        }).subscribeOn(scheduler);
    }


    public Single<TextMessage> accountMessageStatusChanged(String accountId, long interactionId, String to, int status) {
        return Single.fromCallable(() -> {
            List<Interaction> textList = getInteractionDataDao(accountId).queryForEq(Interaction.COLUMN_DAEMON_ID, Long.toString(interactionId));
            if (textList == null || textList.isEmpty()) {
                throw new RuntimeException("accountMessageStatusChanged: not able to find message with id " + interactionId + " in database");
            }
            Interaction text = textList.get(0);
            String participant = (new Uri(to)).getUri();
            if (!text.getConversation().getParticipant().equals(participant)) {
                throw new RuntimeException("accountMessageStatusChanged: received an invalid text message");
            }
            TextMessage msg = new TextMessage(text);
            msg.setStatus(status);
            getInteractionDataDao(accountId).update(msg);
            msg.setAccount(accountId);
            return msg;
        }).subscribeOn(scheduler);
    }
}