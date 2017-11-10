package cx.ring.database.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

/**
 * Created by hdesousa on 09/11/17.
 */
@Entity(tableName = "interaction",
        foreignKeys = arrayOf(
                ForeignKey(entity = ProfileModel::class,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("accountId")),
                ForeignKey(entity = ProfileModel::class,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("authorId")),
                ForeignKey(entity = ConversationModel::class,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("conversationId"))))
data class InteractionModel(
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        val accountId: Long,
        val authorId: Long,
        val conversationId: Long,
        val timestamp: Int,
        val body: String = "",
        val type: String = "",
        val status: String = "",
        val directionIn: Boolean
)