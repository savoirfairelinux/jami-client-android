/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package net.jami.share;

import net.jami.utils.QRCodeUtils;
import net.jami.model.Account;

public class ShareViewModel {

    private final String shareUri;
    private final String displayUri;
    private final Account mAccount;

    public ShareViewModel(Account account) {
        shareUri = account.getUri();
        displayUri = account.getDisplayUri();
        mAccount = account;
    }

    public net.jami.utils.QRCodeUtils.QRCodeData getAccountQRCodeData(final int foregroundColor, final int backgroundColor) {
        return QRCodeUtils.encodeStringAsQRCodeData(shareUri, foregroundColor, backgroundColor);
    }

    public String getAccountShareUri() {
        return shareUri;
    }

    public String getAccountDisplayUri() {
        return displayUri;
    }

    public Account getAccount() {
        return mAccount;
    }


}
