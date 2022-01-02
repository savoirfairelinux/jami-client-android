/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package cx.ring.tv.cards

import android.content.Context
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.Presenter
import cx.ring.tv.cards.iconcards.IconCardPresenter
import cx.ring.tv.cards.contacts.ContactCardPresenter
import cx.ring.R
import net.jami.services.ConversationFacade
import java.security.InvalidParameterException
import java.util.HashMap

/**
 * This PresenterSelector will decide what Presenter to use depending on a given card's type.
 */
class CardPresenterSelector(private val context: Context, private val conversationFacade: ConversationFacade) :
    PresenterSelector() {
    private val presenters = HashMap<Card.Type, Presenter>()
    override fun getPresenter(item: Any): Presenter {
        val type = (item as Card).type
        return presenters.getOrPut(type) {
            when (type) {
                Card.Type.ACCOUNT_ADD_DEVICE, Card.Type.ACCOUNT_EDIT_PROFILE, Card.Type.ACCOUNT_SHARE_ACCOUNT, Card.Type.ADD_CONTACT -> IconCardPresenter(context)
                Card.Type.SEARCH_RESULT, Card.Type.CONTACT -> ContactCardPresenter(context, conversationFacade, R.style.ContactCardTheme)
                Card.Type.CONTACT_ONLINE -> ContactCardPresenter(context, conversationFacade, R.style.ContactCardOnlineTheme)
                Card.Type.CONTACT_WITH_USERNAME -> ContactCardPresenter(
                    context,
                    conversationFacade,
                    R.style.ContactCompleteCardTheme
                )
                Card.Type.CONTACT_WITH_USERNAME_ONLINE -> ContactCardPresenter(
                    context,
                    conversationFacade,
                    R.style.ContactCompleteCardOnlineTheme
                )
                else -> throw InvalidParameterException("Uncatched card type $type")
            }
        }
    }
}