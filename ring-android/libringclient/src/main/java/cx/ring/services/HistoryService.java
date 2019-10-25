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
import com.j256.ormlite.support.ConnectionSource;

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

    public abstract Observable<MigrationStatus> getMigrationStatus();

    public enum MigrationStatus {
        FAILED, SUCCESSFUL, LEGACY_DELETED
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Clears a conversation's history
     *
     * @param contactId          the participant's contact ID
     * @param accountId          the user's contact ID
     * @param deleteConversation true to completely delete the conversation including contact events
     * @return
     */
    public Completable clearHistory(final String contactId, final String accountId, boolean deleteConversation) {
        if (StringUtils.isEmpty(accountId))
            return Completable.complete();
        return Completable.fromAction(() -> {
            int deleted = 0;

            ConversationHistory conversation = getConversationDataDao(accountId).queryBuilder()
                    .where().eq(ConversationHistory.COLUMN_PARTICIPANT, contactId).queryForFirst();

            if (conversation == null)
                return;

            DeleteBuilder<Interaction, Integer> deleteBuilder = getInteractionDataDao(accountId).deleteBuilder();
            if (deleteConversation) {
                // complete delete, remove conversation and all interactions
                deleteBuilder.where().eq(Interaction.COLUMN_CONVERSATION, conversation.getId());
                getConversationDataDao(accountId).deleteById(conversation.getId());
            } else {
                // keep conversation and contact event interactions
                deleteBuilder.where()
                        .eq(Interaction.COLUMN_CONVERSATION, conversation.getId()).and()
                        .ne(Interaction.COLUMN_TYPE, Interaction.InteractionType.CONTACT.toString());
            }

            deleted += deleteBuilder.delete();

            Log.w(TAG, "clearHistory: removed " + deleted + " elements");
        }).subscribeOn(scheduler);
    }

    /**
     * Clears all interactions in the app. Maintains contact events and actual conversations.
     *
     * @param accounts the list of accounts in the app
     * @return a completable
     */
    public Completable clearHistory(List<Account> accounts) {
        return Completable.fromAction(() -> {
            for (Account account : accounts) {
                String accountId = account.getAccountID();
                DeleteBuilder<Interaction, Integer> deleteBuilder = getInteractionDataDao(accountId).deleteBuilder();
                deleteBuilder.where()
                        .ne(Interaction.COLUMN_TYPE, Interaction.InteractionType.CONTACT.toString());
                deleteBuilder.delete();
            }
        }).subscribeOn(scheduler);
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
     * Inserts an interaction into the database, and if necessary, a conversation
     *
     * @param accountId    the user's account ID
     * @param conversation the conversation
     * @param interaction  the interaction to insert
     * @return a conversation single
     */
    public Completable insertInteraction(String accountId, Conversation conversation, Interaction interaction) {
        return Completable.fromAction(() -> {
            Log.d(TAG, "Inserting interaction for account -> " + accountId);
            Dao<ConversationHistory, Integer> conversationDataDao = getConversationDataDao(accountId);

            ConversationHistory history = conversationDataDao.queryBuilder().where().eq(ConversationHistory.COLUMN_PARTICIPANT, conversation.getParticipant()).queryForFirst();

            if (history == null) {
                history = conversationDataDao.createIfNotExists(new ConversationHistory(conversation.getParticipant()));
                interaction.setConversation(history);
            } else
                interaction.setConversation(history);


            conversation.setId(history.getId());
            getInteractionDataDao(accountId).create(interaction);
        })
                .doOnError(e -> Log.e(TAG, "Can't insert interaction", e))
                .subscribeOn(scheduler);
    }

    /**
     * Loads data required to load the smartlist. Only requires the most recent message or contact action.
     *
     * @param accountId required to query the appropriate account database
     * @return a list of the most recent interactions with each contact
     */
    public Single<List<Interaction>> getSmartlist(final String accountId) {
        Log.d(TAG, "Loading smartlist");
        return Single.fromCallable(() ->
                // a raw query is done as MAX is not supported by ormlite without a raw query and a raw query cannot be combined with an orm query so a complete raw query is done
                // raw row mapper maps the sqlite result which is a list of strings, into the interactions object
                getInteractionDataDao(accountId).queryRaw("SELECT * FROM (SELECT DISTINCT id, author, conversation, MAX(timestamp), body, type, status, daemon_id, is_read, extra_data from interactions GROUP BY interactions.conversation) as final\n" +
                        "JOIN conversations\n" +
                        "WHERE conversations.id = final.conversation\n" +
                        "GROUP BY final.conversation\n", (columnNames, resultColumns) -> new Interaction(resultColumns[0],
                        resultColumns[1], new ConversationHistory(Integer.parseInt(resultColumns[2]), resultColumns[12]), resultColumns[3], resultColumns[4], resultColumns[5], resultColumns[6], resultColumns[7], resultColumns[8], resultColumns[9])).getResults())
                .subscribeOn(scheduler).doOnError(e -> Log.e(TAG, "Can't load smartlist from database", e))
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
        Log.d(TAG, "Loading conversation history:  Account ID -> " + accountId + ", ConversationID -> " + conversationId);
        return Single.fromCallable(() -> {
            Dao<Interaction, Integer> interactionDataDao = getInteractionDataDao(accountId);

            return interactionDataDao.query(interactionDataDao.queryBuilder()
                    .orderBy(Interaction.COLUMN_TIMESTAMP, true)
                    .where().eq(Interaction.COLUMN_CONVERSATION, conversationId)
                    .prepare());

        }).subscribeOn(scheduler).doOnError(e -> Log.e(TAG, "Can't load conversation from database", e))
                .onErrorReturn(e -> new ArrayList<>());
    }

    Single<TextMessage> incomingMessage(final String accountId, final String daemonId, final String from, final String message) {
        return Single.fromCallable(() -> {
            String f = new Uri(from).getUri();
            Dao<ConversationHistory, Integer> conversationDataDao = getConversationDataDao(accountId);

            ConversationHistory conversation = conversationDataDao.queryBuilder().where().eq(ConversationHistory.COLUMN_PARTICIPANT, f).queryForFirst();

            if (conversation == null) {
                conversation = new ConversationHistory(f);
                conversation.setId(conversationDataDao.extractId(conversationDataDao.createIfNotExists(conversation)));
            }

            TextMessage txt = new TextMessage(f, accountId, daemonId, conversation, message);
            txt.setStatus(Interaction.InteractionStatus.SUCCESS);


            Log.w(TAG, "New text messsage " + txt.getAuthor() + " " + txt.getDaemonId() + " " + txt.getBody());
            getInteractionDataDao(accountId).create(txt);
            return txt;
        }).subscribeOn(scheduler);
    }


    Single<TextMessage> accountMessageStatusChanged(String accountId, long daemonId, String to, int status) {
        return Single.fromCallable(() -> {
            List<Interaction> textList = getInteractionDataDao(accountId).queryForEq(Interaction.COLUMN_DAEMON_ID, Long.toString(daemonId));
            if (textList == null || textList.isEmpty()) {
                throw new RuntimeException("accountMessageStatusChanged: not able to find message with id " + daemonId + " in database");
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