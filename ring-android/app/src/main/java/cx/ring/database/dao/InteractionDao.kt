package cx.ring.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import cx.ring.database.models.InteractionModel
import io.reactivex.Flowable

/**
 * Created by hdesousa on 09/11/17.
 */
@Dao
interface InteractionDao {

    @Query("SELECT * FROM interaction")
    fun getAllInteraction(): Flowable<List<InteractionModel>>

    @Insert
    fun insert(interaction: InteractionModel)
}