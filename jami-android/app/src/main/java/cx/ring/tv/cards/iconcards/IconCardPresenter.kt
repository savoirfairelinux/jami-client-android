/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.tv.cards.iconcards

import android.content.Context
import cx.ring.tv.cards.AbstractCardPresenter
import cx.ring.R
import android.graphics.PorterDuff
import android.view.ContextThemeWrapper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import cx.ring.tv.cards.Card
import cx.ring.tv.cards.CardView
import io.reactivex.rxjava3.disposables.CompositeDisposable

class IconCardPresenter(context: Context) : AbstractCardPresenter<CardView>(ContextThemeWrapper(context, R.style.ContactCardTheme)) {
    override fun onCreateView() = CardView(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.tv_transparent))
        setInfoAreaBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
        mainImageView.apply {
            setPadding(IMAGE_PADDING)
            setColorFilter(ContextCompat.getColor(context, android.R.color.white), PorterDuff.Mode.SRC_IN)
        }
        titleTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    override fun onBindViewHolder(card: Card, cardView: CardView, disposable: CompositeDisposable) {
        cardView.titleText = card.title
        cardView.contentText = card.description
        cardView.mainImage = card.drawable
    }

    companion object {
        private const val IMAGE_PADDING = 35
    }
}