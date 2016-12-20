/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
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
package cx.ring.share;

import java.lang.ref.WeakReference;

import cx.ring.model.Account;
import cx.ring.utils.QRCodeUtils;

public class ShareViewModel {

    private WeakReference<Account> mAccount;

    public ShareViewModel(Account account) {
        mAccount = new WeakReference<>(account);
    }

    private boolean isAccountValid() {
        return mAccount != null && mAccount.get() != null && mAccount.get().isEnabled();
    }

    public QRCodeUtils.QRCodeData getAccountQRCodeData() {
        if (!isAccountValid()) {
            return null;
        }

        String accountShareUri = getAccountShareUri();

        if (accountShareUri == null || accountShareUri.isEmpty()) {
            return null;
        }

        return QRCodeUtils.encodeStringAsQRCodeData(mAccount.get().getShareURI());
    }

    public String getAccountShareUri() {
        if (!isAccountValid()) {
            return null;
        }

        return mAccount.get().getShareURI();
    }

    public String getAccountRegisteredUsername() {
        if (!isAccountValid()) {
            return null;
        }

        return mAccount.get().getRegisteredName();
    }

}
