package cx.ring.dependencyinjection

import android.app.Application
import android.arch.persistence.room.Room
import cx.ring.database.AppDatabase
import cx.ring.database.AppDatabase.Companion.MIGRATION_8_9
import cx.ring.database.dao.ConversationDao
import cx.ring.database.dao.HistoryTextDao
import cx.ring.database.dao.InteractionDao
import cx.ring.database.dao.ProfileDao
import cx.ring.services.RoomService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


/**
 * Created by hdesousa on 09/11/17.
 */
@Module
class RoomModule(mApplication: Application) {

    private val appDatabase: AppDatabase = Room.databaseBuilder(mApplication, AppDatabase::class.java, "history.db")
            .addMigrations(MIGRATION_8_9)
            .build()

    @Singleton
    @Provides
    internal fun providesRoomDatabase(): AppDatabase {
        return appDatabase
    }

    @Singleton
    @Provides
    internal fun providesProfileDao(appDatabase: AppDatabase): ProfileDao {
        return appDatabase.profileDao()
    }

    @Singleton
    @Provides
    internal fun providesInteractionDao(appDatabase: AppDatabase): InteractionDao {
        return appDatabase.interactionDao()
    }

    @Singleton
    @Provides
    internal fun providesConversationDao(appDatabase: AppDatabase): ConversationDao {
        return appDatabase.conversationDao()
    }

    @Singleton
    @Provides
    internal fun providesHistoryTextDao(appDatabase: AppDatabase): HistoryTextDao {
        return appDatabase.historyTextDao()
    }

    @Singleton
    @Provides
    internal fun providesRoomService(profileDao: ProfileDao,
                                     conversationDao: ConversationDao,
                                     interactionDao: InteractionDao,
                                     historyTextDao: HistoryTextDao): RoomService {
        return RoomService(profileDao, conversationDao, interactionDao, historyTextDao)
    }

}