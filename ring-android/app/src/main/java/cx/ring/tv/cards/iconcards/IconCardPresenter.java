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
package cx.ring.tv.cards.iconcards;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.leanback.widget.ImageCardView;
import androidx.core.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.widget.ImageView;


import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.tv.cards.AbstractCardPresenter;
import cx.ring.tv.cards.Card;

public class IconCardPresenter extends AbstractCardPresenter<ImageCardView> {

    private static final int ANIMATION_DURATION = 200;

    public IconCardPresenter(Context context) {
        super(new ContextThemeWrapper(context, R.style.IconCardTheme));
    }

    @Override
    protected ImageCardView onCreateView() {
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
        ImageCardView imageCardView = new ImageCardView(getContext());
        final ImageView image = imageCardView.getMainImageView();
        image.setBackgroundResource(R.drawable.icon_focused);
        image.getBackground().setAlpha(0);
        imageCardView.setOnFocusChangeListener((v, hasFocus) -> animateIconBackground(image.getBackground(), hasFocus));
        return imageCardView;
    }

    @Override
    public void onBindViewHolder(Card card, ImageCardView cardView) {
        cardView.setTitleText(card.getTitle());
        cardView.setContentText(card.getDescription());
        cardView.setMainImage(card.getDrawable(cardView.getContext()));
    }

    private void animateIconBackground(Drawable drawable, boolean hasFocus) {
        if (hasFocus) {
            ObjectAnimator.ofInt(drawable, "alpha", 0, 255).setDuration(ANIMATION_DURATION).start();
        } else {
            ObjectAnimator.ofInt(drawable, "alpha", 255, 0).setDuration(ANIMATION_DURATION).start();
        }
    }
}
