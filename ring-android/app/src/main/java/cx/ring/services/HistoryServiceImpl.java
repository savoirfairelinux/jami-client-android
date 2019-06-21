/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import cx.ring.history.DatabaseHelper;
import cx.ring.model.DataTransfer;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryText;

/**
 * Implements the necessary Android related methods for the {@link HistoryService}
 */
public class HistoryServiceImpl extends HistoryService {
    private static final String TAG = HistoryServiceImpl.class.getSimpleName();

    @Inject
    protected Context mContext;

    private ConcurrentHashMap<String, DatabaseHelper> databaseHelpers = new ConcurrentHashMap<>();
    private final static String DATABASE_NAME = "history.db";
    private final static String LEGACY_DATABASE_KEY = "legacy";
    private static boolean migrationInProcess = false;

    public HistoryServiceImpl() {
    }

    @Override
    protected ConnectionSource getConnectionSource(String dbName) {
        return getHelper(dbName).getConnectionSource();
    }

    @Override
    protected Dao<HistoryCall, Integer> getCallHistoryDao(String dbName) {
        try {
            return getHelper(dbName).getHistoryDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a CallHistoryDao");
            return null;
        }
    }

    @Override
    protected Dao<HistoryText, Long> getTextHistoryDao(String dbName) {
        try {
            return getHelper(dbName).getTextHistoryDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a TextHistoryDao");
            return null;
        }
    }

    @Override
    protected Dao<DataTransfer, Long> getDataHistoryDao(String dbName) {
        try {
            return getHelper(dbName).getDataHistoryDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a DataHistoryDao");
            return null;
        }
    }

    /**
     * Creates an instance of our database's helper.
     *
     * @param accountId represents the file where the database is stored
     * @return the database helper
     */
    @Override
    protected DatabaseHelper initHelper(String accountId) {
        String dbDirectory = mContext.getFilesDir().getAbsolutePath() + File.separator + accountId + File.separator + DATABASE_NAME;
        DatabaseHelper helper = new DatabaseHelper(mContext, dbDirectory);
        databaseHelpers.put(accountId, helper);
        return helper;
    }

    /**
     * Retrieve helper for our DB. Creates a new instance if it does not exist.
     * Stores the result in a hash map for easy retrieval.
     *
     * @param accountId represents the file where the database is stored
     * @return the database helper
     */
    @Override
    protected DatabaseHelper getHelper(String accountId) {
        if (checkForLegacyDb() && !migrationInProcess)
            return initLegacyDb();

        if (!databaseHelpers.isEmpty() && databaseHelpers.containsKey(accountId))
            return databaseHelpers.get(accountId);
        else
            return initHelper(accountId);
    }

    // DATABASE MIGRATION

    /**
     * Checks if the legacy database exists in the file path for migration purposes.
     *
     * @return true if history.db exists
     */
    private Boolean checkForLegacyDb() {
        return mContext.getDatabasePath(DATABASE_NAME).exists();
    }

    /**
     * Initializes the database prior to version 10
     *
     * @return the database helper
     */
    private DatabaseHelper initLegacyDb() {
        if(databaseHelpers.containsKey(LEGACY_DATABASE_KEY))
            return databaseHelpers.get(LEGACY_DATABASE_KEY);

        DatabaseHelper helper = new DatabaseHelper(mContext, DATABASE_NAME);
        databaseHelpers.put(LEGACY_DATABASE_KEY, helper);
        return helper;
    }

    /**
     * Deletes a database and removes its helper from the hashmap
     * @param dbName the name of the database you want to delete
     */
    private void deleteDatabase(String dbName, boolean legacy) {
        try {
            getConnectionSource(dbName).close();
            mContext.deleteDatabase(dbName);
            if(legacy)
                databaseHelpers.remove(LEGACY_DATABASE_KEY);
            else
                databaseHelpers.remove(dbName);
        }
        catch(IOException e) {
            Log.e(TAG, "Error deleting database", e);
        }
    }

    @Override
    /**
     * Migrates to the new per account database system. Should only be used once.
     */
    protected void migrateDatabase() {

        migrationInProcess = true;

        try {
            SQLiteDatabase db = databaseHelpers.get(LEGACY_DATABASE_KEY).getReadableDatabase();
            // Cursor cursor = db.rawQuery("select * from datatransfer", null);


            // if (cursor.moveToFirst()) {
            //  while (!cursor.isAfterLast()) {
            //  String currentId = cursor.getString(cursor.getColumnIndex("accountId"));
            // String newDb = currentId;

            List<String> accounts = getAccounts();

            if (accounts == null) {
                Log.i(TAG, "No existing accounts found in directory, aborting migration...");
                return;
            }

            for (String newDb : accounts) {

                DatabaseHelper helper = getHelper(newDb);

                SQLiteDatabase newDatabase = helper.getWritableDatabase();

                String legacyDbPath = mContext.getDatabasePath(DATABASE_NAME).getAbsolutePath();

                newDatabase.execSQL("ATTACH DATABASE '" + legacyDbPath + "' AS tempDb");


                // while (currentId.equals(newDb)) {

                newDatabase.execSQL("INSERT INTO historydata SELECT * FROM tempDb.datatransfers WHERE accountId = " + newDb);
                newDatabase.execSQL("INSERT INTO historycall SELECT * FROM tempDb.datatransfers WHERE accountID = " + newDb);
                newDatabase.execSQL("INSERT INTO historytext SELECT * FROM tempDb.datatransfers WHERE accountID = " + newDb);

                newDatabase.execSQL("DETACH tempDb");

                newDatabase.close();

                migrationInProcess = false;

                deleteDatabase(DATABASE_NAME, true);

            }

            //   cursor.moveToNext();
            //   currentId = cursor.getString(cursor.getColumnIndex("accountId"));

            //}

            // cursor.moveToNext();
            // }
            // }

            //  cursor.close();
            db.close();


        } catch (
                SQLiteException e) {
            migrationInProcess = false;
            Log.e(TAG, "Error migrating database", e);
        }

    }

    /**
     * Retrieves all accountIds stored locally by checking the file dir where daemon generated files are stored.
     * @return a list of existing account ids or null if empty
     */
    private List<String> getAccounts() {
        File fileDir = mContext.getFilesDir();
        File[] files = fileDir.listFiles();
        List<String> accountsList = new ArrayList<>();

        if (files == null)
            return null;

        for (File file : files) {
            File configFile = new File(file.getAbsolutePath() + File.separator + "config.yml");
            if (configFile.exists())
                accountsList.add(file.getName());
        }

        return accountsList;
    }

}
