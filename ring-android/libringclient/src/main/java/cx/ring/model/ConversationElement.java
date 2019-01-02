/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 * Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

package cx.ring.model;

public interface ConversationElement {

    CEType getType();

    long getDate();

    Uri getContactNumber();

    boolean isRead();

    long getId();

    enum CEType {
        TEXT, CALL, FILE, CONTACT
    }

    static int compare(ConversationElement a, ConversationElement b) {
        if (a == null)
            return b == null ? 0 : -1;
        if (b == null) return 1;
        return Long.compare(a.getDate(), b.getDate());
    }
}
