package cx.ring.services

import cx.ring.database.dao.ConversationDao
import cx.ring.database.dao.HistoryTextDao
import cx.ring.database.dao.InteractionDao
import cx.ring.database.dao.ProfileDao
import javax.inject.Inject

/**
 * Created by hdesousa on 10/11/17.
 */
class RoomService @Inject constructor(val profileDao: ProfileDao,
                                      val conversationDao: ConversationDao,
                                      val interactionDao: InteractionDao,
                                      val historyTextDao: HistoryTextDao) {


}