/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.cards.contacts

import cx.ring.tv.cards.Card
import net.jami.model.ContactViewModel
import net.jami.smartlist.ConversationItemViewModel

class ContactCard : Card {
    private var mModel: ConversationItemViewModel

    constructor(m: ConversationItemViewModel, type: Type) {
        mModel = m
        model = m
        //this.type = type
    }

    var model: ConversationItemViewModel
        get() = mModel
        set(model) {
            mModel = model
            title = model.contactName
            description = model.uriTitle ?: ""
            val isOnline = model.isOnline
            type = if (title == description) {
                if (isOnline) {
                    Type.CONTACT_ONLINE
                } else {
                    Type.CONTACT
                }
            } else {
                if (isOnline) {
                    Type.CONTACT_WITH_USERNAME_ONLINE
                } else {
                    Type.CONTACT_WITH_USERNAME
                }
            }
        }
}