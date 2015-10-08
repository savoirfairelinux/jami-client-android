/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.history;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.TableUtils;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;

import java.sql.SQLException;
import java.util.List;

public class HistoryManager {

    private Context mContext;
    private DatabaseHelper historyDBHelper = null;

    public HistoryManager(Context context) {
        mContext = context;
        getHelper();
    }

    public boolean insertNewEntry(Conference toInsert){
        for (SipCall call : toInsert.getParticipants()) {
            call.setTimestampEnd(System.currentTimeMillis());
            HistoryCall persistent = new HistoryCall(call);
            try {
                Log.w("HistoryManager", "HistoryDao().create() " + persistent.getNumber() + " " + persistent.getStartDate().toString() + " " + persistent.getEndDate());
                getHelper().getHistoryDao().create(persistent);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public boolean insertNewTextMessage(HistoryText txt) {
        try {
            Log.w("HistoryManager", "HistoryDao().create() acc:" + txt.getAccountID() + " num:" + txt.getNumber() + " date:" + txt.getDate().toString() + " msg:" + txt.getMessage());
            getHelper().getTextHistoryDao().create(txt);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
    * Necessary when user hang up a call in a Conference
    * The call creates an HistoryCall, but the conference still goes on
    */
    public boolean insertNewEntry(SipCall toInsert){
        return true;
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

    public List<HistoryCall> getAll() throws SQLException {
        QueryBuilder<HistoryCall, Integer> qb = getHelper().getHistoryDao().queryBuilder();
        qb.orderBy("TIMESTAMP_START", true);
        return getHelper().getHistoryDao().query(qb.prepare());
    }

    public List<HistoryText> getAllTextMessages() throws SQLException {
        QueryBuilder<HistoryText, Integer> qb = getHelper().getTextHistoryDao().queryBuilder();
        qb.orderBy("TIMESTAMP", true);
        return getHelper().getTextHistoryDao().query(qb.prepare());
    }

    public boolean clearDB() {
        try {
            TableUtils.clearTable(getHelper().getConnectionSource(), HistoryCall.class);
            TableUtils.clearTable(getHelper().getConnectionSource(), HistoryText.class);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
