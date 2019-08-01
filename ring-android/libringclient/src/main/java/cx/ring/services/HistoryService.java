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
import cx.ring.model.ConversationHistory;
import cx.ring.model.Interaction;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
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

    public abstract Observable<Boolean> isAccountMigrationComplete();

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Completable clearHistory(final String contactId, final String accountId, boolean deleteConversation) {
        if (StringUtils.isEmpty(accountId))
            return Completable.complete();
        return Completable.fromAction(() -> {
            int deleted = 0;

            List<ConversationHistory> history = getConversationDataDao(accountId).queryBuilder()
                    .where().eq(ConversationHistory.COLUMN_PARTICIPANT, contactId).query();

            if (history == null || history.isEmpty())
                return;

            ConversationHistory conversation = history.get(0);

            DeleteBuilder<Interaction, Integer> deleteBuilder = getInteractionDataDao(accountId).deleteBuilder();
            if (deleteConversation) {
                deleteBuilder.where().eq(Interaction.COLUMN_CONVERSATION, conversation);
                getConversationDataDao(accountId).deleteById(conversation.getId());
            } else {
                deleteBuilder.where()
                        .eq(Interaction.COLUMN_CONVERSATION, conversation).and()
                        .ne(Interaction.COLUMN_TYPE, Interaction.InteractionType.CONTACT.toString());
            }

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


    /**
     * Inserts a conversation (if needed) and an interaction element. Typically used for the initial contact event.
     *
     * @param conversation the conversation object
     * @param event        the first interaction to insert
     * @param accountId    the user's account ID
     * @return a completable
     */
    Single<Integer> insertConversationAndEvent(ConversationHistory conversation, Interaction event, String accountId) {
        return Single.fromCallable(() -> {
            Log.d(TAG, "Insert conversation is running......");
            Dao<ConversationHistory, Integer> conversationDataDao = getConversationDataDao(accountId);

            ConversationHistory history = conversationDataDao.queryBuilder().where().eq(ConversationHistory.COLUMN_PARTICIPANT, conversation.getParticipant()).queryForFirst();

            if (history == null) {
                int id = conversationDataDao.create(conversation);
                event.setConversation(new ConversationHistory(id, conversation.getParticipant()));
            } else
                event.setConversation(history);


            getInteractionDataDao(accountId).createIfNotExists(event);

            return event.getConversation().getId();

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

    /**
     * Loads data required to load the smartlist. Only requires the most recent message or contact action.
     *
     * @param accountId required to query the appropriate account database
     * @return a list of the most recent interactions with each contact
     */
    public Single<List<Interaction>> getSmartlist(final String accountId) {
        return Single.fromCallable(() ->
                // a raw query is done as MAX is not supported by ormlite without a raw query and a raw query cannot be combined with an orm query so a complete raw query is done
                // raw row mapper maps the sqlite result which is a list of strings, into the interactions object
                getInteractionDataDao(accountId).queryRaw("SELECT * FROM (SELECT DISTINCT id, author, conversation, MAX(timestamp), body, type, status, daemon_id, is_read, extra_data from interactions GROUP BY interactions.conversation) as final\n" +
                        "JOIN conversations\n" +
                        "WHERE conversations.id = final.conversation\n" +
                        "GROUP BY final.conversation\n", (columnNames, resultColumns) -> new Interaction(resultColumns[0],
                        resultColumns[1], new ConversationHistory(Integer.parseInt(resultColumns[2]), resultColumns[12]), resultColumns[3], resultColumns[4], resultColumns[5], resultColumns[6], resultColumns[7], resultColumns[8], resultColumns[9])).getResults())
                .doOnError(e -> Log.e(TAG, "Can't load smartlist from database", e))
                .onErrorReturn(e -> new ArrayList<>());
    }

    /**
     * Retrieves an entire conversations history
     *
     * @param accountId      the user's account id
     * @param conversationId the conversation id
     * @return a conversation and all of its interactions
     */
    public Single<List<Interaction>> getConversationHistory(final String accountId, final int conversationId) {
        return Single.fromCallable(() -> {
            QueryBuilder<Interaction, Integer> interactionQueryBuilder = getInteractionDataDao(accountId).queryBuilder();

            QueryBuilder<ConversationHistory, Integer> conversationQueryBuilder = getConversationDataDao(accountId).queryBuilder();
            conversationQueryBuilder.where().eq(ConversationHistory.COLUMN_CONVERSATION_ID, conversationId);
            return getInteractionDataDao(accountId).query(interactionQueryBuilder.join(conversationQueryBuilder).orderBy(Interaction.COLUMN_TIMESTAMP, true).prepare());
        }).doOnError(e -> Log.e(TAG, "Can't load conversation from database", e))
                .onErrorReturn(e -> new ArrayList<>());
    }

    Single<TextMessage> incomingMessage(final String accountId, final String callId, final String from, final String message) {
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


    Single<TextMessage> accountMessageStatusChanged(String accountId, long interactionId, String to, int status) {
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