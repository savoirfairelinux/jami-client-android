package cx.ring.database.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by hdesousa on 09/11/17.
 */
@Entity(tableName = "profile")
data class ProfileModel(
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        val uri: String = "",
        val alias: String = "",
        val photo: String = "",
        val type: String = "",
        val status: String = ""
)