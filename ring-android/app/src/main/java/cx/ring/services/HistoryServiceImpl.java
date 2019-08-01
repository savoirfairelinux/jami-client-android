/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import cx.ring.history.DatabaseHelper;
import cx.ring.model.ConversationHistory;
import cx.ring.model.Interaction;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

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
    private static boolean migrationInitialized = false;
    public Subject<Boolean> accountsMigrationComplete = BehaviorSubject.create();

    public HistoryServiceImpl() {
    }

    public Observable<Boolean> isAccountMigrationComplete() {
        return accountsMigrationComplete;
    }

    @Override
    protected ConnectionSource getConnectionSource(String dbName) {
        return getHelper(dbName).getConnectionSource();
    }

    @Override
    protected Dao<Interaction, Integer> getInteractionDataDao(String dbName) {
        try {
            return getHelper(dbName).getInteractionDataDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a interactionDataDao");
            return null;
        }
    }

    @Override
    protected Dao<ConversationHistory, Integer> getConversationDataDao(String dbName) {
        try {
            return getHelper(dbName).getConversationDataDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a conversationDataDao");
            return null;
        }
    }

    /**
     * Creates an instance of our database's helper.
     * Stores it in a hash map for easy retrieval in the future.
     *
     * @param accountId represents the file where the database is stored
     * @return the database helper
     */
    private DatabaseHelper initHelper(String accountId) {
        File dbPath = new File(mContext.getFilesDir(), accountId);
        File db = new File(dbPath, DATABASE_NAME);
        DatabaseHelper helper = new DatabaseHelper(mContext, db.getAbsolutePath());
        databaseHelpers.put(accountId, helper);
        return helper;
    }

    /**
     * Retrieve helper for our DB. Creates a new instance if it does not exist through the initHelper method.
     *
     * @param accountId represents the file where the database is stored
     * @return the database helper
     * @see #initHelper(String) initHelper
     */
    @SuppressWarnings("JavadocReference")
    @Override
    protected DatabaseHelper getHelper(String accountId) {
        if (checkForLegacyDb()) {
            return initLegacyDb();
        }


        if (!databaseHelpers.isEmpty() && databaseHelpers.containsKey(accountId))
            return databaseHelpers.get(accountId);
        else
            return initHelper(accountId);
    }

    // DATABASE MIGRATION

    /**
     * Checks if the legacy database exists in the file path for migration purposes.
     *
     * @return true if history.db exists in the database folder
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
        if (databaseHelpers.containsKey(LEGACY_DATABASE_KEY)) {
            return databaseHelpers.get(LEGACY_DATABASE_KEY);
        }

        DatabaseHelper helper = new DatabaseHelper(mContext, DATABASE_NAME);
        databaseHelpers.put(LEGACY_DATABASE_KEY, helper);
        return helper;
    }

    /**
     * Deletes a database and removes its helper from the hashmap
     *
     * @param dbName the name of the database you want to delete
     */
    private void deleteLegacyDatabase(String dbName) {
        try {
            getConnectionSource(dbName).close();
            mContext.deleteDatabase(dbName);
            databaseHelpers.remove(LEGACY_DATABASE_KEY);
        } catch (IOException e) {
            Log.e(TAG, "Error deleting database", e);
        }
    }

    /**
     * Deletes the user's account file and all its children
     *
     * @param accountId the file name
     * @see #deleteFolder(File) deleteFolder
     */
    @Override
    protected void deleteAccountHistory(String accountId) {
        File accountDir = new File(mContext.getFilesDir(), accountId);
        if (accountDir.exists())
            deleteFolder(accountDir);
    }

    /**
     * Deletes a file and all its children recursively
     *
     * @param file the file to delete
     */
    private void deleteFolder(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children)
                    deleteFolder(child);
            }
        }
        file.delete();
    }

    /**
     * Migrates to the new per account database system. Should only be used once.
     *
     * @param accounts The list of accounts to migrate
     */
    @Override
    protected void migrateDatabase(List<String> accounts) {

        if (!checkForLegacyDb()) {
            accountsMigrationComplete.onNext(false);
            return;
        }

        if (migrationInitialized)
            return;

        migrationInitialized = true;

        Log.i(TAG, "Initializing database migration...");

        SQLiteDatabase db = initLegacyDb().getReadableDatabase();

        try {

            if (accounts == null) {
                Log.i(TAG, "No existing accounts found in directory, aborting migration...");
                return;
            }

            // create new database for each account
            for (String newDb : accounts) {

                File dbPath = new File(mContext.getFilesDir(), newDb);
                File dbLocation = new File(dbPath, DATABASE_NAME);

                SQLiteDatabase newDatabase = mContext.openOrCreateDatabase(dbLocation.getAbsolutePath(), Context.MODE_PRIVATE, null);
                newDatabase.setVersion(9);

                String legacyDbPath = mContext.getDatabasePath(DATABASE_NAME).getAbsolutePath();

                String[] dbName = {newDb};

                // attach new database to begin migration
                newDatabase.execSQL("ATTACH DATABASE '" + legacyDbPath + "' AS tempDb");

                newDatabase.beginTransaction();

                newDatabase.execSQL("CREATE TABLE `historycall` (`accountID` VARCHAR , `callID` VARCHAR , `call_end` BIGINT , `TIMESTAMP_START` BIGINT , `contactID` BIGINT , `contactKey` VARCHAR , `direction` INTEGER , `missed` SMALLINT , `number` VARCHAR , `recordPath` VARCHAR )");
                newDatabase.execSQL("CREATE TABLE `historydata` (`accountId` VARCHAR , `displayName` VARCHAR , `dataTransferEventCode` VARCHAR , `id` INTEGER PRIMARY KEY AUTOINCREMENT , `isOutgoing` SMALLINT , `peerId` VARCHAR , `TIMESTAMP` BIGINT , `totalSize` BIGINT )");
                newDatabase.execSQL("CREATE TABLE `historytext` (`accountID` VARCHAR , `callID` VARCHAR , `contactID` BIGINT , `contactKey` VARCHAR , `direction` INTEGER , `id` BIGINT , `message` VARCHAR , `number` VARCHAR , `read` SMALLINT , `state` VARCHAR , `TIMESTAMP` BIGINT , PRIMARY KEY (`id`) )");
                newDatabase.execSQL("CREATE TABLE `conversations` (`extra_data` VARCHAR , `id` INTEGER PRIMARY KEY AUTOINCREMENT , `participant` VARCHAR )");
                newDatabase.execSQL("CREATE TABLE `interactions` (`is_read` INTEGER , `author` VARCHAR , `body` VARCHAR , `conversation` INTEGER , `daemon_id` BIGINT , `extra_data` VARCHAR , `id` INTEGER PRIMARY KEY AUTOINCREMENT , `status` VARCHAR , `timestamp` BIGINT , `type` VARCHAR )");

                // migrate any data where account id matches
                newDatabase.execSQL("INSERT INTO historycall SELECT * FROM tempDb.historycall WHERE accountId=?", dbName);
                newDatabase.execSQL("INSERT INTO historydata SELECT * FROM tempDb.historydata WHERE accountID=?", dbName);
                newDatabase.execSQL("INSERT INTO historytext SELECT * FROM tempDb.historytext WHERE accountID=?", dbName);

                newDatabase.setTransactionSuccessful();
                newDatabase.endTransaction();

                newDatabase.execSQL("DETACH tempDb");

                newDatabase.close();
            }

            for(String newDb : accounts) {
                initHelper(newDb);
            }

            db.close();
            deleteLegacyDatabase(DATABASE_NAME);
            accountsMigrationComplete.onNext(true);
            Log.i(TAG, "Migration complete. Each account now has its own database");

        } catch (SQLiteException e) {
            db.close();
            accountsMigrationComplete.onNext(false);
            migrationInitialized = false;
            Log.e(TAG, "Error migrating database.", e);
        } catch (NullPointerException e) {
            db.close();
            accountsMigrationComplete.onNext(false);
            migrationInitialized = false;
            Log.e(TAG, "An unexpected error occurred. The migration will run again when the helper is called again", e);
        }
    }

}
