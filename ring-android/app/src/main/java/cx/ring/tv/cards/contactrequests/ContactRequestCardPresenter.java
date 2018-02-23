/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.view.ContextThemeWrapper;

import cx.ring.R;
import cx.ring.contacts.AvatarFactory;
import cx.ring.tv.cards.AbstractCardPresenter;
import cx.ring.tv.cards.Card;
import cx.ring.tv.model.TVContactRequestViewModel;

public class ContactRequestCardPresenter extends AbstractCardPresenter<ImageCardView> {

    private static final String TAG = ContactRequestCardPresenter.class.getSimpleName();

    public ContactRequestCardPresenter(Context context, int resId) {
        super(new ContextThemeWrapper(context, resId));
    }

    @Override
    protected ImageCardView onCreateView() {
        return new ImageCardView(getContext());
    }

    @Override
    public void onBindViewHolder(Card card, ImageCardView cardView) {
        ContactRequestCard contact = (ContactRequestCard) card;

        TVContactRequestViewModel model = contact.getModel();
        if (model.getUserName().isEmpty() || model.getDisplayName().equals(model.getUserName())) {
            cardView.setTitleText(model.getDisplayName());
            cardView.setContentText("");
        } else {
            cardView.setTitleText(model.getUserName());
            cardView.setContentText(model.getDisplayName());
        }

        cardView.setBackgroundColor(cardView.getResources().getColor(R.color.color_primary_dark));
        cardView.setMainImage(getCardImage(contact));
    }

    public Drawable getCardImage(ContactRequestCard contact) {
        String username = contact.getModel().getDisplayName();
        if (username == null || username.isEmpty()) {
            username = contact.getModel().getUserName();
        }

        return AvatarFactory.getAvatar(getContext(),
                contact.getPhoto(),
                username,
                contact.getModel().getContactId());
    }
}
