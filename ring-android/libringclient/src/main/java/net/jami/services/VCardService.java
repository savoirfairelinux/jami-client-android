/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.services;

import java.io.File;

import net.jami.model.Account;
import net.jami.utils.Tuple;
import ezvcard.VCard;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public abstract class VCardService {

    public static final int MAX_SIZE_SIP = 256 * 1024;
    public static final int MAX_SIZE_REQUEST = 16 * 1024;

    public abstract Observable<Tuple<String, Object>> loadProfile(Account account);

    public abstract Maybe<VCard> loadSmallVCard(String accountId, int maxSize);
    public Single<VCard> loadSmallVCardWithDefault(String accountId, int maxSize) {
        return loadSmallVCard(accountId, maxSize)
                .switchIfEmpty(Single.fromCallable(VCard::new));
    }

    public abstract Single<VCard> saveVCardProfile(String accountId, String uri, String displayName, String picture);
    public abstract Single<Tuple<String, Object>> loadVCardProfile(VCard vcard);
    public abstract Single<Tuple<String, Object>> peerProfileReceived(String accountId, String peerId, File vcard);

    public abstract Object base64ToBitmap(String base64);

}
