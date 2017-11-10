package cx.ring.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import cx.ring.database.models.HistoryTextModel
import io.reactivex.Flowable

/**
 * Created by hdesousa on 10/11/17.
 */
@Dao
interface HistoryTextDao {

    @Query("SELECT * FROM historytext")
    fun getAllHistorytext(): Flowable<List<HistoryTextModel>>

    @Insert
    fun insert(historytext: HistoryTextModel)
}