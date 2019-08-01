/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.history;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.model.ConversationHistory;
import cx.ring.model.DataTransfer;
import cx.ring.model.Interaction;
import cx.ring.services.HistoryService;

/*
 * Database History Version
 * 7 : changing columns names. See https://gerrit-ring.savoirfairelinux.com/#/c/4297
 * 10: Switches to per account database system and implements new interaction and conversations table.
 */

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getSimpleName();
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 10;

    private Dao<Interaction, Integer> interactionDataDao = null;
    private Dao<ConversationHistory, Integer> conversationDataDao = null;

    @Inject
    HistoryService mHistoryService;

    public DatabaseHelper(Context context, String dbDirectory) {
        super(context, dbDirectory, null, DATABASE_VERSION);
        Log.d(TAG, "Helper initialized for " + dbDirectory);
        ((RingApplication) context.getApplicationContext()).getRingInjectionComponent().inject(this);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            db.beginTransaction();
            try {
                TableUtils.createTable(connectionSource, ConversationHistory.class);
                TableUtils.createTable(connectionSource, Interaction.class);
                db.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.e(TAG, "Can't create database", e);
                throw new RuntimeException(e);
            }
        } finally {
            db.endTransaction();
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade " + oldVersion + " -> " + newVersion);
        try {
            // if we are under version 10, it must first wait for account splitting to be complete which occurs in history service
            if (oldVersion >= 10)
                updateDatabase(oldVersion, db, connectionSource);
            else
                mHistoryService.getMigrationStatus().firstOrError().subscribe(migrationStatus -> {
                    if (migrationStatus == HistoryService.MigrationStatus.LEGACY_DELETED || migrationStatus == HistoryService.MigrationStatus.SUCCESSFUL)
                        updateDatabase(oldVersion, db, connectionSource);
                });
        } catch (SQLException exc) {
            exc.printStackTrace();
            clearDatabase(db);
            onCreate(db, connectionSource);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */

    public Dao<Interaction, Integer> getInteractionDataDao() throws SQLException {
        if (interactionDataDao == null) {
            interactionDataDao = getDao(Interaction.class);
        }
        return interactionDataDao;
    }

    public Dao<ConversationHistory, Integer> getConversationDataDao() throws SQLException {
        if (conversationDataDao == null) {
            conversationDataDao = getDao(ConversationHistory.class);
        }
        return conversationDataDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        interactionDataDao = null;
        conversationDataDao = null;
    }

    /**
     * Main method to update the database from an old version to the last
     *
     * @param fromDatabaseVersion the old version of the database
     * @param db                  the SQLiteDatabase to work with
     * @throws SQLiteException database has failed to update to the last version
     */
    private void updateDatabase(int fromDatabaseVersion, SQLiteDatabase db, ConnectionSource connectionSource) throws SQLException {
        try {
            while (fromDatabaseVersion < DATABASE_VERSION) {
                switch (fromDatabaseVersion) {
                    case 6:
                        updateDatabaseFrom6(db);
                        break;
                    case 7:
                        updateDatabaseFrom7(db);
                        break;
                    case 8:
                        updateDatabaseFrom8(connectionSource);
                        break;
                    case 9:
                        updateDatabaseFrom9(db);
                        break;
                }
                fromDatabaseVersion++;
            }
            Log.d(TAG, "updateDatabase: Database has been updated to the last version.");
        } catch (SQLException exc) {
            Log.e(TAG, "updateDatabase: Database has failed to update to the last version.");
            throw exc;
        }
    }

    /**
     * Executes the migration from the database version 6 to the next
     *
     * @param db the SQLiteDatabase to work with
     * @throws SQLiteException migration from database version 6 to next, failed
     */
    private void updateDatabaseFrom6(SQLiteDatabase db) throws SQLiteException {
        if (db != null && db.isOpen()) {
            try {
                Log.d(TAG, "updateDatabaseFrom6: Will begin migration from database version 6 to next.");
                db.beginTransaction();
                //~ Create the new historyCall table and int index
                db.execSQL("CREATE TABLE IF NOT EXISTS `historycall` (`accountID` VARCHAR , `callID` VARCHAR , " +
                        "`call_end` BIGINT , `TIMESTAMP_START` BIGINT , `contactID` BIGINT , " +
                        "`contactKey` VARCHAR , `direction` INTEGER , `missed` SMALLINT , " +
                        "`number` VARCHAR , `recordPath` VARCHAR ) ;");
                db.execSQL("CREATE INDEX IF NOT EXISTS `historycall_TIMESTAMP_START_idx` ON `historycall` " +
                        "( `TIMESTAMP_START` );");
                //~ Create the new historyText table and int indexes
                db.execSQL("CREATE TABLE IF NOT EXISTS `historytext` (`accountID` VARCHAR , `callID` VARCHAR , " +
                        "`contactID` BIGINT , `contactKey` VARCHAR , `direction` INTEGER , " +
                        "`id` BIGINT , `message` VARCHAR , `number` VARCHAR , `read` SMALLINT , " +
                        "`TIMESTAMP` BIGINT , PRIMARY KEY (`id`) );");
                db.execSQL("CREATE INDEX IF NOT EXISTS `historytext_TIMESTAMP_idx` ON `historytext` ( `TIMESTAMP` );");
                db.execSQL("CREATE INDEX IF NOT EXISTS `historytext_id_idx` ON `historytext` ( `id` );");

                Cursor hasATable = db.rawQuery("SELECT name FROM sqlite_master WHERE type=? AND name=?;",
                        new String[]{"table", "a"});
                if (hasATable.getCount() > 0) {
                    //~ Copying data from the old table "a"
                    db.execSQL("INSERT INTO `historycall` (TIMESTAMP_START, call_end, number, missed," +
                            "direction, recordPath, accountID, contactID, contactKey, callID) " +
                            "SELECT TIMESTAMP_START,b,c,d,e,f,g,h,i,j FROM a;");
                    db.execSQL("DROP TABLE IF EXISTS a_TIMESTAMP_START_idx;");
                    db.execSQL("DROP TABLE a;");
                }
                hasATable.close();

                Cursor hasETable = db.rawQuery("SELECT name FROM sqlite_master WHERE type=? AND name=?;",
                        new String[]{"table", "e"});
                if (hasETable.getCount() > 0) {
                    //~ Copying data from the old table "e"
                    db.execSQL("INSERT INTO historytext (id, TIMESTAMP, number, direction, accountID," +
                            "contactID, contactKey, callID, message, read) " +
                            "SELECT id,TIMESTAMP,c,d,e,f,g,h,i,j FROM e;");
                    //~ Remove old tables "a" and "e"
                    db.execSQL("DROP TABLE IF EXISTS e_TIMESTAMP_idx;");
                    db.execSQL("DROP TABLE IF EXISTS e_id_idx;");
                    db.execSQL("DROP TABLE e;");
                }
                hasETable.close();

                db.setTransactionSuccessful();
                db.endTransaction();
                Log.d(TAG, "updateDatabaseFrom6: Migration from database version 6 to next, done.");
            } catch (SQLiteException exception) {
                Log.e(TAG, "updateDatabaseFrom6: Migration from database version 6 to next, failed.");
                throw exception;
            }
        }
    }

    private void updateDatabaseFrom7(SQLiteDatabase db) throws SQLiteException {
        if (db != null && db.isOpen()) {
            try {
                Log.d(TAG, "updateDatabaseFrom7: Will begin migration from database version 7 to next.");
                db.beginTransaction();
                db.execSQL("ALTER TABLE historytext ADD COLUMN state VARCHAR DEFAULT ''");
                db.setTransactionSuccessful();
                db.endTransaction();
                Log.d(TAG, "updateDatabaseFrom7: Migration from database version 7 to next, done.");
            } catch (SQLiteException exception) {
                Log.e(TAG, "updateDatabaseFrom7: Migration from database version 7 to next, failed.");
                throw exception;
            }
        }
    }

    private void updateDatabaseFrom8(ConnectionSource connectionSource) throws SQLException {
        try {
            TableUtils.createTable(connectionSource, DataTransfer.class);
            Log.d(TAG, "Migration from database version 8 to next, done.");
        } catch (SQLException e) {
            Log.e(TAG, "Migration from database version 8 to next, failed.", e);
            throw e;
        }
    }

    /**
     * This updates the database to version 10 which includes the switch to interaction and conversation tables.
     * It will delete previous tables.
     *
     * @param db the database to migrate
     * @throws SQLiteException
     */
    private void updateDatabaseFrom9(SQLiteDatabase db) throws SQLiteException {
        if (db != null && db.isOpen()) {
            try {
                Log.d(TAG, "updateDatabaseFrom9: Will begin migration from database version 9 to next for db: " + db.getPath());
                db.beginTransaction();

                // removing ring prefix from both call and text database

                // where clause improves performance (not required)

                db.execSQL("UPDATE historytext \n" +
                        "SET number = replace( number, 'ring:', '' )\n" +
                        "WHERE number LIKE 'ring:%'");

                db.execSQL("UPDATE historycall \n" +
                        "SET number = replace( number, 'ring:', '' )\n" +
                        "WHERE number LIKE 'ring:%'");

                // populating conversations table

                db.execSQL("INSERT INTO conversations (participant)\n" +
                        "SELECT DISTINCT historytext.number\n" +
                        "FROM historytext\n" +
                        "LEFT JOIN historycall ON historycall.number = historytext.number\n" +
                        "LEFT JOIN historydata ON historytext.number = historydata.peerId\n");


                // MESSAGE TABLE

                // updating status in text message table

                db.execSQL("UPDATE historytext SET state='SUCCEEDED' WHERE state='SENT'");
                db.execSQL("UPDATE historytext SET state='SUCCEEDED' WHERE state='READ'");
                db.execSQL("UPDATE historytext SET state='FAILED' WHERE state='FAILURE'");
                db.execSQL("UPDATE historytext SET state='INVALID' WHERE state=null");

                // migration

                // divided into two similar functions, where author needs to be null in case of outgoing message

                db.execSQL("INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)\n" +
                        "SELECT null, historytext.id, historytext.message, historytext.read, historytext.state, historytext.TIMESTAMP, 'TEXT','{}', conversations.id\n" +
                        "FROM historytext\n" +
                        "JOIN conversations ON conversations.participant = historytext.number\n" +
                        "WHERE direction = 2\n"
                );

                db.execSQL("INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)\n" +
                        "SELECT historytext.number, historytext.id, historytext.message, historytext.read, historytext.state, historytext.TIMESTAMP, 'TEXT','{}', conversations.id\n" +
                        "FROM historytext\n" +
                        "JOIN conversations ON conversations.participant = historytext.number\n" +
                        "WHERE direction = 1\n"
                );


                // CALL TABLE

                // setting the timestamp end to the duration string before migration
                db.execSQL("UPDATE historycall SET call_end='{\"duration\":' || (historycall.call_end - historycall.TIMESTAMP_START) || '}' WHERE missed = 0");
                db.execSQL("UPDATE historycall SET call_end='{}' WHERE missed = 1");


                db.execSQL("INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)\n" +
                        "SELECT null, historycall.callID, null, 1, 'SUCCEEDED', historycall.TIMESTAMP_START, 'CALL', historycall.call_end, conversations.id\n" +
                        "FROM historycall\n" +
                        "JOIN conversations ON conversations.participant = historycall.number\n" +
                        "WHERE direction = 1\n"
                );

                db.execSQL("INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)\n" +
                        "SELECT historycall.number, historycall.callID, null, 1, 'SUCCEEDED', historycall.TIMESTAMP_START, 'CALL', historycall.call_end, conversations.id\n" +
                        "FROM historycall\n" +
                        "JOIN conversations ON conversations.participant = historycall.number\n" +
                        "WHERE direction = 0\n"
                );

                // DATA TRANSFER TABLE

                // updating the statuses to the new schema

                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_CREATED' WHERE dataTransferEventCode='CREATED'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_ERROR' WHERE dataTransferEventCode='UNSUPPORTED'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_AWAITING_PEER' WHERE dataTransferEventCode='WAIT_PEER_ACCEPTANCE'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_AWAITING_HOST' WHERE dataTransferEventCode='WAIT_HOST_ACCEPTANCE'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_ONGOING' WHERE dataTransferEventCode='ONGOING'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_FINISHED' WHERE dataTransferEventCode='FINISHED'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_UNJOINABLE_PEER' WHERE dataTransferEventCode='CLOSED_BY_HOST'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_UNJOINABLE_PEER' WHERE dataTransferEventCode='CLOSED_BY_PEER'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_ERROR' WHERE dataTransferEventCode='INVALID_PATHNAME'");
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_UNJOINABLE_PEER' WHERE dataTransferEventCode='UNJOINABLE_PEER'");

                // migration

                db.execSQL("INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)\n" +
                        "SELECT historydata.peerId, null, historydata.displayName, 1, historydata.dataTransferEventCode, historydata.TIMESTAMP, 'DATA_TRANSFER', '{}', conversations.id\n" +
                        "FROM historydata\n" +
                        "JOIN conversations ON conversations.participant = historydata.peerId\n" +
                        "WHERE isOutgoing = 0\n"
                );

                db.execSQL("INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)\n" +
                        "SELECT null, null, historydata.displayName, 1, historydata.dataTransferEventCode, historydata.TIMESTAMP, 'DATA_TRANSFER', '{}', conversations.id\n" +
                        "FROM historydata\n" +
                        "JOIN conversations ON conversations.participant = historydata.peerId\n" +
                        "WHERE isOutgoing = 1\n"
                );


                // drop old tables

                db.execSQL("DROP TABLE historycall;");
                db.execSQL("DROP TABLE historytext;");
                db.execSQL("DROP TABLE historydata;");

                db.setTransactionSuccessful();
                db.endTransaction();
                Log.d(TAG, "updateDatabaseFrom9: Migration from database version 9 to next, done.");
            } catch (SQLiteException exception) {
                Log.e(TAG, "updateDatabaseFrom9: Migration from database version 9 to next, failed.", exception);
            }
        }
    }

    /**
     * Removes all the data from the database, ie all the tables.
     *
     * @param db the SQLiteDatabase to work with
     */
    private void clearDatabase(SQLiteDatabase db) {
        if (db != null && db.isOpen()) {
            Log.d(TAG, "clearDatabase: Will clear database.");
            ArrayList<String> tableNames = new ArrayList<>();
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    tableNames.add(c.getString(0));
                    c.moveToNext();
                }
            }
            c.close();

            try {
                db.beginTransaction();
                for (String tableName : tableNames) {
                    db.execSQL("DROP TABLE " + tableName + ";");
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                Log.d(TAG, "clearDatabase: Database is cleared");
            } catch (SQLiteException exc) {
                exc.printStackTrace();
            }
        }
    }
}
