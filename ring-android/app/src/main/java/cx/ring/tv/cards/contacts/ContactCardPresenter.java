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

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ImageCardView;
import android.view.ContextThemeWrapper;

import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;

import cx.ring.R;
import cx.ring.contacts.AvatarFactory;
import cx.ring.model.CallContact;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.tv.cards.AbstractCardPresenter;
import cx.ring.tv.cards.Card;
import cx.ring.views.AvatarDrawable;

public class ContactCardPresenter extends AbstractCardPresenter<ImageCardView> {

    private static final String TAG = ContactCardPresenter.class.getSimpleName();

    public ContactCardPresenter(Context context, int resId) {
        super(new ContextThemeWrapper(context, resId));
    }

    @Override
    protected ImageCardView onCreateView() {
        return new ImageCardView(getContext());
    }

    @Override
    public void onBindViewHolder(Card card, ImageCardView cardView) {
        ContactCard contact = (ContactCard) card;

        SmartListViewModel model = contact.getModel();
        /*String username = model.getContact().getUsername();

        if (username == null) {
            username = model.getUri().getRawUriString();
        }

        if (username != null && (username.isEmpty() || username.equals(model.getContactName()))) {
            cardView.setTitleText(username);
            cardView.setContentText("");
        } else {
            cardView.setTitleText(model.getContactName());
            cardView.setContentText(username);
        }*/
        cardView.setTitleText(card.getTitle());
        cardView.setContentText(card.getDescription());

        cardView.setBackgroundColor(cardView.getResources().getColor(R.color.color_primary_dark));
        cardView.setMainImage(
                new AvatarDrawable.Builder()
                        .withViewModel(model)
                        .withPresence(false)
                        .withCircleCrop(false)
                        .build(getContext())
        );
    }
}
