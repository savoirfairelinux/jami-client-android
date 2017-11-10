package cx.ring.database.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by hdesousa on 10/11/17.
 */
@Entity(tableName = "historytext")
data class HistoryTextModel(
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        val accountID: String,
        val callID: String = "",
        val contactID: Long = 0,
        val contactKey: String = "",
        val direction: Int = 0,
        val message: String = "",
        val number: String = "",
        val read: Int = 0,
        val state: String = "",
        val TIMESTAMP: Long
)