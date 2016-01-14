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
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "history.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 6;

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
            //TableUtils.dropTable(connectionSource, HistoryCall.class, true);
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, HistoryCall.class);
            TableUtils.createTable(connectionSource, HistoryText.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onUpgrade " + oldVersion + " -> " + newVersion);
            if (oldVersion == 4 && newVersion == 5) {
                getTextHistoryDao().executeRaw("ALTER TABLE `historytext` ADD COLUMN read INTEGER;");
            } else {
                //TableUtils.
                TableUtils.dropTable(connectionSource, HistoryCall.class, true);
                TableUtils.dropTable(connectionSource, HistoryText.class, true);
                // after we drop the old databases, we create the new ones
                onCreate(db, connectionSource);
            }
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't update databases", e);
            throw new RuntimeException(e);
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
}
