/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
package cx.ring.account;

import android.graphics.Bitmap;

import cx.ring.model.Account;

public class RingAccountViewModel {

    private Bitmap mPhotoProfile;
    private String mFullname;
    private String mUsername;
    private String mPassword;
    private String mPin;
    private String mAccountType;
    private Account mAccount;
    private String mCreatedAccountId;

    public Bitmap getPhotoProfile() {
        return mPhotoProfile;
    }

    public void setPhotoProfile(Bitmap mPhotoProfile) {
        this.mPhotoProfile = mPhotoProfile;
    }

    public String getFullname() {
        return mFullname;
    }

    public void setFullname(String mFullname) {
        this.mFullname = mFullname;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String mUsername) {
        this.mUsername = mUsername;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String mPassword) {
        this.mPassword = mPassword;
    }

    public String getPin() {
        return mPin;
    }

    public void setPin(String mPin) {
        this.mPin = mPin;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public void setAccountType(String mAccountType) {
        this.mAccountType = mAccountType;
    }

    public Account getAccount() {
        return mAccount;
    }

    public void setAccount(Account mAccount) {
        this.mAccount = mAccount;
    }

    public String getCreatedAccountId() {
        return mCreatedAccountId;
    }

    public void setCreatedAccountId(String mCreatedAccountId) {
        this.mCreatedAccountId = mCreatedAccountId;
    }
}
