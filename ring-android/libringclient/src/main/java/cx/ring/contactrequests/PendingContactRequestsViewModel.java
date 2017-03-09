/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.contactrequests;

import cx.ring.model.Account;
import cx.ring.model.TrustRequest;
import ezvcard.VCard;

public class PendingContactRequestsViewModel {

    private VCard mVcard;
    private String mFullname;
    private String mUsername;
    private String mAccountUsername;
    private boolean hasPane;
    private String mContactId;


    public PendingContactRequestsViewModel(Account account, TrustRequest trustRequest, boolean pane) {
        mVcard = trustRequest.getVCard();
        mFullname = trustRequest.getFullname();
        mUsername = trustRequest.getDisplayname();
        hasPane = pane;
        mAccountUsername = getAccountUsername(account);
        mContactId = trustRequest.getContactId();
    }

    private String getAccountUsername(Account account) {
        String username = account.getRegisteredName();
        if (account.registeringUsername || username == null || username.isEmpty()) {
            username = account.getUsername();
        }
        return username;
    }

    public String getAccountUsername() {
        return mAccountUsername;
    }

    public VCard getVCard() {
        return mVcard;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getFullname() {
        return mFullname;
    }

    public boolean hasPane() {
        return hasPane;
    }

    public String getContactId() {
        return mContactId;
    }
}
