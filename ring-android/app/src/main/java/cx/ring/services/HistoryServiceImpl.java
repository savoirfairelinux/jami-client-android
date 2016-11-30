/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

import javax.inject.Inject;

import cx.ring.history.DatabaseHelper;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryText;

/**
 * Implements the necessary Android related methods for the {@link HistoryService}
 */
public class HistoryServiceImpl extends HistoryService {

    private static final String TAG = HistoryServiceImpl.class.getSimpleName();

    @Inject
    Context mContext;

    private DatabaseHelper historyDBHelper = null;

    public HistoryServiceImpl() {
    }

    @Override
    protected ConnectionSource getConnectionSource() {
        return getHelper().getConnectionSource();
    }

    @Override
    protected Dao<HistoryCall, Integer> getCallHistoryDao() {
        try {
            return getHelper().getHistoryDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a CallHistoryDao");
            return null;
        }
    }

    @Override
    protected Dao<HistoryText, Integer> getTextHistoryDao() {
        try {
            return getHelper().getTextHistoryDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a TextHistoryDao");
            return null;
        }
    }

    /**
     * Init Helper for our DB
     */
    public void initHelper() {
        if (historyDBHelper == null) {
            historyDBHelper = OpenHelperManager.getHelper(mContext, DatabaseHelper.class);
        }
    }

    /**
     * Retrieve helper for our DB
     */
    private DatabaseHelper getHelper() {
        if (historyDBHelper == null) {
            historyDBHelper = OpenHelperManager.getHelper(mContext, DatabaseHelper.class);
        }
        return historyDBHelper;
    }
}
