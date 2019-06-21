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
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.io.File;
import java.sql.SQLException;
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



    @Override
    protected void migrateDatabase() {

    }

    /**
     * Checks if the legacy database exists in the file path for migration purposes.
     * @return true if history.db exists
     */
    private Boolean checkForLegacyDb() {
        return mContext.getDatabasePath(DATABASE_NAME).exists();
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
        if (checkForLegacyDb())
            return initHelper(DATABASE_NAME);

        if (!databaseHelpers.isEmpty() && databaseHelpers.containsKey(accountId))
            return databaseHelpers.get(accountId);
        else
            return initHelper(accountId);
    }


}
