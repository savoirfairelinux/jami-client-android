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
import net.jami.model.Contact
import net.jami.smartlist.SmartListViewModel

class ContactCard : Card {
    private var mModel: SmartListViewModel

    constructor(accountId: String, pContact: Contact, type: Type?) {
        mModel = SmartListViewModel(accountId, pContact, null)
        id = pContact.id
        title = pContact.displayName
        description = pContact.ringUsername
        this.type = type
    }

    constructor(m: SmartListViewModel) {
        mModel = m
        model = m
    }

    var model: SmartListViewModel
        get() = mModel
        set(model) {
            mModel = model
            title = model.contactName ?: ""
            val contact = model.getContact()!!
            val username = contact.ringUsername
            description = username
            val isOnline = contact.isOnline
            type = if (model.contactName == username) {
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