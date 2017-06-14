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

    private final WeakReference<Account> mAccount;

    public ShareViewModel(Account account) {
        mAccount = new WeakReference<>(account);
    }

    private Account getAccount() {
        Account account = mAccount.get();
        if (account == null  || !account.isEnabled())
            return null;
        return account;
    }

    public QRCodeUtils.QRCodeData getAccountQRCodeData() {
        Account account = getAccount();
        if (account == null) {
            return null;
        }

        return QRCodeUtils.encodeStringAsQRCodeData(account.getUri());
    }

    public String getAccountShareUri() {
        Account account = getAccount();
        if (account == null) {
            return null;
        }

        return account.getDisplayUri();
    }
}
