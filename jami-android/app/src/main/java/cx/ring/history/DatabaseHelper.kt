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
package cx.ring.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import net.jami.model.ConversationHistory
import net.jami.model.DataTransfer
import net.jami.model.Interaction
import java.sql.SQLException
import java.util.*

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
class DatabaseHelper(context: Context?, dbDirectory: String) :
    OrmLiteSqliteOpenHelper(context, dbDirectory, null, DATABASE_VERSION) {
    val interactionDataDao: Dao<Interaction, Int> by lazy { getDao(Interaction::class.java) }
    val conversationDataDao: Dao<ConversationHistory, Int> by lazy { getDao(ConversationHistory::class.java) }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    override fun onCreate(db: SQLiteDatabase, connectionSource: ConnectionSource) {
        try {
            db.beginTransaction()
            try {
                TableUtils.createTable(connectionSource, ConversationHistory::class.java)
                TableUtils.createTable(connectionSource, Interaction::class.java)
                db.setTransactionSuccessful()
            } catch (e: SQLException) {
                Log.e(TAG, "Can't create database", e)
                throw RuntimeException(e)
            }
        } finally {
            db.endTransaction()
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    override fun onUpgrade(
        db: SQLiteDatabase,
        connectionSource: ConnectionSource,
        oldVersion: Int,
        newVersion: Int
    ) {
        Log.i(TAG, "onUpgrade $oldVersion -> $newVersion")
        try {
            // if we are under version 10, it must first wait for account splitting to be complete which occurs in history service
            if (oldVersion >= 10) updateDatabase(oldVersion, db, connectionSource)
        } catch (exc: SQLException) {
            exc.printStackTrace()
            clearDatabase(db)
            onCreate(db, connectionSource)
        }
    }

    /**
     * Main method to update the database from an old version to the last
     *
     * @param fromDatabaseVersion the old version of the database
     * @param db                  the SQLiteDatabase to work with
     * @throws SQLiteException database has failed to update to the last version
     */
    @Throws(SQLException::class)
    private fun updateDatabase(
        fromDatabaseVersion: Int,
        db: SQLiteDatabase,
        connectionSource: ConnectionSource
    ) {
        var fromVersion = fromDatabaseVersion
        try {
            while (fromVersion < DATABASE_VERSION) {
                when (fromVersion) {
                    6 -> updateDatabaseFrom6(db)
                    7 -> updateDatabaseFrom7(db)
                    8 -> updateDatabaseFrom8(connectionSource)
                    9 -> updateDatabaseFrom9(db)
                }
                fromVersion++
            }
            Log.d(TAG, "updateDatabase: Database has been updated to the last version.")
        } catch (exc: SQLException) {
            Log.e(TAG, "updateDatabase: Database has failed to update to the last version.")
            throw exc
        }
    }

    /**
     * Executes the migration from the database version 6 to the next
     *
     * @param db the SQLiteDatabase to work with
     * @throws SQLiteException migration from database version 6 to next, failed
     */
    @Throws(SQLiteException::class)
    private fun updateDatabaseFrom6(db: SQLiteDatabase?) {
        if (db != null && db.isOpen) {
            try {
                Log.d(
                    TAG,
                    "updateDatabaseFrom6: Will begin migration from database version 6 to next."
                )
                db.beginTransaction()
                //~ Create the new historyCall table and int index
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `historycall` (`accountID` VARCHAR , `callID` VARCHAR , " +
                            "`call_end` BIGINT , `TIMESTAMP_START` BIGINT , `contactID` BIGINT , " +
                            "`contactKey` VARCHAR , `direction` INTEGER , `missed` SMALLINT , " +
                            "`number` VARCHAR , `recordPath` VARCHAR ) ;"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `historycall_TIMESTAMP_START_idx` ON `historycall` " +
                            "( `TIMESTAMP_START` );"
                )
                //~ Create the new historyText table and int indexes
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `historytext` (`accountID` VARCHAR , `callID` VARCHAR , " +
                            "`contactID` BIGINT , `contactKey` VARCHAR , `direction` INTEGER , " +
                            "`id` BIGINT , `message` VARCHAR , `number` VARCHAR , `read` SMALLINT , " +
                            "`TIMESTAMP` BIGINT , PRIMARY KEY (`id`) );"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `historytext_TIMESTAMP_idx` ON `historytext` ( `TIMESTAMP` );")
                db.execSQL("CREATE INDEX IF NOT EXISTS `historytext_id_idx` ON `historytext` ( `id` );")
                db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type=? AND name=?;",
                    arrayOf("table", "a")
                ).use { hasATable ->
                    if (hasATable.count > 0) {
                        //~ Copying data from the old table "a"
                        db.execSQL(
                            "INSERT INTO `historycall` (TIMESTAMP_START, call_end, number, missed," +
                                    "direction, recordPath, accountID, contactID, contactKey, callID) " +
                                    "SELECT TIMESTAMP_START,b,c,d,e,f,g,h,i,j FROM a;"
                        )
                        db.execSQL("DROP TABLE IF EXISTS a_TIMESTAMP_START_idx;")
                        db.execSQL("DROP TABLE a;")
                    }
                }
                db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type=? AND name=?;",
                    arrayOf("table", "e")
                ).use { hasETable ->
                    if (hasETable.count > 0) {
                        //~ Copying data from the old table "e"
                        db.execSQL(
                            "INSERT INTO historytext (id, TIMESTAMP, number, direction, accountID," +
                                    "contactID, contactKey, callID, message, read) " +
                                    "SELECT id,TIMESTAMP,c,d,e,f,g,h,i,j FROM e;"
                        )
                        //~ Remove old tables "a" and "e"
                        db.execSQL("DROP TABLE IF EXISTS e_TIMESTAMP_idx;")
                        db.execSQL("DROP TABLE IF EXISTS e_id_idx;")
                        db.execSQL("DROP TABLE e;")
                    }
                }
                db.setTransactionSuccessful()
                db.endTransaction()
                Log.d(TAG, "updateDatabaseFrom6: Migration from database version 6 to next, done.")
            } catch (exception: SQLiteException) {
                Log.e(
                    TAG,
                    "updateDatabaseFrom6: Migration from database version 6 to next, failed."
                )
                throw exception
            }
        }
    }

    @Throws(SQLiteException::class)
    private fun updateDatabaseFrom7(db: SQLiteDatabase?) {
        if (db != null && db.isOpen) {
            try {
                Log.d(
                    TAG,
                    "updateDatabaseFrom7: Will begin migration from database version 7 to next."
                )
                db.beginTransaction()
                db.execSQL("ALTER TABLE historytext ADD COLUMN state VARCHAR DEFAULT ''")
                db.setTransactionSuccessful()
                db.endTransaction()
                Log.d(TAG, "updateDatabaseFrom7: Migration from database version 7 to next, done.")
            } catch (exception: SQLiteException) {
                Log.e(
                    TAG,
                    "updateDatabaseFrom7: Migration from database version 7 to next, failed."
                )
                throw exception
            }
        }
    }

    @Throws(SQLException::class)
    private fun updateDatabaseFrom8(connectionSource: ConnectionSource) {
        try {
            TableUtils.createTable(connectionSource, DataTransfer::class.java)
            Log.d(TAG, "Migration from database version 8 to next, done.")
        } catch (e: SQLException) {
            Log.e(TAG, "Migration from database version 8 to next, failed.", e)
            throw e
        }
    }

    /**
     * This updates the database to version 10 which includes the switch to interaction and conversation tables.
     * It will delete previous tables.
     *
     * @param db the database to migrate
     * @throws SQLiteException
     */
    @Throws(SQLiteException::class)
    private fun updateDatabaseFrom9(db: SQLiteDatabase?) {
        if (db != null && db.isOpen) {
            try {
                Log.d(
                    TAG,
                    "updateDatabaseFrom9: Will begin migration from database version 9 to next for db: " + db.path
                )
                db.beginTransaction()

                // removing ring prefix from both call and text database

                // where clause improves performance (not required)
                db.execSQL(
                    """
    UPDATE historytext 
    SET number = replace( number, 'ring:', '' )
    WHERE number LIKE 'ring:%'
    """.trimIndent()
                )
                db.execSQL(
                    """
    UPDATE historycall 
    SET number = replace( number, 'ring:', '' )
    WHERE number LIKE 'ring:%'
    """.trimIndent()
                )

                // populating conversations table
                db.execSQL(
                    """INSERT INTO conversations (participant)
SELECT DISTINCT historytext.number
FROM   historytext 
       LEFT JOIN historycall 
	   ON historycall.number = historytext.number
UNION 
SELECT DISTINCT historycall.number
FROM   historycall
       LEFT JOIN historytext
	   ON historytext.number = historycall.number
UNION
SELECT DISTINCT historydata.peerId
FROM   historydata
       LEFT JOIN historytext
	   ON historytext.number = historydata.peerId"""
                )


                // DATA TRANSFER TABLE

                // Data transfer migration is done first as we maintain the same ID's as in the previous database

                // updating the statuses to the new schema
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_CREATED' WHERE dataTransferEventCode='CREATED'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_ERROR' WHERE dataTransferEventCode='UNSUPPORTED'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_AWAITING_PEER' WHERE dataTransferEventCode='WAIT_PEER_ACCEPTANCE'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_AWAITING_HOST' WHERE dataTransferEventCode='WAIT_HOST_ACCEPTANCE'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_ONGOING' WHERE dataTransferEventCode='ONGOING'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_FINISHED' WHERE dataTransferEventCode='FINISHED'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_UNJOINABLE_PEER' WHERE dataTransferEventCode='CLOSED_BY_HOST'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_UNJOINABLE_PEER' WHERE dataTransferEventCode='CLOSED_BY_PEER'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_ERROR' WHERE dataTransferEventCode='INVALID_PATHNAME'")
                db.execSQL("UPDATE historydata SET dataTransferEventCode='TRANSFER_UNJOINABLE_PEER' WHERE dataTransferEventCode='UNJOINABLE_PEER'")

                // migration
                db.execSQL(
                    """
    INSERT INTO interactions (id, author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)
    SELECT historydata.id, historydata.peerId, null, historydata.displayName, 1, historydata.dataTransferEventCode, historydata.TIMESTAMP, 'DATA_TRANSFER', '{}', conversations.id
    FROM historydata
    JOIN conversations ON conversations.participant = historydata.peerId
    WHERE isOutgoing = 0
    
    """.trimIndent()
                )
                db.execSQL(
                    """
    INSERT INTO interactions (id, author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)
    SELECT historydata.id, null, null, historydata.displayName, 1, historydata.dataTransferEventCode, historydata.TIMESTAMP, 'DATA_TRANSFER', '{}', conversations.id
    FROM historydata
    JOIN conversations ON conversations.participant = historydata.peerId
    WHERE isOutgoing = 1
    
    """.trimIndent()
                )


                // MESSAGE TABLE

                // updating status in text message table
                db.execSQL("UPDATE historytext SET state='SUCCESS' WHERE state='SENT'")
                db.execSQL("UPDATE historytext SET state='SUCCESS' WHERE state='READ'")
                db.execSQL("UPDATE historytext SET state='FAILURE' WHERE state='FAILURE'")
                db.execSQL("UPDATE historytext SET state='INVALID' WHERE state= null")

                // migration

                // divided into two similar functions, where author needs to be null in case of outgoing message
                db.execSQL(
                    """
    INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)
    SELECT null, historytext.id, historytext.message, historytext.read, historytext.state, historytext.TIMESTAMP, 'TEXT','{}', conversations.id
    FROM historytext
    JOIN conversations ON conversations.participant = historytext.number
    WHERE direction = 2
    
    """.trimIndent()
                )
                db.execSQL(
                    """
    INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)
    SELECT historytext.number, historytext.id, historytext.message, historytext.read, historytext.state, historytext.TIMESTAMP, 'TEXT','{}', conversations.id
    FROM historytext
    JOIN conversations ON conversations.participant = historytext.number
    WHERE direction = 1
    
    """.trimIndent()
                )


                // CALL TABLE

                // setting the timestamp end to the duration string before migration
                db.execSQL("UPDATE historycall SET call_end='{\"duration\":' || (historycall.call_end - historycall.TIMESTAMP_START) || '}' WHERE missed = 0")
                db.execSQL("UPDATE historycall SET call_end='{}' WHERE missed = 1")
                db.execSQL(
                    """
    INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)
    SELECT null, historycall.callID, null, 1, 'SUCCEEDED', historycall.TIMESTAMP_START, 'CALL', historycall.call_end, conversations.id
    FROM historycall
    JOIN conversations ON conversations.participant = historycall.number
    WHERE direction = 1
    
    """.trimIndent()
                )
                db.execSQL(
                    """
    INSERT INTO interactions (author ,daemon_id, body, is_read, status, timestamp, type, extra_data, conversation)
    SELECT historycall.number, historycall.callID, null, 1, 'SUCCEEDED', historycall.TIMESTAMP_START, 'CALL', historycall.call_end, conversations.id
    FROM historycall
    JOIN conversations ON conversations.participant = historycall.number
    WHERE direction = 0
    
    """.trimIndent()
                )

                // drop old tables
                db.execSQL("DROP TABLE historycall;")
                db.execSQL("DROP TABLE historytext;")
                db.execSQL("DROP TABLE historydata;")
                db.setTransactionSuccessful()
                db.endTransaction()
                Log.d(TAG, "updateDatabaseFrom9: Migration from database version 9 to next, done.")
            } catch (exception: SQLiteException) {
                Log.e(
                    TAG,
                    "updateDatabaseFrom9: Migration from database version 9 to next, failed.",
                    exception
                )
            }
        }
    }

    /**
     * Removes all the data from the database, ie all the tables.
     *
     * @param db the SQLiteDatabase to work with
     */
    private fun clearDatabase(db: SQLiteDatabase?) {
        if (db != null && db.isOpen) {
            Log.d(TAG, "clearDatabase: Will clear database.")
            val tableNames = ArrayList<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                tableNames.ensureCapacity(c.count)
                while (c.moveToNext()) tableNames.add(c.getString(0))
            }
            try {
                db.beginTransaction()
                for (tableName in tableNames) {
                    db.execSQL("DROP TABLE $tableName;")
                }
                db.setTransactionSuccessful()
                db.endTransaction()
                Log.d(TAG, "clearDatabase: Database is cleared")
            } catch (exc: SQLiteException) {
                exc.printStackTrace()
            }
        }
    }

    companion object {
        private val TAG = DatabaseHelper::class.java.simpleName

        // any time you make changes to your database objects, you may have to increase the database version
        private const val DATABASE_VERSION = 10
    }

    init {
        Log.d(TAG, "Helper initialized for $dbDirectory")
    }
}