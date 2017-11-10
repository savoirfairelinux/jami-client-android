package cx.ring.database.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

/**
 * Created by hdesousa on 09/11/17.
 */
@Entity(tableName = "conversation",
        foreignKeys = arrayOf(ForeignKey(entity = ProfileModel::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("participantId"))))
data class ConversationModel(
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        val participantId: Long
)