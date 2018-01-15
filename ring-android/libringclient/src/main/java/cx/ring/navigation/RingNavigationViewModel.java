/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

package cx.ring.navigation;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import cx.ring.model.Account;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public class RingNavigationViewModel {

    final private WeakReference<Account> mAccount;
    final private List<Account> mAccounts;

    public RingNavigationViewModel(Account account, List<Account> accounts) {
        mAccount = new WeakReference<>(account);
        mAccounts = accounts;
    }

    private boolean isAccountValid() {
        return mAccount.get() != null;
    }

    public VCard getVcard(File filesDir) {
        Account account = mAccount.get();
        if (account == null) {
            return null;
        }
        String accountId = isAccountValid() ? account.getAccountID() : null;
        return VCardUtils.loadLocalProfileFromDisk(filesDir, accountId);
    }

    public Account getAccount() {
        if (!isAccountValid()) {
            return null;
        }
        return mAccount.get();
    }

    public List<Account> getAccounts() {
        return mAccounts;
    }
}
