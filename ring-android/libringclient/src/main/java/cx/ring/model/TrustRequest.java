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

package cx.ring.model;

import java.util.Random;

import ezvcard.VCard;

public class TrustRequest {

    private static final String TAG = TrustRequest.class.getSimpleName();

    private String mAccountId;
    private String mContactUsername = null;
    private String mContactId;
    private VCard mVcard;
    private String message;
    private int mUuid;

    public TrustRequest(String accountId, String contact){
        mAccountId = accountId;
        mContactId = contact;
        mVcard = new VCard();
        message = null;
        mUuid = new Random().nextInt();
    }

    public int getUuid(){
        return mUuid;
    }

    public String getAccountId(){
        return mAccountId;
    }

    public String getContactId(){
        return mContactId;
    }

    public  String getDisplayname(){
        if(mVcard != null && mVcard.getFormattedName() != null){
            return mVcard.getFormattedName().getValue();
        }
        return mContactUsername == null ? mContactId : mContactUsername;
    }

    public void setUsername(String username){
        mContactUsername = username;
    }
}
