/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.services

import android.content.Context
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import cx.ring.fragments.ConversationFragment
import cx.ring.history.DatabaseHelper
import net.jami.model.ConversationHistory
import net.jami.model.interaction.Interaction
import net.jami.model.Uri
import net.jami.services.HistoryService
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Implements the necessary Android related methods for the [HistoryService]
 */
class HistoryServiceImpl(private val mContext: Context) : HistoryService() {
    private val databaseHelpers = ConcurrentHashMap<String, DatabaseHelper>()
    override fun getConnectionSource(dbName: String): ConnectionSource {
        return getHelper(dbName).connectionSource
    }

    override fun getInteractionDataDao(dbName: String): Dao<Interaction, Int> {
        return getHelper(dbName).interactionDataDao
    }

    override fun getConversationDataDao(dbName: String): Dao<ConversationHistory, Int> {
        return getHelper(dbName).conversationDataDao
    }

    /**
     * Creates an instance of our database's helper.
     * Stores it in a hash map for easy retrieval in the future.
     *
     * @param accountId represents the file where the database is stored
     * @return the database helper
     */
    private fun initHelper(accountId: String): DatabaseHelper {
        val db = File(File(mContext.filesDir, accountId), Companion.DATABASE_NAME)
        val helper = DatabaseHelper(mContext, db.absolutePath)
        databaseHelpers[accountId] = helper
        return helper
    }

    /**
     * Retrieve helper for our DB. Creates a new instance if it does not exist through the initHelper method.
     *
     * @param accountId represents the file where the database is stored
     * @return the database helper
     * @see .initHelper
     */
    override fun getHelper(accountId: String): DatabaseHelper {
        val helper = databaseHelpers[accountId]
        return helper ?: initHelper(accountId)
    }

    /**
     * Deletes the user's account file and all its children
     *
     * @param accountId the file name
     * @see .deleteFolder
     */
    override fun deleteAccountHistory(accountId: String) {
        val accountDir = File(mContext.filesDir, accountId)
        if (accountDir.exists()) deleteFolder(accountDir)
    }

    override fun setMessageNotified(accountId: String, conversationUri: Uri, lastId: String) {
        val preferences = mContext.getSharedPreferences(accountId + "_" + conversationUri.uri, Context.MODE_PRIVATE)
        preferences.edit()
            .putString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_LAST_READ, lastId).apply()
    }

    override fun getLastMessageNotified(accountId: String, conversationUri: Uri): String? {
        val preferences = mContext.getSharedPreferences(accountId + "_" + conversationUri.uri, Context.MODE_PRIVATE)
        return preferences.getString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_LAST_READ, null)
    }

    /**
     * Deletes a file and all its children recursively
     *
     * @param file the file to delete
     */
    private fun deleteFolder(file: File) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) deleteFolder(child)
            }
        }
        file.delete()
    }

    companion object {
        private const val DATABASE_NAME = "history.db"
    }
}