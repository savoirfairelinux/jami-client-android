/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.services;

import java.util.List;
import java.util.Map;

import cx.ring.model.CallContact;
import cx.ring.utils.Tuple;
import ezvcard.VCard;
import io.reactivex.Single;

public abstract class VCardService {

    public static final int MAX_SIZE_SIP = 256 * 1024;
    public static final int MAX_SIZE_REQUEST = 64 * 1024;

    public abstract Single<VCard> loadSmallVCard(String accountId, int maxSize);
    public abstract Single<Tuple<String, Object>> loadVCardProfile(VCard vcard);
    public abstract void migrateContact(Map<String, CallContact> contacts, String accountId);
    public abstract void migrateProfiles(List<String> accountIds);
    public abstract void deleteLegacyProfiles();

}
