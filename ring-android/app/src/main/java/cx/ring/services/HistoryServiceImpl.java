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
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.io.IOException;
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
     * Deletes a specified database.
     *
     * @param dbName the database name which is the user's account ID
     */
    protected void deleteDatabase(String dbName) {
        try {
            getConnectionSource(dbName).close();
            databaseHelpers.remove(dbName);
            mContext.deleteDatabase(dbName + ".db");
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "Error deleting database", e);
        }
    }

    /**
     * Creates an instance of our database's helper.
     * @param dbName the database name which is the user's account ID
     * @return the database helper
     */
    protected DatabaseHelper initHelper(String dbName) {
        DatabaseHelper helper = new DatabaseHelper(mContext, dbName + ".db");
        databaseHelpers.put(dbName, helper);
        return helper;
    }

    /**
     * Retrieve helper for our DB. Creates a new instance if it does not exist.
     * Stores the result in a hash map for easy retrieval.
     *
     * @param dbName the database name which is the user's account ID
     * @return the database helper
     */
    protected DatabaseHelper getHelper(String dbName) {
        if (!databaseHelpers.isEmpty() && databaseHelpers.containsKey(dbName)) {
            return databaseHelpers.get(dbName);
        } else {
            return initHelper(dbName);
        }
    }


}
