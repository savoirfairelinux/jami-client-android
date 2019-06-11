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

import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.DrawableRes;

import cx.ring.tv.cards.Card;

public class IconCard extends Card {

    public IconCard(Type pType, String name, CharSequence description, @DrawableRes int imageId) {
        setType(pType);
        setTitle(name);
        setDescription(description);
        setLocalImageResource(imageId);
    }

    public IconCard(Type pType, String name, CharSequence description, BitmapDrawable bitmapDrawable) {
        setType(pType);
        setTitle(name);
        setDescription(description);
        setBitmapDrawableResource(bitmapDrawable);
    }
}
