/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import cx.ring.tv.cards.Card

class IconCard : Card {
    constructor(type: Type, name: String, description: CharSequence, context: Context, @DrawableRes imageId: Int) : super(type, name, description) {
        drawable = if (imageId != -1) ContextCompat.getDrawable(context, imageId)!! else ColorDrawable(0x00000000)
    }

    constructor(type: Type, name: String, description: CharSequence, bitmapDrawable: BitmapDrawable?) : super(type, name, description) {
        drawable = bitmapDrawable
    }
}