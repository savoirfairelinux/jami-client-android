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

package org.sflphone.history;

import android.content.Context;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import org.sflphone.model.Conference;
import org.sflphone.model.SipCall;

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
            call.setTimestampEnd_(System.currentTimeMillis() * 1000);
            HistoryCall persistent = new HistoryCall(call);
            try {
                getHelper().getHistoryDao().create(persistent);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

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
        return getHelper().getHistoryDao().queryForAll();
    }
}
