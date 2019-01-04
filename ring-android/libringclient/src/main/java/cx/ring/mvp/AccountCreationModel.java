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
package cx.ring.mvp;

import cx.ring.model.Account;
import ezvcard.VCard;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public abstract class AccountCreationModel {

    protected String mFullName = "";
    protected String mUsername = "";
    protected String mPassword = "";
    protected String mPin = "";
    protected boolean link = false;
    protected boolean mPush = false;
    private Account newAccount = null;
    protected Object photo = null;

    private Observable<Account> account;
    protected final Subject<AccountCreationModel> profile = BehaviorSubject.createDefault(this);

    public AccountCreationModel() {
    }

    public String getFullName() {
        return mFullName;
    }

    public void setFullName(String fullName) {
        this.mFullName = fullName;
        profile.onNext(this);
    }

    public Object getPhoto() {
        return photo;
    }

    public void setPhoto(Object photo) {
        this.photo = photo;
        profile.onNext(this);
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        this.mPassword = password;
    }

    public String getPin() {
        return mPin;
    }

    public void setPin(String pin) {
        this.mPin = pin.toUpperCase();
    }

    public boolean isPush() {
        return mPush;
    }

    public void setPush(boolean push) {
        mPush = push;
    }

    public boolean isLink() {
        return link;
    }

    public void setLink(boolean link) {
        this.link = link;
    }

    public void setNewAccount(Account account) {
        newAccount = account;
        profile.onNext(this);
    }

    public Account getNewAccount() {
        return newAccount;
    }

    public void setAccountObservable(Observable<Account> account) {
        this.account = account;
    }

    public Observable<Account> getAccountObservable() {
        return account;
    }

    public abstract Single<VCard> toVCard();

    public Observable<AccountCreationModel> getProfileUpdates() {
        return profile;
    }
}
