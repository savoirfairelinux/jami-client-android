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

package cx.ring.history;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;

public class HistoryManager {
    private static final String TAG = HistoryManager.class.getSimpleName();

    private Context mContext;
    private DatabaseHelper historyDBHelper = null;

    public HistoryManager(Context context) {
        mContext = context;
        getHelper();
    }

    public boolean insertNewEntry(Conference toInsert) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean val = sharedPref.getBoolean(mContext.getString(R.string.pref_systemDialer_key), false);

        for (SipCall call : toInsert.getParticipants()) {
            call.setTimestampEnd(System.currentTimeMillis());
            if (val) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(CallLog.Calls.NUMBER, call.getNumber());
                    values.put(CallLog.Calls.DATE, call.getTimestampStart());
                    values.put(CallLog.Calls.DURATION, call.getDuration());
                    values.put(CallLog.Calls.TYPE, call.isMissed() ? CallLog.Calls.MISSED_TYPE : (call.isIncoming() ? CallLog.Calls.INCOMING_TYPE : CallLog.Calls.OUTGOING_TYPE));
                    values.put(CallLog.Calls.NEW, 1);
                    values.put(CallLog.Calls.CACHED_NAME, call.getContact().getDisplayName());
                    values.put(CallLog.Calls.CACHED_NUMBER_TYPE, 0);
                    values.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");
                    mContext.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
                } catch (SecurityException e) {
                    Log.e(TAG, "Can't insert call in call log: ", e);
                }
            }

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
            Log.d("HistoryManager", "HistoryDao().create() id:" + txt.id + " acc:" + txt.getAccountID() + " num:" + txt.getNumber() + " date:" + txt.getDate().toString() + " msg:" + txt.getMessage());
            getHelper().getTextHistoryDao().create(txt);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean insertNewTextMessage(TextMessage txt) {
        HistoryText htxt = new HistoryText(txt);
        if (!insertNewTextMessage(htxt))
            return false;
        txt.setID(htxt.id);
        return true;
    }

    public boolean updateTextMessage(HistoryText txt) {
        try {
            Log.w("HistoryManager", "HistoryDao().update() id:" + txt.id + " acc:" + txt.getAccountID() + " num:" + txt.getNumber() + " date:" + txt.getDate().toString() + " msg:" + txt.getMessage());
            getHelper().getTextHistoryDao().update(txt);
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
    public boolean insertNewEntry(SipCall toInsert) {
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
        qb.orderBy(HistoryCall.COLUMN_TIMESTAMP_START_NAME, true);
        return getHelper().getHistoryDao().query(qb.prepare());
    }

    public List<HistoryText> getAllTextMessages() throws SQLException {
        QueryBuilder<HistoryText, Integer> qb = getHelper().getTextHistoryDao().queryBuilder();
        qb.orderBy(HistoryText.COLUMN_TIMESTAMP_NAME, true);
        return getHelper().getTextHistoryDao().query(qb.prepare());
    }

    /**
     * Removes all the text messages and call histories from the database.
     *
     * @param conversation The conversation containing the elements to delete.
     */
    public void clearHistoryForConversation(Conversation conversation) {
        if (conversation == null) {
            Log.d(TAG, "clearHistoryForConversation: conversation is null");
            return;
        }
        try {
            Map<String, HistoryEntry> history = conversation.getRawHistory();
            for (Map.Entry<String, HistoryEntry> entry : history.entrySet()) {
                //~ Deleting messages
                ArrayList<Long> textMessagesIds = new ArrayList<>(entry.getValue().getTextMessages().size());
                for (TextMessage textMessage : entry.getValue().getTextMessages().values()) {
                    textMessagesIds.add(textMessage.getId());
                }
                DeleteBuilder<HistoryText, Integer> deleteTextHistoryBuilder = getHelper()
                        .getTextHistoryDao()
                        .deleteBuilder();
                deleteTextHistoryBuilder.where().in(HistoryText.COLUMN_ID_NAME, textMessagesIds);
                deleteTextHistoryBuilder.delete();
                //~ Deleting calls
                ArrayList<String> callIds = new ArrayList<>(entry.getValue().getCalls().size());
                for (HistoryCall historyCall : entry.getValue().getCalls().values()) {
                    callIds.add(historyCall.getCallId().toString());
                }
                DeleteBuilder<HistoryCall, Integer> deleteCallsHistoryBuilder = getHelper()
                        .getHistoryDao()
                        .deleteBuilder();
                deleteCallsHistoryBuilder.where().in(HistoryCall.COLUMN_CALL_ID_NAME, callIds);
                deleteCallsHistoryBuilder.delete();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
