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

import java.io.File;

import cx.ring.model.Account;
import ezvcard.VCard;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public abstract class AccountCreationModel {

    private String mManagementServer = null;
    private String mFullName = "";
    private String mUsername = "";
    private String mPassword = "";
    private String mPin = "";
    private File mArchive = null;

    private boolean link = false;
    private boolean mPush = true;
    private Account newAccount = null;
    private Object photo = null;

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

    public void setArchive(File archive) {
        mArchive = archive;
    }

    public File getArchive() {
        return mArchive;
    }

    public void setManagementServer(String server) {
        mManagementServer = server;
    }

    public String getManagementServer() {
        return mManagementServer;
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
