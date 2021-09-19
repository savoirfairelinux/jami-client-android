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
package cx.ring.tv.cards.iconcards

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import cx.ring.tv.cards.iconcards.IconCard
import cx.ring.tv.cards.iconcards.IconCardHelper
import cx.ring.R
import cx.ring.tv.cards.Card

object IconCardHelper {
    fun getAboutCardByType(pContext: Context, type: Card.Type?): IconCard? {
        return when (type) {
            Card.Type.ACCOUNT_ADD_DEVICE -> getAccountAddDeviceCard(pContext)
            Card.Type.ACCOUNT_EDIT_PROFILE -> getAccountManagementCard(pContext)
            Card.Type.ACCOUNT_SHARE_ACCOUNT -> getAccountShareCard(pContext, null)
            else -> null
        }
    }

    fun getAccountAddDeviceCard(pContext: Context): IconCard {
        return IconCard(
            Card.Type.ACCOUNT_ADD_DEVICE,
            pContext.getString(R.string.account_export_title),
            "",
            R.drawable.baseline_androidtv_link_device
        )
    }

    fun getAccountManagementCard(pContext: Context): IconCard {
        return IconCard(
            Card.Type.ACCOUNT_EDIT_PROFILE,
            pContext.getString(R.string.account_edit_profile),
            "",
            R.drawable.baseline_androidtv_account
        )
    }

    fun getAccountShareCard(pContext: Context, bitmapDrawable: BitmapDrawable?): IconCard {
        return IconCard(
            Card.Type.ACCOUNT_SHARE_ACCOUNT,
            pContext.getString(R.string.menu_item_share),
            "",
            bitmapDrawable
        )
    }

    fun getAddContactCard(pContext: Context): IconCard {
        return IconCard(
            Card.Type.ADD_CONTACT,
            pContext.getString(R.string.account_tv_add_contact),
            "",
            R.drawable.baseline_androidtv_add_user
        )
    }
}