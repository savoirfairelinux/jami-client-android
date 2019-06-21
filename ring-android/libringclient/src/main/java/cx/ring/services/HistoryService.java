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

import java.util.ArrayList;
import java.util.List;

import cx.ring.model.CallContact;
import cx.ring.model.ConversationElement;
import cx.ring.model.HistoryCall;
import cx.ring.model.DataTransfer;
import cx.ring.model.HistoryText;
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
    protected abstract Dao<HistoryCall, Integer> getCallHistoryDao(String dbName);
    protected abstract Dao<HistoryText, Long> getTextHistoryDao(String dbName);
    protected abstract Dao<DataTransfer, Long> getDataHistoryDao(String dbName);
    protected abstract Object getHelper(String dbName);
    protected abstract Object initHelper(String dbName);
    protected abstract void migrateDatabase();

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Completable insertAccount(final String accountId) {
        return Completable.fromAction(() -> initHelper(accountId)).subscribeOn(scheduler);
    }

    public Completable insertNewEntry(final HistoryCall call) {
        return Completable
                .fromAction(() -> getCallHistoryDao(call.getAccountID()).create(call))
                .subscribeOn(scheduler);
    }

    public Completable insertNewTextMessage(TextMessage txt) {
        return Completable.fromAction(() -> {
            HistoryText historyTxt = new HistoryText(txt);
            getTextHistoryDao(txt.getAccount()).create(historyTxt);
            txt.setID(historyTxt.id);
        }).subscribeOn(scheduler);
    }

    public Completable updateTextMessage(final HistoryText txt) {
        return Completable.fromAction(() -> {
            Log.d(TAG, "HistoryDao().update() id:" + txt.id + " acc:" + txt.getAccountID() + " num:"
                    + txt.getNumber() + " date:" + txt.getDate() + " msg:" + txt.getMessage() + " status:" + txt.getStatus());
            getTextHistoryDao(txt.getAccountID()).update(txt);
        }).subscribeOn(scheduler);
    }

    public Completable insertDataTransfer(DataTransfer dataTransfer) {
        return Completable.fromAction(() -> getDataHistoryDao(dataTransfer.getAccountId()).create(dataTransfer))
                .subscribeOn(scheduler);
    }

    public Completable updateDataTransfer(DataTransfer dataTransfer) {
        return Completable.fromAction(() -> getDataHistoryDao(dataTransfer.getAccountId()).update(dataTransfer))
                .subscribeOn(scheduler);
    }

    public Single<List<ConversationElement>> getCallsSingle(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<HistoryCall, Integer> queryBuilder = getCallHistoryDao(accountId).queryBuilder();
            queryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(HistoryCall.COLUMN_TIMESTAMP_START_NAME, false);
            return (List<ConversationElement>)(List<?>)getCallHistoryDao(accountId).query(queryBuilder.prepare());
        }).doOnError(e -> Log.e(TAG, "Can't load calls", e))
          .onErrorReturn(e -> new ArrayList<>());
    }

    public Single<List<ConversationElement>> getMessagesSingle(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<HistoryText, Long> queryBuilder = getTextHistoryDao(accountId).queryBuilder();
            queryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(HistoryText.COLUMN_TIMESTAMP_NAME, false);
            return getTextHistoryDao(accountId).query(queryBuilder.prepare());
        }).map(l -> {
            List<ConversationElement> ret = new ArrayList<>(l.size());
            for (HistoryText t : l)
                ret.add(new TextMessage(t));
            return ret;
        }).doOnError(e -> Log.e(TAG, "Can't load messages", e))
          .onErrorReturn(e -> new ArrayList<>());
    }

    public Single<List<ConversationElement>> getTransfersSingle(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<DataTransfer, Long> queryBuilder = getDataHistoryDao(accountId).queryBuilder();
            queryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(DataTransfer.COLUMN_TIMESTAMP_NAME, false);
            return (List<ConversationElement>)(List<?>)getDataHistoryDao(accountId).query(queryBuilder.prepare());
        }).doOnError(e -> Log.e(TAG, "Can't load data transfers", e))
          .onErrorReturn(e -> new ArrayList<>());
    }

    public Completable clearHistory(final String contactId, final String accountId) {
        if (StringUtils.isEmpty(accountId))
            return Completable.complete();
        return Completable.fromAction(() -> {
            int deleted = 0;
            DeleteBuilder<HistoryText, Long> deleteTextHistoryBuilder = getTextHistoryDao(accountId)
                    .deleteBuilder();
            deleteTextHistoryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryText.COLUMN_NUMBER_NAME, contactId);
            deleted += deleteTextHistoryBuilder.delete();

            DeleteBuilder<HistoryCall, Integer> deleteCallsHistoryBuilder = getCallHistoryDao(accountId)
                    .deleteBuilder();
            deleteCallsHistoryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryCall.COLUMN_NUMBER_NAME, contactId);
            deleted += deleteCallsHistoryBuilder.delete();

            DeleteBuilder<DataTransfer, Long> deleteDataTransferHistoryBuilder = getDataHistoryDao(accountId)
                    .deleteBuilder();
            deleteDataTransferHistoryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(DataTransfer.COLUMN_PEER_ID_NAME, contactId);
            deleted += deleteDataTransferHistoryBuilder.delete();
            Log.w(TAG, "clearHistory: removed " + deleted + " elements");
        }).subscribeOn(scheduler);
    }

    public Completable clearHistory(String accountId) {
        return Completable.fromAction(() -> {
            TableUtils.clearTable(getConnectionSource(accountId), HistoryCall.class);
            TableUtils.clearTable(getConnectionSource(accountId), HistoryText.class);
            TableUtils.clearTable(getConnectionSource(accountId), DataTransfer.class);
        }).subscribeOn(scheduler);
    }

    public Single<TextMessage> incomingMessage(final String accountId, final String callId, final String from, final String message) {
        return Single.fromCallable(() -> {
            String f = from;
            if (!f.contains(CallContact.PREFIX_RING)) {
                f = CallContact.PREFIX_RING + from;
            }

            TextMessage txt = new TextMessage(true, message, new Uri(f), callId, accountId);
            Log.w(TAG, "New text messsage " + txt.getAccount() + " " + txt.getCallId() + " " + txt.getMessage());

            HistoryText t = new HistoryText(txt);
            getTextHistoryDao(accountId).create(t);
            txt.setID(t.id);
            return txt;
        }).subscribeOn(scheduler);
    }

    public Single<TextMessage> accountMessageStatusChanged(String accountId, long messageId, String to, int status) {
        return Single.fromCallable(() -> {
            HistoryText historyText = getTextHistoryDao(accountId).queryForId(messageId);
            if (historyText == null) {
                throw new RuntimeException("accountMessageStatusChanged: not able to find message with id " + messageId + " in database");
            }
            TextMessage textMessage = new TextMessage(historyText);
            if (!textMessage.getAccount().equals(accountId)) {
                throw new RuntimeException("accountMessageStatusChanged: received an invalid text message");
            }
            textMessage.setStatus(status);
            getTextHistoryDao(accountId).update(new HistoryText(textMessage));
            return textMessage;
        }).subscribeOn(scheduler);
    }

    public Completable deleteFileHistory(long id, String accountId) {
        return Completable
                .fromAction(() -> getDataHistoryDao(accountId).deleteById(id))
                .subscribeOn(scheduler);
    }

    public Completable deleteMessageHistory(long id, String accountId) {
        return Completable
                .fromAction(() -> getTextHistoryDao(accountId).deleteById(id))
                .subscribeOn(scheduler);
    }

    public Completable deleteCallHistory(CharSequence id, String accountId) {
        return Completable
                .fromAction(() -> {
                    DeleteBuilder<HistoryCall, Integer> deleteBuilder = getCallHistoryDao(accountId).deleteBuilder();
                    deleteBuilder.where().eq("callID", id);
                    deleteBuilder.delete();
                })
                .subscribeOn(scheduler);
    }
}