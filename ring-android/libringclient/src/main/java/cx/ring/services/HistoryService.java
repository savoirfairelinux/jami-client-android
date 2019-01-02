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

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationElement;
import cx.ring.model.HistoryCall;
import cx.ring.model.DataTransfer;
import cx.ring.model.HistoryEntry;
import cx.ring.model.HistoryText;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * A service managing all history related tasks.
 * Its responsibilities:
 * - inserting new entries (text or call)
 * - deleting conversations
 * - notifying Observers when history changes
 */
public abstract class HistoryService {

    private static final String TAG = HistoryService.class.getSimpleName();

    @Inject
    @Named("ApplicationExecutor")
    protected ExecutorService mApplicationExecutor;
    private final Scheduler scheduler = Schedulers.single();

    protected abstract ConnectionSource getConnectionSource();
    protected abstract Dao<HistoryCall, Integer> getCallHistoryDao();
    protected abstract Dao<HistoryText, Long> getTextHistoryDao();
    protected abstract Dao<DataTransfer, Long> getDataHistoryDao();

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Completable insertNewEntry(Conference toInsert) {
        return Completable.fromAction(() -> {
            for (SipCall call : toInsert.getParticipants()) {
                if (call.getTimestampEnd() == 0)
                    call.setTimestampEnd(System.currentTimeMillis());

                HistoryCall persistent = new HistoryCall(call);
                Log.d(TAG, "HistoryDao().create() " + persistent.getNumber() + " " + persistent.getStartDate().toString() + " " + persistent.getEndDate());
                getCallHistoryDao().create(persistent);
            }
        }).subscribeOn(scheduler);
    }

    public Completable insertNewEntry(final HistoryCall call) {
        return Completable
                .fromAction(() -> getCallHistoryDao().create(call))
                .subscribeOn(scheduler);
    }

    private boolean insertNewTextMessage(HistoryText txt) {
        try {
            Log.d(TAG, "HistoryDao().create() id:" + txt.id + " acc:" + txt.getAccountID() + " num:" + txt.getNumber() + " date:" + txt.getDate() + " msg:" + txt.getMessage());
            getTextHistoryDao().create(txt);
        } catch (SQLException e) {
            Log.e(TAG, "Error while inserting text message", e);
            return false;
        }

        return true;
    }

    public Completable insertNewTextMessage(TextMessage txt) {
        return Completable.fromCallable((Callable<Boolean>) () -> {
            HistoryText historyTxt = new HistoryText(txt);
            if (!insertNewTextMessage(historyTxt)) {
                return false;
            }
            txt.setID(historyTxt.id);
            return true;
        }).subscribeOn(scheduler);
    }

    public Completable updateTextMessage(final HistoryText txt) {
        return Completable.fromCallable(() -> {
            Log.d(TAG, "HistoryDao().update() id:" + txt.id + " acc:" + txt.getAccountID() + " num:"
                    + txt.getNumber() + " date:" + txt.getDate() + " msg:" + txt.getMessage() + " status:" + txt.getStatus());
            getTextHistoryDao().update(txt);
            return null;
        }).subscribeOn(scheduler);
    }

    public boolean insertDataTransfer(DataTransfer dataTransfer) {
        try {
            getDataHistoryDao().create(dataTransfer);
        } catch (SQLException e) {
            Log.e(TAG, "Error while inserting data transfer", e);
            return false;
        }

        return true;
    }

    public boolean updateDataTransfer(DataTransfer dataTransfer) {
        try {
            getDataHistoryDao().update(dataTransfer);
        } catch (SQLException e) {
            Log.e(TAG, "Error while updating data transfer", e);
            return false;
        }

        return true;
    }

