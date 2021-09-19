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

import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import cx.ring.tv.cards.Card

class IconCard : Card {
    constructor(pType: Type, name: String, description: CharSequence, @DrawableRes imageId: Int) {
        type = pType
        title = name
        this.description = description
        localImageResource = imageId
    }

    constructor(pType: Type, name: String, description: CharSequence, bitmapDrawable: BitmapDrawable?) {
        type = pType
        title = name
        this.description = description
        setDrawable(bitmapDrawable)
    }
}