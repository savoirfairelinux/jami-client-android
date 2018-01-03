/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.tv.cards.contactrequests;

import java.util.Arrays;

import cx.ring.tv.cards.Card;
import cx.ring.tv.model.TVContactRequestViewModel;

public class ContactRequestCard extends Card {
    TVContactRequestViewModel mModel = null;
    private byte[] mPhoto = null;

    public ContactRequestCard(TVContactRequestViewModel model) {
        mModel = model;
        setTitle(mModel.getUserName());
        setDescription(mModel.getDisplayName());
        if (mModel.getPhoto() != null) {
            mPhoto = mModel.getPhoto();
        }
        if (mModel.getDisplayName().equals(mModel.getUserName())) {
            setType(Type.CONTACT_REQUEST);
        } else {
            setType(Type.CONTACT_REQUEST_WITH_USERNAME);
        }
    }

    public TVContactRequestViewModel getModel() {
        return mModel;
    }


    public byte[] getPhoto() {
        return mPhoto;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (pO == null || getClass() != pO.getClass()) return false;

        ContactRequestCard that = (ContactRequestCard) pO;

        if (mModel != null) {
            return mModel.getContactId().equals(that.mModel.getContactId());
        }

        return Arrays.equals(mPhoto, that.mPhoto);

    }

}
