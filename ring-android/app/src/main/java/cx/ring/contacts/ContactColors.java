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

import java.util.Arrays;
import java.util.List;

public class ContactColors {

    public static final MaterialColor UNKNOWN_COLOR = MaterialColor.GREY;

    public static final List<MaterialColor> CONVERSATION_PALETTE = Arrays.asList(
            MaterialColor.RED,
            MaterialColor.PINK,
            MaterialColor.PURPLE,
            MaterialColor.DEEP_PURPLE,
            MaterialColor.INDIGO,
            MaterialColor.BLUE,
            MaterialColor.LIGHT_BLUE,
            MaterialColor.CYAN,
            MaterialColor.TEAL,
            MaterialColor.GREEN,
            MaterialColor.LIGHT_GREEN,
            MaterialColor.ORANGE,
            MaterialColor.DEEP_ORANGE,
            MaterialColor.AMBER,
            MaterialColor.BLUE_GREY
    );

    public static MaterialColor generateFor(String name) {
        if (name == null) {
            return UNKNOWN_COLOR;
        }
        return CONVERSATION_PALETTE.get(Math.abs(name.hashCode()) % CONVERSATION_PALETTE.size());
    }
}