    /*
    public Single<List<HistoryText>> getLastMessagesForAccountAndContactRingId(final String accountId, final String contactRingId) {
        return Single.fromCallable(() -> {
            QueryBuilder<HistoryText, Long> queryBuilder = getTextHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryText.COLUMN_NUMBER_NAME, contactRingId);
            queryBuilder.orderBy(HistoryText.COLUMN_TIMESTAMP_NAME, false);
            queryBuilder.limit(1L);
            return getTextHistoryDao().query(queryBuilder.prepare());
        });
    }

    public Single<List<HistoryCall>> getLastCallsForAccountAndContactRingId(final String accountId, final String contactRingId) {
        return Single.fromCallable(() -> {
            QueryBuilder<HistoryCall, Integer> queryBuilder = getCallHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryText.COLUMN_NUMBER_NAME, contactRingId);
            queryBuilder.orderBy(HistoryCall.COLUMN_TIMESTAMP_START_NAME, false);
            queryBuilder.limit(1L);
            return getCallHistoryDao().query(queryBuilder.prepare());
        });
    }

    public Observable<HistoryText> getMessagesForContact(final String accountId, final String contactId) {
        return PublishSubject.create(l -> {
            QueryBuilder<HistoryText, Long> queryBuilder = getTextHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryText.COLUMN_NUMBER_NAME, contactId);
            queryBuilder.orderBy(HistoryText.COLUMN_TIMESTAMP_NAME, false);
            CloseableIterator<HistoryText> i = getTextHistoryDao().iterator(queryBuilder.prepare());
            try {
                while (i.hasNext()) {
                    l.onNext(i.next());
                }
            } catch (Exception e) {
                l.onError(e);
            } finally {
                i.close();
            }
            l.onComplete();
        });
    }

    public Observable<HistoryCall> getCallForContact(final String accountId, final String contactId) {
        return PublishSubject.create(l -> {
            QueryBuilder<HistoryCall, Integer> queryBuilder = getCallHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryCall.COLUMN_NUMBER_NAME, contactId);
            queryBuilder.orderBy(HistoryCall.COLUMN_TIMESTAMP_START_NAME, false);
            CloseableIterator<HistoryCall> i = getCallHistoryDao().iterator(queryBuilder.prepare());
            try {
                while (i.hasNext()) {
                    l.onNext(i.next());
                }
            } catch (Exception e) {
                l.onError(e);
            } finally {
                i.close();
            }
            l.onComplete();
        });
    }
    public Observable<DataTransfer> getTransfersForContact(final String accountId, final String contactId) {
        return PublishSubject.create(l -> {
            QueryBuilder<DataTransfer, Long> queryBuilder = getDataHistoryDao().queryBuilder();
            queryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(DataTransfer.COLUMN_PEER_ID_NAME, contactId);
            queryBuilder.orderBy(DataTransfer.COLUMN_TIMESTAMP_NAME, false);
            CloseableIterator<DataTransfer> i = getDataHistoryDao().iterator(queryBuilder.prepare());
            try {
                while (i.hasNext()) {
                    l.onNext(i.next());
                }
                l.onComplete();
            } catch (Exception e) {
                l.onError(e);
            } finally {
                i.close();
            }
        });
    }*/


