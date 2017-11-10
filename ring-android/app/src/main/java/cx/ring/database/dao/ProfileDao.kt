package cx.ring.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import cx.ring.database.models.ProfileModel
import io.reactivex.Flowable

/**
 * Created by hdesousa on 09/11/17.
 */
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile")
    fun getAllProfile(): Flowable<List<ProfileModel>>

    @Insert
    fun insert(profile: ProfileModel)
}