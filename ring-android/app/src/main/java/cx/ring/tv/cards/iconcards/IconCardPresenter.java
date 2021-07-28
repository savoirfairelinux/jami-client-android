/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
 *  Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
package cx.ring.tv.cards.iconcards;

import android.content.Context;
import android.graphics.PorterDuff;

import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;

import cx.ring.R;
import cx.ring.tv.cards.AbstractCardPresenter;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardView;

public class IconCardPresenter extends AbstractCardPresenter<CardView> {

    private static final int IMAGE_PADDING = 35;

    public IconCardPresenter(Context context) {
        super(new ContextThemeWrapper(context, R.style.ContactCardTheme));
    }

    @Override
    protected CardView onCreateView() {
        CardView cardView = new CardView(getContext());
        cardView.setTitleSingleLine(false);
        cardView.setBackgroundColor(getContext().getResources().getColor(R.color.tv_transparent));
        cardView.setInfoAreaBackgroundColor(getContext().getResources().getColor(R.color.transparent));
        ImageView image = cardView.getMainImageView();
        image.setPadding(IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING);
        image.setColorFilter(getContext().getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
        cardView.getTitleTextView().setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return cardView;
    }

    @Override
    public void onBindViewHolder(Card card, CardView cardView) {
        cardView.setTitleText(card.getTitle());
        cardView.setContentText(card.getDescription());
        cardView.setMainImage(card.getDrawable(cardView.getContext()));
    }
    
}
