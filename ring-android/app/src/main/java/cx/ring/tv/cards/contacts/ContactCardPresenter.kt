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

import android.content.Context
import android.view.ContextThemeWrapper
import cx.ring.R
import cx.ring.tv.cards.AbstractCardPresenter
import cx.ring.tv.cards.Card
import cx.ring.tv.cards.CardView
import cx.ring.views.AvatarDrawable

class ContactCardPresenter(context: Context, resId: Int) :
    AbstractCardPresenter<CardView>(ContextThemeWrapper(context, resId)) {
    override fun onCreateView(): CardView {
        return CardView(context)
    }

    override fun onBindViewHolder(card: Card, cardView: CardView) {
        val contact = card as ContactCard
        cardView.titleText = card.getTitle()
        cardView.contentText = card.getDescription()
        cardView.setTitleSingleLine(true)
        cardView.setBackgroundColor(context.resources.getColor(R.color.tv_transparent))
        cardView.setInfoAreaBackgroundColor(context.resources.getColor(R.color.transparent))
        cardView.mainImage = AvatarDrawable.Builder()
            .withViewModel(contact.model)
            .withPresence(false)
            .withCircleCrop(false)
            .build(context)
    }
}