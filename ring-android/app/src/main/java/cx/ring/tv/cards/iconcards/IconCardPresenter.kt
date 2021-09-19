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
package cx.ring.tv.cards.iconcards

import android.content.Context
import cx.ring.tv.cards.AbstractCardPresenter
import cx.ring.R
import android.graphics.PorterDuff
import android.view.ContextThemeWrapper
import android.view.View
import cx.ring.tv.cards.Card
import cx.ring.tv.cards.CardView

class IconCardPresenter(context: Context) : AbstractCardPresenter<CardView>(ContextThemeWrapper(context, R.style.ContactCardTheme)) {
    override fun onCreateView(): CardView {
        val cardView = CardView(context)
        cardView.setTitleSingleLine(false)
        cardView.setBackgroundColor(context.resources.getColor(R.color.tv_transparent))
        cardView.setInfoAreaBackgroundColor(context.resources.getColor(R.color.transparent))
        val image = cardView.mainImageView
        image.setPadding(IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING)
        image.setColorFilter(context.resources.getColor(android.R.color.white), PorterDuff.Mode.SRC_IN)
        cardView.titleTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return cardView
    }

    override fun onBindViewHolder(card: Card, cardView: CardView) {
        cardView.titleText = card.title
        cardView.contentText = card.description
        cardView.mainImage = card.getDrawable(cardView.context)
    }

    companion object {
        private const val IMAGE_PADDING = 35
    }
}