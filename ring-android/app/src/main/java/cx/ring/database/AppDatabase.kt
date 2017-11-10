package cx.ring.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import cx.ring.database.dao.ConversationDao
import cx.ring.database.dao.HistoryTextDao
import cx.ring.database.dao.InteractionDao
import cx.ring.database.dao.ProfileDao
import cx.ring.database.models.ConversationModel
import cx.ring.database.models.HistoryTextModel
import cx.ring.database.models.InteractionModel
import cx.ring.database.models.ProfileModel


/**
 * Created by hdesousa on 09/11/17.
 */
@Database(entities = arrayOf(ProfileModel::class, InteractionModel::class, ConversationModel::class, HistoryTextModel::class), version = 9)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @JvmField
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.

                val cursor = database.query("SELECT * FROM historytext")
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            val message = cursor.getString(cursor.getColumnIndex("message"))
                            val account = cursor.getString(cursor.getColumnIndex("accountID"))

                            // TODO FINISH MIGRATION

                        } while (cursor.moveToNext())
                    }
                }
            }
        }
    }

    abstract fun profileDao(): ProfileDao

    abstract fun interactionDao(): InteractionDao

    abstract fun conversationDao(): ConversationDao

    abstract fun historyTextDao(): HistoryTextDao


}