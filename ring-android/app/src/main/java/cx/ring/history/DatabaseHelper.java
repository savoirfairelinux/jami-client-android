/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

/**
 * Database History Version
 * 7 : changing columns names. See https://gerrit-ring.savoirfairelinux.com/#/c/4297
 */

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "history.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 7;

    private Dao<HistoryCall, Integer> historyDao = null;
    private Dao<HistoryText, Integer> historyTextDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.d(TAG, "onCreate");
            TableUtils.createTable(connectionSource, HistoryCall.class);
            TableUtils.createTable(connectionSource, HistoryText.class);
        } catch (SQLException e) {
            Log.e(TAG, "Can't create database", e);
            throw new RuntimeException(e);
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
            updateDatabase(oldVersion, db);
        } catch (SQLiteException exc) {
            exc.printStackTrace();
            clearDatabase(db);
            onCreate(db, connectionSource);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<HistoryCall, Integer> getHistoryDao() throws SQLException {
        if (historyDao == null) {
            historyDao = getDao(HistoryCall.class);
        }
        return historyDao;
    }

    public Dao<HistoryText, Integer> getTextHistoryDao() throws SQLException {
        if (historyTextDao == null) {
            historyTextDao = getDao(HistoryText.class);
        }
        return historyTextDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        historyDao = null;
        historyTextDao = null;
    }

    /**
     * Main method to update the database from an old version to the last
     *
     * @param fromDatabaseVersion the old version of the database
     * @param db                  the SQLiteDatabase to work with
     * @throws SQLiteException
     */
    private void updateDatabase(int fromDatabaseVersion, SQLiteDatabase db) throws SQLiteException {
        try {
            while (fromDatabaseVersion < DATABASE_VERSION) {
                switch (fromDatabaseVersion) {
                    case 6:
                        updateDatabaseFrom6(db);
                        break;
                }
                fromDatabaseVersion++;
            }
            Log.d(TAG, "Database has been updated to the last version.");
        } catch (SQLiteException exc) {
            Log.e(TAG, "Database has failed to update to the last version.");
            throw exc;
        }
    }

    /**
     * Executes the migration from the database version 6 to the next
     *
     * @param db the SQLiteDatabase to work with
     * @throws SQLiteException
     */
    private void updateDatabaseFrom6(SQLiteDatabase db) throws SQLiteException {
        if (db != null && db.isOpen()) {
            try {
                Log.d(TAG, "Will begin migration from database version 6 to next.");
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
                Log.d(TAG, "Migration from database version 6 to next, done.");
            } catch (SQLiteException exception) {
                Log.e(TAG, "Migration from database version 6 to next, failed.");
                throw exception;
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
            Log.d(TAG, "Will clear database.");
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
                Log.d(TAG, "Database is cleared");
            } catch (SQLiteException exc) {
                exc.printStackTrace();
            }
        }
    }
}
