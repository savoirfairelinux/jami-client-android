/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import android.content.SharedPreferences;
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

import cx.ring.R;
import cx.ring.history.DatabaseHelper;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationHistory;
import cx.ring.model.Interaction;
import cx.ring.model.Uri;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static cx.ring.fragments.ConversationFragment.KEY_PREFERENCE_CONVERSATION_LAST_READ;

/**
 * Implements the necessary Android related methods for the {@link HistoryService}
 */
public class HistoryServiceImpl extends HistoryService {
    private static final String TAG = HistoryServiceImpl.class.getSimpleName();
    private final static String DATABASE_NAME = "history.db";
    private final static String LEGACY_DATABASE_KEY = "legacy";

    private final ConcurrentHashMap<String, DatabaseHelper> databaseHelpers = new ConcurrentHashMap<>();

    @Inject
    protected Context mContext;

    public HistoryServiceImpl() {
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
            Log.e(TAG, "Unable to get a interactionDataDao");
            return null;
        }
    }

    @Override
    protected Dao<ConversationHistory, Integer> getConversationDataDao(String dbName) {
        try {
            return getHelper(dbName).getConversationDataDao();
        } catch (SQLException e) {
            Log.e(TAG, "Unable to get a conversationDataDao");
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
        File db = new File(new File(mContext.getFilesDir(), accountId), DATABASE_NAME);
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
        DatabaseHelper helper = databaseHelpers.get(accountId);
        return helper == null ? initHelper(accountId) : helper;
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

    @Override
    public void setMessageRead(String accountId, Uri conversationUri, String lastId) {
        SharedPreferences preferences = mContext.getSharedPreferences(accountId + "_" + conversationUri.getUri(), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_PREFERENCE_CONVERSATION_LAST_READ, lastId).apply();
    }

    @Override
    public String getLastMessageRead(String accountId, Uri conversationUri) {
        SharedPreferences preferences = mContext.getSharedPreferences(accountId + "_" + conversationUri.getUri(), Context.MODE_PRIVATE);
        return preferences.getString(KEY_PREFERENCE_CONVERSATION_LAST_READ, null);
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

}
