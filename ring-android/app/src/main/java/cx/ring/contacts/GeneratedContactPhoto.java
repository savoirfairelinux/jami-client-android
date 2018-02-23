/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.contacts;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.amulyakhare.textdrawable.TextDrawable;

public class GeneratedContactPhoto implements ContactPhoto {

    private final String name;

    GeneratedContactPhoto(String name) {
        this.name = name;
    }

    @Override
    public Drawable asDrawable(Context context, int color) {
        return asDrawable(context, color, false);
    }

    @Override
    public Drawable asDrawable(Context context, int color, boolean inverted) {
//        int targetSize = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);
        int targetSize = 128;

        return TextDrawable.builder()
                .beginConfig()
                .width(targetSize)
                .height(targetSize)
                .textColor(inverted ? color : Color.WHITE)
                .endConfig()
                .buildRound(getCharacter(name), inverted ? Color.WHITE : color);
    }

    private String getCharacter(String name) {
        String cleanedName = name.replaceFirst("[^\\p{L}\\p{Nd}\\p{P}\\p{S}]+", "");

        if (cleanedName.isEmpty()) {
            return "#";
        } else {
            return String.valueOf(cleanedName.charAt(0));
        }
    }
}