    public Single<List<ConversationElement>> getCallsSingle(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<HistoryCall, Integer> queryBuilder = getCallHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(HistoryCall.COLUMN_TIMESTAMP_START_NAME, false);
            return (List<ConversationElement>)(List<?>)getCallHistoryDao().query(queryBuilder.prepare());
        });
    }

    public Single<List<ConversationElement>> getMessagesSingle(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<HistoryText, Long> queryBuilder = getTextHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(HistoryText.COLUMN_TIMESTAMP_NAME, false);
            return getTextHistoryDao().query(queryBuilder.prepare());
        }).map(l -> {
            List<ConversationElement> ret = new ArrayList<>(l.size());
            for (HistoryText t : l)
                ret.add(new TextMessage(t));
            return ret;
        });
    }

    public Single<List<ConversationElement>> getTransfersSingle(final String accountId) {
        return Single.fromCallable(() -> {
            QueryBuilder<DataTransfer, Long> queryBuilder = getDataHistoryDao().queryBuilder();
            queryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(DataTransfer.COLUMN_TIMESTAMP_NAME, false);
            return (List<ConversationElement>)(List<?>)getDataHistoryDao().query(queryBuilder.prepare());
        });
    }

    public Observable<ConversationElement> getMessages(final String accountId) {
        return PublishSubject.<HistoryText>create(l -> {
            QueryBuilder<HistoryText, Long> queryBuilder = getTextHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(HistoryText.COLUMN_TIMESTAMP_NAME, false);
            CloseableIterator<HistoryText> i = getTextHistoryDao().iterator(queryBuilder.prepare());
            try {
                while (i.hasNext()) {
                    l.onNext(i.next());
                }
            } catch (Exception e) {
                l.onError(e);
            } finally {
                i.close();
            }
            l.onComplete();
        }).map(TextMessage::new);
    }

    public Observable<ConversationElement> getCalls(final String accountId) {
        return PublishSubject.create(l -> {
            QueryBuilder<HistoryCall, Integer> queryBuilder = getCallHistoryDao().queryBuilder();
            queryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(HistoryCall.COLUMN_TIMESTAMP_START_NAME, false);
            CloseableIterator<HistoryCall> i = getCallHistoryDao().iterator(queryBuilder.prepare());
            try {
                while (i.hasNext()) {
                    l.onNext(i.next());
                }
                l.onComplete();
            } catch (Exception e) {
                l.onError(e);
            } finally {
                i.close();
            }
        });
    }
    public Observable<ConversationElement> getTransfers(final String accountId) {
        return PublishSubject.create(l -> {
            QueryBuilder<DataTransfer, Long> queryBuilder = getDataHistoryDao().queryBuilder();
            queryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId);
            queryBuilder.orderBy(DataTransfer.COLUMN_TIMESTAMP_NAME, false);
            CloseableIterator<DataTransfer> i = getDataHistoryDao().iterator(queryBuilder.prepare());
            try {
                while (i.hasNext()) {
                    l.onNext(i.next());
                }
                l.onComplete();
            } catch (Exception e) {
                l.onError(e);
            } finally {
                i.close();
            }
        });
    }

    private List<HistoryText> getHistoryTexts(String accountId, String contactRingId) throws SQLException {
        QueryBuilder<HistoryText, Long> queryBuilder = getTextHistoryDao().queryBuilder();
        queryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryText.COLUMN_NUMBER_NAME, contactRingId);
        queryBuilder.orderBy(HistoryText.COLUMN_TIMESTAMP_NAME, true);
        return getTextHistoryDao().query(queryBuilder.prepare());
    }

    public Single<List<HistoryCall>> getAllCallsForAccountAndContactRingId(final String accountId, final String contactRingId) {
        return Single.fromCallable(() -> getHistoryCalls(accountId, contactRingId));
    }

    private List<HistoryCall> getHistoryCalls(String accountId, String contactRingId) throws SQLException {
        QueryBuilder<HistoryCall, Integer> queryBuilder = getCallHistoryDao().queryBuilder();
        queryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryCall.COLUMN_NUMBER_NAME, contactRingId);
        queryBuilder.orderBy(HistoryCall.COLUMN_TIMESTAMP_START_NAME, true);
        return getCallHistoryDao().query(queryBuilder.prepare());
    }

    public Single<List<DataTransfer>> getAllFilesForAccount(final String accountId) {
        return Single.fromCallable(() -> getHistoryDataTransfers(accountId));
    }

    public Single<List<DataTransfer>> getAllFilesForAccountAndContactRingId(final String accountId, final String contactRingId) {
        return Single.fromCallable(() -> getHistoryDataTransfers(accountId, contactRingId));
    }



    public List<DataTransfer> getHistoryDataTransfers(String accountId) throws SQLException {
        QueryBuilder<DataTransfer, Long> queryBuilder = getDataHistoryDao().queryBuilder();
        queryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId);
        queryBuilder.orderBy(DataTransfer.COLUMN_TIMESTAMP_NAME, true);
        return getDataHistoryDao().query(queryBuilder.prepare());
    }

    public List<DataTransfer> getHistoryDataTransfers(String accountId, String contactRingId) throws SQLException {
        QueryBuilder<DataTransfer, Long> queryBuilder = getDataHistoryDao().queryBuilder();
        queryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(DataTransfer.COLUMN_PEER_ID_NAME, contactRingId);
        queryBuilder.orderBy(DataTransfer.COLUMN_TIMESTAMP_NAME, true);
        return getDataHistoryDao().query(queryBuilder.prepare());
    }

    public Completable clearHistory(final String contactId, final String accountId) {
        if (StringUtils.isEmpty(accountId))
            return Completable.complete();
        return Completable.fromAction(() -> {
            int deleted = 0;
            DeleteBuilder<HistoryText, Long> deleteTextHistoryBuilder = getTextHistoryDao()
                    .deleteBuilder();
            deleteTextHistoryBuilder.where().eq(HistoryText.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryText.COLUMN_NUMBER_NAME, contactId);
            deleted += deleteTextHistoryBuilder.delete();

            DeleteBuilder<HistoryCall, Integer> deleteCallsHistoryBuilder = getCallHistoryDao()
                    .deleteBuilder();
            deleteCallsHistoryBuilder.where().eq(HistoryCall.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(HistoryCall.COLUMN_NUMBER_NAME, contactId);
            deleted += deleteCallsHistoryBuilder.delete();

            DeleteBuilder<DataTransfer, Long> deleteDataTransferHistoryBuilder = getDataHistoryDao()
                    .deleteBuilder();
            deleteDataTransferHistoryBuilder.where().eq(DataTransfer.COLUMN_ACCOUNT_ID_NAME, accountId).and().eq(DataTransfer.COLUMN_PEER_ID_NAME, contactId);
            deleted += deleteDataTransferHistoryBuilder.delete();
            Log.w(TAG, "clearHistory: removed " + deleted + " elements");
        }).subscribeOn(scheduler);
    }

    private HistoryText getTextMessage(long id) throws SQLException {
        return getTextHistoryDao().queryForId(id);
    }

    /**
     * Removes all the text messages and call histories from the database.
     *
     * @param conversation The conversation containing the elements to delete.
     */
    public void clearHistoryForConversation(final Conversation conversation) {
        if (conversation == null) {
            Log.d(TAG, "clearHistoryForConversation: conversation is null");
            return;
        }

        mApplicationExecutor.submit(() -> {
            try {
                Map<String, HistoryEntry> history = conversation.getRawHistory();
                for (Map.Entry<String, HistoryEntry> entry : history.entrySet()) {
                    //~ Deleting messages
                    ArrayList<Long> textMessagesIds = new ArrayList<>(entry.getValue().getTextMessages().size());
                    for (TextMessage textMessage : entry.getValue().getTextMessages().values()) {
                        textMessagesIds.add(textMessage.getId());
                    }
                    DeleteBuilder<HistoryText, Long> deleteTextHistoryBuilder = getTextHistoryDao()
                            .deleteBuilder();
                    deleteTextHistoryBuilder.where().in(HistoryText.COLUMN_ID_NAME, textMessagesIds);
                    deleteTextHistoryBuilder.delete();

                    //~ Deleting calls
                    ArrayList<String> callIds = new ArrayList<>(entry.getValue().getCalls().size());
                    for (HistoryCall historyCall : entry.getValue().getCalls().values()) {
                        callIds.add(historyCall.getCallId().toString());
                    }
                    DeleteBuilder<HistoryCall, Integer> deleteCallsHistoryBuilder = getCallHistoryDao()
                            .deleteBuilder();
                    deleteCallsHistoryBuilder.where().in(HistoryCall.COLUMN_CALL_ID_NAME, callIds);
                    deleteCallsHistoryBuilder.delete();

                    //~ Deleting data transfers
                    ArrayList<Long> dataTransferIds = new ArrayList<>(entry.getValue().getDataTransfers().size());
                    for (DataTransfer dataTransfer : entry.getValue().getDataTransfers().values()) {
                        dataTransferIds.add(dataTransfer.getId());
                    }
                    DeleteBuilder<DataTransfer, Long> deleteDataTransfersHistoryBuilder = getDataHistoryDao()
                            .deleteBuilder();
                    deleteDataTransfersHistoryBuilder.where().in(DataTransfer.COLUMN_ID_NAME, dataTransferIds);
                    deleteDataTransfersHistoryBuilder.delete();
                }

                // notify the observers
                //setChanged();
                //ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.HISTORY_MODIFIED);
                //notifyObservers(event);
            } catch (SQLException e) {
                Log.e(TAG, "Error while clearing history for conversation", e);
            }
        });

    }

    public void clearHistory() {
        try {
            TableUtils.clearTable(getConnectionSource(), HistoryCall.class);
            TableUtils.clearTable(getConnectionSource(), HistoryText.class);
            TableUtils.clearTable(getConnectionSource(), DataTransfer.class);

            // notify the observers
            //setChanged();
            //notifyObservers();
        } catch (SQLException e) {
            Log.e(TAG, "Error while clearing history tables", e);
        }
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
            getTextHistoryDao().create(t);
            txt.setID(t.id);
            return txt;
        }).subscribeOn(scheduler);
    }

    public Single<TextMessage> accountMessageStatusChanged(String accountId, long messageId, String to, int status) {
        return Single.fromCallable(() -> {
            HistoryText historyText = getTextHistoryDao().queryForId(messageId);
            if (historyText == null) {
                throw new RuntimeException("accountMessageStatusChanged: not able to find message with id " + messageId + " in database");
            }
            TextMessage textMessage = new TextMessage(historyText);
            if (!textMessage.getAccount().equals(accountId)) {
                throw new RuntimeException("accountMessageStatusChanged: received an invalid text message");
            }
            textMessage.setStatus(status);
            getTextHistoryDao().update(new HistoryText(textMessage));
            return textMessage;
        }).subscribeOn(scheduler);
    }

    public boolean hasAnHistory(String accountId, String contactRingId) {
        try {
            List<HistoryCall> historyCalls = getHistoryCalls(accountId, contactRingId);
            if (!historyCalls.isEmpty()) {
                return true;
            }

            List<HistoryText> historyTexts = getHistoryTexts(accountId, contactRingId);
            if (!historyTexts.isEmpty()) {
                return true;
            }

            List<DataTransfer> historyData = getHistoryDataTransfers(accountId, contactRingId);
            if (!historyData.isEmpty()) {
                return true;
            }
        } catch (SQLException e) {
            Log.e(TAG, "hasAnHistory: a sql error occurred", e);
        }
        return false;
    }

    public Completable deleteFileHistory(long id) {
        return Completable
                .fromAction(() -> getDataHistoryDao().deleteById(id))
                .subscribeOn(scheduler);
    }
}