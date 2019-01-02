/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import java.util.Arrays;

import cx.ring.model.CallContact;
import cx.ring.tv.cards.Card;
import cx.ring.tv.model.TVListViewModel;

public class ContactCard extends Card {
    private TVListViewModel mModel;
    private byte[] mPhoto = null;

    public ContactCard(CallContact pCallContact, Type type) {
        mModel =  new TVListViewModel(pCallContact);
        setId(pCallContact.getId());
        setTitle(pCallContact.getDisplayName());
        setDescription(pCallContact.getRingUsername());
        if (pCallContact.getPhoto() != null) {
            mPhoto = pCallContact.getPhoto();
        }
        setType(type);
    }

    public ContactCard(TVListViewModel model) {
        mModel = model;
        setTitle(mModel.getContact().getDisplayName());
        setDescription(mModel.getContact().getRingUsername());
        if (mModel.getContact().getPhoto() != null) {
            mPhoto = mModel.getContact().getPhoto();
        }
        if (mModel.getContact().getDisplayName().equals(mModel.getContact().getRingUsername())) {
            if (model.isOnline()) {
                setType(Type.CONTACT_ONLINE);
            } else {
                setType(Type.CONTACT);
            }
        } else {
            if (model.isOnline()) {
                setType(Type.CONTACT_WITH_USERNAME_ONLINE);
            } else {
                setType(Type.CONTACT_WITH_USERNAME);
            }
        }
    }

    public void setModel(TVListViewModel model) {
        mModel = model;

        setTitle(model.getContact().getDisplayName());
        setDescription(model.getContact().getRingUsername());

        if (model.isOnline()) {
            setType(Type.CONTACT_WITH_USERNAME_ONLINE);
        } else {
            setType(Type.CONTACT_WITH_USERNAME);
        }

        if (model.getContact().getPhoto() != null && !Arrays.equals(mPhoto, model.getContact().getPhoto())) {
            mPhoto = model.getContact().getPhoto();
        }
    }

    public TVListViewModel getModel() {
        return mModel;
    }


    public byte[] getPhoto() {
        return mPhoto;
    }

}
