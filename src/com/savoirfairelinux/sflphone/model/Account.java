/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package com.savoirfairelinux.sflphone.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Account implements Parcelable {

    String accountID;
    String host;
    String registered_state;
    String alias;

    private Account(String bAccountID, String bHost, String bRegistered_state, String bAlias) {
        accountID = bAccountID;
        host = bHost;
        registered_state = bRegistered_state;
        alias = bAlias;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRegistered_state() {
        return registered_state;
    }

    public void setRegistered_state(String registered_state) {
        this.registered_state = registered_state;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Account(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int arg1) {
        
        dest.writeString(accountID);
        dest.writeString(host);
        dest.writeString(registered_state);
        dest.writeString(alias);
    }

    private void readFromParcel(Parcel in) {

        accountID = in.readString();
        host = in.readString();
        registered_state = in.readString();
        alias = in.readString();
    }

    public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    public static class AccountBuilder {

        String bAccountID;
        String bHost;
        String bRegistered_state;
        String bAlias;
        
        private static final String TAG = AccountBuilder.class.getSimpleName();

        public AccountBuilder setHost(String h) {
            Log.i(TAG, "setHost" + h);
            bHost = h;
            return this;
        }

        public AccountBuilder setAlias(String h) {
            Log.i(TAG, "setAlias" + h);
            bAlias = h;
            return this;
        }

        public AccountBuilder setRegisteredState(String h) {
            Log.i(TAG, "setRegisteredState" + h);
            bRegistered_state = h;
            return this;
        }

        public AccountBuilder setAccountID(String h) {
            Log.i(TAG, "setAccountID" + h);
            bAccountID = h;
            return this;
        }

        public Account build() throws Exception {
            if (bHost.contentEquals("") || bAlias.contentEquals("") || bAccountID.contentEquals("") || bRegistered_state.contentEquals("")) {
                throw new Exception("Builders parameters missing");
            }
            return new Account(bAccountID, bHost, bRegistered_state, bAlias);
        }
        
        public static AccountBuilder getInstance() {
            return new AccountBuilder();
        }
    }

}
