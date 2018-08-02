/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import java.util.List;

import cx.ring.model.Account;
import ezvcard.VCard;

public class RingNavigationViewModel {

    final private Account mAccount;
    final private List<Account> mAccounts;
    final private VCard mProfile;

    public RingNavigationViewModel(Account account, List<Account> accounts) {
        mAccount = account;
        mAccounts = accounts;
        mProfile = account == null ? null : account.getProfile();
    }

    public VCard getVcard() {
        return mProfile;
    }

    public Account getAccount() {
        return mAccount;
    }

    public List<Account> getAccounts() {
        return mAccounts;
    }
}
