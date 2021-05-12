/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package cx.ring.tv.cards.contacts;

import net.jami.model.CallContact;
import net.jami.smartlist.SmartListViewModel;
import cx.ring.tv.cards.Card;

public class ContactCard extends Card {
    private SmartListViewModel mModel;

    public ContactCard(String accountId, CallContact pCallContact, Type type) {
        mModel =  new SmartListViewModel(accountId, pCallContact, null);
        setId(pCallContact.getId());
        setTitle(pCallContact.getDisplayName());
        setDescription(pCallContact.getRingUsername());
        setType(type);
    }

    public ContactCard(SmartListViewModel model) {
        setModel(model);
    }

    public void setModel(SmartListViewModel model) {
        mModel = model;
        setTitle(mModel.getContactName());
        String username = mModel.getContact().get(0).getRingUsername();
        setDescription(username);
        boolean isOnline = mModel.getContact().get(0).isOnline();
        if (mModel.getContactName().equals(username)) {
            if (isOnline) {
                setType(Type.CONTACT_ONLINE);
            } else {
                setType(Type.CONTACT);
            }
        } else {
            if (isOnline) {
                setType(Type.CONTACT_WITH_USERNAME_ONLINE);
            } else {
                setType(Type.CONTACT_WITH_USERNAME);
            }
        }
    }

    public SmartListViewModel getModel() {
        return mModel;
    }
}
