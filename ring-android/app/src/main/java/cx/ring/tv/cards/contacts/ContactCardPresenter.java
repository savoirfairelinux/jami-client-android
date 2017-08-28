/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.view.ContextThemeWrapper;

import cx.ring.R;
import cx.ring.tv.cards.AbstractCardPresenter;
import cx.ring.tv.cards.Card;

public class ContactCardPresenter extends AbstractCardPresenter<ImageCardView> {

    public ContactCardPresenter(Context context, int resId) {
        super(new ContextThemeWrapper(context, resId));
    }

    @Override
    protected ImageCardView onCreateView() {
        ImageCardView imageCardView = new ImageCardView(getContext());
        return imageCardView;
    }

    @Override
    public void onBindViewHolder(Card card, ImageCardView cardView) {
        cardView.setTitleText(card.getTitle());
        cardView.setContentText(card.getDescription());
        cardView.setBackgroundColor(cardView.getResources().getColor(R.color.color_primary_dark));

        ContactCard contact = (ContactCard) card;
        if (contact.getPhoto() == null) {
            cardView.setMainImage(getDefaultCardImage());
        } else {
            cardView.setMainImage(new BitmapDrawable(cardView.getResources(), BitmapFactory.decodeByteArray(contact.getPhoto(), 0, contact.getPhoto().length)));
        }
    }

    public Drawable getDefaultCardImage() {
        return getContext().getResources().getDrawable(R.drawable.ic_contact_picture);
    }
}
