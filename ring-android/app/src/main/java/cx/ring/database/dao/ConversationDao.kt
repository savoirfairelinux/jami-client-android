package cx.ring.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import cx.ring.database.models.ConversationModel
import io.reactivex.Flowable

/**
 * Created by hdesousa on 09/11/17.
 */
@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversation")
    fun getAllConversation(): Flowable<List<ConversationModel>>

    @Insert
    fun insert(conversation: ConversationModel)
}