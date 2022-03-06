/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import cx.ring.R
import cx.ring.tv.cards.AbstractCardPresenter
import cx.ring.tv.cards.Card
import cx.ring.tv.cards.CardView
import cx.ring.views.AvatarDrawable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.services.ConversationFacade

class ContactCardPresenter(context: Context, val conversationFacade: ConversationFacade, resId: Int) :
    AbstractCardPresenter<CardView>(ContextThemeWrapper(context, resId)) {
    override fun onCreateView() = CardView(context).apply {
        setMainImage(ContextCompat.getDrawable(context, R.drawable.tv_item_selected_background), false)
        setTitleSingleLine(true)
        setBackgroundColor(ContextCompat.getColor(context, R.color.tv_transparent))
        setInfoAreaBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
    }

    data class ConversationView (
        val title: String,
        val uri: String,
        val avatar: Drawable,
        val isOnline: Boolean
    )

    override fun onBindViewHolder(card: Card, cardView: CardView, disposable: CompositeDisposable) {
        val badge = cardView.badgeImage
        disposable.add(conversationFacade.observeConversation((card as ContactCard).model, card.type.hasPresence())
            .map { vm -> ConversationView(vm.contactName, vm.uriTitle, AvatarDrawable.Builder()
                .withViewModel(vm)
                .withPresence(false)
                .withCircleCrop(false)
                .build(context), vm.isOnline) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { vm -> cardView.apply {
                titleText = vm.title
                contentText = vm.uri
                badgeImage = if (vm.isOnline) badge else null
                setMainImage(vm.avatar, true)
            }
        })
    }
}